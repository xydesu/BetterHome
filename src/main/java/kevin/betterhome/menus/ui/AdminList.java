package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.holders.AdminMenu;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AdminList {

    private static Plugin plugin;
    private static LuckPerms luckPerms;

    // 快取的使用者清單
    private static List<User> cachedUsers = Collections.emptyList();
    private static boolean cacheLoaded = false;

    /**
     * 初始化方法，必須在 Plugin 啟動時呼叫一次
     */
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            plugin.getLogger().severe("[AdminList] 無法取得 LuckPerms API 實例，請確認 LuckPerms 插件已啟用。");
        }
    }

    /**
     * 異步刷新所有玩家快取（包含離線玩家）
     * callback 將在主線程執行
     */
    public static void refreshCacheAsync(Plugin plugin, Runnable callback) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin 不能為 null");
        }
        if (luckPerms == null) {
            plugin.getLogger().severe("[AdminList] LuckPerms API 尚未取得，無法刷新快取");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("[AdminList] 開始刷新玩家快取...");
                OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();

                List<CompletableFuture<User>> futureUsers = new ArrayList<>();
                for (OfflinePlayer offlinePlayer : offlinePlayers) {
                    UUID uuid = offlinePlayer.getUniqueId();
                    CompletableFuture<User> futureUser = luckPerms.getUserManager().loadUser(uuid);
                    futureUsers.add(futureUser);
                }

                // 等待所有非同步載入完成
                CompletableFuture.allOf(futureUsers.toArray(new CompletableFuture[0])).join();

                // 收集所有成功載入的 User
                List<User> users = futureUsers.stream()
                        .map(CompletableFuture::join)  // join 後必定有結果（或拋異常）
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 排序
                users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));

                cachedUsers = Collections.unmodifiableList(users);
                cacheLoaded = true;

                plugin.getLogger().info("[AdminList] 玩家快取刷新完成，使用者數量: " + cachedUsers.size());

                if (callback != null) {
                    Bukkit.getScheduler().runTask(plugin, callback);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[AdminList] 快取刷新發生錯誤:");
                e.printStackTrace();
            }
        });
    }

    /**
     * 取得快取的使用者清單
     */
    public static List<User> getCachedUsers() {
        return cachedUsers;
    }

    /**
     * 判斷快取是否已加載完成
     */
    public static boolean isCacheLoaded() {
        return cacheLoaded;
    }

    /**
     * 開啟管理員清單 GUI
     */
    public static void open(Player player, int page) {
        if (!cacheLoaded) {
            Inventory loading = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Loading...");
            player.openInventory(loading);
            // 快取尚未載入時，可由外部控制快取觸發
            return;
        }

        int itemsPerPage = 45; // 54格-9格底欄
        int totalPages = Math.max(1, (int) Math.ceil((double) cachedUsers.size() / itemsPerPage));
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, cachedUsers.size());

        String title = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("admin-menu.gui-title", "&8Admin Menu - Page %page%/%maxpages%"))
                .replace("%page%", String.valueOf(page + 1))
                .replace("%maxpages%", String.valueOf(totalPages));

        Inventory menu = Bukkit.createInventory(new AdminMenu(), 54, title);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        for (int i = start; i < end; i++) {
            User user = cachedUsers.get(i);
            UUID uuid = user.getUniqueId();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName(ChatColor.YELLOW + user.getUsername());

                boolean online = offlinePlayer.isOnline();
                ChatColor statusColor = online ? ChatColor.GREEN : ChatColor.RED;
                String statusText = online ? "在線" : "離線";

                String lastSeen = offlinePlayer.getLastPlayed() > 0
                        ? sdf.format(new Date(offlinePlayer.getLastPlayed()))
                        : "未知";

                List<String> lore = plugin.getConfig().getStringList("admin-menu.player-head-lore");
                if (lore.isEmpty()) {
                    lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "狀態: " + statusColor + statusText);
                    lore.add(ChatColor.GRAY + "最後上線: " + ChatColor.WHITE + lastSeen);
                } else {
                    lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&',
                            line.replace("%player%", user.getUsername())
                                    .replace("%status%", statusColor + statusText)
                                    .replace("%lastlogin%", lastSeen)));
                }
                meta.setLore(lore);

                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "adminHomeTarget"),
                        PersistentDataType.STRING,
                        uuid.toString()
                );

                skull.setItemMeta(meta);
            }

            menu.addItem(skull);
        }

        decorateFooter(menu, page, totalPages);

        player.openInventory(menu);
    }

    private static void decorateFooter(Inventory menu, int page, int totalPages) {
        // 黑玻璃底
        for (int i = 45; i < 54; i++) {
            ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) meta.setDisplayName(" ");
            glass.setItemMeta(meta);
            menu.setItem(i, glass);
        }

        // 返回 (箭頭)
        addButton(menu, Material.ARROW, plugin.getConfig().getString("admin-menu.go-back-item", "&a返回"), 45);

        // 關閉 (障礙物)
        addButton(menu, Material.BARRIER, plugin.getConfig().getString("homes-menu.close-item", "&c關閉"), 49);

        // 上一頁 (紙張)
        if (page > 0) {
            addPageButton(menu, "adminPage", page - 1, plugin.getConfig().getString("admin-menu.previous-page-item", "&7上一頁"), 48);
        }

        // 下一頁 (紙張)
        if (page < totalPages - 1) {
            addPageButton(menu, "adminPage", page + 1, plugin.getConfig().getString("admin-menu.next-page-item", "&7下一頁"), 50);
        }
    }

    private static void addButton(Inventory menu, Material material, String name, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        menu.setItem(slot, item);
    }

    private static void addPageButton(Inventory menu, String key, int value, String name, int slot) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, value);
        }
        item.setItemMeta(meta);
        menu.setItem(slot, item);
    }
}
