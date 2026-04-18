package kevin.betterhome.tabcompleters;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.SharedHomeUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class HomeTabCompleter implements TabCompleter {

    private final BetterHome plugin;

    public HomeTabCompleter(BetterHome plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> suggestions = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();

        // 取得主指令名稱（例如 /home），移除前置斜線
        String configuredCommand = config.getString("menu.open-command", "/home").replace("/", "");
        String cmdName = command.getName().toLowerCase(Locale.ROOT);
        String aliasName = alias == null ? "" : alias.toLowerCase(Locale.ROOT);

        // =========================
        // /gohome <name | owner:home>
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, "gohome")) {
            if (!(sender instanceof Player p)) return List.of();
            if (!p.hasPermission("betterhome.use")) return List.of();

            if (args.length == 1) {
                String input = args[0];

                // 支援 owner:home 形式
                if (input.contains(":")) {
                    String[] split = input.split(":", 2);
                    String ownerNamePrefix = split[0];
                    String homePrefix = split.length > 1 ? split[1] : "";

                    // 列出符合玩家名前綴的離線/在線玩家（曾進服）
                    for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                        String name = op.getName();
                        if (name == null) continue;
                        if (!name.toLowerCase(Locale.ROOT).startsWith(ownerNamePrefix.toLowerCase(Locale.ROOT))) continue;

                        // 從「別人共享給我」清單找該 owner 分享給我的家
                        Map<UUID, List<String>> sharedIn = SharedHomeUtils.getSharedIn(plugin, p.getUniqueId());
                        List<String> homes = sharedIn.getOrDefault(op.getUniqueId(), List.of());
                        for (String h : homes) {
                            if (h.toLowerCase(Locale.ROOT).startsWith(homePrefix.toLowerCase(Locale.ROOT))) {
                                suggestions.add(name + ":" + h);
                            }
                        }
                    }
                } else {
                    // 補全自己的家園
                    suggestions.addAll(plugin.getHomesFor(p));
                }
                return unique(filterByPrefix(suggestions, input));
            }
            return List.of();
        }

        // =========================
        // /sethome <name>
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, "sethome")) {
            if (sender instanceof Player p && p.hasPermission("betterhome.use")) {
                if (args.length == 1 && isBlank(args[0])) suggestions.add("<家園名稱>");
                return unique(filterByPrefix(suggestions, args[0]));
            }
            return List.of();
        }

        // =========================
        // /share <home> <player>
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, "share")) {
            if (!(sender instanceof Player p) || !p.hasPermission("betterhome.share")) return List.of();
            if (args.length == 1) {
                suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[0]));
            }
            if (args.length == 2) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(p.getUniqueId())) suggestions.add(online.getName());
                }
                return unique(filterByPrefix(suggestions, args[1]));
            }
            return List.of();
        }

        // =========================
        // /unshare <home> <player>
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, "unshare")) {
            if (!(sender instanceof Player p) || !p.hasPermission("betterhome.share")) return List.of();
            if (args.length == 1) {
                suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[0]));
            }
            if (args.length == 2) {
                List<String> sharedNames = SharedHomeUtils
                        .getSharedPlayerNamesForHome(plugin, p.getUniqueId(), args[0]);
                suggestions.addAll(sharedNames);
                return unique(filterByPrefix(suggestions, args[1]));
            }
            return List.of();
        }

        // =========================
        // /sharelist
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, "sharelist")) {
            return List.of();
        }

        // =========================
        // /home 主指令（可自訂名稱）
        // =========================
        if (equalsAnyIgnoreCase(cmdName, aliasName, configuredCommand)) {
            if (!(sender instanceof Player p)) return List.of();

            // /home <tab>
            if (args.length == 1) {
                if (p.hasPermission("betterhome.reload")) suggestions.add("reload");
                if (p.hasPermission("betterhome.import.essentials") || p.hasPermission("betterhome.import.huskhomes"))
                    suggestions.add("import");
                if (p.hasPermission("betterhome.admin")) suggestions.add("admin");

                // 一般子指令
                suggestions.addAll(List.of("create", "delete", "tp", "rename"));

                // 共享相關
                suggestions.addAll(List.of("share", "unshare", "sharelist"));

                return unique(filterByPrefix(suggestions, args[0]));
            }

            // /home import <tab>
            if (args.length == 2 && equalsIgnoreCase(args[0], "import")) {
                if (p.hasPermission("betterhome.import.essentials")) suggestions.add("Essentials");
                if (p.hasPermission("betterhome.import.huskhomes")) suggestions.add("HuskHomes");
                return unique(filterByPrefix(suggestions, args[1]));
            }

            // /home create <tab>
            if (args.length == 2 && equalsIgnoreCase(args[0], "create")) {
                if (isBlank(args[1])) suggestions.add("<家園名稱>");
                return unique(filterByPrefix(suggestions, args[1]));
            }

            // /home delete <home>
            if (args.length == 2 && equalsIgnoreCase(args[0], "delete")) {
                File playerFile = plugin.getPlayerDataFile(p.getUniqueId());
                if (playerFile.exists()) suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[1]));
            }

            // /home tp <name> 或 <owner>:<home>
            if (args.length == 2 && equalsIgnoreCase(args[0], "tp")) {
                if (!p.hasPermission("betterhome.use")) return List.of();

                // 先加上自己的所有家園
                suggestions.addAll(plugin.getHomesFor(p));

                // 再加上「別人分享給我的」家園
                Map<UUID, List<String>> sharedIn = SharedHomeUtils.getSharedIn(plugin, p.getUniqueId());
                for (UUID ownerId : sharedIn.keySet()) {
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
                    if (owner.getName() == null) continue;

                    for (String h : sharedIn.get(ownerId)) {
                        suggestions.add(owner.getName() + ":" + h);
                    }
                }

                // 如果玩家有輸入前綴就過濾，否則顯示全部
                String input = args[1];
                return unique(filterByPrefix(suggestions, input));
            }

            // /home rename <oldName> <newName>
            if (args.length == 2 && equalsIgnoreCase(args[0], "rename")) {
                suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[1]));
            }
            if (args.length == 3 && equalsIgnoreCase(args[0], "rename")) {
                if (isBlank(args[2])) suggestions.add("<新家園名稱>");
                return unique(filterByPrefix(suggestions, args[2]));
            }

            // /home share <home> <player>
            if (args.length == 2 && equalsIgnoreCase(args[0], "share")) {
                suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[1]));
            }
            if (args.length == 3 && equalsIgnoreCase(args[0], "share")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(p.getUniqueId())) {
                        suggestions.add(online.getName());
                    }
                }
                return unique(filterByPrefix(suggestions, args[2]));
            }

            // /home unshare <home> <player>
            if (args.length == 2 && equalsIgnoreCase(args[0], "unshare")) {
                // 列出玩家自己擁有的家園名稱
                suggestions.addAll(plugin.getHomesFor(p));
                return unique(filterByPrefix(suggestions, args[1]));
            }
            if (args.length == 3 && equalsIgnoreCase(args[0], "unshare")) {
                // 列出這個家園已分享過的玩家名稱
                String homeName = args[1];
                List<String> sharedNames = SharedHomeUtils
                        .getSharedPlayerNamesForHome(plugin, p.getUniqueId(), homeName);
                suggestions.addAll(sharedNames);
                return unique(filterByPrefix(suggestions, args[2]));
            }

            // /home admin <create/delete>
            if (args.length == 2 && equalsIgnoreCase(args[0], "admin")) {
                suggestions.add("create");
                suggestions.add("delete");
                return unique(filterByPrefix(suggestions, args[1]));
            }

            // /home admin create <player>
            if (args.length == 3 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "create")) {
                for (Player online : Bukkit.getOnlinePlayers()) suggestions.add(online.getName());
                return unique(filterByPrefix(suggestions, args[2]));
            }

            // /home admin create <player> <homeName>
            if (args.length == 4 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "create")) {
                if (isBlank(args[3])) suggestions.add("<家園名稱>");
                return unique(filterByPrefix(suggestions, args[3]));
            }

            // /home admin create <player> <homeName> <x/y/z>
            if (args.length >= 5 && args.length <= 6 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "create")) {
                suggestions.add("<x>");
                suggestions.add("<y>");
                return unique(filterByPrefix(suggestions, args[args.length - 1]));
            }
            if (args.length == 7 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "create")) {
                suggestions.add("<z>");
                return unique(filterByPrefix(suggestions, args[6]));
            }

            // /home admin delete <player>
            if (args.length == 3 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "delete")) {
                for (Player online : Bukkit.getOnlinePlayers()) suggestions.add(online.getName());
                return unique(filterByPrefix(suggestions, args[2]));
            }

            // /home admin delete <player> <homeName>
            if (args.length == 4 && equalsIgnoreCase(args[0], "admin") && equalsIgnoreCase(args[1], "delete")) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) suggestions.addAll(plugin.getHomesFor(target));
                else if (isBlank(args[3])) suggestions.add("<家園名稱>");
                return unique(filterByPrefix(suggestions, args[3]));
            }
        }

        return unique(suggestions);
    }

    /** 依前綴過濾（不分大小寫） */
    private List<String> filterByPrefix(List<String> list, String prefix) {
        if (list == null || list.isEmpty()) return List.of();
        if (prefix == null || prefix.isEmpty()) return list;
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s != null && s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        }
        return out;
    }

    /** alias / commandName 其一符合即成立 */
    private boolean equalsAnyIgnoreCase(String a, String b, String target) {
        return equalsIgnoreCase(a, target) || equalsIgnoreCase(b, target);
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private List<String> unique(List<String> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }
}
