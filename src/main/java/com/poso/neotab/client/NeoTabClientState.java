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
    private static Map<UUID, Float> playerHealths    = new HashMap<>();
    private static Map<UUID, Float> playerMaxHealths = new HashMap<>();
    /** TAB 列表是否被固定常显（Tab+右键触发）。 */
    private static boolean tabPinned = false;

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

    public static void setPlayerHealths(Map<UUID, Float> healths, Map<UUID, Float> maxHealths) {
        playerHealths    = healths    == null ? new HashMap<>() : new HashMap<>(healths);
        playerMaxHealths = maxHealths == null ? new HashMap<>() : new HashMap<>(maxHealths);
    }

    /** 获取指定玩家的当前血量（半颗心 = 1.0f）。未收到数据时返回 20.0f。 */
    public static float getPlayerHealth(UUID playerId) {
        return playerHealths.getOrDefault(playerId, 20.0f);
    }

    /** 获取指定玩家的最大血量（半颗心 = 1.0f）。未收到数据时返回 20.0f。 */
    public static float getPlayerMaxHealth(UUID playerId) {
        return playerMaxHealths.getOrDefault(playerId, 20.0f);
    }

    public static void reset() {
        currentConfig = TabConfig.defaults();
        onlineDurations.clear();
        playerHealths.clear();
        playerMaxHealths.clear();
        tabPinned = false;
    }

    public static boolean isTabPinned() {
        return tabPinned;
    }

    public static void setTabPinned(boolean pinned) {
        tabPinned = pinned;
    }

    /** 切换固定状态，返回切换后的值。 */
    public static boolean toggleTabPinned() {
        tabPinned = !tabPinned;
        return tabPinned;
    }
}
