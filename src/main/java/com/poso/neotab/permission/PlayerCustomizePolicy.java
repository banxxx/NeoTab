package com.poso.neotab.permission;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 玩家自定义权限策略。
 *
 * <p>每个字段对应配置界面中的一个可控项，管理员可以逐项开放或锁定。</p>
 *
 * <p>策略有两个来源：</p>
 * <ul>
 *     <li>全局策略（{@code TabConfig.globalPolicy}）：对所有普通玩家生效</li>
 *     <li>个人专属策略（{@code TabConfig.playerPolicies}）：针对特定 UUID，优先级更高</li>
 * </ul>
 */
public record PlayerCustomizePolicy(
    // ── 顶部信息 ──────────────────────────────────────────────────────────────
    boolean allowTopTitleToggle,
    boolean allowTopTitleEdit,
    boolean allowTopContentToggle,
    boolean allowTopContentEdit,

    // ── 玩家列表 ──────────────────────────────────────────────────────────────
    boolean allowPingDisplayToggle,
    boolean allowDurationToggle,
    boolean allowTitleToggle,
    boolean allowHealthDisplayToggle,
    boolean allowHealthModeChange,

    // ── 底部信息 ──────────────────────────────────────────────────────────────
    boolean allowFooterCustomEdit,
    boolean allowFooterTpsToggle,
    boolean allowFooterMsptToggle,
    boolean allowFooterOnlineToggle,

    // ── 主题 ──────────────────────────────────────────────────────────────────
    boolean allowThemeChange,

    // ── 刷新间隔 ──────────────────────────────────────────────────────────────
    boolean allowRefreshIntervalChange
) {

    /**
     * 全部锁定，服务器默认值。
     * 普通玩家在没有任何策略配置时使用此值。
     */
    public static PlayerCustomizePolicy locked() {
        return new PlayerCustomizePolicy(
            false, false, false, false,
            false, false, false, false, false,
            false, false, false, false,
            false,
            false
        );
    }

    /**
     * 全部开放，管理员（OP ≥ 2）使用此值。
     */
    public static PlayerCustomizePolicy unlocked() {
        return new PlayerCustomizePolicy(
            true, true, true, true,
            true, true, true, true, true,
            true, true, true, true,
            true,
            true
        );
    }

    /**
     * 将两个策略做 AND 合并：只有两者都允许时才开放。
     *
     * <p>用于"全局策略 AND 权限节点"的合并场景。</p>
     */
    public PlayerCustomizePolicy and(PlayerCustomizePolicy other) {
        return new PlayerCustomizePolicy(
            allowTopTitleToggle    && other.allowTopTitleToggle,
            allowTopTitleEdit      && other.allowTopTitleEdit,
            allowTopContentToggle  && other.allowTopContentToggle,
            allowTopContentEdit    && other.allowTopContentEdit,
            allowPingDisplayToggle && other.allowPingDisplayToggle,
            allowDurationToggle    && other.allowDurationToggle,
            allowTitleToggle       && other.allowTitleToggle,
            allowHealthDisplayToggle && other.allowHealthDisplayToggle,
            allowHealthModeChange  && other.allowHealthModeChange,
            allowFooterCustomEdit  && other.allowFooterCustomEdit,
            allowFooterTpsToggle   && other.allowFooterTpsToggle,
            allowFooterMsptToggle  && other.allowFooterMsptToggle,
            allowFooterOnlineToggle && other.allowFooterOnlineToggle,
            allowThemeChange       && other.allowThemeChange,
            allowRefreshIntervalChange && other.allowRefreshIntervalChange
        );
    }

    // ── 网络序列化 ────────────────────────────────────────────────────────────

    /** 写入网络缓冲区。 */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(allowTopTitleToggle);
        buffer.writeBoolean(allowTopTitleEdit);
        buffer.writeBoolean(allowTopContentToggle);
        buffer.writeBoolean(allowTopContentEdit);
        buffer.writeBoolean(allowPingDisplayToggle);
        buffer.writeBoolean(allowDurationToggle);
        buffer.writeBoolean(allowTitleToggle);
        buffer.writeBoolean(allowHealthDisplayToggle);
        buffer.writeBoolean(allowHealthModeChange);
        buffer.writeBoolean(allowFooterCustomEdit);
        buffer.writeBoolean(allowFooterTpsToggle);
        buffer.writeBoolean(allowFooterMsptToggle);
        buffer.writeBoolean(allowFooterOnlineToggle);
        buffer.writeBoolean(allowThemeChange);
        buffer.writeBoolean(allowRefreshIntervalChange);
    }

    /** 从网络缓冲区读取。 */
    public static PlayerCustomizePolicy read(FriendlyByteBuf buffer) {
        return new PlayerCustomizePolicy(
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean()
        );
    }

    // ── JSON 序列化 ───────────────────────────────────────────────────────────

    /** 序列化为 JSON。 */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("allowTopTitleToggle",       allowTopTitleToggle);
        json.addProperty("allowTopTitleEdit",         allowTopTitleEdit);
        json.addProperty("allowTopContentToggle",     allowTopContentToggle);
        json.addProperty("allowTopContentEdit",       allowTopContentEdit);
        json.addProperty("allowPingDisplayToggle",    allowPingDisplayToggle);
        json.addProperty("allowDurationToggle",       allowDurationToggle);
        json.addProperty("allowTitleToggle",          allowTitleToggle);
        json.addProperty("allowHealthDisplayToggle",  allowHealthDisplayToggle);
        json.addProperty("allowHealthModeChange",     allowHealthModeChange);
        json.addProperty("allowFooterCustomEdit",     allowFooterCustomEdit);
        json.addProperty("allowFooterTpsToggle",      allowFooterTpsToggle);
        json.addProperty("allowFooterMsptToggle",     allowFooterMsptToggle);
        json.addProperty("allowFooterOnlineToggle",   allowFooterOnlineToggle);
        json.addProperty("allowThemeChange",          allowThemeChange);
        json.addProperty("allowRefreshIntervalChange", allowRefreshIntervalChange);
        return json;
    }

    /** 从 JSON 反序列化，缺失字段回退到 locked() 的默认值（false）。 */
    public static PlayerCustomizePolicy fromJson(JsonObject json) {
        return new PlayerCustomizePolicy(
            readBool(json, "allowTopTitleToggle",       false),
            readBool(json, "allowTopTitleEdit",         false),
            readBool(json, "allowTopContentToggle",     false),
            readBool(json, "allowTopContentEdit",       false),
            readBool(json, "allowPingDisplayToggle",    false),
            readBool(json, "allowDurationToggle",       false),
            readBool(json, "allowTitleToggle",          false),
            readBool(json, "allowHealthDisplayToggle",  false),
            readBool(json, "allowHealthModeChange",     false),
            readBool(json, "allowFooterCustomEdit",     false),
            readBool(json, "allowFooterTpsToggle",      false),
            readBool(json, "allowFooterMsptToggle",     false),
            readBool(json, "allowFooterOnlineToggle",   false),
            readBool(json, "allowThemeChange",          false),
            readBool(json, "allowRefreshIntervalChange", false)
        );
    }

    private static boolean readBool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }
}