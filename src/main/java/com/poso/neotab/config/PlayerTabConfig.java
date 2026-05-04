package com.poso.neotab.config;

import com.google.gson.JsonObject;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家个人 TAB 配置。
 *
 * <p>所有可自定义字段均为可空（{@code @Nullable}），
 * {@code null} 表示"跟随服务器设置"，不覆盖服务器值。</p>
 *
 * <p>通过 {@link #mergeInto(TabConfig, PlayerCustomizePolicy)} 将个人配置
 * 叠加到服务器配置上，生成最终有效配置。</p>
 */
public record PlayerTabConfig(
    UUID playerId,

    // ── 顶部信息 ──────────────────────────────────────────────────────────────
    @Nullable Boolean topTitleEnabled,
    @Nullable String  topTitleText,
    @Nullable Boolean topContentEnabled,
    @Nullable String  topContentText,

    // ── 玩家列表 ──────────────────────────────────────────────────────────────
    @Nullable Boolean betterPingEnabled,
    @Nullable Boolean onlineDurationEnabled,
    @Nullable Boolean titleEnabled,
    @Nullable Boolean healthDisplayEnabled,
    @Nullable HealthDisplayMode healthDisplayMode,

    // ── 底部信息 ──────────────────────────────────────────────────────────────
    @Nullable String  footerCustomText,
    @Nullable Boolean footerTpsEnabled,
    @Nullable Boolean footerMsptEnabled,
    @Nullable Boolean footerOnlineEnabled,

    // ── 主题 ──────────────────────────────────────────────────────────────────
    @Nullable String  tabTheme
) {

    /** 全部跟随服务器设置的默认个人配置。 */
    public static PlayerTabConfig defaults(UUID playerId) {
        return new PlayerTabConfig(
            playerId,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null
        );
    }

    /**
     * 将个人配置叠加到服务器配置上，生成最终有效配置。
     *
     * <p>合并规则：策略允许 AND 个人有设置 → 用个人值，否则用服务器值。</p>
     *
     * <p>注意：策略字段（globalPolicy / playerPolicies）和 refreshIntervalTicks
     * 始终来自服务器配置，不受个人配置影响。</p>
     */
    public TabConfig mergeInto(TabConfig server, PlayerCustomizePolicy policy) {
        return new TabConfig(
            // 顶部信息
            policy.allowTopTitleToggle()      && topTitleEnabled    != null ? topTitleEnabled    : server.topTitleEnabled(),
            policy.allowTopTitleEdit()        && topTitleText       != null ? topTitleText       : server.topTitleText(),
            policy.allowTopContentToggle()    && topContentEnabled  != null ? topContentEnabled  : server.topContentEnabled(),
            policy.allowTopContentEdit()      && topContentText     != null ? topContentText     : server.topContentText(),
            // 玩家列表
            policy.allowPingDisplayToggle()   && betterPingEnabled  != null ? betterPingEnabled  : server.betterPingEnabled(),
            policy.allowDurationToggle()      && onlineDurationEnabled != null ? onlineDurationEnabled : server.onlineDurationEnabled(),
            policy.allowTitleToggle()         && titleEnabled       != null ? titleEnabled       : server.titleEnabled(),
            policy.allowHealthDisplayToggle() && healthDisplayEnabled != null ? healthDisplayEnabled : server.healthDisplayEnabled(),
            policy.allowHealthModeChange()    && healthDisplayMode  != null ? healthDisplayMode  : server.healthDisplayMode(),
            // 主题
            policy.allowThemeChange()         && tabTheme           != null ? tabTheme           : server.tabTheme(),
            // 底部信息
            policy.allowFooterCustomEdit()    && footerCustomText   != null ? footerCustomText   : server.footerCustomText(),
            policy.allowFooterTpsToggle()     && footerTpsEnabled   != null ? footerTpsEnabled   : server.footerTpsEnabled(),
            policy.allowFooterMsptToggle()    && footerMsptEnabled  != null ? footerMsptEnabled  : server.footerMsptEnabled(),
            policy.allowFooterOnlineToggle()  && footerOnlineEnabled != null ? footerOnlineEnabled : server.footerOnlineEnabled(),
            // 以下字段始终来自服务器，不开放给玩家
            server.refreshIntervalTicks(),
            server.globalPolicy(),
            server.playerPolicies()
        );
    }

    // ── 网络序列化 ────────────────────────────────────────────────────────────

    private static final int NETWORK_TEXT_LENGTH = 1024;

    /** 写入网络缓冲区。 */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        writeNullableBoolean(buffer, topTitleEnabled);
        writeNullableString(buffer, topTitleText);
        writeNullableBoolean(buffer, topContentEnabled);
        writeNullableString(buffer, topContentText);
        writeNullableBoolean(buffer, betterPingEnabled);
        writeNullableBoolean(buffer, onlineDurationEnabled);
        writeNullableBoolean(buffer, titleEnabled);
        writeNullableBoolean(buffer, healthDisplayEnabled);
        // HealthDisplayMode: null → ""，有值 → id 字符串
        buffer.writeUtf(healthDisplayMode != null ? healthDisplayMode.toId() : "", 16);
        writeNullableString(buffer, footerCustomText);
        writeNullableBoolean(buffer, footerTpsEnabled);
        writeNullableBoolean(buffer, footerMsptEnabled);
        writeNullableBoolean(buffer, footerOnlineEnabled);
        writeNullableString(buffer, tabTheme);
    }

    /** 从网络缓冲区读取。 */
    public static PlayerTabConfig read(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        Boolean topTitleEnabled     = readNullableBoolean(buffer);
        String  topTitleText        = readNullableString(buffer);
        Boolean topContentEnabled   = readNullableBoolean(buffer);
        String  topContentText      = readNullableString(buffer);
        Boolean betterPingEnabled   = readNullableBoolean(buffer);
        Boolean onlineDurationEnabled = readNullableBoolean(buffer);
        Boolean titleEnabled        = readNullableBoolean(buffer);
        Boolean healthDisplayEnabled = readNullableBoolean(buffer);
        String  healthModeId        = buffer.readUtf(16);
        HealthDisplayMode healthDisplayMode = healthModeId.isEmpty() ? null : HealthDisplayMode.fromId(healthModeId);
        String  footerCustomText    = readNullableString(buffer);
        Boolean footerTpsEnabled    = readNullableBoolean(buffer);
        Boolean footerMsptEnabled   = readNullableBoolean(buffer);
        Boolean footerOnlineEnabled = readNullableBoolean(buffer);
        String  tabTheme            = readNullableString(buffer);
        return new PlayerTabConfig(
            playerId,
            topTitleEnabled, topTitleText, topContentEnabled, topContentText,
            betterPingEnabled, onlineDurationEnabled, titleEnabled,
            healthDisplayEnabled, healthDisplayMode,
            footerCustomText, footerTpsEnabled, footerMsptEnabled, footerOnlineEnabled,
            tabTheme
        );
    }

    // ── JSON 序列化 ───────────────────────────────────────────────────────────

    /** 序列化为 JSON。 */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("playerId", playerId.toString());
        writeNullableBoolJson(json, "topTitleEnabled",      topTitleEnabled);
        writeNullableStrJson(json,  "topTitleText",         topTitleText);
        writeNullableBoolJson(json, "topContentEnabled",    topContentEnabled);
        writeNullableStrJson(json,  "topContentText",       topContentText);
        writeNullableBoolJson(json, "betterPingEnabled",    betterPingEnabled);
        writeNullableBoolJson(json, "onlineDurationEnabled", onlineDurationEnabled);
        writeNullableBoolJson(json, "titleEnabled",         titleEnabled);
        writeNullableBoolJson(json, "healthDisplayEnabled", healthDisplayEnabled);
        if (healthDisplayMode != null) {
            json.addProperty("healthDisplayMode", healthDisplayMode.toId());
        }
        writeNullableStrJson(json,  "footerCustomText",     footerCustomText);
        writeNullableBoolJson(json, "footerTpsEnabled",     footerTpsEnabled);
        writeNullableBoolJson(json, "footerMsptEnabled",    footerMsptEnabled);
        writeNullableBoolJson(json, "footerOnlineEnabled",  footerOnlineEnabled);
        writeNullableStrJson(json,  "tabTheme",             tabTheme);
        return json;
    }

    /** 从 JSON 反序列化。 */
    public static PlayerTabConfig fromJson(UUID playerId, JsonObject json) {
        return new PlayerTabConfig(
            playerId,
            readNullableBoolJson(json, "topTitleEnabled"),
            readNullableStrJson(json,  "topTitleText"),
            readNullableBoolJson(json, "topContentEnabled"),
            readNullableStrJson(json,  "topContentText"),
            readNullableBoolJson(json, "betterPingEnabled"),
            readNullableBoolJson(json, "onlineDurationEnabled"),
            readNullableBoolJson(json, "titleEnabled"),
            readNullableBoolJson(json, "healthDisplayEnabled"),
            json.has("healthDisplayMode") ? HealthDisplayMode.fromId(json.get("healthDisplayMode").getAsString()) : null,
            readNullableStrJson(json,  "footerCustomText"),
            readNullableBoolJson(json, "footerTpsEnabled"),
            readNullableBoolJson(json, "footerMsptEnabled"),
            readNullableBoolJson(json, "footerOnlineEnabled"),
            readNullableStrJson(json,  "tabTheme")
        );
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────────────

    /** 可空 Boolean 网络写入：1字节标志位 + 可选 1字节值。 */
    private static void writeNullableBoolean(FriendlyByteBuf buffer, @Nullable Boolean value) {
        buffer.writeBoolean(value != null);
        if (value != null) buffer.writeBoolean(value);
    }

    /** 可空 Boolean 网络读取。 */
    @Nullable
    private static Boolean readNullableBoolean(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readBoolean() : null;
    }

    /** 可空 String 网络写入：1字节标志位 + 可选字符串。 */
    private static void writeNullableString(FriendlyByteBuf buffer, @Nullable String value) {
        buffer.writeBoolean(value != null);
        if (value != null) buffer.writeUtf(value, NETWORK_TEXT_LENGTH);
    }

    /** 可空 String 网络读取。 */
    @Nullable
    private static String readNullableString(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUtf(NETWORK_TEXT_LENGTH) : null;
    }

    private static void writeNullableBoolJson(JsonObject json, String key, @Nullable Boolean value) {
        if (value != null) json.addProperty(key, value);
    }

    private static void writeNullableStrJson(JsonObject json, String key, @Nullable String value) {
        if (value != null) json.addProperty(key, value);
    }

    @Nullable
    private static Boolean readNullableBoolJson(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsBoolean() : null;
    }

    @Nullable
    private static String readNullableStrJson(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }
    
    /**
     * 从 TabConfig 创建 PlayerTabConfig。
     * 
     * <p>根据权限策略，将允许自定义的字段从 TabConfig 复制到 PlayerTabConfig。</p>
     * 
     * @param config TabConfig 源配置
     * @param policy 权限策略
     * @return 新的 PlayerTabConfig 实例
     */
    public static PlayerTabConfig fromTabConfig(TabConfig config, PlayerCustomizePolicy policy) {
        // 获取当前玩家的 UUID（从客户端获取）
        UUID playerId = net.minecraft.client.Minecraft.getInstance().player != null
            ? net.minecraft.client.Minecraft.getInstance().player.getUUID()
            : new UUID(0, 0);
        
        return new PlayerTabConfig(
            playerId,
            // 顶部信息
            policy.allowTopTitleToggle() ? config.topTitleEnabled() : null,
            policy.allowTopTitleEdit() ? config.topTitleText() : null,
            policy.allowTopContentToggle() ? config.topContentEnabled() : null,
            policy.allowTopContentEdit() ? config.topContentText() : null,
            // 玩家列表
            policy.allowPingDisplayToggle() ? config.betterPingEnabled() : null,
            policy.allowDurationToggle() ? config.onlineDurationEnabled() : null,
            policy.allowTitleToggle() ? config.titleEnabled() : null,
            policy.allowHealthDisplayToggle() ? config.healthDisplayEnabled() : null,
            policy.allowHealthModeChange() ? config.healthDisplayMode() : null,
            // 底部信息
            policy.allowFooterCustomEdit() ? config.footerCustomText() : null,
            policy.allowFooterTpsToggle() ? config.footerTpsEnabled() : null,
            policy.allowFooterMsptToggle() ? config.footerMsptEnabled() : null,
            policy.allowFooterOnlineToggle() ? config.footerOnlineEnabled() : null,
            // 主题
            policy.allowThemeChange() ? config.tabTheme() : null
        );
    }
}