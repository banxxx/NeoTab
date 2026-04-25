package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ImprovedRichTextEditBox;
import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.SaveConfigPayload;
import net.minecraft.ChatFormatting;
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
    private static final int TITLE_INPUT_HEIGHT      = 40;
    private static final int MULTILINE_INPUT_HEIGHT  = 40;
    private static final int TOGGLE_WIDTH            = 56;
    private static final int SECTION_HEADER_HEIGHT   = 18;
    private static final int SECTION_GAP             = 16;
    private static final int ROW_GAP                 = 10;
    private static final int FOOTER_COLUMN_GAP       = 12;
    private static final int VIEWPORT_TOP            = 34;
    private static final int VIEWPORT_BOTTOM_MARGIN  = 12;
    private static final int SCROLL_STEP             = 18;
    // Tab 栏常量
    private static final int TAB_BAR_WIDTH           = 72;
    private static final int TAB_CONTENT_GAP          = 8;   // Tab 栏与内容区的间距
    private static final int TAB_BUTTON_HEIGHT        = 24;
    private static final int TAB_BUTTON_GAP           = 4;
    // 颜色常量
    private static final int SECTION_LINE_COLOR   = 0x80FFFFFF;
    private static final int SECTION_TITLE_COLOR  = 0xFFFFFF;
    private static final int LABEL_COLOR          = 0xE0E0E0;
    private static final int LABEL_HOVER_COLOR    = 0xFFFFA0;
    private static final int BUTTON_BAR_COLOR     = 0x90000000;
    private static final int SCROLL_TRACK_COLOR   = 0x60303030;
    private static final int SCROLL_THUMB_COLOR   = 0xB0FFFFFF;
    private static final int TAB_BAR_BG_COLOR     = 0x60000000;
    private static final int TAB_ACTIVE_BG_COLOR  = 0xB0334466;
    private static final int TAB_HOVER_BG_COLOR   = 0x60505070;
    private static final int TAB_DIVIDER_COLOR    = 0x80AAAAAA;
    private static final int TAB_TEXT_ACTIVE      = 0xFFFFFF;
    private static final int TAB_TEXT_INACTIVE    = 0xAAAAAA;

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
        if (hoveredScrollableWidget(mouseX, mouseY) instanceof CycleButton<?>) {
            if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
            setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
        setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = buildLayout();
        // 优先处理 Tab 栏点击
        if (button == 0 && mouseX >= layout.tabBarX() && mouseX <= layout.tabBarX() + TAB_BAR_WIDTH && mouseY >= VIEWPORT_TOP) {
            ConfigTab[] tabs = ConfigTab.values();
            for (int i = 0; i < tabs.length; i++) {
                int btnY = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                    switchTab(tabs[i]);
                    return true;
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
        // 滚动条点击
        if (button == 0 && layout.maxScroll() > 0) {
            int trackX = layout.right() + 6;
            int trackTop = layout.viewportTop();
            int trackBottom = layout.viewportBottom();
            if (mouseX >= trackX && mouseX <= trackX + 4 && mouseY >= trackTop && mouseY <= trackBottom) {
                int trackHeight = trackBottom - trackTop;
                int thumbHeight = Math.max(20, trackHeight * trackHeight / Math.max(trackHeight, layout.contentHeight()));
                int thumbTravel = trackHeight - thumbHeight;
                int thumbY = trackTop + this.scrollOffset * thumbTravel / layout.maxScroll();
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    dragStartY = (int) mouseY;
                    dragStartScrollOffset = this.scrollOffset;
                    return true;
                } else {
                    int newScrollOffset = (int) ((mouseY - trackTop - thumbHeight / 2.0) * layout.maxScroll() / thumbTravel);
                    setScrollOffset(newScrollOffset, layout);
                    return true;
                }
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
            int trackHeight = layout.viewportBottom() - layout.viewportTop();
            int thumbHeight = Math.max(20, trackHeight * trackHeight / Math.max(trackHeight, layout.contentHeight()));
            int thumbTravel = trackHeight - thumbHeight;
            if (thumbTravel > 0) {
                int deltaY = (int) mouseY - dragStartY;
                setScrollOffset(dragStartScrollOffset + (deltaY * layout.maxScroll() / thumbTravel), layout);
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
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        renderTabBar(g, mouseX, mouseY, layout);
        renderScrollableContent(g, mouseX, mouseY, partialTick, layout);
        renderButtonBar(g, layout);
        renderFixedWidgets(g, mouseX, mouseY, partialTick);
        renderHoveredTooltip(g, mouseX, mouseY, layout);
    }

    /** 绘制左侧 Tab 栏。 */
    private void renderTabBar(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        int x = layout.tabBarX();
        int barBottom = layout.viewportBottom();
        // 背景
        g.fill(x, VIEWPORT_TOP, x + TAB_BAR_WIDTH, barBottom, TAB_BAR_BG_COLOR);
        // 右侧分隔线
        g.fill(x + TAB_BAR_WIDTH - 1, VIEWPORT_TOP, x + TAB_BAR_WIDTH, barBottom, TAB_DIVIDER_COLOR);
        ConfigTab[] tabs = ConfigTab.values();
        for (int i = 0; i < tabs.length; i++) {
            ConfigTab tab = tabs[i];
            int btnY = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
            boolean active = activeTab == tab;
            boolean hovered = mouseX >= x && mouseX <= x + TAB_BAR_WIDTH - 1
                           && mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT;
            int bg = active ? TAB_ACTIVE_BG_COLOR : (hovered ? TAB_HOVER_BG_COLOR : 0);
            if (bg != 0) g.fill(x, btnY, x + TAB_BAR_WIDTH - 1, btnY + TAB_BUTTON_HEIGHT, bg);
            // 激活指示条（左侧 2px）
            if (active) g.fill(x, btnY, x + 2, btnY + TAB_BUTTON_HEIGHT, 0xFF88AAFF);
            // 底部分隔线
            if (i < tabs.length - 1)
                g.fill(x + 4, btnY + TAB_BUTTON_HEIGHT, x + TAB_BAR_WIDTH - 5, btnY + TAB_BUTTON_HEIGHT + 1, TAB_DIVIDER_COLOR);
            // 文字
            int textColor = active ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE;
            Component label = tab.label();
            int textW = this.font.width(label);
            int textX = x + (TAB_BAR_WIDTH - 1 - textW) / 2;
            int textY = btnY + (TAB_BUTTON_HEIGHT - this.font.lineHeight) / 2;
            g.drawString(this.font, label, textX, textY, textColor);
        }
    }

    private void renderScrollableContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, Layout layout) {
        g.enableScissor(layout.scissorLeft(), layout.viewportTop(), layout.scissorRight(), layout.viewportBottom());
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            // 渲染原来三个分区的全部内容
            drawSectionHeader(g, layout.left(), layout.right(), layout.toScreenY(layout.topSectionHeaderY()), Component.translatable("screen.neotab.section.top"));
            drawSettingRow(g, Component.translatable("screen.neotab.top.title"), layout.topTitleLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.top.content"), layout.topContentLabelBounds(), mouseX, mouseY);
            drawSectionHeader(g, layout.left(), layout.right(), layout.toScreenY(layout.listSectionHeaderY()), Component.translatable("screen.neotab.section.list"));
            drawSettingRow(g, Component.translatable("screen.neotab.list.better_ping"), layout.betterPingLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.list.online_duration"), layout.onlineDurationLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.list.title"), layout.titleLabelBounds(), mouseX, mouseY);
            drawSectionHeader(g, layout.left(), layout.right(), layout.toScreenY(layout.footerSectionHeaderY()), Component.translatable("screen.neotab.section.footer"));
            drawSettingRow(g, Component.translatable("screen.neotab.footer.custom"), layout.footerCustomLabelBounds(), mouseX, mouseY);
        }
        // THEME tab 暂无内容，留空
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        renderScrollbar(g, layout);
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            r.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void renderFixedWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.doneButton.render(g, mouseX, mouseY, partialTick);
        this.cancelButton.render(g, mouseX, mouseY, partialTick);
    }

    private void renderButtonBar(GuiGraphics g, Layout layout) {
        g.fill(0, layout.buttonBarTop(), this.width, this.height, BUTTON_BAR_COLOR);
        g.fill(0, layout.buttonBarTop(), this.width, layout.buttonBarTop() + 1, SECTION_LINE_COLOR);
    }

    private void renderScrollbar(GuiGraphics g, Layout layout) {
        if (layout.maxScroll() <= 0) return;
        int trackX = layout.right() + 6;
        int trackTop = layout.viewportTop();
        int trackBottom = layout.viewportBottom();
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = Math.max(20, trackHeight * trackHeight / Math.max(trackHeight, layout.contentHeight()));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackTop + this.scrollOffset * thumbTravel / layout.maxScroll();
        g.fill(trackX, trackTop, trackX + 4, trackBottom, SCROLL_TRACK_COLOR);
        g.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, SCROLL_THUMB_COLOR);
    }

    private void renderHoveredTooltip(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        if (!isInsideViewport(mouseX, mouseY, layout)) return;
        HoverTarget ht = hoveredTarget(mouseX, mouseY, layout);
        if (ht != null) g.renderTooltip(this.font, ht.tooltip(), mouseX, mouseY);
    }

    private void drawSectionHeader(GuiGraphics g, int left, int right, int y, Component title) {
        g.drawString(this.font, title.copy().withStyle(ChatFormatting.WHITE), left, y, SECTION_TITLE_COLOR);
        int lineY = y + 10;
        g.fill(left + this.font.width(title) + 8, lineY, right, lineY + 1, SECTION_LINE_COLOR);
    }

    private void drawSettingRow(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(g, label, bounds, mouseX, mouseY);
    }

    private void drawFooterOption(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(g, label, bounds, mouseX, mouseY);
    }

    private void drawLabel(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        int color = bounds.contains(mouseX, mouseY) ? LABEL_HOVER_COLOR : LABEL_COLOR;
        int labelY = bounds.y() + (INPUT_HEIGHT - this.font.lineHeight) / 2 + 1;
        g.drawString(this.font, label, bounds.x(), labelY, color);
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
        return mouseX >= layout.left() && mouseX <= layout.right() + 12
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
        int toggleX = right - TOGGLE_WIDTH;

        int buttonWidth = Math.min(150, (contentWidth - 10) / 2);
        int buttonY = this.height - 6 - INPUT_HEIGHT;
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

        // PAGE_CONFIG：三个分区连续排列，y 坐标累加
        int y = 0;
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

        int footerColumnWidth = (contentWidth - FOOTER_COLUMN_GAP * 2) / 3;
        int footerFirstColumnX  = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX  = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;

        int labelWidth = Math.max(80, contentWidth - TOGGLE_WIDTH - 8);

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
            this.width / 2 - buttonWidth - 5,
            this.width / 2 + 5,
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
