# BetterHome

A feature-rich Minecraft home management plugin for Spigot/Paper servers. Manage homes through an intuitive GUI or commands, share homes with other players, and let admins oversee everything — all with no required dependencies.

[繁體中文說明文件](README.zh-TW.md)

![Build](https://github.com/Kevin28576/BetterHome/actions/workflows/build.yml/badge.svg)

---

## Features

- **GUI management** — browse, teleport, rename, delete, and favourite homes from a clean inventory UI
- **Direct commands** — set and teleport to homes without opening a menu
- **Home sharing** — share individual homes with specific players and manage your share list
- **Admin tools** — create or delete homes on behalf of any player via GUI or command
- **Data import** — migrate existing home data from Essentials or HuskHomes
- **Per-player home limits** — grant different limits to different groups via permission nodes (`betterhome.maxhomes.<n>`); works with any permission plugin
- **Cooldown system** — configurable teleport cooldown with movement cancellation
- **World restrictions** — blacklist worlds where homes cannot be set
- **PlaceholderAPI support** — expose home data to scoreboards, chat, etc.
- **Optional Guilds integration** — shows guild-home UI slots when the Guilds plugin is present; silently disabled when it isn't
- **bStats analytics** — anonymous usage statistics (Service ID: 26627)

---

## Requirements

| Requirement | Version |
|---|---|
| Java | 16+ |
| Spigot / Paper | 1.14+ |

No plugins are required. All integrations below are optional.

---

## Optional Integrations

| Plugin | Purpose |
|---|---|
| [EssentialsX](https://essentialsx.net/) | Powers `/back` after teleportation |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Exposes `%betterhome_*%` placeholders |
| [Guilds](https://www.spigotmc.org/resources/guilds.48920/) | Adds a guild-home button in the homes GUI |

---

## Installation

1. Drop `BetterHome-<version>.jar` into your server's `plugins/` folder.
2. Start (or restart) the server — config files are generated automatically.
3. Edit `plugins/BetterHome/config.yml` to suit your server.
4. Install any optional integrations you want, then restart.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/home` | Open the main menu / run sub-commands | `betterhome.use` |
| `/homes` | Open the homes list GUI | `betterhome.use` |
| `/homegui` | Open the homes GUI directly | `betterhome.use` |
| `/gohome <name>` | Teleport to a home | `betterhome.use` |
| `/sethome <name>` | Set a home at your current location | `betterhome.use` |
| `/home create <name>` | Create a home | `betterhome.use` |
| `/home tp <name\|owner:name>` | Teleport to your home or a shared home | `betterhome.use` |
| `/home rename <old> <new>` | Rename a home | `betterhome.use` |
| `/home delete <name>` | Delete a home | `betterhome.use` |
| `/home share <name> <player>` | Share a home with a player | `betterhome.share` |
| `/home unshare <name> <player>` | Remove a player's access to a shared home | `betterhome.share` |
| `/home sharelist` | View your shared homes | `betterhome.share` |
| `/home import <Essentials\|HuskHomes>` | Import homes from another plugin | `betterhome.import.*` |
| `/home admin create <player> <name> [x y z]` | Create a home for another player | `betterhome.admin` |
| `/home admin delete <player> <name>` | Delete a home owned by another player | `betterhome.admin` |
| `/home reload` | Reload the configuration | `betterhome.reload` |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `betterhome.use` | `true` | Access all basic home commands |
| `betterhome.maxhomes.<n>` | — | Allow up to `n` homes (highest granted value wins) |
| `betterhome.share` | `true` | Share homes with other players |
| `betterhome.cooldown.bypass` | `op` | Skip the teleport cooldown |
| `betterhome.world.bypass.<world>` | `op` | Set homes in blacklisted worlds |
| `betterhome.reload` | `op` | Reload the plugin configuration |
| `betterhome.admin` | `op` | Manage homes for other players |
| `betterhome.import.essentials` | `op` | Import data from Essentials |
| `betterhome.import.huskhomes` | `op` | Import data from HuskHomes |
| `betterhome.debug` | `op` | Enable debug logging |

### Setting per-group home limits

Assign `betterhome.maxhomes.<n>` to a group in your permission plugin. The plugin reads all effective permissions and uses the highest value. No LuckPerms API is required — any permission plugin works.

```
# Example (LuckPerms command)
/lp group vip permission set betterhome.maxhomes.10 true
/lp group mvp permission set betterhome.maxhomes.25 true
```

The fallback when no `maxhomes` permission is granted is controlled by `default-max-homes` in `config.yml`.

---

## PlaceholderAPI Placeholders

Identifier: `betterhome`

| Placeholder | Description |
|---|---|
| `%betterhome_count%` | Number of homes the player has set |
| `%betterhome_max%` | Maximum homes the player is allowed |
| `%betterhome_free%` | Remaining home slots |
| `%betterhome_favorite%` | Name of the player's favourite home |
| `%betterhome_cooldown%` | Remaining cooldown in seconds |
| `%betterhome_cooldown_formatted%` | Remaining cooldown formatted as `mm:ss` |
| `%betterhome_command%` | Command to open the homes menu |
| `%betterhome_exists_<name>%` | Whether a home named `<name>` exists (`true`/`false`) |
| `%betterhome_world_<name>%` | World of home `<name>` |
| `%betterhome_x_<name>%` | X coordinate of home `<name>` |
| `%betterhome_y_<name>%` | Y coordinate of home `<name>` |
| `%betterhome_z_<name>%` | Z coordinate of home `<name>` |
| `%betterhome_yaw_<name>%` | Yaw of home `<name>` |
| `%betterhome_pitch_<name>%` | Pitch of home `<name>` |
| `%betterhome_icon_<name>%` | Icon material of home `<name>` |
| `%betterhome_loc_<name>%` | Location string of home `<name>` (`world x y z`) |

---

## Configuration

| File | Purpose |
|---|---|
| `plugins/BetterHome/config.yml` | Main configuration (Chinese locale) |
| `plugins/BetterHome/config_en.yml` | Reference configuration (English) |
| `plugins/BetterHome/sounds.yml` | UI and teleport sound settings |
| `plugins/BetterHome/data/<uuid>.yml` | Per-player home data |

Key settings in `config.yml`:

| Key | Description |
|---|---|
| `default-max-homes` | Fallback home limit when no permission node is set |
| `teleport-cooldown` | Cooldown in seconds between teleports |
| `cancel-on-move` | Cancel a pending teleport if the player moves |
| `blacklisted-worlds` | List of world names where homes cannot be set |
| `log.*` | Toggle various startup/broadcast/debug log entries |

---

## Building from Source

```bash
git clone https://github.com/Kevin28576/BetterHome.git
cd BetterHome
mvn -B -DskipTests package
# Output: target/BetterHome-<version>.jar
```

Requires **Java 16** and **Maven 3.6+**. A pre-built JAR is uploaded as a CI artifact on every push.

---

## License

All Rights Reserved. You may run this plugin on your own Minecraft server, but you may not redistribute, modify, decompile, or use it commercially without prior written permission. See [`LICENSE`](LICENSE) for the full terms.
