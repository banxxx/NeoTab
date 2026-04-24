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

/**
 * NeoTab 配置界面。
 *
 * <p>这一版重点做了两件事：</p>
 * <ul>
 *     <li>把“内容信息”改成普通单行输入框，解决多行输入框中间光标不明显的问题。</li>
 *     <li>在底部信息模块新增“自定义信息”输入框，并继续保留滚动布局以兼容不同 GUI Scale。</li>
 * </ul>
 */
public class NeoTabConfigScreen extends Screen {
    private static final int MAX_CONTENT_WIDTH = 360;
    private static final int CONTENT_SIDE_PADDING = 32;
    private static final int ROW_HEIGHT = 24;
    private static final int INPUT_HEIGHT = 20;
    private static final int TITLE_INPUT_HEIGHT = 40;  // 标题信息输入框高度（原来的2倍）
    private static final int MULTILINE_INPUT_HEIGHT = 40;  // 内容信息和自定义信息输入框高度（与标题信息一致）
    private static final int TOGGLE_WIDTH = 56;
    private static final int SECTION_HEADER_HEIGHT = 18;
    private static final int SECTION_GAP = 16;
    private static final int ROW_GAP = 10;
    private static final int FOOTER_COLUMN_GAP = 12;
    private static final int VIEWPORT_TOP = 34;
    private static final int VIEWPORT_BOTTOM_MARGIN = 12;
    private static final int SCROLL_STEP = 18;
    private static final int SECTION_LINE_COLOR = 0x80FFFFFF;
    private static final int SECTION_TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xE0E0E0;
    private static final int LABEL_HOVER_COLOR = 0xFFFFA0;
    private static final int BUTTON_BAR_COLOR = 0x90000000;
    private static final int SCROLL_TRACK_COLOR = 0x60303030;
    private static final int SCROLL_THUMB_COLOR = 0xB0FFFFFF;

    private final Screen parent;
    private final TabConfig initialConfig;

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

