package kevin.betterhome.integration.guilds;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

public class GuildsManager {

    private static Object guildsApi;

    private GuildsManager() {}

    public static void initialize(Plugin plugin) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin guildsPlugin = pluginManager.getPlugin("Guilds");
        if (guildsPlugin == null || !guildsPlugin.isEnabled()) {
            guildsApi = null;
            return;
        }

        try {
            Class<?> guildsClass = Class.forName("me.glaremasters.guilds.Guilds");
            Method getApiMethod = guildsClass.getMethod("getApi");
            guildsApi = getApiMethod.invoke(null);
        } catch (Exception e) {
            guildsApi = null;
            plugin.getLogger().warning("Guilds detected but API initialization failed: " + e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return guildsApi != null;
    }

    public static String getGuildName(Player player) {
        Object guild = getGuildObject(player);
        if (guild == null) return null;
        try {
            Method getNameMethod = guild.getClass().getMethod("getName");
            Object result = getNameMethod.invoke(guild);
            return result == null ? null : result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Location getGuildHomeLocation(Player player) {
        Object guild = getGuildObject(player);
        if (guild == null) return null;
        try {
            Method getHomeMethod = guild.getClass().getMethod("getHome");
            Object guildHome = getHomeMethod.invoke(guild);
            if (guildHome == null) return null;
            Method getAsLocationMethod = guildHome.getClass().getMethod("getAsLocation");
            Object location = getAsLocationMethod.invoke(guildHome);
            return location instanceof Location ? (Location) location : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isInGuild(Player player) {
        return getGuildObject(player) != null;
    }

    private static Object getGuildObject(Player player) {
        if (guildsApi == null || player == null) return null;
        try {
            Method getGuildHandlerMethod = guildsApi.getClass().getMethod("getGuildHandler");
            Object handler = getGuildHandlerMethod.invoke(guildsApi);
            if (handler == null) return null;
            Method getGuildByPlayerIdMethod = handler.getClass().getMethod("getGuildByPlayerId", java.util.UUID.class);
            return getGuildByPlayerIdMethod.invoke(handler, player.getUniqueId());
        } catch (Exception ignored) {
            return null;
        }
    }
}
