package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.holders.PlayerMenu;
import kevin.betterhome.utils.SharedHomeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 「別人共享給我」的家園清單
 * - 骨幹：rows = [(ownerUUID, homeName)]
 * - 每頁 21 筆（10~16, 19~25, 28~34）
 * - 頂列 / 底列為框線與控制列
 * - 必備控制：上一頁、下一頁、返回我的家園、關閉
 */
public class SharedHomes {

    // PDC keys
    private static final String KEY_TELEPORT_OWNER = "sharedOwner"; // UUID string
    private static final String KEY_TELEPORT_HOME  = "sharedHome";  // 家名
    private static final String KEY_PAGE           = "sharedPage";  // 分頁整數
    private static final String KEY_BACK           = "sharedBack";  // 返回我的家園

    public static void open(BetterHome plugin, Player viewer, int page) {
        FileConfiguration config = plugin.getConfig();
        // 取得「別人共享給我」: ownerUUID -> [homeName...]
        Map<UUID, List<String>> incoming = SharedHomeUtils.getSharedIn(plugin, viewer.getUniqueId());

        // 攤平成 (owner, home) 列表
        List<Map.Entry<UUID, String>> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<String>> e : incoming.entrySet()) {
            UUID owner = e.getKey();
            for (String hn : e.getValue()) {
                rows.add(new AbstractMap.SimpleEntry<>(owner, hn));
            }
        }

        // 分頁
        final int size = 45; // 5x9
        final int per  = 21; // 10~16, 19~25, 28~34
        int totalPages = Math.max(1, (int) Math.ceil(rows.size() / (double) per));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        String title = ChatColor.translateAlternateColorCodes('&',
                "&9共享家園 &7(&f" + (page + 1) + "/" + totalPages + "&7)");
        Inventory inv = Bukkit.createInventory(new PlayerMenu(), size, title);

        // ===== 背景 =====
        ItemStack topPane = pane(Material.BLUE_STAINED_GLASS_PANE, " ");
        ItemStack midPane = pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inv.setItem(i, midPane);
        for (int i = 0; i < 9; i++) inv.setItem(i, topPane);         // 頂列
        for (int i = 36; i < 45; i++) inv.setItem(i, topPane);       // 底列

        // ===== 內容槽位 =====
        int[] slots = {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34
        };

        int start = page * per;
        if (rows.isEmpty()) {
            // 空清單：顯示提示
            ItemStack tip = new ItemStack(Material.PAPER);
            ItemMeta tm = tip.getItemMeta();
            if (tm != null) {
                tm.setDisplayName(ChatColor.GRAY + "目前沒有共享的家園");
                tm.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "其他玩家可以用 /home share <家名> <你> 分享給你",
                        ChatColor.DARK_GRAY + "收到分享後，會出現在這裡"
                ));
                tip.setItemMeta(tm);
            }
            inv.setItem(22, tip);
        } else {
            for (int i = 0; i < per && (start + i) < rows.size(); i++) {
                int slot = slots[i];
                Map.Entry<UUID, String> row = rows.get(start + i);
                UUID ownerId = row.getKey();
                String homeName = row.getValue();

                OfflinePlayer op = Bukkit.getOfflinePlayer(ownerId);

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) skull.getItemMeta();
                if (sm != null) {
                    sm.setOwningPlayer(op);
                    String ownerName = (op.getName() != null) ? op.getName() : ownerId.toString();
                    sm.setDisplayName(ChatColor.AQUA + homeName + ChatColor.GRAY + "  ─  " + ChatColor.WHITE + ownerName);
                    sm.setLore(Arrays.asList(
                            ChatColor.YELLOW + "左鍵傳送",
                            ChatColor.GRAY + "來自： " + ownerName
                    ));
                    // 傳送所需 PDC
                    sm.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_TELEPORT_OWNER),
                            PersistentDataType.STRING, ownerId.toString());
                    sm.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_TELEPORT_HOME),
                            PersistentDataType.STRING, homeName);
                    skull.setItemMeta(sm);
                }
                inv.setItem(slot, skull);
            }
        }

        // ===== 控制列（底列）=====
        // 返回我的家園
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(ChatColor.YELLOW + "返回我的家園");
            bm.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, KEY_BACK),
                    PersistentDataType.INTEGER, 1
            );
            back.setItemMeta(bm);
        }
        inv.setItem(39, back);

        // 上一頁
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName(ChatColor.YELLOW + "上一頁");
                pm.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, KEY_PAGE),
                        PersistentDataType.INTEGER, page - 1
                );
                prev.setItemMeta(pm);
            }
            inv.setItem(37, prev);
        }

        // 下一頁
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName(ChatColor.YELLOW + "下一頁");
                nm.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, KEY_PAGE),
                        PersistentDataType.INTEGER, page + 1
                );
                next.setItemMeta(nm);
            }
            inv.setItem(41, next);
        }

        // 關閉按鈕
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            String closePath = config.getString("homes-menu.close-item");
            closeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', closePath));
            closeMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            close.setItemMeta(closeMeta);
        }
        inv.setItem(40, close);

        viewer.openInventory(inv);
    }

    private static ItemStack pane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
