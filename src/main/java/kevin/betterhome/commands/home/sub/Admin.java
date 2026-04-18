package kevin.betterhome.commands.home.sub;

import kevin.betterhome.BetterHome;
import kevin.betterhome.commands.ICommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class Admin implements ICommand {
    private final AdminCreate create;
    private final AdminDelete delete;

    public Admin(BetterHome plugin) {
        this.create = new AdminCreate(plugin);
        this.delete = new AdminDelete(plugin);
    }

    @Override public String name() { return "admin"; }
    @Override public String permission() { return "betterhome.admin"; }
    @Override public String usage() { return "&f/home admin &7<&ecreate|delete&7> &8..."; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        if ("create".equalsIgnoreCase(args[0])) return create.execute(sender, args);
        if ("delete".equalsIgnoreCase(args[0])) return delete.execute(sender, args);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
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
}
