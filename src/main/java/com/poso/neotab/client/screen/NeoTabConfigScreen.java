package com.poso.neotab.client.screen;

import com.poso.neotab.client.gui.AEStyleRenderer;
import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.SaveConfigPayload;
import com.poso.neotab.network.payload.SavePlayerConfigPayload;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoTab config screen - delegates to manager classes. */
public class NeoTabConfigScreen extends Screen {
    //  Enums 
    public enum ScreenMode { ADMIN, PLAYER }

    public enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme"),
        PERMISSIONS("screen.neotab.tab.permissions");
        final String langKey;
        ConfigTab(String langKey) { this.langKey = langKey; }
        Component label() { return Component.translatable(langKey); }
    }

    //  Constants 
    private static final int MAX_CONTENT_WIDTH      = 360;
    private static final int CONTENT_SIDE_PADDING   = 32;
    private static final int ROW_HEIGHT              = 24;
    private static final int INPUT_HEIGHT            = 20;
    private static final int TITLE_INPUT_HEIGHT      = 60;
    private static final int MULTILINE_INPUT_HEIGHT  = 60;
    private static final int TOGGLE_WIDTH            = 56;
    private static final int LAYOUT_BUTTON_WIDTH     = 80;
    private static final int THEME_OPTION_HEIGHT     = 20;
    private static final int THEME_OPTION_GAP        = 4;
    private static final int THEME_LIST_INSET        = 4;
    private static final int THEME_LIST_TOP_GAP      = 4;
    private static final int THEME_INDICATOR_SIZE    = 8;
    private static final int SECTION_HEADER_HEIGHT   = 18;
    private static final int SECTION_GAP             = 16;
    private static final int ROW_GAP                 = 10;
    private static final int FOOTER_COLUMN_GAP       = 12;
    private static final int VIEWPORT_TOP            = 34;
    private static final int VIEWPORT_BOTTOM_MARGIN  = 12;
    private static final int SCROLL_STEP             = 18;
    private static final int CONTENT_TOP_PADDING     = 8;
    private static final int TAB_BAR_WIDTH           = 72;
    private static final int TAB_CONTENT_GAP         = 8;
    private static final int TAB_BUTTON_HEIGHT       = 24;
    private static final int TAB_BUTTON_GAP          = 4;
    private static final int TAB_BUTTON_LEFT_PADDING = 6;
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
    private static final int TAB_TEXT_ACTIVE      = 0xFFE8ECF0;
    private static final int TAB_TEXT_INACTIVE    = 0xFF3A3A3A;
    private static final int SCROLL_TRACK_W = 14;
    private static final int TOOLTIP_MAX_WIDTH = 200;

    //  Fields 
    private final Screen parent;
    private final TabConfig initialConfig;
    private ConfigTab activeTab = ConfigTab.PAGE_CONFIG;
    private final ScreenMode screenMode;
    private final PlayerCustomizePolicy policy;
    private final com.poso.neotab.config.PlayerTabConfig personalConfig;

    // Managers
    final PageConfigTabManager pageConfig;
    final PermissionsTabManager permissions;
    final ThemeTabManager theme;

    // Bottom buttons (managed by main class)
    private Button doneButton;
    private Button cancelButton;

    // Scroll state
    private int scrollOffset;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;

    //  Constructors 
    /** Admin constructor (full config, no restrictions). */
    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
        this.screenMode = ScreenMode.ADMIN;
        this.policy = com.poso.neotab.permission.PlayerCustomizePolicy.unlocked();
        this.personalConfig = null;
        this.pageConfig = new PageConfigTabManager(this);
        this.permissions = new PermissionsTabManager(this);
        this.theme = new ThemeTabManager(this);
    }

    /** Player constructor (personal customization, restricted by policy). */
    public NeoTabConfigScreen(Screen parent, TabConfig serverConfig,
                              ScreenMode mode, PlayerCustomizePolicy policy,
                              com.poso.neotab.config.PlayerTabConfig personalConfig) {
        super(Component.translatable(
            mode == ScreenMode.PLAYER ? "screen.neotab.title.player" : "screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = serverConfig;
        this.screenMode = mode;
        this.policy = policy != null ? policy : com.poso.neotab.permission.PlayerCustomizePolicy.locked();
        this.personalConfig = personalConfig;
        this.pageConfig = new PageConfigTabManager(this);
        this.permissions = new PermissionsTabManager(this);
        this.theme = new ThemeTabManager(this);
    }

    //  Manager callback helpers 
    <T extends AbstractWidget> T addWidget(T widget) { return addRenderableWidget(widget); }
    @Override public void removeWidget(net.minecraft.client.gui.components.events.GuiEventListener widget) { super.removeWidget(widget); }
    Font font() { return this.font; }
    ConfigTab getActiveTab() { return activeTab; }
    TabConfig getInitialConfig() { return initialConfig; }
    ScreenMode getScreenMode() { return screenMode; }
    void syncVisibility() { syncTabWidgetVisibility(); }
    void reinit() { this.init(); }
    NeoTabConfigScreenLayout.Layout buildLayout() { return buildLayoutImpl(); }
    void applyLayout(NeoTabConfigScreenLayout.Layout layout) { applyWidgetLayout(layout); }
    void setScrollOffset(int offset, NeoTabConfigScreenLayout.Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(offset, layout.maxScroll()));
        applyWidgetLayout(layout);
    }
    void adjustLayoutConfigToCurrentLimits() { pageConfig.adjustLayoutConfigToCurrentLimits(); }

    //  init() 
    @Override
    protected void init() {
        clearWidgets();
        theme.themeOptionButtons.clear();
        theme.themeOptionIds.clear();
        permissions.clear();
        if (activeTab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) {
            activeTab = ConfigTab.PAGE_CONFIG;
        }
        com.poso.neotab.config.TabConfig effectiveInit = (screenMode == ScreenMode.PLAYER && personalConfig != null)
            ? personalConfig.mergeInto(initialConfig, policy)
            : initialConfig;
        theme.selectedThemeId = com.poso.neotab.theme.TabThemeRegistry.get(effectiveInit.tabTheme()).id();
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        pageConfig.initPageConfigWidgets(layout, effectiveInit, policy);
        theme.initThemeWidgets(layout);
        if (screenMode == ScreenMode.ADMIN) {
            permissions.initPermissionsWidgets(layout, initialConfig);
        }
        pageConfig.initFooterWidgets(layout, effectiveInit, policy);
        this.doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
            .bounds(layout.doneButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT).build());
        this.cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT).build());
        theme.rebuildCustomBorderColorButtons();
        pageConfig.adjustLayoutConfigToCurrentLimits();
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    //  syncTabWidgetVisibility() 
    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean themeTab = activeTab == ConfigTab.THEME;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : permissions.globalPolicyToggles) btn.visible = perms;
        if (permissions.permTargetModeButton != null) permissions.permTargetModeButton.visible = perms;
        if (permissions.playerSearchBox != null) permissions.playerSearchBox.visible = perms;
        if (permissions.permAddButton != null) permissions.permAddButton.visible = perms;
        for (Button btn : permissions.targetPlayerRemoveButtons) btn.visible = perms && permissions.permTargetIsPlayer;
        if (permissions.permSaveButton != null) permissions.permSaveButton.visible = perms;
        // Page config tab widgets
        pageConfig.topTitleEnabled.visible       = page;
        pageConfig.topTitleInput.visible         = page;
        pageConfig.topContentEnabled.visible     = page;
        pageConfig.topContentInput.visible       = page;
        pageConfig.betterPingEnabled.visible     = page;
        pageConfig.onlineDurationEnabled.visible = page;
        pageConfig.titleEnabled.visible          = page;
        pageConfig.healthDisplayEnabled.visible  = page;
        pageConfig.footerCustomInput.visible     = page;
        pageConfig.footerTpsEnabled.visible      = page;
        pageConfig.footerMsptEnabled.visible     = page;
        pageConfig.footerOnlineEnabled.visible   = page;
        // Theme tab widgets
        pageConfig.healthDisplayMode.visible     = themeTab;
        if (pageConfig.layoutEnabledToggle != null) pageConfig.layoutEnabledToggle.visible = themeTab;
        if (pageConfig.layoutColumnsButton != null) pageConfig.layoutColumnsButton.visible = themeTab;
        if (pageConfig.layoutRowsButton    != null) pageConfig.layoutRowsButton.visible    = themeTab;
        for (Button button : theme.themeOptionButtons) button.visible = themeTab;
        boolean isCustom = "custom".equals(theme.selectedThemeId);
        theme.customBackgroundColorButton.visible = themeTab && isCustom;
        theme.customBorderOuterFactorButton.visible = themeTab && isCustom;
        theme.customAnimationToggle.visible = themeTab && isCustom;
        if (theme.customAnimationSpeedButton != null) theme.customAnimationSpeedButton.visible = themeTab && isCustom;
        if (theme.resetToDefaultButton != null) theme.resetToDefaultButton.visible = themeTab && isCustom;
        if (theme.resetConfirmButton != null) theme.resetConfirmButton.visible = themeTab && isCustom && theme.showResetConfirmation;
        if (theme.resetCancelButton != null) theme.resetCancelButton.visible = themeTab && isCustom && theme.showResetConfirmation;
        for (Button button : theme.customBorderColorButtons) button.visible = themeTab && isCustom;
        if (theme.addCustomBorderColorButton != null) theme.addCustomBorderColorButton.visible = themeTab && isCustom;
        if (theme.embeddedColorPicker != null) theme.embeddedColorPicker.visible = themeTab && isCustom;
    }

    //  switchTab 
    private void switchTab(ConfigTab tab) {
        if (activeTab == tab) return;
        activeTab = tab;
        scrollOffset = 0;
        if (theme.showResetConfirmation) {
            theme.showResetConfirmation = false;
        }
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    //  onClose / repositionElements 
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void repositionElements() {
        super.repositionElements();
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        clampScroll(layout);
        applyWidgetLayout(layout);
    }
    // ── mouseScrolled ────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        if (hoveredScrollableWidget(mouseX, mouseY) instanceof CycleButton<?>) {
            if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
            setScrollOffsetInternal(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
            return true;
        }
        AbstractWidget hovered = hoveredScrollableWidget(mouseX, mouseY);
        if (hovered instanceof ImprovedRichTextMultiLineEditBox input) {
            boolean onInputScrollbar = mouseX >= input.getX() + input.getWidth()
                    && mouseX <= input.getX() + input.getWidth() + 8;
            if (onInputScrollbar) return input.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
        setScrollOffsetInternal(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    // ── mouseClicked ─────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        // Bottom button bar
        if (mouseY >= layout.buttonBarTop()) {
            if (theme.showResetConfirmation) { theme.showResetConfirmation = false; syncTabWidgetVisibility(); }
            if (doneButton != null) {
                int bx = doneButton.getX(), by = doneButton.getY();
                if (mouseX >= bx && mouseX <= bx + doneButton.getWidth() && mouseY >= by && mouseY <= by + doneButton.getHeight())
                    return doneButton.mouseClicked(mouseX, mouseY, button);
            }
            if (cancelButton != null) {
                int bx = cancelButton.getX(), by = cancelButton.getY();
                if (mouseX >= bx && mouseX <= bx + cancelButton.getWidth() && mouseY >= by && mouseY <= by + cancelButton.getHeight())
                    return cancelButton.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        // Theme tab buttons
        if (activeTab == ConfigTab.THEME) {
            for (Button themeBtn : theme.themeOptionButtons) {
                if (themeBtn.visible && themeBtn.isMouseOver(mouseX, mouseY)) {
                    if (theme.showResetConfirmation) { theme.showResetConfirmation = false; syncTabWidgetVisibility(); }
                    return themeBtn.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        if ("custom".equals(theme.selectedThemeId) && activeTab == ConfigTab.THEME) {
            if (theme.resetConfirmButton != null && theme.resetConfirmButton.visible && theme.resetConfirmButton.isMouseOver(mouseX, mouseY))
                return theme.resetConfirmButton.mouseClicked(mouseX, mouseY, button);
            if (theme.resetCancelButton != null && theme.resetCancelButton.visible && theme.resetCancelButton.isMouseOver(mouseX, mouseY))
                return theme.resetCancelButton.mouseClicked(mouseX, mouseY, button);
            if (theme.resetToDefaultButton != null && theme.resetToDefaultButton.visible && theme.resetToDefaultButton.isMouseOver(mouseX, mouseY))
                return theme.resetToDefaultButton.mouseClicked(mouseX, mouseY, button);
            if (theme.showResetConfirmation) { theme.showResetConfirmation = false; syncTabWidgetVisibility(); }
            if (theme.customAnimationToggle != null && theme.customAnimationToggle.visible && theme.customAnimationToggle.isMouseOver(mouseX, mouseY))
                return theme.customAnimationToggle.mouseClicked(mouseX, mouseY, button);
            if (theme.customBorderOuterFactorButton != null && theme.customBorderOuterFactorButton.visible && theme.customBorderOuterFactorButton.isMouseOver(mouseX, mouseY))
                return theme.customBorderOuterFactorButton.mouseClicked(mouseX, mouseY, button);
            if (theme.customBackgroundColorButton != null && theme.customBackgroundColorButton.visible && theme.customBackgroundColorButton.isMouseOver(mouseX, mouseY))
                return theme.customBackgroundColorButton.mouseClicked(mouseX, mouseY, button);
            for (Button btn : theme.customBorderColorButtons) {
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) return btn.mouseClicked(mouseX, mouseY, button);
            }
            if (theme.addCustomBorderColorButton != null && theme.addCustomBorderColorButton.visible && theme.addCustomBorderColorButton.isMouseOver(mouseX, mouseY))
                return theme.addCustomBorderColorButton.mouseClicked(mouseX, mouseY, button);
        }
        // Color picker
        if (theme.embeddedColorPicker != null && theme.embeddedColorPicker.visible) {
            int px = theme.embeddedColorPicker.getX(), py = theme.embeddedColorPicker.getY();
            int pw = theme.embeddedColorPicker.getWidth(), ph = theme.embeddedColorPicker.getHeight();
            if (mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph) {
                boolean result = theme.embeddedColorPicker.mouseClicked(mouseX, mouseY, button);
                if (result) { setFocused(theme.embeddedColorPicker); if (button == 0) setDragging(true); return true; }
            }
        }
        // Permissions dropdown
        if (activeTab == ConfigTab.PERMISSIONS && permissions.permTargetIsPlayer
                && permissions.playerSearchBox != null && permissions.playerSearchBox.isFocused()
                && !permissions.playerSuggestions.isEmpty() && button == 0) {
            int dropX = permissions.playerSearchBox.getX();
            int dropY = permissions.playerSearchBox.getY() + INPUT_HEIGHT + 1;
            int dropW = permissions.playerSearchBox.getWidth();
            int itemH = INPUT_HEIGHT - 2;
            for (int i = 0; i < permissions.playerSuggestions.size(); i++) {
                int itemY = dropY + 1 + i * itemH;
                if (mouseX >= dropX && mouseX < dropX + dropW && mouseY >= itemY && mouseY < itemY + itemH) {
                    permissions.playerSearchBox.setValue(permissions.playerSuggestions.get(i));
                    permissions.playerSuggestions.clear();
                    return true;
                }
            }
        }
        // Permissions player name click
        if (activeTab == ConfigTab.PERMISSIONS && permissions.permTargetIsPlayer && button == 0) {
            NeoTabConfigScreenLayout.Layout permLayout = buildLayoutImpl();
            int permY = CONTENT_TOP_PADDING + ROW_HEIGHT + ROW_GAP;
            if (!permissions.targetPlayers.isEmpty()) {
                permY += ROW_HEIGHT;
                for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
                    int rowY = permLayout.toScreenY(permY);
                    int nameX = permLayout.left() + 22;
                    int nameW = permLayout.right() - nameX - 6;
                    if (mouseX >= nameX && mouseX < nameX + nameW && mouseY >= rowY && mouseY < rowY + INPUT_HEIGHT) {
                        permissions.editingPlayerUUID = entry.getKey();
                        permissions.loadPolicyToggles(initialConfig);
                        return true;
                    }
                    permY += ROW_HEIGHT + 2;
                }
            }
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;
        // Tab bar click
        if (button == 0 && mouseY >= VIEWPORT_TOP) {
            int tabBtnX = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int tabBtnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;
            if (mouseX >= tabBtnX && mouseX <= tabBtnX + tabBtnW) {
                int tabIndex = 0;
                for (ConfigTab tab : ConfigTab.values()) {
                    if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;
                    int btnY = VIEWPORT_TOP + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                    if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                        if (tab == ConfigTab.PERMISSIONS) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player == null || !mc.player.hasPermissions(2)) {
                                mc.player.sendSystemMessage(Component.translatable("message.neotab.no_permission"));
                                return true;
                            }
                        }
                        switchTab(tab);
                        return true;
                    }
                    tabIndex++;
                }
            }
        }
        // Scrollbar click
        if (button == 0 && layout.maxScroll() > 0) {
            int thumbX = layout.right() + 8;
            int trackTop = layout.viewportTop(), trackBottom = layout.viewportBottom();
            int trackH = trackBottom - trackTop;
            int thumbPad = 2, thumbW = SCROLL_TRACK_W, thumbSize = thumbW + 6;
            if (mouseX >= thumbX && mouseX <= thumbX + thumbW && mouseY >= trackTop && mouseY <= trackBottom) {
                int travelH = trackH - thumbSize - thumbPad * 2;
                int thumbY = travelH > 0 ? trackTop + thumbPad + this.scrollOffset * travelH / layout.maxScroll() : trackTop + thumbPad;
                if (mouseY >= thumbY && mouseY <= thumbY + thumbSize) {
                    isDraggingScrollbar = true; dragStartY = (int) mouseY; dragStartScrollOffset = this.scrollOffset;
                } else {
                    int newOffset = travelH > 0 ? (int) ((mouseY - trackTop - thumbPad - thumbSize / 2.0) * layout.maxScroll() / travelH) : 0;
                    setScrollOffsetInternal(newOffset, layout);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) { isDraggingScrollbar = false; return true; }
        if (theme.embeddedColorPicker != null && theme.embeddedColorPicker.visible) {
            if (theme.embeddedColorPicker.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
            int trackH = layout.viewportBottom() - layout.viewportTop();
            int thumbSize = SCROLL_TRACK_W + 6, thumbPad = 2;
            int travelH = trackH - thumbSize - thumbPad * 2;
            if (travelH > 0) {
                int deltaY = (int) mouseY - dragStartY;
                setScrollOffsetInternal(dragStartScrollOffset + deltaY * layout.maxScroll() / travelH, layout);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }



    // ── render ───────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        clampScroll(layout);
        applyWidgetLayout(layout);
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int scrollTrackW = SCROLL_TRACK_W;
        int panelX = layout.tabBarX() - 2;
        int panelY = 8;
        int panelW = (layout.right() + 8 + scrollTrackW + 4) - panelX;
        int panelH = this.height - panelY - 8;
        AEStyleRenderer.drawMainPanel(g, panelX, panelY, panelW, panelH);

        net.minecraft.network.chat.Component boldTitle = this.title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        int titleTextW = this.font.width(boldTitle);
        int titleTextX = panelX + (panelW - titleTextW) / 2;
        g.drawString(this.font, boldTitle, titleTextX, panelY + 6, AEStyleRenderer.COLOR_TITLE_TEXT, false);

        NeoTabConfigScreenRenderer.renderTabBar(g, this.font, this.activeTab, layout, mouseX, mouseY, this.screenMode);
        renderScrollableContent(g, mouseX, mouseY, partialTick, layout);
        NeoTabConfigScreenRenderer.renderButtonBar(g, layout, this.height);
        // Fixed widgets (done/cancel buttons)
        NeoTabConfigScreenRenderer.renderAEButton(g, this.font, this.doneButton, mouseX, mouseY);
        NeoTabConfigScreenRenderer.renderAEButton(g, this.font, this.cancelButton, mouseX, mouseY);
        // Permissions dropdown on top
        renderPlayerSuggestionDropdown(g, mouseX, mouseY);
        NeoTabConfigScreenRenderer.renderHoveredTooltip(g, this.font, hoveredTarget(mouseX, mouseY, layout), mouseX, mouseY);
    }

    private void renderPlayerSuggestionDropdown(GuiGraphics g, int mouseX, int mouseY) {
        if (activeTab != ConfigTab.PERMISSIONS) return;
        if (!permissions.permTargetIsPlayer || permissions.playerSearchBox == null) return;
        if (!permissions.playerSearchBox.isFocused() || permissions.playerSuggestions.isEmpty()) return;
        int dropX = permissions.playerSearchBox.getX();
        int dropY = permissions.playerSearchBox.getY() + INPUT_HEIGHT + 1;
        int dropW = permissions.playerSearchBox.getWidth();
        int itemH = INPUT_HEIGHT - 2;
        int totalH = permissions.playerSuggestions.size() * itemH + 2;
        g.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + totalH + 1, AEStyleRenderer.COLOR_OUTLINE);
        g.fill(dropX, dropY, dropX + dropW, dropY + totalH, 0xFF2A2A2A);
        for (int i = 0; i < permissions.playerSuggestions.size(); i++) {
            int itemY = dropY + 1 + i * itemH;
            boolean hovered = mouseX >= dropX && mouseX < dropX + dropW && mouseY >= itemY && mouseY < itemY + itemH;
            if (hovered) g.fill(dropX, itemY, dropX + dropW, itemY + itemH, 0xFF334466);
            g.drawString(this.font, permissions.playerSuggestions.get(i),
                dropX + 4, itemY + (itemH - this.font.lineHeight) / 2,
                hovered ? 0xFF55FF55 : AEStyleRenderer.COLOR_LABEL, false);
        }
    }

    private void renderScrollableContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, NeoTabConfigScreenLayout.Layout layout) {
        int contentAreaX = layout.left() - 4;
        int contentAreaY = layout.viewportTop() - 2;
        int contentAreaW = layout.right() - layout.left() + 8;
        int contentAreaH = layout.viewportBottom() - layout.viewportTop() + 4;
        AEStyleRenderer.drawContentArea(g, contentAreaX, contentAreaY, contentAreaW, contentAreaH);
        g.enableScissor(layout.scissorLeft(), layout.viewportTop(), layout.scissorRight(), layout.viewportBottom());

        if (activeTab == ConfigTab.PAGE_CONFIG) {
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.top"),
                    layout.left(), layout.toScreenY(layout.topSectionHeaderY()), layout.right());
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.top.title"), layout.topTitleLabelBounds(), mouseX, mouseY);
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.top.content"), layout.topContentLabelBounds(), mouseX, mouseY);
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.list"),
                    layout.left(), layout.toScreenY(layout.listSectionHeaderY()), layout.right());
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.list.better_ping"), layout.betterPingLabelBounds(), mouseX, mouseY);
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.list.online_duration"), layout.onlineDurationLabelBounds(), mouseX, mouseY);
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.list.title"), layout.titleLabelBounds(), mouseX, mouseY);
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.list.health_display"), layout.healthDisplayLabelBounds(), mouseX, mouseY);
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.footer"),
                    layout.left(), layout.toScreenY(layout.footerSectionHeaderY()), layout.right());
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.footer.custom"), layout.footerCustomLabelBounds(), mouseX, mouseY);
        } else if (activeTab == ConfigTab.THEME) {
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.theme.tab_theme"),
                    layout.left(), layout.toScreenY(layout.themeSectionHeaderY()), layout.right());
            NeoTabConfigScreenRenderer.renderThemeSelectorBackground(g, layout);
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.health"),
                    layout.left(), layout.toScreenY(layout.healthSectionHeaderY()), layout.right());
            NeoTabConfigScreenRenderer.drawLabel(g, this.font, Component.translatable("screen.neotab.theme.health_mode"), layout.healthModeLabelBounds(), mouseX, mouseY);
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.layout"),
                    layout.left(), layout.toScreenY(layout.layoutSectionHeaderY()), layout.right());
            if ("custom".equals(theme.selectedThemeId)) {
                int customConfigStartY = layout.themeSelectorY() + layout.themeSelectorHeight() + THEME_LIST_TOP_GAP;
                int customConfigY = customConfigStartY + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 15;
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.animation"),
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), AEStyleRenderer.COLOR_SECTION_TEXT, false);
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.background"),
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), AEStyleRenderer.COLOR_SECTION_TEXT, false);
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.outer_border"),
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), AEStyleRenderer.COLOR_SECTION_TEXT, false);
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.border"),
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), AEStyleRenderer.COLOR_SECTION_TEXT, false);
            }
        } else if (activeTab == ConfigTab.PERMISSIONS) {
            renderPermissionsContent(g, mouseX, mouseY, layout);
        }
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        NeoTabConfigScreenRenderer.renderScrollbar(g, layout, this.scrollOffset);
    }

    private void renderPermissionsContent(GuiGraphics g, int mouseX, int mouseY, NeoTabConfigScreenLayout.Layout layout) {
        String[] policyKeys = {
            "screen.neotab.policy.top_title_toggle",   "screen.neotab.policy.top_title_edit",
            "screen.neotab.policy.top_content_toggle", "screen.neotab.policy.top_content_edit",
            "screen.neotab.policy.ping_toggle",        "screen.neotab.policy.duration_toggle",
            "screen.neotab.policy.title_toggle",       "screen.neotab.policy.health_toggle",
            "screen.neotab.policy.health_mode",        "screen.neotab.policy.footer_custom",
            "screen.neotab.policy.footer_tps",         "screen.neotab.policy.footer_mspt",
            "screen.neotab.policy.footer_online",      "screen.neotab.policy.theme",
            "screen.neotab.policy.refresh_interval"
        };
        int y = CONTENT_TOP_PADDING;
        y += ROW_HEIGHT + ROW_GAP;
        int titleY = layout.toScreenY(y) + (INPUT_HEIGHT - this.font.lineHeight) / 2;
        if (permissions.permTargetIsPlayer) {
            g.drawString(this.font, Component.translatable("screen.neotab.policy.target_list"),
                layout.left(), titleY, AEStyleRenderer.COLOR_SECTION_TEXT, false);
        } else {
            g.drawString(this.font, Component.translatable("screen.neotab.policy.target_list_hint"),
                layout.left(), titleY, 0xFF666666, false);
        }
        y += ROW_HEIGHT;
        int tagAreaY = layout.toScreenY(y);
        int tagAreaH = INPUT_HEIGHT;
        AEStyleRenderer.drawSunkenPanel(g, layout.left(), tagAreaY, layout.contentWidth() - 6, tagAreaH, AEStyleRenderer.COLOR_CONTENT_BG, 1);
        if (permissions.permTargetIsPlayer) {
            if (permissions.targetPlayers.isEmpty()) {
                g.drawString(this.font, Component.translatable("screen.neotab.policy.no_targets"),
                    layout.left() + 4, tagAreaY + (tagAreaH - this.font.lineHeight) / 2, 0xFF888888, false);
            } else {
                int tagX = layout.left() + 3, tagY = tagAreaY + 2, tagH = tagAreaH - 2, tagPadX = 4, removeW = 14;
                for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
                    boolean isEditing = entry.getKey().equals(permissions.editingPlayerUUID);
                    String name = entry.getValue();
                    int nameW = this.font.width(name);
                    int tagW = nameW + tagPadX * 2 + removeW + 2;
                    if (tagX + tagW > layout.right() - 8) break;
                    g.fill(tagX, tagY, tagX + tagW, tagY + tagH, isEditing ? 0xFF334466 : 0xFF4A4E58);
                    AEStyleRenderer.drawOutline(g, tagX, tagY, tagW, tagH, isEditing ? 0xFF5577AA : AEStyleRenderer.COLOR_OUTLINE, 1);
                    g.drawString(this.font, name, tagX + tagPadX, tagY + (tagH - this.font.lineHeight) / 2,
                        isEditing ? 0xFF55FF55 : AEStyleRenderer.COLOR_LABEL, false);
                    tagX += tagW + 3;
                }
            }
        }
        y += tagAreaH + 4;
        String sectionKey = permissions.permTargetIsPlayer && permissions.editingPlayerUUID != null
            ? "screen.neotab.policy.section_player" : "screen.neotab.policy.section_all";
        AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable(sectionKey),
            layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;
        int itemGap = 4, itemW = (layout.contentWidth() - 6 - itemGap) / 2, itemH = INPUT_HEIGHT + 2, labelPad = 4;
        for (int i = 0; i < policyKeys.length; i++) {
            int col = i % 2, row = i / 2;
            if (col == 0 && i > 0) y += itemH + 2;
            int itemX = layout.left() + col * (itemW + itemGap);
            int itemY = layout.toScreenY(y);
            AEStyleRenderer.drawRaisedPanelNoOutline(g, itemX, itemY, itemW, itemH, AEStyleRenderer.COLOR_BUTTON_BG, 1);
            g.drawString(this.font, Component.translatable(policyKeys[i]), itemX + labelPad,
                itemY + (itemH - this.font.lineHeight) / 2, AEStyleRenderer.COLOR_LABEL, false);
        }
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            if (r instanceof CycleButton<?> cb) {
                NeoTabConfigScreenRenderer.renderAECycleButton(g, this.font, cb, mouseX, mouseY);
            } else if (r instanceof Button btn) {
                int themeIndex = theme.themeOptionButtons.indexOf(btn);
                if (themeIndex >= 0) {
                    NeoTabConfigScreenRenderer.renderThemeOptionButton(g, this.font, btn, theme.themeOptionIds.get(themeIndex), theme.selectedThemeId, mouseX, mouseY);
                } else if (btn == theme.customBackgroundColorButton) {
                    NeoTabConfigScreenRenderer.renderSelectableColorButton(g, this.font, btn, "background".equals(theme.currentSelectedColorType), mouseX, mouseY);
                } else if (theme.customBorderColorButtons.contains(btn)) {
                    int bi = theme.customBorderColorButtons.indexOf(btn);
                    if (bi % 2 == 0) {
                        NeoTabConfigScreenRenderer.renderSelectableColorButton(g, this.font, btn, ("border_" + (bi / 2)).equals(theme.currentSelectedColorType), mouseX, mouseY);
                    } else {
                        NeoTabConfigScreenRenderer.renderAEButton(g, this.font, btn, mouseX, mouseY);
                    }
                } else if (btn == theme.customBorderOuterFactorButton) {
                    NeoTabConfigScreenRenderer.renderSelectableColorButton(g, this.font, btn, "outer_border".equals(theme.currentSelectedColorType), mouseX, mouseY);
                } else {
                    NeoTabConfigScreenRenderer.renderAEButton(g, this.font, btn, mouseX, mouseY);
                }
            } else {
                r.render(g, mouseX, mouseY, partialTick);
            }
        }
    }



    // ── applyWidgetLayout ────────────────────────────────────────────────────
    private void applyWidgetLayout(NeoTabConfigScreenLayout.Layout layout) {
        pageConfig.applyLayout(layout);
        theme.applyLayout(layout);
        if (activeTab == ConfigTab.PERMISSIONS) {
            permissions.applyLayout(layout, ROW_HEIGHT, ROW_GAP, INPUT_HEIGHT,
                CONTENT_TOP_PADDING, SECTION_HEADER_HEIGHT, TOGGLE_WIDTH);
        }
        if (doneButton != null) { doneButton.setX(layout.doneButtonX()); doneButton.setY(layout.buttonY()); }
        if (cancelButton != null) { cancelButton.setX(layout.cancelButtonX()); cancelButton.setY(layout.buttonY()); }
    }



    // ── buildLayoutImpl ──────────────────────────────────────────────────────
    private NeoTabConfigScreenLayout.Layout buildLayoutImpl() {
        int minSidePadding = 16;
        int scrollbarAndMargin = SCROLL_TRACK_W + 20;
        int availableWidth = this.width - minSidePadding * 2 - TAB_BAR_WIDTH - TAB_CONTENT_GAP - scrollbarAndMargin;
        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(200, availableWidth));
        int totalWidth = TAB_BAR_WIDTH + TAB_CONTENT_GAP + contentWidth + scrollbarAndMargin;
        int blockLeft = Math.max(minSidePadding, (this.width - totalWidth) / 2);
        int tabBarX = blockLeft;
        int left = blockLeft + TAB_BAR_WIDTH + TAB_CONTENT_GAP;
        int right = left + contentWidth;
        int toggleX = right - 6 - TOGGLE_WIDTH;
        int buttonWidth = Math.min(150, (contentWidth - 10) / 2);
        int buttonY = this.height - 8 - 10 - INPUT_HEIGHT;
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

        int y = CONTENT_TOP_PADDING;
        int topSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        int topTitleRowY = y, topTitleInputY = topTitleRowY + ROW_HEIGHT;
        y = topTitleInputY + TITLE_INPUT_HEIGHT + ROW_GAP;
        int topContentRowY = y, topContentInputY = topContentRowY + ROW_HEIGHT;
        y = topContentInputY + MULTILINE_INPUT_HEIGHT + SECTION_GAP;
        int listSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        int betterPingRowY = y; y += ROW_HEIGHT + ROW_GAP;
        int onlineDurationRowY = y; y += ROW_HEIGHT + ROW_GAP;
        int titleRowY = y; y += ROW_HEIGHT + ROW_GAP;
        int healthDisplayRowY = y; y += ROW_HEIGHT + SECTION_GAP;
        int footerSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        int footerCustomRowY = y, footerCustomInputY = footerCustomRowY + ROW_HEIGHT;
        y = footerCustomInputY + MULTILINE_INPUT_HEIGHT + ROW_GAP;
        int footerRowY = y;

        int footerTotalWidth = contentWidth - 6;
        int footerColumnWidth = (footerTotalWidth - FOOTER_COLUMN_GAP * 2) / 3;
        int footerFirstColumnX  = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX  = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;
        int labelWidth = Math.max(80, contentWidth - 6 - TOGGLE_WIDTH - 8);

        int themeSectionHeaderY = CONTENT_TOP_PADDING;
        int themeSelectorY = themeSectionHeaderY + SECTION_HEADER_HEIGHT;
        int themeSelectorWidth = contentWidth - 6;
        int themeSelectorHeight = ThemeTabManager.themeSelectorHeight(com.poso.neotab.theme.TabThemeRegistry.ids().size());

        int customConfigBaseY = themeSelectorY + themeSelectorHeight + THEME_LIST_TOP_GAP;
        int customConfigHeight = 0;
        if ("custom".equals(theme.selectedThemeId) && theme.customThemeConfig != null) {
            customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;
            customConfigHeight += 15 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;
            customConfigHeight += 8 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;
            customConfigHeight += 8 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;
            int customButtonWidth = Math.max(60, (themeSelectorWidth - THEME_LIST_INSET * 2) / 2);
            int pickerX = left + THEME_LIST_INSET + customButtonWidth + 10;
            int maxRight = right - THEME_LIST_INSET;
            if (pickerX + 158 > maxRight) customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            customConfigHeight += 8;
            java.util.List<Integer> borderColors = theme.customThemeConfig.getBorderColors();
            if (borderColors != null) {
                customConfigHeight += borderColors.size() * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP);
                if (borderColors.size() < 7) customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
        }

        int healthSectionHeaderY = customConfigBaseY + customConfigHeight + SECTION_GAP;
        int healthModeRowY = healthSectionHeaderY + SECTION_HEADER_HEIGHT;
        int layoutSectionHeaderY = healthModeRowY + ROW_HEIGHT + SECTION_GAP;
        int layoutButtonsY = layoutSectionHeaderY + SECTION_HEADER_HEIGHT;

        int permTagAreaH = INPUT_HEIGHT;
        int permTargetListHeight = ROW_HEIGHT + permTagAreaH + 4;
        int permItemH = INPUT_HEIGHT + 2;
        int permRowCount = (15 + 1) / 2;
        int permissionsContentHeight = CONTENT_TOP_PADDING + (ROW_HEIGHT + ROW_GAP) + permTargetListHeight
            + SECTION_HEADER_HEIGHT + permRowCount * (permItemH + 2) + ROW_GAP + INPUT_HEIGHT + 4;

        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) contentHeight = footerRowY + ROW_HEIGHT;
        else if (activeTab == ConfigTab.PERMISSIONS) contentHeight = permissionsContentHeight;
        else contentHeight = layoutButtonsY + INPUT_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));

        int panelX = tabBarX - 2;
        int panelW = (right + 8 + SCROLL_TRACK_W + 4) - panelX;
        int panelCenterX = panelX + panelW / 2;

        return new NeoTabConfigScreenLayout.Layout(
            contentWidth, left, right, toggleX, labelWidth,
            this.scrollOffset, viewportTop, viewportBottom, buttonBarTop,
            topSectionHeaderY, topTitleRowY, topTitleInputY, topContentRowY, topContentInputY,
            listSectionHeaderY, betterPingRowY, onlineDurationRowY, titleRowY, healthDisplayRowY,
            footerSectionHeaderY, footerCustomRowY, footerCustomInputY, footerRowY,
            footerFirstColumnX, footerSecondColumnX, footerThirdColumnX,
            footerFirstColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerSecondColumnX + footerColumnWidth - TOGGLE_WIDTH,
            footerThirdColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerColumnWidth, contentHeight, maxScroll, buttonWidth, buttonY,
            panelCenterX - buttonWidth - 5, panelCenterX + 5, tabBarX,
            themeSectionHeaderY, themeSelectorY, themeSelectorWidth, themeSelectorHeight,
            healthSectionHeaderY, healthModeRowY, layoutSectionHeaderY, layoutButtonsY,
            lb(Component.translatable("screen.neotab.top.title"),           left, viewportTop - scrollOffset + topTitleRowY,       labelWidth),
            lb(Component.translatable("screen.neotab.top.content"),         left, viewportTop - scrollOffset + topContentRowY,     labelWidth),
            lb(Component.translatable("screen.neotab.list.better_ping"),    left, viewportTop - scrollOffset + betterPingRowY,     labelWidth),
            lb(Component.translatable("screen.neotab.list.online_duration"),left, viewportTop - scrollOffset + onlineDurationRowY, labelWidth),
            lb(Component.translatable("screen.neotab.list.title"),          left, viewportTop - scrollOffset + titleRowY,          labelWidth),
            lb(Component.translatable("screen.neotab.list.health_display"), left, viewportTop - scrollOffset + healthDisplayRowY,  labelWidth),
            lb(Component.translatable("screen.neotab.footer.custom"),       left, viewportTop - scrollOffset + footerCustomRowY,   labelWidth),
            lb(Component.translatable("screen.neotab.footer.tps"),    footerFirstColumnX,  viewportTop - scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6),
            lb(Component.translatable("screen.neotab.footer.mspt"),   footerSecondColumnX, viewportTop - scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6),
            lb(Component.translatable("screen.neotab.footer.online"), footerThirdColumnX,  viewportTop - scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6),
            lb(Component.translatable("screen.neotab.theme.health_mode"), left, viewportTop - scrollOffset + healthModeRowY, labelWidth)
        );
    }

    private NeoTabConfigScreenLayout.LabelBounds lb(Component text, int x, int y, int maxWidth) {
        int w = Math.min(this.font.width(text), maxWidth);
        return new NeoTabConfigScreenLayout.LabelBounds(x, y, Math.max(w, 1), INPUT_HEIGHT);
    }

    // ── Scroll helpers ───────────────────────────────────────────────────────
    private void setScrollOffsetInternal(int nextOffset, NeoTabConfigScreenLayout.Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(nextOffset, layout.maxScroll()));
        applyWidgetLayout(layout);
    }

    private void clampScroll(NeoTabConfigScreenLayout.Layout layout) {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, layout.maxScroll()));
    }

    private boolean isInsideViewport(double mouseX, double mouseY, NeoTabConfigScreenLayout.Layout layout) {
        return mouseX >= layout.left() && mouseX <= layout.right() + 8 + SCROLL_TRACK_W
            && mouseY >= layout.viewportTop() && mouseY <= layout.viewportBottom();
    }

    private AbstractWidget hoveredScrollableWidget(double mouseX, double mouseY) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            if (r instanceof AbstractWidget w && w.visible && w.isMouseOver(mouseX, mouseY)) return w;
        }
        return null;
    }

    private NeoTabConfigScreenLayout.HoverTarget hoveredTarget(int mouseX, int mouseY, NeoTabConfigScreenLayout.Layout layout) {
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            if (layout.topTitleLabelBounds().contains(mouseX, mouseY))        return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.top.title.tooltip"));
            if (layout.topContentLabelBounds().contains(mouseX, mouseY))      return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.top.content.tooltip"));
            if (layout.betterPingLabelBounds().contains(mouseX, mouseY))      return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.list.better_ping.tooltip"));
            if (layout.onlineDurationLabelBounds().contains(mouseX, mouseY))  return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.list.online_duration.note"));
            if (layout.titleLabelBounds().contains(mouseX, mouseY))           return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.list.title.tooltip"));
            if (layout.healthDisplayLabelBounds().contains(mouseX, mouseY))   return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.list.health_display.tooltip"));
            if (layout.footerCustomLabelBounds().contains(mouseX, mouseY))    return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.footer.custom.tooltip"));
        } else if (activeTab == ConfigTab.THEME) {
            if (layout.healthModeLabelBounds().contains(mouseX, mouseY))      return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.theme.health_mode.tooltip"));
            if (pageConfig.layoutEnabledToggle != null && pageConfig.layoutEnabledToggle.isMouseOver(mouseX, mouseY)) return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.layout.enabled.tooltip"));
            if (pageConfig.layoutColumnsButton != null && pageConfig.layoutColumnsButton.isMouseOver(mouseX, mouseY)) return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.layout.columns.tooltip"));
            if (pageConfig.layoutRowsButton    != null && pageConfig.layoutRowsButton.isMouseOver(mouseX, mouseY))    return new NeoTabConfigScreenLayout.HoverTarget(Component.translatable("screen.neotab.layout.rows.tooltip"));
        }
        return null;
    }

    // ── save ─────────────────────────────────────────────────────────────────
    private void save() {
        if (screenMode == ScreenMode.PLAYER) {
            com.poso.neotab.config.PlayerTabConfig playerCfg = new com.poso.neotab.config.PlayerTabConfig(
                Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : java.util.UUID.randomUUID(),
                policy.allowTopTitleToggle()      ? pageConfig.topTitleEnabled.getValue()      : null,
                policy.allowTopTitleEdit()        ? pageConfig.topTitleInput.getValue()        : null,
                policy.allowTopContentToggle()    ? pageConfig.topContentEnabled.getValue()    : null,
                policy.allowTopContentEdit()      ? pageConfig.topContentInput.getValue()      : null,
                policy.allowPingDisplayToggle()   ? pageConfig.betterPingEnabled.getValue()    : null,
                policy.allowDurationToggle()      ? pageConfig.onlineDurationEnabled.getValue() : null,
                policy.allowTitleToggle()         ? pageConfig.titleEnabled.getValue()         : null,
                policy.allowHealthDisplayToggle() ? pageConfig.healthDisplayEnabled.getValue() : null,
                policy.allowHealthModeChange()    ? pageConfig.healthDisplayMode.getValue()    : null,
                policy.allowFooterCustomEdit()    ? pageConfig.footerCustomInput.getValue()    : null,
                policy.allowFooterTpsToggle()     ? pageConfig.footerTpsEnabled.getValue()     : null,
                policy.allowFooterMsptToggle()    ? pageConfig.footerMsptEnabled.getValue()    : null,
                policy.allowFooterOnlineToggle()  ? pageConfig.footerOnlineEnabled.getValue()  : null,
                policy.allowThemeChange()         ? theme.selectedThemeId                      : null
            );
            PacketDistributor.sendToServer(new SavePlayerConfigPayload(playerCfg));
        } else {
            TabConfig config = new TabConfig(
                pageConfig.topTitleEnabled.getValue(),
                pageConfig.topTitleInput.getValue(),
                pageConfig.topContentEnabled.getValue(),
                pageConfig.topContentInput.getValue(),
                pageConfig.betterPingEnabled.getValue(),
                pageConfig.onlineDurationEnabled.getValue(),
                pageConfig.titleEnabled.getValue(),
                pageConfig.healthDisplayEnabled.getValue(),
                pageConfig.healthDisplayMode.getValue(),
                theme.selectedThemeId,
                pageConfig.footerCustomInput.getValue(),
                pageConfig.footerTpsEnabled.getValue(),
                pageConfig.footerMsptEnabled.getValue(),
                pageConfig.footerOnlineEnabled.getValue(),
                initialConfig.refreshIntervalTicks(),
                permissions.buildGlobalPolicyFromToggles(initialConfig),
                permissions.buildPlayerPoliciesFromToggles(initialConfig)
            ).sanitized();
            PacketDistributor.sendToServer(new SaveConfigPayload(config));
        }
        onClose();
    }
}

