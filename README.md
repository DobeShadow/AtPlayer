# AtPlayer - @玩家提醒插件

Paper/Purpur 1.21+ 的 @提及插件，在聊天中 @玩家 即可发送 TITLE 和声音提醒。

## 功能

- **@玩家提醒** — 聊天输入 `@玩家ID`，对方收到全屏 TITLE + 声音通知
- **TAB 补全** — 输入 `@` 后按 TAB 自动补全在线玩家名
- **消息高亮** — 聊天中的 `@玩家` 自动染色
- **开关控制** — `/at toggle` 自由开关接收提醒
- **数据持久化** — 开关状态保存到文件

## 安装

1. 将 `AtPlayer-1.0.0-SNAPSHOT.jar` 放入 `plugins/` 目录
2. 重启服务器
3. 编辑 `plugins/AtPlayer/config.yml` 自定义配置

## 命令

| 命令 | 说明 |
|------|------|
| `/at` | 开关 @提醒通知 |
| `/at toggle` | 同上 |
| `/at status` | 查看当前接收状态 |
| `@玩家名` + TAB | 聊天中补全在线玩家 |

## 配置

```yaml
# plugins/AtPlayer/config.yml

mention-prefix: "@"              # @ 符号

title:
  title: "&e&l有人@你！"
  subtitle: "&f{sender} &7在聊天中提到了你"
  fade-in: 10
  stay: 40
  fade-out: 10

sound:
  type: BLOCK_NOTE_BLOCK_PLING    # 声音类型
  volume: 1.0
  pitch: 2.0

highlight-color: "&b"            # 消息中 @玩家 的染色
```

## 权限

| 权限 | 说明 | 默认 |
|------|------|------|
| `atplayer.use` | 使用 @提及 | true |
| `atplayer.notify` | 接收提醒 | true |

## 项目结构

```
AtPlayer/
├── pom.xml
└── src/main/
    ├── java/com/minemc/atplayer/
    │   ├── AtPlayer.java
    │   └── listener/
    │       └── ChatListener.java
    └── resources/
        ├── config.yml
        └── plugin.yml
```
