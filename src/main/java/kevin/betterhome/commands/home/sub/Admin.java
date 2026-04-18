package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class Admin implements ICommand {
    private final AdminCreate create;
    private final AdminDelete delete;
    private final String rootCommandLabel;

    public Admin(BetterHome plugin) {
        this.create = new AdminCreate(plugin);
        this.delete = new AdminDelete(plugin);
        this.rootCommandLabel = plugin.getConfig().getString("menu.open-command", "/home").replace("/", "");
    }

    @Override public String name() { return "admin"; }
    @Override public String permission() { return "betterhome.admin"; }
    @Override public String usage() { return "&f/" + rootCommandLabel + " admin &7<&ecreate|delete&7> &8..."; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return execute(sender, args, rootCommandLabel + " " + name());
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            sender.sendMessage(color("&7[&bBetterHome&7] &cUsage: &f/" + label + " &7<&ecreate|delete&7> &8..."));
            return true;
        }
        if ("create".equalsIgnoreCase(args[0])) return create.execute(sender, args, label + " " + args[0].toLowerCase(Locale.ROOT));
        if ("delete".equalsIgnoreCase(args[0])) return delete.execute(sender, args, label + " " + args[0].toLowerCase(Locale.ROOT));
        sender.sendMessage(color("&7[&bBetterHome&7] &cUnknown admin subcommand: &e" + args[0]));
        sender.sendMessage(color("&7[&bBetterHome&7] &cUsage: &f/" + label + " &7<&ecreate|delete&7> &8..."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return List.of("create", "delete").stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if ("create".equalsIgnoreCase(args[0])) {
            return create.tabComplete(sender, args);
        }
        if ("delete".equalsIgnoreCase(args[0])) {
            return delete.tabComplete(sender, args);
        }
        return List.of();
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
