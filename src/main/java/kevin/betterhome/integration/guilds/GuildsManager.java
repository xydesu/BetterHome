package kevin.betterhome.integration.guilds;

import me.glaremasters.guilds.api.GuildsAPI;
import me.glaremasters.guilds.guild.Guild;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildsManager {

    /**
     * 透過玩家取得所屬的 Guild 對象
     *
     * @param player 要查詢的玩家
     * @return 公會對象，如果沒有加入公會則為 null
     */
    public static Guild getGuild(Player player) {
        GuildsAPI api = IntegrationInstanceGuilds.getApi();
        return api.getGuildHandler().getGuildByPlayerId(player.getUniqueId());
    }

    /**
     * 檢查玩家是否有加入公會
     *
     * @param player 玩家
     * @return true 表示有加入公會
     */
    public static boolean isInGuild(Player player) {
        return getGuild(player) != null;
    }
}
