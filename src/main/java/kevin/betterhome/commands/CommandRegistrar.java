package kevin.betterhome.commands;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.home.Home;
import kevin.betterhome.commands.home.sub.Share;
import kevin.betterhome.commands.home.sub.SharesList;
import kevin.betterhome.commands.home.sub.Unshare;
import kevin.betterhome.tabcompleters.HomeTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

public class CommandRegistrar {
    public static void registerAll(BetterHome plugin, HomeTabCompleter completer) {
        String base = plugin.getConfig().getString("menu.open-command", "/home").replace("/", "");

        register(plugin, base, new Home(plugin, base), completer);
        register(plugin, base + "s", new Homes(plugin), completer);
        register(plugin, "homegui", new HomeGui(plugin), completer);
        register(plugin, "gohome", new GoHome(plugin), completer);
        register(plugin, "sethome", new SetHome(plugin), completer);
    }

    private static void register(BetterHome plugin, String cmdName, Object executor, HomeTabCompleter completer) {
        PluginCommand cmd = Bukkit.getPluginCommand(cmdName);
        if (cmd == null) {
            plugin.getLogger().warning("Command not found in plugin.yml: " + cmdName);
            return;
        }
        cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
        cmd.setTabCompleter(completer);
    }
}
