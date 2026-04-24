# Minecraft 原版 API 分析报告

## 📋 当前占位符使用的系统信息

你的 NeoTab 项目当前获取以下系统信息：

| 占位符 | 获取的信息 | 数据来源 |
|--------|-----------|---------|
| `%player_name%` | 玩家名称 | `ServerPlayer.getGameProfile().getName()` |
| `%player_ping%` | 玩家延迟 | `ServerPlayer.connection.latency()` |
| `%viewer_name%` | 查看者名称 | `ServerPlayer.getGameProfile().getName()` |
| `%viewer_ping%` | 查看者延迟 | `ServerPlayer.connection.latency()` |
| `%online%` | 在线人数 | `MinecraftServer.getPlayerCount()` |
| `%max_players%` | 最大人数 | `MinecraftServer.getMaxPlayers()` |
| `%tps%` | 服务器 TPS | 通过 `MinecraftServer.getAverageTickTimeNanos()` 计算 |
| `%mspt%` | 每 Tick 毫秒数 | 通过 `MinecraftServer.getAverageTickTimeNanos()` 转换 |

---

## ✅ 原版 API 使用情况分析

### 好消息：你已经在使用原版最高效的 API！

经过分析，你的代码**已经使用了 Minecraft 原版提供的最直接、最高效的方法**：

#### 1. **服务端性能指标** ✅ 最优
```java
// 你当前的实现（TabMetrics.java）
double mspt = (double) server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
double tps = Math.min(20.0D, 1000.0D / Math.max(50.0D, mspt));
```

**原版 API**:
- `MinecraftServer.getAverageTickTimeNanos()` - 这是原版提供的**唯一官方方法**
- 返回最近 tick 的平均纳秒时间
- 这个方法内部已经做了平滑处理，非常高效

**结论**: ✅ **无需优化**，这已经是最佳实践

---

#### 2. **在线玩家数量** ✅ 最优
```java
// 你当前的实现
server.getPlayerCount()
```

**原版 API**:
- `MinecraftServer.getPlayerCount()` - 直接返回缓存的玩家数量
- 内部实现：`return this.playerList.getPlayerCount()`
- 这是一个 O(1) 的操作，直接返回计数器

**结论**: ✅ **无需优化**，这已经是最高效的方法

---

#### 3. **最大玩家数** ✅ 最优
```java
// 你当前的实现
server.getMaxPlayers()
```

**原版 API**:
- `MinecraftServer.getMaxPlayers()` - 直接返回配置的最大玩家数
- 这是一个简单的字段访问，O(1) 操作

**结论**: ✅ **无需优化**

---

#### 4. **玩家延迟** ✅ 最优
```java
// 你当前的实现
player.connection.latency()
```

**原版 API**:
- `ServerGamePacketListenerImpl.latency()` - 返回缓存的延迟值
- 原版每 tick 更新一次，不需要实时计算
- 这是一个简单的字段访问

**结论**: ✅ **无需优化**

---

#### 5. **玩家名称** ✅ 最优
```java
// 你当前的实现
player.getGameProfile().getName()
```

**原版 API**:
- `GameProfile.getName()` - 直接返回玩家名称字符串
- 这是一个简单的字段访问

**结论**: ✅ **无需优化**

---

## 🔍 原版中的其他可用信息

虽然你当前的实现已经是最优的，但原版还提供了一些其他有用的信息，你可以考虑添加：

### 1. **内存使用情况**
```java
// 获取 JVM 内存信息
Runtime runtime = Runtime.getRuntime();
long maxMemory = runtime.maxMemory();      // 最大内存
long totalMemory = runtime.totalMemory();  // 已分配内存
long freeMemory = runtime.freeMemory();    // 空闲内存
long usedMemory = totalMemory - freeMemory; // 已使用内存

// 计算使用百分比
double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
```

**性能**: ✅ 非常高效，这些都是简单的字段访问

---

### 2. **服务器运行时间**
```java
// 获取服务器启动时间
long serverStartTime = server.getServerStartTime(); // 返回毫秒时间戳
long uptime = System.currentTimeMillis() - serverStartTime;

// 格式化为天/小时/分钟
long days = uptime / (24 * 60 * 60 * 1000);
long hours = (uptime / (60 * 60 * 1000)) % 24;
long minutes = (uptime / (60 * 1000)) % 60;
```

