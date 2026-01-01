package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.HomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Create implements ICommand {
    private final BetterHome plugin;
    public Create(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "create"; }
    @Override public String permission() { return "betterhome.use"; }
    @Override public String usage() { return "/home create <HomeName>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FileConfiguration config = plugin.getConfig();
        Player player = (Player) sender;

        if (args.length != 1) {
            SoundUtils.playFail(plugin, player);
            sender.sendMessage(color(usage()));
            return true;
        }

        String homeName = args[0];

        File dataFolder = new File(plugin.getDataFolder(), "data");
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        List<String> homes = playerConfig.getStringList("homes");
        int maxHomes = HomeUtils.getMaxHomesForPlayer(plugin, player);

        if (homes.size() >= maxHomes) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.home-limit-reached").replace("%limit%", String.valueOf(maxHomes))));
            return true;
        }

        if (playerConfig.contains(homeName)) {
            SoundUtils.playFail(plugin, player);
            player.sendMessage(color(config.getString("messages.home-exists")));
            return true;
        }

        Location loc = player.getLocation();
        String worldName = loc.getWorld().getName();

        homes.add(homeName);
        playerConfig.set("homes", homes);
        playerConfig.set(homeName + ".world", worldName);
        playerConfig.set(homeName + ".x", loc.getX());
        playerConfig.set(homeName + ".y", loc.getY());
        playerConfig.set(homeName + ".z", loc.getZ());
        playerConfig.set(homeName + ".yaw", loc.getYaw());
        playerConfig.set(homeName + ".pitch", loc.getPitch());

        try {
            playerConfig.save(playerFile);
            SoundUtils.playSuccess(plugin, player);
            player.sendMessage(color(config.getString("messages.home-established")).replace("%home%", homeName));
        } catch (IOException e) {
            e.printStackTrace();
            SoundUtils.playError(plugin, player);
            player.sendMessage(color(config.getString("messages.saving-error")));
        }
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
