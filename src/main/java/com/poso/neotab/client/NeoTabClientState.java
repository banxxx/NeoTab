package com.poso.neotab.client;

import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;

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

    /**
     * 当前玩家的有效自定义策略。
     * 由服务端通过 {@code SyncCustomizePolicyPayload} 下发，已经过 resolvePolicy 计算。
     * 默认全部锁定，防止服务端未同步时客户端误判。
     */
    private static PlayerCustomizePolicy currentPolicy = PlayerCustomizePolicy.locked();

    /**
     * 当前玩家的个人配置（原始值，未合并）。
     * 由服务端通过 {@code OpenCustomizeScreenPayload} 下发。
     */
    private static PlayerTabConfig personalConfig = null;
    /** TAB 列表是否被固定常显（Tab+右键触发）。 */
    private static boolean tabPinned = false;

    /** 当前分页页码（0-based）。 */
    private static int currentPage = 0;
    /** 当前渲染帧的总页数（由 mixin 每帧更新）。 */
    private static int totalPages  = 1;
    /** 每页最大玩家数（由 recalculatePages 与渲染切片同源写入）。 */
    private static int playersPerPage = 20;

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

    public static void setOnlineDurations(Map<UUID, String> durations) {
        // P2 优化：复用已有 Map，避免每次收包都 new HashMap
        onlineDurations.clear();
        if (durations != null) {
            onlineDurations.putAll(durations);
        }
    }

    /** 获取指定玩家的在线时长文本。未同步到时返回空串（与 Forge 端一致，不显示占位时长）。 */
    public static String getOnlineDuration(UUID playerId) {
        return onlineDurations.getOrDefault(playerId, "");
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

    /** 获取指定玩家的当前血量（半颗心 = 1.0f）。未收到数据时返回 0.0f（不画假满心）。 */
    public static float getPlayerHealth(UUID playerId) {
        return playerHealths.getOrDefault(playerId, 0.0F);
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
        currentPolicy  = PlayerCustomizePolicy.locked();
        personalConfig = null;
        tabPinned   = false;
        currentPage = 0;
        totalPages  = 1;
        playersPerPage = 20;
        // 重置箭头命中区，避免断线/换服后残留旧 TAB 区域导致误点。
        clearTabBounds();
    }

    public static boolean isTabPinned() { return tabPinned; }
    public static void setTabPinned(boolean pinned) { tabPinned = pinned; }
    /** 切换固定状态，返回切换后的值。 */
    public static boolean toggleTabPinned() { tabPinned = !tabPinned; return tabPinned; }

    // ── 自定义策略 ────────────────────────────────────────────────────────────

    /** 获取当前玩家的有效自定义策略。 */
    public static PlayerCustomizePolicy getCurrentPolicy() { return currentPolicy; }

    /** 更新当前玩家的有效自定义策略（由 SyncCustomizePolicyPayload 调用）。 */
    public static void setCurrentPolicy(PlayerCustomizePolicy policy) {
        currentPolicy = policy != null ? policy : PlayerCustomizePolicy.locked();
    }

    /** 获取当前玩家的个人配置（原始值，未合并）。 */
    public static PlayerTabConfig getPersonalConfig() { return personalConfig; }

    /** 更新当前玩家的个人配置（由 OpenCustomizeScreenPayload 调用）。 */
    public static void setPersonalConfig(PlayerTabConfig config) { personalConfig = config; }

    public static int getCurrentPage()  { return currentPage; }
    public static int getTotalPages()   { return totalPages; }
    public static int getPlayersPerPage() { return playersPerPage; }

    /**
     * 重新计算总页数。
     *
     * <p>唯一写入 playersPerPage/totalPages 的入口，由 TAB 渲染路径（Mixin）每帧调用，
     * perPage 必须与实际切片用的值一致，否则翻页边界与 subList 越界会不匹配。</p>
     *
     * @param totalPlayers 总玩家数
     * @param perPage      每页玩家数（与渲染切片同源）
     */
    public static void recalculatePages(int totalPlayers, int perPage) {
        playersPerPage = Math.max(1, perPage);
        totalPages = Math.max(1, (totalPlayers + playersPerPage - 1) / playersPerPage);
        // 确保当前页码在有效范围内
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
    }

    /** 翻到下一页，成功返回 true。 */
    public static boolean nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            return true;
        }
        return false;
    }

    /** 翻到上一页，成功返回 true。 */
    public static boolean prevPage() {
        if (currentPage > 0) {
            currentPage--;
            return true;
        }
        return false;
    }

    /** 跳转到指定页（0-based），成功返回 true。 */
    public static boolean goToPage(int page) {
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            return true;
        }
        return false;
    }

    public static void setTabBounds(int left, int top, int right, int bottom) {
        tabBoundsLeft   = left;
        tabBoundsTop    = top;
        tabBoundsRight  = right;
        tabBoundsBottom = bottom;
    }

    /** TAB 隐藏/无分页时失效边界，防止陈旧 bounds 被点击检测命中（幽灵翻页）。 */
    public static void clearTabBounds() {
        tabBoundsLeft   = -1;
        tabBoundsTop    = -1;
        tabBoundsRight  = -1;
        tabBoundsBottom = -1;
    }

    public static int getTabBoundsLeft()   { return tabBoundsLeft; }
    public static int getTabBoundsTop()    { return tabBoundsTop; }
    public static int getTabBoundsRight()  { return tabBoundsRight; }
    public static int getTabBoundsBottom() { return tabBoundsBottom; }

    /**
     * 检测鼠标点击是否在翻页箭头区域内，如果是则翻页并返回 true。
     * 命中区与 TabBorderRenderer.drawPageArrows 的绘制区共用同一组常量。
     */
    public static boolean handlePageArrowClick(double mouseX, double mouseY) {
        if (tabBoundsLeft == -1 || totalPages <= 1) return false;

        // 与 TabBorderRenderer.drawPageArrows 共用同一组常量，保证命中区与绘制区对齐
        final int arrowW = com.poso.neotab.client.tab.TabBorderRenderer.PAGE_ARROW_W;
        final int arrowH = com.poso.neotab.client.tab.TabBorderRenderer.PAGE_ARROW_H;
        final int pad    = com.poso.neotab.client.tab.TabBorderRenderer.TAB_CONTENT_PADDING;
        // 命中区在绘制区基础上外扩 2px，方便点击
        final int slack  = 2;
        int centerY = (tabBoundsTop + tabBoundsBottom) / 2;
        int arrowY  = centerY - arrowH / 2;

        // 左箭头（上一页）：绘制于 left + TAB_CONTENT_PADDING
        if (currentPage > 0) {
            int ax = tabBoundsLeft + pad;
            if (mouseX >= ax - slack && mouseX < ax + arrowW + slack
                    && mouseY >= arrowY - slack && mouseY < arrowY + arrowH + slack) {
                prevPage();
                return true;
            }
        }
        // 右箭头（下一页）：绘制于 right - TAB_CONTENT_PADDING - PAGE_ARROW_W
        if (currentPage < totalPages - 1) {
            int ax = tabBoundsRight - pad - arrowW;
            if (mouseX >= ax - slack && mouseX < ax + arrowW + slack
                    && mouseY >= arrowY - slack && mouseY < arrowY + arrowH + slack) {
                nextPage();
                return true;
            }
        }
        return false;
    }
}
