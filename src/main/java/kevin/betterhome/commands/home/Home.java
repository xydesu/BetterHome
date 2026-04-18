package kevin.betterhome.commands.home;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import kevin.betterhome.commands.home.sub.*;
import kevin.betterhome.menus.ui.MainMenu;
import kevin.betterhome.utils.SoundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class Home implements CommandExecutor, TabCompleter {
    private final BetterHome plugin;
    private final String baseName;
    private final Map<String, ICommand> subs = new LinkedHashMap<>();

    public Home(BetterHome plugin, String baseName) {
        this.plugin = plugin;
        this.baseName = baseName;
        registerSubs();
    }

    private void registerSubs() {
        add(new Create(plugin));
        add(new Tp(plugin));
        add(new Rename(plugin));
        add(new Delete(plugin));
        add(new Reload(plugin));
        add(new Import(plugin));
        add(new Share(plugin));
        add(new Unshare(plugin));
        add(new ShareList(plugin));
        add(new Admin(plugin));
    }

    private void add(ICommand cmd) { subs.put(cmd.name().toLowerCase(Locale.ROOT), cmd); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();

        // /home -> 開主選單
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (!p.hasPermission("betterhome.use")) {
                    SoundUtils.playFail(plugin, p);
                    p.sendMessage(color(config.getString("messages.no-permissions")));
                    return true;
                }
                SoundUtils.playUiOpen(plugin, p);
                MainMenu.open(plugin, p);
                return true;
            }
            return true;
        }

        // 分派子指令
        ICommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.sendMessage(color("&7[&bBetterHome&7] &e/home &7sub-commands:"));
            for (ICommand s : subs.values()) {
                sender.sendMessage(color("  &8\u00bb " + s.usage()));
            }
            return true;
        }
        if (sub.playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(color(config.getString("messages.player-only")));
            return true;
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            if (sender instanceof Player p) SoundUtils.playFail(plugin, p);
            sender.sendMessage(color(config.getString("messages.no-permissions")));
            return true;
        }
        // 把子指令名稱吃掉再傳參
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, subArgs, label + " " + args[0].toLowerCase(Locale.ROOT));
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // Tab 補全
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cur = args[0].toLowerCase(Locale.ROOT);
            List<String> keys = new ArrayList<>(subs.keySet());
            keys.removeIf(k -> !k.startsWith(cur));
            return keys;
        }
        ICommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub != null) {
            return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return List.of();
    }
}
