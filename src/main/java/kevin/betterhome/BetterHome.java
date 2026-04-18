package kevin.betterhome;

import kevin.betterhome.commands.CommandRegistrar;
import kevin.betterhome.menus.MenuManager;
import kevin.betterhome.integration.Metrics;
import kevin.betterhome.integration.guilds.GuildsManager;
import kevin.betterhome.menus.ui.AdminList;
import kevin.betterhome.tabcompleters.HomeTabCompleter;
import kevin.betterhome.utils.HomeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.earth2me.essentials.Essentials;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class BetterHome extends JavaPlugin {
    private FileConfiguration soundsConfig;
    private File soundsFile;
    private static final String DEBUG_PERMISSION = "betterhome.debug";

    ConsoleCommandSender console = Bukkit.getConsoleSender();
    private BukkitRunnable cacheUpdateTask;
    private Essentials essentials;

    // 狀態資料
    public Map<Player, Long> teleportCooldowns = new HashMap<>();
    public Set<Player> teleportingPlayers = new HashSet<>();
    public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    public Map<Player, ScheduledFuture<?>> cooldownTasks = new ConcurrentHashMap<>();
    public Map<Player, HomeUtils.PendingHomeData> pendingHomeNames = new HashMap<>();
    public Map<Player, HomeUtils.PendingHomeData> pendingRenameHomes = new HashMap<>();
    public final Map<Player, String> pendingShareAdds = new HashMap<>();
    @Override
    public void onEnable() {

        // 初始化並建立預設配置
        saveDefaultConfig();
        loadSoundsConfig();
        reloadConfig();

        // 啟動訊息 Banner
        getLogger().info("======================================");
        getLogger().info("BetterHome is starting...");
        getLogger().info("Plugin version: " + getDescription().getVersion());
        getLogger().info("Server: " + getServer().getName() + " " + getServer().getVersion());
        getLogger().info("======================================");

        // Essentials 整合 (非必需)
        Plugin essPlugin = getServer().getPluginManager().getPlugin("Essentials");
        if (essPlugin instanceof Essentials) {
            this.essentials = (Essentials) essPlugin;
            getLogger().info("Detected Essentials v" + getPluginVersionSafe(essPlugin) + "; /back integration enabled.");
        } else {
            this.essentials = null;
            getLogger().warning("Essentials not found; /back integration disabled.");
        }

        // Guilds 整合（非必需）
        GuildsManager.initialize(this);
        if (GuildsManager.isAvailable()) {
            getLogger().info("Guilds integration initialized successfully (Guilds v" + getOtherPluginVersion("Guilds") + ").");
        } else {
            getLogger().info("Guilds not detected or not compatible; guild-home integration disabled.");
        }

        // PlaceholderAPI 整合
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new kevin.betterhome.integration.PlaceholderAPI(this).register();
                getLogger().info("Registered PlaceholderAPI expansion successfully.");
            } catch (Throwable t) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
            }
        } else {
            getLogger().info("PlaceholderAPI not detected; skipping expansion registration.");
        }

        // 註冊指令與補全
        CommandRegistrar.registerAll(this, new HomeTabCompleter(this));
        getLogger().info("Commands and tab-completer registered.");
        AdminList.initialize(this);

        // 註冊 GUI 事件
        getServer().getPluginManager().registerEvents(new MenuManager(this), this);
        getLogger().info("GUI event listeners registered.");

        // 啟用 bStats
        new Metrics(this, 26627);
        getLogger().info("bStats metrics initialized (service id: 26627).");

        // 廣播啟動訊息
        String userCommand = getConfig().getString("menu.open-command", "/home").replace("/", "");
        broadcastIfConfigured("startup", userCommand);

        getLogger().info("BetterHome enabled. User command: /" + userCommand);
        getLogger().info("BetterHome started successfully.");
    }

    @Override
    public void onDisable() {
        if (cacheUpdateTask != null) {
            cacheUpdateTask.cancel();
        }
        console.sendMessage("[BetterHome] Saving player homes...");
        console.sendMessage("[BetterHome] Saving configuration...");

        // 廣播關閉訊息
        broadcastIfConfigured("shutdown", null);

        getLogger().info("BetterHome has been disabled.");
    }

    // 重新註冊配置檔
    public void registerConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    // 取得傳送冷卻時間 (毫秒)
    public long getTeleportCooldownTime() {
        return getConfig().getInt("teleport-cooldown", 5) * 1000L;
    }

    // 取得玩家資料檔案
    public File getPlayerDataFile(UUID uuid) {
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    // 取得玩家家園清單
    public List<String> getHomesFor(Player player) {
        File file = getPlayerDataFile(player.getUniqueId());
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> homes = config.getStringList("homes");
        return homes != null ? homes : new ArrayList<>();
    }

    public Essentials getEssentials() {
        return essentials;
    }

    // =========================
    // 除錯 & 廣播工具
    // =========================

    // 除錯訊息
    public void logDebug(String message) {
        boolean consoleDebug = getConfig().getBoolean("log.console-debug", false);
        boolean notifyPlayers = getConfig().getBoolean("log.notify-debug-to-players", false);

        String prefix = ChatColor.GRAY + "[BetterHome DEBUG] " + ChatColor.RESET;
        String plainPrefix = "[BetterHome DEBUG] ";

        if (consoleDebug) {
            getLogger().info(plainPrefix + message);
        }
        if (notifyPlayers) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(DEBUG_PERMISSION)) {
                    p.sendMessage(prefix + ChatColor.WHITE + message);
                }
            }
        }
    }

    // 廣播啟動或關閉訊息
    private void broadcastIfConfigured(String type, String userCommand) {
        String base = "log.broadcast." + type;
        if (getConfig().getBoolean(base + ".enabled", true)) {
            String msg = getConfig().getString(base + ".message", "");
            if (userCommand != null) {
                msg = msg.replace("%command%", userCommand);
            }
            msg = ChatColor.translateAlternateColorCodes('&', msg);
            Bukkit.broadcastMessage(msg);
        } else {
            logDebug("Broadcast '" + type + "' is disabled in config.");
        }
    }

    public void loadSoundsConfig() {
        soundsFile = new File(getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            saveResource("sounds.yml", false); // 複製 jar 裡的預設檔
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
    }

    public FileConfiguration getSoundsConfig() {
        return soundsConfig;
    }

    public void saveSoundsConfig() {
        try {
            soundsConfig.save(soundsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 顏色轉換
    private String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    // 取得插件版本
    private String getOtherPluginVersion(String name) {
        Plugin p = getServer().getPluginManager().getPlugin(name);
        if (p == null) return "N/A";
        return getPluginVersionSafe(p);
    }

    private String getPluginVersionSafe(Plugin plugin) {
        try {
            return plugin.getDescription() != null && plugin.getDescription().getVersion() != null
                    ? plugin.getDescription().getVersion()
                    : "unknown";
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
