package kevin.betterhome.commands;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface ICommand {
    String name();                 // 子指令名稱，如 "create"
    String permission();           // 權限字串，無則返回 null
    String usage();                // 用法提示（帶色碼）
    default String usageMessage() { return "&7[&bBetterHome&7] &cUsage: " + usage(); }
    boolean playerOnly();          // 是否只能玩家使用
    boolean execute(CommandSender sender, String[] args);
    default List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
