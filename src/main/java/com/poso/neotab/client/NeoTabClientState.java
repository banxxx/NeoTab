package com.poso.neotab.client;

import com.poso.neotab.config.TabConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端侧当前生效的 NeoTab 配置快照。
 *
 * <p>之所以单独保留这份状态，是因为某些客户端渲染行为
 * 不能只靠服务端 header/footer 文本判断，例如：</p>
 * <ul>
 *     <li>是否隐藏原版延迟信号图标</li>
 *     <li>后续是否启用更复杂的客户端视觉效果</li>
 * </ul>
 */
public final class NeoTabClientState {
    private static TabConfig currentConfig = TabConfig.defaults();
    private static Map<UUID, String> onlineDurations = new HashMap<>();

    private NeoTabClientState() {
    }

    public static TabConfig getCurrentConfig() {
        return currentConfig;
    }

    public static void setCurrentConfig(TabConfig config) {
        currentConfig = config == null ? TabConfig.defaults() : config.sanitized();
    }

    public static Map<UUID, String> getOnlineDurations() {
        return onlineDurations;
    }

    public static void setOnlineDurations(Map<UUID, String> durations) {
        onlineDurations = durations == null ? new HashMap<>() : new HashMap<>(durations);
    }

    public static String getOnlineDuration(UUID playerId) {
        return onlineDurations.getOrDefault(playerId, "1h");
    }

    public static void reset() {
        currentConfig = TabConfig.defaults();
        onlineDurations.clear();
    }
}
