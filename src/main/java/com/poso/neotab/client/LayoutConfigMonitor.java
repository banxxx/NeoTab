package com.poso.neotab.client;

import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.TabLayoutConfig;
import com.poso.neotab.config.TabLayoutLimits;
import net.minecraft.client.Minecraft;

/**
 * 布局配置监听器 - 监听GUI缩放和血量显示模式的变化，自动调整布局配置。
 * 
 * <p>采用事件驱动方式，只在变化时调整，避免每帧都检查造成性能问题。</p>
 */
public final class LayoutConfigMonitor {
    
    /** 上一次检查的GUI缩放值 */
    private static int lastGuiScale = -1;
    /** 上一次检查的血量显示模式 */
    private static HealthDisplayMode lastHealthMode = null;
    
    private LayoutConfigMonitor() {
        // 工具类，禁止实例化
    }
    
    /**
     * 检查GUI缩放和血量模式是否变化，如果变化则自动调整布局配置。
     * 
     * <p>此方法应该在合适的时机调用，例如：</p>
     * <ul>
     *   <li>客户端tick事件（每秒20次，性能开销可接受）</li>
     *   <li>配置变化事件</li>
     *   <li>TAB列表渲染前（但有缓存机制，不会每帧都调整）</li>
     * </ul>
     */
    public static void checkAndAdjust() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        
        // 获取当前值
        int currentGuiScale = mc.options.guiScale().get();
        HealthDisplayMode currentHealthMode = NeoTabClientState.getCurrentConfig().healthDisplayMode();
        
        // 检查是否变化
        boolean guiScaleChanged = (lastGuiScale != -1 && lastGuiScale != currentGuiScale);
        boolean healthModeChanged = (lastHealthMode != null && lastHealthMode != currentHealthMode);
        
        // 如果有变化，调整配置
        if (guiScaleChanged || healthModeChanged) {
            adjustLayoutConfig(currentHealthMode);
        }
        
        // 更新缓存值
        lastGuiScale = currentGuiScale;
        lastHealthMode = currentHealthMode;
    }
    
    /**
     * 强制调整布局配置到当前限制范围内。
     * 
     * <p>用于初始化或手动触发调整。</p>
     */
    public static void forceAdjust() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        
        HealthDisplayMode currentHealthMode = NeoTabClientState.getCurrentConfig().healthDisplayMode();
        adjustLayoutConfig(currentHealthMode);
        
        // 更新缓存值
        lastGuiScale = mc.options.guiScale().get();
        lastHealthMode = currentHealthMode;
    }
    
    /**
     * 调整布局配置到当前限制范围内。
     * 
     * @param healthMode 当前血量显示模式
     */
    private static void adjustLayoutConfig(HealthDisplayMode healthMode) {
        TabLayoutConfig config = TabLayoutConfig.get();
        
        // 获取当前限制
        int maxColumns = TabLayoutLimits.getMaxColumns(healthMode);
        int maxRows = TabLayoutLimits.getMaxRows();
        
        // 获取当前配置
        int currentColumns = config.getColumns();
        int currentRows = config.getRowsPerColumn();
        
        // 检查是否需要调整
        boolean needsAdjust = false;
        
        if (currentColumns > maxColumns) {
            config.setColumns(maxColumns);
            needsAdjust = true;
        }
        
        if (currentRows > maxRows) {
            config.setRowsPerColumn(maxRows);
            needsAdjust = true;
        }
        
        // 如果有调整，保存配置
        if (needsAdjust) {
            TabLayoutConfig.save(config);
            com.poso.neotab.NeoTab.LOGGER.info(
                "Layout config adjusted: columns={}, rows={} (max: {}x{})",
                config.getColumns(), config.getRowsPerColumn(), maxColumns, maxRows
            );
        }
    }
    
    /**
     * 重置监听器状态。
     * 
     * <p>用于客户端断开连接或重新加载时。</p>
     */
    public static void reset() {
        lastGuiScale = -1;
        lastHealthMode = null;
    }
}
