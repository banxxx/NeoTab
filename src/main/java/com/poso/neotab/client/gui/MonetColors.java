package com.poso.neotab.client.gui;

/**
 * 莫奈印象派配色方案
 * 
 * <p>灵感来自莫奈的《睡莲》系列画作，采用柔和的蓝灰色调，清新淡雅，视觉舒适。</p>
 * <p>所有颜色值均为 ARGB 格式（0xAARRGGBB）</p>
 * 
 * @see <a href="../../../../../../docs/developer/monet-color-scheme.md">完整配色文档</a>
 */
public final class MonetColors {
    
    // ========== 背景色 ==========
    
    /** 页面背景 - 浅蓝灰 (#B8C5D6) */
    public static final int BG_BODY = 0xFFB8C5D6;
    
    /** 主面板背景 - 极浅蓝 (#E8EDF4) */
    public static final int BG_PANEL = 0xFFE8EDF4;
    
    /** 侧边栏背景 - 淡蓝灰 (#D4DFEC) */
    public static final int BG_SIDEBAR = 0xFFD4DFEC;
    
    /** 内容区背景 - 中蓝灰 (#C8D5E5) */
    public static final int BG_CONTENT = 0xFFC8D5E5;
    
    /** 模块背景 - 蓝灰 (#B8C5D6) */
    public static final int BG_MODULE = 0xFFB8C5D6;
    
    /** 输入框背景 - 深蓝灰 (#A8B5C6) */
    public static final int BG_INPUT = 0xFFA8B5C6;
    
    /** 按钮背景（普通） */
    public static final int BG_BUTTON = 0xFFB8C5D6;
    
    /** 按钮背景（悬停） */
    public static final int BG_BUTTON_HOVER = 0xFFC8D5E5;
    
    /** 按钮背景（按下） */
    public static final int BG_BUTTON_ACTIVE = 0xFFA8B5C6;
    
    // ========== 边框色 ==========
    
    /** 深色边框 - 灰蓝 (#7A8A9E) */
    public static final int BORDER_DARK = 0xFF7A8A9E;
    
    /** 浅色边框 - 极浅蓝 (#E8EDF4) */
    public static final int BORDER_LIGHT = 0xFFE8EDF4;
    
    /** 中等边框 - 中灰蓝 (#9AABC0) */
    public static final int BORDER_MEDIUM = 0xFF9AABC0;
    
    /** 外轮廓 - 深蓝灰 (#2C3E50) */
    public static final int OUTLINE = 0xFF2C3E50;
    
    // ========== 文字色 ==========
    
    /** 主要文字 - 深蓝灰 (#2C3E50) */
    public static final int TEXT_PRIMARY = 0xFF2C3E50;
    
    /** 次要文字 - 中蓝灰 (#5A6C7E) */
    public static final int TEXT_SECONDARY = 0xFF5A6C7E;
    
    /** 浅色文字 - 极浅蓝 (#F0F4F8) */
    public static final int TEXT_LIGHT = 0xFFF0F4F8;
    
    /** 白色文字 */
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    
    /** Tab 激活文字色 */
    public static final int TAB_TEXT_ACTIVE = 0xFF2C3E50;
    
    /** Tab 非激活文字色 */
    public static final int TAB_TEXT_INACTIVE = 0xFF7A8A9E;
    
    // ========== 强调色 ==========
    
    /** 主强调色 - 莫奈蓝 (#6B9BD1) */
    public static final int ACCENT_PRIMARY = 0xFF6B9BD1;
    
    /** 深强调色 - 深莫奈蓝 (#4A7BA8) */
    public static final int ACCENT_DARK = 0xFF4A7BA8;
    
    // ========== 特殊颜色 ==========
    
    /** 开关 ON 状态背景 */
    public static final int TOGGLE_ON_BG = 0xFF1A3A1A;
    
    /** 开关 ON 状态文字 */
    public static final int TOGGLE_ON_TEXT = 0xFF55FF55;
    
    /** 开关 OFF 状态背景 */
    public static final int TOGGLE_OFF_BG = 0xFF5A6C7E;
    
    /** 开关 OFF 状态文字 */
    public static final int TOGGLE_OFF_TEXT = 0xFF2C3E50;
    
    /** 分区分隔线颜色 */
    public static final int SECTION_LINE = 0xFF9AABC0;
    
    /** 分区分隔线高光（线下方 1px） */
    public static final int SECTION_LINE_HL = 0x60FFFFFF;
    
    /** 滚动条轨道背景 */
    public static final int SCROLL_TRACK = 0xFFC8D5E5;
    
    /** 滚动条轨道边框（白色） */
    public static final int SCROLL_BORDER = 0xFFFFFFFF;
    
    /** 滚动条滑块内部 */
    public static final int SCROLL_THUMB = 0xFF7A8A9E;
    
    /** 滚动条滑块次层边框 */
    public static final int SCROLL_THUMB_SH = 0xFF9AABC0;
    
    private MonetColors() {}
}
