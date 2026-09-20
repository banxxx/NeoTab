package com.poso.neotab.client;

import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NeoTab 客户端状态管理系统。
 * 
 * <p>负责管理客户端的所有状态数据，包括：</p>
 * <ul>
 *     <li>当前有效的TAB配置</li>
 *     <li>玩家自定义权限策略</li>
 *     <li>多页TAB列表的翻页状态</li>
 *     <li>在线时长和血量数据缓存</li>
 *     <li>GUI缩放监听和布局调整</li>
 * </ul>
 */
public final class NeoTabClientState {
    
    // ── 配置和策略 ────────────────────────────────────────────────────────────
    
    /** 当前有效的TAB配置（服务器配置 + 个人配置合并后） */
    private static TabConfig currentConfig = TabConfig.defaults();
    
    /** 当前玩家的自定义权限策略 */
    private static PlayerCustomizePolicy currentPolicy = PlayerCustomizePolicy.locked();
    
    // ── 翻页状态 ──────────────────────────────────────────────────────────────
    
    /** 总页数 */
    private static int totalPages = 1;
    
    /** 当前页码（从0开始） */
    private static int currentPage = 0;
    
    /** 每页最大玩家数 */
    private static int playersPerPage = 20;
    
    /** Tab列表是否被固定显示 */
    private static boolean tabPinned = false;
    
    /** Tab列表边界（用于翻页箭头点击检测） */
    private static int tabBoundsLeft = -1;
    private static int tabBoundsTop = -1;
    private static int tabBoundsRight = -1;
    private static int tabBoundsBottom = -1;
    
    // ── 数据缓存 ──────────────────────────────────────────────────────────────
    
    /** 在线时长数据缓存 */
    private static final Map<UUID, String> onlineDurations = new HashMap<>();
    
    /** 玩家血量数据缓存 */
    private static final Map<UUID, Float> playerHealths = new HashMap<>();
    
    /** 玩家最大血量数据缓存 */
    private static final Map<UUID, Float> playerMaxHealths = new HashMap<>();
    
    // ── GUI状态 ───────────────────────────────────────────────────────────────
    
    /** 上次记录的GUI缩放比例 */
    private static double lastGuiScale = -1.0;
    
    /** 是否需要重新计算布局 */
    private static boolean needsLayoutRecalculation = true;
    
    private NeoTabClientState() {
        // 工具类，禁止实例化
    }
    
    // ── 配置管理 ──────────────────────────────────────────────────────────────
    
    /**
     * 获取当前有效配置。
     * 
     * @return 当前TAB配置
     */
    public static TabConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * 更新当前配置。
     *
     * <p>由网络包处理器调用，更新客户端的有效配置。</p>
     *
     * @param config 新的配置
     */
    public static void updateConfig(TabConfig config) {
        if (config != null) {
            com.poso.neotab.NeoTab.LOGGER.debug("NeoTabClientState.updateConfig: ping={}, duration={}, health={}, mode={}",
                config.betterPingEnabled(),
                config.onlineDurationEnabled(),
                config.healthDisplayEnabled(),
                config.healthDisplayMode());
            currentConfig = config;
            needsLayoutRecalculation = true;
        } else {
            com.poso.neotab.NeoTab.LOGGER.warn("NeoTabClientState.updateConfig: received null config!");
        }
    }
    
    /**
     * 获取当前玩家的自定义策略。
     * 
     * @return 当前策略
     */
    public static PlayerCustomizePolicy getCurrentPolicy() {
        return currentPolicy;
    }
    
    /**
     * 更新当前玩家的自定义策略。
     * 
     * <p>由网络包处理器调用，更新客户端的权限策略。</p>
     * 
     * @param policy 新的策略
     */
    public static void updatePolicy(PlayerCustomizePolicy policy) {
        if (policy != null) {
            currentPolicy = policy;
        }
    }
    
    // ── 翻页管理 ──────────────────────────────────────────────────────────────
    
    /**
     * 获取总页数。
     * 
     * @return 总页数
     */
    public static int getTotalPages() {
        return totalPages;
    }
    
