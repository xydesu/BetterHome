package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SharedHomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class Tp implements ICommand {
    private final BetterHome plugin;
    public Tp(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "tp"; }
    @Override public String permission() { return "betterhome.use"; }
    @Override public String usage() { return "&f/home tp &7<&eHomeName &7| &eOwnerName:HomeName&7>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FileConfiguration config = plugin.getConfig();
        Player player = (Player) sender;

        if (args.length != 1) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(usageMessage()));
            return true;
        }

        String arg = args[0];

        // ---- 支援 owner:home（共享家園）----
        if (arg.contains(":")) {
            String[] parts = arg.split(":", 2);
            String ownerName = parts[0].trim();
            String homeName  = parts.length > 1 ? parts[1].trim() : "";

            if (ownerName.isEmpty() || homeName.isEmpty()) {
                player.sendMessage(color("&7[&bBetterHome&7] &cInvalid format. Use &f/home tp owner:home"));
                return true;
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
            if (owner == null || (owner.getName() == null && !owner.isOnline())) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.owner-not-found")
                                .replace("%owner%", ownerName)
                ));
                return true;
            }

            UUID ownerUuid = owner.getUniqueId();

            // 確認真的被分享過
            Map<UUID, List<String>> sharedIn = SharedHomeUtils.getSharedIn(plugin, player.getUniqueId());
            List<String> homes = sharedIn.get(ownerUuid);
            if (homes == null || homes.stream().noneMatch(h -> h.equalsIgnoreCase(homeName))) {
                SoundUtils.playFail(plugin, player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.shared-home-not-shared")
                                .replace("%owner%", ownerName)
                                .replace("%home%", homeName)
                ));
                return true;
            }

            // 傳送（帶冷卻）
            SharedHomeUtils.teleportPlayerToSharedHomeWithCooldown(plugin, player, ownerUuid, homeName);
            return true;
        }

        // ---- 原本：自己的家 ----
        String homeName = arg;

        if (HomeUtils.getCurrentHomeCount(plugin, player) == 0) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.not-established-home")));
            return true;
        }
        if (!HomeUtils.hasHome(plugin, player, homeName)) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", homeName)));
            return true;
        }

        HomeUtils.teleportPlayerToHomeWithCooldown(plugin, player, homeName);
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
