package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.integration.guilds.GuildsManager;
import kevin.betterhome.menus.holders.PlayerMenu;
import kevin.betterhome.utils.HomeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class HomesMenu {

    public static void open(Plugin plugin, Player player, int page) {
        FileConfiguration config = plugin.getConfig();
        int size = 36;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        // 讀取玩家所有家園
        List<String> homes = playerConfig.getStringList("homes");
        if (homes == null) homes = new ArrayList<>();

        // 讀取最愛家園名稱，若有的話置頂
        String favorite = playerConfig.getString("favorite", null);
        List<String> orderedHomes = new ArrayList<>(homes);
        if (favorite != null && orderedHomes.contains(favorite)) {
            orderedHomes.remove(favorite);
            orderedHomes.add(0, favorite);
        }

        // Guilds Home 狀態旗幟 & 顏料
        ItemStack guildFlag;
        ItemStack guildDye;

        int maxHomes = HomeUtils.getMaxHomesForPlayer((BetterHome) plugin, player);

        int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) maxHomes / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;

        String rawTitle = ChatColor.translateAlternateColorCodes('&',
                        config.getString("homes-menu.gui-title", "&9家園清單"))
                .replace("%page%", String.valueOf(page + 1))
                .replace("%maxpages%", String.valueOf(totalPages));

        Inventory homesMenu = Bukkit.createInventory(new PlayerMenu(), size, rawTitle);

        // 背景
        ItemStack background = createPane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            homesMenu.setItem(i, background);
        }

        // ===== 頂列中間：共享家園（玩家頭顱） =====
        // slot 4 是第一列的中間
        ItemStack sharedButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) sharedButton.getItemMeta();
        if (skullMeta != null) {
            // 顯示為玩家本人的頭顱
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.GOLD + "共享家園");
            skullMeta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "點擊打開「別人共享給我」的家園清單",
                    ChatColor.GRAY + "可直接傳送到他人分享的位置"
            ));
            // 讓 MenuManager 辨識並打開共享清單
            skullMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "openSharedHomes"),
                    PersistentDataType.STRING, "open"
            );
            sharedButton.setItemMeta(skullMeta);
        }
        homesMenu.setItem(4, sharedButton);

        // Guilds 欄位（沿用原本槽位）
        try {
            String guildName = GuildsManager.getGuildName(player);
            org.bukkit.Location guildHome = GuildsManager.getGuildHomeLocation(player);

            if (guildName != null) {

                // 旗幟
                guildFlag = new ItemStack(Material.BLUE_BANNER);
                ItemMeta flagMeta = guildFlag.getItemMeta();
                if (flagMeta != null) {
                    flagMeta.setDisplayName(ChatColor.AQUA + "所屬工會：§f" + guildName);
                    if (guildHome != null) {
                        flagMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "點擊顏料可傳送到工會家園"));
                    } else {
                        flagMeta.setLore(Collections.singletonList(ChatColor.RED + "此工會尚未設定家園"));
                    }
                    guildFlag.setItemMeta(flagMeta);
                }

                // 顏料
                if (guildHome != null) {
                    guildDye = new ItemStack(Material.BLUE_DYE);
                    ItemMeta dyeMeta = guildDye.getItemMeta();
                    if (dyeMeta != null) {
                        dyeMeta.setDisplayName(ChatColor.AQUA + "點擊可傳送到工會家園");
                        dyeMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "點擊即可傳送至工會設定的家園位置"));
                        dyeMeta.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "guildHome"),
                                PersistentDataType.STRING, "teleport"
                        );
                        guildDye.setItemMeta(dyeMeta);
                    }
                } else {
                    guildDye = new ItemStack(Material.GRAY_DYE);
                    ItemMeta dyeMeta = guildDye.getItemMeta();
                    if (dyeMeta != null) {
                        dyeMeta.setDisplayName(ChatColor.RED + "尚未設定工會家園");
                        dyeMeta.setLore(Collections.singletonList(
                                ChatColor.GRAY + "請由會長使用 " + ChatColor.LIGHT_PURPLE + "/guild sethome" + ChatColor.GRAY + " 來設定家園"
                        ));
                        guildDye.setItemMeta(dyeMeta);
                    }
                }

            } else {
                // 沒有工會
                guildFlag = new ItemStack(Material.RED_BANNER);
                ItemMeta flagMeta = guildFlag.getItemMeta();
                if (flagMeta != null) {
                    flagMeta.setDisplayName(ChatColor.RED + "你沒有任何工會");
                    flagMeta.setLore(Collections.singletonList(ChatColor.GRAY + "加入工會即可使用工會家園功能"));
                    guildFlag.setItemMeta(flagMeta);
                }

                guildDye = new ItemStack(Material.RED_DYE);
                ItemMeta dyeMeta = guildDye.getItemMeta();
                if (dyeMeta != null) {
                    dyeMeta.setDisplayName(ChatColor.RED + "請加入工會以啟用此功能");
                    guildDye.setItemMeta(dyeMeta);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("無法載入 Guilds 功能：" + e.getMessage());

            guildFlag = new ItemStack(Material.BARRIER);
            ItemMeta guildFlagMeta = guildFlag.getItemMeta();
            if (guildFlagMeta != null) {
                guildFlagMeta.setDisplayName(ChatColor.DARK_RED + "Guilds 插件未正確啟用");
                guildFlag.setItemMeta(guildFlagMeta);
            }

            guildDye = new ItemStack(Material.BARRIER);
            ItemMeta guildDyeMeta = guildDye.getItemMeta();
            if (guildDyeMeta != null) {
                guildDyeMeta.setDisplayName(ChatColor.DARK_RED + "Guilds 插件未正確啟用");
                guildDye.setItemMeta(guildDyeMeta);
            }
        }

        homesMenu.setItem(10, guildFlag);
        homesMenu.setItem(19, guildDye);

        // 家園顯示槽位（保持原樣）
        int[] bedSlots = {12, 13, 14, 15, 16};
        int[] dyeSlots = {21, 22, 23, 24, 25};

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, maxHomes);

        for (int i = 0; i < itemsPerPage; i++) {
            int slotBed = bedSlots[i];
            int slotDye = dyeSlots[i];

            // 超出可用家園格數 → 顯示封鎖
            if (start + i >= maxHomes) {
                ItemStack blocked = new ItemStack(Material.RED_BED);
                ItemMeta meta = blocked.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.RED + "沒有權限使用此空格");
                    meta.setLore(Collections.singletonList(ChatColor.GRAY + "請提升權限或等級以解鎖"));
                    blocked.setItemMeta(meta);
                }
                homesMenu.setItem(slotBed, blocked);

                ItemStack blockedDye = new ItemStack(Material.RED_DYE);
                ItemMeta dyeMeta = blockedDye.getItemMeta();
                if (dyeMeta != null) {
                    dyeMeta.setDisplayName(ChatColor.RED + "沒有權限使用此空格");
                    dyeMeta.setLore(Collections.singletonList(ChatColor.GRAY + "請提升權限或等級以解鎖"));
                    blockedDye.setItemMeta(dyeMeta);
                }
                homesMenu.setItem(slotDye, blockedDye);
                continue;
            }

            // 此索引對應的家園名稱（若有），否則顯示預設命名
            String homeName;
            boolean exists;
            if (start + i < orderedHomes.size()) {
                homeName = orderedHomes.get(start + i);
                exists = true;
            } else {
                homeName = "home" + (start + i + 1);
                exists = false;
            }

            boolean isFavorite = exists && favorite != null && favorite.equalsIgnoreCase(homeName);

            Material icon = exists ? HomeUtils.getHomeIcon((BetterHome) plugin, player, homeName) : null;

            ItemStack displayItem;
            if (!exists) {
                displayItem = new ItemStack(Material.LIGHT_GRAY_BED);
            } else if (icon != null) {
                displayItem = new ItemStack(icon);
            } else {
                displayItem = new ItemStack(isFavorite ? Material.PINK_BED : Material.LIGHT_BLUE_BED);
            }

            ItemMeta metaDisplay = displayItem.getItemMeta();
            if (metaDisplay != null) {
                if (exists) {
                    String world = playerConfig.getString(homeName + ".world", "world");
                    double x = playerConfig.getDouble(homeName + ".x", 0);
                    double y = playerConfig.getDouble(homeName + ".y", 0);
                    double z = playerConfig.getDouble(homeName + ".z", 0);

                    Material iconMat = HomeUtils.getHomeIcon((BetterHome) plugin, player, homeName);

                    String title = (isFavorite ? ChatColor.LIGHT_PURPLE + "★ " : "")
                            + ChatColor.AQUA + homeName
                            + ChatColor.GRAY + " ─ "
                            + ChatColor.WHITE + "家園";

                    metaDisplay.setDisplayName(title);

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------");

                    lore.add(ChatColor.GRAY + "世界: " + ChatColor.WHITE + world);
                    lore.add(ChatColor.GRAY + "座標: " + ChatColor.WHITE
                            + String.format("X %.1f  Y %.1f  Z %.1f", x, y, z));
                    if (iconMat != null) {
                        lore.add(ChatColor.GRAY + "圖示: " + ChatColor.WHITE + iconMat.name());
                    }

                    lore.add(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------");

                    lore.add(ChatColor.GOLD + "操作");
                    lore.add(ChatColor.YELLOW + "左鍵 " + ChatColor.WHITE + "傳送");
                    lore.add(ChatColor.AQUA + "右鍵 " + ChatColor.WHITE + "更換圖示");
                    lore.add(ChatColor.GREEN + "Shift+左鍵 " + ChatColor.WHITE + "改名");
                    if (isFavorite) {
                        lore.add(ChatColor.LIGHT_PURPLE + "Shift+右鍵 " + ChatColor.WHITE + "取消最愛");
                    } else {
                        lore.add(ChatColor.LIGHT_PURPLE + "Shift+右鍵 " + ChatColor.WHITE + "設為最愛");
                    }
                    lore.add(ChatColor.BLUE + "Q " + ChatColor.WHITE + "共享管理");
                    lore.add(ChatColor.RED + "下方藍色染料(右鍵) " + ChatColor.WHITE + "刪除");

                    lore.add(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------");

                    metaDisplay.setLore(lore);

                } else {
                    metaDisplay.setDisplayName(ChatColor.GRAY + "空格位：" + ChatColor.WHITE + homeName);
                    metaDisplay.setLore(Arrays.asList(
                            ChatColor.YELLOW + "左鍵 " + ChatColor.WHITE + "立即建立此家園",
                            ChatColor.GRAY + "將自動命名為：" + homeName
                    ));
                }

                metaDisplay.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "homePosition"),
                        PersistentDataType.STRING,
                        homeName
                );
                if (isFavorite) {
                    metaDisplay.getPersistentDataContainer().set(
                            new NamespacedKey(plugin, "favorite"),
                            PersistentDataType.INTEGER, 1
                    );
                }

                displayItem.setItemMeta(metaDisplay);
            }

            homesMenu.setItem(slotBed, displayItem);

            // DYE：刪除（存在時）→ 最愛顯示粉紅色，否則藍色；未設置為灰
            Material dyeType;
            if (!exists) {
                dyeType = Material.GRAY_DYE;
            } else if (isFavorite) {
                dyeType = Material.PINK_DYE;     // ★ 最愛 → 粉紅色
            } else {
                dyeType = Material.BLUE_DYE;     // 一般 → 藍色
            }

            ItemStack dye = new ItemStack(dyeType);
            ItemMeta dyeMeta = dye.getItemMeta();
            if (dyeMeta != null) {
                if (exists) {
                    dyeMeta.setDisplayName(ChatColor.RED + "右鍵點擊刪除此家園");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "刪除：" + homeName);
                    if (isFavorite) {
                        lore.add(ChatColor.DARK_RED + "（目前為最愛，刪除後最愛會一併清除）");
                    }
                    dyeMeta.setLore(lore);
                    dyeMeta.getPersistentDataContainer().set(
                            new NamespacedKey(plugin, "deleteHome"),
                            PersistentDataType.STRING,
                            homeName
                    );
                } else {
                    dyeMeta.setDisplayName(ChatColor.GRAY + "尚未設置");
                }
                dye.setItemMeta(dyeMeta);
            }
            homesMenu.setItem(slotDye, dye);
        }

        // ======（移除舊的 slot 28 共享家園按鈕，不再放）======

        // 上一頁
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "上一頁");
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "menuPage"), PersistentDataType.INTEGER, page - 1);
                prev.setItemMeta(meta);
            }
            homesMenu.setItem(30, prev);
        }

        // 下一頁
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "下一頁");
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "menuPage"), PersistentDataType.INTEGER, page + 1);
                next.setItemMeta(meta);
            }
            homesMenu.setItem(32, next);
        }

        // 關閉按鈕
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            String closePath = config.getString("homes-menu.close-item");
            closeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', closePath));
            close.setItemMeta(closeMeta);
        }
        homesMenu.setItem(31, close);

        player.openInventory(homesMenu);
    }

    private static ItemStack createPane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
