package kevin.betterhome.utils;

import com.earth2me.essentials.Essentials;
import kevin.betterhome.BetterHome;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SharedHomeUtils {

    /** 取得玩家的資料檔 */
    private static File getPlayerFile(BetterHome plugin, UUID uuid) {
        return plugin.getPlayerDataFile(uuid);
    }

    /** 取得玩家 YAML 配置 */
    private static YamlConfiguration getPlayerConfig(BetterHome plugin, UUID uuid) {
        return YamlConfiguration.loadConfiguration(getPlayerFile(plugin, uuid));
    }

    /** 儲存 YAML */
    private static void savePlayerConfig(BetterHome plugin, UUID uuid, YamlConfiguration config) {
        try {
            config.save(getPlayerFile(plugin, uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    /** 擁有者是否有該家園（依照現有資料結構：homes 為清單 + 以家名為key的節點） */
    private static boolean ownerHasHome(YamlConfiguration ownerCfg, String homeName) {
        List<String> homes = ownerCfg.getStringList("homes");
        if (homes != null && homes.stream().anyMatch(h -> h.equalsIgnoreCase(homeName))) {
            return true;
        }
        // 兼容：若有人手動編檔沒更新清單，直接看節點是否存在
        return ownerCfg.contains(homeName + ".world");
    }

    /** 新增共享 (owner -> target) */
    public static boolean addShare(BetterHome plugin, Player owner, String homeName, Player target) {
        UUID ownerId = owner.getUniqueId();
        UUID targetId = target.getUniqueId();

        YamlConfiguration ownerCfg = getPlayerConfig(plugin, ownerId);
        YamlConfiguration targetCfg = getPlayerConfig(plugin, targetId);

        // 確認家園存在（依你的現有結構）
        if (!ownerHasHome(ownerCfg, homeName)) return false;

        // owner shared-out
        String outPath = "shared-homes.shared-out." + homeName + ".players";
        List<String> outList = ownerCfg.getStringList(outPath);
        if (!outList.contains(targetId.toString())) outList.add(targetId.toString());
        ownerCfg.set(outPath, outList);
        savePlayerConfig(plugin, ownerId, ownerCfg);

        // target shared-in
        targetCfg.set("shared-homes.shared-in." + ownerId + "." + homeName, true);
        savePlayerConfig(plugin, targetId, targetCfg);

        return true;
    }

    /** 移除共享 (owner -> target) */
    public static boolean removeShare(BetterHome plugin, Player owner, String homeName, Player target) {
        UUID ownerId = owner.getUniqueId();
        UUID targetId = target.getUniqueId();

        YamlConfiguration ownerCfg = getPlayerConfig(plugin, ownerId);
        YamlConfiguration targetCfg = getPlayerConfig(plugin, targetId);

        String outPath = "shared-homes.shared-out." + homeName + ".players";
        List<String> outList = ownerCfg.getStringList(outPath);
        outList.remove(targetId.toString());
        ownerCfg.set(outPath, outList);
        savePlayerConfig(plugin, ownerId, ownerCfg);

        targetCfg.set("shared-homes.shared-in." + ownerId + "." + homeName, null);
        savePlayerConfig(plugin, targetId, targetCfg);

        return true;
    }

    /** 列出 owner 分享出去的清單：homeName -> [UUID, ...] */
    public static Map<String, List<UUID>> getSharedOut(BetterHome plugin, UUID ownerId) {
        YamlConfiguration cfg = getPlayerConfig(plugin, ownerId);
        Map<String, List<UUID>> result = new HashMap<>();

        if (cfg.contains("shared-homes.shared-out")) {
            for (String home : Objects.requireNonNull(cfg.getConfigurationSection("shared-homes.shared-out")).getKeys(false)) {
                List<String> players = cfg.getStringList("shared-homes.shared-out." + home + ".players");
                List<UUID> uuids = new ArrayList<>();
                for (String s : players) {
                    try {
                        uuids.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }
                result.put(home, uuids);
            }
        }
        return result;
    }

    /** 列出 target 擁有的 shared-in 清單：ownerUUID -> [homeName, ...] */
    public static Map<UUID, List<String>> getSharedIn(BetterHome plugin, UUID targetId) {
        YamlConfiguration cfg = getPlayerConfig(plugin, targetId);
        Map<UUID, List<String>> result = new HashMap<>();
        if (cfg.contains("shared-homes.shared-in")) {
            for (String owner : Objects.requireNonNull(cfg.getConfigurationSection("shared-homes.shared-in")).getKeys(false)) {
                Set<String> homes = Objects.requireNonNull(cfg.getConfigurationSection("shared-homes.shared-in." + owner)).getKeys(false);
                result.put(UUID.fromString(owner), new ArrayList<>(homes));
            }
        }
        return result;
    }

    /** 取得：某個 owner 的某個家園，目前已分享給哪些玩家（UUID 清單） */
    public static List<UUID> getSharedPlayersForHome(BetterHome plugin, UUID ownerId, String homeName) {
        YamlConfiguration cfg = getPlayerConfig(plugin, ownerId);
        String outPath = "shared-homes.shared-out." + homeName + ".players";
        List<String> list = cfg.getStringList(outPath);
        List<UUID> uuids = new ArrayList<>();
        for (String s : list) {
            try {
                uuids.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        }
        return uuids;
    }

    /** 取得：某個 owner 的某個家園，已分享玩家的「名稱清單」（Tab 補全好用） */
    public static List<String> getSharedPlayerNamesForHome(BetterHome plugin, UUID ownerId, String homeName) {
        List<UUID> ids = getSharedPlayersForHome(plugin, ownerId, homeName);
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = (op.getName() != null) ? op.getName() : id.toString();
            names.add(name);
        }
        return names;
    }

    /** 便捷多載：傳 Player 作為 owner */
    public static boolean isSharedWith(BetterHome plugin, Player owner, String homeName, UUID targetUUID) {
        return isSharedWith(plugin, owner.getUniqueId(), homeName, targetUUID);
    }

    /** 檢查 owner 的家園是否已分享給 targetUUID */
    public static boolean isSharedWith(BetterHome plugin, UUID ownerId, String homeName, UUID targetUUID) {
        YamlConfiguration ownerCfg = getPlayerConfig(plugin, ownerId);
        String outPath = "shared-homes.shared-out." + homeName + ".players";
        List<String> list = ownerCfg.getStringList(outPath);
        return list != null && list.contains(targetUUID.toString());
    }


    /**
     * 刪除家園時，同步清理共享資訊並發通知：
     * - 擁有者: 移除 shared-homes.shared-out.<homeName> 整段
     * - 每個被分享對象: 移除 shared-homes.shared-in.<ownerUUID>.<homeName>
     * - 若對象在線：發「擁有者已刪除此家園，你不再擁有存取權」訊息
     * @return 實際被移除的共享人數
     */
    static int unlinkSharesOnDeleteAndNotify(BetterHome plugin,
                                             UUID ownerId,
                                             String ownerName,
                                             String homeName,
                                             YamlConfiguration ownerCfg) {
        String baseOut = "shared-homes.shared-out." + homeName;
        if (!ownerCfg.contains(baseOut)) {
            return 0;
        }

        int removed = 0;
        List<String> tgtList = ownerCfg.getStringList(baseOut + ".players");
        if (tgtList != null) {
            File dataDir = new File(plugin.getDataFolder(), "data");
            for (String uuidStr : tgtList) {
                try {
                    UUID tgt = UUID.fromString(uuidStr);
                    File tgtFile = new File(dataDir, tgt + ".yml");
                    if (!tgtFile.exists()) {
                        removed++; // 檔案不在也算一個清理對象
                        continue;
                    }
                    YamlConfiguration tcfg = YamlConfiguration.loadConfiguration(tgtFile);

                    String inPath = "shared-homes.shared-in." + ownerId + "." + homeName;
                    if (tcfg.contains(inPath)) {
                        tcfg.set(inPath, null);
                        try {
                            tcfg.save(tgtFile);
                        } catch (IOException e) {
                            plugin.getLogger().warning("[BetterHome] 儲存共享對象資料失敗：" + tgt + " - " + e.getMessage());
                        }
                        removed++;
                    }

                    // 對線上目標發通知
                    Player targetOnline = Bukkit.getPlayer(tgt);
                    if (targetOnline != null) {
                        String raw = plugin.getConfig().getString(
                                "messages.due-delete-target"
                        );
                        targetOnline.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                raw.replace("%owner%", ownerName != null ? ownerName : ownerId.toString())
                                        .replace("%home%", homeName)));
                    }
                } catch (IllegalArgumentException ignore) {
                    // 無效 UUID 字串，略過但不影響流程
                }
            }
        }

        // 移除擁有者的 shared-out/<homeName> 節點
        ownerCfg.set(baseOut, null);
        return removed;
    }

    /** 傳送玩家到共享家園（依你的資料結構：<homeName>.* 在根層） */
    public static void teleportPlayerToSharedHomeWithCooldown(BetterHome plugin, Player player, UUID ownerId, String homeName) {
        FileConfiguration config = plugin.getConfig();

        // 無冷卻權限
        if (player.hasPermission("betterhome.cooldown.bypass")) {
            teleportPlayerToSharedHome(plugin, player, ownerId, homeName);
            return;
        }

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

        // 清掉前一個冷卻任務
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
                    teleportPlayerToSharedHome(plugin, player, ownerId, homeName);
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

                    // Chat 提示
                    String msg = ChatColor.translateAlternateColorCodes('&',
                                    config.getString("messages.teleport-cooldown", "&b傳送倒數：&e%seconds%秒"))
                            .replace("%seconds%", String.valueOf(secondsLeft));
                    player.sendMessage(msg);

                    // Title 提示
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

                    // Action bar 提示
                    if (config.getBoolean("action-bar.cooldown-title.enable")) {
                        String actionBar = ChatColor.translateAlternateColorCodes('&',
                                        config.getString("action-bar.cooldown-title.teleport-message", "傳送中..."))
                                .replace("%seconds%", String.valueOf(secondsLeft));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(actionBar));
                    }
                });
            }

        }, 0, 100, TimeUnit.MILLISECONDS);

        plugin.cooldownTasks.put(player, task);

        // 若沒啟用倒數 title，但啟用靜態 title
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
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(actionBar));
                }
            });
        }

        // 關閉 GUI
        plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
    }

    public static void teleportPlayerToSharedHome(BetterHome plugin, Player player, UUID ownerId, String homeName) {
        File ownerFile = plugin.getPlayerDataFile(ownerId);
        YamlConfiguration ownerConfig = YamlConfiguration.loadConfiguration(ownerFile);
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        String ownerName = owner.getName() != null ? owner.getName() : "未知玩家";

        if (!ownerConfig.contains(homeName)) {
            SoundUtils.playTpCancel(plugin, player);
            player.sendMessage("§c該共享家園不存在！");
            plugin.teleportCooldowns.remove(player);
            plugin.teleportingPlayers.remove(player);
            return;
        }

        String worldName = ownerConfig.getString(homeName + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            SoundUtils.playTpCancel(plugin, player);
            String worldNotFound = plugin.getConfig().getString("messages.world-not-found");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', worldNotFound));
            plugin.teleportCooldowns.remove(player);
            plugin.teleportingPlayers.remove(player);
            return;
        }

        double x = ownerConfig.getDouble(homeName + ".x");
        double y = ownerConfig.getDouble(homeName + ".y");
        double z = ownerConfig.getDouble(homeName + ".z");
        float yaw = (float) ownerConfig.getDouble(homeName + ".yaw");
        float pitch = (float) ownerConfig.getDouble(homeName + ".pitch");

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
            SoundUtils.playError(plugin, player);
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
            String teleportedToHomePath = plugin.getConfig().getString("messages.shared-go-teleported", "&a已傳送至共享家園 &f%home%");
            String teleportMessage = ChatColor.translateAlternateColorCodes('&', teleportedToHomePath)
                    .replace("%owner%", ownerName)
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

            // ActionBar
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
}
