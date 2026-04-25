package com.poso.neotab.client.screen;

import com.poso.neotab.client.gui.AEStyleRenderer;
import com.poso.neotab.client.widget.ImprovedRichTextEditBox;
import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.SaveConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoTab 配置界面，左侧 Tab 栏切换三个分区。 */
public class NeoTabConfigScreen extends Screen {
    // Tab 枚举
    private enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme");
        final String langKey;
        ConfigTab(String langKey) { this.langKey = langKey; }
        Component label() { return Component.translatable(langKey); }
    }

    // 布局常量
    private static final int MAX_CONTENT_WIDTH      = 360;
    private static final int CONTENT_SIDE_PADDING   = 32;
    private static final int ROW_HEIGHT              = 24;
    private static final int INPUT_HEIGHT            = 20;
    private static final int TITLE_INPUT_HEIGHT      = 60;   // 原 40，增加 1/2
    private static final int MULTILINE_INPUT_HEIGHT  = 60;   // 原 40，增加 1/2
    private static final int TOGGLE_WIDTH            = 56;
    private static final int SECTION_HEADER_HEIGHT   = 18;
    private static final int SECTION_GAP             = 16;
    private static final int ROW_GAP                 = 10;
    private static final int FOOTER_COLUMN_GAP       = 12;
    private static final int VIEWPORT_TOP            = 34;
    private static final int VIEWPORT_BOTTOM_MARGIN  = 12;
    private static final int SCROLL_STEP             = 18;
    /** 内容区顶部内边距，让"顶部信息"标题与上边框保持间距。 */
    private static final int CONTENT_TOP_PADDING     = 8;
    // Tab 栏常量
    private static final int TAB_BAR_WIDTH           = 72;
    private static final int TAB_CONTENT_GAP          = 8;   // Tab 栏与内容区的间距
    private static final int TAB_BUTTON_HEIGHT        = 24;
    private static final int TAB_BUTTON_GAP           = 4;
    private static final int TAB_BUTTON_LEFT_PADDING  = 6;   // Tab 按钮与左边距的间距
    // 颜色常量（保留用于兼容，实际渲染已迁移到 AEStyleRenderer）
    private static final int SECTION_LINE_COLOR   = 0x80FFFFFF;
    private static final int SECTION_TITLE_COLOR  = 0xFF2A2A2A;
    private static final int LABEL_COLOR          = 0xFF2A2A2A;
    private static final int LABEL_HOVER_COLOR    = 0xFFFFFF55;
    private static final int BUTTON_BAR_COLOR     = 0x90000000;
    private static final int SCROLL_TRACK_COLOR   = 0x60303030;
    private static final int SCROLL_THUMB_COLOR   = 0xB0FFFFFF;
    private static final int TAB_BAR_BG_COLOR     = 0x60000000;
    private static final int TAB_ACTIVE_BG_COLOR  = 0xB0334466;
    private static final int TAB_HOVER_BG_COLOR   = 0x60505070;
    private static final int TAB_DIVIDER_COLOR    = 0x80AAAAAA;
    // Tab 文字颜色（激活=浅色，非激活=深色）
    private static final int TAB_TEXT_ACTIVE      = 0xFFE8ECF0;  // 激活：浅色
    private static final int TAB_TEXT_INACTIVE    = 0xFF3A3A3A;  // 非激活：深色

    // 状态字段
    private final Screen parent;
    private final TabConfig initialConfig;
    private ConfigTab activeTab = ConfigTab.PAGE_CONFIG;
    private CycleButton<Boolean> topTitleEnabled;
    private ImprovedRichTextMultiLineEditBox topTitleInput;
    private CycleButton<Boolean> topContentEnabled;
    private ImprovedRichTextMultiLineEditBox topContentInput;
    private CycleButton<Boolean> betterPingEnabled;
    private CycleButton<Boolean> onlineDurationEnabled;
    private CycleButton<Boolean> titleEnabled;
    private ImprovedRichTextMultiLineEditBox footerCustomInput;
    private CycleButton<Boolean> footerTpsEnabled;
    private CycleButton<Boolean> footerMsptEnabled;
    private CycleButton<Boolean> footerOnlineEnabled;
    private Button doneButton;
    private Button cancelButton;
    private int scrollOffset;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;

    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
    }

    @Override
    protected void init() {
        clearWidgets();
        Layout layout = buildLayout();
        this.topTitleEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.topTitleEnabled()));
        this.topTitleInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), TITLE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.top.title")));
        this.topTitleInput.setMaxVisibleLength(TabConfig.MAX_TOP_TITLE_LENGTH);
        this.topTitleInput.setValue(initialConfig.topTitleText());
        this.topContentEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.topContentEnabled()));
        this.topContentInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), MULTILINE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.top.content")));
        this.topContentInput.setMaxVisibleLength(TabConfig.MAX_TOP_CONTENT_LENGTH);
        this.topContentInput.setAutoResize(true);
        this.topContentInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.topContentInput.setValue(initialConfig.topContentText());
        this.betterPingEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.betterPingEnabled()));
        this.onlineDurationEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.onlineDurationEnabled()));
        this.titleEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.titleEnabled()));
        this.footerCustomInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), MULTILINE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.footer.custom")));
        this.footerCustomInput.setMaxVisibleLength(TabConfig.MAX_FOOTER_CUSTOM_LENGTH);
        this.footerCustomInput.setAutoResize(true);
        this.footerCustomInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.footerCustomInput.setValue(initialConfig.footerCustomText());
        this.footerTpsEnabled = addRenderableWidget(newLabeledToggle(layout.footerFirstColumnX(), layout.footerColumnWidth(), initialConfig.footerTpsEnabled(), Component.translatable("screen.neotab.footer.tps")));
        this.footerMsptEnabled = addRenderableWidget(newLabeledToggle(layout.footerSecondColumnX(), layout.footerColumnWidth(), initialConfig.footerMsptEnabled(), Component.translatable("screen.neotab.footer.mspt")));
        this.footerOnlineEnabled = addRenderableWidget(newLabeledToggle(layout.footerThirdColumnX(), layout.footerColumnWidth(), initialConfig.footerOnlineEnabled(), Component.translatable("screen.neotab.footer.online")));
        this.doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
            .bounds(layout.doneButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());
        this.cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    /** 根据当前激活 Tab 显示/隐藏对应控件。 */
    private void syncTabWidgetVisibility() {
        boolean page = activeTab == ConfigTab.PAGE_CONFIG;
        // PAGE_CONFIG 显示全部原有控件，THEME 全部隐藏（暂无内容）
        topTitleEnabled.visible       = page;
        topTitleInput.visible         = page;
        topContentEnabled.visible     = page;
        topContentInput.visible       = page;
        betterPingEnabled.visible     = page;
        onlineDurationEnabled.visible = page;
        titleEnabled.visible          = page;
        footerCustomInput.visible     = page;
        footerTpsEnabled.visible      = page;
        footerMsptEnabled.visible     = page;
        footerOnlineEnabled.visible   = page;
    }

    /** 切换到指定 Tab，重置滚动并刷新控件可见性。 */
    private void switchTab(ConfigTab tab) {
        if (activeTab == tab) return;
        activeTab = tab;
        scrollOffset = 0;
        Layout layout = buildLayout();
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void repositionElements() {
        super.repositionElements();
        Layout layout = buildLayout();
        clampScroll(layout);
        applyWidgetLayout(layout);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = buildLayout();
        // CycleButton 悬浮时，由页面处理滚动（防止开关被误切换）
        if (hoveredScrollableWidget(mouseX, mouseY) instanceof CycleButton<?>) {
            if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
            setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
            return true;
        }
        // 输入框悬浮时：只有鼠标在输入框内部滚动条区域才让输入框处理，
        // 其余情况一律由页面处理，保证页面滚动流畅
        AbstractWidget hovered = hoveredScrollableWidget(mouseX, mouseY);
        if (hovered instanceof ImprovedRichTextMultiLineEditBox input) {
            // 输入框内部滚动条区域：getX()+getWidth() 到 getX()+getWidth()+8
            boolean onInputScrollbar = mouseX >= input.getX() + input.getWidth()
                    && mouseX <= input.getX() + input.getWidth() + 8;
            if (onInputScrollbar) {
                // 让输入框自己处理内部滚动
                return input.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            // 不在输入框滚动条上，交给页面滚动
        }
        // 页面滚动
        if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
        setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = buildLayout();
        // 优先处理 Tab 栏点击（与 renderTabBar 中的按钮位置保持一致）
        if (button == 0 && mouseY >= VIEWPORT_TOP) {
            int tabBtnX = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int tabBtnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;
            if (mouseX >= tabBtnX && mouseX <= tabBtnX + tabBtnW) {
                ConfigTab[] tabs = ConfigTab.values();
                for (int i = 0; i < tabs.length; i++) {
                    int btnY = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                    if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                        switchTab(tabs[i]);
                        return true;
                    }
                }
            }
        }
        // 拦截底部按钮栏
        if (mouseY >= layout.buttonBarTop()) {
            if (this.doneButton != null) {
                int bx = this.doneButton.getX(), by = this.doneButton.getY();
                if (mouseX >= bx && mouseX <= bx + this.doneButton.getWidth() && mouseY >= by && mouseY <= by + this.doneButton.getHeight())
                    return this.doneButton.mouseClicked(mouseX, mouseY, button);
            }
            if (this.cancelButton != null) {
                int bx = this.cancelButton.getX(), by = this.cancelButton.getY();
                if (mouseX >= bx && mouseX <= bx + this.cancelButton.getWidth() && mouseY >= by && mouseY <= by + this.cancelButton.getHeight())
                    return this.cancelButton.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;
        // 滚动条点击（坐标与 renderScrollbar 保持一致）
        if (button == 0 && layout.maxScroll() > 0) {
            int thumbX    = layout.right() + 8;
            int trackTop  = layout.viewportTop();
            int trackBottom = layout.viewportBottom();
            int trackH    = trackBottom - trackTop;
            int thumbPad  = 2;
            int thumbW    = SCROLL_TRACK_W;
            int thumbSize = thumbW + 6;  // 长方形滑块高度
            if (mouseX >= thumbX && mouseX <= thumbX + thumbW
                    && mouseY >= trackTop && mouseY <= trackBottom) {
                int travelH    = trackH - thumbSize - thumbPad * 2;
                int thumbY     = travelH > 0
                        ? trackTop + thumbPad + this.scrollOffset * travelH / layout.maxScroll()
                        : trackTop + thumbPad;
                if (mouseY >= thumbY && mouseY <= thumbY + thumbSize) {
                    isDraggingScrollbar    = true;
                    dragStartY             = (int) mouseY;
                    dragStartScrollOffset  = this.scrollOffset;
                } else {
                    int newOffset = travelH > 0
                            ? (int) ((mouseY - trackTop - thumbPad - thumbSize / 2.0) * layout.maxScroll() / travelH)
                            : 0;
                    setScrollOffset(newOffset, layout);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) { isDraggingScrollbar = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            Layout layout = buildLayout();
            int trackH    = layout.viewportBottom() - layout.viewportTop();
            int thumbW    = SCROLL_TRACK_W;
            int thumbSize = thumbW + 6;  // 长方形滑块高度
            int thumbPad  = 2;
            int travelH   = trackH - thumbSize - thumbPad * 2;
            if (travelH > 0) {
                int deltaY = (int) mouseY - dragStartY;
                setScrollOffset(dragStartScrollOffset + deltaY * layout.maxScroll() / travelH, layout);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Layout layout = buildLayout();
        clampScroll(layout);
        applyWidgetLayout(layout);

        this.renderBackground(g, mouseX, mouseY, partialTick);

        // AE2 风格主面板：从标题区顶部开始，宽度与内容区对齐（含Tab栏+间距+内容区+滚动条）
        int scrollTrackW = 14;
        int panelX = layout.tabBarX() - 2;
        int panelY = 8;  // 标题文字上方留少量空间
        int panelW = (layout.right() + 8 + scrollTrackW + 4) - panelX;
        int panelBottomMargin = 8;  // 面板底部与游戏窗口的间距
        int panelH = this.height - panelY - panelBottomMargin;
        AEStyleRenderer.drawMainPanel(g, panelX, panelY, panelW, panelH);

        // 标题文字（在面板内，加粗，不带阴影，清晰显示）
        net.minecraft.network.chat.Component boldTitle = this.title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        int titleTextW = this.font.width(boldTitle);
        int titleTextX = panelX + (panelW - titleTextW) / 2;
        g.drawString(this.font, boldTitle, titleTextX, panelY + 6,
                AEStyleRenderer.COLOR_TITLE_TEXT, false);

        renderTabBar(g, mouseX, mouseY, layout);
        renderScrollableContent(g, mouseX, mouseY, partialTick, layout);
        renderButtonBar(g, layout);
        renderFixedWidgets(g, mouseX, mouseY, partialTick);
        renderHoveredTooltip(g, mouseX, mouseY, layout);
    }

    /** 绘制左侧 Tab 栏（AE2 风格）。 */
    private void renderTabBar(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        int x = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;  // 按钮左边距
        int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;  // 按钮宽度（不超出 Tab 栏右边）

        ConfigTab[] tabs = ConfigTab.values();
        for (int i = 0; i < tabs.length; i++) {
            ConfigTab tab = tabs[i];
            int btnY = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
            boolean active  = activeTab == tab;
            boolean hovered = !active
                    && mouseX >= x && mouseX <= x + btnW - 1
                    && mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT;

            AEStyleRenderer.drawTabButton(g, x, btnY, btnW, TAB_BUTTON_HEIGHT, active, hovered);

            int textColor = active ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE;
            Component label = tab.label();
            int textW = this.font.width(label);
            int textX = x + (btnW - textW) / 2;
            int textY = btnY + (TAB_BUTTON_HEIGHT - this.font.lineHeight) / 2;
            g.drawString(this.font, label, textX, textY, textColor, false);
        }
    }

    private void renderScrollableContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, Layout layout) {
        // AE2 风格：绘制内容区凹陷背景（不包含滚动条区域，滚动条在外部）
        int contentAreaX = layout.left() - 4;
        int contentAreaY = layout.viewportTop() - 2;
        int contentAreaW = layout.right() - layout.left() + 8;
        int contentAreaH = layout.viewportBottom() - layout.viewportTop() + 4;
        AEStyleRenderer.drawContentArea(g, contentAreaX, contentAreaY, contentAreaW, contentAreaH);

        // 开启 scissor，所有后续绘制（包括输入框背景）都受裁剪约束
        g.enableScissor(layout.scissorLeft(), layout.viewportTop(), layout.scissorRight(), layout.viewportBottom());

        if (activeTab == ConfigTab.PAGE_CONFIG) {
            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.top"),
                    layout.left(), layout.toScreenY(layout.topSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.top.title"), layout.topTitleLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.top.content"), layout.topContentLabelBounds(), mouseX, mouseY);

            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.list"),
                    layout.left(), layout.toScreenY(layout.listSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.list.better_ping"), layout.betterPingLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.list.online_duration"), layout.onlineDurationLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.list.title"), layout.titleLabelBounds(), mouseX, mouseY);

            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.footer"),
                    layout.left(), layout.toScreenY(layout.footerSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.footer.custom"), layout.footerCustomLabelBounds(), mouseX, mouseY);
        }
        // 渲染控件（输入框背景在此方法内部、控件渲染之前绘制，受 scissor 裁剪）
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        renderScrollbar(g, layout);
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            if (r instanceof CycleButton<?> cb) {
                renderAECycleButton(g, cb, mouseX, mouseY);
            } else {
                // 输入框背景已在 NoCountMultiLineEditBox.renderBackground() 中处理
                r.render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * 用 AE2 风格绘制 CycleButton。
     * 背景用 AEStyleRenderer，文字颜色根据 ON/OFF 状态区分。
     */
    private void renderAECycleButton(GuiGraphics g, CycleButton<?> cb, int mouseX, int mouseY) {
        if (!cb.visible) return;
        boolean hovered = cb.isMouseOver(mouseX, mouseY);
        int bx = cb.getX(), by = cb.getY(), bw = cb.getWidth(), bh = cb.getHeight();
        // AE2 风格背景
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        // 文字：判断是否为 Boolean ON/OFF
        Component msg = cb.getMessage();
        String msgStr = msg.getString();
        int textColor;
        if (msgStr.equals("ON") || msgStr.equals("on")) {
            textColor = AEStyleRenderer.COLOR_ON;
        } else if (msgStr.equals("OFF") || msgStr.equals("off")) {
            textColor = AEStyleRenderer.COLOR_OFF;
        } else {
            // 带标签的按钮（如 TPS ON）：整体用按钮文字色
            textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        }
        g.drawCenteredString(this.font, msg, bx + bw / 2, by + (bh - this.font.lineHeight) / 2, textColor);
    }

    private void renderFixedWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // AE2 风格绘制 Done/Cancel 按钮（覆盖原版样式）
        renderAEButton(g, this.doneButton, mouseX, mouseY);
        renderAEButton(g, this.cancelButton, mouseX, mouseY);
    }

    /**
     * 用 AE2 风格绘制一个 Button，覆盖原版渲染。
     * 先绘制 AE2 背景，再让原版 Button 渲染文字（文字颜色由原版处理）。
     */
    private void renderAEButton(GuiGraphics g, Button btn, int mouseX, int mouseY) {
        if (btn == null || !btn.visible) return;
        boolean hovered = btn.isMouseOver(mouseX, mouseY);
        int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
        // 绘制 AE2 风格背景（覆盖原版）
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        // 绘制文字（居中，AE2 按钮文字色）
        int textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(this.font, btn.getMessage(), bx + bw / 2, by + (bh - this.font.lineHeight) / 2, textColor);
    }

    private void renderButtonBar(GuiGraphics g, Layout layout) {
        int scrollTrackW = 14;
        int panelX = layout.tabBarX() - 2;
        int panelW = (layout.right() + 8 + scrollTrackW + 4) - panelX;
        int panelBottomMargin = 8;
        int panelBottom = this.height - panelBottomMargin;
        AEStyleRenderer.drawButtonBar(g, panelX + 1, layout.buttonBarTop(),
                panelW - 2, panelBottom - layout.buttonBarTop());
    }

    /** 滚动条轨道宽度（与 mouseClicked 保持一致）。 */
    private static final int SCROLL_TRACK_W = 14;

    private void renderScrollbar(GuiGraphics g, Layout layout) {
        if (layout.maxScroll() <= 0) return;
        // 滚动条紧贴内容区右侧，留 8px 间距
        int trackX = layout.right() + 8;
        AEStyleRenderer.drawScrollbar(g,
                trackX, layout.viewportTop(), layout.viewportBottom(),
                SCROLL_TRACK_W, this.scrollOffset, layout.maxScroll());
    }

    private void renderHoveredTooltip(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        if (!isInsideViewport(mouseX, mouseY, layout)) return;
        HoverTarget ht = hoveredTarget(mouseX, mouseY, layout);
        if (ht != null) g.renderTooltip(this.font, ht.tooltip(), mouseX, mouseY);
    }

    // drawSectionHeader 已迁移到 AEStyleRenderer.drawSectionHeader()

    private void drawSettingRow(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(g, label, bounds, mouseX, mouseY);
    }

    private void drawFooterOption(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(g, label, bounds, mouseX, mouseY);
    }

    private void drawLabel(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        int color = hovered ? AEStyleRenderer.COLOR_LABEL_HOVER : AEStyleRenderer.COLOR_LABEL;
        int labelY = bounds.y() + (INPUT_HEIGHT - this.font.lineHeight) / 2 + 1;
        g.drawString(this.font, label, bounds.x(), labelY, color, false);
    }

    private CycleButton<Boolean> newToggle(int x, boolean initialValue) {
        return CycleButton.onOffBuilder(initialValue)
            .displayOnlyValue()
            .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY);
    }

    /** 带完整标签的开关按钮，文字显示在按钮内部，宽度占满整列。 */
    private CycleButton<Boolean> newLabeledToggle(int x, int width, boolean initialValue, Component label) {
        return CycleButton.onOffBuilder(initialValue)
            .create(x, 0, width, INPUT_HEIGHT, label);
    }

    private HoverTarget hoveredTarget(int mouseX, int mouseY, Layout layout) {
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            if (layout.topTitleLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.top.title.tooltip"));
            if (layout.topContentLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.top.content.tooltip"));
            if (layout.betterPingLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.list.better_ping.tooltip"));
            if (layout.onlineDurationLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.list.online_duration.note"));
            if (layout.titleLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.list.title.tooltip"));
            if (layout.footerCustomLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.footer.custom.tooltip"));
        }
        return null;
    }

    private boolean isInsideViewport(double mouseX, double mouseY, Layout layout) {
        // 右边界扩展到包含滚动条轨道区域（right + 8 起始，宽 SCROLL_TRACK_W）
        return mouseX >= layout.left() && mouseX <= layout.right() + 8 + SCROLL_TRACK_W
            && mouseY >= layout.viewportTop() && mouseY <= layout.viewportBottom();
    }

    private AbstractWidget hoveredScrollableWidget(double mouseX, double mouseY) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            if (r instanceof AbstractWidget w && w.isMouseOver(mouseX, mouseY)) return w;
        }
        return null;
    }

    private void setScrollOffset(int nextOffset, Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(nextOffset, layout.maxScroll()));
        applyWidgetLayout(layout);
    }

    private void clampScroll(Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, layout.maxScroll()));
    }

    private void applyWidgetLayout(Layout layout) {
        placeScrollableWidget(this.topTitleEnabled,      layout.toggleX(),           layout.toScreenY(layout.topTitleRowY()));
        placeScrollableWidget(this.topTitleInput,        layout.left(),              layout.toScreenY(layout.topTitleInputY()));
        placeScrollableWidget(this.topContentEnabled,    layout.toggleX(),           layout.toScreenY(layout.topContentRowY()));
        placeScrollableWidget(this.topContentInput,      layout.left(),              layout.toScreenY(layout.topContentInputY()));
        placeScrollableWidget(this.betterPingEnabled,    layout.toggleX(),           layout.toScreenY(layout.betterPingRowY()));
        placeScrollableWidget(this.onlineDurationEnabled,layout.toggleX(),           layout.toScreenY(layout.onlineDurationRowY()));
        placeScrollableWidget(this.titleEnabled,         layout.toggleX(),           layout.toScreenY(layout.titleRowY()));
        placeScrollableWidget(this.footerCustomInput,    layout.left(),              layout.toScreenY(layout.footerCustomInputY()));
        placeScrollableWidget(this.footerTpsEnabled,     layout.footerFirstColumnX(),  layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerMsptEnabled,    layout.footerSecondColumnX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerOnlineEnabled,  layout.footerThirdColumnX(),  layout.toScreenY(layout.footerRowY()));
        this.doneButton.setX(layout.doneButtonX());
        this.doneButton.setY(layout.buttonY());
        this.cancelButton.setX(layout.cancelButtonX());
        this.cancelButton.setY(layout.buttonY());
    }

    private void placeScrollableWidget(AbstractWidget w, int x, int y) {
        w.setX(x); w.setY(y);
    }

    private void save() {
        TabConfig config = new TabConfig(
            this.topTitleEnabled.getValue(),
            this.topTitleInput.getValue(),
            this.topContentEnabled.getValue(),
            this.topContentInput.getValue(),
            this.betterPingEnabled.getValue(),
            this.onlineDurationEnabled.getValue(),
            this.titleEnabled.getValue(),
            this.footerCustomInput.getValue(),
            this.footerTpsEnabled.getValue(),
            this.footerMsptEnabled.getValue(),
            this.footerOnlineEnabled.getValue(),
            this.initialConfig.refreshIntervalTicks()
        ).sanitized();
        PacketDistributor.sendToServer(new SaveConfigPayload(config));
        onClose();
    }

    /**
     * 重算整张页面的布局。
     * Tab 栏占据左侧 TAB_BAR_WIDTH 像素，内容区在其右侧。
     */
    private Layout buildLayout() {
        // Tab 栏起始 X：屏幕水平居中后，内容区左边再往左 TAB_BAR_WIDTH
        int contentWidth = Math.min(MAX_CONTENT_WIDTH, this.width - CONTENT_SIDE_PADDING - TAB_BAR_WIDTH);
        // 整体块（Tab栏 + 内容区）居中
        int totalWidth = TAB_BAR_WIDTH + TAB_CONTENT_GAP + contentWidth;
        int blockLeft = (this.width - totalWidth) / 2;
        int tabBarX = blockLeft;
        int left = blockLeft + TAB_BAR_WIDTH + TAB_CONTENT_GAP;
        int right = left + contentWidth;
        // toggleX 对齐到输入框实际右边框（输入框宽度减去 NoCountMultiLineEditBox 的 SCROLLBAR_SPACE=6）
        int toggleX = right - 6 - TOGGLE_WIDTH;

        int buttonWidth = Math.min(150, (contentWidth - 10) / 2);
        int panelBottomMargin = 8;
        int buttonY = this.height - panelBottomMargin - 10 - INPUT_HEIGHT;  // 面板底部 - 间距 - 按钮高度
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

        // PAGE_CONFIG：三个分区连续排列，y 坐标累加
        // 顶部留内边距，让"顶部信息"标题与内容区上边框保持间距
        int y = CONTENT_TOP_PADDING;
        int topSectionHeaderY = y;
        y += SECTION_HEADER_HEIGHT;
        int topTitleRowY = y;
        int topTitleInputY = topTitleRowY + ROW_HEIGHT;
        y = topTitleInputY + TITLE_INPUT_HEIGHT + ROW_GAP;
        int topContentRowY = y;
        int topContentInputY = topContentRowY + ROW_HEIGHT;
        y = topContentInputY + MULTILINE_INPUT_HEIGHT + SECTION_GAP;

        int listSectionHeaderY = y;
        y += SECTION_HEADER_HEIGHT;
        int betterPingRowY = y;
        y += ROW_HEIGHT + ROW_GAP;
        int onlineDurationRowY = y;
        y += ROW_HEIGHT + ROW_GAP;
        int titleRowY = y;
        y += ROW_HEIGHT + SECTION_GAP;

        int footerSectionHeaderY = y;
        y += SECTION_HEADER_HEIGHT;
        int footerCustomRowY = y;
        int footerCustomInputY = footerCustomRowY + ROW_HEIGHT;
        y = footerCustomInputY + MULTILINE_INPUT_HEIGHT + ROW_GAP;
        int footerRowY = y;

        // 底部三列宽度：总宽与输入框右边框对齐（contentWidth - 6，与 SCROLLBAR_SPACE 一致）
        int footerTotalWidth = contentWidth - 6;
        int footerColumnWidth = (footerTotalWidth - FOOTER_COLUMN_GAP * 2) / 3;
        int footerFirstColumnX  = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX  = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;

        int labelWidth = Math.max(80, contentWidth - 6 - TOGGLE_WIDTH - 8);

        // 当前 Tab 的内容高度
        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            // 完整内容：三个分区全部展示
            contentHeight = footerRowY + ROW_HEIGHT;
        } else {
            // THEME tab 暂无内容
            contentHeight = 0;
        }
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));

        // 面板中心 X（与 render 方法中的 panelX/panelW 计算保持一致）
        int scrollTrackW = SCROLL_TRACK_W;
        int panelX = tabBarX - 2;
        int panelW = (right + 8 + scrollTrackW + 4) - panelX;
        int panelCenterX = panelX + panelW / 2;

        return new Layout(
            contentWidth, left, right, toggleX, labelWidth,
            this.scrollOffset, viewportTop, viewportBottom,
            buttonBarTop,
            topSectionHeaderY, topTitleRowY, topTitleInputY,
            topContentRowY, topContentInputY,
            listSectionHeaderY, betterPingRowY, onlineDurationRowY, titleRowY,
            footerSectionHeaderY, footerCustomRowY, footerCustomInputY, footerRowY,
            footerFirstColumnX, footerSecondColumnX, footerThirdColumnX,
            footerFirstColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerSecondColumnX + footerColumnWidth - TOGGLE_WIDTH,
            footerThirdColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerColumnWidth,
            contentHeight, maxScroll, buttonWidth, buttonY,
            panelCenterX - buttonWidth - 5,
            panelCenterX + 5,
            tabBarX,
            labelBounds(Component.translatable("screen.neotab.top.title"),        left, viewportTop - this.scrollOffset + topTitleRowY,       labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.top.content"),      left, viewportTop - this.scrollOffset + topContentRowY,     labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.better_ping"), left, viewportTop - this.scrollOffset + betterPingRowY,     labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.online_duration"), left, viewportTop - this.scrollOffset + onlineDurationRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.title"),       left, viewportTop - this.scrollOffset + titleRowY,          labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.custom"),    left, viewportTop - this.scrollOffset + footerCustomRowY,   labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.tps"),    footerFirstColumnX,  viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.mspt"),   footerSecondColumnX, viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.online"), footerThirdColumnX,  viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font)
        );
    }

    private LabelBounds labelBounds(Component text, int x, int y, int maxWidth, Font font) {
        int w = Math.min(font.width(text), maxWidth);
        return new LabelBounds(x, y, Math.max(w, 1), INPUT_HEIGHT);
    }

    private record LabelBounds(int x, int y, int width, int height) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY <= this.y + this.height;
        }
    }

    private record HoverTarget(Component tooltip) {}

    private record Layout(
        int contentWidth, int left, int right, int toggleX, int labelWidth,
        int scrollOffset, int viewportTop, int viewportBottom, int buttonBarTop,
        int topSectionHeaderY, int topTitleRowY, int topTitleInputY,
        int topContentRowY, int topContentInputY,
        int listSectionHeaderY, int betterPingRowY, int onlineDurationRowY, int titleRowY,
        int footerSectionHeaderY, int footerCustomRowY, int footerCustomInputY, int footerRowY,
        int footerFirstColumnX, int footerSecondColumnX, int footerThirdColumnX,
        int footerFirstToggleX, int footerSecondToggleX, int footerThirdToggleX,
        int footerColumnWidth,
        int contentHeight, int maxScroll, int buttonWidth, int buttonY,
        int doneButtonX, int cancelButtonX,
        int tabBarX,
        LabelBounds topTitleLabelBounds, LabelBounds topContentLabelBounds,
        LabelBounds betterPingLabelBounds, LabelBounds onlineDurationLabelBounds,
        LabelBounds titleLabelBounds, LabelBounds footerCustomLabelBounds,
        LabelBounds footerFirstLabelBounds, LabelBounds footerSecondLabelBounds,
        LabelBounds footerThirdLabelBounds
    ) {
        private int toScreenY(int contentY) {
            return this.viewportTop - this.scrollOffset + contentY;
        }
        private int scissorLeft() { return Math.max(0, this.left - 2); }
        private int scissorRight() { return this.right + 10; }
    }
}
