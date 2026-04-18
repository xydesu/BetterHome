# BetterHome

適用於 Spigot/Paper 伺服器的功能完整 Minecraft 家園管理插件。你可以透過直覺的 GUI 或指令管理家園、與其他玩家分享家園，並讓管理員統一維護——且不需要任何必裝依賴。

![Build](https://github.com/xydesu/BetterHome/actions/workflows/build.yml/badge.svg)

---

## 功能特色

- **GUI 管理**：在整潔的物品欄介面中瀏覽、傳送、重新命名、刪除與收藏家園
- **直接指令**：不開選單也能快速設定家園與傳送
- **家園分享**：可將指定家園分享給特定玩家並管理分享清單
- **管理員工具**：管理員可透過 GUI 或指令替任意玩家建立或刪除家園
- **資料匯入**：可從 Essentials 或 HuskHomes 匯入既有家園資料
- **每玩家家園上限**：可透過權限節點 (`betterhome.maxhomes.<n>`) 針對不同群組設定上限；相容任意權限插件
- **冷卻系統**：可設定傳送冷卻時間，並支援移動後取消傳送
- **世界限制**：可封鎖指定世界，禁止設定家園
- **PlaceholderAPI 支援**：可將家園資料顯示在計分板、聊天等位置
- **選用 Guilds 整合**：安裝 Guilds 後會顯示公會家園 UI 欄位；未安裝時會自動停用
- **bStats 統計**：匿名使用統計（Service ID: 26627）

---

## 需求環境

| 需求 | 版本 |
|---|---|
| Java | 16+ |
| Spigot / Paper | 1.14+ |

本插件不需要任何必裝插件，以下整合皆為選用。

---

## 選用整合

| 插件 | 用途 |
|---|---|
| [EssentialsX](https://essentialsx.net/) | 傳送後提供 `/back` 功能 |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | 提供 `%betterhome_*%` 佔位符 |
| [Guilds](https://www.spigotmc.org/resources/guilds.48920/) | 在家園 GUI 新增公會家園按鈕 |

---

## 安裝方式

1. 將 `BetterHome-<version>.jar` 放入伺服器 `plugins/` 目錄。
2. 啟動（或重新啟動）伺服器，設定檔會自動產生。
3. 依需求修改 `plugins/BetterHome/config.yml`。
4. 安裝你需要的選用整合後再次重啟伺服器。

---

## 指令

| 指令 | 說明 | 權限 |
|---|---|---|
| `/home` | 開啟主選單／執行子指令 | `betterhome.use` |
| `/homes` | 開啟家園清單 GUI | `betterhome.use` |
| `/homegui` | 直接開啟家園 GUI | `betterhome.use` |
| `/gohome <name>` | 傳送到家園 | `betterhome.use` |
| `/sethome <name>` | 在目前位置設定家園 | `betterhome.use` |
| `/home create <name>` | 建立家園 | `betterhome.use` |
| `/home tp <name\|owner:name>` | 傳送到自己的家園或他人分享的家園 | `betterhome.use` |
| `/home rename <old> <new>` | 重新命名家園 | `betterhome.use` |
| `/home delete <name>` | 刪除家園 | `betterhome.use` |
| `/home share <name> <player>` | 與指定玩家分享家園 | `betterhome.share` |
| `/home unshare <name> <player>` | 取消玩家對共享家園的存取權 | `betterhome.share` |
| `/home sharelist` | 檢視你分享出去的家園 | `betterhome.share` |
| `/home import <Essentials\|HuskHomes>` | 從其他插件匯入家園 | `betterhome.import.*` |
| `/home admin create <player> <name> [x y z]` | 替其他玩家建立家園 | `betterhome.admin` |
| `/home admin delete <player> <name>` | 刪除其他玩家的家園 | `betterhome.admin` |
| `/home reload` | 重新載入設定 | `betterhome.reload` |

---

## 權限節點

| 權限 | 預設 | 說明 |
|---|---|---|
| `betterhome.use` | `true` | 使用所有基本家園指令 |
| `betterhome.maxhomes.<n>` | — | 允許最多 `n` 個家園（以最高數值為準） |
| `betterhome.share` | `true` | 與其他玩家分享家園 |
| `betterhome.cooldown.bypass` | `op` | 無視傳送冷卻時間 |
| `betterhome.world.bypass.<world>` | `op` | 在被封鎖世界中仍可設定家園 |
| `betterhome.reload` | `op` | 重新載入插件設定 |
| `betterhome.admin` | `op` | 管理其他玩家家園 |
| `betterhome.import.essentials` | `op` | 匯入 Essentials 資料 |
| `betterhome.import.huskhomes` | `op` | 匯入 HuskHomes 資料 |
| `betterhome.debug` | `op` | 啟用除錯日誌 |

### 設定群組家園上限

在你的權限插件中為群組賦予 `betterhome.maxhomes.<n>`。插件會讀取玩家所有生效權限，並採用最高值。不需 LuckPerms API，任何權限插件皆可運作。

```bash
# 範例（LuckPerms 指令）
/lp group vip permission set betterhome.maxhomes.10 true
/lp group mvp permission set betterhome.maxhomes.25 true
```

若沒有任何 `maxhomes` 權限，將使用 `config.yml` 中的 `default-max-homes` 作為預設值。

---

## PlaceholderAPI 佔位符

識別符：`betterhome`

| 佔位符 | 說明 |
|---|---|
| `%betterhome_count%` | 玩家已設定的家園數量 |
| `%betterhome_max%` | 玩家可擁有的家園上限 |
| `%betterhome_free%` | 剩餘家園欄位數 |
| `%betterhome_favorite%` | 玩家收藏家園名稱 |
| `%betterhome_cooldown%` | 剩餘冷卻秒數 |
| `%betterhome_cooldown_formatted%` | 格式化冷卻時間（`mm:ss`） |
| `%betterhome_command%` | 開啟家園選單的指令 |
| `%betterhome_exists_<name>%` | 名為 `<name>` 的家園是否存在（`true`/`false`） |
| `%betterhome_world_<name>%` | 家園 `<name>` 所在世界 |
| `%betterhome_x_<name>%` | 家園 `<name>` 的 X 座標 |
| `%betterhome_y_<name>%` | 家園 `<name>` 的 Y 座標 |
| `%betterhome_z_<name>%` | 家園 `<name>` 的 Z 座標 |
| `%betterhome_yaw_<name>%` | 家園 `<name>` 的 Yaw |
| `%betterhome_pitch_<name>%` | 家園 `<name>` 的 Pitch |
| `%betterhome_icon_<name>%` | 家園 `<name>` 的圖示材質 |
| `%betterhome_loc_<name>%` | 家園 `<name>` 的位置字串（`world x y z`） |

---

## 設定檔

| 檔案 | 用途 |
|---|---|
| `plugins/BetterHome/config.yml` | 主要設定（中文語系） |
| `plugins/BetterHome/config_en.yml` | 英文參考設定 |
| `plugins/BetterHome/sounds.yml` | UI 與傳送音效設定 |
| `plugins/BetterHome/data/<uuid>.yml` | 玩家家園資料 |

`config.yml` 重要設定：

| 鍵值 | 說明 |
|---|---|
| `default-max-homes` | 未設定權限節點時的家園上限預設值 |
| `teleport-cooldown` | 傳送間隔冷卻秒數 |
| `cancel-on-move` | 玩家移動時是否取消待處理傳送 |
| `blacklisted-worlds` | 禁止設定家園的世界名稱清單 |
| `log.*` | 控制各類啟動／廣播／除錯日誌輸出 |

---

## 從原始碼建置

```bash
git clone https://github.com/Kevin28576/BetterHome.git
cd BetterHome
mvn -B -DskipTests package
# 輸出：target/BetterHome-<version>.jar
```

需要 **Java 16** 與 **Maven 3.6+**。每次 push 都會在 CI Artifact 提供預建 JAR。

---

## 授權

保留所有權利。你可以在自己的 Minecraft 伺服器使用本插件，但未經書面許可不得重新散佈、修改、反編譯或商業使用。完整條款請見 [`LICENSE`](LICENSE)。
