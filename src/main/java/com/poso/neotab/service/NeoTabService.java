package com.poso.neotab.service;

import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.PlayerTabConfigRepository;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.config.TabConfigRepository;
import com.poso.neotab.network.payload.SyncCustomizePolicyPayload;
import com.poso.neotab.network.payload.SyncOnlineDurationsPayload;
import com.poso.neotab.network.payload.SyncPlayerHealthPayload;
import com.poso.neotab.network.payload.SyncTabConfigPayload;
import com.poso.neotab.permission.NeoTabPermissions;
import com.poso.neotab.permission.PlayerCustomizePolicy;
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
 *     <li>管理玩家个人配置缓存，合并生成每位玩家的有效配置</li>
 * </ul>
 */
public final class NeoTabService {

    // ── 仓库 ──────────────────────────────────────────────────────────────────

    /** 负责把服务器配置落到世界目录下的 serverconfig 中。 */
    private final TabConfigRepository repository = new TabConfigRepository();

    /** 负责读写玩家个人配置文件。 */
    private final PlayerTabConfigRepository playerRepository = new PlayerTabConfigRepository();

    // ── 服务器配置 ────────────────────────────────────────────────────────────

    /** 当前正在使用的服务器配置，服务端运行时所有逻辑都读这个对象。 */
    private TabConfig config = TabConfig.defaults();

    /** 当前服务端实例，玩家加入/离开时需要用到。 */
    private MinecraftServer currentServer = null;

    // ── 玩家个人配置缓存 ──────────────────────────────────────────────────────

    /**
     * 玩家个人配置内存缓存。
     *
     * <p>玩家加入时从磁盘加载，离开时保存并清理。
     * 使用 ConcurrentHashMap 保证线程安全。</p>
     */
    private final Map<UUID, PlayerTabConfig> playerConfigs = new ConcurrentHashMap<>();

    // ── Tick 计数器与指标缓存 ─────────────────────────────────────────────────

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
     * 上一次同步的血量数据。
     *
     * <p>性能优化：只在数据变化时才发包。</p>
     */
    private Map<UUID, Float> lastPlayerHealths    = new HashMap<>();
    private Map<UUID, Float> lastPlayerMaxHealths = new HashMap<>();
    
    /**
     * 在线时长同步计数器。
     * 
     * <p>性能优化：在线时长不需要每个刷新间隔都同步，每秒同步一次即可。</p>
     */
    private int onlineDurationSyncCounter = 0;
    
    /** 在线时长同步间隔（tick）：每秒同步一次 */
    private static final int ONLINE_DURATION_SYNC_INTERVAL = 20;

    // ── 公共 API ──────────────────────────────────────────────────────────────

    /**
     * 返回当前服务器配置（不含个人覆盖）。
     */
    public TabConfig getConfig() {
        return config;
    }

    /**
     * 获取指定玩家的有效配置（服务器配置 + 个人配置合并后的结果）。
     *
     * <p>合并规则：</p>
     * <ol>
     *     <li>解析该玩家的有效策略（OP / 个人专属 / 全局策略 AND 权限节点）</li>
     *     <li>将玩家个人配置中被策略允许的字段覆盖到服务器配置上</li>
     * </ol>
     *
     * @param player 玩家对象
     * @return 该玩家实际看到的有效配置
     */
    public TabConfig getEffectiveConfig(ServerPlayer player) {
        PlayerCustomizePolicy policy = NeoTabPermissions.resolvePolicy(player, config);
        PlayerTabConfig personal = playerConfigs.getOrDefault(
            player.getUUID(), PlayerTabConfig.defaults(player.getUUID())
        );
        return personal.mergeInto(config, policy);
    }

    /**
     * 获取指定玩家的个人配置（原始值，未合并）。
     * 如果玩家没有个人配置，返回全部跟随服务器的默认值。
     */
    public PlayerTabConfig getPlayerConfig(UUID playerId) {
        return playerConfigs.getOrDefault(playerId, PlayerTabConfig.defaults(playerId));
    }

