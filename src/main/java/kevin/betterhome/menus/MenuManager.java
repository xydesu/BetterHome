package kevin.betterhome.menus;
import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.holders.AdminMenu;
import kevin.betterhome.menus.holders.PlayerMenu;
import kevin.betterhome.menus.ui.*;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.HomeUtils.PendingHomeData;
import kevin.betterhome.utils.SharedHomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static org.apache.logging.log4j.LogManager.getLogger;

public class MenuManager implements Listener {

    private final BetterHome plugin;
    public static boolean isAdmin = false;


    public MenuManager(BetterHome plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

    /*Menu menu = new Menu(plugin);
    // 取得執行指令的玩家
    Player player = event.getPlayer();

    // 讀取設定檔
    FileConfiguration config = plugin.getConfig();
    String path1 = "menu.open-command";

    // 如果玩家沒有權限使用 betterhome.use，傳送無權限訊息並停止
    if (!player.hasPermission("betterhome.use")) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.no-permissions", "&c你沒有權限執行此指令。")));
        return;
    }

    // 檢查玩家輸入的指令是否與設定檔中指定的指令相符
    if (event.getMessage().equalsIgnoreCase(config.getString(path1))) {
        menu.openMainMenu(player);
    }*/
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        String itemName = meta.hasDisplayName() ? meta.getDisplayName() : "";

        // ===== Keys =====
        NamespacedKey pageKey        = new NamespacedKey(plugin, "menuPage");
        NamespacedKey homeKey        = new NamespacedKey(plugin, "homePosition");
        NamespacedKey deleteKey      = new NamespacedKey(plugin, "deleteHome");
        NamespacedKey guildHomeKey   = new NamespacedKey(plugin, "guildHome");

        // Icon 選單
        NamespacedKey iconPickKey    = new NamespacedKey(plugin, "iconPick");
        NamespacedKey iconHomeKey    = new NamespacedKey(plugin, "iconHome");
        NamespacedKey iconPageKey    = new NamespacedKey(plugin, "iconPage");
        NamespacedKey iconBackKey    = new NamespacedKey(plugin, "iconBack");

        // ManageSharesMenu keys
        NamespacedKey SharedHomesKey = new NamespacedKey(plugin, "openSharedHomes");
        NamespacedKey sharedBackKey  = new NamespacedKey(plugin, "sharedBack");
        NamespacedKey ManageSharesKey= new NamespacedKey(plugin, "openManageShares");
        NamespacedKey manageHomeKey  = new NamespacedKey(plugin, ManageShares.KEY_MANAGE_HOME);
        NamespacedKey sharedTgtKey   = new NamespacedKey(plugin, ManageShares.KEY_SHARED_TGT);
        NamespacedKey addShareKey    = new NamespacedKey(plugin, ManageShares.KEY_ADD_SHARE);
        NamespacedKey backKey        = new NamespacedKey(plugin, ManageShares.KEY_BACK);

        // ====== Shared Homes 點擊頭顱傳送 ======
        NamespacedKey sharedOwnerKey = new NamespacedKey(plugin, "sharedOwner");
        NamespacedKey sharedHomeKey  = new NamespacedKey(plugin, "sharedHome");

        // ====== 先處理 Icon 選單 ======
        if (meta.getPersistentDataContainer().has(iconPickKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(iconPageKey, PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(iconBackKey, PersistentDataType.INTEGER)) {

            event.setCancelled(true);

            if (meta.getPersistentDataContainer().has(iconPageKey, PersistentDataType.INTEGER)) {
                int nextPage = meta.getPersistentDataContainer().get(iconPageKey, PersistentDataType.INTEGER);
                String hn = meta.getPersistentDataContainer().get(iconHomeKey, PersistentDataType.STRING);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SoundUtils.playUiNext(plugin, player);
                    IconPicker.open(plugin, player, hn, nextPage);
                }, 1L);
                return;
            }

            if (meta.getPersistentDataContainer().has(iconBackKey, PersistentDataType.INTEGER)) {
                int currentPage = HomeUtils.parseCurrentPageFromTitle(
                        player.getOpenInventory().getTitle(),
                        plugin.getConfig().getString("homes-menu.gui-title"));
                final int reopenPage = Math.max(0, currentPage);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SoundUtils.playUiBack(plugin, player);
                    HomesMenu.open(plugin, player, reopenPage);
                }, 1L);
                return;
            }

