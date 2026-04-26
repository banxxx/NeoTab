# 占位符

NeoTab 内置 20 个占位符，可在顶部内容和底部自定义信息中使用。

## 所有占位符

### 👤 玩家信息

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%player_name%` | 目标玩家名称 | `Steve` |
| `%player_ping%` | 目标玩家延迟（ms） | `45` |
| `%viewer_name%` | 查看者名称 | `Alex` |
| `%viewer_ping%` | 查看者延迟（ms） | `32` |

### 🖥️ 服务器状态

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%online%` | 当前在线人数 | `15` |
| `%max_players%` | 最大玩家数 | `100` |
| `%tps%` | 服务器 TPS | `19.85` |
| `%mspt%` | 每 Tick 毫秒数 | `45.23` |

### 💾 内存使用

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%memory_used%` | 已使用内存（MB） | `2048` |
| `%memory_max%` | 最大可用内存（MB） | `4096` |
| `%memory_total%` | 内存使用情况 | `2048/4096MB` |
| `%memory_percent%` | 内存使用百分比 | `50.0%` |

### ⏱️ 运行时间

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%uptime%` | 服务器运行时间 | `2d 5h 30m` |
| `%uptime_days%` | 运行天数 | `2` |
| `%uptime_hours%` | 运行总小时数 | `53` |

`%uptime%` 会自动选择最合适的格式：
- 不足 1 小时：`30m`
- 不足 1 天：`5h 30m`
- 1 天以上：`2d 5h 30m`

### 🌍 世界信息

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%world_time%` | 主世界时间（24小时制） | `14:30` |
| `%world_day%` | 主世界天数 | `127` |
| `%loaded_chunks%` | 已加载区块数 | `1523` |

Minecraft 时间对照：

| Minecraft 时间 | 游戏时间 |
|---------------|---------|
| 0 | 06:00（日出） |
| 6000 | 12:00（正午） |
| 12000 | 18:00（日落） |
| 18000 | 00:00（午夜） |

## 快速参考

```
玩家信息:
%player_name%  %player_ping%  %viewer_name%  %viewer_ping%

服务器状态:
%online%  %max_players%  %tps%  %mspt%

内存使用:
%memory_used%  %memory_max%  %memory_total%  %memory_percent%

运行时间:
%uptime%  %uptime_days%  %uptime_hours%

世界信息:
%world_time%  %world_day%  %loaded_chunks%
```

## 配置示例

### 简洁状态栏

```
在线: %online%/%max_players% | TPS: %tps% | 内存: %memory_percent%
```

### 详细监控面板

```
<gradient #FFD700,#FFA500><bold>服务器状态监控</bold></gradient>

<color #55FF55>性能指标</color>
TPS: %tps%  MSPT: %mspt%
内存: %memory_total% (%memory_percent%)

<color #55FFFF>世界信息</color>
主世界第 %world_day% 天  时间: %world_time%
已加载区块: %loaded_chunks%

<color #FFFF55>服务器信息</color>
在线玩家: %online%/%max_players%
运行时间: %uptime%
```

### 玩家个性化信息

```
欢迎 <color #FFD700>%viewer_name%</color>
你的延迟: %viewer_ping%ms
服务器 TPS: %tps%
```

## 性能说明

所有占位符都通过原版高效 API 获取，无额外性能开销：

| 占位符类型 | 数据来源 | 性能开销 |
|-----------|---------|---------|
| 内存信息 | `Runtime.getRuntime()` | 极低 |
| 运行时间 | 基于 tick 计数 | 极低 |
| 世界信息 | 原版缓存值 | 极低 |
| TPS/MSPT | `getAverageTickTimeNanos()` | 极低 |

> 内存信息为 JVM 层面的内存，不包含操作系统级别的内存使用。
> 运行时间基于服务器 tick 计数估算，为近似值。