    /**
     * 处理玩家从客户端提交的个人配置保存请求。
     *
     * <p>服务端二次校验：逐字段检查是否在策略允许范围内，
     * 拒绝玩家修改不被允许的字段，防止客户端绕过限制。</p>
     *
     * @param player   提交配置的玩家
     * @param incoming 客户端发来的个人配置
     */
    public void savePlayerConfig(ServerPlayer player, PlayerTabConfig incoming) {
        PlayerCustomizePolicy policy = NeoTabPermissions.resolvePolicy(player, config);

        // 服务端二次校验：只保留策略允许的字段，其余字段强制为 null（跟随服务器）
        PlayerTabConfig sanitized = new PlayerTabConfig(
            player.getUUID(),
            policy.allowTopTitleToggle()      ? incoming.topTitleEnabled()      : null,
            policy.allowTopTitleEdit()        ? incoming.topTitleText()         : null,
            policy.allowTopContentToggle()    ? incoming.topContentEnabled()    : null,
            policy.allowTopContentEdit()      ? incoming.topContentText()       : null,
            policy.allowPingDisplayToggle()   ? incoming.betterPingEnabled()    : null,
            policy.allowDurationToggle()      ? incoming.onlineDurationEnabled() : null,
            policy.allowTitleToggle()         ? incoming.titleEnabled()         : null,
            policy.allowHealthDisplayToggle() ? incoming.healthDisplayEnabled() : null,
            policy.allowHealthModeChange()    ? incoming.healthDisplayMode()    : null,
            policy.allowFooterCustomEdit()    ? incoming.footerCustomText()     : null,
            policy.allowFooterTpsToggle()     ? incoming.footerTpsEnabled()     : null,
            policy.allowFooterMsptToggle()    ? incoming.footerMsptEnabled()    : null,
            policy.allowFooterOnlineToggle()  ? incoming.footerOnlineEnabled()  : null,
            policy.allowThemeChange()         ? incoming.tabTheme()             : null
        );

        // 更新内存缓存
        playerConfigs.put(player.getUUID(), sanitized);

        // 持久化到磁盘
        if (currentServer != null) {
            playerRepository.save(currentServer, sanitized);
        }

        // 重新同步有效配置给该玩家（让客户端立即看到合并后的结果）
        syncEffectiveConfigTo(player);

        player.sendSystemMessage(Component.translatable("message.neotab.personal_saved"));
    }

    // ── 生命周期 ──────────────────────────────────────────────────────────────

    /**
     * 从磁盘重新加载服务器配置，并立即刷新在线玩家 TAB。
     */
    public void reload(MinecraftServer server) {
        this.config = repository.load(server).sanitized();
        this.tickCounter = 0;
        this.onlineDurationSyncCounter = 0; // 重置在线时长同步计数器
        this.currentServer = server;

        // 清空缓存，确保使用新配置
        this.cachedMetrics = null;
        this.lastOnlineDurations.clear();
        this.lastPlayerHealths.clear();
        this.lastPlayerMaxHealths.clear();
        RichTextEngine.clearCache();

        // 策略可能变化，重新同步每位玩家的有效配置
        syncEffectiveConfigToAll(server);
        applyAll(server);
        refreshAllNames(server);
    }

    /**
     * 服务端启动完成后加载配置。
     */
    public void onServerStarted(MinecraftServer server) {
        this.currentServer = server;
        reload(server);
    }

    /**
     * 服务端关闭时清理内存状态。
     *
     * <p>这里不需要额外保存，因为保存动作只在玩家主动提交配置时进行。</p>
     */
    public void onServerStopped() {
        this.tickCounter = 0;
        this.onlineDurationSyncCounter = 0; // 重置在线时长同步计数器
        this.config = TabConfig.defaults();
        this.cachedMetrics = null;
        this.currentServer = null;
        this.lastOnlineDurations.clear();
        this.lastPlayerHealths.clear();
        this.lastPlayerMaxHealths.clear();
        this.playerConfigs.clear();
        RichTextEngine.clearCache();
    }

