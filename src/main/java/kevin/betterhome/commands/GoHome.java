package kevin.betterhome.commands;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SharedHomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class GoHome implements CommandExecutor {
    private final BetterHome plugin;
    public GoHome(BetterHome plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var config = plugin.getConfig();
        if (!(sender instanceof Player p)) {
            sender.sendMessage(color(config.getString("messages.player-only")));
            return true;
        }
        if (!p.hasPermission("betterhome.use")) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color(config.getString("messages.no-permissions")));
            return true;
        }
        if (args.length != 1) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color("&7[&bBetterHome&7] &cUsage: &f/gohome &7<&eHomeName &7| &eOwnerName:HomeName&7>"));
            return true;
        }

        String arg = args[0];

        // ---- 支援 owner:home（共享家園）----
        if (arg.contains(":")) {
            String[] parts = arg.split(":", 2);
            String ownerName = parts[0].trim();
            String homeName  = parts.length > 1 ? parts[1].trim() : "";

            if (ownerName.isEmpty() || homeName.isEmpty()) {
                SoundUtils.playFail(plugin, p);
                p.sendMessage(color("&7[&bBetterHome&7] &cInvalid format. Use &f/gohome owner:home"));
                return true;
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
            if (owner == null || (owner.getName() == null && !owner.isOnline())) {
                SoundUtils.playFail(plugin, p);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.owner-not-found")
                                .replace("%owner%", ownerName)
                ));
                return true;
            }

            UUID ownerUuid = owner.getUniqueId();

            // 確認真的被分享過
            Map<UUID, List<String>> sharedIn = SharedHomeUtils.getSharedIn(plugin, p.getUniqueId());
            List<String> homes = sharedIn.get(ownerUuid);
            if (homes == null || homes.stream().noneMatch(h -> h.equalsIgnoreCase(homeName))) {
                SoundUtils.playFail(plugin, p);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.shared-home-not-shared")
                                .replace("%owner%", ownerName)
                                .replace("%home%", homeName)
                ));
                return true;
            }

            // 傳送（帶冷卻）
            SharedHomeUtils.teleportPlayerToSharedHomeWithCooldown(plugin, p, ownerUuid, homeName);
            return true;
        }

        // ---- 原本：自己的家 ----
        String homeName = arg;

        if (HomeUtils.getCurrentHomeCount(plugin, p) == 0) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color(config.getString("messages.not-established-home")));
            return true;
        }
        if (!HomeUtils.hasHome(plugin, p, homeName)) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", homeName)));
            return true;
        }
        HomeUtils.teleportPlayerToHomeWithCooldown(plugin, p, homeName);
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
