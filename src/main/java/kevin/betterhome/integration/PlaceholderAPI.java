package kevin.betterhome.integration;

import kevin.betterhome.BetterHome;
import kevin.betterhome.utils.HomeUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final BetterHome plugin;

    public PlaceholderAPI(BetterHome plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "betterhome"; }
    @Override public String getAuthor() { return "Kevi_28576"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer offline, String paramsRaw) {
        if (offline == null) return "";
        String params = paramsRaw == null ? "" : paramsRaw;

        File playerFile = plugin.getPlayerDataFile(offline.getUniqueId());
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playerFile);

        List<String> homes = cfg.getStringList("homes");
        int count = homes == null ? 0 : homes.size();
        int max = 0;
        try {
            if (offline.isOnline()) {
                max = HomeUtils.getMaxHomesForPlayer(plugin, (Player) offline.getPlayer());
            } else {
                max = plugin.getConfig().getInt("default-max-homes", 3);
            }
        } catch (Throwable ignored) {
            max = plugin.getConfig().getInt("default-max-homes", 3);
        }
        int free = Math.max(0, max - count);

        int cdSeconds = 0;
        if (offline.isOnline() && plugin.teleportCooldowns.containsKey(offline.getPlayer())) {
            long now = System.currentTimeMillis();
            long end = plugin.teleportCooldowns.get(offline.getPlayer());
            if (now < end) {
                cdSeconds = (int) Math.ceil((end - now) / 1000.0);
            }
        }

        switch (params) {
            case "count":                 return String.valueOf(count);
            case "max":                   return String.valueOf(max);
            case "free":                  return String.valueOf(free);
            case "favorite":              return safe(cfg.getString("favorite", ""));
            case "cooldown":              return String.valueOf(cdSeconds);
            case "cooldown_formatted":    return formatSeconds(cdSeconds); // mm:ss
            case "command":               return plugin.getConfig().getString("menu.open-command", "/home");
        }

        // %betterhome_exists_<name>%
        if (params.startsWith("exists_")) {
            String name = params.substring("exists_".length());
            return boolYesNo(homes != null && homes.contains(name));
        }

        // %betterhome_world_<name>% / x_ / y_ / z_ / yaw_ / pitch_ / icon_ / loc_<name>%
        if (startsAny(params, "world_", "x_", "y_", "z_", "yaw_", "pitch_", "icon_", "loc_")) {
            String[] split = params.split("_", 2);
            if (split.length == 2) {
                String key = split[0];       // world/x/y/z/yaw/pitch/icon/loc
                String name = split[1];      // home 名稱
                if (homes == null || !homes.contains(name)) return "";

                if (key.equals("icon")) {
                    String icon = cfg.getString(name + ".icon", "");
                    return icon == null ? "" : icon;
                }
                if (key.equals("loc")) {
                    String w = cfg.getString(name + ".world", "");
                    double x = cfg.getDouble(name + ".x", 0D);
                    double y = cfg.getDouble(name + ".y", 0D);
                    double z = cfg.getDouble(name + ".z", 0D);
                    return w + " " + formatNum(x) + " " + formatNum(y) + " " + formatNum(z);
                }

                switch (key) {
                    case "world": return safe(cfg.getString(name + ".world", ""));
                    case "x":     return formatNum(cfg.getDouble(name + ".x", 0D));
                    case "y":     return formatNum(cfg.getDouble(name + ".y", 0D));
                    case "z":     return formatNum(cfg.getDouble(name + ".z", 0D));
                    case "yaw":   return formatNum(cfg.getDouble(name + ".yaw", 0D));
                    case "pitch": return formatNum(cfg.getDouble(name + ".pitch", 0D));
                }
            }
        }

        return null; // 未匹配
    }

    private static boolean startsAny(String s, String... prefixes) {
        for (String p : prefixes) if (s.startsWith(p)) return true;
        return false;
    }

    private static String formatNum(double d) {
        return String.format(java.util.Locale.US, "%.1f", d);
    }

    private static String formatSeconds(int total) {
        int m = total / 60, s = total % 60;
        return String.format("%02d:%02d", m, s);
    }

    private static String boolYesNo(boolean v) {
        return v ? "true" : "false";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
