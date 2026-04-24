package com.poso.neotab.service;

import com.poso.neotab.config.TabConfig;
import com.poso.neotab.config.TabConfigRepository;
import com.poso.neotab.network.payload.SyncOnlineDurationsPayload;
import com.poso.neotab.network.payload.SyncTabConfigPayload;
import com.poso.neotab.permission.NeoTabPermissions;
import com.poso.neotab.tab.PlaceholderContext;
import com.poso.neotab.tab.PlaceholderEngine;
import com.poso.neotab.text.RichTextEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoTab 核心服务。
 *
 * <p>这里是整个模组最重要的协调层，职责包括：</p>
 * <ul>
 *     <li>加载/保存服务端配置</li>
 *     <li>定时刷新 TAB 头部、底部和玩家名</li>
 *     <li>在玩家加入、离开时重新应用显示内容</li>
 *     <li>在管理员保存配置后立即广播给在线玩家</li>
 * </ul>
 *
 * <p>你后续要扩展功能时，优先从这里继续接入：</p>
 * <ul>
 *     <li>更多统计项采样</li>
 *     <li>分世界/分权限显示不同内容</li>
 *     <li>缓存策略与节流刷新</li>
 * </ul>
 */
public final class NeoTabService {
    /** 负责把配置落到世界目录下的 serverconfig 中。 */
    private final TabConfigRepository repository = new TabConfigRepository();

    /** 当前正在使用的内存配置，服务端运行时所有逻辑都读这个对象。 */
    private TabConfig config = TabConfig.defaults();

    /** 记录玩家本次上线会话的开始时间，用于计算在线时长。 */
    private final Map<UUID, Long> sessionJoinTimes = new ConcurrentHashMap<>();

    /** 简单的 tick 计数器，用于按配置间隔刷新 TAB。 */
    private int tickCounter;
    
    /**
     * 上一次采样的服务端指标缓存。
     * 
     * <p>性能优化：缓存上一次的 TPS/MSPT 等指标，避免每次都重新采样和格式化。
     * 只有在刷新间隔到达时才更新缓存。</p>
     */
    private TabMetrics cachedMetrics = null;
    
    /**
     * 上一次同步的在线时长数据。
     * 
     * <p>性能优化：缓存上一次发送的在线时长数据，避免重复创建相同的 HashMap。</p>
     */
    private Map<UUID, String> lastOnlineDurations = new HashMap<>();

    /**
     * 返回当前生效配置。
     */
    public TabConfig getConfig() {
        return config;
    }

    /**
     * 从磁盘重新加载配置，并立即刷新在线玩家 TAB。
     */
    public void reload(MinecraftServer server) {
        this.config = repository.load(server).sanitized();
        this.tickCounter = 0;
        
        // 清空缓存，确保使用新配置
        this.cachedMetrics = null;
        this.lastOnlineDurations.clear();
        RichTextEngine.clearCache(); // 清空富文本解析缓存
        
        syncConfigToAll(server);
        applyAll(server);
        refreshAllNames(server);
    }

    /**
     * 服务端启动完成后加载配置。
     */
    public void onServerStarted(MinecraftServer server) {
        reload(server);
    }

    /**
     * 服务端关闭时清理内存状态。
     *
     * <p>这里不需要额外保存，因为保存动作只在管理员主动提交配置时进行。</p>
     */
    public void onServerStopped() {
        this.tickCounter = 0;
        this.config = TabConfig.defaults();
        this.sessionJoinTimes.clear();
        this.cachedMetrics = null;
        this.lastOnlineDurations.clear();
        RichTextEngine.clearCache();
    }

    /**
     * 新玩家加入时，立刻给他下发 TAB 头/尾内容，
     * 然后刷新所有人的玩家名显示。
     */
    public void onPlayerJoined(ServerPlayer player) {
        this.sessionJoinTimes.put(player.getUUID(), System.currentTimeMillis());
        syncConfigTo(player);
        applyPlayer(player);
        
        // 玩家数量变化，需要刷新所有人的显示
        refreshAllNames(player.server);
        
        // 如果启用了在线时长显示，同步在线时长数据
        if (config.onlineDurationEnabled()) {
            syncOnlineDurationsTo(player);
            // 也需要向其他玩家同步新玩家的在线时长
            syncOnlineDurationsToAllOptimized(player.server);
        }
    }

