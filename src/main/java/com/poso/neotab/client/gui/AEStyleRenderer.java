package com.poso.neotab.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * AE2 风格 GUI 绘制工具类。
 *
 * <p>封装所有像素风格的绘制原语，供配置界面使用。
 * 所有方法均为纯绘制，不持有任何状态。</p>
 *
 * <p>配色体系（来自 AE2 1.21.x 截图分析）：</p>
 * <ul>
 *   <li>面板背景：{@code #C0C4CC}</li>
 *   <li>内容区（凹陷）：{@code #9AA0AC}</li>
 *   <li>按钮背景：{@code #8A8E98}</li>
 *   <li>凸起高光边：{@code #C8CCD4}（顶/左）</li>
 *   <li>凸起阴影边：{@code #4A4E58}（底/右）</li>
 *   <li>凹陷高光边：{@code #4A4E58}（顶/左）</li>
 *   <li>凹陷阴影边：{@code #C8CCD4}（底/右）</li>
 *   <li>外层轮廓：{@code #3A3A3A}</li>
 * </ul>
 */
public final class AEStyleRenderer {

    // ── 颜色常量 ──────────────────────────────────────────────────────────────
    // 基于 HTML 原型的温暖米色/棕色主题

    /** 主面板背景色（米白色） */
    public static final int COLOR_PANEL_BG        = 0xFFFAF7EF;  // --bg-panel
    /** 主面板外层深色轮廓 */
    public static final int COLOR_PANEL_OUTLINE   = 0xFFB7AF9B;  // --border-dark
    /** 主面板中间边框 */
    public static final int COLOR_PANEL_BORDER    = 0xFFC8C0AD;  // --border-medium
    /** 卡片浅色边框 */
    public static final int COLOR_BORDER_LIGHT    = 0xFFE3DDCD;  // --border-light
    /** 内容区背景色 */
    public static final int COLOR_CONTENT_BG      = 0xFFFAF7EF;  // --bg-content
    /** 按钮/Tab非激活背景色 */
    public static final int COLOR_BUTTON_BG       = 0xFFF3EFE4;  // --bg-card
    /** 按钮悬浮背景色 */
    public static final int COLOR_BUTTON_HOVER    = 0xFFFAF7EF;  // --bg-card-hover
    /** 按钮凸起高光（顶/左） */
    public static final int COLOR_BUTTON_HL       = 0xFFFFFFFF;  // 白色高光
    /** 按钮凸起阴影（底/右） */
    public static final int COLOR_BUTTON_SH       = 0xFFC8C0AD;  // --border-medium
    /** Tab 栏背景色 */
    public static final int COLOR_TAB_BAR_BG      = 0xFFF0ECDE;  // --bg-sidebar
    /** Tab 非激活背景色 */
    public static final int COLOR_TAB_INACTIVE_BG = 0xFFF0ECDE;  // --bg-sidebar
    /** Tab 悬浮背景色 */
    public static final int COLOR_TAB_HOVER_BG    = 0xFFE3DDCD;  // --border-light
    /** Tab 激活背景色 */
    public static final int COLOR_TAB_ACTIVE_BG   = 0xFFFAF7EF;  // --bg-panel
    /** Tab 激活左侧绿色竖条 */
    public static final int COLOR_TAB_ACTIVE_BAR  = 0xFF8CAE5C;  // --accent-light

    /** 凸起：顶/左高光 */
    public static final int COLOR_RAISED_HL       = 0xFFFFFFFF;  // 白色
    /** 凸起：底/右阴影 */
    public static final int COLOR_RAISED_SH       = 0xFFC8C0AD;  // --border-medium
    /** 凹陷：顶/左阴影 */
    public static final int COLOR_SUNKEN_SH       = 0xFFC8C0AD;  // --border-medium
    /** 凹陷：底/右高光 */
    public static final int COLOR_SUNKEN_HL       = 0xFFFFFFFF;  // 白色
    /** 外层深色轮廓 */
    public static final int COLOR_OUTLINE         = 0xFFB7AF9B;  // --border-dark

    /** 主面板上的标题文字色 */
    public static final int COLOR_TITLE_TEXT      = 0xFF3B3629;  // --text-primary
    /** 分区标题文字色 */
    public static final int COLOR_SECTION_TEXT    = 0xFF3B3629;  // --text-primary
    /** 模块标题文字色 */
    public static final int COLOR_MODULE_TITLE    = 0xFF4A4233;  // 深棕色
    /** 模块副标题文字色 */
    public static final int COLOR_MODULE_SUBTITLE = 0xFF6B6454;  // 中棕色
    /** 普通标签文字色 */
    public static final int COLOR_LABEL           = 0xFF3B3629;  // --text-primary
    /** 悬浮标签文字色 */
    public static final int COLOR_LABEL_HOVER     = 0xFF6B8C42;  // --accent
    /** 按钮文字色 */
    public static final int COLOR_BUTTON_TEXT     = 0xFF3B3629;  // --text-primary
    /** 按钮悬浮文字色 */
    public static final int COLOR_BUTTON_TEXT_HOVER = 0xFF6B8C42;  // --accent

    /** ON 状态颜色（绿色） */
    public static final int COLOR_ON              = 0xFF8CAE5C;  // --accent-light
    /** OFF 状态颜色（灰色） */
    public static final int COLOR_OFF             = 0xFFC2BBAA;  // --toggle-off-bg

    /** 分区分隔线颜色 */
    public static final int COLOR_SECTION_LINE    = 0xFFC8C0AD;  // --border-medium
    /** 分区分隔线高光（线下方 1px） */
    public static final int COLOR_SECTION_LINE_HL = 0x60FFFFFF;  // 半透明白色

    /** 底部按钮栏背景色 */
    public static final int COLOR_BUTTON_BAR_BG   = 0xFFF0ECDE;  // --bg-sidebar
    /** 底部按钮栏顶部分隔线 */
    public static final int COLOR_BUTTON_BAR_LINE = 0xFFD0C9B5;  // 深米色

    /** 滚动条轨道背景 */
    public static final int COLOR_SCROLL_TRACK    = 0xFFF3EFE4;  // --scrollbar-track
    /** 滚动条滑块颜色 */
    public static final int COLOR_SCROLL_THUMB    = 0xFFC8C0AD;  // --scrollbar-thumb
    /** 滚动条轨道边框 */
    public static final int COLOR_SCROLL_BORDER   = 0xFFC8C0AD;  // --border-medium

    /** Tab 栏竖向分隔线 */
    public static final int COLOR_TAB_DIVIDER     = 0xFFC8C0AD;  // --border-medium

    private AEStyleRenderer() {}

    // ── 基础绘制原语 ──────────────────────────────────────────────────────────

    /**
     * 绘制凸起面板（顶/左高光，底/右阴影，带外层轮廓）。
     *
     * @param g      GuiGraphics
     * @param x      左上角 X
     * @param y      左上角 Y
     * @param w      宽度
     * @param h      高度
     * @param bg     背景色
     * @param border 边框厚度（像素）
     */
    public static void drawRaisedPanel(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        // 外层轮廓
        drawOutline(g, x - border, y - border, w + border * 2, h + border * 2, COLOR_OUTLINE, border);
        // 凸起边框
        drawRaisedBorder(g, x, y, w, h, border);
        // 背景填充
        g.fill(x + border, y + border, x + w - border, y + h - border, bg);
    }

    /**
     * 绘制凸起面板（不带外层轮廓，用于内嵌按钮）。
     */
    public static void drawRaisedPanelNoOutline(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        drawRaisedBorder(g, x, y, w, h, border);
        g.fill(x + border, y + border, x + w - border, y + h - border, bg);
    }

    /**
     * 绘制凹陷区域（顶/左阴影，底/右高光）。
     */
    public static void drawSunkenPanel(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        drawSunkenBorder(g, x, y, w, h, border);
        g.fill(x + border, y + border, x + w - border, y + h - border, bg);
    }

    /**
     * 绘制凸起边框（不填充背景）。
     */
    public static void drawRaisedBorder(GuiGraphics g, int x, int y, int w, int h, int t) {
        g.fill(x,         y,         x + w,     y + t,     COLOR_RAISED_HL); // 顶
        g.fill(x,         y,         x + t,     y + h,     COLOR_RAISED_HL); // 左
        g.fill(x,         y + h - t, x + w,     y + h,     COLOR_RAISED_SH); // 底
        g.fill(x + w - t, y,         x + w,     y + h,     COLOR_RAISED_SH); // 右
    }

    /**
     * 绘制凹陷边框（不填充背景）。
     */
    public static void drawSunkenBorder(GuiGraphics g, int x, int y, int w, int h, int t) {
        g.fill(x,         y,         x + w,     y + t,     COLOR_SUNKEN_SH); // 顶
        g.fill(x,         y,         x + t,     y + h,     COLOR_SUNKEN_SH); // 左
        g.fill(x,         y + h - t, x + w,     y + h,     COLOR_SUNKEN_HL); // 底
        g.fill(x + w - t, y,         x + w,     y + h,     COLOR_SUNKEN_HL); // 右
    }

    /**
     * 绘制纯色矩形轮廓（不填充内部）。
     */
    public static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
        g.fill(x,         y,         x + w,     y + t,     color); // 顶
        g.fill(x,         y + h - t, x + w,     y + h,     color); // 底
        g.fill(x,         y + t,     x + t,     y + h - t, color); // 左
        g.fill(x + w - t, y + t,     x + w,     y + h - t, color); // 右
    }

    // ── 复合组件 ──────────────────────────────────────────────────────────────

    /**
     * 绘制主面板背景。
     * 结构（从外到内）：
     * <ol>
     *   <li>最外层 1px 深色轮廓 {@code #2C3E50}</li>
     *   <li>中间 2px 灰蓝色边框 {@code #7A8A9E}</li>
     *   <li>面板背景填充 {@code #E8EDF4}</li>
     * </ol>
     */
    public static void drawMainPanel(GuiGraphics g, int x, int y, int w, int h) {
        // 最外层 1px 深色轮廓
        drawOutline(g, x, y, w, h, COLOR_PANEL_OUTLINE, 1);
        // 中间 2px 灰蓝色边框
        drawOutline(g, x + 1, y + 1, w - 2, h - 2, COLOR_PANEL_BORDER, 2);
        // 面板背景
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, COLOR_PANEL_BG);
    }

    /**
     * 绘制内容区（平面填充，无边框）。
     * 背景色 {@code #C8D5E5}。
     */
    public static void drawContentArea(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COLOR_CONTENT_BG);
    }

    /**
     * 绘制按钮（凸起，带外层轮廓）。
     * 结构：1px 深色轮廓 → 凸起边框（高光 #E8EDF4 / 阴影 #7A8A9E）→ 背景填充。
     *
     * @param hovered 是否悬浮
     */
    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        // 外层 1px 轮廓
        drawOutline(g, x, y, w, h, COLOR_PANEL_OUTLINE, 1);
        // 凸起边框（使用按钮专属高光/阴影）
        g.fill(x + 1,     y + 1,     x + w - 1, y + 2,     COLOR_BUTTON_HL); // 顶高光
        g.fill(x + 1,     y + 1,     x + 2,     y + h - 1, COLOR_BUTTON_HL); // 左高光
        g.fill(x + 1,     y + h - 2, x + w - 1, y + h - 1, COLOR_BUTTON_SH); // 底阴影
        g.fill(x + w - 2, y + 1,     x + w - 1, y + h - 1, COLOR_BUTTON_SH); // 右阴影
        // 背景
        int bg = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, bg);
    }

    /**
     * 绘制标题栏底部分隔线。
     * 1px 边框色分隔线。
     */
    public static void drawTitleBarDivider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COLOR_PANEL_BORDER);
    }

    /**
     * 绘制标题栏渐变背景（HTML原型风格）。
     * 渐变：#e8e1ce → #d9d1b8 → #cfc5aa
     */
    public static void drawTitleBarGradient(GuiGraphics g, int x, int y, int w, int h) {
        // 简化的三色渐变：顶部、中部、底部
        int topColor    = 0xFFE8E1CE;  // 浅米色
        int midColor    = 0xFFD9D1B8;  // 中米色
        int bottomColor = 0xFFCFC5AA;  // 深米色
        
        // 分三段绘制渐变
        int midY = y + h * 40 / 100;  // 40% 位置
        
        // 顶部到中部的渐变
        for (int i = 0; i < midY - y; i++) {
            float ratio = (float) i / (midY - y);
            int color = blendColors(topColor, midColor, ratio);
            g.fill(x, y + i, x + w, y + i + 1, color);
        }
        
        // 中部到底部的渐变
        for (int i = 0; i < y + h - midY; i++) {
            float ratio = (float) i / (y + h - midY);
            int color = blendColors(midColor, bottomColor, ratio);
            g.fill(x, midY + i, x + w, midY + i + 1, color);
        }
    }
    
    /**
     * 混合两个颜色。
     * @param color1 起始颜色（ARGB）
     * @param color2 结束颜色（ARGB）
     * @param ratio 混合比例（0.0 = color1, 1.0 = color2）
     * @return 混合后的颜色（ARGB）
     */
    private static int blendColors(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 绘制Tab栏背景和右侧分隔线。
     * 背景色 {@code #D4DFEC}，右侧 1px 分隔线 {@code #9AABC0}。
     * 传入的宽度 w 包含右侧分隔线，实际背景宽度为 w-1。
     */
    public static void drawTabBarBackground(GuiGraphics g, int x, int y, int w, int h) {
        // 背景填充（不包含右侧分隔线）
        g.fill(x, y, x + w - 1, y + h, COLOR_TAB_BAR_BG);
        // 右侧分隔线（最后1px）
        g.fill(x + w - 1, y, x + w, y + h, COLOR_PANEL_BORDER);
    }

    /**
     * 绘制Tab按钮（HTML原型样式）。
     * <ul>
     *   <li>非激活：透明背景，左侧4px透明边框占位</li>
     *   <li>悬浮：背景 {@code #E3DDCD}，左侧4px {@code #C8C0AD} 边框</li>
     *   <li>激活：水平渐变背景（绿色10%透明度→透明），左侧4px {@code #8CAE5C} 绿色边框，右侧6x6绿色小方块</li>
     * </ul>
     *
     * @param active  是否激活
     * @param hovered 是否悬浮
     */
    public static void drawTabButton(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered) {
        if (active) {
            // 激活状态：水平渐变背景（从左侧绿色10%透明度渐变到右侧透明）
            // rgba(107,140,66,0.1) = 0x1A6B8C42，渐变到透明
            // 用逐列渐变模拟 linear-gradient(90deg, rgba(107,140,66,0.1) 0%, transparent 70%)
            int gradientWidth = w * 70 / 100;  // 渐变区域占70%宽度
            for (int i = 0; i < gradientWidth; i++) {
                float ratio = (float) i / gradientWidth;
                // 从 alpha=0x1A(10%) 渐变到 alpha=0x00(0%)，颜色固定为 #6B8C42
                int alpha = (int) (0x1A * (1.0f - ratio));
                int col = (alpha << 24) | 0x006B8C42;
                g.fill(x + 4 + i, y, x + 4 + i + 1, y + h, col);
            }
            // 左侧4px绿色竖条
            g.fill(x, y, x + 4, y + h, COLOR_TAB_ACTIVE_BAR);
            // 右侧6x6绿色小方块（居中）
            int dotSize = 6;
            int dotX = x + w - dotSize - 8;
            int dotY = y + (h - dotSize) / 2;
            g.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, COLOR_TAB_ACTIVE_BAR);
        } else if (hovered) {
            // 悬浮状态：米色背景 + 左侧4px边框色竖条
            g.fill(x, y, x + w, y + h, COLOR_TAB_HOVER_BG);
            g.fill(x, y, x + 4, y + h, COLOR_PANEL_BORDER);
        } else {
            // 非激活：使用Tab栏背景色（透明效果）
            g.fill(x, y, x + w, y + h, COLOR_TAB_INACTIVE_BG);
        }
    }

    /**
     * 绘制输入框背景（白色背景，带边框）。
     * 在原版输入框控件渲染之前调用，覆盖其黑色背景。
     */
    public static void drawInputBackground(GuiGraphics g, int x, int y, int w, int h) {
        // 外层边框（中等边框色）
        drawOutline(g, x - 1, y - 1, w + 2, h + 2, COLOR_PANEL_BORDER, 1);
        // 内部填充（白色）
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);  // --bg-input: #FFFFFF
    }

    /**
     * 绘制输入框背景（不带外层边框，直接填充白色）。
     */
    public static void drawInputFill(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);  // 白色
    }

    /**
     * 绘制滑块式开关按钮（HTML原型样式，适配Minecraft GUI比例）。
     * 尺寸：32x14px，滑块：10x10px白色方块，距边缘2px。
     * OFF状态：背景 #C2BBAA，边框 #B7AF9B，滑块在左侧。
     * ON状态：背景 #8CAE5C，边框 #6B8C42，滑块在右侧。
     */
    public static void drawSliderToggle(GuiGraphics g, int x, int y, int w, int h, boolean isOn, boolean hovered) {
        // 背景颜色和边框（根据ON/OFF状态）
        int bgColor = isOn ? 0xFF8CAE5C : 0xFFC2BBAA;  // ON: 绿色, OFF: 灰色
        int borderColor = isOn ? 0xFF6B8C42 : 0xFFB7AF9B;  // ON: 深绿, OFF: 深灰
        
        // 绘制外层边框（1px）
        drawOutline(g, x, y, w, h, borderColor, 1);
        
        // 绘制背景填充
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        // 计算滑块位置（10x10px，距离边缘2px，考虑1px边框）
        int knobSize = h - 4;  // 高度减去上下各2px间距
        int knobPadding = 2;   // 距离边缘2px（不含边框）
        int knobX = isOn ? (x + w - 1 - knobPadding - knobSize) : (x + 1 + knobPadding);
        int knobY = y + (h - knobSize) / 2;
        
        // 绘制白色滑块（纯白色方块）
        g.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFFFFFFF);
    }

    /**
     * 绘制主要按钮（HTML原型 .footer-btn.primary 样式）。
     * 绿色背景，白色文字，用于"完成"按钮。
     */
    public static void drawPrimaryButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        // 背景色：悬浮时稍亮
        int bg = hovered ? 0xFF7D9E4D : 0xFF6B8C42;  // --accent / hover
        int border = 0xFF5B7A37;
        // 外层边框
        drawOutline(g, x, y, w, h, border, 1);
        // 顶部高光（1px 半透明白）
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
        // 背景填充
        g.fill(x + 1, y + 2, x + w - 1, y + h - 1, bg);
        // 底部阴影（1px 深色）
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0xFF4B6530);
    }

    /**
     * 绘制次要按钮（HTML原型 .footer-btn.secondary 样式）。
     * 米色背景，深棕文字，用于"取消"按钮。
     */
    public static void drawSecondaryButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? 0xFFD3CBB3 : 0xFFE3DDCD;  // secondary / hover
        int border = 0xFFC8C0AD;
        // 外层边框
        drawOutline(g, x, y, w, h, border, 1);
        // 顶部高光
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        // 背景填充
        g.fill(x + 1, y + 2, x + w - 1, y + h - 1, bg);
        // 底部阴影
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0xFFB7AF9B);
    }

    /**
     * 绘制颜色配置项行（HTML原型 .border-color-item 样式）。
     * 左侧标签文字，右侧颜色预览方块 + hex值。
     * 
     * @param colorPreview 颜色预览值（ARGB），-1 表示不显示预览
     * @param selected 是否被选中
     * @param hovered 是否悬浮
     */
    public static void drawColorItemRow(GuiGraphics g, net.minecraft.client.gui.Font font,
                                         int x, int y, int w, int h,
                                         net.minecraft.network.chat.Component label,
                                         int colorPreview, boolean selected, boolean hovered) {
        // 背景：白色，带边框
        int bg = selected ? 0xFFF4F7E8 : (hovered ? 0xFFFAF7EF : 0xFFFFFFFF);
        int border = selected ? 0xFF8CAE5C : COLOR_PANEL_BORDER;
        drawOutline(g, x, y, w, h, border, 1);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        
        int textColor = selected ? 0xFF3B3629 : (hovered ? 0xFF4A4233 : 0xFF6B6454);
        int innerY = y + (h - font.lineHeight) / 2;
        
        // 左侧：标签文字
        g.drawString(font, label, x + 6, innerY, textColor, false);
        
        // 右侧：颜色预览方块 + hex值
        if (colorPreview != -1) {
            int swatchSize = h - 6;
            String hexText = String.format("#%06X", colorPreview & 0xFFFFFF);
            int hexWidth = font.width(hexText);
            int rightPadding = 6;
            int swatchX = x + w - rightPadding - swatchSize;
            int swatchY = y + (h - swatchSize) / 2;
            int hexX = swatchX - 4 - hexWidth;
            
            // hex值文字
            g.drawString(font, hexText, hexX, innerY, textColor, false);
            
            // 棋盘格背景（用于透明色）
            g.fill(swatchX, swatchY, swatchX + swatchSize / 2, swatchY + swatchSize / 2, 0xFFCCCCCC);
            g.fill(swatchX + swatchSize / 2, swatchY, swatchX + swatchSize, swatchY + swatchSize / 2, 0xFF999999);
            g.fill(swatchX, swatchY + swatchSize / 2, swatchX + swatchSize / 2, swatchY + swatchSize, 0xFF999999);
            g.fill(swatchX + swatchSize / 2, swatchY + swatchSize / 2, swatchX + swatchSize, swatchY + swatchSize, 0xFFCCCCCC);
            // 颜色填充
            g.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, colorPreview);
            // 边框
            drawOutline(g, swatchX, swatchY, swatchSize, swatchSize, COLOR_OUTLINE, 1);
        }
    }

    /**
     * 绘制配置模块卡片背景。
     * 背景色 {@code #F3EFE4}，带 1px 浅色边框 {@code #E3DDCD}。
     */
    public static void drawConfigModuleCard(GuiGraphics g, int x, int y, int w, int h) {
        // 1px 浅色边框（使用 --border-light）
        drawOutline(g, x, y, w, h, COLOR_BORDER_LIGHT, 1);
        // 背景填充
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_BUTTON_BG);
    }

    /**
     * 绘制分区标题行（文字加粗 + 右侧单线分隔线）。
     * 文字白色 {@code #F0F4F8}，分隔线灰蓝色 {@code #9AABC0}。
     */
    public static void drawSectionHeader(GuiGraphics g, net.minecraft.client.gui.Font font,
                                          net.minecraft.network.chat.Component title,
                                          int x, int y, int right) {
        // 文字加粗
        net.minecraft.network.chat.Component boldTitle = title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        g.drawString(font, boldTitle, x, y, COLOR_SECTION_TEXT, false);
        // 文字右侧分隔线（从文字末尾 + 4px 间距开始）
        int lineX = x + font.width(boldTitle) + 4;
        int lineY = y + font.lineHeight / 2;
        if (lineX < right - 2) {
            // 单线：灰蓝色
            g.fill(lineX, lineY, right, lineY + 1, COLOR_PANEL_BORDER);
        }
    }

    /**
     * 绘制底部按钮栏背景。
     * 传入的 x/y/w/h 是按钮栏在主面板最外层轮廓内侧的区域（即 panelX+1, ..., panelW-2）。
     * 方法内部会补回左/右/底三侧被覆盖的主面板边框：
     * <ul>
     *   <li>最外层 1px 深色轮廓（{@code COLOR_PANEL_OUTLINE}）</li>
     *   <li>中间 2px 灰蓝色边框（{@code COLOR_PANEL_BORDER}）</li>
     * </ul>
     * 顶部用 2px 灰蓝色分隔线与内容区分隔。
     */
    public static void drawButtonBar(GuiGraphics g, int x, int y, int w, int h) {
        // 顶部 2px 分隔线（与主面板中间边框同色）
        g.fill(x, y,     x + w, y + 2, COLOR_BUTTON_BAR_LINE);
        // 背景
        g.fill(x, y + 2, x + w, y + h, COLOR_BUTTON_BAR_BG);

        // 补回左/右/底三侧被覆盖的主面板边框
        // 中间 2px 灰蓝色边框（左/右/底）
        g.fill(x,             y, x + 2,         y + h, COLOR_PANEL_BORDER); // 左
        g.fill(x + w - 2,     y, x + w,         y + h, COLOR_PANEL_BORDER); // 右
        g.fill(x,     y + h - 2, x + w,         y + h, COLOR_PANEL_BORDER); // 底
        // 最外层 1px 深色轮廓（左/右/底），在灰蓝边框外侧 —— 坐标偏移 -1
        g.fill(x - 1,         y, x,             y + h + 1, COLOR_PANEL_OUTLINE); // 左
        g.fill(x + w,         y, x + w + 1,     y + h + 1, COLOR_PANEL_OUTLINE); // 右
        g.fill(x - 1, y + h,     x + w + 1,     y + h + 1, COLOR_PANEL_OUTLINE); // 底
    }

    /**
     * 绘制 AE2 风格的滚动条。
    /**
     * 绘制 AE2 风格的滚动条。
     *
     * <p>轨道：窄（6px），1px 白色边框，中间 {@code #9AA0AC}。</p>
     * <p>滑块：长方形（宽14px，高略大于宽），比轨道宽，左右各超出 4px，顶底各留 2px 间距。
     * 三层：最外 1px {@code #3A3A3A}，次层 1px {@code #6A7080}，内部 {@code #4A4E58}。</p>
     */
    public static void drawScrollbar(GuiGraphics g,
                                      int trackCenterX, int trackTop, int trackBottom,
                                      int thumbW,
                                      int scrollOffset, int maxScroll) {
        if (maxScroll <= 0) return;

        int trackH = trackBottom - trackTop;
        int trackW = 6;
        int trackX = trackCenterX + (thumbW - trackW) / 2;

        // ── 轨道：1px 白色边框 + 中间填充 ──
        drawOutline(g, trackX, trackTop, trackW, trackH, COLOR_SCROLL_BORDER, 1);
        g.fill(trackX + 1, trackTop + 1, trackX + trackW - 1, trackBottom - 1, COLOR_SCROLL_TRACK);

        // ── 滑块：长方形，三层嵌套，顶底各留 2px 间距 ──
        int thumbPad  = 2;
        int thumbSize = thumbW + 6;  // 高度略大于宽度，形成长方形
        int travelH   = trackH - thumbSize - thumbPad * 2;
        int thumbY    = travelH > 0
                        ? trackTop + thumbPad + scrollOffset * travelH / maxScroll
                        : trackTop + thumbPad;
        int thumbX    = trackCenterX;

        // 外层 1px 轮廓
        drawOutline(g, thumbX, thumbY, thumbW, thumbSize, COLOR_OUTLINE, 1);
        // 内部填充
        g.fill(thumbX + 1, thumbY + 1, thumbX + thumbW - 1, thumbY + thumbSize - 1, COLOR_SCROLL_THUMB);
    }
}
