package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Reload implements ICommand {
    private final BetterHome plugin;
    public Reload(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "reload"; }
    @Override public String permission() { return "betterhome.reload"; }
    @Override public String usage() { return "/home reload"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FileConfiguration config = plugin.getConfig();
        plugin.reloadConfig();
        SoundUtils.playSuccess(plugin, (Player) sender);
        sender.sendMessage(color(config.getString("messages.plugin-reloaded", "&a插件重新加載成功。")));
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
