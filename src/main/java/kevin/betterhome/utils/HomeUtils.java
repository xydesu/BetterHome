package kevin.betterhome.utils;

import com.earth2me.essentials.Essentials;
import kevin.betterhome.BetterHome;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static kevin.betterhome.utils.SharedHomeUtils.unlinkSharesOnDeleteAndNotify;

public class HomeUtils {
    private final BetterHome plugin;

    public HomeUtils(BetterHome plugin) {
        this.plugin = plugin;
    }
    public static class PendingHomeData {
        public String lastAttemptedName; // 舊家名稱
        public String newName; // 新名稱
        public long timestamp;

        // 原本的建構子 (兩個參數) → 給建立/覆蓋 Home 用
        public PendingHomeData(String lastAttemptedName, long timestamp) {
            this.lastAttemptedName = lastAttemptedName;
            this.timestamp = timestamp;
        }

        // 新的建構子 (三個參數) → 給改名流程用
        public PendingHomeData(String lastAttemptedName, String newName, long timestamp) {
            this.lastAttemptedName = lastAttemptedName;
            this.newName = newName;
            this.timestamp = timestamp;
        }
    }

    public static int getMaxHomesForPlayer(BetterHome plugin, Player player) {
        FileConfiguration config = plugin.getConfig();
        int defaultMaxHomes = config.getInt("default-max-homes", 3);
        final String prefix = "betterhome.maxhomes.";

        try {
            int max = defaultMaxHomes;
            for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
                if (!permissionInfo.getValue()) continue;
                String permission = permissionInfo.getPermission();
                if (permission == null || !permission.startsWith(prefix)) continue;

                String valuePart = permission.substring(prefix.length());
                if (valuePart.isEmpty()) continue;
                try {
                    int value = Integer.parseInt(valuePart);
                    if (value > max) {
                        max = value;
                    }
                } catch (NumberFormatException ignored) {
                    plugin.logDebug("Ignored invalid max-homes permission: " + permission);
                }
            }
            return max;
        } catch (Exception e) {
            plugin.getLogger().severe("無法讀取玩家最大家園權限: " + e.getMessage());
            e.printStackTrace();
        }

