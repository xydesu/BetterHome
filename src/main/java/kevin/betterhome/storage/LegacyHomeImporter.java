package kevin.betterhome.storage;

import kevin.betterhome.BetterHome;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LegacyHomeImporter {
    ConsoleCommandSender console = Bukkit.getConsoleSender();
    private final BetterHome plugin;

    public LegacyHomeImporter(BetterHome plugin) {
        this.plugin = plugin;
    }

    /**
     * 從 Essentials 的 userdata 中匯入所有玩家的 home 資料
     */
    public void importHomesFromEssentialsForAllPlayers(CommandSender sender) {
        File essentialsDir = new File(plugin.getDataFolder().getParentFile(), "Essentials/userdata");
        File pluginDataDir = new File(plugin.getDataFolder(), "data");

        if (!essentialsDir.exists() || !essentialsDir.isDirectory()) {
            String message = "找不到 Essentials 的 userdata 資料夾：" + essentialsDir.getAbsolutePath();
            sender.sendMessage(ChatColor.RED + message);
            plugin.getLogger().warning(message);
            return;
        }

        File[] playerFiles = essentialsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null || playerFiles.length == 0) {
            String message = "Essentials/userdata 中沒有找到任何玩家資料。";
            sender.sendMessage(ChatColor.RED + message);
            plugin.getLogger().warning(message);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "開始從 Essentials 匯入所有玩家的 Home 資料...");

        for (File essentialsPlayerFile : playerFiles) {
            try {
                FileConfiguration essentialsConfig = YamlConfiguration.loadConfiguration(essentialsPlayerFile);
                ConfigurationSection homesSection = essentialsConfig.getConfigurationSection("homes");

                if (homesSection == null) {
                    String message = "此玩家檔案未包含任何 Home：" + essentialsPlayerFile.getName();
                    sender.sendMessage(ChatColor.YELLOW + message);
                    plugin.getLogger().info(message);
                    continue;
                }

                String uuidString = essentialsPlayerFile.getName().replace(".yml", "");
                File pluginPlayerFile = new File(pluginDataDir, uuidString + ".yml");

                if (!pluginPlayerFile.exists()) {
                    pluginPlayerFile.getParentFile().mkdirs();
                    pluginPlayerFile.createNewFile();
                }

                FileConfiguration pluginConfig = YamlConfiguration.loadConfiguration(pluginPlayerFile);

                List<String> homeList = pluginConfig.getStringList("homes");
                if (homeList == null) {
                    homeList = new ArrayList<>();
                }

                for (String homeName : homesSection.getKeys(false)) {
                    if (!homeList.contains(homeName)) {
                        homeList.add(homeName);

                        ConfigurationSection homeData = homesSection.getConfigurationSection(homeName);
                        ConfigurationSection newHomeData = pluginConfig.createSection(homeName);
                        newHomeData.set("world", homeData.getString("world-name"));
                        newHomeData.set("x", homeData.getDouble("x"));
                        newHomeData.set("y", homeData.getDouble("y"));
                        newHomeData.set("z", homeData.getDouble("z"));
                        newHomeData.set("yaw", homeData.getDouble("yaw"));
                        newHomeData.set("pitch", homeData.getDouble("pitch"));
                    }
                }

                pluginConfig.set("homes", homeList);
                pluginConfig.save(pluginPlayerFile);

                String message = "成功匯入玩家 " + uuidString + " 的 Home。";
                sender.sendMessage(ChatColor.GREEN + message);
                plugin.getLogger().info(message);

            } catch (Exception e) {
                String message = "匯入玩家檔案時發生錯誤：" + essentialsPlayerFile.getName();
                sender.sendMessage(ChatColor.RED + message);
                plugin.getLogger().warning(message);
                e.printStackTrace();
            }
        }

        sender.sendMessage(ChatColor.GREEN + "所有玩家的 Home 已成功從 Essentials 匯入！");
        plugin.getLogger().info("所有玩家的 Home 已成功從 Essentials 匯入！");
    }

    /**
     * 從 HuskHomes 的 SQLite 資料庫中匯入所有玩家的 home 資料
     */
    public void importHomesFromHuskHomesForAllPlayers(CommandSender sender, String huskHomesDbPath) {
        File huskHomesDbFile = new File(plugin.getDataFolder().getParentFile(), huskHomesDbPath);
        File pluginDataDir = new File(plugin.getDataFolder(), "data");

        console.sendMessage(String.valueOf(pluginDataDir));
        console.sendMessage(String.valueOf(huskHomesDbFile));

        if (!huskHomesDbFile.exists()) {
            String message = "找不到 HuskHomes 的資料庫檔案：" + huskHomesDbFile.getAbsolutePath();
            sender.sendMessage(ChatColor.RED + message);
            plugin.getLogger().warning(message);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "開始從 HuskHomes 匯入所有玩家的 Home 資料...");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + huskHomesDbFile.getAbsolutePath())) {
            String query =
                    "SELECT h.owner_uuid, s.name, p.x, p.y, p.z, p.yaw, p.pitch, p.world_name " +
                            "FROM huskhomes_homes h " +
                            "JOIN huskhomes_saved_positions s ON h.saved_position_id = s.id " +
                            "JOIN huskhomes_position_data p ON s.position_id = p.id";

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String ownerUUID = resultSet.getString("owner_uuid");
                    String homeName = resultSet.getString("name");
                    double x = resultSet.getDouble("x");
                    double y = resultSet.getDouble("y");
                    double z = resultSet.getDouble("z");
                    float yaw = resultSet.getFloat("yaw");
                    float pitch = resultSet.getFloat("pitch");
                    String worldName = resultSet.getString("world_name");

                    File playerFile = new File(pluginDataDir, ownerUUID + ".yml");

                    // 如果資料檔不存在，建立它
                    if (!playerFile.exists()) {
                        playerFile.getParentFile().mkdirs();
                        playerFile.createNewFile();
                    }

                    FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

                    List<String> homeList = playerConfig.getStringList("homes");
                    if (homeList == null) {
                        homeList = new ArrayList<>();
                    }

                    // 如果該 home 尚未存在，就新增
                    if (!homeList.contains(homeName)) {
                        homeList.add(homeName);
                        playerConfig.set("homes", homeList);

                        String homePath = homeName;
                        playerConfig.set(homePath + ".world", worldName);
                        playerConfig.set(homePath + ".x", x);
                        playerConfig.set(homePath + ".y", y);
                        playerConfig.set(homePath + ".z", z);

                        playerConfig.save(playerFile);
                    }
                }
            }

            sender.sendMessage(ChatColor.GREEN + "所有玩家的 Home 已成功從 HuskHomes 匯入！");
            plugin.getLogger().info("所有玩家的 Home 已成功從 HuskHomes 匯入！");

        } catch (Exception e) {
            String message = "匯入 HuskHomes 時發生錯誤：" + e.getMessage();
            sender.sendMessage(ChatColor.RED + message);
            plugin.getLogger().warning(message);
            e.printStackTrace();
        }
    }
}
