package kevin.betterhome.utils;

import kevin.betterhome.BetterHome;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class SoundUtils {

    private SoundUtils() {}

    /** 依 key 播放音效（若 disabled 或找不到音效名就略過） */
    public static void play(BetterHome plugin, Player player, String key) {
        if (player == null || plugin == null) return;

        FileConfiguration cfg = plugin.getSoundsConfig();
        if (!cfg.getBoolean("enabled", true)) return;

        String name = cfg.getString(key + ".name", null);
        if (name == null || name.isBlank()) return;

        Sound sound;
        try {
            sound = Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return; // 無效音效名就略過
        }

        float volume = (float) cfg.getDouble(key + ".volume", 1.0);
        float pitch = (float) cfg.getDouble(key + ".pitch", 1.0);

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // ===== System sounds =====

    /** 你知道的 */
    public static void playSuccess(BetterHome plugin, Player player) {
        play(plugin, player, "system.success");
    }

    /** 你知道的 */
    public static void playFail(BetterHome plugin, Player player) {
        play(plugin, player, "system.fail");
    }

    /** 錯誤 (像是世界找不到之類的) */
    public static void playError(BetterHome plugin, Player player) {
        play(plugin, player, "system.error");
    }

    // ===== TP Home sounds =====

    /** 倒數音效（一般 tick） */
    public static void playTpCountdown(BetterHome plugin, Player player) {
        play(plugin, player, "tp.countdown");
    }

    /** 倒數最後 1 秒的音效 */
    public static void playTpFinalTick(BetterHome plugin, Player player) {
        play(plugin, player, "tp.final-tick");
    }

    /** 開始傳送那一瞬間（或真正進行位移時）的音效 */
    public static void playTpTeleport(BetterHome plugin, Player player) {
        play(plugin, player, "tp.teleport");
    }

    /** 傳送完成的音效（UI/訊息送出後） */
    public static void playTpComplete(BetterHome plugin, Player player) {
        play(plugin, player, "tp.complete");
    }

    /** 傳送被取消（移動等原因） */
    public static void playTpCancel(BetterHome plugin, Player player) {
        play(plugin, player, "tp.cancel");
    }

    // ===== UI sounds =====

    /** UI 開啟 */
    public static void playUiOpen(BetterHome plugin, Player player) {
        play(plugin, player, "ui.open");
    }

    /** UI 一般點擊 */
    public static void playUiClick(BetterHome plugin, Player player) {
        play(plugin, player, "ui.click");
    }

    /** UI 導覽（例如切換分頁） */
    public static void playUiNavigate(BetterHome plugin, Player player) {
        play(plugin, player, "ui.navigate");
    }

    /** UI 返回上一層 */
    public static void playUiBack(BetterHome plugin, Player player) {
        play(plugin, player, "ui.back");
    }

    /** UI 下一個 */
    public static void playUiNext(BetterHome plugin, Player player) {
        play(plugin, player, "ui.next");
    }

    /** UI 上一個 */
    public static void playUiPrev(BetterHome plugin, Player player) {
        play(plugin, player, "ui.prev");
    }

    /** UI 關閉 */
    public static void playUiClose(BetterHome plugin, Player player) {
        play(plugin, player, "ui.close");
    }

    /** UI 確認 */
    public static void playUiConfirm(BetterHome plugin, Player player) {
        play(plugin, player, "ui.confirm");
    }

    /** UI 取消 */
    public static void playUiCancel(BetterHome plugin, Player player) {
        play(plugin, player, "ui.cancel");
    }

    /** UI 選擇圖示 */
    public static void playUiPickIcon(BetterHome plugin, Player player) {
        play(plugin, player, "ui.pick-icon");
    }

    /** UI 共享/新增 */
    public static void playUiShareAdd(BetterHome plugin, Player player) {
        play(plugin, player, "ui.share-add");
    }

    /** UI 共享/移除 */
    public static void playUiShareRemove(BetterHome plugin, Player player) {
        play(plugin, player, "ui.share-remove");
    }

    /** UI 提示（例如輸入提示） */
    public static void playUiPrompt(BetterHome plugin, Player player) {
        play(plugin, player, "ui.prompt");
    }
}
