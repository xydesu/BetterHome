package kevin.betterhome.commands;

import kevin.betterhome.BetterHome;
import kevin.betterhome.menus.ui.MainMenu;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HomeGui implements CommandExecutor {
    private final BetterHome plugin;
    public HomeGui(BetterHome plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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
        SoundUtils.playUiOpen(plugin, p);
        MainMenu.open(plugin, p);
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