    /**
     * 新玩家加入时：
     * <ol>
     *     <li>从磁盘加载该玩家的个人配置到缓存</li>
     *     <li>同步有效配置（服务器 + 个人合并）给该玩家</li>
     *     <li>刷新所有人的玩家名显示</li>
     * </ol>
     */
    public void onPlayerJoined(ServerPlayer player) {
        // 加载个人配置到缓存
        if (currentServer != null) {
            PlayerTabConfig personal = playerRepository.load(currentServer, player.getUUID());
            playerConfigs.put(player.getUUID(), personal);
        }

        // 同步有效配置（含个人覆盖）给该玩家
        syncEffectiveConfigTo(player);
        // 同步该玩家的有效策略，客户端据此决定自定义界面哪些选项可操作
        syncPolicyTo(player);
        applyPlayer(player);

        // 玩家数量变化，需要刷新所有人的显示
        refreshAllNames(player.server);

        // 如果启用了在线时长显示，同步在线时长数据
        if (config.onlineDurationEnabled()) {
            syncOnlineDurationsTo(player);
            syncOnlineDurationsToAllOptimized(player.server);
        }

        // 如果启用了血量显示，同步血量数据
        if (config.healthDisplayEnabled()) {
            syncPlayerHealthsTo(player);
            syncPlayerHealthsToAllOptimized(player.server);
        }
    }

    /**
     * 玩家离开时：清理缓存，刷新全服 TAB。
     */
    public void onPlayerLeft(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // 清理个人配置缓存（磁盘文件已在 savePlayerConfig 时写入，无需再写）
        playerConfigs.remove(uuid);

        // 清理称号缓存，避免内存泄漏
        com.poso.neotab.api.NeoTabAPI.invalidateTitleCache(uuid);

        // 玩家数量变化，需要刷新所有人的显示
        applyAll(player.server);
        refreshAllNames(player.server);
    }

