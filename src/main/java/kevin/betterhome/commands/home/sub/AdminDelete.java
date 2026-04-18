package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminDelete implements ICommand {
    private final BetterHome plugin;
    public AdminDelete(BetterHome plugin){ this.plugin = plugin; }

    @Override public String name() { return "admin"; }
    @Override public String permission() { return "betterhome.admin"; }
    @Override public String usage() { return "&f/home admin delete &7<&ePlayer&7> &7<&eHomeName&7>"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return execute(sender, args, "home admin delete");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, String label) {
        var config = plugin.getConfig();

        if (args.length != 3 || !"delete".equalsIgnoreCase(args[0])) {
            if (sender instanceof Player p) SoundUtils.playFail(plugin, p);
            sender.sendMessage(color("&7[&bBetterHome&7] &cUsage: &f/" + label + " &7<&ePlayer&7> &7<&eHomeName&7>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            if (sender instanceof Player p) SoundUtils.playFail(plugin, p);
            sender.sendMessage(color(config.getString("messages.player-not-found"))
                    .replace("%player%", args[1]));
            return true;
        }

        String homeName = args[2];

        // 用 Offline 版刪除（會清共享 & 通知被分享者；也會回覆 admin 訊息）
        boolean ok = HomeUtils.deleteHome(plugin, target, homeName, sender);

        if (!ok) {
            // 失敗時補一條明確訊息
            if (sender instanceof Player p) SoundUtils.playFail(plugin, p);
            sender.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", homeName)));
        }
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
