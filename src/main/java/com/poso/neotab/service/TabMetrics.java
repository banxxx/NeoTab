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
 *
 * <p>P1 优化：所有格式化字符串在 {@link #sample} 时一次性计算完毕，
 * 后续每个玩家调用 tpsText() 等方法时直接返回缓存字段，
 * 避免对每个在线玩家重复执行 String.format。</p>
 */
public final class TabMetrics {

    // ── 原始数值字段 ──────────────────────────────────────────────────────────
    private final double tps;
    private final double mspt;
    private final int    onlinePlayers;
    private final int    maxPlayers;
    private final long   usedMemoryMB;
    private final long   maxMemoryMB;
    private final long   uptimeSeconds;
    private final long   worldTime;
    private final long   worldDay;
    private final int    loadedChunks;

    // ── 预计算的格式化字符串（P1 优化）────────────────────────────────────────
    private final String cachedTpsText;
    private final String cachedMsptText;
    private final String cachedMemoryText;
    private final String cachedMemoryPercentText;
    private final String cachedUptimeText;
    private final String cachedUptimeDaysText;
    private final String cachedUptimeHoursText;
    private final String cachedWorldTimeText;
    private final String cachedWorldDayText;
    private final String cachedLoadedChunksText;

    private TabMetrics(
            double tps, double mspt, int onlinePlayers, int maxPlayers,
            long usedMemoryMB, long maxMemoryMB, long uptimeSeconds,
            long worldTime, long worldDay, int loadedChunks) {
        this.tps           = tps;
        this.mspt          = mspt;
        this.onlinePlayers = onlinePlayers;
        this.maxPlayers    = maxPlayers;
        this.usedMemoryMB  = usedMemoryMB;
        this.maxMemoryMB   = maxMemoryMB;
        this.uptimeSeconds = uptimeSeconds;
        this.worldTime     = worldTime;
        this.worldDay      = worldDay;
        this.loadedChunks  = loadedChunks;

        // 预计算所有格式化字符串，只算一次
        this.cachedTpsText  = String.format(Locale.ROOT, "%.2f", tps);
        this.cachedMsptText = String.format(Locale.ROOT, "%.2f", mspt);
        this.cachedMemoryText = usedMemoryMB + "/" + maxMemoryMB + "MB";
        this.cachedMemoryPercentText = maxMemoryMB == 0 ? "0.0%"
                : String.format(Locale.ROOT, "%.1f%%", (double) usedMemoryMB / maxMemoryMB * 100);
        this.cachedUptimeText       = buildUptimeText(uptimeSeconds);
        this.cachedUptimeDaysText   = String.valueOf(uptimeSeconds / 86400);
        this.cachedUptimeHoursText  = String.valueOf(uptimeSeconds / 3600);
        this.cachedWorldTimeText    = buildWorldTimeText(worldTime);
        this.cachedWorldDayText     = String.valueOf(worldDay);
        this.cachedLoadedChunksText = String.valueOf(loadedChunks);
    }

    // ── 静态工厂 ──────────────────────────────────────────────────────────────

    /**
     * 从服务端采样一份最新数据，并预计算所有格式化字符串。
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
        // 1. TPS 和 MSPT
        double mspt = (double) server.getAverageTickTimeNanos() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
        double tps  = Math.min(20.0D, 1000.0D / Math.max(50.0D, mspt));

        // 2. 内存信息
        Runtime runtime     = Runtime.getRuntime();
        long usedMemoryMB   = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMB    = runtime.maxMemory() / (1024 * 1024);

        // 3. 服务器运行时间（基于 tick 计数，近似值）
        long uptimeSeconds  = server.getTickCount() / 20L;

        // 4. 世界信息（主世界）
        long worldTime    = 0;
        long worldDay     = 0;
        int  loadedChunks = 0;

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            worldTime     = overworld.getDayTime() % 24000;
            worldDay      = overworld.getDayTime() / 24000;
            loadedChunks  = overworld.getChunkSource().getLoadedChunksCount();
        }

        return new TabMetrics(
                tps, mspt,
                server.getPlayerCount(), server.getMaxPlayers(),
                usedMemoryMB, maxMemoryMB,
                uptimeSeconds,
                worldTime, worldDay, loadedChunks);
    }

    // ── 原始数值访问器 ────────────────────────────────────────────────────────

    public double tps()           { return tps; }
    public double mspt()          { return mspt; }
    public int    onlinePlayers() { return onlinePlayers; }
    public int    maxPlayers()    { return maxPlayers; }
    public long   usedMemoryMB()  { return usedMemoryMB; }
    public long   maxMemoryMB()   { return maxMemoryMB; }
    public long   uptimeSeconds() { return uptimeSeconds; }
    public long   worldTime()     { return worldTime; }
    public long   worldDay()      { return worldDay; }
    public int    loadedChunks()  { return loadedChunks; }

    // ── 格式化字符串访问器（直接返回预计算结果，O(1)）────────────────────────

    /** 格式化 TPS 文本，保留两位小数。 */
    public String tpsText()           { return cachedTpsText; }

    /** 格式化 MSPT 文本，保留两位小数。 */
    public String msptText()          { return cachedMsptText; }

    /** 格式化内存使用文本，格式：已使用/最大 MB。 */
    public String memoryText()        { return cachedMemoryText; }

    /** 格式化内存使用百分比，保留一位小数。 */
    public String memoryPercentText() { return cachedMemoryPercentText; }

    /** 格式化运行时间，格式：Xd Xh Xm（天 小时 分钟）。 */
    public String uptimeText()        { return cachedUptimeText; }

    /** 格式化运行时间（仅天数）。 */
    public String uptimeDaysText()    { return cachedUptimeDaysText; }

    /** 格式化运行时间（仅小时数）。 */
    public String uptimeHoursText()   { return cachedUptimeHoursText; }

    /** 格式化世界时间，格式：HH:MM（24小时制）。 */
    public String worldTimeText()     { return cachedWorldTimeText; }

    /** 格式化世界天数。 */
    public String worldDayText()      { return cachedWorldDayText; }

    /** 格式化已加载区块数。 */
    public String loadedChunksText()  { return cachedLoadedChunksText; }

    // ── 私有格式化辅助方法 ────────────────────────────────────────────────────

    private static String buildUptimeText(long uptimeSeconds) {
        long days    = uptimeSeconds / 86400;
        long hours   = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;

        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    private static String buildWorldTimeText(long worldTime) {
        // Minecraft 时间转换：0 = 6:00, 6000 = 12:00, 12000 = 18:00, 18000 = 0:00
        long minecraftHour   = (worldTime / 1000 + 6) % 24;
        long minecraftMinute = (worldTime % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", minecraftHour, minecraftMinute);
    }
}
