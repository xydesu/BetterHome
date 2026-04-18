# BetterHome

![Build](https://github.com/xydesu/BetterHome/actions/workflows/build.yml/badge.svg)

BetterHome 是一款適用於 Spigot/Paper 伺服器的 Minecraft 家園管理插件。  
玩家可透過指令或 GUI 介面來建立、管理與分享家園位置。

## 版本

目前版本：**1.7.2**

## 功能特色

- 透過 GUI 介面與指令管理家園
- 玩家之間可互相分享家園
- 管理員家園管理工具
- 支援從 Essentials / HuskHomes 匯入家園資料
- 使用權限節點設定每位玩家的家園數量上限
- 支援傳送冷卻時間與世界黑名單
- 可選擇整合 PlaceholderAPI 與 Guilds 插件

## 系統需求

- Java 16 以上
- Spigot / Paper 1.13 以上

## 安裝方式

1. 從 [Releases 頁面](https://github.com/xydesu/BetterHome/releases) 下載 jar 檔案。
2. 將其放入伺服器的 `plugins/` 資料夾。
3. 重新啟動伺服器。
4. 如有需要，編輯 `plugins/BetterHome/config.yml` 進行設定。

## 主要指令

- `/home`
- `/homes`
- `/sethome <名稱>`
- `/gohome <名稱>`
- `/home share <名稱> <玩家>`
- `/home admin create <玩家> <名稱>`

## 主要權限

- `betterhome.use`
- `betterhome.share`
- `betterhome.admin`
- `betterhome.reload`
- `betterhome.maxhomes.<n>`

## 從原始碼建置

```bash
mvn -B -DskipTests package
```

## 相關連結

- 儲存庫：[xydesu/BetterHome](https://github.com/xydesu/BetterHome)
- 問題回報：[Issue Tracker](https://github.com/xydesu/BetterHome/issues)

## 授權條款

保留所有權利。詳見 [LICENSE](LICENSE)。