        // 先添加所有滚动区域的控件
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
        this.topContentInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2); // 最大高度为原来的2倍
        this.topContentInput.setValue(initialConfig.topContentText());

        this.betterPingEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.betterPingEnabled()));
        this.onlineDurationEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.onlineDurationEnabled()));
        this.titleEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.titleEnabled()));

        this.footerCustomInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), MULTILINE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.footer.custom")));
        this.footerCustomInput.setMaxVisibleLength(TabConfig.MAX_FOOTER_CUSTOM_LENGTH);
        this.footerCustomInput.setAutoResize(true);
        this.footerCustomInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2); // 最大高度为原来的2倍
        this.footerCustomInput.setValue(initialConfig.footerCustomText());

        this.footerTpsEnabled = addRenderableWidget(newToggle(layout.footerFirstToggleX(), initialConfig.footerTpsEnabled()));
        this.footerMsptEnabled = addRenderableWidget(newToggle(layout.footerSecondToggleX(), initialConfig.footerMsptEnabled()));
        this.footerOnlineEnabled = addRenderableWidget(newToggle(layout.footerThirdToggleX(), initialConfig.footerOnlineEnabled()));

        // 最后添加底部固定按钮，确保它们在 children() 列表的最后
        // Minecraft 的 Screen.mouseClicked() 会反向遍历，所以最后添加的会最先处理
        this.doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
            .bounds(layout.doneButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());
        this.cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());

        clampScroll(layout);
        applyWidgetLayout(layout);
        // 移除默认聚焦，让用户手动点击输入框
        // setInitialFocus(this.topTitleInput);
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
            if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) {
                return false;
            }

            setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
            return true;
        }

        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) {
            return false;
        }

        setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = buildLayout();
        
        // Minecraft 原版风格：对于固定在特殊位置的按钮（如底部按钮栏），
        // 需要手动优先处理，防止事件穿透到被遮挡的控件。
        // 参考：Minecraft 原版的 OptionsScreen、VideoSettingsScreen 等都使用类似方法。
        
        // 首先检查是否点击了整个底部按钮栏区域
        // 如果点击了底部按钮栏区域，需要拦截事件，防止穿透到下层控件
        if (mouseY >= layout.buttonBarTop()) {
            // 点击了底部按钮栏区域，检查是否点击了按钮
            boolean clickedButton = false;
            
            if (this.doneButton != null) {
                int btnX = this.doneButton.getX();
                int btnY = this.doneButton.getY();
                int btnW = this.doneButton.getWidth();
                int btnH = this.doneButton.getHeight();
                if (mouseX >= btnX && mouseX <= btnX + btnW && 
                    mouseY >= btnY && mouseY <= btnY + btnH) {
                    // 点击了 Done 按钮
                    return this.doneButton.mouseClicked(mouseX, mouseY, button);
                }
            }
            
            if (this.cancelButton != null) {
                int btnX = this.cancelButton.getX();
                int btnY = this.cancelButton.getY();
                int btnW = this.cancelButton.getWidth();
                int btnH = this.cancelButton.getHeight();
                if (mouseX >= btnX && mouseX <= btnX + btnW && 
                    mouseY >= btnY && mouseY <= btnY + btnH) {
                    // 点击了 Cancel 按钮
                    return this.cancelButton.mouseClicked(mouseX, mouseY, button);
                }
            }
            
            // 点击了底部按钮栏的空白区域，拦截事件，不传递到下层
            // 返回 true 表示事件已处理，防止穿透
            return true;
        }
        
        // 使用 Minecraft 原版的标准控件点击处理
        // Screen.mouseClicked() 会遍历所有 children() 并调用它们的 mouseClicked
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            return true;
        }
        
        // 处理自定义的滚动条点击（这是自定义功能，不是原版的）
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
                    int newScrollOffset = (int) ((mouseY - trackTop - thumbHeight / 2) * layout.maxScroll() / thumbTravel);
                    setScrollOffset(newScrollOffset, layout);
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            Layout layout = buildLayout();
            int trackTop = layout.viewportTop();
            int trackBottom = layout.viewportBottom();
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / Math.max(trackHeight, layout.contentHeight()));
            int thumbTravel = trackHeight - thumbHeight;
            
            if (thumbTravel > 0) {
                int deltaY = (int) mouseY - dragStartY;
                int newScrollOffset = dragStartScrollOffset + (deltaY * layout.maxScroll() / thumbTravel);
                setScrollOffset(newScrollOffset, layout);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = buildLayout();
        clampScroll(layout);
        applyWidgetLayout(layout);

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        renderScrollableContent(guiGraphics, mouseX, mouseY, partialTick, layout);
        renderButtonBar(guiGraphics, layout);
        renderFixedWidgets(guiGraphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(guiGraphics, mouseX, mouseY, layout);
    }

    /**
     * 绘制滚动配置区域。
     */
    private void renderScrollableContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Layout layout) {
        guiGraphics.enableScissor(layout.scissorLeft(), layout.viewportTop(), layout.scissorRight(), layout.viewportBottom());

        drawSectionHeader(guiGraphics, layout.left(), layout.right(), layout.toScreenY(layout.topSectionHeaderY()), Component.translatable("screen.neotab.section.top"));
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.top.title"), layout.topTitleLabelBounds(), mouseX, mouseY);
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.top.content"), layout.topContentLabelBounds(), mouseX, mouseY);

        drawSectionHeader(guiGraphics, layout.left(), layout.right(), layout.toScreenY(layout.listSectionHeaderY()), Component.translatable("screen.neotab.section.list"));
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.list.better_ping"), layout.betterPingLabelBounds(), mouseX, mouseY);
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.list.online_duration"), layout.onlineDurationLabelBounds(), mouseX, mouseY);
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.list.title"), layout.titleLabelBounds(), mouseX, mouseY);

        drawSectionHeader(guiGraphics, layout.left(), layout.right(), layout.toScreenY(layout.footerSectionHeaderY()), Component.translatable("screen.neotab.section.footer"));
        drawSettingRow(guiGraphics, Component.translatable("screen.neotab.footer.custom"), layout.footerCustomLabelBounds(), mouseX, mouseY);
        drawFooterOption(guiGraphics, Component.translatable("screen.neotab.footer.tps"), layout.footerFirstLabelBounds(), mouseX, mouseY);
        drawFooterOption(guiGraphics, Component.translatable("screen.neotab.footer.mspt"), layout.footerSecondLabelBounds(), mouseX, mouseY);
        drawFooterOption(guiGraphics, Component.translatable("screen.neotab.footer.online"), layout.footerThirdLabelBounds(), mouseX, mouseY);

        renderScrollableWidgets(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, layout);
    }

    /**
     * 绘制所有随滚动变化的控件，不包括底部固定按钮。
     */
    private void renderScrollableWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            if (renderable == this.doneButton || renderable == this.cancelButton) {
                continue;
            }
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 完成/取消按钮固定在底部操作栏中。
     */
    private void renderFixedWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.doneButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 绘制底部操作栏。
     */
    private void renderButtonBar(GuiGraphics guiGraphics, Layout layout) {
        guiGraphics.fill(0, layout.buttonBarTop(), this.width, this.height, BUTTON_BAR_COLOR);
        guiGraphics.fill(0, layout.buttonBarTop(), this.width, layout.buttonBarTop() + 1, SECTION_LINE_COLOR);
    }

    /**
     * 绘制滚动条。
     */
    private void renderScrollbar(GuiGraphics guiGraphics, Layout layout) {
        if (layout.maxScroll() <= 0) {
            return;
        }

        int trackX = layout.right() + 6;
        int trackTop = layout.viewportTop();
        int trackBottom = layout.viewportBottom();
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = Math.max(20, trackHeight * trackHeight / Math.max(trackHeight, layout.contentHeight()));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackTop + this.scrollOffset * thumbTravel / layout.maxScroll();

        guiGraphics.fill(trackX, trackTop, trackX + 4, trackBottom, SCROLL_TRACK_COLOR);
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, SCROLL_THUMB_COLOR);
    }

    /**
     * 绘制悬浮 tooltip。
     */
    private void renderHoveredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        if (!isInsideViewport(mouseX, mouseY, layout)) {
            return;
        }

        HoverTarget hoverTarget = hoveredTarget(mouseX, mouseY, layout);
        if (hoverTarget != null) {
            guiGraphics.renderTooltip(this.font, hoverTarget.tooltip(), mouseX, mouseY);
        }
    }

    /**
     * 绘制分组标题与分隔线。
     */
    private void drawSectionHeader(GuiGraphics guiGraphics, int left, int right, int y, Component title) {
        guiGraphics.drawString(this.font, title.copy().withStyle(ChatFormatting.WHITE), left, y, SECTION_TITLE_COLOR);
        int lineY = y + 10;
        guiGraphics.fill(left + this.font.width(title) + 8, lineY, right, lineY + 1, SECTION_LINE_COLOR);
    }

    /**
     * 绘制普通设置项标签。
     */
    private void drawSettingRow(GuiGraphics guiGraphics, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(guiGraphics, label, bounds, mouseX, mouseY);
    }

    /**
     * 绘制底部三列的标签。
     */
    private void drawFooterOption(GuiGraphics guiGraphics, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        drawLabel(guiGraphics, label, bounds, mouseX, mouseY);
    }

    /**
     * 统一绘制标签，悬浮时提亮颜色。
     */
    private void drawLabel(GuiGraphics guiGraphics, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        int color = bounds.contains(mouseX, mouseY) ? LABEL_HOVER_COLOR : LABEL_COLOR;
        int labelY = bounds.y() + (INPUT_HEIGHT - this.font.lineHeight) / 2 + 1;
        guiGraphics.drawString(this.font, label, bounds.x(), labelY, color);
    }

    /**
     * 创建布尔开关按钮。
     */
    private CycleButton<Boolean> newToggle(int x, boolean initialValue) {
        return CycleButton.onOffBuilder(initialValue)
            .displayOnlyValue()
            .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY);
    }

    /**
     * 根据鼠标位置判断当前应该显示哪个 tooltip。
     */
    private HoverTarget hoveredTarget(int mouseX, int mouseY, Layout layout) {
        if (layout.topTitleLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.top.title.tooltip"));
        }
        if (layout.topContentLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.top.content.tooltip"));
        }
        if (layout.betterPingLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.list.better_ping.tooltip"));
        }
        if (layout.onlineDurationLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.list.online_duration.note"));
        }
        if (layout.titleLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.list.title.tooltip"));
        }
        if (layout.footerCustomLabelBounds().contains(mouseX, mouseY)) {
            return new HoverTarget(Component.translatable("screen.neotab.footer.custom.tooltip"));
        }
        return null;
    }

    /**
     * 鼠标是否位于滚动内容可视区内。
     */
    private boolean isInsideViewport(double mouseX, double mouseY, Layout layout) {
        return mouseX >= layout.left()
            && mouseX <= layout.right() + 12
            && mouseY >= layout.viewportTop()
            && mouseY <= layout.viewportBottom();
    }

    /**
     * 找出当前鼠标所在位置的可滚动控件。
     *
     * <p>这里专门用来识别鼠标是否悬停在开关按钮上。
     * 如果是，就由页面自己处理滚轮，避免开关因为滚轮被误切换。</p>
     */
    private AbstractWidget hoveredScrollableWidget(double mouseX, double mouseY) {
        for (Renderable renderable : this.renderables) {
            if (renderable == this.doneButton || renderable == this.cancelButton) {
                continue;
            }
            if (renderable instanceof AbstractWidget widget && widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * 设置滚动偏移，并同步刷新控件坐标。
     */
    private void setScrollOffset(int nextOffset, Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(nextOffset, layout.maxScroll()));
        applyWidgetLayout(layout);
    }

    /**
     * 在窗口尺寸变化后重新钳制滚动位置。
     */
    private void clampScroll(Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, layout.maxScroll()));
    }

    /**
     * 把所有滚动控件移动到当前布局对应的位置。
     */
    private void applyWidgetLayout(Layout layout) {
        placeScrollableWidget(this.topTitleEnabled, layout.toggleX(), layout.toScreenY(layout.topTitleRowY()));
        placeScrollableWidget(this.topTitleInput, layout.left(), layout.toScreenY(layout.topTitleInputY()));
        placeScrollableWidget(this.topContentEnabled, layout.toggleX(), layout.toScreenY(layout.topContentRowY()));
        placeScrollableWidget(this.topContentInput, layout.left(), layout.toScreenY(layout.topContentInputY()));
        placeScrollableWidget(this.betterPingEnabled, layout.toggleX(), layout.toScreenY(layout.betterPingRowY()));
        placeScrollableWidget(this.onlineDurationEnabled, layout.toggleX(), layout.toScreenY(layout.onlineDurationRowY()));
        placeScrollableWidget(this.titleEnabled, layout.toggleX(), layout.toScreenY(layout.titleRowY()));
        placeScrollableWidget(this.footerCustomInput, layout.left(), layout.toScreenY(layout.footerCustomInputY()));
        placeScrollableWidget(this.footerTpsEnabled, layout.footerFirstToggleX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerMsptEnabled, layout.footerSecondToggleX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerOnlineEnabled, layout.footerThirdToggleX(), layout.toScreenY(layout.footerRowY()));

        this.doneButton.setX(layout.doneButtonX());
        this.doneButton.setY(layout.buttonY());
        this.cancelButton.setX(layout.cancelButtonX());
        this.cancelButton.setY(layout.buttonY());
    }

    /**
     * 统一移动单个滚动控件。
     */
    private void placeScrollableWidget(AbstractWidget widget, int x, int y) {
        widget.setX(x);
        widget.setY(y);
    }

    /**
     * 保存界面配置并发回服务端。
     */
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
     *
     * <p>顶部和底部新增输入框后，滚动区高度继续统一由按钮栏上方决定，
     * 所以界面在自动缩放下仍然不会把内容挤出屏幕。</p>
     */
    private Layout buildLayout() {
        int contentWidth = Math.min(MAX_CONTENT_WIDTH, this.width - CONTENT_SIDE_PADDING);
        int left = (this.width - contentWidth) / 2;
        int right = left + contentWidth;
        int toggleX = right - TOGGLE_WIDTH;

        int buttonWidth = Math.min(150, (contentWidth - 10) / 2);
        int buttonY = this.height - 6 - INPUT_HEIGHT;
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

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
        int footerFirstColumnX = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;

        int labelWidth = Math.max(80, contentWidth - TOGGLE_WIDTH - 8);
        int contentHeight = footerRowY + ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));

        return new Layout(
            contentWidth,
            left,
            right,
            toggleX,
            labelWidth,
            this.scrollOffset,
            viewportTop,
            viewportBottom,
            buttonBarTop,
            topSectionHeaderY,
            topTitleRowY,
            topTitleInputY,
            topContentRowY,
            topContentInputY,
            listSectionHeaderY,
            betterPingRowY,
            onlineDurationRowY,
            titleRowY,
            footerSectionHeaderY,
            footerCustomRowY,
            footerCustomInputY,
            footerRowY,
            footerFirstColumnX,
            footerSecondColumnX,
            footerThirdColumnX,
            footerFirstColumnX + footerColumnWidth - TOGGLE_WIDTH,
            footerSecondColumnX + footerColumnWidth - TOGGLE_WIDTH,
            footerThirdColumnX + footerColumnWidth - TOGGLE_WIDTH,
            contentHeight,
            maxScroll,
            buttonWidth,
            buttonY,
            this.width / 2 - buttonWidth - 5,
            this.width / 2 + 5,
            labelBounds(Component.translatable("screen.neotab.top.title"), left, viewportTop - this.scrollOffset + topTitleRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.top.content"), left, viewportTop - this.scrollOffset + topContentRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.better_ping"), left, viewportTop - this.scrollOffset + betterPingRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.online_duration"), left, viewportTop - this.scrollOffset + onlineDurationRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.title"), left, viewportTop - this.scrollOffset + titleRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.custom"), left, viewportTop - this.scrollOffset + footerCustomRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.tps"), footerFirstColumnX, viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.mspt"), footerSecondColumnX, viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.online"), footerThirdColumnX, viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font)
        );
    }

    /**
     * 为标签生成可悬浮的命中区域。
     */
    private LabelBounds labelBounds(Component text, int x, int y, int maxWidth, Font font) {
        int width = Math.min(font.width(text), maxWidth);
        return new LabelBounds(x, y, Math.max(width, 1), INPUT_HEIGHT);
    }

    /**
     * 单个标签的悬浮命中区。
     */
    private record LabelBounds(int x, int y, int width, int height) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x
                && mouseX <= this.x + this.width
                && mouseY >= this.y
                && mouseY <= this.y + this.height;
        }
    }

    /**
     * 当前悬浮标签要显示的 tooltip。
     */
    private record HoverTarget(Component tooltip) {
    }

    /**
     * 布局快照，集中保存当前窗口尺寸下的坐标数据。
     */
    private record Layout(
        int contentWidth,
        int left,
        int right,
        int toggleX,
        int labelWidth,
        int scrollOffset,
        int viewportTop,
        int viewportBottom,
        int buttonBarTop,
        int topSectionHeaderY,
        int topTitleRowY,
        int topTitleInputY,
        int topContentRowY,
        int topContentInputY,
        int listSectionHeaderY,
        int betterPingRowY,
        int onlineDurationRowY,
        int titleRowY,
        int footerSectionHeaderY,
        int footerCustomRowY,
        int footerCustomInputY,
        int footerRowY,
        int footerFirstColumnX,
        int footerSecondColumnX,
        int footerThirdColumnX,
        int footerFirstToggleX,
        int footerSecondToggleX,
        int footerThirdToggleX,
        int contentHeight,
        int maxScroll,
        int buttonWidth,
        int buttonY,
        int doneButtonX,
        int cancelButtonX,
        LabelBounds topTitleLabelBounds,
        LabelBounds topContentLabelBounds,
        LabelBounds betterPingLabelBounds,
        LabelBounds onlineDurationLabelBounds,
        LabelBounds titleLabelBounds,
        LabelBounds footerCustomLabelBounds,
        LabelBounds footerFirstLabelBounds,
        LabelBounds footerSecondLabelBounds,
        LabelBounds footerThirdLabelBounds
    ) {
        private int toScreenY(int contentY) {
            return this.viewportTop - this.scrollOffset + contentY;
        }

        private int scissorLeft() {
            return Math.max(0, this.left - 2);
        }

        private int scissorRight() {
            return this.right + 10;
        }
    }
}
