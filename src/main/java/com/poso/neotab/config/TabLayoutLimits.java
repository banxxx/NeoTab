package com.poso.neotab.config;

import com.poso.neotab.util.MathUtils;
import net.minecraft.client.Minecraft;

/**
 * TAB 布局限制计算器 - 混合方案实现。
 * 
 * <p>结合固定映射表和动态计算，确保在不同GUI缩放和血量显示模式下，
 * TAB列表的列数和行数都在合理范围内。</p>
 * 
 * <p>策略：</p>
 * <ul>
 *   <li>固定映射表：根据GUI缩放和血量显示模式提供安全上限</li>
 *   <li>动态计算：根据实际屏幕尺寸计算理论最大值</li>
 *   <li>最终限制：取两者中的较小值</li>
 * </ul>
 */
public final class TabLayoutLimits {
    
    // ── 固定映射表：安全上限 ──────────────────────────────────────────────────
    
    /**
     * 安全上限映射表。
     * 第一维：GUI缩放 (0=自动, 1-6=对应缩放级别)
     * 第二维：血量显示模式 (0=COMPACT单独, 1=FULL完整)
     * 值：[最大列数, 最大行数]
     */
    private static final int[][][] SAFE_LIMITS = {
        // GUI缩放 = 自动 (索引0)：与缩放6保持一致
        {
            {3, 15},  // COMPACT 单独
            {2, 15}   // FULL 完整
        },
        // GUI缩放 = 1 (索引1)
        {
            {7, 40},  // COMPACT 单独
            {6, 40}   // FULL 完整
        },
        // GUI缩放 = 2 (索引2)
        {
            {6, 40},  // COMPACT 单独
            {5, 40}   // FULL 完整
        },
        // GUI缩放 = 3 (索引3)
        {
            {5, 40},  // COMPACT 单独
            {4, 40}   // FULL 完整
        },
        // GUI缩放 = 4 (索引4)
        {
            {4, 35},  // COMPACT 单独
            {3, 35}   // FULL 完整
        },
        // GUI缩放 = 5 (索引5)
        {
            {3, 30},  // COMPACT 单独
            {2, 30}   // FULL 完整
        },
        // GUI缩放 = 6 (索引6)
        {
            {3, 15},  // COMPACT 单独
            {2, 15}   // FULL 完整
        }
    };
    
    // ── 动态计算常量 ──────────────────────────────────────────────────────────
    
    /** TAB列表左右边距（像素） */
    private static final int HORIZONTAL_MARGIN = 10;
    /** TAB列表上下边距（像素） */
    private static final int VERTICAL_MARGIN = 20;
    /** 翻页箭头占用空间（左右各一个） */
    private static final int ARROW_SPACE = 10;
    /** 标题和页脚占用的垂直空间 */
    private static final int HEADER_FOOTER_SPACE = 40;
    
    /** 玩家名称基础宽度（像素，保守估计） */
    private static final int BASE_NAME_WIDTH = 80;
    /** COMPACT模式血量显示宽度（1颗心 + 间距 + 数字） */
    private static final int COMPACT_HEALTH_WIDTH = 25;
    /** FULL模式血量显示宽度（10颗心 + 间距 + 数字） */
    private static final int FULL_HEALTH_WIDTH = 70;
    /** 延迟显示宽度 */
    private static final int PING_WIDTH = 35;
    /** 在线时长显示宽度 */
    private static final int DURATION_WIDTH = 45;
    /** 列间距和内边距 */
    private static final int COLUMN_PADDING = 15;
    
    /** 单行高度（像素） */
    private static final int ROW_HEIGHT = 9;
    
    /** 绝对最大列数（性能保护） */
    private static final int ABSOLUTE_MAX_COLUMNS = 10;
    /** 绝对最大行数（性能保护） */
    private static final int ABSOLUTE_MAX_ROWS = 50;
    
    private TabLayoutLimits() {
        // 工具类，禁止实例化
    }
    
    // ── 公共API ───────────────────────────────────────────────────────────────
    
    /**
     * 获取当前环境下的最大列数限制。
     * 
     * <p>直接使用固定映射表的值，不进行动态计算。</p>
     * 
     * @param healthMode 血量显示模式
     * @return 最大列数
     */
    public static int getMaxColumns(HealthDisplayMode healthMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return getDefaultMaxColumns(healthMode);
        }
        
        int guiScale = mc.options.guiScale().get();
        int result = getSafeMaxColumns(guiScale, healthMode);
        
        // 调试日志
        com.poso.neotab.NeoTab.LOGGER.debug(
            "getMaxColumns: guiScale={}, healthMode={}, result={}", 
            guiScale, healthMode, result
        );
        
