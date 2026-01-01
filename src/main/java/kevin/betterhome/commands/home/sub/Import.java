package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.storage.LegacyHomeImporter;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Import implements ICommand {
    private final BetterHome plugin;
    private final LegacyHomeImporter importer;

    public Import(BetterHome plugin) {
        this.plugin = plugin;
        this.importer = new LegacyHomeImporter(plugin);
    }

    @Override public String name() { return "import"; }
    @Override public String usage() { return "/home import <Essentials|HuskHomes>"; }
    @Override public String permission() { return null; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var config = plugin.getConfig();

        if (args.length < 1) {
            SoundUtils.playFail(plugin, (Player) sender);
            sender.sendMessage(color(usage()));
            return true;
        }

        String type = args[0];
        if (type.equalsIgnoreCase("Essentials")) {
            // Essentials 匯入
            if (sender instanceof Player p) {
                if (!p.hasPermission("betterhome.import.essentials")) {
                    SoundUtils.playFail(plugin, (Player) sender);
                    p.sendMessage(color(config.getString("messages.import-no-permission")));
                    return true;
                }
                importer.importHomesFromEssentialsForAllPlayers(p);
            } else {
                importer.importHomesFromEssentialsForAllPlayers(sender);
            }
        } else if (type.equalsIgnoreCase("HuskHomes")) {
            // HuskHomes 匯入
            String dbPath = "HuskHomes/HuskHomesData.db";
            if (sender instanceof Player p) {
                if (!p.hasPermission("betterhome.import.huskhomes")) {
                    SoundUtils.playFail(plugin, (Player) sender);
                    p.sendMessage(color(config.getString("messages.import-no-permission")));
                    return true;
                }
                importer.importHomesFromHuskHomesForAllPlayers(p, dbPath);
            } else {
                importer.importHomesFromHuskHomesForAllPlayers(sender, dbPath);
            }
        } else {
            SoundUtils.playFail(plugin, (Player) sender);
            sender.sendMessage(color(usage()));
        }
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
