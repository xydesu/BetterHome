# BetterHome

![Build](https://github.com/xydesu/BetterHome/actions/workflows/build.yml/badge.svg)

BetterHome is a Minecraft home-management plugin for Spigot/Paper servers.  
It lets players create, manage, and share homes using commands or a GUI.

## Version

Current version: **1.7.2**

## Features

- Home management via GUI and commands
- Home sharing between players
- Admin home management tools
- Essentials/HuskHomes import support
- Per-player home limits using permissions
- Teleport cooldown and world blacklist support
- Optional PlaceholderAPI and Guilds integrations

## Requirements

- Java 16+
- Spigot/Paper 1.13+

## Installation

1. Download the jar from the [Releases page](https://github.com/xydesu/BetterHome/releases).
2. Put it in your server `plugins/` folder.
3. Restart the server.
4. Edit `plugins/BetterHome/config.yml` if needed.

## Main Commands

- `/home`
- `/homes`
- `/sethome <name>`
- `/gohome <name>`
- `/home share <name> <player>`
- `/home admin create <player> <name>`

## Main Permissions

- `betterhome.use`
- `betterhome.share`
- `betterhome.admin`
- `betterhome.reload`
- `betterhome.maxhomes.<n>`

## Build from Source

```bash
mvn -B -DskipTests package
```

## Links

- Repository: [xydesu/BetterHome](https://github.com/xydesu/BetterHome)
- Issues: [Issue Tracker](https://github.com/xydesu/BetterHome/issues)

## License

All Rights Reserved. See [LICENSE](LICENSE).
