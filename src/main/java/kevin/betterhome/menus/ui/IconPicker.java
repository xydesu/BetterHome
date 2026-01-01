package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.holders.PlayerMenu;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class IconPicker {

    private static final int SIZE = 54; // 6*9，保留幾格做翻頁/返回
    private static final int CONTENT_START = 0;
    private static final int CONTENT_END = 44; // 0~44 = 45格內容
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    public static void open(Plugin plugin, Player player, String homeName, int page) {
        FileConfiguration cfg = plugin.getConfig();
        String title = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("icon-picker.title", "&9選擇家園圖示")) + " §7(" + homeName + ")";

        List<String> list = cfg.getStringList("icon-picker.whitelist");
        List<Material> mats = new ArrayList<>();
        for (String s : list) {
            try {
                Material m = Material.valueOf(s);
                if (m.isItem()) mats.add(m);
            } catch (IllegalArgumentException ignored) {}
        }
        int perPage = (CONTENT_END - CONTENT_START + 1);
        int totalPages = Math.max(1, (int)Math.ceil(mats.size() / (double)perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(new PlayerMenu(), SIZE, title);

        // 填內容
        int start = page * perPage;
        int end = Math.min(start + perPage, mats.size());
        NamespacedKey pickKey = new NamespacedKey(plugin, "iconPick");
        NamespacedKey homeKey = new NamespacedKey(plugin, "iconHome");
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            int slot = CONTENT_START + i;
            if (idx >= end) break;
            Material m = mats.get(idx);
            ItemStack it = new ItemStack(m);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + m.name());
                meta.getPersistentDataContainer().set(pickKey, PersistentDataType.STRING, m.name());
                meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
                it.setItemMeta(meta);
            }
            inv.setItem(slot, it);
        }

        // 翻頁 + 返回
        // prev
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta im = prev.getItemMeta();
            im.setDisplayName(ChatColor.YELLOW + "上一頁");
            im.getPersistentDataContainer().set(new NamespacedKey(plugin, "iconPage"), PersistentDataType.INTEGER, page - 1);
            im.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
            prev.setItemMeta(im);
            inv.setItem(SLOT_PREV, prev);
        }
        // back
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(ChatColor.RED + "返回");
        bm.getPersistentDataContainer().set(new NamespacedKey(plugin, "iconBack"), PersistentDataType.INTEGER, 1);
        bm.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);
        // next
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta im = next.getItemMeta();
            im.setDisplayName(ChatColor.YELLOW + "下一頁");
            im.getPersistentDataContainer().set(new NamespacedKey(plugin, "iconPage"), PersistentDataType.INTEGER, page + 1);
            im.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
            next.setItemMeta(im);
            inv.setItem(SLOT_NEXT, next);
        }

        player.openInventory(inv);
    }
}
