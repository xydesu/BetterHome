package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.HomeUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class AdminConfirm {
    private final BetterHome plugin;

    public AdminConfirm(BetterHome plugin) {
        this.plugin = plugin;
    }

    public static void open(BetterHome plugin, Player admin, OfflinePlayer target, String homeName) {
        FileConfiguration config = plugin.getConfig();

        Inventory menu = Bukkit.createInventory(null, 9,
                ChatColor.translateAlternateColorCodes('&',
                        config.getString("confirmation-menu-admin.gui-title")));

        Material confirmMat = Material.getMaterial(config.getString("confirmation-menu.confirm-item.material", "GREEN_WOOL"));
        Material cancelMat = Material.getMaterial(config.getString("confirmation-menu.cancel-item.material", "RED_WOOL"));
        if (confirmMat == null) confirmMat = Material.GREEN_WOOL;
        if (cancelMat == null) cancelMat = Material.RED_WOOL;

        ItemStack confirm = new ItemStack(confirmMat);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("confirmation-menu.confirm-item.display-name", "&aConfirm")));
        confirmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "homeName"), PersistentDataType.STRING, homeName);
        confirmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomeTarget"), PersistentDataType.STRING, target.getUniqueId().toString());
        confirm.setItemMeta(confirmMeta);

        ItemStack cancel = new ItemStack(cancelMat);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("confirmation-menu.cancel-item.display-name", "&cCancel")));
        cancel.setItemMeta(cancelMeta);

        menu.setItem(3, confirm);
        menu.setItem(5, cancel);

        admin.openInventory(menu);
        plugin.pendingHomeNames.put(admin, new HomeUtils.PendingHomeData(homeName, System.currentTimeMillis()));
    }

    public Map<Player, HomeUtils.PendingHomeData> getPendingHomeNames() {
        return plugin.pendingHomeNames;
    }
}