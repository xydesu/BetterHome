package kevin.betterhome.integration.guilds;

import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.api.GuildsAPI;
import org.bukkit.plugin.Plugin;

public class IntegrationInstanceGuilds {

    private static GuildsAPI api;

    public static void initialize(Plugin plugin) {
        try {
            api = Guilds.getApi(); // 確保 Guilds 已啟用
            plugin.getLogger().info("成功整合 Guilds 插件。");
        } catch (Throwable t) {
            plugin.getLogger().severe("載入 Guilds API 時失敗，BetterHome 將關閉。");
            throw new RuntimeException("Guilds API 初始化失敗", t);
        }
    }

    public static GuildsAPI getApi() {
        return api;
    }
}