**性能**: ✅ 非常高效，只是简单的数学计算

---

### 3. **世界信息**
```java
// 获取主世界信息
ServerLevel overworld = server.getLevel(Level.OVERWORLD);
if (overworld != null) {
    // 世界时间
    long worldTime = overworld.getDayTime();
    long day = worldTime / 24000L;
    
    // 世界中的实体数量
    int entityCount = overworld.getEntityCount();
    
    // 世界中的区块数量
    int loadedChunks = overworld.getChunkSource().getLoadedChunksCount();
}
```

**性能**: ✅ 高效，这些都是缓存的值

---

### 4. **玩家游戏模式**
```java
// 获取玩家游戏模式
GameType gameMode = player.gameMode.getGameModeForPlayer();
String modeName = switch (gameMode) {
    case SURVIVAL -> "生存";
    case CREATIVE -> "创造";
    case ADVENTURE -> "冒险";
    case SPECTATOR -> "旁观";
};
```

**性能**: ✅ 非常高效

---

### 5. **玩家位置信息**
```java
// 获取玩家坐标
int x = (int) player.getX();
int y = (int) player.getY();
int z = (int) player.getZ();

// 获取玩家所在维度
String dimension = player.level().dimension().location().toString();
// 例如: "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
```

**性能**: ✅ 非常高效

---

### 6. **玩家健康和饥饿值**
```java
// 获取玩家生命值
float health = player.getHealth();
float maxHealth = player.getMaxHealth();

// 获取玩家饥饿值
int foodLevel = player.getFoodData().getFoodLevel();
float saturation = player.getFoodData().getSaturationLevel();
```

**性能**: ✅ 非常高效

---

## 📊 性能对比：原版 API vs 自定义实现

| 信息类型 | 原版 API | 自定义实现 | 性能差异 |
|---------|---------|-----------|---------|
| TPS/MSPT | `getAverageTickTimeNanos()` | 手动计算每个 tick | 原版更优（已平滑） |
| 在线人数 | `getPlayerCount()` | 遍历玩家列表计数 | 原版更优（O(1) vs O(n)） |
| 玩家延迟 | `connection.latency()` | 手动计算网络延迟 | 原版更优（已缓存） |
| 内存使用 | `Runtime.getRuntime()` | 无原生支持 | 原版唯一选择 |

---

## ✨ 优化建议

### 当前实现评估：⭐⭐⭐⭐⭐ 完美

你的代码已经使用了所有最优的原版 API，**无需进行任何更改**。

### 为什么你的实现是最优的？

1. **直接使用原版缓存值**
   - `getPlayerCount()` - 使用原版维护的计数器
   - `latency()` - 使用原版每 tick 更新的延迟值
   - `getAverageTickTimeNanos()` - 使用原版平滑后的 tick 时间

2. **避免重复计算**
   - 你已经在 `NeoTabService` 中添加了 `cachedMetrics`
   - 这进一步优化了原版 API 的使用

3. **使用高效的数据结构**
   - `LinkedHashMap` 用于占位符映射（保持顺序）
   - `StringBuilder` 用于字符串拼接（避免临时对象）

---

## 🎯 可选的扩展占位符

如果你想添加更多占位符，以下是推荐的原版 API：

### 高优先级（常用且高效）

```java
// 1. 内存使用
%memory_used%     // 已使用内存（MB）
%memory_max%      // 最大内存（MB）
%memory_percent%  // 内存使用百分比

// 2. 服务器运行时间
%uptime%          // 运行时间（格式化）
%uptime_days%     // 运行天数
%uptime_hours%    // 运行小时数

// 3. 世界信息
%world_time%      // 世界时间
%world_day%       // 世界天数
%loaded_chunks%   // 已加载区块数
```

### 中优先级（有用但不常用）

