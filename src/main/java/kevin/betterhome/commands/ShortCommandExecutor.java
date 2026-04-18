package kevin.betterhome.commands;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Wraps an {@link ICommand} as a standalone {@link CommandExecutor} so that
 * short commands (e.g. {@code /share}, {@code /unshare}, {@code /sharelist})
 * can delegate directly to the same logic used by {@code /home <sub>}.
 */
public class ShortCommandExecutor implements CommandExecutor {
    private final BetterHome plugin;
    private final ICommand delegate;

    public ShortCommandExecutor(BetterHome plugin, ICommand delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var config = plugin.getConfig();

        if (delegate.playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(color(config.getString("messages.player-only")));
            return true;
        }

        String perm = delegate.permission();
        if (perm != null && !sender.hasPermission(perm)) {
            if (sender instanceof Player p) SoundUtils.playFail(plugin, p);
            sender.sendMessage(color(config.getString("messages.no-permissions")));
            return true;
        }

        return delegate.execute(sender, args, label.toLowerCase(Locale.ROOT));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
