package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Rename implements ICommand {
    private final BetterHome plugin;
    public Rename(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "rename"; }
    @Override public String permission() { return "betterhome.use"; }
    @Override public String usage() { return "/home rename <OldName> <NewName>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FileConfiguration config = plugin.getConfig();
        Player player = (Player) sender;

        if (args.length != 2) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(usage()));
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        if (oldName.equalsIgnoreCase(newName)) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.rename-same-name")));
            return true;
        }
        if (!HomeUtils.hasHome(plugin, player, oldName)) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.home-not-found")
                    .replace("%home%", oldName)));
            return true;
        }
        if (HomeUtils.hasHome(plugin, player, newName)) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.home-exists")));
            return true;
        }

        boolean ok = HomeUtils.renameHome(plugin, player, oldName, newName);
        if (ok) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.home-renamed"))
                    .replace("%old%", oldName).replace("%new%", newName));
        } else {
            SoundUtils.playError(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.saving-error")));
        }
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
