package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SharedHomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class Unshare implements ICommand {
    private final BetterHome plugin;
    public Unshare(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "unshare"; }
    @Override public String permission() { return "betterhome.share"; }
    @Override public String usage() { return "&f/home unshare &7<&eHomeName&7> &7<&ePlayer&7> &8| &f/unshare &7<&eHomeName&7> &7<&ePlayer&7>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var config = plugin.getConfig();
        Player player = (Player) sender;

        if (args.length != 2) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(usageMessage()));
            return true;
        }

        String homeName = args[0];
        Player target = Bukkit.getPlayer(args[1]);

        if (!HomeUtils.hasHome(plugin, player, homeName)) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", homeName)));
            return true;
        }

        if (target == null) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.player-not-found")
                    .replace("%player%", args[1])));
            return true;
        }

        // 先檢查是否真的有共享給此玩家
        if (!SharedHomeUtils.isSharedWith(plugin, player, homeName, target.getUniqueId())) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.not-shared-with-player")
                    .replace("%home%", homeName)
                    .replace("%player%", target.getName())));
            return true;
        }

        if (SharedHomeUtils.removeShare(plugin, player, homeName, target)) {
            SoundUtils.playUiShareRemove(plugin, player);
            player.sendMessage(color(config.getString("messages.unshare-success")
                    .replace("%home%", homeName)
                    .replace("%player%", target.getName())));
            SoundUtils.playUiShareRemove(plugin, target);
            target.sendMessage(color(config.getString("messages.unshare-received")
                    .replace("%owner%", player.getName())
                    .replace("%home%", homeName)));
        } else {
            // 萬一底層保存失敗或其他原因
            SoundUtils.playError(plugin, player);
            player.sendMessage(color(config.getString("messages.unshare-failed")));
        }
        return true;
    }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