    /**
     * 获取当前页码。
     * 
     * @return 当前页码（从0开始）
     */
    public static int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 获取每页玩家数。
     * 
     * @return 每页最大玩家数
     */
    public static int getPlayersPerPage() {
        return playersPerPage;
    }
    
    /**
     * Tab列表是否被固定显示。
     * 
     * @return 如果Tab列表被固定返回 true
     */
    public static boolean isTabPinned() {
        return tabPinned;
    }
    
    /**
     * 设置Tab列表固定状态。
     * 
     * @param pinned 是否固定
     */
    public static void setTabPinned(boolean pinned) {
        tabPinned = pinned;
    }
    
    /**
     * 切换Tab列表固定状态。
     * 
     * @return 切换后的固定状态
     */
    public static boolean toggleTabPinned() {
        tabPinned = !tabPinned;
        return tabPinned;
    }
    
    /**
     * 翻到下一页。
     * 
     * @return 如果成功翻页返回 true
     */
    public static boolean nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            return true;
        }
        return false;
    }
    
    /**
     * 翻到上一页。
     * 
     * @return 如果成功翻页返回 true
     */
    public static boolean prevPage() {
        if (currentPage > 0) {
            currentPage--;
            return true;
        }
        return false;
    }
    
    /**
     * 跳转到指定页。
     * 
     * @param page 目标页码（从0开始）
     * @return 如果成功跳转返回 true
     */
    public static boolean goToPage(int page) {
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            return true;
        }
        return false;
    }
    
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
    
    /**
     * 设置Tab列表边界。
     * 
     * @param left 左边界
     * @param top 上边界
     * @param right 右边界
     * @param bottom 下边界
     */
    public static void setTabBounds(int left, int top, int right, int bottom) {
        tabBoundsLeft = left;
        tabBoundsTop = top;
        tabBoundsRight = right;
        tabBoundsBottom = bottom;
    }

    /** TAB 隐藏/无分页时失效边界，防止陈旧 bounds 被点击检测命中（幽灵翻页）。 */
    public static void clearTabBounds() {
        tabBoundsLeft = -1;
        tabBoundsTop = -1;
        tabBoundsRight = -1;
        tabBoundsBottom = -1;
    }
    
    /**
     * 获取Tab列表左边界。
     * 
     * @return 左边界，如果未设置返回 -1
     */
    public static int getTabBoundsLeft() {
        return tabBoundsLeft;
    }
    
    /**
     * 获取Tab列表上边界。
     * 
     * @return 上边界，如果未设置返回 -1
     */
    public static int getTabBoundsTop() {
        return tabBoundsTop;
    }
    
    /**
     * 获取Tab列表右边界。
     * 
     * @return 右边界，如果未设置返回 -1
     */
    public static int getTabBoundsRight() {
        return tabBoundsRight;
    }
    
    /**
     * 获取Tab列表下边界。
     * 
     * @return 下边界，如果未设置返回 -1
     */
    public static int getTabBoundsBottom() {
        return tabBoundsBottom;
    }
    
    /**
     * 处理翻页箭头点击。
     * 
     * <p>检测鼠标点击是否在翻页箭头区域内，如果是则翻页。</p>
     * 
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @return 如果点击了箭头并翻页返回 true
     */
    public static boolean handlePageArrowClick(double mouseX, double mouseY) {
        if (tabBoundsLeft == -1 || totalPages <= 1) {
            return false;
        }

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
    
    // ── 数据缓存管理 ──────────────────────────────────────────────────────────
    
    /**
     * 更新在线时长数据。
     * 
     * @param durations 在线时长数据映射
     */
    public static void updateOnlineDurations(Map<UUID, String> durations) {
        onlineDurations.clear();
        if (durations != null) {
            onlineDurations.putAll(durations);
        }
    }
    
    /**
     * 获取指定玩家的在线时长。
     * 
     * @param playerId 玩家UUID
     * @return 在线时长文本，如果没有数据返回空字符串
     */
    public static String getOnlineDuration(UUID playerId) {
        String duration = onlineDurations.get(playerId);
        return duration != null ? duration : "";
    }
    
    /**
     * 更新玩家血量数据。
     * 
     * @param healths 当前血量数据映射
     * @param maxHealths 最大血量数据映射
     */
    public static void updatePlayerHealths(Map<UUID, Float> healths, Map<UUID, Float> maxHealths) {
        playerHealths.clear();
        playerMaxHealths.clear();
        if (healths != null) {
            playerHealths.putAll(healths);
        }
        if (maxHealths != null) {
            playerMaxHealths.putAll(maxHealths);
        }
    }
    
    /**
     * 获取指定玩家的当前血量。
     * 
     * @param playerId 玩家UUID
     * @return 当前血量，如果没有数据返回 0
     */
    public static float getPlayerHealth(UUID playerId) {
        Float health = playerHealths.get(playerId);
        return health != null ? health : 0.0F;
    }
    
    /**
     * 获取指定玩家的最大血量。
     * 
     * @param playerId 玩家UUID
     * @return 最大血量，如果没有数据返回 20
     */
    public static float getPlayerMaxHealth(UUID playerId) {
        Float maxHealth = playerMaxHealths.get(playerId);
        return maxHealth != null ? maxHealth : 20.0F;
    }
    
    // ── GUI状态管理 ───────────────────────────────────────────────────────────
    
    /**
     * 客户端Tick处理。
     * 
     * <p>每个客户端tick调用一次，处理：</p>
     * <ul>
     *     <li>GUI缩放变化监听</li>
     *     <li>布局重新计算</li>
     *     <li>快捷键状态检查</li>
     * </ul>
     * 
     * @param minecraft Minecraft实例
     */
    public static void onClientTick(Minecraft minecraft) {
        // 检查GUI缩放是否变化
        double currentGuiScale = minecraft.getWindow().getGuiScale();
        if (lastGuiScale != currentGuiScale) {
            lastGuiScale = currentGuiScale;
            needsLayoutRecalculation = true;
        }
        
        // 如果需要重新计算布局
        if (needsLayoutRecalculation) {
            recalculateLayout(minecraft);
            needsLayoutRecalculation = false;
        }
    }
    
    /**
     * 重新计算布局。
     *
     * <p>注意：每页玩家数 {@code playersPerPage} 的唯一权威来源是 {@code TabLayoutConfig}，
     * 由 TAB 渲染 Mixin 每帧通过 {@link #recalculatePages(int, int)} 写入。
     * 此处不得再用屏幕高度推算并覆写 playersPerPage，否则会与切片用的 perPage 不一致，
     * 导致翻页边界错位甚至 subList 越界。这里只根据既有 playersPerPage 做一次安全钳制。</p>
     *
     * @param minecraft Minecraft实例
     */
    private static void recalculateLayout(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.level.players() != null) {
            recalculatePages(minecraft.level.players().size(), playersPerPage);
        }
    }
    
    /**
     * 标记需要重新计算布局。
     */
    public static void markLayoutDirty() {
        needsLayoutRecalculation = true;
    }
    
    // ── 状态重置 ──────────────────────────────────────────────────────────────
    
    /**
     * 清理客户端状态。
     * 
     * <p>在玩家登出或切换服务器时调用，清理所有缓存数据。</p>
     */
    public static void cleanup() {
        currentConfig = TabConfig.defaults();
        currentPolicy = PlayerCustomizePolicy.locked();
        totalPages = 1;
        currentPage = 0;
        playersPerPage = 20;
        tabPinned = false;
        tabBoundsLeft = -1;
        tabBoundsTop = -1;
        tabBoundsRight = -1;
        tabBoundsBottom = -1;
        onlineDurations.clear();
        playerHealths.clear();
        playerMaxHealths.clear();
        lastGuiScale = -1.0;
        needsLayoutRecalculation = true;
    }
    
    /**
     * 重置翻页状态。
     * 
     * <p>保留配置和数据缓存，只重置翻页相关状态。</p>
     */
    public static void resetPagination() {
        totalPages = 1;
        currentPage = 0;
        needsLayoutRecalculation = true;
    }
}
