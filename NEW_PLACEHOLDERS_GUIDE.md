# 🎯 新增占位符使用指南

## 📋 新增占位符列表

你的 NeoTab 现在支持 **20 个占位符**，分为 5 大类：

---

## 1️⃣ 玩家信息（4个）

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%player_name%` | 目标玩家名称 | `Steve` |
| `%player_ping%` | 目标玩家延迟（毫秒） | `45` |
| `%viewer_name%` | 查看者名称 | `Alex` |
| `%viewer_ping%` | 查看者延迟（毫秒） | `32` |

**使用场景**:
- TAB 玩家名称显示
- 个性化欢迎消息
- 延迟监控

---

## 2️⃣ 服务器状态（4个）

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%online%` | 当前在线人数 | `15` |
| `%max_players%` | 最大玩家数 | `100` |
| `%tps%` | 服务器 TPS（每秒 Tick 数） | `19.85` |
| `%mspt%` | 每 Tick 毫秒数 | `45.23` |

**使用场景**:
- 服务器状态监控
- TAB 底部信息栏
- 性能展示

---

## 3️⃣ 内存使用（4个）✨ 新增

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%memory_used%` | 已使用内存（MB） | `2048` |
| `%memory_max%` | 最大可用内存（MB） | `4096` |
| `%memory_total%` | 内存使用情况（已使用/最大） | `2048/4096MB` |
| `%memory_percent%` | 内存使用百分比 | `50.0%` |

**使用场景**:
- 服务器资源监控
- 管理员信息面板
- 性能警告提示

**示例配置**:
```
服务器状态
内存: %memory_total% (%memory_percent%)
TPS: %tps%  MSPT: %mspt%
```

---

## 4️⃣ 运行时间（3个）✨ 新增

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%uptime%` | 服务器运行时间（格式化） | `2d 5h 30m` |
| `%uptime_days%` | 运行天数 | `2` |
| `%uptime_hours%` | 运行总小时数 | `53` |

**使用场景**:
- 服务器稳定性展示
- 运行时间统计
- 管理员监控面板

**格式说明**:
- `%uptime%` 会自动选择最合适的格式：
  - 不足 1 小时：`30m`
  - 不足 1 天：`5h 30m`
  - 1 天以上：`2d 5h 30m`

**示例配置**:
```
服务器已稳定运行: %uptime%
```

---

## 5️⃣ 世界信息（3个）✨ 新增

| 占位符 | 说明 | 示例输出 |
|--------|------|---------|
| `%world_time%` | 主世界当前时间（24小时制） | `14:30` |
| `%world_day%` | 主世界天数 | `127` |
| `%loaded_chunks%` | 已加载区块数 | `1523` |

**使用场景**:
- 世界信息展示
- 游戏时间显示
- 服务器负载监控

**时间转换说明**:
- Minecraft 时间 → 真实时间：
  - `0` → `06:00`（日出）
  - `6000` → `12:00`（正午）
  - `12000` → `18:00`（日落）
  - `18000` → `00:00`（午夜）

**示例配置**:
```
主世界: 第 %world_day% 天  时间: %world_time%
已加载区块: %loaded_chunks%
```

---

## 🎨 完整示例配置

### 示例 1: 简洁信息栏
```
服务器状态
在线: %online%/%max_players%  TPS: %tps%
内存: %memory_percent%  运行: %uptime%
```

**效果**:
```
服务器状态
在线: 15/100  TPS: 19.85
内存: 50.0%  运行: 2d 5h 30m
```

---

### 示例 2: 详细监控面板
```
<gradient #FFD700,#FFA500>服务器状态监控</gradient>

<color #55FF55>性能指标</color>
TPS: %tps%  MSPT: %mspt%
内存: %memory_used%/%memory_max%MB (%memory_percent%)

<color #55FFFF>世界信息</color>
主世界第 %world_day% 天  时间: %world_time%
已加载区块: %loaded_chunks%

<color #FFFF55>服务器信息</color>
在线玩家: %online%/%max_players%
运行时间: %uptime%
```

---

### 示例 3: 玩家个性化信息
```
欢迎 <color #FFD700>%viewer_name%</color>
你的延迟: %viewer_ping%ms
服务器 TPS: %tps%
```

---

