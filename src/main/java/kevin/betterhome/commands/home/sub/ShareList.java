package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.utils.SharedHomeUtils;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;

public class ShareList implements ICommand {
    private final BetterHome plugin;
    public ShareList(BetterHome plugin) { this.plugin = plugin; }

    @Override public String name() { return "sharelist"; }
    @Override public String permission() { return "betterhome.share"; }
    @Override public String usage() { return "&f/home sharelist"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var config = plugin.getConfig();
        Player player = (Player) sender;

        Map<String, List<UUID>> out = SharedHomeUtils.getSharedOut(plugin, player.getUniqueId());

        player.sendMessage(color(config.getString("messages.list-shares-header")));

        if (out.isEmpty()) {
            SoundUtils.playFail(plugin, (Player) sender);
            player.sendMessage(color(config.getString("messages.list-shares-empty")));
        } else {
            SoundUtils.playSuccess(plugin, (Player) sender);
            for (Map.Entry<String, List<UUID>> e : out.entrySet()) {
                String home = e.getKey();
                List<String> names = new ArrayList<>();
                for (UUID id : e.getValue()) {
                    var off = Bukkit.getOfflinePlayer(id);
                    names.add(off.getName() != null ? off.getName() : id.toString());
                }
                player.sendMessage(color(config.getString("messages.list-shares-entry")
                        .replace("%home%", home)
                        .replace("%players%", String.join(", ", names))));
            }
        }
        return true;
    }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
