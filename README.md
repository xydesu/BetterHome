# BetterHome

Minecraft 家園系統外掛，提供 GUI 管理、指令操作、分享與管理功能。

## 功能

- GUI 管理家與列表、設定最愛
- 直接設置與傳送到指定家
- 支援分享家給其他玩家並管理分享
- 管理員可代玩家建立/刪除家
- 匯入 Essentials 或 HuskHomes 的家資料

## 指令

| 指令 | 說明 | 權限 |
| --- | --- | --- |
| `/home` | 開啟主選單與子指令 | `betterhome.use` |
| `/homes` | 開啟家列表 | `betterhome.use` |
| `/homegui` | 開啟 GUI | `betterhome.use` |
| `/gohome <name>` | 直接傳送到家 | `betterhome.use` |
| `/sethome <name>` | 直接設置家 | `betterhome.use` |
| `/home create <HomeName>` | 建立家 | `betterhome.use` |
| `/home tp <HomeName|OwnerName:HomeName>` | 傳送到家/共享家 | `betterhome.use` |
| `/home rename <OldName> <NewName>` | 重新命名家 | `betterhome.use` |
| `/home delete <HomeName>` | 刪除家 | `betterhome.use` |
| `/home reload` | 重載設定 | `betterhome.reload` |
| `/home import <Essentials|HuskHomes>` | 匯入舊資料 | `betterhome.import.*` |
| `/home share <HomeName> <Player>` | 分享家 | `betterhome.share` |
| `/home unshare <HomeName> <Player>` | 取消分享 | `betterhome.share` |
| `/home shareslist` | 查看分享清單 | `betterhome.share` |
| `/home admin create <Player> <HomeName> [x y z]` | 管理員建立家 | `betterhome.admin` |
| `/home admin delete <Player> <HomeName>` | 管理員刪除家 | `betterhome.admin` |

## 權限

- `betterhome.use`: 基本功能 (預設 true)
- `betterhome.share`: 分享功能 (預設 true)
- `betterhome.cooldown.bypass`: 無視冷卻 (預設 op)
- `betterhome.world.bypass.worldname`: 無視世界限制 (預設 op)
- `betterhome.reload`: 重載設定 (預設 op)
- `betterhome.admin`: 管理權限 (預設 op)
- `betterhome.import.essentials`: 匯入 Essentials (預設 op)
- `betterhome.import.huskhomes`: 匯入 HuskHomes (預設 op)
- `betterhome.debug`: 除錯 (預設 op)

## PlaceholderAPI 參數

識別字：`betterhome`

- `%betterhome_count%` 家數量
- `%betterhome_max%` 可用最大家數
- `%betterhome_free%` 剩餘可設家數
- `%betterhome_favorite%` 最愛家名稱
- `%betterhome_cooldown%` 冷卻秒數
- `%betterhome_cooldown_formatted%` 冷卻時間 (mm:ss)
- `%betterhome_command%` 開啟選單指令
- `%betterhome_exists_<name>%` 指定家是否存在 (true/false)
- `%betterhome_world_<name>%` 家世界
- `%betterhome_x_<name>%` / `%betterhome_y_<name>%` / `%betterhome_z_<name>%` 座標
- `%betterhome_yaw_<name>%` / `%betterhome_pitch_<name>%` 朝向
- `%betterhome_icon_<name>%` 圖示材質
- `%betterhome_loc_<name>%` 位置字串 (world x y z)

## 設定與資料

- 設定檔：`plugins/BetterHome/config.yml`
- 英文設定範本：`plugins/BetterHome/config_en.yml`
- 音效設定：`plugins/BetterHome/sounds.yml`
- 玩家資料：`plugins/BetterHome/data/<uuid>.yml`

常見設定項目包含：
- `default-max-homes` 預設可設家數
- `teleport-cooldown` 傳送冷卻秒數
- `cancel-on-move` 移動是否取消傳送
- `blacklisted-worlds` 禁止設家的世界
- `log.*` 啟動/關閉廣播與除錯
- `sounds.yml` UI 與傳送音效

## 依賴

- 相容 Spigot / Paper (API 1.13+)
- 必要依賴: LuckPerms, Guilds
- 選用依賴: Essentials, PlaceholderAPI

## 整合

- Essentials: 連動 `/back` 回傳功能
- PlaceholderAPI: 提供 BetterHome 變數
- bStats: 啟用匿名統計 (Service ID: 26627)

## 安裝

1. 將外掛 jar 放入 `plugins/`
2. 視需求安裝依賴外掛
3. 啟動伺服器產生設定檔並調整

## 開發

- Java 16
- Maven 建置: `mvn -DskipTests package`
- 產物輸出: `target/BetterHome-<version>.jar`

## 使用條款

本專案為「完全保留權利」(All Rights Reserved)。

未經事先書面許可，不得：
- 轉載、再發布、分發或提供下載
- 修改、反編譯、反向工程、改作、衍生或二次創作
- 用於任何商業目的或收費服務

允許：僅供在自有或已獲授權的 Minecraft 伺服器上運行本外掛。  
詳情請見 `LICENSE`。
