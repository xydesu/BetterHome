package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AdminCreate implements ICommand {
    private final BetterHome plugin;
    public AdminCreate(BetterHome plugin){ this.plugin = plugin; }

    @Override public String name() { return "admin"; }
    @Override public String permission() { return "betterhome.admin"; }
    @Override public String usage() { return "&f/home admin create &7<&ePlayer&7> &7<&eHomeName&7> &7[&ex y z&7]"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var config = plugin.getConfig();

        if (args.length != 4 && args.length != 6 || !"create".equalsIgnoreCase(args[0])) {
            SoundUtils.playFail(plugin, (Player) sender);
            sender.sendMessage(color(usageMessage()));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            SoundUtils.playFail(plugin, (Player) sender);
            sender.sendMessage(color(config.getString("messages.player-not-found")));
            return true;
        }

        String homeName = args[2];
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, target.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        if (playerConfig.contains(homeName)) {
            SoundUtils.playFail(plugin, (Player) sender);
            sender.sendMessage(color(config.getString("messages.home-exists")));
            return true;
        }

        Location loc;
        if (args.length == 6) {
            double x, y, z;
            try {
                x = Double.parseDouble(args[3]);
                y = Double.parseDouble(args[4]);
                z = Double.parseDouble(args[5]);
            } catch (NumberFormatException e) {
                SoundUtils.playError(plugin, (Player) sender);
                sender.sendMessage(color(config.getString("messages.invalid-coordinates")));
                return true;
            }
            String worldName = (target.getPlayer() != null)
                    ? target.getPlayer().getWorld().getName()
                    : Bukkit.getWorlds().get(0).getName();
            loc = new Location(Bukkit.getWorld(worldName), x, y, z);
        } else {
            if (!(sender instanceof org.bukkit.entity.Player exec)) {
                sender.sendMessage(color(config.getString("messages.player-only")));
                return true;
            }
            loc = exec.getLocation();
        }

        List<String> homes = playerConfig.getStringList("homes");
        homes.add(homeName);
        playerConfig.set("homes", homes);
        playerConfig.set(homeName + ".world", loc.getWorld().getName());
        playerConfig.set(homeName + ".x", loc.getX());
        playerConfig.set(homeName + ".y", loc.getY());
        playerConfig.set(homeName + ".z", loc.getZ());
        playerConfig.set(homeName + ".yaw", loc.getYaw());
        playerConfig.set(homeName + ".pitch", loc.getPitch());

        try {
            playerConfig.save(playerFile);
            SoundUtils.playSuccess(plugin, (Player) sender);
            sender.sendMessage(color(config.getString("messages.home-established-to-other"))
                    .replace("%player%", target.getName()));
        } catch (IOException e) {
            e.printStackTrace();
            SoundUtils.playError(plugin, (Player) sender);
            sender.sendMessage(color(config.getString("messages.saving-error")));
        }
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
