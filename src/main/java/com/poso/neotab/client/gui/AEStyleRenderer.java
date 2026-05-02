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

    /** 主面板背景色（莫奈浅蓝灰） */
    public static final int COLOR_PANEL_BG        = 0xFFE8EDF4;
    /** 主面板外层深色轮廓 */
    public static final int COLOR_PANEL_OUTLINE   = 0xFF2C3E50;
    /** 主面板中间灰蓝色边框 */
    public static final int COLOR_PANEL_BORDER    = 0xFF7A8A9E;
    /** 内容区（大凹陷区域）背景色 */
    public static final int COLOR_CONTENT_BG      = 0xFFC8D5E5;
    /** 按钮/Tab非激活背景色 */
    public static final int COLOR_BUTTON_BG       = 0xFFB8C5D6;
    /** 按钮悬浮背景色 */
    public static final int COLOR_BUTTON_HOVER    = 0xFFC8D5E5;
    /** 按钮凸起高光（顶/左） */
    public static final int COLOR_BUTTON_HL       = 0xFFE8EDF4;
    /** 按钮凸起阴影（底/右） */
    public static final int COLOR_BUTTON_SH       = 0xFF7A8A9E;
    /** Tab 栏背景色 */
    public static final int COLOR_TAB_BAR_BG      = 0xFFD4DFEC;
    /** Tab 非激活背景色（与Tab栏同色） */
    public static final int COLOR_TAB_INACTIVE_BG = 0xFFD4DFEC;
    /** Tab 悬浮背景色 */
    public static final int COLOR_TAB_HOVER_BG    = 0xFFE8EDF4;
    /** Tab 激活背景色 */
    public static final int COLOR_TAB_ACTIVE_BG   = 0xFFC8D5E5;
    /** Tab 激活左侧蓝色竖条 */
    public static final int COLOR_TAB_ACTIVE_BAR  = 0xFF6B9BD1;

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
    public static final int COLOR_SECTION_TEXT    = 0xFFF0F4F8;
    /** 模块标题文字色（卡片背景，用深色） */
    public static final int COLOR_MODULE_TITLE    = 0xFF2C3E50;
    /** 模块副标题文字色（卡片背景，用灰色） */
    public static final int COLOR_MODULE_SUBTITLE = 0xFF5A6C7E;
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

    /** 底部按钮栏背景色（与面板背景同色） */
    public static final int COLOR_BUTTON_BAR_BG   = 0xFFE8EDF4;
    /** 底部按钮栏顶部分隔线（2px 灰蓝色） */
    public static final int COLOR_BUTTON_BAR_LINE = 0xFF9AABC0;

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
     * 1px 灰蓝色分隔线 {@code #9AABC0}。
     */
    public static void drawTitleBarDivider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COLOR_PANEL_BORDER);
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
     * 绘制Tab按钮（平面风格，无边框）。
     * <ul>
     *   <li>非激活：背景 {@code #D4DFEC}，文字 {@code #7A8A9E}</li>
     *   <li>悬浮：背景 {@code #E8EDF4}，文字 {@code #2C3E50}</li>
     *   <li>激活：背景 {@code #C8D5E5}，文字 {@code #2C3E50} 加粗，左侧 3px 蓝色竖条 {@code #6B9BD1}</li>
     * </ul>
     *
     * @param active  是否激活
     * @param hovered 是否悬浮
     */
    public static void drawTabButton(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered) {
        // 背景填充（平面，无边框）
        int bg;
        if (active) {
            bg = COLOR_TAB_ACTIVE_BG;
        } else if (hovered) {
            bg = COLOR_TAB_HOVER_BG;
        } else {
            bg = COLOR_TAB_INACTIVE_BG;
        }
        g.fill(x, y, x + w, y + h, bg);

        // 激活Tab：左侧3px蓝色竖条
        if (active) {
            g.fill(x, y, x + 3, y + h, COLOR_TAB_ACTIVE_BAR);
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
     * 绘制滑块式开关按钮（HTML原型样式）。
     * ON状态：绿色滑块在右侧，OFF状态：灰色滑块在左侧。
     */
    public static void drawSliderToggle(GuiGraphics g, int x, int y, int w, int h, boolean isOn, boolean hovered) {
        // 背景凹陷区域
        g.fill(x, y, x + w, y + h, 0xFFA8B5C6);  // 凹陷背景色
        // 凹陷边框（顶/左深色）
        g.fill(x, y, x + w, y + 1, 0xFF7A8A9E);  // 顶
        g.fill(x, y, x + 1, y + h, 0xFF7A8A9E);  // 左
        // 凹陷边框（底/右浅色）
        g.fill(x, y + h - 1, x + w, y + h, 0xFFE8EDF4);  // 底
        g.fill(x + w - 1, y, x + w, y + h, 0xFFE8EDF4);  // 右

        // 滑块（24x14px，距离边缘2px）
        int sliderW = 24;
        int sliderH = 14;
        int sliderX = isOn ? (x + w - sliderW - 2) : (x + 2);
        int sliderY = y + 2;

        if (isOn) {
            // ON状态：深绿色滑块
            int sliderBg = hovered ? 0xFF1E421E : 0xFF1A3A1A;
            g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, sliderBg);
            // 滑块边框（凸起效果）
            g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 1, 0xFF2A5A2A);  // 顶
            g.fill(sliderX, sliderY, sliderX + 1, sliderY + sliderH, 0xFF2A5A2A);  // 左
            g.fill(sliderX, sliderY + sliderH - 1, sliderX + sliderW, sliderY + sliderH, 0xFF0A1A0A);  // 底
            g.fill(sliderX + sliderW - 1, sliderY, sliderX + sliderW, sliderY + sliderH, 0xFF0A1A0A);  // 右

            // "ON"文字（左侧，绿色）
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, "ON", 
                x + 6, y + (h - 8) / 2, COLOR_ON, false);
        } else {
            // OFF状态：灰色滑块
            int sliderBg = hovered ? 0xFF9AABC0 : 0xFF5A6C7E;
            g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, sliderBg);
            // 滑块边框（凸起效果）
            g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 1, 0xFFE8EDF4);  // 顶
            g.fill(sliderX, sliderY, sliderX + 1, sliderY + sliderH, 0xFFE8EDF4);  // 左
            g.fill(sliderX, sliderY + sliderH - 1, sliderX + sliderW, sliderY + sliderH, 0xFF2C3E50);  // 底
            g.fill(sliderX + sliderW - 1, sliderY, sliderX + sliderW, sliderY + sliderH, 0xFF2C3E50);  // 右

            // "OFF"文字（右侧，深色）
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, "OFF", 
                x + w - 22, y + (h - 8) / 2, 0xFF2C3E50, false);
        }
    }

    /**
     * 绘制配置模块卡片背景。
     * 背景色 {@code #B8C5D6}，带 1px 边框 {@code #9AABC0}。
     */
    public static void drawConfigModuleCard(GuiGraphics g, int x, int y, int w, int h) {
        // 1px 边框
        drawOutline(g, x, y, w, h, COLOR_PANEL_BORDER, 1);
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

        // 最外层 1px #3A3A3A
        drawOutline(g, thumbX, thumbY, thumbW, thumbSize, COLOR_OUTLINE, 1);
        // 次层 1px COLOR_SCROLL_THUMB_SH
        drawOutline(g, thumbX + 1, thumbY + 1, thumbW - 2, thumbSize - 2, COLOR_SCROLL_THUMB_SH, 1);
        // 内部 COLOR_SCROLL_THUMB
        g.fill(thumbX + 2, thumbY + 2, thumbX + thumbW - 2, thumbY + thumbSize - 2, COLOR_SCROLL_THUMB);
    }
}
