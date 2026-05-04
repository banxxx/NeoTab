package com.poso.neotab.client.tab;

import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.config.TabConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static com.poso.neotab.client.tab.TabHealthRenderer.HEARTS_W;
import static com.poso.neotab.client.tab.TabHealthRenderer.SECTION_GAP;

/**
 * TAB 列表列宽计算辅助类。
 *
 * <p>负责根据当前配置（betterPing / onlineDuration / health）计算
 * 每行右侧信息区所需的最大像素宽度，供 Mixin 注入点使用。</p>
 *
 * <p>所有方法均为静态，不持有任何状态。</p>
 */
public final class TabColumnWidthCalculator {

    /** 原版信号图标宽度（px） */
    public static final int PING_ICON_W = 13;
    /** 血量图标与用户名之间的最小保留间距（px） */
    public static final int NAME_GAP    = 8;

    private TabColumnWidthCalculator() {}

    // ─────────────────────────────────────────────────────────────────────────
    // 列宽计算
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 计算所有玩家行中最大的右侧信息区宽度，作为列宽扩展量。
     *
     * @param font            客户端字体
     * @param config          当前 TAB 配置
     * @param onlinePlayers   当前在线玩家集合
     * @param cachedHealthAreaW 已缓存的统一血量区宽度（-1 表示未缓存）
     * @param playerInfoMap   UUID→PlayerInfo 快速查找表
     */
    public static int calcRequiredSpace(Font font, TabConfig config,
                                        Collection<PlayerInfo> onlinePlayers,
                                        int cachedHealthAreaW,
                                        Map<UUID, PlayerInfo> playerInfoMap) {
        int maxW = 0;

        for (var pi : onlinePlayers) {
            maxW = Math.max(maxW, calcRowWidth(font, config, pi.getProfile().getId(),
                    cachedHealthAreaW, playerInfoMap));
        }

        if (maxW == 0) {
            maxW = calcRowWidthFallback(font, config);
        }

        // 血量显示开启时额外加 NAME_GAP，对应渲染时 rightEdge 扣掉的 NAME_GAP
        int extra = config.healthDisplayEnabled() ? NAME_GAP : 0;
        return maxW + extra + 10;
    }

    /**
     * 计算单个玩家行的内容宽度（使用统一血量区宽度）。
     *
     * @param font              客户端字体
     * @param config            当前 TAB 配置
     * @param pid               玩家 UUID
     * @param cachedHealthAreaW 已缓存的统一血量区宽度
     * @param playerInfoMap     UUID→PlayerInfo 快速查找表
     */
    public static int calcRowWidth(Font font, TabConfig config, UUID pid,
                                   int cachedHealthAreaW,
                                   Map<UUID, PlayerInfo> playerInfoMap) {
        int w = 0;

        if (config.healthDisplayEnabled()) {
            w += cachedHealthAreaW > 0 ? cachedHealthAreaW : 0;
            if (config.betterPingEnabled() || config.onlineDurationEnabled()) {
                w += SECTION_GAP;
            }
        }

        if (config.betterPingEnabled()) {
            w += font.width(getLatencyText(pid, playerInfoMap));
            if (config.onlineDurationEnabled()) w += SECTION_GAP;
        }

        if (config.onlineDurationEnabled()) {
            // betterPing 未开启时，渲染侧会扣掉 PING_ICON_W 给信号图标，
            // 列宽里也需要把这 13px 算进去，否则时长会贴着用户名
            if (!config.betterPingEnabled()) {
                w += PING_ICON_W;
            }
            w += font.width(NeoTabClientState.getOnlineDuration(pid));
        }

        return w;
    }

    /**
     * 当没有玩家数据时的保守宽度估计。
     *
     * @param font   客户端字体
     * @param config 当前 TAB 配置
     */
    public static int calcRowWidthFallback(Font font, TabConfig config) {
        int w = 0;
        if (config.healthDisplayEnabled()) {
            w += HEARTS_W; // 保守估计：正常模式
            if (config.betterPingEnabled() || config.onlineDurationEnabled()) w += SECTION_GAP;
        }
        if (config.betterPingEnabled()) {
            w += font.width("999ms");
            if (config.onlineDurationEnabled()) w += SECTION_GAP;
        }
        if (config.onlineDurationEnabled()) {
            if (!config.betterPingEnabled()) {
                w += PING_ICON_W;
            }
            w += font.width("99d23h");
        }
        return w;
    }

    /**
     * 获取指定玩家的延迟文本（用于宽度计算）。直接查 playerInfoMap，O(1)。
     *
     * @param pid           玩家 UUID
     * @param playerInfoMap UUID→PlayerInfo 快速查找表
     */
    public static String getLatencyText(UUID pid, Map<UUID, PlayerInfo> playerInfoMap) {
        PlayerInfo pi = playerInfoMap.get(pid);
        if (pi != null) {
            return pi.getLatency() + "ms";
        }
        return "999ms";
    }
}
