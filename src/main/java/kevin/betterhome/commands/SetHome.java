package kevin.betterhome.commands;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SetHome implements CommandExecutor {
    private final BetterHome plugin;
    public SetHome(BetterHome plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        if (args.length != 1) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color("&7[&bBetterHome&7] &cUsage: &f/sethome &7<&eHomeName&7>"));
            return true;
        }

        String homeName = args[0];
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, p.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        List<String> homes = playerConfig.getStringList("homes");
        int maxHomes = HomeUtils.getMaxHomesForPlayer(plugin, p);

        if (homes.size() >= maxHomes) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color(config.getString("messages.home-limit-reached")
                    .replace("%limit%", String.valueOf(maxHomes))));
            return true;
        }
        if (playerConfig.contains(homeName)) {
            SoundUtils.playFail(plugin, p);
            p.sendMessage(color(config.getString("messages.home-exists")));
            return true;
        }

        Location loc = p.getLocation();
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
            SoundUtils.playSuccess(plugin, p);
            p.sendMessage(color(config.getString("messages.home-established")).replace("%home%", homeName));
        } catch (IOException e) {
            e.printStackTrace();
            SoundUtils.playError(plugin, p);
            p.sendMessage(color(config.getString("messages.saving-error")));
        }
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