        return defaultMaxHomes;
    }

    public static int getCurrentHomeCount(BetterHome plugin, Player player) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        if (!playerFile.exists()) return 0;

        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        List<String> homes = playerConfig.getStringList("homes");
        return homes.size();
    }

    public static boolean hasHome(BetterHome plugin, Player player, String homeName) {
        File dataFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        if (!dataFile.exists()) {
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<String> homeList = config.getStringList("homes");
        return homeList.contains(homeName);
    }

    public static void saveHome(BetterHome plugin, Player player, String homeName) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        Location loc = player.getLocation();
        List<String> homes = playerConfig.getStringList("homes");

        if (!homes.contains(homeName)) {
            homes.add(homeName);
        }

        playerConfig.set("homes", homes);
        playerConfig.set(homeName + ".world", loc.getWorld().getName());
        playerConfig.set(homeName + ".x", loc.getX());
        playerConfig.set(homeName + ".y", loc.getY());
        playerConfig.set(homeName + ".z", loc.getZ());
        playerConfig.set(homeName + ".yaw", loc.getYaw());
        playerConfig.set(homeName + ".pitch", loc.getPitch());

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void teleportPlayerToHomeWithCooldown(BetterHome plugin, Player player, String homeName) {
        FileConfiguration config = plugin.getConfig();

        // 無冷卻權限
        if (player.hasPermission("betterhome.cooldown.bypass")) {
            teleportPlayerToHome(plugin, player, homeName);
            return;
        }

        // 冷卻檢查
        long now = System.currentTimeMillis();
        if (plugin.teleportCooldowns.containsKey(player)) {
            long end = plugin.teleportCooldowns.get(player);
            if (now < end) {
                int seconds = (int) Math.ceil((end - now) / 1000.0);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.teleport-in-progress", "&c你正在冷卻中，請等待 &e%seconds% 秒"))
                        .replace("%seconds%", String.valueOf(seconds)));
                return;
            }
        }

        long cooldownMillis = plugin.getTeleportCooldownTime();
        long endTime = now + cooldownMillis;
        int totalSeconds = (int) Math.ceil(cooldownMillis / 1000.0);

        plugin.teleportCooldowns.put(player, endTime);
        plugin.teleportingPlayers.add(player);

        // 清除前一任務
        ScheduledFuture<?> oldTask = plugin.cooldownTasks.remove(player);
        if (oldTask != null) oldTask.cancel(true);

        final int[] lastSentSecond = { -1 };

        ScheduledFuture<?> task = plugin.scheduler.scheduleAtFixedRate(() -> {
            long current = System.currentTimeMillis();
            long remaining = endTime - current;

            if (!plugin.teleportingPlayers.contains(player)) {
                plugin.teleportCooldowns.remove(player);
                plugin.cooldownTasks.remove(player);
                throw new CancellationException("Teleport cancelled");
            }

            if (remaining <= 0) {
                plugin.teleportingPlayers.remove(player);
                plugin.teleportCooldowns.remove(player);
                plugin.cooldownTasks.remove(player);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    SoundUtils.playTpTeleport(plugin, player);
                    teleportPlayerToHome(plugin, player, homeName);
                });
                throw new CancellationException("Teleport complete");
            }

            int secondsLeft = (int) Math.ceil(remaining / 1000.0);
            if (secondsLeft != lastSentSecond[0]) {
                lastSentSecond[0] = secondsLeft;

                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    if (secondsLeft == 1) {
                        SoundUtils.playTpFinalTick(plugin, player);
                    } else {
                        SoundUtils.playTpCountdown(plugin, player);
                    }

                    // 顯示訊息
                    String msg = ChatColor.translateAlternateColorCodes('&',
                                    config.getString("messages.teleport-cooldown", "&b傳送倒數：&e%seconds%秒"))
                            .replace("%seconds%", String.valueOf(secondsLeft));
                    player.sendMessage(msg);

                    // Title 顯示
                    if (config.getBoolean("titles.cooldown-title.enable")) {
                        String title = ChatColor.translateAlternateColorCodes('&',
                                config.getString("titles.cooldown-title.teleport-title"));
                        String subtitle = ChatColor.translateAlternateColorCodes('&',
                                config.getString("titles.cooldown-title.teleport-subtitle"));
                        player.sendTitle(
                                title.replace("%seconds%", String.valueOf(secondsLeft)),
                                subtitle.replace("%seconds%", String.valueOf(secondsLeft)),
                                5, 20, 5
                        );
                    }

                    // Action bar 顯示
                    if (config.getBoolean("action-bar.cooldown-title.enable")) {
                        String actionBar = ChatColor.translateAlternateColorCodes('&',
                                        config.getString("action-bar.cooldown-title.teleport-message", "傳送中..."))
                                .replace("%seconds%", String.valueOf(secondsLeft));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(actionBar));
                    }
                });
            }

        }, 0, 100, TimeUnit.MILLISECONDS); // 每 100ms 執行一次，但只在秒數變化時發送 title

        plugin.cooldownTasks.put(player, task);

        // 若沒啟用倒數 title，但啟用靜態 title（只出現一次）
        if (!config.getBoolean("titles.cooldown-title.enable") && config.getBoolean("titles.static-title.enable")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', config.getString("titles.static-title.teleport-title")),
                        ChatColor.translateAlternateColorCodes('&',
                                        config.getString("titles.static-title.teleport-subtitle"))
                                .replace("%seconds%", String.valueOf(totalSeconds)),
                        10, totalSeconds * 20, 10
                );
                if (config.getBoolean("action-bar.static-title.enable")) {
                    String actionBar = ChatColor.translateAlternateColorCodes('&',
                                    config.getString("action-bar.static-title.teleport-subtitle"))
                            .replace("%seconds%", String.valueOf(totalSeconds));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(actionBar));
                }
            });
        }

        // 關閉 GUI
        plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
    }

    public static void teleportPlayerToHome(BetterHome plugin, Player player, String homeName) {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String worldName = playerConfig.getString(homeName + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            SoundUtils.playTpCancel(plugin, player);
            String worldNotFound = plugin.getConfig().getString("messages.world-not-found");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', worldNotFound));
            plugin.teleportCooldowns.remove(player);
            plugin.teleportingPlayers.remove(player);
            return;
        }

        double x = playerConfig.getDouble(homeName + ".x");
        double y = playerConfig.getDouble(homeName + ".y");
        double z = playerConfig.getDouble(homeName + ".z");
        float yaw = (float) playerConfig.getDouble(homeName + ".yaw");
        float pitch = (float) playerConfig.getDouble(homeName + ".pitch");

        Location homeLocation = new Location(world, x, y, z, yaw, pitch);

        try {
            Essentials ess = plugin.getEssentials();
            if (ess != null) {
                com.earth2me.essentials.User essUser = ess.getUser(player);
                if (essUser != null) {
                    Location current = player.getLocation().clone();
                    essUser.setLastLocation(current);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[BetterHome] Failed to set Essentials lastLocation for " + player.getName() + ": " + t.getMessage());
            String backWarn = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.back-may-not-work"));
            player.sendMessage(backWarn);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = player.teleport(homeLocation);
            if (!success) {
                SoundUtils.playTpCancel(plugin, player);
                String worldNotFound = plugin.getConfig().getString("messages.world-not-found");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', worldNotFound));

                plugin.teleportCooldowns.remove(player);
                plugin.teleportingPlayers.remove(player);
                return;
            }

            SoundUtils.playTpComplete(plugin, player);
            String teleportedToHomePath = plugin.getConfig().getString("messages.teleported");
            String teleportMessage = ChatColor.translateAlternateColorCodes('&', teleportedToHomePath)
                    .replace("%home%", homeName);
            player.sendMessage(teleportMessage);

            // Title
            if (plugin.getConfig().getBoolean("titles.teleport-finish.enable")) {
                String title = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("titles.teleport-finish.title"));
                String subtitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("titles.teleport-finish.subtitle", ""));
                int fadeIn = plugin.getConfig().getInt("titles.teleport-finish.fade-in", 10);
                int stay = plugin.getConfig().getInt("titles.teleport-finish.stay", 40);
                int fadeOut = plugin.getConfig().getInt("titles.teleport-finish.fade-out", 10);

                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }


            if (plugin.getConfig().getBoolean("action-bar.teleport-finish.enable")) {
                String actionBarMsg = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("action-bar.teleport-finish.message"));
                int durationTicks = plugin.getConfig().getInt("action-bar.teleport-finish.duration-ticks", 40);

                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(actionBarMsg));

                if (durationTicks > 20) {
                    new BukkitRunnable() {
                        int ticksLeft = durationTicks - 20;

                        @Override
                        public void run() {
                            if (ticksLeft <= 0) {
                                cancel();
                                return;
                            }
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new net.md_5.bungee.api.chat.TextComponent(actionBarMsg));
                            ticksLeft -= 20;
                        }
                    }.runTaskTimer(plugin, 20L, 20L);
                }
            }

            plugin.teleportCooldowns.remove(player);
            plugin.teleportingPlayers.remove(player);
        });
    }

    public static void teleportAdminToPlayerHome(BetterHome plugin, Player admin, OfflinePlayer target, String homeName) {
        // 取得玩家的資料檔案（data/玩家UUID.yml）
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, target.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        // 檢查該家是否存在於玩家資料中
        if (!playerConfig.contains(homeName)) {
            String msg = plugin.getConfig().getString("messages.player-home-not-exist");
            if (msg != null) {
                // 傳送錯誤訊息給管理員，告知該家不存在
                msg = ChatColor.translateAlternateColorCodes('&', msg)
                        .replace("%home%", homeName)
                        .replace("%player%", target.getName());
                SoundUtils.playFail(plugin, admin);
                admin.sendMessage(msg);
            }
            return;
        }

        // 取得家座標與世界名稱
        String world = playerConfig.getString(homeName + ".world");
        double x = playerConfig.getDouble(homeName + ".x");
        double y = playerConfig.getDouble(homeName + ".y");
        double z = playerConfig.getDouble(homeName + ".z");
        float yaw = (float) playerConfig.getDouble(homeName + ".yaw");
        float pitch = (float) playerConfig.getDouble(homeName + ".pitch");

        // 檢查世界名稱是否為空
        if (world == null) {
            SoundUtils.playFail(plugin, admin);
            admin.sendMessage(ChatColor.translateAlternateColorCodes('&', playerConfig.getString("messages.invalid-location")));
            return;
        }

        // 取得 Bukkit 世界物件
        World targetWorld = Bukkit.getServer().getWorld(world);
        if (targetWorld == null) {
            // 如果世界不存在，通知管理員
            SoundUtils.playFail(plugin, admin);
            admin.sendMessage(ChatColor.translateAlternateColorCodes('&', playerConfig.getString("messages.world-not-exist")));
            return;
        }

        // 建立家位置 Location
        Location homeLocation = new Location(targetWorld, x, y, z, yaw, pitch);
        SoundUtils.playTpTeleport(plugin, admin);
        // 傳送管理員到該位置
        admin.teleport(homeLocation);

        // 從 config 讀取並格式化傳送成功訊息，替換 %home% 與 %player_home%
        String message = plugin.getConfig().getString("messages.teleported-other-home");
        message = ChatColor.translateAlternateColorCodes('&', message)
                .replace("%home%", homeName)
                .replace("%player_home%", target.getName());

        // 傳送訊息給管理員
        SoundUtils.playTpComplete(plugin, admin);
        admin.sendMessage(message);
    }

    public static OfflinePlayer getTargetFromItemMeta(BetterHome plugin, ItemMeta meta) {
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(plugin, "adminHomeTarget");

        // 檢查是否有存儲該 key
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            plugin.getLogger().warning("[BetterHome] 物品沒有 adminHomeTarget 資訊。");
            return null;
        }

        // 取得 UUID 字串
        String uuidString = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (uuidString == null) {
            plugin.getLogger().warning("[BetterHome] adminHomeTarget 為 null。");
            return null;
        }

        uuidString = uuidString.trim();
        if (uuidString.isEmpty()) {
            plugin.getLogger().warning("[BetterHome] adminHomeTarget 是空字串。");
            return null;
        }

        try {
            // 將字串轉成 UUID 並取得 OfflinePlayer
            UUID uuid = UUID.fromString(uuidString);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[BetterHome] 無效的 UUID: " + uuidString);
            return null;
        }
    }

    // HomeUtils.java（加在 class 內其他靜態方法旁）
    public static Material getHomeIcon(BetterHome plugin, Player player, String homeName) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        if (!playerFile.exists()) return null;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playerFile);
        String path = homeName + ".icon";
        String iconName = cfg.getString(path, null);
        if (iconName == null || iconName.isEmpty()) return null;
        try {
            return Material.valueOf(iconName);
        } catch (IllegalArgumentException e) {
            // 存了舊版/無效材質，忽略
            return null;
        }
    }

    public static void setHomeIcon(BetterHome plugin, Player player, String homeName, Material iconOrNull) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playerFile);
        String base = homeName;
        if (!cfg.contains(base)) return; // 家園不存在就不寫

        if (iconOrNull == null) {
            cfg.set(base + ".icon", null); // 清除圖示
        } else {
            cfg.set(base + ".icon", iconOrNull.name());
        }
        try {
            cfg.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[BetterHome] 無法儲存 icon：" + e.getMessage());
        }
    }

    // ===== 刪除家園（含：homes 清單移除、節點刪除、最愛清除、共享清理+通知）=====
    // Overload：離線/線上皆可—用 OfflinePlayer 進行刪除；同樣會：
    // 1) 刪除 owner 的 home 節點與 homes 清單
    // 2) 清除 favorite
    // 3) 針對所有被分享者移除 shared-in，並對在線者即時通知
    // 4) 若 owner 在線，也會收到「移除了 X 位共享對象」摘要
    public static boolean deleteHome(BetterHome plugin, OfflinePlayer owner, String homeName, @org.jetbrains.annotations.Nullable CommandSender notifier) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        File ownerFile = new File(dataDir, owner.getUniqueId() + ".yml");
        if (!ownerFile.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(ownerFile);
        List<String> homes = cfg.getStringList("homes");
        if (homes == null || !homes.contains(homeName)) return false;

        // 清共享 + 通知被分享者（在線）
        int removedCount = 0;
        try {
            removedCount = unlinkSharesOnDeleteAndNotify(plugin, owner.getUniqueId(),
                    owner.getName() != null ? owner.getName() : owner.getUniqueId().toString(),
                    homeName, cfg);
        } catch (Exception e) {
            plugin.getLogger().warning("[BetterHome] 清理共享資料時發生例外：" + e.getMessage());
        }

        // 從列表移除與刪 root 節點
        homes.remove(homeName);
        cfg.set("homes", homes);
        cfg.set(homeName, null);

        // favorite 若是它就清
        String fav = cfg.getString("favorite", null);
        if (fav != null && fav.equalsIgnoreCase(homeName)) {
            cfg.set("favorite", null);
        }

        try {
            cfg.save(ownerFile);

            // 擁有者在線 → 只送一種訊息：有共享則 due-delete-owner，否則 home-removed
            Player ownerOnline = owner.isOnline() ? owner.getPlayer() : null;
            if (ownerOnline != null) {
                String key = (removedCount > 0) ? "messages.due-delete-owner"
                        : "messages.home-removed";
                String raw = plugin.getConfig().getString(key);
                ownerOnline.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        raw.replace("%home%", homeName)
                                .replace("%count%", String.valueOf(removedCount))));
            }

            // optional：回覆觸發者（管理員/控制台）
            if (notifier != null) {
                String msg = plugin.getConfig().getString("messages.admin-home-removed");
                notifier.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msg.replace("%home%", homeName)
                                .replace("%player%", owner.getName() != null
                                        ? owner.getName()
                                        : owner.getUniqueId().toString())));
            }

            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("[BetterHome] 儲存玩家家園資料失敗：" + e.getMessage());
            if (notifier != null) {
                notifier.sendMessage(ChatColor.RED + "儲存資料時發生錯誤。");
            }
            return false;
        }
    }

    public static boolean renameHome(BetterHome plugin, Player player, String oldName, String newName) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        List<String> homes = playerConfig.getStringList("homes");
        if (homes == null) return false;

        if (!homes.contains(oldName) || homes.contains(newName)) {
            return false;
        }

        int idx = homes.indexOf(oldName);

        if (playerConfig.isConfigurationSection(oldName)) {
            for (String key : playerConfig.getConfigurationSection(oldName).getKeys(false)) {
                playerConfig.set(newName + "." + key, playerConfig.get(oldName + "." + key));
            }
        } else {
            playerConfig.set(newName + ".world", playerConfig.getString(oldName + ".world"));
            playerConfig.set(newName + ".x", playerConfig.getDouble(oldName + ".x"));
            playerConfig.set(newName + ".y", playerConfig.getDouble(oldName + ".y"));
            playerConfig.set(newName + ".z", playerConfig.getDouble(oldName + ".z"));
            playerConfig.set(newName + ".yaw", playerConfig.getDouble(oldName + ".yaw"));
            playerConfig.set(newName + ".pitch", playerConfig.getDouble(oldName + ".pitch"));

            if (playerConfig.contains(oldName + ".icon")) {
                playerConfig.set(newName + ".icon", playerConfig.getString(oldName + ".icon"));
            }
        }

        playerConfig.set(oldName, null);

        homes.set(idx, newName);
        playerConfig.set("homes", homes);

        String fav = playerConfig.getString("favorite", null);
        if (fav != null && fav.equalsIgnoreCase(oldName)) {
            playerConfig.set("favorite", newName);
        }

        try {
            playerConfig.save(playerFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 嘗試從標題解析目前頁碼（你的 gui-title 範本包含 %page% / %maxpages%）
     * 例如： "§9家園清單 §7(第 2 / 5 頁)" 或 "&9家園清單 - 2/5"
     * 這裡只簡單抓第一個出現的整數（-1 若失敗）
     */
    public static int parseCurrentPageFromTitle(String title, String rawTemplate) {
        // 粗略解析：找第一個連續數字
        int page = -1;
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
                // 允許多位數
            } else if (num.length() > 0) {
                break;
            }
        }
        if (num.length() > 0) {
            try {
                page = Integer.parseInt(num.toString()) - 1; // 顯示是 1-based，內部用 0-based
            } catch (NumberFormatException ignored) {}
        }
        page = 0;
        return page;
    }

    /** 取得玩家目前的最愛家園名稱（沒有則回傳 null） */
    public static String getFavoriteHome(BetterHome plugin, Player player) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        if (!playerFile.exists()) return null;
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        String fav = playerConfig.getString("favorite", null);
        return (fav != null && !fav.trim().isEmpty()) ? fav : null;
    }

    /** 設定最愛家園（會先檢查該家園是否存在）；成功回傳 true */
    public static boolean setFavoriteHome(BetterHome plugin, Player player, String homeName) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        List<String> homes = playerConfig.getStringList("homes");
        if (homes == null || !homes.contains(homeName)) return false;

        playerConfig.set("favorite", homeName);
        try {
            playerConfig.save(playerFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[BetterHome] 儲存最愛家園失敗: " + e.getMessage());
            return false;
        }
    }

    /** 清除最愛家園；成功回傳 true（即使原本就是 null 也算成功寫回） */
    public static boolean clearFavoriteHome(BetterHome plugin, Player player) {
        File playerFile = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        playerConfig.set("favorite", null);
        try {
            playerConfig.save(playerFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[BetterHome] 清除最愛家園失敗: " + e.getMessage());
            return false;
        }
    }

    /** 判斷某家園是否為最愛 */
    public static boolean isFavoriteHome(BetterHome plugin, Player player, String homeName) {
        String fav = getFavoriteHome(plugin, player);
        return fav != null && fav.equalsIgnoreCase(homeName);
    }

    /**
     * 便利方法：切換最愛家園。
     * - 若目前最愛不是 homeName → 設為最愛並回傳 true
     * - 若目前最愛就是 homeName → 取消最愛並回傳 false
     */
    public static boolean toggleFavoriteHome(BetterHome plugin, Player player, String homeName) {
        if (isFavoriteHome(plugin, player, homeName)) {
            clearFavoriteHome(plugin, player);
            return false;
        } else {
            setFavoriteHome(plugin, player, homeName);
            return true;
        }
    }

}