```java
// 4. 玩家详细信息
%player_gamemode% // 玩家游戏模式
%player_health%   // 玩家生命值
%player_food%     // 玩家饥饿值
%player_level%    // 玩家经验等级

// 5. 位置信息
%player_x%        // 玩家 X 坐标
%player_y%        // 玩家 Y 坐标
%player_z%        // 玩家 Z 坐标
%player_dimension% // 玩家所在维度
```

### 低优先级（性能开销较大）

```java
// 6. 实体统计（需要遍历）
%entity_count%    // 实体总数（可能较慢）
%mob_count%       // 生物数量（需要过滤）

// 7. 区块统计（需要计算）
%chunk_count%     // 区块数量
```

---

## 📝 实现示例

如果你想添加内存和运行时间占位符，可以这样实现：

### 1. 扩展 TabMetrics

```java
public record TabMetrics(
    double tps, 
    double mspt, 
    int onlinePlayers, 
    int maxPlayers,
    // 新增字段
    long usedMemoryMB,
    long maxMemoryMB,
    long uptimeSeconds
) {
    public static TabMetrics sample(MinecraftServer server) {
        // 原有的 TPS/MSPT 计算
        double mspt = (double) server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
        double tps = Math.min(20.0D, 1000.0D / Math.max(50.0D, mspt));
        
        // 新增：内存信息
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        
        // 新增：运行时间
        long uptimeSeconds = (System.currentTimeMillis() - server.getServerStartTime()) / 1000;
        
        return new TabMetrics(
            tps, 
            mspt, 
            server.getPlayerCount(), 
            server.getMaxPlayers(),
            usedMemoryMB,
            maxMemoryMB,
            uptimeSeconds
        );
    }
    
    // 格式化方法
    public String memoryText() {
        return usedMemoryMB + "/" + maxMemoryMB + "MB";
    }
    
    public String memoryPercentText() {
        double percent = (double) usedMemoryMB / maxMemoryMB * 100;
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
    
    public String uptimeText() {
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        return hours + "h" + minutes + "m";
    }
}
```

### 2. 添加新占位符

```java
// 在 PlaceholderEngine.values() 中添加
values.put("%memory_used%", Long.toString(context.metrics().usedMemoryMB()));
values.put("%memory_max%", Long.toString(context.metrics().maxMemoryMB()));
values.put("%memory_percent%", context.metrics().memoryPercentText());
values.put("%uptime%", context.metrics().uptimeText());
```

**性能影响**: ✅ **几乎为零**
- `Runtime.getRuntime()` 是单例，访问非常快
- 内存信息是简单的字段访问
- 运行时间只是简单的减法运算

---

## 🔧 性能最佳实践总结

### ✅ 你已经做对的事情

1. **使用原版缓存的值** - 不重复计算
2. **批量采样** - 一次采样，多次使用（cachedMetrics）
3. **避免遍历** - 使用 `getPlayerCount()` 而不是遍历玩家列表
4. **缓存解析结果** - 富文本解析缓存

### ⚠️ 需要注意的事项

1. **避免频繁的 GC 操作**
   - ✅ 你已经使用 StringBuilder
   - ✅ 你已经缓存解析结果

2. **避免遍历大集合**
   - ✅ 你使用 `getPlayerCount()` 而不是遍历
   - ✅ 你缓存了在线时长数据

3. **避免重复采样**
   - ✅ 你已经添加了 `cachedMetrics`

---

## 🎉 结论

### 你的实现评分：⭐⭐⭐⭐⭐ (5/5)

**你已经在使用 Minecraft 原版提供的最高效的 API！**

- ✅ 所有系统信息都通过原版 API 获取
- ✅ 使用了原版的缓存机制
- ✅ 避免了不必要的计算和遍历
- ✅ 添加了额外的缓存层（cachedMetrics）

**无需进行任何更改**，你的实现已经是最佳实践。

### 可选的改进方向

如果你想扩展功能，可以考虑添加：
1. 内存使用信息（使用 `Runtime.getRuntime()`）
2. 服务器运行时间（使用 `server.getServerStartTime()`）
3. 世界信息（使用 `server.getLevel()`）

这些都是原版提供的高效 API，不会对性能产生负面影响。

---

**分析完成日期**: 2026-04-25  
**Minecraft 版本**: 1.21.1  
**NeoForge 版本**: 最新
