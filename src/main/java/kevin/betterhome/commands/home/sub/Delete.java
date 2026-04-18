package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Delete implements ICommand {
    private final BetterHome plugin;
    public Delete(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "delete"; }
    @Override public String permission() { return "betterhome.use"; }
    @Override public String usage() { return "&f/home delete &7<&eHomeName&7>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FileConfiguration config = plugin.getConfig();
        Player player = (Player) sender;

        if (args.length != 1) {
            SoundUtils.playFail(plugin, player);
            sender.sendMessage(color(usageMessage()));
            return true;
        }

        String homeName = args[0];

        boolean ok = HomeUtils.deleteHome(plugin, player, homeName, null);

        if (!ok) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", homeName)));
        }
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
