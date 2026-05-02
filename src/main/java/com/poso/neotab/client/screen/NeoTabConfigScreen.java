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
    private static final int VIEWPORT_BOTTOM_MARGIN  = 0;  // 内容区域直接贴近按钮栏顶部，与HTML原型一致
    private static final int SCROLL_STEP             = 18;
    private static final int CONTENT_TOP_PADDING     = 8;
    private static final int TAB_BAR_WIDTH           = 84;  // HTML中Tab栏宽度
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
        // permTargetModeButton 已在新设计中移除
        if (permissions.playerSearchBox != null) permissions.playerSearchBox.visible = perms;
        if (permissions.permAddButton != null) permissions.permAddButton.visible = perms;
        for (Button btn : permissions.targetPlayerRemoveButtons) btn.visible = perms;
        if (permissions.permSaveButton != null) permissions.permSaveButton.visible = false;  // 旧的保存按钮已废弃
        if (permissions.applyToAllButton != null) permissions.applyToAllButton.visible = perms;  // 应用到全部玩家按钮
        if (permissions.applyToAddedButton != null) permissions.applyToAddedButton.visible = perms;  // 应用到已添加玩家按钮
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
        
        // 检查是否在下拉菜单上滚动
        if (activeTab == ConfigTab.PERMISSIONS && permissions.playerSearchBox != null 
                && permissions.playerSearchBox.isFocused() && !permissions.playerSuggestions.isEmpty()) {
            int dropX = permissions.playerSearchBox.getX();
            int dropY = permissions.playerSearchBox.getY() + INPUT_HEIGHT + 1;
            int dropW = permissions.playerSearchBox.getWidth();
            int maxVisibleItems = 8;
            int visibleItems = Math.min(permissions.playerSuggestions.size(), maxVisibleItems);
            int itemH = INPUT_HEIGHT - 2;
            int totalH = visibleItems * itemH + 2;
            
            if (mouseX >= dropX && mouseX < dropX + dropW && mouseY >= dropY && mouseY < dropY + totalH) {
                // 在下拉菜单上滚动
                int maxScroll = Math.max(0, (permissions.playerSuggestions.size() - maxVisibleItems) * itemH);
                permissions.dropdownScrollOffset = Math.max(0, Math.min(maxScroll, 
                    permissions.dropdownScrollOffset - (int) Math.round(scrollY * itemH)));
                return true;
            }
        }
        
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
        if (activeTab == ConfigTab.PERMISSIONS
                && permissions.playerSearchBox != null && permissions.playerSearchBox.isFocused()
                && !permissions.playerSuggestions.isEmpty() && button == 0) {
            int dropX = permissions.playerSearchBox.getX();
            int dropY = permissions.playerSearchBox.getY() + INPUT_HEIGHT + 1;
            int dropW = permissions.playerSearchBox.getWidth();
            int itemH = INPUT_HEIGHT - 2;
            int maxVisibleItems = 8;
            int visibleItems = Math.min(permissions.playerSuggestions.size(), maxVisibleItems);
            int totalH = visibleItems * itemH + 2;
            boolean needsScrollbar = permissions.playerSuggestions.size() > maxVisibleItems;
            
            // 检查是否点击在下拉菜单区域
            if (mouseX >= dropX && mouseX < dropX + dropW && mouseY >= dropY && mouseY < dropY + totalH) {
                // 检查是否点击在滚动条上
                if (needsScrollbar && mouseX >= dropX + dropW - 8) {
                    // 点击在滚动条区域，不处理项目选择
                    return true;
                }
                
                // 点击在项目上，考虑滚动偏移
                int scrollOffset = permissions.dropdownScrollOffset;
                for (int i = 0; i < permissions.playerSuggestions.size(); i++) {
                    int itemY = dropY + 1 + i * itemH - scrollOffset;
                    if (itemY + itemH < dropY || itemY > dropY + totalH) continue;
                    
                    if (mouseY >= itemY && mouseY < itemY + itemH) {
                        permissions.playerSearchBox.setValue(permissions.playerSuggestions.get(i));
                        permissions.playerSuggestions.clear();
                        permissions.dropdownScrollOffset = 0;  // 重置滚动
                        return true;
                    }
                }
                return true;  // 点击在下拉菜单内但不在任何项目上
            }
        }
        
        // Permissions tab buttons - 显式处理权限配置页面的所有按钮
        if (activeTab == ConfigTab.PERMISSIONS && button == 0) {
            // 全局策略开关按钮
            for (CycleButton<Boolean> toggle : permissions.globalPolicyToggles) {
                if (toggle.visible && toggle.isMouseOver(mouseX, mouseY)) {
                    return toggle.mouseClicked(mouseX, mouseY, button);
                }
            }
            
            // 添加按钮
            if (permissions.permAddButton != null && permissions.permAddButton.visible 
                    && permissions.permAddButton.isMouseOver(mouseX, mouseY)) {
                return permissions.permAddButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 删除按钮
            for (Button removeBtn : permissions.targetPlayerRemoveButtons) {
                if (removeBtn.visible && removeBtn.isMouseOver(mouseX, mouseY)) {
                    return removeBtn.mouseClicked(mouseX, mouseY, button);
                }
            }
            
            // 应用到全部玩家按钮
            if (permissions.applyToAllButton != null && permissions.applyToAllButton.visible 
                    && permissions.applyToAllButton.isMouseOver(mouseX, mouseY)) {
                return permissions.applyToAllButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 应用到已添加玩家按钮
            if (permissions.applyToAddedButton != null && permissions.applyToAddedButton.visible 
                    && permissions.applyToAddedButton.isMouseOver(mouseX, mouseY)) {
                return permissions.applyToAddedButton.mouseClicked(mouseX, mouseY, button);
            }
        }
        // Permissions player name click (暂时禁用，新设计中不需要)
        // if (activeTab == ConfigTab.PERMISSIONS && button == 0) {
        //     NeoTabConfigScreenLayout.Layout permLayout = buildLayoutImpl();
        //     int permY = CONTENT_TOP_PADDING + ROW_HEIGHT + ROW_GAP;
        //     if (!permissions.targetPlayers.isEmpty()) {
        //         permY += ROW_HEIGHT;
        //         for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
        //             int rowY = permLayout.toScreenY(permY);
        //             int nameX = permLayout.left() + 22;
        //             int nameW = permLayout.right() - nameX - 6;
        //             if (mouseX >= nameX && mouseX < nameX + nameW && mouseY >= rowY && mouseY < rowY + INPUT_HEIGHT) {
        //                 permissions.editingPlayerUUID = entry.getKey();
        //                 permissions.loadPolicyToggles(initialConfig);
        //                 return true;
        //             }
        //             permY += ROW_HEIGHT + 2;
        //         }
        //     }
        // }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;
        // Tab bar click
        if (button == 0 && mouseY >= VIEWPORT_TOP) {
            int tabBarBgX = layout.tabBarX() + 1;  // Tab栏背景起始位置
            int tabBtnX = tabBarBgX + TAB_BUTTON_LEFT_PADDING;
            int tabBtnW = (TAB_BAR_WIDTH - 1) - TAB_BUTTON_LEFT_PADDING - 1 - 4;  // 背景宽度 - 左padding - 分隔线 - 右padding
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

        // 标题栏底部分隔线（在标题下方，分隔标题栏和主体区域）
        int titleBarBottom = panelY + 24;  // 标题栏高度约24px
        AEStyleRenderer.drawTitleBarDivider(g, panelX + 3, titleBarBottom, panelW - 6);

        NeoTabConfigScreenRenderer.renderTabBar(g, this.font, this.activeTab, layout, panelY, mouseX, mouseY, this.screenMode);
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
        if (permissions.playerSearchBox == null) return;
        
        boolean showingDropdown = permissions.playerSearchBox.isFocused() && !permissions.playerSuggestions.isEmpty();
        
        // 当显示下拉框时，临时隐藏删除按钮以防止穿透
        if (showingDropdown) {
            for (Button btn : permissions.targetPlayerRemoveButtons) {
                btn.visible = false;
            }
        }
        
        if (!showingDropdown) {
            // 恢复删除按钮的可见性
            for (Button btn : permissions.targetPlayerRemoveButtons) {
                btn.visible = true;
            }
            return;
        }
        
        int dropX = permissions.playerSearchBox.getX();
        int dropY = permissions.playerSearchBox.getY() + INPUT_HEIGHT + 1;
        int dropW = permissions.playerSearchBox.getWidth();
        int itemH = INPUT_HEIGHT - 2;
        
        // 限制下拉菜单最大高度（最多显示8个项目）
        int maxVisibleItems = 8;
        int visibleItems = Math.min(permissions.playerSuggestions.size(), maxVisibleItems);
        int totalH = visibleItems * itemH + 2;
        boolean needsScrollbar = permissions.playerSuggestions.size() > maxVisibleItems;
        
        // 绘制下拉菜单背景和边框
        g.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + totalH + 1, AEStyleRenderer.COLOR_OUTLINE);
        g.fill(dropX, dropY, dropX + dropW, dropY + totalH, 0xFF2A2A2A);
        
        // 启用裁剪区域
        g.enableScissor(dropX, dropY, dropX + dropW, dropY + totalH);
        
        // 使用滚动偏移
        int scrollOffset = permissions.dropdownScrollOffset;
        
        // 绘制可见的项目
        for (int i = 0; i < permissions.playerSuggestions.size(); i++) {
            int itemY = dropY + 1 + i * itemH - scrollOffset;
            
            // 跳过不可见的项目
            if (itemY + itemH < dropY || itemY > dropY + totalH) continue;
            
            boolean hovered = mouseX >= dropX && mouseX < dropX + dropW - (needsScrollbar ? 8 : 0) 
                && mouseY >= itemY && mouseY < itemY + itemH && mouseY >= dropY && mouseY < dropY + totalH;
            if (hovered) g.fill(dropX, itemY, dropX + dropW - (needsScrollbar ? 8 : 0), itemY + itemH, 0xFF334466);
            g.drawString(this.font, permissions.playerSuggestions.get(i),
                dropX + 4, itemY + (itemH - this.font.lineHeight) / 2,
                hovered ? 0xFF55FF55 : AEStyleRenderer.COLOR_LABEL, false);
        }
        
        g.disableScissor();
        
        // 绘制滚动条（如果需要）
        if (needsScrollbar) {
            int scrollbarX = dropX + dropW - 6;
            int scrollbarW = 4;
            int scrollbarH = totalH - 4;
            int maxScroll = (permissions.playerSuggestions.size() - maxVisibleItems) * itemH;
            int thumbH = Math.max(20, scrollbarH * visibleItems / permissions.playerSuggestions.size());
            int thumbY = scrollbarH > thumbH ? (int)((float)scrollOffset / maxScroll * (scrollbarH - thumbH)) : 0;
            
            // 滚动条轨道
            g.fill(scrollbarX, dropY + 2, scrollbarX + scrollbarW, dropY + totalH - 2, 0x60303030);
            // 滚动条滑块
            g.fill(scrollbarX, dropY + 2 + thumbY, scrollbarX + scrollbarW, dropY + 2 + thumbY + thumbH, 0xB0FFFFFF);
        }
    }

    private void renderScrollableContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, NeoTabConfigScreenLayout.Layout layout) {
        // 内容区域背景：从Tab栏右边界到主面板右边界，从标题栏分隔线下方到按钮栏顶部
        int tabBarX = layout.tabBarX();
        int panelY = 8;  // 主面板顶部Y坐标
        int titleBarHeight = 25;  // 标题栏高度（包括分隔线）
        
        int contentAreaX = tabBarX + TAB_BAR_WIDTH;  // 从Tab栏右边界开始
        int contentAreaY = panelY + titleBarHeight;  // 从标题栏分隔线下方开始（与Tab栏同高）
        
        // 计算主面板的右边界
        int scrollTrackW = SCROLL_TRACK_W;
        int panelX = tabBarX - 2;
        int panelRightInner = panelX + ((layout.right() + 8 + scrollTrackW + 4) - panelX) - 3;  // 主面板右边界内侧（减去边框）
        
        int contentAreaW = panelRightInner - contentAreaX;  // 从Tab栏右边界到主面板右边界
        int contentAreaH = layout.buttonBarTop() - contentAreaY;  // 从标题栏分隔线下方到按钮栏顶部
        AEStyleRenderer.drawContentArea(g, contentAreaX, contentAreaY, contentAreaW, contentAreaH);
        g.enableScissor(layout.scissorLeft(), layout.viewportTop(), layout.scissorRight(), layout.viewportBottom());

        if (activeTab == ConfigTab.PAGE_CONFIG) {
            int CARD_PADDING = 10;
            
            // 顶部信息分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.top"),
                    layout.left(), layout.toScreenY(layout.topSectionHeaderY()), layout.right());
            
            // 标题信息卡片
            int cardY = layout.toScreenY(layout.topTitleRowY());
            int titleLineHeight = this.font.lineHeight;
            int subtitleLineHeight = this.font.lineHeight;
            int topTitleCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + TITLE_INPUT_HEIGHT + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), topTitleCardHeight);
            
            // 绘制标题和开关（同一行）
            g.drawString(this.font, Component.translatable("screen.neotab.top.title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING, 
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            // 开关按钮由widget系统绘制，位置已在applyLayout中设置
            
            // 绘制副标题（缩小字体）
            drawScaledText(g, Component.translatable("screen.neotab.top.title.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            // 输入框由widget系统绘制
            
            // 内容信息卡片
            cardY = layout.toScreenY(layout.topContentRowY());
            int topContentCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + MULTILINE_INPUT_HEIGHT + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), topContentCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.top.content"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.top.content.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 玩家列表分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.list"),
                    layout.left(), layout.toScreenY(layout.listSectionHeaderY()), layout.right());
            
            // 更好的延迟显示卡片
            cardY = layout.toScreenY(layout.betterPingRowY());
            int simpleCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.list.better_ping"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.list.better_ping.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 在线时长显示卡片
            cardY = layout.toScreenY(layout.onlineDurationRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.list.online_duration"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.list.online_duration.note"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 称号功能卡片（无副标题）
            cardY = layout.toScreenY(layout.titleRowY());
            int noSubtitleCardHeight = CARD_PADDING + titleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), noSubtitleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.list.title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 玩家血量卡片
            cardY = layout.toScreenY(layout.healthDisplayRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.list.health_display"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.list.health_display.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 底部信息分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.section.footer"),
                    layout.left(), layout.toScreenY(layout.footerSectionHeaderY()), layout.right());
            
            // 自定义信息卡片（无开关）
            cardY = layout.toScreenY(layout.footerCustomRowY());
            int footerCustomCardHeight = CARD_PADDING + titleLineHeight + 8 + MULTILINE_INPUT_HEIGHT + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), footerCustomCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.custom"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // TPS 信息卡片
            cardY = layout.toScreenY(layout.footerTpsRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.tps"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.literal("显示服务器 TPS（每秒刻数）"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // MSPT 信息卡片
            cardY = layout.toScreenY(layout.footerMsptRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.mspt"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.literal("显示服务器 MSPT（每刻毫秒数）"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 在线人数卡片
            cardY = layout.toScreenY(layout.footerOnlineRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.online"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.literal("显示当前在线玩家数量"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        } else if (activeTab == ConfigTab.THEME) {
            int CARD_PADDING = 10;
            
            // TAB 主题分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.theme.tab_theme"),
                    layout.left(), layout.toScreenY(layout.themeSectionHeaderY()), layout.right());
            
            // 预设主题卡片
            int cardY = layout.toScreenY(layout.themeSelectorY());
            int titleLineHeight = this.font.lineHeight;
            int subtitleLineHeight = this.font.lineHeight;
            
            // 计算主题选择器卡片高度：padding + 标题 + 副标题间距 + 副标题 + 内容间距 + 主题列表 + padding
            int themeListHeight = theme.themeOptionButtons.size() * THEME_OPTION_HEIGHT + 
                                  Math.max(0, theme.themeOptionButtons.size() - 1) * THEME_OPTION_GAP;
            int themeCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + themeListHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), themeCardHeight);
            
            // 标题文字（限制宽度，避免覆盖按钮）
            int titleMaxWidth = layout.contentWidth() - CARD_PADDING * 2;
            Component titleText = Component.literal("预设主题");
            g.drawString(this.font, titleText,
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题文字（限制宽度，自动换行）
            drawScaledText(g, Component.literal("快速应用一套完整的视觉风格"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            // 主题选择器按钮由widget系统绘制
            
            // 如果选中了"自定义"主题，显示自定义配置卡片
            if ("custom".equals(theme.selectedThemeId)) {
                // 重置为默认卡片（放在最上面）
                cardY = layout.toScreenY(layout.customResetRowY());
                int resetCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), resetCardHeight);
                
                // 标题（限制宽度）
                int resetTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 56 - 8;  // 减去按钮宽度和间距
                g.drawString(this.font, Component.literal("重置默认"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                drawScaledText(g, Component.literal("将所有自定义设置恢复为默认值"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                
                // 动画效果卡片（独立卡片）
                cardY = layout.toScreenY(layout.customAnimationRowY());
                int animCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + THEME_OPTION_HEIGHT + CARD_PADDING;
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), animCardHeight);
                
                // 标题（限制宽度，避免覆盖右侧按钮）
                int animTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2;
                g.drawString(this.font, Component.literal("动画效果"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                
                // 副标题（限制宽度，自动换行）
                drawWrappedScaledText(g, "配置边框动画的开关和速度",
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                        animTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                // 动画开关和速度按钮由widget系统绘制
                
                // 颜色配置卡片（左右分栏：左侧按钮，右侧颜色选择器）
                cardY = layout.toScreenY(layout.customBgColorRowY());
                
                // 计算左侧按钮列表高度
                java.util.List<Integer> borderColors = theme.customThemeConfig != null ? 
                    theme.customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
                
                int leftColumnHeight = CARD_PADDING + 
                    titleLineHeight + 2 + subtitleLineHeight + 8 +  // 卡片标题+副标题
                    THEME_OPTION_HEIGHT + THEME_OPTION_GAP +  // 背景颜色
                    THEME_OPTION_HEIGHT + THEME_OPTION_GAP +  // 外层边框颜色
                    titleLineHeight + 4;  // 边框颜色标题
                
                leftColumnHeight += borderColors.size() * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP);
                if (borderColors.size() < 7) {
                    leftColumnHeight += THEME_OPTION_HEIGHT;  // 添加按钮（最后一个不需要GAP）
                }
                leftColumnHeight += CARD_PADDING;
                
                // 右侧颜色选择器高度
                int colorPickerHeight = theme.embeddedColorPicker != null ? 
                    (int)(theme.embeddedColorPicker.getHeight() * 1.2f) : 200;
                
                int customConfigCardHeight = Math.max(leftColumnHeight, colorPickerHeight + CARD_PADDING * 2);
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), customConfigCardHeight);
                
                // 绘制卡片标题和副标题
                g.drawString(this.font, Component.literal("颜色配置"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                drawScaledText(g, Component.literal("左侧选择要配置的项目，右侧使用颜色选择器调整颜色"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                
                // 左侧按钮区域和右侧颜色选择器由widget系统绘制
                // 在左侧绘制"边框颜色"小标题
                int borderColorTitleY = cardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 +
                    THEME_OPTION_HEIGHT + THEME_OPTION_GAP +  // 背景颜色
                    THEME_OPTION_HEIGHT + THEME_OPTION_GAP;   // 外层边框颜色
                g.drawString(this.font, Component.literal("边框颜色"),
                        layout.left() + CARD_PADDING, borderColorTitleY,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
            }
            
            // 血量显示分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.literal("血量显示"),
                    layout.left(), layout.toScreenY(layout.healthSectionHeaderY()), layout.right());
            
            // 显示效果卡片
            cardY = layout.toScreenY(layout.healthModeRowY());
            int healthCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), healthCardHeight);
            
            // 标题（限制宽度）
            int healthTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 80 - 8;  // 减去按钮宽度
            g.drawString(this.font, Component.literal("显示效果"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题（自动换行）
            drawWrappedScaledText(g, "完整：最多显示 10 颗心 | 单独：只显示 1 颗心 + 数字",
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    healthTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            // 血量模式按钮由widget系统绘制
            
            // 布局分列分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.literal("布局分列"),
                    layout.left(), layout.toScreenY(layout.layoutSectionHeaderY()), layout.right());
            
            // 启用分列卡片
            cardY = layout.toScreenY(layout.layoutEnabledRowY());
            int layoutEnabledCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutEnabledCardHeight);
            
            // 标题（限制宽度）
            int layoutTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 56 - 8;
            g.drawString(this.font, Component.literal("启用分列"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题
            drawWrappedScaledText(g, "开启后将 TAB 列表显示为多列布局，可自定义列数和行数",
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 展示列数卡片
            cardY = layout.toScreenY(layout.layoutColumnsRowY());
            int layoutColumnsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutColumnsCardHeight);
            
            g.drawString(this.font, Component.literal("展示列数"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            drawWrappedScaledText(g, "设置 TAB 列表显示的列数，最大值根据屏幕宽度自动调整",
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 展示行数卡片
            cardY = layout.toScreenY(layout.layoutRowsRowY());
            int layoutRowsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutRowsCardHeight);
            
            g.drawString(this.font, Component.literal("展示行数"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            drawWrappedScaledText(g, "设置 TAB 列表显示的行数，当玩家数超过容量时自动分页",
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 提示卡片（无按钮）
            cardY = layout.toScreenY(layout.layoutHintRowY());
            // 多行副标题需要计算高度
            String hintText = "当玩家数超过容量时，TAB 列表将自动分页显示。\n快捷键：TAB + 左/右箭头键翻页。";
            int hintLines = 2;  // 两行文字
            int layoutHintCardHeight = CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * hintLines * 0.82f) + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutHintCardHeight);
            
            g.drawString(this.font, Component.literal("提示"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            // 绘制多行副标题
            drawScaledText(g, Component.literal("当玩家数超过容量时，TAB 列表将自动分页显示。"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            drawScaledText(g, Component.literal("快捷键：TAB + 左/右箭头键翻页。"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 0.82f),
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        } else if (activeTab == ConfigTab.PERMISSIONS) {
            renderPermissionsContent(g, mouseX, mouseY, layout);
        }
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        NeoTabConfigScreenRenderer.renderScrollbar(g, layout, this.scrollOffset);
    }

    private void renderPermissionsContent(GuiGraphics g, int mouseX, int mouseY, NeoTabConfigScreenLayout.Layout layout) {
        int CARD_PADDING = 10;
        int CARD_GAP = 8;
        int titleLineHeight = this.font.lineHeight;
        int subtitleLineHeight = this.font.lineHeight;
        
        int y = CONTENT_TOP_PADDING;
        
        // 全局策略分区标题
        AEStyleRenderer.drawSectionHeader(g, this.font, Component.literal("全局策略（所有玩家）"),
                layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;
        
        // 全局策略权限卡片（2列网格布局）
        String[] policyKeys = {
            "顶部标题 开关", "顶部标题 内容",
            "顶部内容 开关", "顶部内容 文字",
            "延迟显示 开关", "在线时长 开关",
            "称号功能 开关", "血量显示 开关",
            "血量显示 模式", "底部自定义文字",
            "底部 TPS 开关", "底部 MSPT 开关",
            "底部在线人数 开关", "主题切换"
        };
        
        String[] policySubtitles = {
            "允许玩家切换顶部标题的显示", "允许玩家自定义顶部标题内容",
            "允许玩家切换顶部内容的显示", "允许玩家自定义顶部内容文字",
            "允许玩家切换延迟显示功能", "允许玩家切换在线时长显示",
            "允许玩家切换称号功能", "允许玩家切换血量显示功能",
            "允许玩家切换血量显示模式", "允许玩家自定义底部文字内容",
            "允许玩家切换 TPS 信息显示", "允许玩家切换 MSPT 信息显示",
            "允许玩家切换在线人数显示", "允许玩家切换 TAB 主题"
        };
        
        int cardWidth = (layout.contentWidth() - CARD_GAP) / 2;  // 两列，中间8px间距
        int cardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
        
        for (int i = 0; i < Math.min(policyKeys.length, permissions.globalPolicyToggles.size()); i++) {
            int col = i % 2;
            int row = i / 2;
            
            if (col == 0 && i > 0) {
                y += cardHeight + CARD_GAP;
            }
            
            int cardX = layout.left() + col * (cardWidth + CARD_GAP);
            int cardY = layout.toScreenY(y);
            
            // 绘制卡片背景
            AEStyleRenderer.drawConfigModuleCard(g, cardX, cardY, cardWidth, cardHeight);
            
            // 绘制标题（限制宽度，避免覆盖开关）
            int titleMaxWidth = cardWidth - CARD_PADDING * 2 - 56 - 8;
            g.drawString(this.font, policyKeys[i],
                    cardX + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 绘制副标题
            drawWrappedScaledText(g, policySubtitles[i],
                    cardX + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    titleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 开关按钮由widget系统绘制
        }
        
        // 移动到下一行
        if (policyKeys.length % 2 == 1) {
            y += cardHeight + SECTION_GAP;
        } else {
            y += cardHeight + SECTION_GAP;
        }
        
        // 指定玩家策略分区标题
        AEStyleRenderer.drawSectionHeader(g, this.font, Component.literal("指定玩家策略"),
                layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;
        
        // 添加玩家卡片
        int addPlayerCardY = layout.toScreenY(y);
        int addPlayerCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + INPUT_HEIGHT + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), addPlayerCardY, layout.contentWidth(), addPlayerCardHeight);
        
        g.drawString(this.font, Component.literal("添加玩家"),
                layout.left() + CARD_PADDING, addPlayerCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.literal("为特定玩家设置独立的自定义权限"),
                layout.left() + CARD_PADDING, addPlayerCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        // 输入框和添加按钮由widget系统绘制
        
        y += addPlayerCardHeight + CARD_GAP;
        
        // 玩家列表显示卡片
        int playerListCardY = layout.toScreenY(y);
        int playerListContentHeight;
        if (permissions.targetPlayers.isEmpty()) {
            playerListContentHeight = 30;  // 降低空状态高度
        } else {
            // 计算标签布局的实际高度（与applyLayout保持一致）
            int tagHeight = INPUT_HEIGHT;
            int tagGap = 6;
            int maxWidth = layout.contentWidth() - CARD_PADDING * 2;
            int tagX = 0;
            int tagY = 0;
            
            for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = this.font.width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                
                // 检查是否需要换行
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                
                tagX += tagWidth + tagGap;
            }
            playerListContentHeight = tagY + tagHeight;
        }
        int playerListCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + playerListContentHeight + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), playerListCardY, layout.contentWidth(), playerListCardHeight);
        
        g.drawString(this.font, Component.literal("玩家列表显示"),
                layout.left() + CARD_PADDING, playerListCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.literal("已添加的玩家及其权限配置"),
                layout.left() + CARD_PADDING, playerListCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        
        // 玩家列表内容区域
        int listContentY = playerListCardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
        if (permissions.targetPlayers.isEmpty()) {
            // 空状态提示（居中显示）
            drawScaledText(g, Component.literal("暂无玩家，添加后将在此处显示"),
                    layout.left() + CARD_PADDING, listContentY + 5,
                    0xFF5A6C7E, 0.82f);
        } else {
            // 显示玩家列表（标签样式，自动换行）
            int tagX = 0;  // 相对于内容区域的X坐标
            int tagY = 0;  // 相对于内容区域的Y坐标
            int tagGap = 6;  // 标签之间的间距
            int tagHeight = INPUT_HEIGHT;
            int maxWidth = layout.contentWidth() - CARD_PADDING * 2;
            
            for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = this.font.width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                
                // 检查是否需要换行
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                
                // 计算绝对坐标
                int absTagX = layout.left() + CARD_PADDING + tagX;
                int absTagY = listContentY + tagY;
                
                // 绘制标签背景（圆角矩形）
                int tagBgColor = 0xFF9AABC0;  // 使用边框颜色作为标签背景
                g.fill(absTagX, absTagY, absTagX + tagWidth, absTagY + tagHeight, tagBgColor);
                
                // 绘制玩家名称
                g.drawString(this.font, playerName,
                        absTagX + tagPadding, absTagY + (tagHeight - this.font.lineHeight) / 2,
                        0xFFFFFFFF, false);  // 白色文字
                
                // 删除按钮由widget系统绘制（会覆盖在标签上，右对齐）
                
                tagX += tagWidth + tagGap;
            }
        }
        
        y += playerListCardHeight + CARD_GAP;
        
        // 提示卡片
        int hintCardY = layout.toScreenY(y);
        int hintCardHeight = CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 2 * 0.82f) + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), hintCardY, layout.contentWidth(), hintCardHeight);
        
        g.drawString(this.font, Component.literal("提示"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.literal("添加玩家后，可在上方权限列表中单独配置。"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        drawScaledText(g, Component.literal("玩家权限优先级高于全局策略。"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 0.82f),
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        
        y += hintCardHeight + 16;
        
        // 应用权限设置卡片
        int applySettingsCardY = layout.toScreenY(y);
        int applySettingsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + INPUT_HEIGHT + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), applySettingsCardY, layout.contentWidth(), applySettingsCardHeight);
        
        g.drawString(this.font, Component.literal("应用权限设置"),
                layout.left() + CARD_PADDING, applySettingsCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.literal("点击下方按钮应用当前权限配置"),
                layout.left() + CARD_PADDING, applySettingsCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        // 两个应用按钮由widget系统绘制
        
        y += applySettingsCardHeight + 16;
        
        // 保存按钮（由widget系统绘制，这里不需要绘制卡片）
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            
            // 检查widget的visible属性
            if (r instanceof AbstractWidget widget && !widget.visible) continue;
            
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

        // 卡片布局常量
        int CARD_PADDING = 10;  // 卡片内边距
        int CARD_GAP = 8;  // 卡片之间的间距（恢复为HTML中的8px）
        int TITLE_LINE_HEIGHT = font.lineHeight;  // 标题行高度（约9px）
        int SUBTITLE_LINE_HEIGHT = font.lineHeight;  // 副标题行高度（约9px）
        
        int y = CONTENT_TOP_PADDING;
        int topSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        
        // 标题信息卡片：padding + 标题 + 副标题间距 + 副标题 + 内容间距 + 输入框 + padding
        int topTitleRowY = y;
        int topTitleInputY = topTitleRowY + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
        int topTitleCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + TITLE_INPUT_HEIGHT + CARD_PADDING;
        y = topTitleRowY + topTitleCardHeight + CARD_GAP;
        
        // 内容信息卡片
        int topContentRowY = y;
        int topContentInputY = topContentRowY + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
        int topContentCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + MULTILINE_INPUT_HEIGHT + CARD_PADDING;
        y = topContentRowY + topContentCardHeight + SECTION_GAP;
        
        // 玩家列表分区
        int listSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        
        // 更好的延迟卡片（无输入框）：padding + 标题 + 副标题间距 + 副标题 + padding
        int betterPingRowY = y;
        int betterPingCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = betterPingRowY + betterPingCardHeight + CARD_GAP;
        
        // 在线时长卡片
        int onlineDurationRowY = y;
        int onlineDurationCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = onlineDurationRowY + onlineDurationCardHeight + CARD_GAP;
        
        // 称号功能卡片（无副标题）：padding + 标题 + padding
        int titleRowY = y;
        int titleCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + CARD_PADDING;
        y = titleRowY + titleCardHeight + CARD_GAP;
        
        // 玩家血量卡片
        int healthDisplayRowY = y;
        int healthDisplayCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = healthDisplayRowY + healthDisplayCardHeight + SECTION_GAP;
        
        // 底部信息分区
        int footerSectionHeaderY = y; y += SECTION_HEADER_HEIGHT;
        
        // 自定义信息卡片（无副标题）：padding + 标题 + 内容间距 + 输入框 + padding
        int footerCustomRowY = y;
        int footerCustomInputY = footerCustomRowY + CARD_PADDING + TITLE_LINE_HEIGHT + 8;
        int footerCustomCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 8 + MULTILINE_INPUT_HEIGHT + CARD_PADDING;
        y = footerCustomRowY + footerCustomCardHeight + CARD_GAP;
        
        // TPS 信息卡片
        int footerTpsRowY = y;
        int footerTpsCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = footerTpsRowY + footerTpsCardHeight + CARD_GAP;
        
        // MSPT 信息卡片
        int footerMsptRowY = y;
        int footerMsptCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = footerMsptRowY + footerMsptCardHeight + CARD_GAP;
        
        // 在线人数卡片
        int footerOnlineRowY = y;
        int footerOnlineCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        y = footerOnlineRowY + footerOnlineCardHeight;
        
        // 底部三列开关（已废弃，改为卡片）
        int footerRowY = y;

        int footerTotalWidth = contentWidth - 6;
        int footerColumnWidth = (footerTotalWidth - FOOTER_COLUMN_GAP * 2) / 3;
        int footerFirstColumnX  = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX  = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;
        int labelWidth = Math.max(80, contentWidth - 6 - TOGGLE_WIDTH - 8);

        // 主题样式Tab布局（卡片式）
        int themeY = CONTENT_TOP_PADDING;
        
        int themeSectionHeaderY = themeY;
        themeY += SECTION_HEADER_HEIGHT;
        
        // 预设主题卡片
        int themeSelectorY = themeY;
        int themeListHeight = com.poso.neotab.theme.TabThemeRegistry.ids().size() * THEME_OPTION_HEIGHT + 
                              Math.max(0, com.poso.neotab.theme.TabThemeRegistry.ids().size() - 1) * THEME_OPTION_GAP;
        int themeSelectorCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + themeListHeight + CARD_PADDING;
        themeY = themeSelectorY + themeSelectorCardHeight + CARD_GAP;
        
        // 自定义主题配置卡片（仅在选中"custom"时显示）
        int customResetRowY = themeY;
        int customAnimationRowY = themeY;
        int customAnimSpeedRowY = themeY;
        int customBgColorRowY = themeY;
        int customOuterBorderRowY = themeY;
        int customAddBorderColorRowY = themeY;
        
        if ("custom".equals(theme.selectedThemeId)) {
            // 重置为默认卡片（放在最上面）
            customResetRowY = themeY;
            int resetCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
            themeY = customResetRowY + resetCardHeight + CARD_GAP;
            
            // 动画效果卡片（独立卡片）
            customAnimationRowY = themeY;
            int animCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + THEME_OPTION_HEIGHT + CARD_PADDING;
            themeY = customAnimationRowY + animCardHeight + CARD_GAP;
            
            // 动画速度按钮与动画开关在同一行，所以Y坐标相同
            customAnimSpeedRowY = customAnimationRowY;
            
            // 颜色配置大卡片（包含所有颜色配置项和颜色选择器）
            customBgColorRowY = themeY;
            
            // 计算左侧按钮列表高度
            java.util.List<Integer> borderColors = theme.customThemeConfig != null ? 
                theme.customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
            
            int leftColumnHeight = CARD_PADDING + 
                TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 +  // 卡片标题+副标题
                THEME_OPTION_HEIGHT + THEME_OPTION_GAP +  // 背景颜色
                THEME_OPTION_HEIGHT + THEME_OPTION_GAP +  // 外层边框颜色
                TITLE_LINE_HEIGHT + 4;  // 边框颜色标题
            
            leftColumnHeight += borderColors.size() * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP);
            if (borderColors.size() < 7) {
                leftColumnHeight += THEME_OPTION_HEIGHT;  // 添加按钮（最后一个不需要GAP）
            }
            leftColumnHeight += CARD_PADDING;
            
            // 右侧颜色选择器高度
            int colorPickerHeight = theme.embeddedColorPicker != null ? 
                (int)(theme.embeddedColorPicker.getHeight() * 1.2f) : 200;
            
            int customConfigCardHeight = Math.max(leftColumnHeight, colorPickerHeight + CARD_PADDING * 2);
            themeY = customBgColorRowY + customConfigCardHeight + SECTION_GAP;
            
            // 保存其他行的Y坐标（相对于颜色配置卡片内部）
            customOuterBorderRowY = customBgColorRowY;
            customAddBorderColorRowY = customBgColorRowY;
        }
        
        // 血量显示分区
        int healthSectionHeaderY = themeY;
        themeY += SECTION_HEADER_HEIGHT;
        
        // 显示效果卡片
        int healthModeRowY = themeY;
        int healthModeCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        themeY = healthModeRowY + healthModeCardHeight + SECTION_GAP;
        
        // 布局分列分区
        int layoutSectionHeaderY = themeY;
        themeY += SECTION_HEADER_HEIGHT;
        
        // 启用分列卡片
        int layoutEnabledRowY = themeY;
        int layoutEnabledCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        themeY = layoutEnabledRowY + layoutEnabledCardHeight + CARD_GAP;
        
        // 展示列数卡片
        int layoutColumnsRowY = themeY;
        int layoutColumnsCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        themeY = layoutColumnsRowY + layoutColumnsCardHeight + CARD_GAP;
        
        // 展示行数卡片
        int layoutRowsRowY = themeY;
        int layoutRowsCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        themeY = layoutRowsRowY + layoutRowsCardHeight + CARD_GAP;
        
        // 提示卡片
        int layoutHintRowY = themeY;
        int hintLines = 2;
        int layoutHintCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + (int)(SUBTITLE_LINE_HEIGHT * hintLines * 0.82f) + CARD_PADDING;
        themeY = layoutHintRowY + layoutHintCardHeight;
        
        int themeSelectorWidth = contentWidth - 6;
        int themeSelectorHeight = themeSelectorCardHeight;  // 整个卡片的高度
        int layoutButtonsY = layoutEnabledRowY;  // 保持兼容性

        // 权限配置页面内容高度计算（卡片式布局）
        int permY = CONTENT_TOP_PADDING;
        
        // 全局策略分区标题
        permY += SECTION_HEADER_HEIGHT;
        
        // 全局策略权限卡片（2列网格布局，14个权限项）
        int permCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
        int permRowCount = (14 + 1) / 2;  // 向上取整，14个权限项分2列
        permY += permRowCount * (permCardHeight + CARD_GAP) - CARD_GAP;  // 最后一行不需要GAP
        permY += SECTION_GAP;
        
        // 指定玩家策略分区标题
        permY += SECTION_HEADER_HEIGHT;
        
        // 添加玩家卡片
        int addPlayerCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + INPUT_HEIGHT + CARD_PADDING;
        permY += addPlayerCardHeight + CARD_GAP;
        
        // 玩家列表显示卡片
        int playerListContentHeight;
        if (permissions.targetPlayers.isEmpty()) {
            playerListContentHeight = 30;  // 降低空状态高度
        } else {
            // 计算标签布局的实际高度（与applyLayout保持一致）
            int tagHeight = INPUT_HEIGHT;
            int tagGap = 6;
            int maxWidth = contentWidth - CARD_PADDING * 2;
            int tagX = 0;
            int tagY = 0;
            
            for (java.util.Map.Entry<java.util.UUID, String> entry : permissions.targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = this.font.width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                
                // 检查是否需要换行
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                
                tagX += tagWidth + tagGap;
            }
            playerListContentHeight = tagY + tagHeight;
        }
        int playerListCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + playerListContentHeight + CARD_PADDING;
        permY += playerListCardHeight + CARD_GAP;
        
        // 提示卡片
        int permHintCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + (int)(SUBTITLE_LINE_HEIGHT * 2 * 0.82f) + CARD_PADDING;
        permY += permHintCardHeight + 16;
        
        // 应用权限设置卡片
        int applySettingsCardHeight = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8 + INPUT_HEIGHT + CARD_PADDING;
        permY += applySettingsCardHeight + 16;
        
        // 保存按钮（已废弃）
        // permY += INPUT_HEIGHT + 8;
        
        int permissionsContentHeight = permY;

        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) contentHeight = footerRowY;  // 直接使用最后一个卡片的底部位置
        else if (activeTab == ConfigTab.PERMISSIONS) contentHeight = permissionsContentHeight;
        else contentHeight = themeY;  // 主题Tab使用themeY作为内容高度
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));

        int panelX = tabBarX - 2;
        int panelW = (right + 8 + SCROLL_TRACK_W + 4) - panelX;
        int panelCenterX = panelX + panelW / 2;

        return new NeoTabConfigScreenLayout.Layout(
            contentWidth, left, right, toggleX, labelWidth,
            this.scrollOffset, viewportTop, viewportBottom, buttonBarTop,
            topSectionHeaderY, topTitleRowY, topTitleInputY, topContentRowY, topContentInputY,
            listSectionHeaderY, betterPingRowY, onlineDurationRowY, titleRowY, healthDisplayRowY,
            footerSectionHeaderY, footerCustomRowY, footerCustomInputY, 
            footerTpsRowY, footerMsptRowY, footerOnlineRowY, footerRowY,
            footerFirstColumnX, footerSecondColumnX, footerThirdColumnX,
            footerFirstColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerSecondColumnX + footerColumnWidth - TOGGLE_WIDTH,
            footerThirdColumnX  + footerColumnWidth - TOGGLE_WIDTH,
            footerColumnWidth, contentHeight, maxScroll, buttonWidth, buttonY,
            panelCenterX - buttonWidth - 5, panelCenterX + 5, tabBarX,
            themeSectionHeaderY, themeSelectorY, themeSelectorWidth, themeSelectorHeight,
            customAnimationRowY, customAnimSpeedRowY, customBgColorRowY, 
            customOuterBorderRowY, customResetRowY, customAddBorderColorRowY,
            healthSectionHeaderY, healthModeRowY, layoutSectionHeaderY, layoutButtonsY,
            layoutEnabledRowY, layoutColumnsRowY, layoutRowsRowY, layoutHintRowY,
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

    /**
     * 绘制缩放文字（用于实现不同字体大小）。
     * 
     * @param g GuiGraphics
     * @param text 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param color 颜色
     * @param scale 缩放比例（1.0 = 正常大小，0.75 = 75%大小）
     */
    private void drawScaledText(GuiGraphics g, Component text, float x, float y, int color, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        pose.popPose();
    }

    /**
     * 绘制自动换行的缩放文字。
     * 
     * @param g GuiGraphics
     * @param text 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param maxWidth 最大宽度
     * @param color 颜色
     * @param scale 缩放比例
     */
    private void drawWrappedScaledText(GuiGraphics g, String text, float x, float y, int maxWidth, int color, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        
        // 计算缩放后的最大宽度
        int scaledMaxWidth = (int)(maxWidth / scale);
        
        // 简单的换行逻辑：按空格分词
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int currentY = 0;
        int lineHeight = this.font.lineHeight;
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int testWidth = this.font.width(testLine);
            
            if (testWidth > scaledMaxWidth && currentLine.length() > 0) {
                // 绘制当前行
                g.drawString(this.font, currentLine.toString(), 0, currentY, color, false);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        // 绘制最后一行
        if (currentLine.length() > 0) {
            g.drawString(this.font, currentLine.toString(), 0, currentY, color, false);
        }
        
        pose.popPose();
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

