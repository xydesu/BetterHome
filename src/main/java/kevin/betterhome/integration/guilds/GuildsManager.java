package kevin.betterhome.integration.guilds;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

public class GuildsManager {

    private static Object guildsApi;
    private static Method getGuildHandlerMethod;
    private static Method getGuildByPlayerIdMethod;
    private static Method getGuildNameMethod;
    private static Method getGuildHomeMethod;
    private static Method getGuildHomeAsLocationMethod;

    private GuildsManager() {}

    public static void initialize(Plugin plugin) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin guildsPlugin = pluginManager.getPlugin("Guilds");
        if (guildsPlugin == null || !guildsPlugin.isEnabled()) {
            guildsApi = null;
            clearCache();
            return;
        }

        try {
            Class<?> guildsClass = Class.forName("me.glaremasters.guilds.Guilds");
            Method getApiMethod = guildsClass.getMethod("getApi");
            guildsApi = getApiMethod.invoke(null);
            if (guildsApi == null) {
                clearCache();
                return;
            }

            getGuildHandlerMethod = guildsApi.getClass().getMethod("getGuildHandler");
            Object handler = getGuildHandlerMethod.invoke(guildsApi);
            if (handler == null) {
                clearCache();
                return;
            }
            getGuildByPlayerIdMethod = handler.getClass().getMethod("getGuildByPlayerId", java.util.UUID.class);

            Object sampleGuild = null;
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                sampleGuild = getGuildByPlayerIdMethod.invoke(handler, online.getUniqueId());
                if (sampleGuild != null) break;
            }
            if (sampleGuild != null) {
                getGuildNameMethod = sampleGuild.getClass().getMethod("getName");
                getGuildHomeMethod = sampleGuild.getClass().getMethod("getHome");
            }
        } catch (Throwable e) {
            guildsApi = null;
            clearCache();
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
            Method method = getGuildNameMethod;
            if (method == null) {
                method = guild.getClass().getMethod("getName");
                getGuildNameMethod = method;
            }
            Object result = method.invoke(guild);
            return result == null ? null : result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Location getGuildHomeLocation(Player player) {
        Object guild = getGuildObject(player);
        if (guild == null) return null;
        try {
            Method homeMethod = getGuildHomeMethod;
            if (homeMethod == null) {
                homeMethod = guild.getClass().getMethod("getHome");
                getGuildHomeMethod = homeMethod;
            }
            Object guildHome = homeMethod.invoke(guild);
            if (guildHome == null) return null;
            Method locationMethod = getGuildHomeAsLocationMethod;
            if (locationMethod == null) {
                locationMethod = guildHome.getClass().getMethod("getAsLocation");
                getGuildHomeAsLocationMethod = locationMethod;
            }
            Object location = locationMethod.invoke(guildHome);
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
            if (getGuildHandlerMethod == null) {
                getGuildHandlerMethod = guildsApi.getClass().getMethod("getGuildHandler");
            }
            Object handler = getGuildHandlerMethod.invoke(guildsApi);
            if (handler == null) return null;
            if (getGuildByPlayerIdMethod == null) {
                getGuildByPlayerIdMethod = handler.getClass().getMethod("getGuildByPlayerId", java.util.UUID.class);
            }
            return getGuildByPlayerIdMethod.invoke(handler, player.getUniqueId());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void clearCache() {
        getGuildHandlerMethod = null;
        getGuildByPlayerIdMethod = null;
        getGuildNameMethod = null;
        getGuildHomeMethod = null;
        getGuildHomeAsLocationMethod = null;
    }
}
