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
    private static final int TAB_BAR_WIDTH          = 72;
    private static final int THEME_INDICATOR_SIZE   = 8;
    private static final int INPUT_HEIGHT           = 20;
    private static final int SCROLL_TRACK_W         = 14;
    private static final int TAB_TEXT_ACTIVE        = 0xFFE8ECF0;
    private static final int TAB_TEXT_INACTIVE      = 0xFF3A3A3A;

    private NeoTabConfigScreenRenderer() {}

    // ── Tab 栏 ────────────────────────────────────────────────────────────

    public static void renderTabBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                             NeoTabConfigScreen.ConfigTab activeTab,
                             NeoTabConfigScreenLayout.Layout layout,
                             int mouseX, int mouseY,
                             NeoTabConfigScreen.ScreenMode screenMode) {
        int x    = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
        int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;

        int tabIndex = 0;
        for (NeoTabConfigScreen.ConfigTab tab : NeoTabConfigScreen.ConfigTab.values()) {
            // PERMISSIONS tab is only visible in ADMIN mode
            if (tab == NeoTabConfigScreen.ConfigTab.PERMISSIONS && screenMode != NeoTabConfigScreen.ScreenMode.ADMIN) continue;
            int btnY    = VIEWPORT_TOP + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
            boolean active  = activeTab == tab;
            boolean hovered = !active
                    && mouseX >= x && mouseX <= x + btnW - 1
                    && mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT;

            AEStyleRenderer.drawTabButton(g, x, btnY, btnW, TAB_BUTTON_HEIGHT, active, hovered);

            int textColor = active ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE;
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
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);

        Component msg = cb.getMessage();
        String msgStr = msg.getString();
        int textColor;
        if (msgStr.equals("ON") || msgStr.equals("on")) {
            textColor = AEStyleRenderer.COLOR_ON;
        } else if (msgStr.equals("OFF") || msgStr.equals("off")) {
            textColor = AEStyleRenderer.COLOR_OFF;
        } else {
            textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        }
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