### 示例 4: 性能警告（配合条件显示）
```
<color #FF5555>⚠ 内存使用: %memory_percent%</color>
<color #FFAA00>TPS: %tps%</color>
建议重启服务器
```

---

## 📊 性能说明

### ✅ 所有新增占位符都是高效的

| 占位符类型 | 性能开销 | 说明 |
|-----------|---------|------|
| 内存信息 | ⭐⭐⭐⭐⭐ 极低 | 简单的字段访问 |
| 运行时间 | ⭐⭐⭐⭐⭐ 极低 | 基于 tick 计数，简单除法 |
| 世界信息 | ⭐⭐⭐⭐⭐ 极低 | 使用原版缓存的值 |

**所有占位符都通过原版 API 获取，不会对服务器性能产生负面影响。**

---

## 🔧 在配置界面中使用

### 占位符帮助文本

在配置界面中，你可以看到所有可用的占位符：

```
玩家信息:
%player_name%  %player_ping%
%viewer_name%  %viewer_ping%

服务器状态:
%online%  %max_players%
%tps%  %mspt%

内存使用:
%memory_used%  %memory_max%
%memory_total%  %memory_percent%

运行时间:
%uptime%  %uptime_days%  %uptime_hours%

世界信息:
%world_time%  %world_day%  %loaded_chunks%
```

---

## 💡 使用技巧

### 1. 组合使用占位符
```
服务器负载: TPS %tps% | 内存 %memory_percent% | 区块 %loaded_chunks%
```

### 2. 配合富文本标签
```
<gradient #FF0000,#00FF00>内存: %memory_percent%</gradient>
```

### 3. 创建动态信息栏
```
<color #FFD700>%world_time%</color> | 第 %world_day% 天 | %online% 人在线
```

### 4. 性能监控面板
```
<bold>服务器性能</bold>
TPS: %tps% | MSPT: %mspt%
内存: %memory_total%
运行: %uptime%
```

---

## 🎯 推荐配置

### 适合小型服务器（<20人）
```
<gradient #55FF55,#55FFFF>欢迎来到服务器</gradient>

在线: %online%/%max_players%
TPS: %tps%  时间: %world_time%
```

### 适合中型服务器（20-50人）
```
<color #FFD700>服务器状态</color>

性能: TPS %tps% | MSPT %mspt%
在线: %online%/%max_players%
内存: %memory_percent%
运行: %uptime%
```

### 适合大型服务器（50+人）
```
<gradient #FF6B6B,#FFD700>服务器监控面板</gradient>

<color #55FF55>性能</color>  TPS: %tps%  MSPT: %mspt%
<color #55FFFF>资源</color>  内存: %memory_total% (%memory_percent%)
<color #FFFF55>世界</color>  第 %world_day% 天  %world_time%  区块: %loaded_chunks%
<color #FF55FF>在线</color>  %online%/%max_players% 玩家  运行: %uptime%
```

---

## 📝 注意事项

### 1. 运行时间计算方式
- 基于服务器 tick 计数（`server.getTickCount()`）
- 假设 20 ticks = 1 秒
- 这是一个近似值，因为实际 tick 时间可能波动

### 2. 世界信息来源
- 所有世界信息都来自主世界（Overworld）
- 如果需要其他维度的信息，可以联系开发者添加

### 3. 内存信息说明
- `%memory_used%`: 当前 JVM 已使用的内存
- `%memory_max%`: JVM 最大可用内存（-Xmx 参数）
- 不包括操作系统级别的内存使用

---

## 🚀 更新日志

### v1.1 - 新增占位符
- ✅ 添加 4 个内存使用占位符
- ✅ 添加 3 个运行时间占位符
- ✅ 添加 3 个世界信息占位符
- ✅ 更新占位符帮助文本
- ✅ 所有新增功能都经过性能优化

### 性能影响
- ✅ 无额外性能开销
- ✅ 所有数据通过原版 API 获取
- ✅ 使用缓存机制避免重复计算

---

## 📞 技术支持

如果你需要更多占位符或有任何问题，可以：
1. 查看 `VANILLA_API_ANALYSIS.md` 了解可用的原版 API
2. 查看 `PERFORMANCE_OPTIMIZATION_REPORT.md` 了解性能优化细节
3. 联系开发者添加新功能

---

**更新日期**: 2026-04-25  
**版本**: v1.1  
**新增占位符数量**: 10 个  
**总占位符数量**: 20 个
