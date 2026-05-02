package com.poso.neotab.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 莫奈印象派风格 GUI 绘制工具类。
 *
 * <p>封装所有像素风格的绘制原语，供配置界面使用。
 * 所有方法均为纯绘制，不持有任何状态。</p>
 *
 * <p>配色体系基于莫奈印象派配色方案，详见 {@link MonetColors}。</p>
 */
public final class MonetStyleRenderer {

    private MonetStyleRenderer() {}

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
        drawOutline(g, x - border, y - border, w + border * 2, h + border * 2, MonetColors.OUTLINE, border);
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
        g.fill(x,         y,         x + w,     y + t,     MonetColors.BORDER_LIGHT); // 顶
        g.fill(x,         y,         x + t,     y + h,     MonetColors.BORDER_LIGHT); // 左
        g.fill(x,         y + h - t, x + w,     y + h,     MonetColors.BORDER_DARK);  // 底
        g.fill(x + w - t, y,         x + w,     y + h,     MonetColors.BORDER_DARK);  // 右
    }

    /**
     * 绘制凹陷边框（不填充背景）。
     */
    public static void drawSunkenBorder(GuiGraphics g, int x, int y, int w, int h, int t) {
        g.fill(x,         y,         x + w,     y + t,     MonetColors.BORDER_DARK);  // 顶
        g.fill(x,         y,         x + t,     y + h,     MonetColors.BORDER_DARK);  // 左
        g.fill(x,         y + h - t, x + w,     y + h,     MonetColors.BORDER_LIGHT); // 底
        g.fill(x + w - t, y,         x + w,     y + h,     MonetColors.BORDER_LIGHT); // 右
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
     * 绘制莫奈风格的主面板背景。
     * 包含：外层轮廓 + 边框 + 背景填充。
     */
    public static void drawMainPanel(GuiGraphics g, int x, int y, int w, int h) {
        // 外层 1px 深色轮廓
        drawOutline(g, x, y, w, h, MonetColors.OUTLINE, 1);
        // 内层 2px 深色边框
        drawOutline(g, x + 1, y + 1, w - 2, h - 2, MonetColors.BORDER_DARK, 2);
        // 面板背景
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, MonetColors.BG_PANEL);
    }

    /**
     * 绘制莫奈风格的内容区（大凹陷区域）。
     */
    public static void drawContentArea(GuiGraphics g, int x, int y, int w, int h) {
        // 内容区直接填充，不需要凹陷效果
        g.fill(x, y, x + w, y + h, MonetColors.BG_CONTENT);
    }

    /**
     * 绘制莫奈风格的按钮（凸起，带外层轮廓）。
     *
     * @param hovered 是否悬浮
     */
    public static void drawButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        // 凸起边框
        drawRaisedBorder(g, x, y, w, h, 1);
        // 背景
        int bg = hovered ? MonetColors.BG_BUTTON_HOVER : MonetColors.BG_BUTTON;
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
    }

    /**
     * 绘制莫奈风格的 Tab 按钮（扁平设计，无 3D 效果）。
     *
     * @param active  是否激活
     * @param hovered 是否悬浮
     */
    public static void drawTabButton(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered) {
        if (active) {
            // 激活：使用内容区背景色
            g.fill(x, y, x + w, y + h, MonetColors.BG_CONTENT);
            // 左侧蓝色指示条
            g.fill(x, y, x + 3, y + h, MonetColors.ACCENT_PRIMARY);
        } else {
            // 非激活：使用侧边栏背景色
            int bg = hovered ? MonetColors.BORDER_LIGHT : MonetColors.BG_SIDEBAR;
            g.fill(x, y, x + w, y + h, bg);
        }
    }

    /**
     * 绘制莫奈风格的输入框背景（凹陷，背景色 #A8B5C6）。
     * 在原版输入框控件渲染之前调用，覆盖其黑色背景。
     */
    public static void drawInputBackground(GuiGraphics g, int x, int y, int w, int h) {
        // 1px 边框
        drawOutline(g, x - 1, y - 1, w + 2, h + 2, MonetColors.TEXT_SECONDARY, 1);
        // 内部填充
        g.fill(x, y, x + w, y + h, MonetColors.BG_INPUT);
    }

    /**
     * 绘制莫奈风格的输入框背景（不带外层边框，直接填充）。
     */
    public static void drawInputFill(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, MonetColors.BG_INPUT);
    }

    /**
     * 绘制莫奈风格的分区标题行（文字加粗 + 右侧分隔线）。
     * 文字不带阴影，分隔线为单像素中灰 + 下方高光。
     */
    public static void drawSectionHeader(GuiGraphics g, net.minecraft.client.gui.Font font,
                                          net.minecraft.network.chat.Component title,
                                          int x, int y, int right) {
        // 文字加粗（用 ChatFormatting.BOLD 包装）
        net.minecraft.network.chat.Component boldTitle = title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        g.drawString(font, boldTitle, x, y, MonetColors.TEXT_LIGHT, false);
        // 文字右侧分隔线（从文字末尾 + 4px 间距开始）
        int lineX = x + font.width(boldTitle) + 4;
        int lineY = y + font.lineHeight / 2;
        if (lineX < right - 2) {
            // 主线：中灰
            g.fill(lineX, lineY,     right, lineY + 1, MonetColors.SECTION_LINE);
            // 高光线：半透明白，在主线下方 1px
            g.fill(lineX, lineY + 1, right, lineY + 2, MonetColors.SECTION_LINE_HL);
        }
    }

    /**
     * 绘制莫奈风格的底部按钮栏背景。
     */
    public static void drawButtonBar(GuiGraphics g, int x, int y, int w, int h) {
        // 顶部分隔线
        g.fill(x, y,     x + w, y + 1, MonetColors.BORDER_MEDIUM);
        g.fill(x, y + 1, x + w, y + 2, MonetColors.SECTION_LINE_HL);
        // 背景
        g.fill(x, y + 2, x + w, y + h, MonetColors.BG_PANEL);
    }

    /**
     * 绘制莫奈风格的滚动条。
     *
     * <p>轨道：窄（6px），1px 白色边框，中间使用内容区背景色。</p>
     * <p>滑块：长方形（宽14px，高略大于宽），比轨道宽，左右各超出 4px，顶底各留 2px 间距。
     * 三层：最外 1px 深色轮廓，次层 1px 中等边框，内部深色边框。</p>
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
        drawOutline(g, trackX, trackTop, trackW, trackH, MonetColors.SCROLL_BORDER, 1);
        g.fill(trackX + 1, trackTop + 1, trackX + trackW - 1, trackBottom - 1, MonetColors.SCROLL_TRACK);

        // ── 滑块：长方形，三层嵌套，顶底各留 2px 间距 ──
        int thumbPad  = 2;
        int thumbSize = thumbW + 6;  // 高度略大于宽度，形成长方形
        int travelH   = trackH - thumbSize - thumbPad * 2;
        int thumbY    = travelH > 0
                        ? trackTop + thumbPad + scrollOffset * travelH / maxScroll
                        : trackTop + thumbPad;
        int thumbX    = trackCenterX;

        // 最外层 1px 深色轮廓
        drawOutline(g, thumbX, thumbY, thumbW, thumbSize, MonetColors.OUTLINE, 1);
        // 次层 1px 中等边框
        drawOutline(g, thumbX + 1, thumbY + 1, thumbW - 2, thumbSize - 2, MonetColors.SCROLL_THUMB_SH, 1);
        // 内部深色边框
        g.fill(thumbX + 2, thumbY + 2, thumbX + thumbW - 2, thumbY + thumbSize - 2, MonetColors.SCROLL_THUMB);
    }

    /**
     * 绘制配置模块（带边框的矩形）。
     */
    public static void drawConfigModule(GuiGraphics g, int x, int y, int w, int h) {
        // 1px 中等边框
        drawOutline(g, x, y, w, h, MonetColors.BORDER_MEDIUM, 1);
        // 模块背景
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, MonetColors.BG_MODULE);
    }

    /**
     * 绘制配置模块标题行（标题 + 右侧按钮区域）。
     */
    public static void drawModuleTitle(GuiGraphics g, net.minecraft.client.gui.Font font,
                                       net.minecraft.network.chat.Component title,
                                       int x, int y) {
        // 绘制标题文字
        g.drawString(font, title, x, y, MonetColors.TEXT_PRIMARY, false);
    }

    /**
     * 绘制配置模块说明文字。
     */
    public static void drawModuleSubtitle(GuiGraphics g, net.minecraft.client.gui.Font font,
                                          net.minecraft.network.chat.Component subtitle,
                                          int x, int y) {
        g.drawString(font, subtitle, x, y, MonetColors.TEXT_SECONDARY, false);
    }
}