    /**
     * 玩家离开时，在线人数等占位符会变化，因此重新刷新全服 TAB。
     */
    public void onPlayerLeft(ServerPlayer player) {
        this.sessionJoinTimes.remove(player.getUUID());
        
        // 玩家数量变化，需要刷新所有人的显示
        applyAll(player.server);
        refreshAllNames(player.server);
    }

    /**
     * 服务端每 tick 调用一次。
     *
     * <p>性能优化：</p>
     * <ul>
     *     <li>使用缓存的指标数据，避免每次都重新采样</li>
     *     <li>只在刷新间隔到达时才执行更新</li>
     *     <li>检测数据是否真的变化，避免无意义的刷新</li>
     * </ul>
     */
    public void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < config.refreshIntervalTicks()) {
            return;
        }

        tickCounter = 0;
        
        // 更新缓存的指标数据
        cachedMetrics = TabMetrics.sample(server);
        
        // 执行刷新（使用缓存的指标）
        applyAll(server);
        refreshAllNames(server);
        
        // 如果启用了在线时长显示，同步在线时长数据到客户端
        if (config.onlineDurationEnabled()) {
            syncOnlineDurationsToAllOptimized(server);
        }
    }

    /**
     * 处理管理员从客户端 UI 提交的新配置。
     *
     * <p>这里再次做权限校验，而不是只相信客户端，
     * 避免被恶意数据包直接绕过界面限制。</p>
     */
    public void updateConfig(ServerPlayer actor, TabConfig requestedConfig) {
        if (!NeoTabPermissions.canConfigure(actor)) {
            actor.sendSystemMessage(Component.translatable("message.neotab.no_permission"));
            return;
        }

        this.config = requestedConfig.sanitized();
        repository.save(actor.server, this.config);
        
        // 清空缓存，确保使用新配置
        this.cachedMetrics = null;
        this.lastOnlineDurations.clear();
        RichTextEngine.clearCache(); // 清空富文本解析缓存
        
        syncConfigToAll(actor.server);
        applyAll(actor.server);
        refreshAllNames(actor.server);
        
        // 如果启用了在线时长显示，同步在线时长数据
        if (config.onlineDurationEnabled()) {
            syncOnlineDurationsToAll(actor.server);
        }
        
        actor.sendSystemMessage(Component.translatable("message.neotab.saved"));
    }

    /**
     * 把 TAB 头/尾内容重新应用到所有在线玩家。
     */
    public void applyAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyPlayer(player);
        }
    }

    /**
     * 为单个玩家计算并设置 TAB 顶部/底部文本。
     *
     * <p>这里使用"viewer 视角"占位符上下文，因此同一套模板可以根据
     * 不同玩家看到自己的 ping、名字等内容。</p>
     * 
     * <p>性能优化：使用缓存的指标数据，避免重复采样。</p>
     */
    public void applyPlayer(ServerPlayer player) {
        // 使用缓存的指标数据，如果没有缓存则立即采样
        TabMetrics metrics = cachedMetrics != null ? cachedMetrics : TabMetrics.sample(player.server);
        
        PlaceholderContext context = PlaceholderContext.forViewerWithMetrics(player.server, player, metrics);
        Component header = buildHeader(context);
        Component footer = buildFooter(context);
        player.setTabListHeaderFooter(header, footer);
    }

    /**
     * 强制刷新所有玩家在 TAB 中的显示名。
     *
     * <p>玩家名由 NeoForge 的 TabListNameFormat 事件动态生成，
     * 所以每次配置变更、人数变化或定时刷新后都要重新触发一次。</p>
     */
    public void refreshAllNames(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.refreshTabListName();
        }
    }

    /**
     * 供玩家列表渲染逻辑读取指定玩家的在线时长文本。
     *
     * <p>规则：</p>
     * <ul>
     *     <li>不足 1 小时按 1 小时显示</li>
     *     <li>满 24 小时进位为天</li>
     *     <li>输出格式示例：1h、5h、1d1h、2d7h</li>
     * </ul>
     */
    public String getOnlineDurationText(ServerPlayer player) {
        long joinedAt = sessionJoinTimes.getOrDefault(player.getUUID(), System.currentTimeMillis());
        long elapsedMillis = Math.max(1L, System.currentTimeMillis() - joinedAt);
        long totalHours = Math.max(1L, (elapsedMillis + 3_599_999L) / 3_600_000L);
        long days = totalHours / 24L;
        long hours = totalHours % 24L;

        if (days <= 0L) {
            return totalHours + "h";
        }

        return days + "d" + Math.max(1L, hours) + "h";
    }

    /**
     * 判断客户端是否需要隐藏原版延迟信号图标。
     */
    /**
     * 构建 TAB 顶部文本。
     */
    private Component buildHeader(PlaceholderContext context) {
        MutableComponent result = Component.empty();
        boolean hasContent = false;

        if (config.topTitleEnabled() && !config.topTitleText().isBlank()) {
            Component titleComponent = PlaceholderEngine.renderSingleLine(config.topTitleText(), context);
            if (!titleComponent.getString().isBlank()) {
                result.append(titleComponent);
                hasContent = true;
            }
        }

        if (config.topContentEnabled() && !config.topContentText().isBlank()) {
            Component contentComponent = PlaceholderEngine.renderMultiline(config.topContentText(), context);
            if (!contentComponent.getString().isBlank()) {
                if (hasContent) {
                    result.append(Component.literal("\n"));
                }
                result.append(contentComponent);
                hasContent = true;
            }
        }

        return hasContent ? result : Component.empty();
    }

    /**
     * 构建 TAB 底部信息栏。
     */
    private Component buildFooter(PlaceholderContext context) {
        MutableComponent builder = Component.empty();
        boolean hasSegment = false;

        if (!config.footerCustomText().isBlank()) {
            Component customComponent = PlaceholderEngine.renderMultiline(config.footerCustomText(), context);
            if (!customComponent.getString().isBlank()) {
                hasSegment = appendFooterSegment(builder, hasSegment, customComponent);
            }
        }

        if (config.footerTpsEnabled()) {
            hasSegment = appendFooterSegment(builder, hasSegment, Component.literal("TPS: " + context.metrics().tpsText()));
        }

        if (config.footerMsptEnabled()) {
            hasSegment = appendFooterSegment(builder, hasSegment, Component.literal("MSPT: " + context.metrics().msptText()));
        }

        if (config.footerOnlineEnabled()) {
            hasSegment = appendFooterSegment(
                builder,
                hasSegment,
                Component.translatable("tab.neotab.footer.online", context.metrics().onlinePlayers())
            );
        }

        return hasSegment ? builder : Component.empty();
    }

    /**
     * 统一拼接底部各个信息段。
     */
    private boolean appendFooterSegment(MutableComponent builder, boolean hasSegment, Component segment) {
        if (hasSegment) {
            builder.append(Component.literal("   "));
        }
        builder.append(segment);
        return true;
    }

    /**
     * 向单个客户端同步当前配置。
     */
    private void syncConfigTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncTabConfigPayload(this.config));
    }

    /**
     * 向全服客户端同步当前配置。
     */
    private void syncConfigToAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new SyncTabConfigPayload(this.config));
    }

    /**
     * 向单个客户端同步在线时长数据。
     */
    private void syncOnlineDurationsTo(ServerPlayer player) {
        Map<UUID, String> durations = new HashMap<>();
        for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
            durations.put(onlinePlayer.getUUID(), getOnlineDurationText(onlinePlayer));
        }
        PacketDistributor.sendToPlayer(player, new SyncOnlineDurationsPayload(durations));
    }

    /**
     * 向全服客户端同步在线时长数据。
     */
    private void syncOnlineDurationsToAll(MinecraftServer server) {
        Map<UUID, String> durations = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            durations.put(player.getUUID(), getOnlineDurationText(player));
        }
        PacketDistributor.sendToAllPlayers(new SyncOnlineDurationsPayload(durations));
    }
    
    /**
     * 向全服客户端同步在线时长数据（优化版本）。
     * 
     * <p>性能优化：只在数据真正变化时才发送网络包，避免重复发送相同数据。</p>
     */
    private void syncOnlineDurationsToAllOptimized(MinecraftServer server) {
        Map<UUID, String> durations = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            durations.put(player.getUUID(), getOnlineDurationText(player));
        }
        
        // 检查数据是否变化
        if (!durations.equals(lastOnlineDurations)) {
            PacketDistributor.sendToAllPlayers(new SyncOnlineDurationsPayload(durations));
            lastOnlineDurations = new HashMap<>(durations); // 更新缓存
        }
    }
}
