package kevin.betterhome.menus.ui;

import kevin.betterhome.menus.holders.PlayerMenu;
import kevin.betterhome.utils.HomeUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConfirmationMenu {

    private static final Map<Player, HomeUtils.PendingHomeData> pendingHomeNames = new HashMap<>();

    public static void open(Plugin plugin, Player player, String homeName) {
        FileConfiguration config = plugin.getConfig();

        Inventory confirmationMenu = Bukkit.createInventory(new PlayerMenu(), 27,
                ChatColor.translateAlternateColorCodes('&',
                        config.getString("confirmation-menu.gui-title", "&cConfirm Deletion")));

        // 背景與主要顏色設定
        Material backgroundGlass = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

        // 取得按鈕材質
        Material confirmMaterial = Material.matchMaterial(
                config.getString("confirmation-menu.confirm-item.material", "LIME_STAINED_GLASS_PANE").toUpperCase());
        if (confirmMaterial == null) confirmMaterial = Material.LIME_STAINED_GLASS_PANE;

        Material cancelMaterial = Material.matchMaterial(
                config.getString("confirmation-menu.cancel-item.material", "RED_STAINED_GLASS_PANE").toUpperCase());
        if (cancelMaterial == null) cancelMaterial = Material.RED_STAINED_GLASS_PANE;

        // 確認按鈕
        ItemStack confirmItem = new ItemStack(confirmMaterial);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("confirmation-menu.confirm-item.display-name", "&aConfirm")));
        confirmMeta.setLore(Arrays.asList(
                ChatColor.WHITE + "確定要刪除家園: " + ChatColor.AQUA + homeName,
                ChatColor.RED + "此操作不可恢復！請謹慎操作"
        ));
        confirmMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "confirmHome"), PersistentDataType.STRING, homeName);
        confirmItem.setItemMeta(confirmMeta);

        // 取消按鈕
        ItemStack cancelItem = new ItemStack(cancelMaterial);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("confirmation-menu.cancel-item.display-name", "&cCancel")));
        cancelMeta.setLore(Arrays.asList(
                ChatColor.WHITE + "取消刪除，安全返回家園選單"
        ));
        cancelItem.setItemMeta(cancelMeta);

        // 警告物品 (中間)
        ItemStack warningItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta warningMeta = warningItem.getItemMeta();
        warningMeta.setDisplayName(ChatColor.RED + "§l警告");
        warningMeta.setLore(Arrays.asList(
                ChatColor.YELLOW + "你即將刪除家園：" + ChatColor.AQUA + homeName,
                ChatColor.YELLOW + "刪除後將無法復原。",
                ChatColor.YELLOW + "請確認你已備份重要資料。"
        ));
        warningItem.setItemMeta(warningMeta);

        // 背景填充
        ItemStack backgroundItem = new ItemStack(backgroundGlass);
        ItemMeta backgroundMeta = backgroundItem.getItemMeta();
        backgroundMeta.setDisplayName(" ");
        backgroundItem.setItemMeta(backgroundMeta);

        for (int i = 0; i < confirmationMenu.getSize(); i++) {
            confirmationMenu.setItem(i, backgroundItem);
        }

        confirmationMenu.setItem(11, confirmItem);
        confirmationMenu.setItem(15, cancelItem);
        confirmationMenu.setItem(13, warningItem);

        // 開啟選單
        player.openInventory(confirmationMenu);

        // 儲存玩家待確認的操作
        pendingHomeNames.put(player, new HomeUtils.PendingHomeData(homeName, System.currentTimeMillis()));
    }

    public static HomeUtils.PendingHomeData getPendingData(Player player) {
        return pendingHomeNames.get(player);
    }

    public static void clearPending(Player player) {
        pendingHomeNames.remove(player);
    }

}
