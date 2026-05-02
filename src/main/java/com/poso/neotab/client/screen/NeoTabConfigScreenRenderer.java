package com.poso.neotab.client.screen;

import com.poso.neotab.client.gui.AEStyleRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Renderer utilities for NeoTabConfigScreen.
 * Contains all pure rendering logic extracted from the main screen class.
 */
public final class NeoTabConfigScreenRenderer {

    // Constants mirrored from NeoTabConfigScreen
    private static final int VIEWPORT_TOP           = 34;
    private static final int TAB_BUTTON_HEIGHT      = 24;
    private static final int TAB_BUTTON_GAP         = 4;
    private static final int TAB_BUTTON_LEFT_PADDING = 6;
    private static final int TAB_BAR_WIDTH          = 84;  // HTML中Tab栏宽度（包含右侧1px分隔线）
    private static final int THEME_INDICATOR_SIZE   = 8;
    private static final int INPUT_HEIGHT           = 20;
    private static final int SCROLL_TRACK_W         = 14;
    private static final int TAB_TEXT_ACTIVE        = 0xFF2C3E50;  // 激活/悬浮：深色
    private static final int TAB_TEXT_INACTIVE      = 0xFF7A8A9E;  // 非激活：灰蓝色

    private NeoTabConfigScreenRenderer() {}

