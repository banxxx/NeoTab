# 安装

## 服务端安装

将 `neotab-x.x.x.jar` 放入服务器的 `mods/` 目录，重启服务器即可。

## 客户端安装（管理员）

如果需要使用图形化配置界面（`/neotab config`），管理员客户端也需要安装此模组。

> **普通玩家无需安装**，只需服务端安装即可正常查看 TAB 信息。

## 为什么管理员需要安装客户端

因为配置界面是一个 GUI 界面：

- 服务端负责发出"打开配置界面"的指令
- 客户端必须持有这个界面的代码

因此当前模式是：服务端主控，客户端负责打开和显示 GUI。

## 配置文件位置

配置文件保存在世界存档目录下：

```
<世界存档>/serverconfig/neotab-tab.json
```

## 配置文件字段说明

```json
{
  "topTitleEnabled": true,
  "topTitleText": "NeoTab",
  "topContentEnabled": true,
  "topContentText": "Server status overview",
  "betterPingEnabled": true,
  "onlineDurationEnabled": false,
  "titleEnabled": false,
  "footerCustomText": "",
  "footerTpsEnabled": true,
  "footerMsptEnabled": true,
  "footerOnlineEnabled": true,
  "refreshIntervalTicks": 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `topTitleEnabled` | boolean | 是否显示顶部标题 |
| `topTitleText` | string | 顶部标题文字（最多 32 可见字符） |
| `topContentEnabled` | boolean | 是否显示顶部内容 |
| `topContentText` | string | 顶部内容文字（最多 256 可见字符） |
| `betterPingEnabled` | boolean | 是否启用更好的延迟显示 |
| `onlineDurationEnabled` | boolean | 是否显示在线时长 |
| `titleEnabled` | boolean | 是否启用称号功能 |
| `footerCustomText` | string | 底部自定义文字（最多 256 可见字符） |
| `footerTpsEnabled` | boolean | 是否显示 TPS |
| `footerMsptEnabled` | boolean | 是否显示 MSPT |
| `footerOnlineEnabled` | boolean | 是否显示在线人数 |
| `refreshIntervalTicks` | int | 刷新间隔（ticks，范围 20-200，默认 20） |

## 权限系统

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `neotab:configure` | 允许使用 `/neotab config` 和 `/neotab reload` | OP 2级 |

如果服务器安装了支持 NeoForge 权限 API 的权限管理模组，可以通过 `neotab:configure` 节点精细控制权限。未安装权限模组时，自动回退到原版 OP 2 级判断。