        return result;
    }
    
    /**
     * 获取当前环境下的最大行数限制。
     * 
     * <p>直接使用固定映射表的值，不进行动态计算。</p>
     * 
     * @return 最大行数
     */
    public static int getMaxRows() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return getDefaultMaxRows();
        }
        
        int guiScale = mc.options.guiScale().get();
        int result = getSafeMaxRows(guiScale);
        
        // 调试日志
        com.poso.neotab.NeoTab.LOGGER.debug(
            "getMaxRows: guiScale={}, result={}", 
            guiScale, result
        );
        
        return result;
    }
    
    /**
     * 获取推荐的列数（用于配置界面提示）。
     * 
     * @param healthMode 血量显示模式
     * @return 推荐列数
     */
    public static int getRecommendedColumns(HealthDisplayMode healthMode) {
        int maxColumns = getMaxColumns(healthMode);
        // 推荐值为最大值的60-80%，确保有足够空间
        return Math.max(1, (int) (maxColumns * 0.7));
    }
    
    /**
     * 获取推荐的行数（用于配置界面提示）。
     * 
     * @return 推荐行数
     */
    public static int getRecommendedRows() {
        int maxRows = getMaxRows();
        // 推荐值为最大值的60-80%
        return Math.max(5, (int) (maxRows * 0.7));
    }
    
    // ── 固定映射表查询 ────────────────────────────────────────────────────────
    
    /**
     * 从映射表获取安全最大列数。
     * 
     * @param guiScale GUI缩放级别 (0=自动, 1-6)
     * @param healthMode 血量显示模式
     * @return 安全最大列数
     */
    private static int getSafeMaxColumns(int guiScale, HealthDisplayMode healthMode) {
        int scaleIndex = MathUtils.clamp(guiScale, 0, 6);
        int modeIndex = healthMode == HealthDisplayMode.COMPACT ? 0 : 1;
        return SAFE_LIMITS[scaleIndex][modeIndex][0];
    }
    
    /**
     * 从映射表获取安全最大行数。
     * 
     * @param guiScale GUI缩放级别 (0=自动, 1-6)
     * @return 安全最大行数
     */
    private static int getSafeMaxRows(int guiScale) {
        int scaleIndex = MathUtils.clamp(guiScale, 0, 6);
        // 行数限制与血量显示模式无关，取COMPACT模式的值
        return SAFE_LIMITS[scaleIndex][0][1];
    }
    
    // ── 动态计算 ──────────────────────────────────────────────────────────────
    
    /**
     * 根据屏幕宽度动态计算最大列数。
     * 
     * @param screenWidth 屏幕宽度（GUI缩放后）
     * @param healthMode 血量显示模式
     * @return 动态计算的最大列数
     */
    private static int calculateMaxColumns(int screenWidth, HealthDisplayMode healthMode) {
        // 1. 计算可用宽度
        int availableWidth = screenWidth - HORIZONTAL_MARGIN * 2 - ARROW_SPACE * 2;
        
        // 2. 计算单列宽度
        int healthWidth = healthMode == HealthDisplayMode.COMPACT 
                         ? COMPACT_HEALTH_WIDTH 
                         : FULL_HEALTH_WIDTH;
        int columnWidth = BASE_NAME_WIDTH + healthWidth + PING_WIDTH + DURATION_WIDTH + COLUMN_PADDING;
        
        // 3. 计算最大列数
        int maxColumns = availableWidth / columnWidth;
        
        // 4. 应用绝对上限
        return MathUtils.clamp(maxColumns, 1, ABSOLUTE_MAX_COLUMNS);
    }
    
    /**
     * 根据屏幕高度动态计算最大行数。
     * 
     * @param screenHeight 屏幕高度（GUI缩放后）
     * @return 动态计算的最大行数
     */
    private static int calculateMaxRows(int screenHeight) {
        // 1. 计算可用高度
        int availableHeight = screenHeight - VERTICAL_MARGIN * 2 - HEADER_FOOTER_SPACE;
        
        // 2. 计算最大行数
        int maxRows = availableHeight / ROW_HEIGHT;
        
        // 3. 应用绝对上限
        return MathUtils.clamp(maxRows, 5, ABSOLUTE_MAX_ROWS);
    }
    
    // ── 默认值（当Minecraft实例不可用时） ──────────────────────────────────────
    
    private static int getDefaultMaxColumns(HealthDisplayMode healthMode) {
        // 与缩放6保持一致
        return healthMode == HealthDisplayMode.COMPACT ? 3 : 2;
    }
    
    private static int getDefaultMaxRows() {
        // 与缩放6保持一致
        return 15;
    }
    
    // ── 调试和信息 ────────────────────────────────────────────────────────────
    
    /**
     * 获取当前限制的详细信息（用于调试或配置界面显示）。
     * 
     * @param healthMode 血量显示模式
     * @return 格式化的信息字符串
     */
    public static String getLimitsInfo(HealthDisplayMode healthMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return "Limits unavailable (no window)";
        }
        
        int guiScale = mc.options.guiScale().get();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        int safeMaxCols = getSafeMaxColumns(guiScale, healthMode);
        int dynamicMaxCols = calculateMaxColumns(screenWidth, healthMode);
        int finalMaxCols = Math.min(safeMaxCols, dynamicMaxCols);
        
        int safeMaxRows = getSafeMaxRows(guiScale);
        int dynamicMaxRows = calculateMaxRows(screenHeight);
        int finalMaxRows = Math.min(safeMaxRows, dynamicMaxRows);
        
        return String.format(
            "GUI Scale: %d | Screen: %dx%d | Columns: %d (safe:%d, dynamic:%d) | Rows: %d (safe:%d, dynamic:%d)",
            guiScale, screenWidth, screenHeight,
            finalMaxCols, safeMaxCols, dynamicMaxCols,
            finalMaxRows, safeMaxRows, dynamicMaxRows
        );
    }
}