    /**
     * 绘制配置模块卡片（包含标题行、副标题、内容区域）。
     * 标题行：左侧标题文字，右侧开关按钮（如果有）。
     * 
     * @param g GuiGraphics
     * @param font 字体
     * @param x 卡片左边界
     * @param y 卡片顶部
     * @param w 卡片宽度
     * @param title 模块标题
     * @param subtitle 模块副标题（可选）
     * @param toggleX 开关按钮X坐标（如果有）
     * @param toggleY 开关按钮Y坐标（如果有）
     * @param hasToggle 是否有开关按钮
     * @param hasContent 是否有内容区域（如输入框）
     * @param contentHeight 内容区域高度（如果有）
     * @return 卡片总高度
     */
    public static int drawConfigModuleCard(GuiGraphics g, net.minecraft.client.gui.Font font,
                                            int x, int y, int w,
                                            Component title, Component subtitle,
                                            int toggleX, int toggleY, boolean hasToggle,
                                            boolean hasContent, int contentHeight) {
        int padding = 10;  // 卡片内边距
        int titleHeight = font.lineHeight;
        int subtitleHeight = subtitle != null ? (font.lineHeight + 2) : 0;
        int contentGap = hasContent ? 8 : 0;
        
        // 计算卡片高度
        int cardHeight = padding +  // 顶部padding
                        titleHeight +  // 标题行高度
                        subtitleHeight +  // 副标题高度（如果有）
                        contentGap +  // 内容区域间距（如果有）
                        (hasContent ? contentHeight : 0) +  // 内容区域高度
                        padding;  // 底部padding
        
        // 绘制卡片背景
        AEStyleRenderer.drawConfigModuleCard(g, x, y, w, cardHeight);
        
        // 绘制标题（深色，左侧）
        g.drawString(font, title, x + padding, y + padding, 
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
        
        // 绘制副标题（灰色，标题下方）
        if (subtitle != null) {
            g.drawString(font, subtitle, x + padding, y + padding + titleHeight + 2, 
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, false);
        }
        
        // 注意：开关按钮由调用方绘制，这里只返回卡片高度
        return cardHeight;
    }

    // ── Tab 栏 ────────────────────────────────────────────────────────────

    public static void renderTabBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                             NeoTabConfigScreen.ConfigTab activeTab,
                             NeoTabConfigScreenLayout.Layout layout,
                             int panelY,
                             int mouseX, int mouseY,
                             NeoTabConfigScreen.ScreenMode screenMode) {
        // 标题栏高度 + 分隔线
        int titleBarHeight = 25;
        
        // 先绘制Tab栏背景和右侧分隔线（从标题栏分隔线下方开始，延伸到按钮栏顶部）
        // tabBarX 在主面板 2px 灰蓝边框内部，需要向右偏移1px避免覆盖边框
        int tabBarX = layout.tabBarX();
        int tabBarY = panelY + titleBarHeight;
        int tabBarBgX = tabBarX + 1;  // 跳过主面板边框的最后1px
        int tabBarW = TAB_BAR_WIDTH - 1;  // 宽度减1（因为起始位置右移了1px）
        int tabBarH = layout.buttonBarTop() - tabBarY;  // 延伸到按钮栏顶部
        AEStyleRenderer.drawTabBarBackground(g, tabBarBgX, tabBarY, tabBarW, tabBarH);

        // 绘制Tab按钮（左侧留6px padding，右侧留4px padding + 1px分隔线）
        int x    = tabBarBgX + TAB_BUTTON_LEFT_PADDING;
        int btnW = tabBarW - TAB_BUTTON_LEFT_PADDING - 1 - 4;  // 背景宽度 - 左padding - 分隔线 - 右padding

        int tabIndex = 0;
        for (NeoTabConfigScreen.ConfigTab tab : NeoTabConfigScreen.ConfigTab.values()) {
            // PERMISSIONS tab is only visible in ADMIN mode
            if (tab == NeoTabConfigScreen.ConfigTab.PERMISSIONS && screenMode != NeoTabConfigScreen.ScreenMode.ADMIN) continue;
            int btnY    = tabBarY + 6 + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);  // 顶部留6px padding
            boolean active  = activeTab == tab;
            boolean hovered = !active
                    && mouseX >= x && mouseX <= x + btnW - 1
                    && mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT;

            AEStyleRenderer.drawTabButton(g, x, btnY, btnW, TAB_BUTTON_HEIGHT, active, hovered);

            // 文字颜色：激活或悬浮时用深色，非激活用灰蓝色
            int textColor = (active || hovered) ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE;
            Component label = tab.label();
            int textW = font.width(label);
            int textX = x + (btnW - textW) / 2;
            int textY = btnY + (TAB_BUTTON_HEIGHT - font.lineHeight) / 2;
            g.drawString(font, label, textX, textY, textColor, false);
            tabIndex++;
        }
    }

    // ── 主题选择器背景 ────────────────────────────────────────────────────

    public static void renderThemeSelectorBackground(GuiGraphics g, NeoTabConfigScreenLayout.Layout layout) {
        AEStyleRenderer.drawInputBackground(
                g,
                layout.left(),
                layout.toScreenY(layout.themeSelectorY()),
                layout.themeSelectorWidth(),
                layout.themeSelectorHeight()
        );
    }

    // ── 主题选项按钮 ──────────────────────────────────────────────────────

    public static void renderThemeOptionButton(GuiGraphics g, net.minecraft.client.gui.Font font,
                                        Button btn, String themeId, String selectedThemeId,
                                        int mouseX, int mouseY) {
        if (!btn.visible) return;
        boolean hovered  = btn.isMouseOver(mouseX, mouseY);
        boolean selected = themeId.equals(selectedThemeId);
        int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();

        if (selected) {
            AEStyleRenderer.drawSunkenPanel(g, bx, by, bw, bh, AEStyleRenderer.COLOR_BUTTON_HOVER, 1);
        } else {
            AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        }

        int indicatorX = bx + 6;
        int indicatorY = by + (bh - THEME_INDICATOR_SIZE) / 2;
        AEStyleRenderer.drawOutline(g, indicatorX, indicatorY,
                THEME_INDICATOR_SIZE, THEME_INDICATOR_SIZE, AEStyleRenderer.COLOR_OUTLINE, 1);
        g.fill(indicatorX + 1, indicatorY + 1,
               indicatorX + THEME_INDICATOR_SIZE - 1, indicatorY + THEME_INDICATOR_SIZE - 1, 0xFF7A8090);
        if (selected) {
            g.fill(indicatorX + 2, indicatorY + 2,
                   indicatorX + THEME_INDICATOR_SIZE - 2, indicatorY + THEME_INDICATOR_SIZE - 2,
                   AEStyleRenderer.COLOR_ON);
        }

        int textColor = selected
                ? AEStyleRenderer.COLOR_SECTION_TEXT
                : hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawString(font, btn.getMessage(),
                indicatorX + THEME_INDICATOR_SIZE + 8, by + (bh - font.lineHeight) / 2, textColor, false);
    }

    // ── 可选中颜色按钮 ────────────────────────────────────────────────────

    public static void renderSelectableColorButton(GuiGraphics g, net.minecraft.client.gui.Font font,
                                            Button button, boolean selected,
                                            int mouseX, int mouseY) {
        if (!button.visible) return;
        boolean hovered = button.isMouseOver(mouseX, mouseY);
        int bx = button.getX(), by = button.getY(), bw = button.getWidth(), bh = button.getHeight();

        if (selected) {
            AEStyleRenderer.drawSunkenPanel(g, bx, by, bw, bh, AEStyleRenderer.COLOR_BUTTON_HOVER, 1);
        } else {
            AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        }

        int textColor = selected
                ? AEStyleRenderer.COLOR_SECTION_TEXT
                : hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(font, button.getMessage(), bx + bw / 2, by + (bh - font.lineHeight) / 2, textColor);
    }

    // ── AE 风格 CycleButton ───────────────────────────────────────────────

    public static void renderAECycleButton(GuiGraphics g, net.minecraft.client.gui.Font font,
                                    CycleButton<?> cb, int mouseX, int mouseY) {
        if (!cb.visible) return;
        boolean hovered = cb.isMouseOver(mouseX, mouseY);
        int bx = cb.getX(), by = cb.getY(), bw = cb.getWidth(), bh = cb.getHeight();

        Component msg = cb.getMessage();
        String msgStr = msg.getString();
        
        // 检查是否是ON/OFF开关（通过检测按钮宽度，ON/OFF按钮固定宽度56）
        // 或者通过文本内容判断（支持多语言）
        boolean isToggleButton = bw == 56;  // TOGGLE_WIDTH = 56
        
        if (isToggleButton) {
            // 判断是ON还是OFF状态
            // Minecraft的onOffBuilder使用CommonComponents.OPTION_ON/OFF
            // 在不同语言下文本不同，但我们可以通过检查消息键来判断
            boolean isOn = msg.equals(net.minecraft.network.chat.CommonComponents.OPTION_ON) ||
                          msgStr.equalsIgnoreCase("ON") || msgStr.equals("开") || msgStr.contains("✓");
            
            AEStyleRenderer.drawSliderToggle(g, bx, by, bw, bh, isOn, hovered);
            return;
        }

        // 其他类型的CycleButton（如健康显示模式）使用原来的按钮样式
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        int textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(font, msg, bx + bw / 2, by + (bh - font.lineHeight) / 2, textColor);
    }

    // ── AE 风格普通 Button ────────────────────────────────────────────────

    public static void renderAEButton(GuiGraphics g, net.minecraft.client.gui.Font font,
                               Button btn, int mouseX, int mouseY) {
        if (btn == null || !btn.visible) return;
        boolean hovered = btn.isMouseOver(mouseX, mouseY);
        int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        int textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(font, btn.getMessage(), bx + bw / 2, by + (bh - font.lineHeight) / 2, textColor);
    }

    // ── 底部按钮栏 ────────────────────────────────────────────────────────

    public static void renderButtonBar(GuiGraphics g, NeoTabConfigScreenLayout.Layout layout, int screenHeight) {
        int panelX = layout.tabBarX() - 2;
        int panelW = (layout.right() + 8 + SCROLL_TRACK_W + 4) - panelX;
        int panelBottom = screenHeight - 8;
        AEStyleRenderer.drawButtonBar(g, panelX + 1, layout.buttonBarTop(),
                panelW - 2, panelBottom - layout.buttonBarTop());
    }

    // ── 滚动条 ────────────────────────────────────────────────────────────

    public static void renderScrollbar(GuiGraphics g, NeoTabConfigScreenLayout.Layout layout, int scrollOffset) {
        if (layout.maxScroll() <= 0) return;
        AEStyleRenderer.drawScrollbar(g,
                layout.right() + 8, layout.viewportTop(), layout.viewportBottom(),
                SCROLL_TRACK_W, scrollOffset, layout.maxScroll());
    }

    // ── 悬停提示 ──────────────────────────────────────────────────────────

    public static void renderHoveredTooltip(GuiGraphics g, net.minecraft.client.gui.Font font,
                                     NeoTabConfigScreenLayout.HoverTarget ht, int mouseX, int mouseY) {
        if (ht == null) return;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(ht.tooltip(), 200);
        g.renderTooltip(font, lines, mouseX, mouseY);
    }

    // ── 标签行 ────────────────────────────────────────────────────────────

    public static void drawLabel(GuiGraphics g, net.minecraft.client.gui.Font font,
                          Component label, NeoTabConfigScreenLayout.LabelBounds bounds,
                          int mouseX, int mouseY) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        int color  = hovered ? AEStyleRenderer.COLOR_LABEL_HOVER : AEStyleRenderer.COLOR_LABEL;
        int labelY = bounds.y() + (INPUT_HEIGHT - font.lineHeight) / 2 + 1;
        g.drawString(font, label, bounds.x(), labelY, color, false);
    }
}
