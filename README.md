# NeoTab

**NeoTab** 是一个面向 Minecraft 1.21.1 NeoForge 的 TAB 玩家列表增强模组，让服务器管理员能够在 TAB 界面中展示更丰富的服务器信息，并通过图形化配置界面进行实时调整。

---

## 目录

- [功能特性](#功能特性)
- [安装要求](#安装要求)
- [安装方法](#安装方法)
- [使用方法](#使用方法)
- [配置界面](#配置界面)
- [富文本标签](#富文本标签)
- [占位符参考](#占位符参考)
- [配置文件](#配置文件)
- [权限系统](#权限系统)
- [开发者 API](#开发者-api)

---

## 功能特性

### 顶部信息

在 TAB 界面顶部显示自定义内容：

- **标题信息**：单行标题，最多 32 个可见字符，支持富文本标签
- **内容信息**：多行内容，最多 256 个可见字符，支持富文本标签和占位符

### 玩家列表增强

- **称号功能**：在玩家名称前显示称号，支持其他模组通过 API 提供称号数据
- **更好的延迟显示**：在玩家名后直接显示 `xxms`，按延迟大小分为 4 档颜色，同时隐藏原版延迟信号图标
- **在线时长显示**：在玩家名后显示本次会话在线时长，格式如 `1h`、`5h`、`1d1h`

### 底部信息

底部信息栏可按模块独立开关：

- **TPS 信息**：服务器每秒 Tick 数
- **MSPT 信息**：每 Tick 毫秒数
- **在线人数**：当前在线 / 最大人数
- **自定义信息**：自定义底部文字，最多 256 个可见字符，支持富文本标签和占位符

### 其他特性

- **单人世界 TAB 修复**：原版在单人世界只有 1 名玩家时会隐藏 TAB 列表，NeoTab 修复了这一问题，只要有顶部或底部内容就会正常显示
- **AE2 风格配置界面**：参考 Applied Energistics 2 的 UI 风格，提供美观的图形化配置界面
- **实时刷新**：配置保存后立即对所有在线玩家生效

---

## 安装要求

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.227 或更高 |
| Java | 21 或更高 |

---

## 安装方法

### 服务端

将 `neotab-x.x.x.jar` 放入服务器的 `mods/` 目录，重启服务器即可。

### 客户端（管理员）

如果需要使用图形化配置界面（`/neotab config`），管理员客户端也需要安装此模组。

> **普通玩家无需安装**，只需服务端安装即可正常查看 TAB 信息。

---

## 使用方法

### 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/neotab config` | 打开图形化配置界面 | 管理员 |
| `/neotab reload` | 从磁盘重新加载配置并立即刷新 | 管理员 |
| `/neotab customize` | 打开个人自定义界面 | 所有玩家（默认） |

### 快速开始

1. 进入服务器后，管理员执行 `/neotab config`
2. 在配置界面的 **页面配置** 标签页中调整各项设置
3. 点击 **完成** 保存，配置立即对所有在线玩家生效

---

## 配置界面

配置界面分为三个标签页：

### 页面配置

包含所有 TAB 显示内容的设置：

**顶部信息**
- 标题信息开关 + 输入框（最多 32 个可见字符）
- 内容信息开关 + 输入框（最多 256 个可见字符）

**玩家列表**
- 称号功能开关
- 更好的延迟显示开关
- 在线时长显示开关

**底部信息**
- 自定义信息输入框（最多 256 个可见字符）
- TPS 信息 / MSPT 信息 / 在线人数 独立开关

### 主题样式

主题切换及自定义主题配置。

### 权限配置（仅管理员可见）

管理员可在此标签页配置玩家自定义权限策略：

- **全局策略**：对所有普通玩家生效，逐项开放或锁定 15 个可自定义选项
- **个人专属策略**：搜索指定玩家名，为该玩家单独配置策略，优先级高于全局策略

> 点击"权限配置"标签时会再次验证管理员权限（OP 2 级）。

---

## 富文本标签

输入框中支持以下富文本标签，可与占位符混合使用：

### 样式标签

```
<bold>粗体文字</bold>
<italic>斜体文字</italic>
<underlined>下划线文字</underlined>
<strikethrough>删除线文字</strikethrough>
<obfuscated>混淆文字</obfuscated>
<reset>
```

### 颜色标签

```
<color #FF0000>红色文字</color>
<color #00FF00>绿色文字</color>
<color #0000FF>蓝色文字</color>
```

颜色格式为 `#RRGGBB`（6位十六进制）或 `#AARRGGBB`（8位，含透明度）。

### 渐变标签

```
<gradient #FF0000,#0000FF>红蓝渐变</gradient>
<gradient #FF0000,#00FF00,#0000FF>RGB三色渐变</gradient>
```

支持 2 个或更多颜色值，用逗号分隔。

### 组合使用示例

```
<gradient #FFD700,#FFA500><bold>服务器名称</bold></gradient>
<color #00FF00>在线: %online%/%max_players%</color>
<color #FFFF55>欢迎 %viewer_name% 加入！</color>
```

---

## 占位符参考

所有占位符均可在顶部内容和底部自定义信息中使用。

### 玩家信息

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%player_name%` | 目标玩家名称 | `Steve` |
| `%player_ping%` | 目标玩家延迟（ms） | `45` |
| `%viewer_name%` | 查看者名称 | `Alex` |
| `%viewer_ping%` | 查看者延迟（ms） | `32` |

### 服务器状态

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%online%` | 当前在线人数 | `15` |
| `%max_players%` | 最大玩家数 | `100` |
| `%tps%` | 服务器 TPS | `19.85` |
| `%mspt%` | 每 Tick 毫秒数 | `45.23` |

### 内存使用

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%memory_used%` | 已使用内存（MB） | `2048` |
| `%memory_max%` | 最大可用内存（MB） | `4096` |
| `%memory_total%` | 内存使用情况 | `2048/4096MB` |
| `%memory_percent%` | 内存使用百分比 | `50.0%` |

### 运行时间

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%uptime%` | 服务器运行时间 | `2d 5h 30m` |
| `%uptime_days%` | 运行天数 | `2` |
| `%uptime_hours%` | 运行总小时数 | `53` |

### 世界信息

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%world_time%` | 主世界时间（24小时制） | `14:30` |
| `%world_day%` | 主世界天数 | `127` |
| `%loaded_chunks%` | 已加载区块数 | `1523` |

### 配置示例

**简洁风格**
```
在线: %online%/%max_players% | TPS: %tps% | 内存: %memory_percent%
```

**详细监控面板**
```
<gradient #FFD700,#FFA500><bold>服务器状态</bold></gradient>
TPS: %tps%  MSPT: %mspt%
内存: %memory_total% (%memory_percent%)
运行: %uptime% | 在线: %online%/%max_players%
```

---

## 配置文件

配置文件保存在世界存档目录下：

```
<世界存档>/serverconfig/neotab-tab.json
```

### 字段说明

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

---

## 权限系统

### 管理员权限

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `neotab:configure` | 允许使用 `/neotab config` 和 `/neotab reload` | OP 2级 |

### 玩家自定义权限

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `neotab:customize` | 允许使用 `/neotab customize` 打开个人自定义界面 | 所有玩家 |
| `neotab:customize.top_title.toggle` | 允许玩家开关顶部标题 | false |
| `neotab:customize.top_title.edit` | 允许玩家编辑顶部标题内容 | false |
| `neotab:customize.top_content.toggle` | 允许玩家开关顶部内容 | false |
| `neotab:customize.top_content.edit` | 允许玩家编辑顶部内容文字 | false |
| `neotab:customize.ping` | 允许玩家开关延迟显示 | false |
| `neotab:customize.duration` | 允许玩家开关在线时长显示 | false |
| `neotab:customize.title` | 允许玩家开关称号功能 | false |
| `neotab:customize.health.toggle` | 允许玩家开关血量显示 | false |
| `neotab:customize.health.mode` | 允许玩家切换血量显示模式 | false |
| `neotab:customize.footer.custom` | 允许玩家编辑底部自定义文字 | false |
| `neotab:customize.footer.tps` | 允许玩家开关底部 TPS | false |
| `neotab:customize.footer.mspt` | 允许玩家开关底部 MSPT | false |
| `neotab:customize.footer.online` | 允许玩家开关底部在线人数 | false |
| `neotab:customize.theme` | 允许玩家切换主题 | false |

### 权限判断优先级

1. **OP ≥ 2**：完全自由，跳过所有限制（兼容单人世界）
2. **个人专属策略**（管理员在权限配置界面为特定玩家设置）：直接使用，不再叠加权限节点
3. **全局策略 AND 权限节点**：两者都允许才开放

### 受限 UI 行为

普通玩家打开个人自定义界面时，被锁定的选项会显示为灰色（不可点击），鼠标悬停时显示提示"此选项由服务器管理员控制"。功能存在但被锁定，玩家可以看到有哪些功能，但无法操作。

如果服务器安装了支持 NeoForge 权限 API 的权限管理模组（如 LuckPerms），可以通过上述权限节点精细控制。未安装权限模组时，仅由管理员在配置界面设置的全局/个人策略生效。

---

## 延迟颜色规则

启用"更好的延迟显示"后，延迟颜色按以下规则显示：

| 延迟范围 | 颜色 |
|---------|------|
| 0 - 99ms | 🟢 绿色 |
| 100 - 199ms | 🟡 黄色 |
| 200 - 349ms | 🟠 橙色 |
| 350ms 及以上 | 🔴 红色 |

---

## 在线时长规则

启用"在线时长显示"后：

- 不足 1 小时按 `1h` 显示
- 满 24 小时进位为天
- 示例：`1h`、`6h`、`1d1h`、`2d7h`

> 在线时长使用 Minecraft 原版 `minecraft:play_time` 统计项，数据持久化保存在玩家 stats 文件中，**服务器重启后不会重置**，显示的是玩家在该服务器的累计游玩总时长。

---

## 开发者 API

其他模组可以通过 NeoTab 提供的 API 为玩家添加称号。

### 方式一：实现 `TitleProvider` 接口

```java
public class MyTitleProvider implements TitleProvider {

    @Override
    @Nullable
    public String getTitle(ServerPlayer player) {
        // 返回称号文本，支持富文本标签
        // 返回 null 表示此提供者不为该玩家提供称号
        if (isAdmin(player)) {
            return "<color #FF0000><bold>『管理员』</bold></color>";
        }
        return null;
    }

    @Override
    public int getPriority() {
        return 100; // 数值越大优先级越高
    }

    @Override
    public String getProviderId() {
        return "mymod:title_provider";
    }
}
```

在模组初始化时注册：

```java
NeoTabAPI.registerTitleProvider(new MyTitleProvider());
```

### 方式二：监听 `GetPlayerTitleEvent` 事件

```java
@SubscribeEvent
public static void onGetPlayerTitle(GetPlayerTitleEvent event) {
    ServerPlayer player = event.getPlayer();
    String title = MyMod.getTitleForPlayer(player);
    if (title != null) {
        event.setTitle(title);
    }
}
```

### 称号文本格式

称号文本支持所有 NeoTab 富文本标签：

```
<color #FFD700>『黄金会员』</color>
<gradient #FF0000,#FF7F00><bold>『管理员』</bold></gradient>
<bold><color #9400D3>『紫晶学者』</color></bold>
```

---

## 注意事项

- 在线时长为本次会话时长，服务器重启后重置
- 世界信息（时间、天数、区块数）均来自主世界（Overworld）
- 内存信息为 JVM 层面的内存，不包含操作系统级别的内存使用
- 运行时间基于服务器 tick 计数估算，为近似值

---

## 许可证

All Rights Reserved
