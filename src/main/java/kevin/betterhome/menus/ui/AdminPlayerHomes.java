package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.holders.AdminMenu;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.List;

public class AdminPlayerHomes {
    private final BetterHome plugin;

    public AdminPlayerHomes(BetterHome plugin) {
        this.plugin = plugin;
    }

    public static void open(BetterHome plugin, Player admin, OfflinePlayer target, int page) {
        admin.closeInventory();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, target.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        FileConfiguration pluginConfig = plugin.getConfig();

        List<String> homes = config.getStringList("homes");
        int size = pluginConfig.getInt("homes-menu.size");
        int itemsPerPage = size - 9;
        int totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / itemsPerPage));
        String title = ChatColor.translateAlternateColorCodes('&',
                        pluginConfig.getString("admin-menu.admin-gui-title"))
                .replace("%page%", String.valueOf(page + 1))
                .replace("%maxpages%", String.valueOf(totalPages))
                .replace("%player%", target.getName());

        Inventory menu = Bukkit.createInventory(new AdminMenu(), size, title);

        if (homes.isEmpty()) {
            ItemStack noHome = new ItemStack(Material.PAPER);
            ItemMeta meta = noHome.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        pluginConfig.getString("homes-menu.no-homes-item.display-name")));
                List<String> lore = pluginConfig.getStringList("homes-menu.no-homes-item.lore");
                lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(lore);
                noHome.setItemMeta(meta);
            }
            menu.setItem(22, noHome);
        } else {
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, homes.size());

            for (int i = start; i < end; i++) {
                String home = homes.get(i);
                if (!config.contains(home)) continue;

                Location loc = new Location(
                        Bukkit.getWorld(config.getString(home + ".world")),
                        config.getDouble(home + ".x"),
                        config.getDouble(home + ".y"),
                        config.getDouble(home + ".z")
                );

                ItemStack item = new ItemStack(Material.LIGHT_BLUE_BED);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                                pluginConfig.getString("homes-menu.home-item.display-name"))
                        .replace("%home%", home));

                List<String> lore = pluginConfig.getStringList("homes-menu.home-item.lore");
                lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line)
                        .replace("%home%", home)
                        .replace("%world%", loc.getWorld().getName())
                        .replace("%x%", String.valueOf(loc.getBlockX()))
                        .replace("%y%", String.valueOf(loc.getBlockY()))
                        .replace("%z%", String.valueOf(loc.getBlockZ())));
                meta.setLore(lore);

                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomeName"), PersistentDataType.STRING, home);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomeTarget"), PersistentDataType.STRING, target.getUniqueId().toString());
                item.setItemMeta(meta);
                menu.addItem(item);
            }
        }

        decorateFooter(plugin, menu, pluginConfig, page, totalPages, target);
        admin.openInventory(menu);
    }

    private static void decorateFooter(BetterHome plugin, Inventory menu, FileConfiguration config, int page, int totalPages, OfflinePlayer target) {
        int size = menu.getSize();
        for (int i = size - 9; i < size; i++) {
            ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                glass.setItemMeta(meta);
            }
            menu.setItem(i, glass);
        }

        addNavButton(menu, Material.ARROW, config.getString("homes-menu.go-back-item"), size - 9 + 3);
        addNavButton(menu, Material.BARRIER, config.getString("homes-menu.close-item"), size - 9 + 4);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.PAPER);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    config.getString("homes-menu.previous-page-item")));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomesPage"), PersistentDataType.INTEGER, page - 1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomeTarget"), PersistentDataType.STRING, target.getUniqueId().toString());
            prev.setItemMeta(meta);
            menu.setItem(size - 9 + 1, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.PAPER);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    config.getString("homes-menu.next-page-item")));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomesPage"), PersistentDataType.INTEGER, page + 1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "adminHomeTarget"), PersistentDataType.STRING, target.getUniqueId().toString());
            next.setItemMeta(meta);
            menu.setItem(size - 9 + 7, next);
        }
    }

    private static void addNavButton(Inventory menu, Material material, String name, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        menu.setItem(slot, item);
    }
}
