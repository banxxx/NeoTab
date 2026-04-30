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

    /** 当前分页页码（0-based）。 */
    private static int currentPage = 0;
    /** 当前渲染帧的总页数（由 mixin 每帧更新）。 */
    private static int totalPages  = 1;

    /** 上一帧渲染的 TAB 背景边界（含 padding），用于翻页箭头点击检测。 */
    private static int tabBoundsLeft   = -1;
    private static int tabBoundsTop    = -1;
    private static int tabBoundsRight  = -1;
    private static int tabBoundsBottom = -1;

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
        // P2 优化：复用已有 Map，避免每次收包都 new HashMap
        onlineDurations.clear();
        if (durations != null) {
            onlineDurations.putAll(durations);
        }
    }

    public static String getOnlineDuration(UUID playerId) {
        return onlineDurations.getOrDefault(playerId, "1h");
    }

    public static void setPlayerHealths(Map<UUID, Float> healths, Map<UUID, Float> maxHealths) {
        // P2 优化：复用已有 Map，避免每次收包都 new HashMap
        playerHealths.clear();
        if (healths != null) {
            playerHealths.putAll(healths);
        }
        playerMaxHealths.clear();
        if (maxHealths != null) {
            playerMaxHealths.putAll(maxHealths);
        }
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
        tabPinned   = false;
        currentPage = 0;
        totalPages  = 1;
    }

    public static boolean isTabPinned() { return tabPinned; }
    public static void setTabPinned(boolean pinned) { tabPinned = pinned; }
    /** 切换固定状态，返回切换后的值。 */
    public static boolean toggleTabPinned() { tabPinned = !tabPinned; return tabPinned; }

    public static int getCurrentPage()  { return currentPage; }
    public static int getTotalPages()   { return totalPages; }

    public static void setCurrentPage(int page) {
        currentPage = Math.max(0, Math.min(page, totalPages - 1));
    }

    public static void setTotalPages(int pages) {
        totalPages  = Math.max(1, pages);
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
    }

    public static void nextPage() { setCurrentPage(currentPage + 1); }
    public static void prevPage() { setCurrentPage(currentPage - 1); }

    public static void setTabBounds(int left, int top, int right, int bottom) {
        tabBoundsLeft   = left;
        tabBoundsTop    = top;
        tabBoundsRight  = right;
        tabBoundsBottom = bottom;
    }

    public static int getTabBoundsLeft()   { return tabBoundsLeft; }
    public static int getTabBoundsTop()    { return tabBoundsTop; }
    public static int getTabBoundsRight()  { return tabBoundsRight; }
    public static int getTabBoundsBottom() { return tabBoundsBottom; }

    /**
     * 检测鼠标点击是否在翻页箭头区域内，如果是则翻页并返回 true。
     * 箭头区域：左侧 [left, centerY±8]，右侧 [right-10, centerY±8]
     */
    public static boolean handlePageArrowClick(double mouseX, double mouseY) {
        if (tabBoundsLeft == -1 || totalPages <= 1) return false;
        int arrowW = 10;
        int arrowH = 16;
        int centerY = (tabBoundsTop + tabBoundsBottom) / 2;
        int arrowY  = centerY - arrowH / 2;

        // 左箭头
        if (currentPage > 0) {
            int ax = tabBoundsLeft + 3;
            if (mouseX >= ax && mouseX < ax + arrowW && mouseY >= arrowY && mouseY < arrowY + arrowH) {
                prevPage();
                return true;
            }
        }
        // 右箭头
        if (currentPage < totalPages - 1) {
            int ax = tabBoundsRight - 3 - arrowW;
            if (mouseX >= ax && mouseX < ax + arrowW && mouseY >= arrowY && mouseY < arrowY + arrowH) {
                nextPage();
                return true;
            }
        }
        return false;
    }
}
