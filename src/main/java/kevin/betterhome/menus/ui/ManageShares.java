package kevin.betterhome.menus.ui;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.SharedHomeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ManageShares {

    // PDC keys for this GUI
    public static final String KEY_MANAGE_HOME  = "manageHome";
    public static final String KEY_SHARED_TGT   = "sharedTarget";   // UUID string
    public static final String KEY_ADD_SHARE    = "addShare";
    public static final String KEY_BACK         = "back";

    /**
     * 打開「共享管理」選單
     */
    public static void open(BetterHome plugin, Player owner, String homeName) {
        // 安全處理
        if (homeName == null || homeName.isEmpty()) {
            owner.sendMessage("§c無效的家園名稱。");
            return;
        }

        NamespacedKey manageHomeKey = new NamespacedKey(plugin, KEY_MANAGE_HOME);
        NamespacedKey sharedTgtKey  = new NamespacedKey(plugin, KEY_SHARED_TGT);
        NamespacedKey addShareKey   = new NamespacedKey(plugin, KEY_ADD_SHARE);
        NamespacedKey backKey       = new NamespacedKey(plugin, KEY_BACK);

        // 介面大小：一頁 27（3 行），上面列出共享名單，最下排放按鈕
        Inventory inv = Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&',
                        "&9共享管理 &7- &f" + homeName));

        // 背景
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // 取得 owner 分享出去的清單，挑出指定家
        Map<String, List<UUID>> out = SharedHomeUtils.getSharedOut(plugin, owner.getUniqueId());
        List<UUID> list = out.getOrDefault(homeName, Collections.emptyList());

        // 列出共享對象（頭顱）
        int idx = 0;
        for (UUID uid : list) {
            if (idx >= 18) break; // 上方兩行 0~17
            OfflinePlayer op = Bukkit.getOfflinePlayer(uid);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(op);
                String name = (op.getName() != null) ? op.getName() : uid.toString();
                sm.setDisplayName(ChatColor.AQUA + name);
                sm.setLore(Arrays.asList(
                        ChatColor.GRAY + "右鍵：移除共享",
                        ChatColor.DARK_GRAY + "UUID: " + uid.toString()
                ));
                sm.getPersistentDataContainer().set(manageHomeKey, PersistentDataType.STRING, homeName);
                sm.getPersistentDataContainer().set(sharedTgtKey,  PersistentDataType.STRING, uid.toString());
                skull.setItemMeta(sm);
            }
            inv.setItem(idx, skull);
            idx++;
        }

        // 底列：新增 / 返回
        // 新增
        ItemStack add = new ItemStack(Material.LIME_DYE);
        ItemMeta addM = add.getItemMeta();
        if (addM != null) {
            addM.setDisplayName(ChatColor.GREEN + "新增共享");
            addM.setLore(Arrays.asList(
                    ChatColor.YELLOW + "點擊後在聊天欄輸入玩家名稱",
                    ChatColor.GRAY + "輸入 " + ChatColor.RED + "cancel " + ChatColor.GRAY + "可取消"
            ));
            addM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            addM.getPersistentDataContainer().set(addShareKey, PersistentDataType.STRING, homeName);
            add.setItemMeta(addM);
        }
        inv.setItem(22, add);

        // 返回
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backM = back.getItemMeta();
        if (backM != null) {
            backM.setDisplayName(ChatColor.YELLOW + "返回");
            backM.getPersistentDataContainer().set(backKey, PersistentDataType.INTEGER, 1);
            backM.getPersistentDataContainer().set(manageHomeKey, PersistentDataType.STRING, homeName);
            back.setItemMeta(backM);
        }
        inv.setItem(26, back);

        owner.openInventory(inv);
    }
}
