package com.poso.neotab.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import com.poso.neotab.text.RichTextEngine;
import com.poso.neotab.util.MathUtils;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NeoTab 服务端配置模型。
 *
 * <p>当前配置仍然按界面分模块组织，但把真正需要持久化的字段统一收口到这个 record 中，
 * 这样网络同步、磁盘存储和 UI 保存都能共用同一套结构。</p>
 *
 * <p>新增字段：</p>
 * <ul>
 *     <li>{@code globalPolicy} — 全局玩家自定义策略，对所有普通玩家生效</li>
 *     <li>{@code playerPolicies} — 个人专属策略，针对特定 UUID，优先级高于全局策略</li>
 * </ul>
 */
public record TabConfig(
    boolean topTitleEnabled,
    String topTitleText,
    boolean topContentEnabled,
    String topContentText,
    boolean betterPingEnabled,
    boolean onlineDurationEnabled,
    boolean titleEnabled,
    boolean healthDisplayEnabled,
    HealthDisplayMode healthDisplayMode,
    String tabTheme,
    String footerCustomText,
    boolean footerTpsEnabled,
    boolean footerMsptEnabled,
    boolean footerOnlineEnabled,
    int refreshIntervalTicks,
    PlayerCustomizePolicy globalPolicy,
    Map<UUID, PlayerCustomizePolicy> playerPolicies
) {
    /** 顶部标题最多 32 个可见字符。 */
    public static final int MAX_TOP_TITLE_LENGTH = 32;
    /** 顶部内容最多 256 个可见字符（不含富文本标签）。 */
    public static final int MAX_TOP_CONTENT_LENGTH = 256;
    /** 底部自定义信息最多 256 个可见字符（不含富文本标签）。 */
    public static final int MAX_FOOTER_CUSTOM_LENGTH = 256;

    /** 网络传输时的实际字符长度限制（包含富文本标签），应该比可见长度大得多 */
    private static final int NETWORK_TOP_TITLE_LENGTH = 256;
    private static final int NETWORK_TOP_CONTENT_LENGTH = 1024;
    private static final int NETWORK_FOOTER_CUSTOM_LENGTH = 1024;
    /** 刷新间隔最小值，避免每 tick 全量刷新。 */
    public static final int MIN_REFRESH_INTERVAL = 20;
    /** 刷新间隔最大值，避免配置成极端值后几乎不刷新。 */
    public static final int MAX_REFRESH_INTERVAL = 200;
    /** 个人专属策略最大条目数，防止恶意数据导致内存问题。 */
    private static final int MAX_PLAYER_POLICIES = 1000;

    /**
     * 默认配置。
     */
    public static TabConfig defaults() {
        return new TabConfig(
            true, "NeoTab",
            true, "Server status overview",
            true, false, true, false,
            HealthDisplayMode.FULL,
            "vanilla",
            "", true, true, true, 20,
            PlayerCustomizePolicy.locked(),
            new HashMap<>()
        );
    }

    /**
     * 统一清洗配置，防止 UI、网络包或手改 JSON 带入非法值。
     */
    public TabConfig sanitized() {
        Map<UUID, PlayerCustomizePolicy> sanitizedPolicies = playerPolicies != null
            ? new HashMap<>(playerPolicies)
            : new HashMap<>();
        // 限制条目数，防止异常数据
        if (sanitizedPolicies.size() > MAX_PLAYER_POLICIES) {
            sanitizedPolicies.clear();
        }

        return new TabConfig(
            topTitleEnabled,
            sanitizeText(topTitleText, MAX_TOP_TITLE_LENGTH, NETWORK_TOP_TITLE_LENGTH, true),
            topContentEnabled,
            sanitizeText(topContentText, MAX_TOP_CONTENT_LENGTH, NETWORK_TOP_CONTENT_LENGTH, false),
            betterPingEnabled, onlineDurationEnabled, titleEnabled, healthDisplayEnabled,
            healthDisplayMode != null ? healthDisplayMode : HealthDisplayMode.FULL,
            tabTheme != null && !tabTheme.isBlank() ? tabTheme : "vanilla",
            sanitizeText(footerCustomText, MAX_FOOTER_CUSTOM_LENGTH, NETWORK_FOOTER_CUSTOM_LENGTH, false),
            footerTpsEnabled, footerMsptEnabled, footerOnlineEnabled,
            MathUtils.clamp(refreshIntervalTicks, MIN_REFRESH_INTERVAL, MAX_REFRESH_INTERVAL),
            globalPolicy != null ? globalPolicy : PlayerCustomizePolicy.locked(),
            sanitizedPolicies
        );
    }

    /**
     * 写入网络缓冲区。
     */
    public void write(FriendlyByteBuf buffer) {
        TabConfig sanitized = sanitized();
        buffer.writeBoolean(sanitized.topTitleEnabled);
        buffer.writeUtf(sanitized.topTitleText, NETWORK_TOP_TITLE_LENGTH);
        buffer.writeBoolean(sanitized.topContentEnabled);
        buffer.writeUtf(sanitized.topContentText, NETWORK_TOP_CONTENT_LENGTH);
        buffer.writeBoolean(sanitized.betterPingEnabled);
        buffer.writeBoolean(sanitized.onlineDurationEnabled);
        buffer.writeBoolean(sanitized.titleEnabled);
        buffer.writeBoolean(sanitized.healthDisplayEnabled);
        buffer.writeUtf(sanitized.healthDisplayMode.toId(), 16);
        buffer.writeUtf(sanitized.tabTheme, 32);
        buffer.writeUtf(sanitized.footerCustomText, NETWORK_FOOTER_CUSTOM_LENGTH);
        buffer.writeBoolean(sanitized.footerTpsEnabled);
        buffer.writeBoolean(sanitized.footerMsptEnabled);
        buffer.writeBoolean(sanitized.footerOnlineEnabled);
        buffer.writeVarInt(sanitized.refreshIntervalTicks);
        // 策略字段
        sanitized.globalPolicy.write(buffer);
        buffer.writeVarInt(sanitized.playerPolicies.size());
        for (Map.Entry<UUID, PlayerCustomizePolicy> entry : sanitized.playerPolicies.entrySet()) {
            buffer.writeUUID(entry.getKey());
            entry.getValue().write(buffer);
        }
    }

    /**
     * 从网络缓冲区读取配置。
     */
    public static TabConfig read(FriendlyByteBuf buffer) {
        boolean topTitleEnabled     = buffer.readBoolean();
        String  topTitleText        = buffer.readUtf(NETWORK_TOP_TITLE_LENGTH);
        boolean topContentEnabled   = buffer.readBoolean();
        String  topContentText      = buffer.readUtf(NETWORK_TOP_CONTENT_LENGTH);
        boolean betterPingEnabled   = buffer.readBoolean();
        boolean onlineDurationEnabled = buffer.readBoolean();
        boolean titleEnabled        = buffer.readBoolean();
        boolean healthDisplayEnabled = buffer.readBoolean();
        HealthDisplayMode healthDisplayMode = HealthDisplayMode.fromId(buffer.readUtf(16));
        String  tabTheme            = buffer.readUtf(32);
        String  footerCustomText    = buffer.readUtf(NETWORK_FOOTER_CUSTOM_LENGTH);
        boolean footerTpsEnabled    = buffer.readBoolean();
        boolean footerMsptEnabled   = buffer.readBoolean();
        boolean footerOnlineEnabled = buffer.readBoolean();
        int     refreshIntervalTicks = buffer.readVarInt();
        // 策略字段
        PlayerCustomizePolicy globalPolicy = PlayerCustomizePolicy.read(buffer);
        int policyCount = Math.min(buffer.readVarInt(), MAX_PLAYER_POLICIES);
        Map<UUID, PlayerCustomizePolicy> playerPolicies = new HashMap<>(policyCount * 2);
        for (int i = 0; i < policyCount; i++) {
            UUID uuid = buffer.readUUID();
            PlayerCustomizePolicy policy = PlayerCustomizePolicy.read(buffer);
            playerPolicies.put(uuid, policy);
        }
        return new TabConfig(
            topTitleEnabled, topTitleText, topContentEnabled, topContentText,
            betterPingEnabled, onlineDurationEnabled, titleEnabled, healthDisplayEnabled,
            healthDisplayMode, tabTheme, footerCustomText,
            footerTpsEnabled, footerMsptEnabled, footerOnlineEnabled,
            refreshIntervalTicks, globalPolicy, playerPolicies
        ).sanitized();
    }

    /**
     * 序列化为 JSON 保存到磁盘。
     */
    public JsonObject toJson() {
        TabConfig sanitized = sanitized();
        JsonObject json = new JsonObject();
        json.addProperty("topTitleEnabled", sanitized.topTitleEnabled);
        json.addProperty("topTitleText", sanitized.topTitleText);
        json.addProperty("topContentEnabled", sanitized.topContentEnabled);
        json.addProperty("topContentText", sanitized.topContentText);
        json.addProperty("betterPingEnabled", sanitized.betterPingEnabled);
        json.addProperty("onlineDurationEnabled", sanitized.onlineDurationEnabled);
        json.addProperty("titleEnabled", sanitized.titleEnabled);
        json.addProperty("healthDisplayEnabled", sanitized.healthDisplayEnabled);
        json.addProperty("healthDisplayMode", sanitized.healthDisplayMode.toId());
        json.addProperty("tabTheme", sanitized.tabTheme);
        json.addProperty("footerCustomText", sanitized.footerCustomText);
        json.addProperty("footerTpsEnabled", sanitized.footerTpsEnabled);
        json.addProperty("footerMsptEnabled", sanitized.footerMsptEnabled);
        json.addProperty("footerOnlineEnabled", sanitized.footerOnlineEnabled);
        json.addProperty("refreshIntervalTicks", sanitized.refreshIntervalTicks);
        // 策略字段
        json.add("globalPolicy", sanitized.globalPolicy.toJson());
        JsonObject playerPoliciesJson = new JsonObject();
        for (Map.Entry<UUID, PlayerCustomizePolicy> entry : sanitized.playerPolicies.entrySet()) {
            playerPoliciesJson.add(entry.getKey().toString(), entry.getValue().toJson());
        }
        json.add("playerPolicies", playerPoliciesJson);
        return json;
    }

    /**
     * 从 JSON 中恢复配置。
     */
    public static TabConfig fromJson(JsonObject json) {
        TabConfig defaults = defaults();

        // 读取全局策略
        PlayerCustomizePolicy globalPolicy = json.has("globalPolicy")
            ? PlayerCustomizePolicy.fromJson(json.getAsJsonObject("globalPolicy"))
            : PlayerCustomizePolicy.locked();

        // 读取个人专属策略
        Map<UUID, PlayerCustomizePolicy> playerPolicies = new HashMap<>();
        if (json.has("playerPolicies")) {
            JsonObject policiesJson = json.getAsJsonObject("playerPolicies");
            int count = 0;
            for (Map.Entry<String, JsonElement> entry : policiesJson.entrySet()) {
                if (count >= MAX_PLAYER_POLICIES) break;
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerCustomizePolicy policy = PlayerCustomizePolicy.fromJson(entry.getValue().getAsJsonObject());
                    playerPolicies.put(uuid, policy);
                    count++;
                } catch (Exception e) {
                    // 跳过格式错误的条目
                }
            }
        }

        return new TabConfig(
            readBoolean(json, "topTitleEnabled", defaults.topTitleEnabled),
            readString(json, "topTitleText", defaults.topTitleText),
            readBoolean(json, "topContentEnabled", defaults.topContentEnabled),
            readString(json, "topContentText", defaults.topContentText),
            readBoolean(json, "betterPingEnabled", defaults.betterPingEnabled),
            readBoolean(json, "onlineDurationEnabled", defaults.onlineDurationEnabled),
            readBoolean(json, "titleEnabled", defaults.titleEnabled),
            readBoolean(json, "healthDisplayEnabled", defaults.healthDisplayEnabled),
            HealthDisplayMode.fromId(readString(json, "healthDisplayMode", defaults.healthDisplayMode.toId())),
            readString(json, "tabTheme", defaults.tabTheme),
            readString(json, "footerCustomText", defaults.footerCustomText),
            readBoolean(json, "footerTpsEnabled", defaults.footerTpsEnabled),
            readBoolean(json, "footerMsptEnabled", defaults.footerMsptEnabled),
            readBoolean(json, "footerOnlineEnabled", defaults.footerOnlineEnabled),
            readInt(json, "refreshIntervalTicks", defaults.refreshIntervalTicks),
            globalPolicy,
            playerPolicies
        ).sanitized();
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static String readString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    /**
     * 统一的文本清洗方法，同时处理可见长度和网络传输长度限制。
     */
    private static String sanitizeText(String value, int maxVisibleLength, int maxNetworkLength, boolean singleLine) {
        String visibleTrimmed = singleLine
            ? normalizeSingleLine(value, maxVisibleLength)
            : normalizeMultiLine(value, maxVisibleLength);

        if (visibleTrimmed.length() > maxNetworkLength) {
            return visibleTrimmed.substring(0, maxNetworkLength);
        }

        return visibleTrimmed;
    }

    /**
     * 单行输入清洗：去掉换行并截断（按可见文本长度）。
     */
    private static String normalizeSingleLine(String value, int maxVisibleLength) {
        String normalized = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
        return RichTextEngine.trimToVisibleLength(normalized, maxVisibleLength, true);
    }

    /**
     * 多行输入清洗：统一换行符并截断（按可见文本长度）。
     */
    private static String normalizeMultiLine(String value, int maxVisibleLength) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return RichTextEngine.trimToVisibleLength(normalized, maxVisibleLength, false);
    }
    
    /**
     * 创建配置的深拷贝。
     * 
     * @return 新的 TabConfig 实例，包含相同的值
     */
    public TabConfig copy() {
        // 深拷贝 playerPolicies Map
        Map<UUID, PlayerCustomizePolicy> copiedPolicies = new HashMap<>(this.playerPolicies);
        
        return new TabConfig(
            this.topTitleEnabled,
            this.topTitleText,
            this.topContentEnabled,
            this.topContentText,
            this.betterPingEnabled,
            this.onlineDurationEnabled,
            this.titleEnabled,
            this.healthDisplayEnabled,
            this.healthDisplayMode,
            this.tabTheme,
            this.footerCustomText,
            this.footerTpsEnabled,
            this.footerMsptEnabled,
            this.footerOnlineEnabled,
            this.refreshIntervalTicks,
            this.globalPolicy,
            copiedPolicies
        );
    }
}