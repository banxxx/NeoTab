package com.poso.neotab.service;

import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.level.Level;

/**
 * TAB 面板里常用的服务端统计快照。
 *
 * <p>当前包含的统计项：</p>
 * <ul>
 *     <li>TPS - 服务器每秒 Tick 数</li>
 *     <li>MSPT - 每 Tick 毫秒数</li>
 *     <li>在线人数</li>
 *     <li>最大人数</li>
 *     <li>内存使用情况</li>
 *     <li>服务器运行时间（基于 tick 计数）</li>
 *     <li>世界信息（时间、天数、区块数）</li>
 * </ul>
 */
public record TabMetrics(
    double tps, 
    double mspt, 
    int onlinePlayers, 
    int maxPlayers,
    long usedMemoryMB,
    long maxMemoryMB,
    long uptimeSeconds,
    long worldTime,
    long worldDay,
    int loadedChunks
) {
    /**
     * 从服务端采样一份最新数据。
     *
     * <p>性能说明：所有数据都通过原版高效 API 获取，不会产生额外的性能开销。</p>
     * <ul>
     *     <li>TPS/MSPT: 使用原版平滑后的 tick 时间</li>
     *     <li>在线人数: O(1) 操作，直接返回计数器</li>
     *     <li>内存信息: 简单的字段访问</li>
     *     <li>运行时间: 基于 tick 计数计算（每 tick 约 50ms）</li>
     *     <li>世界信息: 使用原版缓存的值</li>
     * </ul>
     */
    public static TabMetrics sample(MinecraftServer server) {
        // 1. TPS 和 MSPT（原有逻辑）
        double mspt = (double) server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
        double tps = Math.min(20.0D, 1000.0D / Math.max(50.0D, mspt));
        
        // 2. 内存信息（使用 JVM Runtime API）
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        
        // 3. 服务器运行时间（基于 tick 计数）
        // 注意：这是一个近似值，因为实际 tick 时间可能不是恰好 50ms
        long tickCount = server.getTickCount();
        long uptimeSeconds = tickCount / 20; // 假设 20 ticks = 1 秒
        
        // 4. 世界信息（主世界）
        long worldTime = 0;
        long worldDay = 0;
        int loadedChunks = 0;
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            worldTime = overworld.getDayTime() % 24000; // 当前时间（0-23999）
            worldDay = overworld.getDayTime() / 24000;  // 天数
            loadedChunks = overworld.getChunkSource().getLoadedChunksCount();
        }
        
        return new TabMetrics(
            tps, 
            mspt, 
            server.getPlayerCount(), 
            server.getMaxPlayers(),
            usedMemoryMB,
            maxMemoryMB,
            uptimeSeconds,
            worldTime,
            worldDay,
            loadedChunks
        );
    }

    /** 格式化 TPS 文本，保留两位小数。 */
    public String tpsText() {
        return String.format(Locale.ROOT, "%.2f", tps);
    }

    /** 格式化 MSPT 文本，保留两位小数。 */
    public String msptText() {
        return String.format(Locale.ROOT, "%.2f", mspt);
    }
    
    /** 格式化内存使用文本，格式：已使用/最大 MB。 */
    public String memoryText() {
        return usedMemoryMB + "/" + maxMemoryMB + "MB";
    }
    
    /** 格式化内存使用百分比，保留一位小数。 */
    public String memoryPercentText() {
        if (maxMemoryMB == 0) {
            return "0.0%";
        }
        double percent = (double) usedMemoryMB / maxMemoryMB * 100;
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
    
    /** 格式化运行时间，格式：Xd Xh Xm（天 小时 分钟）。 */
    public String uptimeText() {
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /** 格式化运行时间（仅天数）。 */
    public String uptimeDaysText() {
        return String.valueOf(uptimeSeconds / 86400);
    }
    
    /** 格式化运行时间（仅小时数）。 */
    public String uptimeHoursText() {
        return String.valueOf(uptimeSeconds / 3600);
    }
    
    /** 格式化世界时间，格式：HH:MM（24小时制）。 */
    public String worldTimeText() {
        // Minecraft 时间转换：0 = 6:00, 6000 = 12:00, 12000 = 18:00, 18000 = 0:00
        long minecraftHour = (worldTime / 1000 + 6) % 24;
        long minecraftMinute = (worldTime % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", minecraftHour, minecraftMinute);
    }
    
    /** 格式化世界天数。 */
    public String worldDayText() {
        return String.valueOf(worldDay);
    }
    
    /** 格式化已加载区块数。 */
    public String loadedChunksText() {
        return String.valueOf(loadedChunks);
    }
}