    /**
     * 服务端每 tick 调用一次。
     * 
     * <p>性能优化：在线时长同步频率降低到每秒一次，减少网络包发送和 HashMap 分配。</p>
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

        // 在线时长每秒同步一次即可（不需要每个刷新间隔都同步）
        if (config.onlineDurationEnabled()) {
            onlineDurationSyncCounter++;
            if (onlineDurationSyncCounter >= ONLINE_DURATION_SYNC_INTERVAL) {
                onlineDurationSyncCounter = 0;
                syncOnlineDurationsToAllOptimized(server);
            }
        } else {
            onlineDurationSyncCounter = 0; // 重置计数器
        }

        // 血量需要实时同步（每个刷新间隔）
        if (config.healthDisplayEnabled()) {
            syncPlayerHealthsToAllOptimized(server);
        }
    }

    /**
     * 处理管理员从客户端 UI 提交的新服务器配置。
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
        this.onlineDurationSyncCounter = 0; // 重置在线时长同步计数器
        this.lastOnlineDurations.clear();
        this.lastPlayerHealths.clear();
        this.lastPlayerMaxHealths.clear();
        RichTextEngine.clearCache();

        // 策略可能已变更，重新同步每位玩家的有效配置
        syncEffectiveConfigToAll(actor.server);
        applyAll(actor.server);
        refreshAllNames(actor.server);

        if (config.onlineDurationEnabled()) {
            syncOnlineDurationsToAll(actor.server);
        }

        if (config.healthDisplayEnabled()) {
            syncPlayerHealthsToAll(actor.server);
        }

        actor.sendSystemMessage(Component.translatable("message.neotab.saved"));
    }

    // ── 渲染相关 ──────────────────────────────────────────────────────────────

    /**
     * 把 TAB 头/尾内容重新应用到所有在线玩家。
     *
     * <p>注意：header/footer 使用服务器配置渲染（全局内容），
     * 不受玩家个人配置影响（个人配置主要影响客户端渲染行为）。</p>
     */
    public void applyAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyPlayer(player);
        }
    }

    /**
     * 为单个玩家计算并设置 TAB 顶部/底部文本。
     *
     * <p>性能优化：使用缓存的指标数据，避免重复采样。</p>
     */
    public void applyPlayer(ServerPlayer player) {
        TabMetrics metrics = cachedMetrics != null ? cachedMetrics : TabMetrics.sample(player.server);
        PlaceholderContext context = PlaceholderContext.forViewerWithMetrics(player.server, player, metrics);
        Component header = buildHeader(context);
        Component footer = buildFooter(context);
        player.setTabListHeaderFooter(header, footer);
    }

    /**
     * 强制刷新所有玩家在 TAB 中的显示名。
     */
    public void refreshAllNames(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.refreshTabListName();
        }
    }

    /**
     * 供玩家列表渲染逻辑读取指定玩家的在线时长文本。
     */
    public String getOnlineDurationText(ServerPlayer player) {
        int playTimeTicks = player.getStats().getValue(
            net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));
        long totalSeconds = playTimeTicks / 20L;
        long totalHours = Math.max(1L, (totalSeconds + 3599L) / 3600L);
        long days = totalHours / 24L;
        long hours = totalHours % 24L;

        if (days <= 0L) {
            return totalHours + "h";
        }
        return days + "d" + Math.max(1L, hours) + "h";
    }

    // ── 配置同步 ──────────────────────────────────────────────────────────────

    /**
     * 向单个玩家同步其有效配置（服务器配置 + 个人配置合并后）。
     *
     * <p>与 {@link #syncConfigTo} 的区别：这里发送的是合并后的有效配置，
     * 客户端收到后直接用于渲染，无需再做合并。</p>
     */
    private void syncEffectiveConfigTo(ServerPlayer player) {
        TabConfig effective = getEffectiveConfig(player);
        PacketDistributor.sendToPlayer(player, new SyncTabConfigPayload(effective));
    }

    /**
     * 向所有在线玩家同步各自的有效配置。
     *
     * <p>每位玩家收到的配置可能不同（取决于个人配置和策略）。</p>
     */
    private void syncEffectiveConfigToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncEffectiveConfigTo(player);
            syncPolicyTo(player);
        }
    }

    /**
     * 向单个玩家同步其有效策略。
     */
    private void syncPolicyTo(ServerPlayer player) {
        PlayerCustomizePolicy policy = NeoTabPermissions.resolvePolicy(player, config);
        PacketDistributor.sendToPlayer(player, new SyncCustomizePolicyPayload(policy));
    }

    /**
     * 向单个客户端同步当前服务器配置（不含个人覆盖，仅供管理员界面使用）。
     */
    private void syncConfigTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncTabConfigPayload(this.config));
    }

    /**
     * 向全服客户端同步当前服务器配置。
     *
     * @deprecated 优先使用 {@link #syncEffectiveConfigToAll}，
     *             此方法仅在不需要个人配置合并的场景下使用。
     */
    @Deprecated
    private void syncConfigToAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new SyncTabConfigPayload(this.config));
    }

    // ── 在线时长同步 ──────────────────────────────────────────────────────────

    private void syncOnlineDurationsTo(ServerPlayer player) {
        Map<UUID, String> durations = new HashMap<>();
        for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
            durations.put(onlinePlayer.getUUID(), getOnlineDurationText(onlinePlayer));
        }
        PacketDistributor.sendToPlayer(player, new SyncOnlineDurationsPayload(durations));
    }

    private void syncOnlineDurationsToAll(MinecraftServer server) {
        Map<UUID, String> durations = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            durations.put(player.getUUID(), getOnlineDurationText(player));
        }
        PacketDistributor.sendToAllPlayers(new SyncOnlineDurationsPayload(durations));
    }

    private void syncOnlineDurationsToAllOptimized(MinecraftServer server) {
        java.util.List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Map<UUID, String> current = new HashMap<>(players.size() * 2);
        for (ServerPlayer player : players) {
            current.put(player.getUUID(), getOnlineDurationText(player));
        }
        if (!current.equals(lastOnlineDurations)) {
            PacketDistributor.sendToAllPlayers(new SyncOnlineDurationsPayload(current));
            lastOnlineDurations = current;
        }
    }

    // ── 血量同步 ──────────────────────────────────────────────────────────────

    private void syncPlayerHealthsTo(ServerPlayer player) {
        Map<UUID, Float> healths    = new HashMap<>();
        Map<UUID, Float> maxHealths = new HashMap<>();
        for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
            healths.put(onlinePlayer.getUUID(), onlinePlayer.getHealth());
            maxHealths.put(onlinePlayer.getUUID(), onlinePlayer.getMaxHealth());
        }
        PacketDistributor.sendToPlayer(player, new SyncPlayerHealthPayload(healths, maxHealths));
    }

    private void syncPlayerHealthsToAll(MinecraftServer server) {
        Map<UUID, Float> healths    = new HashMap<>();
        Map<UUID, Float> maxHealths = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            healths.put(player.getUUID(), player.getHealth());
            maxHealths.put(player.getUUID(), player.getMaxHealth());
        }
        PacketDistributor.sendToAllPlayers(new SyncPlayerHealthPayload(healths, maxHealths));
    }

    private void syncPlayerHealthsToAllOptimized(MinecraftServer server) {
        java.util.List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int capacity = players.size() * 2;
        Map<UUID, Float> current    = new HashMap<>(capacity);
        Map<UUID, Float> currentMax = new HashMap<>(capacity);
        for (ServerPlayer player : players) {
            current.put(player.getUUID(), player.getHealth());
            currentMax.put(player.getUUID(), player.getMaxHealth());
        }
        if (!current.equals(lastPlayerHealths) || !currentMax.equals(lastPlayerMaxHealths)) {
            PacketDistributor.sendToAllPlayers(new SyncPlayerHealthPayload(current, currentMax));
            lastPlayerHealths    = current;
            lastPlayerMaxHealths = currentMax;
        }
    }

    // ── 私有渲染辅助 ──────────────────────────────────────────────────────────

    private Component buildHeader(PlaceholderContext context) {
        MutableComponent result = Component.empty();
        boolean hasContent = false;

        if (config.topTitleEnabled() && !config.topTitleText().isBlank()) {
            Component titleComponent = PlaceholderEngine.renderSingleLine(config.topTitleText(), context);
            if (!titleComponent.equals(Component.empty())) {
                result.append(titleComponent);
                hasContent = true;
            }
        }

        if (config.topContentEnabled() && !config.topContentText().isBlank()) {
            Component contentComponent = PlaceholderEngine.renderMultiline(config.topContentText(), context);
            if (!contentComponent.equals(Component.empty())) {
                if (hasContent) {
                    result.append(Component.literal("\n"));
                }
                result.append(contentComponent);
                hasContent = true;
            }
        }

        return hasContent ? result : Component.empty();
    }

    private Component buildFooter(PlaceholderContext context) {
        MutableComponent builder = Component.empty();
        boolean hasSegment = false;

        if (!config.footerCustomText().isBlank()) {
            Component customComponent = PlaceholderEngine.renderMultiline(config.footerCustomText(), context);
            if (!customComponent.equals(Component.empty())) {
                hasSegment = appendFooterSegment(builder, hasSegment, customComponent);
            }
        }

        if (config.footerTpsEnabled()) {
            hasSegment = appendFooterSegment(builder, hasSegment,
                Component.literal("TPS: " + context.metrics().tpsText()));
        }

        if (config.footerMsptEnabled()) {
            hasSegment = appendFooterSegment(builder, hasSegment,
                Component.literal("MSPT: " + context.metrics().msptText()));
        }

        if (config.footerOnlineEnabled()) {
            hasSegment = appendFooterSegment(builder, hasSegment,
                Component.translatable("tab.neotab.footer.online", context.metrics().onlinePlayers()));
        }

        return hasSegment ? builder : Component.empty();
    }

    private boolean appendFooterSegment(MutableComponent builder, boolean hasSegment, Component segment) {
        if (hasSegment) {
            builder.append(Component.literal("   "));
        }
        builder.append(segment);
        return true;
    }
}