            if (meta.getPersistentDataContainer().has(iconPickKey, PersistentDataType.STRING)) {
                String matName  = meta.getPersistentDataContainer().get(iconPickKey,  PersistentDataType.STRING);
                String homeName = meta.getPersistentDataContainer().get(iconHomeKey,  PersistentDataType.STRING);
                SoundUtils.playUiPickIcon(plugin, player);
                try {
                    Material chosen = Material.valueOf(matName);
                    HomeUtils.setHomeIcon(plugin, player, homeName, chosen);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    plugin.getConfig().getString("messages.icon-set", "&7[&bBetterHome&7] &a已更新 %home% 的圖示：&f%icon%"))
                            .replace("%home%", homeName)
                            .replace("%icon%", chosen.name()));
                } catch (IllegalArgumentException ignored) {}

                int currentPage = HomeUtils.parseCurrentPageFromTitle(
                        player.getOpenInventory().getTitle(),
                        plugin.getConfig().getString("homes-menu.gui-title"));
                final int reopenPage = Math.max(0, currentPage);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> HomesMenu.open(plugin, player, reopenPage), 1L);
            }
            return;
        }

        // ====== (A) 在 Homes 選單，Q(丟棄) 開啟共享管理 ======
        if (meta.getPersistentDataContainer().has(homeKey, PersistentDataType.STRING)
                && event.getClick() == ClickType.DROP) {
            event.setCancelled(true);

            String homeName = meta.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING);
            if (homeName == null || homeName.isEmpty()) return;

            // 只允許對已存在的家開啟
            if (!HomeUtils.hasHome(plugin, player, homeName)) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage("§c該家園尚未設置，無法管理共享。");
                return;
            }

            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiOpen(plugin, player);
                ManageShares.open(plugin, player, homeName);
            }, 1L);

            return;
        }

        // ====== (B) ManageSharesMenu 內：新增共享按鈕 ======
        if (meta.getPersistentDataContainer().has(addShareKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String homeName = meta.getPersistentDataContainer().get(addShareKey, PersistentDataType.STRING);
            if (homeName == null || homeName.isEmpty()) return;

            player.closeInventory();
            plugin.pendingShareAdds.put(player, homeName);
            SoundUtils.playUiPrompt(plugin, player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.enter-shares-player-name")));
            return;
        }

        // ====== (C) ManageSharesMenu 內：右鍵頭顱移除共享 ======
        if (meta.getPersistentDataContainer().has(sharedTgtKey, PersistentDataType.STRING)
                && meta.getPersistentDataContainer().has(manageHomeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            // 只處理右鍵
            if (event.getClick() != ClickType.RIGHT) return;

            String homeName = meta.getPersistentDataContainer().get(manageHomeKey, PersistentDataType.STRING);
            String tgtUuidStr = meta.getPersistentDataContainer().get(sharedTgtKey,  PersistentDataType.STRING);
            if (homeName == null || tgtUuidStr == null) return;

            UUID tgtUuid;
            try { tgtUuid = UUID.fromString(tgtUuidStr); }
            catch (IllegalArgumentException ex) {
                SoundUtils.playError(plugin, player);
                player.sendMessage("§cUUID 解析失敗。");
                return;
            }

            Player target = Bukkit.getPlayer(tgtUuid);
            if (target == null) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage("§cThe player is not connected.");
                return;
            }

            // 執行移除
            boolean ok = SharedHomeUtils.removeShare(plugin, player, homeName, target);
            if (ok) {
                // 讀取訊息模板
                String ownerMsg = ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfig().getString("messages.unshare-success", "&a你已取消將 &f%home% &a分享給 &f%player%"))
                        .replace("%home%", homeName)
                        .replace("%player%", target.getName());

                String targetMsg = ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfig().getString("messages.unshare-received", "&c玩家 &f%owner% &c已取消與你共享家園 &f%home%"))
                        .replace("%owner%", player.getName())
                        .replace("%home%", homeName);

                // 通知雙方
                SoundUtils.playUiShareRemove(plugin, player);
                player.sendMessage(ownerMsg);
                SoundUtils.playUiShareRemove(plugin, target);
                target.sendMessage(targetMsg);
            } else {
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.unshare-failed")));
            }
            // 重開管理選單
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    ManageShares.open(plugin, player, homeName), 1L);
            return;
        }

        // ====== (D) ManageSharesMenu 內：返回 ======
        if (meta.getPersistentDataContainer().has(backKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            // 回到 Homes 選單；嘗試回到目前頁數（解析不到就 0）
            int currentPage = HomeUtils.parseCurrentPageFromTitle(
                    player.getOpenInventory().getTitle(),
                    plugin.getConfig().getString("homes-menu.gui-title"));
            final int reopenPage = Math.max(0, currentPage);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiBack(plugin, player);
                HomesMenu.open(plugin, player, reopenPage);
            }, 1L);
            return;
        }

        // ====== 主選單 ======
        String mainMenuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("menu.gui-title"));
        String confirmationMenuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("confirmation-menu.gui-title"));

        if (event.getView().getTitle().equals(mainMenuTitle)) {
            event.setCancelled(true);

            String setHomeItemName = ChatColor.translateAlternateColorCodes('&', config.getString("menu.set-home-item.display-name"));
            String yourHomesItemName = ChatColor.translateAlternateColorCodes('&', config.getString("menu.your-homes-item.display-name"));
            String adminItemName = ChatColor.translateAlternateColorCodes('&', config.getString("admin-menu.display-name"));

            if (itemName.equals(adminItemName) && player.hasPermission("betterhome.admin")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SoundUtils.playUiOpen(plugin, player);
                    AdminList.open(player, 0);
                }, 1L);
                return;
            }

            if (itemName.equals(setHomeItemName)) {
                String currentWorld = player.getWorld().getName();
                List<String> blacklistedWorlds = config.getStringList("blacklisted-worlds");
                File dataFolder = new File(plugin.getDataFolder(), "data");
                File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
                YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                List<String> homes = playerConfig.getStringList("homes");
                int maxHomes = HomeUtils.getMaxHomesForPlayer(plugin, player);

                if (blacklistedWorlds.contains(currentWorld)) {
                    String bypassPermission = "betterhome.world.bypass." + currentWorld;
                    if (!player.hasPermission(bypassPermission)) {
                        SoundUtils.playFail(plugin, player);
                        String errorMessage = config.getString("messages.error-blacklisted-world");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                        return;
                    }
                }

                if (homes.size() >= maxHomes) {
                    SoundUtils.playFail(plugin, player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.home-limit-reached")
                                    .replace("%limit%", String.valueOf(maxHomes))));
                    return;
                }

                plugin.pendingHomeNames.put(player, new HomeUtils.PendingHomeData("", System.currentTimeMillis()));
                SoundUtils.playUiPrompt(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.enter-home-name")));
                player.closeInventory();
                return;
            }

            if (itemName.equals(yourHomesItemName)) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SoundUtils.playUiOpen(plugin, player);
                    HomesMenu.open(plugin, player, 0);
                }, 1L);
                return;
            }
        }

        // ====== 家園清單 & 共享家園 ======
        boolean isOurHomesGui = (inventory.getHolder() instanceof PlayerMenu)
                || meta.getPersistentDataContainer().has(pageKey,   PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(homeKey,   PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(deleteKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(guildHomeKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(manageHomeKey, PersistentDataType.STRING);

        if (!isOurHomesGui) return;

        event.setCancelled(true);

        // 翻頁
        if (meta.getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER)) {
            int newPage = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            SoundUtils.playUiNavigate(plugin, player);
            HomesMenu.open(plugin, player, newPage);
            return;
        }

        // 關閉
        if (itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("homes-menu.close-item")))) {
            SoundUtils.playUiClose(plugin, player);
            player.closeInventory();
            return;
        }

        // Guild 家園
        if (meta.getPersistentDataContainer().has(guildHomeKey, PersistentDataType.STRING)) {
            String value = meta.getPersistentDataContainer().get(guildHomeKey, PersistentDataType.STRING);
            if ("teleport".equals(value)) {
                player.closeInventory();
                try {
                    me.glaremasters.guilds.guild.Guild guild = kevin.betterhome.integration.guilds.GuildsManager.getGuild(player);
                    if (guild != null && guild.getHome() != null) {
                        Location guildHome = guild.getHome().getAsLocation();
                        SoundUtils.playTpTeleport(plugin, player);
                        player.teleport(guildHome);
                        SoundUtils.playTpComplete(plugin, player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.teleporting-to-guild-home")));
                    } else {
                        SoundUtils.playFail(plugin, player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.guild-home-not-set")));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("傳送到工會家園時出錯: " + e.getMessage());
                    SoundUtils.playError(plugin, player);
                    player.sendMessage(ChatColor.RED + "無法傳送到工會家園。請聯繫管理員。");
                }
                return;
            }
        }

        // ====== Shared Homes 入口 ======
        if (meta.getPersistentDataContainer().has(SharedHomesKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiOpen(plugin, player);
                SharedHomes.open(plugin, player, 0); // 打開共享家園清單
            }, 1L);
            return;
        }

        if (meta.getPersistentDataContainer().has(sharedOwnerKey, PersistentDataType.STRING)
                && meta.getPersistentDataContainer().has(sharedHomeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            if (event.getClick() == ClickType.LEFT) {
                String ownerUuidStr = meta.getPersistentDataContainer().get(sharedOwnerKey, PersistentDataType.STRING);
                String homeName     = meta.getPersistentDataContainer().get(sharedHomeKey,  PersistentDataType.STRING);

                if (ownerUuidStr != null && homeName != null) {
                    try {
                        UUID ownerUuid = UUID.fromString(ownerUuidStr);
                        SharedHomeUtils.teleportPlayerToSharedHomeWithCooldown(plugin, player, ownerUuid, homeName);
                        player.closeInventory();
                    } catch (IllegalArgumentException ex) {
                        SoundUtils.playError(plugin, player);
                        player.sendMessage("§c共享家園資訊錯誤，無法傳送。");
                    }
                }
            }
            return;
        }

        // ====== Shared Homes 返回家園 ======
        if (meta.getPersistentDataContainer().has(sharedBackKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiBack(plugin, player);
                HomesMenu.open(plugin, player, 0); // 打開家園清單
            }, 1L);
            return;
        }

        // ====== Manage Shares 入口 ======
        if (meta.getPersistentDataContainer().has(ManageSharesKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiOpen(plugin, player);
                ManageShares.open(plugin, player, null); // 打開管理介面
            }, 1L);
            return;
        }

        // 刪除確認
        if (meta.getPersistentDataContainer().has(deleteKey, PersistentDataType.STRING)
                && event.getClick() == ClickType.RIGHT) {
            String homeName = meta.getPersistentDataContainer().get(deleteKey, PersistentDataType.STRING);
            if (homeName != null && !homeName.isEmpty()) {
                plugin.pendingHomeNames.put(player, new HomeUtils.PendingHomeData(homeName, System.currentTimeMillis()));
                SoundUtils.playUiOpen(plugin, player);
                ConfirmationMenu.open(plugin, player, homeName);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // 點擊家園
        if (meta.getPersistentDataContainer().has(homeKey, PersistentDataType.STRING)) {
            String homeName = meta.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING);
            if (homeName == null || homeName.isEmpty()) return;

            boolean exists = HomeUtils.hasHome((BetterHome) plugin, player, homeName);

            if (!exists) {
                if (event.getClick() == ClickType.LEFT) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.chat("/home create " + homeName), 1L);
                }
                return;
            }

            switch (event.getClick()) {
                case LEFT:
                    HomeUtils.teleportPlayerToHomeWithCooldown(plugin, player, homeName);
                    return;
                case RIGHT:
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SoundUtils.playUiOpen(plugin, player);
                        IconPicker.open(plugin, player, homeName, 0);
                    }, 1L);
                    return;
                case SHIFT_LEFT:
                    player.closeInventory();
                    plugin.pendingRenameHomes.put(player,
                            new HomeUtils.PendingHomeData(homeName, System.currentTimeMillis()));
                    SoundUtils.playUiPrompt(plugin, player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.enter-new-home-name", "&e請輸入新的家園名稱 (輸入 cancel 取消)")));
                    return;
                case SHIFT_RIGHT:
                    boolean nowFavorite = HomeUtils.toggleFavoriteHome((BetterHome) plugin, player, homeName);
                    String key = nowFavorite ? "messages.favorite-set" : "messages.favorite-unset";
                    String raw = plugin.getConfig().getString(key, "&c[BetterHome] 訊息缺失：" + key);
                    String msg = ChatColor.translateAlternateColorCodes('&', raw.replace("%home%", homeName));
                    player.sendMessage(msg);

                    int currentPage = HomeUtils.parseCurrentPageFromTitle(
                            event.getView().getTitle(),
                            plugin.getConfig().getString("homes-menu.gui-title"));
                    final int reopenPage = Math.max(0, currentPage);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SoundUtils.playUiOpen(plugin, player);
                        HomesMenu.open(plugin, player, reopenPage);
                    }, 1L);
                    return;
                default:
                    return;
            }
        }

        // ====== 確認刪除選單 ======
        if (event.getView().getTitle().equals(confirmationMenuTitle)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (!plugin.pendingHomeNames.containsKey(player)) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.no-pending-home")));
                player.closeInventory();
                return;
            }

            String homeName = plugin.pendingHomeNames.get(player).lastAttemptedName;
            if (homeName == null || homeName.trim().isEmpty()) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-not-found")
                                .replace("%home%", homeName)));
                player.closeInventory();
                return;
            }

            if (slot == 11) {
                // 處理刪除、最愛清理、共享清理與雙方通知
                boolean ok = HomeUtils.deleteHome(plugin, player, homeName, null);

                if (!ok) {
                    SoundUtils.playFail(plugin, player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.home-not-found")
                                    .replace("%home%", homeName)));
                }

                SoundUtils.playUiConfirm(plugin, player);
                plugin.pendingHomeNames.remove(player);
                player.closeInventory();

            } else if (slot == 15) {
                SoundUtils.playUiCancel(plugin, player);
                plugin.pendingHomeNames.remove(player);
                player.closeInventory();
            }

        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.teleportingPlayers.contains(player)) {
            FileConfiguration config = plugin.getConfig();
            if (!config.getBoolean("cancel-on-move")) return;

            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getBlockX() != to.getBlockX()
                    || from.getBlockY() != to.getBlockY()
                    || from.getBlockZ() != to.getBlockZ()) {

                plugin.teleportingPlayers.remove(player);
                plugin.teleportCooldowns.remove(player);

                // 取消冷卻倒數任務
                ScheduledFuture<?> task = plugin.cooldownTasks.remove(player);
                if (task != null) {
                    task.cancel(true);
                }

                player.resetTitle();
                SoundUtils.playTpCancel(plugin, player);
                String msg = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.teleport-cancelled", "&c你移動了，傳送已取消！"));
                SoundUtils.playTpCancel(plugin, player);
                player.sendMessage(msg);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();
        String message = event.getMessage().trim();

        // ===== 新增共享流程 =====
        if (plugin.pendingShareAdds.containsKey(player)) {
            event.setCancelled(true);

            String homeName = plugin.pendingShareAdds.get(player);

            if (message.equalsIgnoreCase("cancel")) {
                plugin.pendingShareAdds.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-cancelled")));
                return;
            }

            // 只能加線上玩家
            Player target = Bukkit.getPlayerExact(message);
            if (target == null) {
                plugin.pendingShareAdds.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.player-not-found")
                                .replace("%player%", target.getName())));
                return;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                plugin.pendingShareAdds.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.share-self-error")));
                return;
            }

            // 是否已分享過
            if (SharedHomeUtils.isSharedWith(plugin, player, homeName, target.getUniqueId())) {
                plugin.pendingShareAdds.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.share-already")
                        .replace("%player%", target.getName())
                        .replace("%home%", homeName)));
                return;
            }

            if (SharedHomeUtils.addShare(plugin, player, homeName, target)) {
                SoundUtils.playUiShareAdd(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.share-success")
                        .replace("%home%", homeName)
                        .replace("%player%", target.getName())));
                SoundUtils.playUiShareAdd(plugin, target);
                target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.share-received")
                        .replace("%owner%", player.getName())
                        .replace("%home%", homeName)));
            } else {
                // 萬一底層保存失敗或其他原因
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.share-failed")));
            }

            plugin.pendingShareAdds.remove(player);

            // 重開管理選單
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    ManageShares.open(plugin, player, homeName), 1L);
            return;
        }

        // ===== 改名流程（單次輸入 + 30秒時限）=====
        if (plugin.pendingRenameHomes.containsKey(player)) {
            event.setCancelled(true);
            HomeUtils.PendingHomeData pending = plugin.pendingRenameHomes.get(player);

            // 判斷是否超時
            long now = System.currentTimeMillis();
            if (now - pending.timestamp > 30_000) {
                plugin.pendingRenameHomes.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.rename-timeout", "&c改名已逾時，請重新操作")));
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                plugin.pendingRenameHomes.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.rename-cancelled", "&c已取消改名")));
                return;
            }

            String oldName = pending.lastAttemptedName;

            File dataFolder = new File(plugin.getDataFolder(), "data");
            File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
            YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
            List<String> homes = playerConfig.getStringList("homes");

            if (!homes.contains(oldName)) {
                plugin.pendingRenameHomes.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-not-found")
                                .replace("%home%", oldName)));
                return;
            }

            // 與原名稱相同
            if (message.equalsIgnoreCase(oldName)) {
                plugin.pendingRenameHomes.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.rename-same-as-old", "&c新名稱不能與原名稱相同。")));
                return;
            }

            // 與其他家重複
            if (homes.stream().anyMatch(h -> h.equalsIgnoreCase(message))) {
                plugin.pendingRenameHomes.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.rename-exists", "&c已有相同名稱的家")));
                return;
            }

            // 改名
            if (HomeUtils.renameHome(plugin, player, oldName, message)) {
                SoundUtils.playSuccess(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.rename-success", "&a已將家 &e%old% &a改名為 &e%new%"))
                        .replace("%old%", oldName)
                        .replace("%new%", message));
            } else {
                SoundUtils.playError(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.saving-error")));
            }

            plugin.pendingRenameHomes.remove(player);
            return;
        }


        // ===== 建立 / 覆蓋 Home 流程 =====
        if (plugin.pendingHomeNames.containsKey(player)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                plugin.pendingHomeNames.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-cancelled")));
                return;
            }

            String homeName = message;

            File dataFolder = new File(plugin.getDataFolder(), "data");
            File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
            YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            List<String> homes = playerConfig.getStringList("homes");
            int maxHomes = HomeUtils.getMaxHomesForPlayer(plugin, player);
            long now = System.currentTimeMillis();

            PendingHomeData pending = plugin.pendingHomeNames.get(player);

            if (homes.contains(homeName)) {
                // 名稱已存在
                if (pending.lastAttemptedName == null || pending.lastAttemptedName.isEmpty()) {
                    // 第一次輸入這個已存在的名稱 → 提示再次輸入以覆蓋
                    SoundUtils.playFail(plugin, player);
                    plugin.pendingHomeNames.put(player, new PendingHomeData(homeName, now));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.home-exists-retry")));

                    // 啟動逾時任務
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        PendingHomeData stillPending = plugin.pendingHomeNames.get(player);
                        if (stillPending != null && stillPending.lastAttemptedName.equalsIgnoreCase(homeName)) {
                            plugin.pendingHomeNames.remove(player);
                            SoundUtils.playFail(plugin, player);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    config.getString("messages.home-timeout")));
                        }
                    }, 20L * 30);
                    return;
                }

                if (pending.lastAttemptedName.equalsIgnoreCase(homeName)) {
                    if (now - pending.timestamp <= 30_000) {
                        HomeUtils.saveHome(plugin, player, homeName);
                        SoundUtils.playSuccess(plugin, player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.home-overwritten")
                                        .replace("%home%", homeName)));
                    } else {
                        SoundUtils.playFail(plugin, player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.home-timeout")));
                    }
                } else {
                    SoundUtils.playFail(plugin, player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.home-mismatch-cancel")));
                }

                plugin.pendingHomeNames.remove(player);
                return;
            }

            if (pending.lastAttemptedName != null && !pending.lastAttemptedName.isEmpty()) {
                plugin.pendingHomeNames.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-mismatch-cancel")));
                return;
            }

            if (homes.size() >= maxHomes) {
                plugin.pendingHomeNames.remove(player);
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-limit-reached")
                                .replace("%limit%", String.valueOf(maxHomes))));
                return;
            }

            HomeUtils.saveHome(plugin, player, homeName);
            SoundUtils.playSuccess(plugin, player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.home-established")).replace("%home%", homeName));
            plugin.pendingHomeNames.remove(player);
        }
    }

    @EventHandler
    public void onAdminInventoryClick(InventoryClickEvent event) {

        // 只處理持有者為 AdminMenuHolder 的介面點擊事件
        if (!(event.getInventory().getHolder() instanceof AdminMenu)) return;

        FileConfiguration config = plugin.getConfig();
        String adminConfirmationTitle = ChatColor.translateAlternateColorCodes('&', config.getString("confirmation-menu-admin.gui-title"));

        // 取消預設點擊事件（避免玩家拿走物品）
        event.setCancelled(true);
        Player admin = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        NamespacedKey pageKey = new NamespacedKey(plugin, "adminHomesPage");

        // ----- 點擊玩家頭像：打開該玩家的家列表 -----
        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            OfflinePlayer target = HomeUtils.getTargetFromItemMeta(plugin, meta);
            if (target == null) {
                SoundUtils.playError(plugin, admin);
                admin.sendMessage(ChatColor.RED + "錯誤：無法取得玩家資料。");
                return;
            }
            SoundUtils.playUiOpen(plugin, admin);
            AdminPlayerHomes.open(plugin, admin, target, 0); // 從第 0 頁開始
            return;
        }

        // ----- 點擊分頁按鈕：換頁 -----
        if (meta.getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER)) {
            int newPage = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            OfflinePlayer target = HomeUtils.getTargetFromItemMeta(plugin, meta);
            if (target == null) {
                SoundUtils.playError(plugin, admin);
                admin.sendMessage(ChatColor.RED + "錯誤：無法取得玩家資料。");
                return;
            }
            SoundUtils.playUiOpen(plugin, admin);
            AdminPlayerHomes.open(plugin, admin, target, newPage);
            return;
        }

        // ----- 點擊返回或關閉按鈕 -----
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String backItem = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("homes-menu.go-back-item")));
        String closeItem = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("homes-menu.close-item")));

        if (displayName.equalsIgnoreCase(backItem)) {
            admin.closeInventory();
            // 延遲一刻執行打開主選單，避免異常
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SoundUtils.playUiBack(plugin, admin);
                MainMenu.open(plugin, admin);
            }, 1L);
            return;
        }

        if (displayName.equalsIgnoreCase(closeItem)) {
            SoundUtils.playUiClose(plugin, admin);
            admin.closeInventory();
            return;
        }

        // ----- 左鍵點擊紅床：管理員傳送至玩家家 -----
        if (event.getClick() == ClickType.LEFT && clickedItem.getType() == Material.LIGHT_BLUE_BED) {
            String homeName = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "adminHomeName"), PersistentDataType.STRING);
            if (homeName == null) return;

            OfflinePlayer target =  HomeUtils.getTargetFromItemMeta(plugin, meta);
            if (target == null) {
                SoundUtils.playError(plugin, admin);
                admin.sendMessage(ChatColor.RED + "錯誤：無法取得玩家資料。");
                return;
            }

            HomeUtils.teleportAdminToPlayerHome(plugin, admin, target, homeName);
            return;
        }

        // ----- 右鍵點擊紅床：打開刪除確認選單 -----
        if (event.getClick() == ClickType.RIGHT && clickedItem.getType() == Material.LIGHT_BLUE_BED) {
            String homeName = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "adminHomeName"), PersistentDataType.STRING);
            if (homeName == null) return;

            OfflinePlayer target =  HomeUtils.getTargetFromItemMeta(plugin, meta);
            if (target == null) {
                SoundUtils.playError(plugin, admin);
                admin.sendMessage(ChatColor.RED + "錯誤：無法取得玩家資料。");
                return;
            }

            SoundUtils.playUiOpen(plugin, admin);
            AdminConfirm.open(plugin, admin, target, homeName);
        }

        // ----- 管理員刪除家確認選單的點擊處理 -----
        if (event.getView().getTitle().equals(adminConfirmationTitle)) {
            event.setCancelled(true);

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            ItemMeta metaItem = clickedItem.getItemMeta();
            String itemName = ChatColor.stripColor(metaItem.getDisplayName());

            String confirmName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("confirmation-menu-admin.confirm-item.display-name", "&aConfirm")));
            String cancelName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("confirmation-menu-admin.cancel-item.display-name", "&cCancel")));

            // 確認刪除
            if (itemName.equals(confirmName)) {
                String homeName = metaItem.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "homeName"), PersistentDataType.STRING);
                String uuidString = metaItem.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "adminHomeTarget"), PersistentDataType.STRING);

                if (homeName == null || uuidString == null) {
                    SoundUtils.playError(plugin, admin);
                    admin.sendMessage(ChatColor.RED + "錯誤：無法取得刪除資料。");
                    admin.closeInventory();
                    return;
                }

                UUID targetUUID;
                try {
                    targetUUID = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    SoundUtils.playError(plugin, admin);
                    admin.sendMessage(ChatColor.RED + "錯誤：玩家 UUID 無效。");
                    admin.closeInventory();
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

                // 使用 HomeUtils.deleteHome，共用刪除/最愛/共享清理與訊息
                boolean ok = HomeUtils.deleteHome(plugin, target, homeName, admin);
                String playerName = (target.getName() != null) ? target.getName() : targetUUID.toString();

                if (ok) {
                    String message = ChatColor.translateAlternateColorCodes('&',
                                    plugin.getConfig().getString("messages.home-removed-to-other", "&a已刪除玩家 %player% 的家園 %home%"))
                            .replace("%home%", homeName)
                            .replace("%player%", playerName);
                    SoundUtils.playSuccess(plugin, admin);
                    admin.sendMessage(message);
                } else {
                    String message = ChatColor.translateAlternateColorCodes('&',
                                    plugin.getConfig().getString("messages.player-home-not-exist"))
                            .replace("%home%", homeName)
                            .replace("%player%", playerName);
                    SoundUtils.playFail(plugin, admin);
                    admin.sendMessage(message);
                }

                admin.closeInventory();
            }
            // 取消刪除
            else if (itemName.equals(cancelName)) {
                SoundUtils.playFail(plugin, admin);
                admin.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.home-cancelled")));
                admin.closeInventory();
            }

            return;
        }
    }
}

