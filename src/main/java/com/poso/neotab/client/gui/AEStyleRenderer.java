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

    /** 主面板背景色 */
    public static final int COLOR_PANEL_BG        = 0xFFC0C4CC;
    /** 内容区（大凹陷区域）背景色 */
    public static final int COLOR_CONTENT_BG      = 0xFF9AA0AC;
    /** 按钮/Tab非激活背景色 */
    public static final int COLOR_BUTTON_BG       = 0xFF8A8E98;
    /** 按钮悬浮背景色 */
    public static final int COLOR_BUTTON_HOVER    = 0xFF9AA0AC;
    /** Tab 激活背景色（与面板同色） */
    public static final int COLOR_TAB_ACTIVE_BG   = 0xFFC0C4CC;
    /** Tab 非激活背景色 */
    public static final int COLOR_TAB_INACTIVE_BG = 0xFF8A8E98;

    /** 凸起：顶/左高光 */
    public static final int COLOR_RAISED_HL       = 0xFFC8CCD4;
    /** 凸起：底/右阴影 */
    public static final int COLOR_RAISED_SH       = 0xFF4A4E58;
    /** 凹陷：顶/左阴影 */
    public static final int COLOR_SUNKEN_SH       = 0xFF4A4E58;
    /** 凹陷：底/右高光 */
    public static final int COLOR_SUNKEN_HL       = 0xFFC8CCD4;
    /** 外层深色轮廓 */
    public static final int COLOR_OUTLINE         = 0xFF3A3A3A;

    /** 主面板上的标题文字色（面板背景浅灰，用深色） */
    public static final int COLOR_TITLE_TEXT      = 0xFF2A2A2A;
    /** 分区标题文字色（内容区背景深灰蓝，用白色对比清晰） */
    public static final int COLOR_SECTION_TEXT    = 0xFFFFFFFF;
    /** 普通标签文字色（内容区背景深灰蓝，用白色） */
    public static final int COLOR_LABEL           = 0xFFE8ECF0;
    /** 悬浮标签文字色（MC §e 黄色） */
    public static final int COLOR_LABEL_HOVER     = 0xFFFFFF55;
    /** 按钮文字色（浅色） */
    public static final int COLOR_BUTTON_TEXT     = 0xFFE8ECF0;
    /** 按钮悬浮文字色 */
    public static final int COLOR_BUTTON_TEXT_HOVER = 0xFFFFFF55;

    /** ON 状态颜色（MC §a 绿） */
    public static final int COLOR_ON              = 0xFF55FF55;
    /** OFF 状态颜色（MC §c 红） */
    public static final int COLOR_OFF             = 0xFFFF5555;

    /** 分区分隔线颜色 */
    public static final int COLOR_SECTION_LINE    = 0xFF6A6E78;
    /** 分区分隔线高光（线下方 1px） */
    public static final int COLOR_SECTION_LINE_HL = 0x60FFFFFF;

    /** 底部按钮栏背景色 */
    public static final int COLOR_BUTTON_BAR_BG   = 0xFFC0C4CC;
    /** 底部按钮栏顶部分隔线 */
    public static final int COLOR_BUTTON_BAR_LINE = 0xFF6A6E78;

    /** 滚动条轨道背景（改为 #9AA0AC） */
    public static final int COLOR_SCROLL_TRACK    = 0xFF9AA0AC;
    /** 滚动条轨道边框（白色） */
    public static final int COLOR_SCROLL_BORDER   = 0xFFFFFFFF;
    /** 滚动条滑块内部（原次层色，互换后） */
    public static final int COLOR_SCROLL_THUMB    = 0xFF4A4E58;
    /** 滚动条滑块次层边框（原内部色，互换后） */
    public static final int COLOR_SCROLL_THUMB_SH = 0xFF6A7080;

    /** Tab 栏竖向分隔线 */
    public static final int COLOR_TAB_DIVIDER     = 0xFF6A6E78;

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
     * 绘制 AE2 风格的主面板背景。
     * 包含：外层轮廓 + 凸起边框 + 背景填充。
     */
    public static void drawMainPanel(GuiGraphics g, int x, int y, int w, int h) {
        // 外层 1px 深色轮廓
        drawOutline(g, x, y, w, h, COLOR_OUTLINE, 1);
        // 面板背景
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL_BG);
    }

    /**
     * 绘制 AE2 风格的内容区（大凹陷区域）。
     */
    public static void drawContentArea(GuiGraphics g, int x, int y, int w, int h) {
        drawSunkenPanel(g, x, y, w, h, COLOR_CONTENT_BG, 1);
    }

    /**
     * 绘制 AE2 风格的按钮（凸起，带外层轮廓）。
     *
     * @param hovered 是否悬浮
     */
    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        // 外层 1px 轮廓
        drawOutline(g, x, y, w, h, COLOR_OUTLINE, 1);
        // 凸起边框
        drawRaisedBorder(g, x + 1, y + 1, w - 2, h - 2, 1);
        // 背景
        int bg = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, bg);
    }

    /**
     * 绘制 AE2 风格的 Tab 按钮。
     *
     * @param active  是否激活
     * @param hovered 是否悬浮
     */
    public static void drawTabButton(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered) {
        // 外层 1px 轮廓
        drawOutline(g, x, y, w, h, COLOR_OUTLINE, 1);
        if (active) {
            // 激活：凸起，与面板同色
            drawRaisedBorder(g, x + 1, y + 1, w - 2, h - 2, 1);
            g.fill(x + 2, y + 2, x + w - 2, y + h - 2, COLOR_TAB_ACTIVE_BG);
        } else {
            // 非激活：凸起，深灰色
            drawRaisedBorder(g, x + 1, y + 1, w - 2, h - 2, 1);
            int bg = hovered ? COLOR_BUTTON_HOVER : COLOR_TAB_INACTIVE_BG;
            g.fill(x + 2, y + 2, x + w - 2, y + h - 2, bg);
        }
    }

    /**
     * 绘制 AE2 风格的输入框背景（凹陷，背景色 #7A8090）。
     * 在原版输入框控件渲染之前调用，覆盖其黑色背景。
     */
    public static void drawInputBackground(GuiGraphics g, int x, int y, int w, int h) {
        // 凹陷边框
        drawSunkenBorder(g, x - 1, y - 1, w + 2, h + 2, 1);
        // 内部填充（比内容区略暗的蓝灰）
        g.fill(x, y, x + w, y + h, 0xFF7A8090);
    }

    /**
     * 绘制 AE2 风格的输入框背景（不带外层边框，直接填充）。
     */
    public static void drawInputFill(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF7A8090);
    }

    /**
     * 绘制 AE2 风格的分区标题行（文字加粗 + 右侧分隔线）。
     * 文字不带阴影，分隔线为单像素深灰 + 下方高光。
     */
    public static void drawSectionHeader(GuiGraphics g, net.minecraft.client.gui.Font font,
                                          net.minecraft.network.chat.Component title,
                                          int x, int y, int right) {
        // 文字加粗（用 ChatFormatting.BOLD 包装）
        net.minecraft.network.chat.Component boldTitle = title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        g.drawString(font, boldTitle, x, y, COLOR_SECTION_TEXT, false);
        // 文字右侧分隔线（从文字末尾 + 4px 间距开始）
        int lineX = x + font.width(boldTitle) + 4;
        int lineY = y + font.lineHeight / 2;
        if (lineX < right - 2) {
            // 主线：深灰
            g.fill(lineX, lineY,     right, lineY + 1, COLOR_SECTION_LINE);
            // 高光线：半透明白，在主线下方 1px
            g.fill(lineX, lineY + 1, right, lineY + 2, COLOR_SECTION_LINE_HL);
        }
    }

    /**
     * 绘制 AE2 风格的底部按钮栏背景。
     */
    public static void drawButtonBar(GuiGraphics g, int x, int y, int w, int h) {
        // 顶部分隔线
        g.fill(x, y,     x + w, y + 1, COLOR_BUTTON_BAR_LINE);
        g.fill(x, y + 1, x + w, y + 2, COLOR_SECTION_LINE_HL);
        // 背景
        g.fill(x, y + 2, x + w, y + h, COLOR_BUTTON_BAR_BG);
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

        // 最外层 1px #3A3A3A
        drawOutline(g, thumbX, thumbY, thumbW, thumbSize, COLOR_OUTLINE, 1);
        // 次层 1px COLOR_SCROLL_THUMB_SH
        drawOutline(g, thumbX + 1, thumbY + 1, thumbW - 2, thumbSize - 2, COLOR_SCROLL_THUMB_SH, 1);
        // 内部 COLOR_SCROLL_THUMB
        g.fill(thumbX + 2, thumbY + 2, thumbX + thumbW - 2, thumbY + thumbSize - 2, COLOR_SCROLL_THUMB);
    }
}
