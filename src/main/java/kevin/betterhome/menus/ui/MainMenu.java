package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.menus.holders.PlayerMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainMenu {

    public static void open(Plugin plugin, Player player) {
        FileConfiguration config = plugin.getConfig();
        String menuTitlePath = "menu.gui-title";

        Inventory menu = Bukkit.createInventory(new PlayerMenu(), 27,
                ChatColor.translateAlternateColorCodes('&', config.getString(menuTitlePath)));

        String borderItemName = config.getString("menu.border-item", "LIGHT_GRAY_STAINED_GLASS_PANE").toUpperCase();
        Material borderItemMaterial = Material.matchMaterial(borderItemName);
        if (borderItemMaterial == null) borderItemMaterial = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

        ItemStack borderItem = new ItemStack(borderItemMaterial);
        ItemMeta borderItemMeta = borderItem.getItemMeta();
        borderItemMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderItemMeta);

        int[] borderSlots = {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26
        };
        for (int slot : borderSlots) {
            menu.setItem(slot, borderItem);
        }

        String centerItemName = config.getString("menu.center-item", "LIGHT_BLUE_STAINED_GLASS_PANE").toUpperCase();
        Material centerItemMaterial = Material.matchMaterial(centerItemName);
        if (centerItemMaterial == null) centerItemMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

        ItemStack centerItem = new ItemStack(centerItemMaterial);
        ItemMeta centerItemMeta = centerItem.getItemMeta();
        centerItemMeta.setDisplayName(" ");
        centerItem.setItemMeta(centerItemMeta);

        menu.setItem(10, centerItem);
        menu.setItem(12, centerItem);
        menu.setItem(14, centerItem);
        menu.setItem(16, centerItem);

        String setHomeMaterialName = config.getString("menu.set-home-item.material", "LIGHT_BLUE_BED").toUpperCase();
        Material setHomeMaterial = Material.matchMaterial(setHomeMaterialName);
        if (setHomeMaterial == null) setHomeMaterial = Material.LIGHT_BLUE_BED;

        ItemStack setHomeItem = new ItemStack(setHomeMaterial);
        ItemMeta setHomeMeta = setHomeItem.getItemMeta();
        setHomeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("menu.set-home-item.display-name")));

        List<String> setHomeLore = config.getStringList("menu.set-home-item.lore");
        for (int i = 0; i < setHomeLore.size(); i++) {
            setHomeLore.set(i, ChatColor.translateAlternateColorCodes('&', setHomeLore.get(i)));
        }
        setHomeMeta.setLore(setHomeLore);
        setHomeItem.setItemMeta(setHomeMeta);
        menu.setItem(11, setHomeItem);

        String homeListMaterialName = config.getString("menu.your-homes-item.material").toUpperCase();
        Material homeListMaterial = Material.matchMaterial(homeListMaterialName);
        if (homeListMaterial == null) homeListMaterial = Material.OAK_DOOR;

        ItemStack homeListItem = new ItemStack(homeListMaterial);
        ItemMeta homeListMeta = homeListItem.getItemMeta();
        homeListMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("menu.your-homes-item.display-name")));

        List<String> homeListLore = config.getStringList("menu.your-homes-item.lore");
        for (int i = 0; i < homeListLore.size(); i++) {
            homeListLore.set(i, ChatColor.translateAlternateColorCodes('&', homeListLore.get(i)));
        }
        homeListMeta.setLore(homeListLore);
        homeListItem.setItemMeta(homeListMeta);
        menu.setItem(15, homeListItem);

        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();

        infoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("menu.info-title", "&e家園系統說明")));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GREEN + "▸ 歡迎使用家園管理系統！");
        lore.add(ChatColor.YELLOW + "▸ 你可以隨時新增、刪除或傳送到你的家園");
        lore.add(ChatColor.YELLOW + "▸ 家園資料會自動儲存，安全又方便");
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GOLD + "✔ 已設定家園數量: " + ChatColor.AQUA + HomeUtils.getCurrentHomeCount((BetterHome) plugin, player)
                + ChatColor.GRAY + " / "
                + ChatColor.AQUA + HomeUtils.getMaxHomesForPlayer((BetterHome) plugin, player) + ChatColor.GOLD + " （家園上限）");
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.LIGHT_PURPLE + "小提示: ");
        lore.add(ChatColor.LIGHT_PURPLE + "  使用 " + ChatColor.WHITE + "/home [家園名稱]" + ChatColor.LIGHT_PURPLE + " 快速傳送");
        lore.add(ChatColor.LIGHT_PURPLE + "  使用 " + ChatColor.WHITE + "/home create [家園名稱]" + ChatColor.LIGHT_PURPLE + " 設定新家");
        lore.add(ChatColor.LIGHT_PURPLE + "  使用 " + ChatColor.WHITE + "/home delete [家園名稱]" + ChatColor.LIGHT_PURPLE + " 刪除家園");
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "如需更多家園數量，請洽詢伺服器管理員");

        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        menu.setItem(13, infoItem);

        /*
        if (player.hasPermission("betterhome.admin")) {
            String adminMaterialName = config.getString("admin-menu.material", "BOOK").toUpperCase();
            Material adminMaterial = Material.matchMaterial(adminMaterialName);
            if (adminMaterial == null) adminMaterial = Material.BARRIER;

            ItemStack adminItem = new ItemStack(adminMaterial);
            ItemMeta adminMeta = adminItem.getItemMeta();

            adminMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    config.getString("admin-menu.display-name", "&c管理員面板")));

            List<String> adminMenuLore = config.getStringList("admin-menu.lore");
            if (adminMenuLore.isEmpty()) {
                adminMenuLore = Arrays.asList(
                        "&7點擊開啟管理介面",
                        "&7你可以查看所有玩家家園、強制傳送、管理限制等"
                );
            }

            for (int i = 0; i < adminMenuLore.size(); i++) {
                adminMenuLore.set(i, ChatColor.translateAlternateColorCodes('&', adminMenuLore.get(i)));
            }

            adminMeta.setLore(adminMenuLore);
            adminItem.setItemMeta(adminMeta);
            menu.setItem(26, adminItem);
        }
         */

        player.openInventory(menu);
    }
}
