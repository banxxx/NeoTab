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
    private static final int MAX_CONTENT_WIDTH      = 600;  // 增加最大内容宽度，适应横向布局
    private static final int ROW_HEIGHT              = 24;
    private static final int INPUT_HEIGHT            = 20;
    private static final int TITLE_INPUT_HEIGHT      = 60;
    private static final int MULTILINE_INPUT_HEIGHT  = 60;
    private static final int TOGGLE_WIDTH            = 26;   // 适合Minecraft GUI比例的开关宽度
    private static final int TOGGLE_HEIGHT           = 14;   // 适合Minecraft GUI比例的开关高度
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
    private static final int TAB_BAR_WIDTH           = 120;  // Tab栏宽度，适中大小
    private static final int TAB_CONTENT_GAP         = 8;
    private static final int TAB_BUTTON_HEIGHT       = 24;
    private static final int TAB_BUTTON_GAP          = 4;
    private static final int TAB_BUTTON_LEFT_PADDING = 6;
    private static final int SCROLL_TRACK_W = 14;

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

    /**
     * 重建自定义主题相关控件，保留当前的 selectedThemeId。
     * 用于重置自定义主题配置后刷新界面，避免 reinit() 把主题选择重置回 initialConfig 的值。
     */
    void rebuildCustomThemeWidgets() {
        // 移除所有已注册的 widget，重新初始化
        // 但保留 selectedThemeId，不从 initialConfig 重读
        String savedThemeId = theme.selectedThemeId;
        this.init();
        theme.selectedThemeId = savedThemeId;
        // 重新同步 widget 可见性（因为 init 里会根据 initialConfig 设置 selectedThemeId）
        syncTabWidgetVisibility();
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        applyWidgetLayout(layout);
    }
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
        if (theme.customBackgroundHexInput != null) theme.customBackgroundHexInput.visible = themeTab && isCustom;
        theme.customBorderOuterFactorButton.visible = themeTab && isCustom;
        if (theme.customBorderOuterHexInput != null) theme.customBorderOuterHexInput.visible = themeTab && isCustom;
        theme.customAnimationToggle.visible = themeTab && isCustom;
        if (theme.customAnimationSpeedButton != null) theme.customAnimationSpeedButton.visible = themeTab && isCustom;
        if (theme.resetToDefaultButton != null) theme.resetToDefaultButton.visible = themeTab && isCustom;
        if (theme.resetConfirmButton != null) theme.resetConfirmButton.visible = themeTab && isCustom && theme.showResetConfirmation;
        if (theme.resetCancelButton != null) theme.resetCancelButton.visible = themeTab && isCustom && theme.showResetConfirmation;
        for (Button button : theme.customBorderColorButtons) button.visible = themeTab && isCustom;
        for (net.minecraft.client.gui.components.EditBox box : theme.customBorderHexInputs) {
            box.visible = themeTab && isCustom;
        }
        for (net.minecraft.client.gui.components.EditBox box : theme.customBorderHexInputs) box.visible = themeTab && isCustom;
        if (theme.addCustomBorderColorButton != null) theme.addCustomBorderColorButton.visible = themeTab && isCustom;
        if (theme.embeddedColorPicker != null) theme.embeddedColorPicker.visible = themeTab && isCustom && theme.currentSelectedColorType != null;
        // 不再隐藏"添加边框颜色"按钮，而是通过颜色选择器的背景遮罩来遮挡
    }

    //  switchTab 
    private void switchTab(ConfigTab tab) {
        if (activeTab == tab) return;
        
        // 切换标签页时清除所有HEX输入框的焦点
        for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
            hexBox.setFocused(false);
        }
        
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
        // 滚动时隐藏颜色选择器（对所有颜色类型）
        if (theme.currentSelectedColorType != null) {
            theme.currentSelectedColorType = null;
            theme.currentSelectedBorderIndex = -1;
            // 清除所有HEX输入框的焦点
            if (theme.customBackgroundHexInput != null) {
                theme.customBackgroundHexInput.setFocused(false);
            }
            if (theme.customBorderOuterHexInput != null) {
                theme.customBorderOuterHexInput.setFocused(false);
            }
            for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                hexBox.setFocused(false);
            }
            syncTabWidgetVisibility();
        }
        setScrollOffsetInternal(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    // ── mouseClicked ─────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        NeoTabConfigScreenLayout.Layout layout = buildLayoutImpl();
        
        // 优先检查玩家搜索框的清除按钮（在权限配置标签页）
        if (activeTab == ConfigTab.PERMISSIONS && button == 0 && permissions.playerSearchBox != null) {
            if (permissions.playerSearchBox instanceof com.poso.neotab.client.widget.CenteredEditBox centeredBox) {
                if (centeredBox.handleClearButtonClick(mouseX, mouseY)) {
                    permissions.playerSuggestions.clear();
                    permissions.dropdownScrollOffset = 0;
                    return true;
                }
            }
        }
        
        // 优先检查HEX输入框的点击（在所有其他处理之前）
        if (activeTab == ConfigTab.THEME && "custom".equals(theme.selectedThemeId) && button == 0) {
            for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                if (hexBox.visible && hexBox.isMouseOver(mouseX, mouseY)) {
                    // 清除其他输入框的焦点
                    for (net.minecraft.client.gui.components.EditBox otherBox : theme.customBorderHexInputs) {
                        if (otherBox != hexBox) {
                            otherBox.setFocused(false);
                        }
                    }
                    // 设置当前输入框焦点
                    setFocused(hexBox);
                    hexBox.setFocused(true);
                    return hexBox.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        
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
            if (theme.customAnimationToggle != null && theme.customAnimationToggle.visible
                    && mouseX >= theme.customAnimationToggle.getX()
                    && mouseX < theme.customAnimationToggle.getX() + theme.customAnimationToggle.getWidth()
                    && mouseY >= theme.customAnimationToggle.getY()
                    && mouseY < theme.customAnimationToggle.getY() + theme.customAnimationToggle.getHeight())
                return theme.customAnimationToggle.mouseClicked(mouseX, mouseY, button);
            if (theme.customAnimationSpeedButton != null && theme.customAnimationSpeedButton.visible
                    && theme.customAnimationSpeedButton.isMouseOver(mouseX, mouseY))
                return theme.customAnimationSpeedButton.mouseClicked(mouseX, mouseY, button);
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
            } else if (theme.currentSelectedColorType != null) {
                // 点击颜色选择器外部时隐藏（对所有颜色类型：background, outer_border, border_*）
                boolean clickedSwatch = false;
                boolean clickedHexInput = false;
                
                // 检查是否点击了色块按钮本身（避免立即关闭）
                if ("background".equals(theme.currentSelectedColorType)) {
                    if (theme.customBackgroundColorButton != null && theme.customBackgroundColorButton.visible 
                            && theme.customBackgroundColorButton.isMouseOver(mouseX, mouseY)) {
                        clickedSwatch = true;
                    }
                    // 检查是否点击了背景颜色HEX输入框
                    if (theme.customBackgroundHexInput != null && theme.customBackgroundHexInput.visible 
                            && theme.customBackgroundHexInput.isMouseOver(mouseX, mouseY)) {
                        clickedHexInput = true;
                        setFocused(theme.customBackgroundHexInput);
                        theme.customBackgroundHexInput.setFocused(true);
                    }
                } else if ("outer_border".equals(theme.currentSelectedColorType)) {
                    if (theme.customBorderOuterFactorButton != null && theme.customBorderOuterFactorButton.visible 
                            && theme.customBorderOuterFactorButton.isMouseOver(mouseX, mouseY)) {
                        clickedSwatch = true;
                    }
                    // 检查是否点击了外层边框颜色HEX输入框
                    if (theme.customBorderOuterHexInput != null && theme.customBorderOuterHexInput.visible 
                            && theme.customBorderOuterHexInput.isMouseOver(mouseX, mouseY)) {
                        clickedHexInput = true;
                        setFocused(theme.customBorderOuterHexInput);
                        theme.customBorderOuterHexInput.setFocused(true);
                    }
                } else if (theme.currentSelectedColorType.startsWith("border_")) {
                    // 边框颜色项
                    for (Button btn : theme.customBorderColorButtons) {
                        if (btn.visible && btn.isMouseOver(mouseX, mouseY)) { clickedSwatch = true; break; }
                    }
                    // 检查是否点击了HEX输入框
                    for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                        if (hexBox.visible && hexBox.isMouseOver(mouseX, mouseY)) { 
                            clickedHexInput = true; 
                            // 设置焦点到点击的输入框
                            setFocused(hexBox);
                            hexBox.setFocused(true);
                            // 清除其他输入框的焦点
                            for (net.minecraft.client.gui.components.EditBox otherBox : theme.customBorderHexInputs) {
                                if (otherBox != hexBox) {
                                    otherBox.setFocused(false);
                                }
                            }
                            break; 
                        }
                    }
                }
                
                if (!clickedSwatch && !clickedHexInput) {
                    // 清除颜色选择器状态
                    theme.currentSelectedColorType = null;
                    theme.currentSelectedBorderIndex = -1;
                    // 清除所有HEX输入框的焦点
                    if (theme.customBackgroundHexInput != null) {
                        theme.customBackgroundHexInput.setFocused(false);
                    }
                    if (theme.customBorderOuterHexInput != null) {
                        theme.customBorderOuterHexInput.setFocused(false);
                    }
                    for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                        hexBox.setFocused(false);
                    }
                    syncTabWidgetVisibility();
                    NeoTabConfigScreenLayout.Layout newLayout = buildLayoutImpl();
                    applyWidgetLayout(newLayout);
                }
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
        
        // 限制面板高度，使其更符合横向布局（宽度 > 高度）
        // HTML中的比例是 820:600 ≈ 1.37:1
        int maxPanelH = (int)(panelW / 1.37);  // 根据宽度计算最大高度
        int availableH = this.height - panelY - 8;
        int panelH = Math.min(maxPanelH, availableH);
        
        AEStyleRenderer.drawMainPanel(g, panelX, panelY, panelW, panelH);

        // 绘制标题栏渐变背景（在主面板边框内）
        int titleBarHeight = 24;
        AEStyleRenderer.drawTitleBarGradient(g, panelX + 3, panelY + 3, panelW - 6, titleBarHeight);

        net.minecraft.network.chat.Component boldTitle = this.title.copy()
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        int titleTextX = panelX + 10;  // 左对齐，距离左边框10像素
        g.drawString(this.font, boldTitle, titleTextX, panelY + 6, AEStyleRenderer.COLOR_TITLE_TEXT, false);

        // 标题栏底部分隔线（在标题下方，分隔标题栏和主体区域）
        int titleBarBottom = panelY + 24;  // 标题栏高度约24px
        AEStyleRenderer.drawTitleBarDivider(g, panelX + 3, titleBarBottom, panelW - 6);

        NeoTabConfigScreenRenderer.renderTabBar(g, this.font, this.activeTab, layout, panelY, mouseX, mouseY, this.screenMode);
        renderScrollableContent(g, mouseX, mouseY, partialTick, layout);
        NeoTabConfigScreenRenderer.renderButtonBar(g, layout, this.height);
        // Fixed widgets (done/cancel buttons) - 完成用主要样式（绿色），取消用次要样式（米色）
        NeoTabConfigScreenRenderer.renderPrimaryButton(g, this.font, this.doneButton, mouseX, mouseY);
        NeoTabConfigScreenRenderer.renderSecondaryButton(g, this.font, this.cancelButton, mouseX, mouseY);
        // Permissions dropdown on top
        renderPlayerSuggestionDropdown(g, mouseX, mouseY);
        // ── 颜色相关浮动组件（最后渲染，确保在最上层）──
        renderColorOverlay(g, mouseX, mouseY, partialTick, layout);
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
        
        // 绘制下拉菜单背景和边框（白色背景）
        g.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + totalH + 1, 0xFFA0A0A0);  // 灰色边框
        g.fill(dropX, dropY, dropX + dropW, dropY + totalH, 0xFFFFFFFF);  // 白色背景
        
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
            if (hovered) g.fill(dropX, itemY, dropX + dropW - (needsScrollbar ? 8 : 0), itemY + itemH, 0xFFB8D4A8);  // 莫奈绿色悬停背景
            g.drawString(this.font, permissions.playerSuggestions.get(i),
                dropX + 4, itemY + (itemH - this.font.lineHeight) / 2,
                0xFF000000, false);  // 黑色文字
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
            
            // 滚动条轨道（浅灰色）
            g.fill(scrollbarX, dropY + 2, scrollbarX + scrollbarW, dropY + totalH - 2, 0xFFD0D0D0);
            // 滚动条滑块（深灰色，更明显）
            g.fill(scrollbarX, dropY + 2 + thumbY, scrollbarX + scrollbarW, dropY + 2 + thumbY + thumbH, 0xFF808080);
        }
    }

    private void renderColorOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick,
                                    NeoTabConfigScreenLayout.Layout layout) {
        if (activeTab != ConfigTab.THEME) return;

        if (theme.embeddedColorPicker == null || !theme.embeddedColorPicker.visible) return;

        int py = theme.embeddedColorPicker.getY();
        int ph = theme.embeddedColorPicker.getHeight();
        int pw = theme.embeddedColorPicker.getWidth();
        int cpX = theme.embeddedColorPicker.getX();

        // 智能翻转：下方空间不足时向上展开
        int renderY = py;
        if (py + ph > layout.buttonBarTop()) {
            renderY = Math.max(layout.viewportTop(), py - ph - 28);
        }
        theme.embeddedColorPicker.setY(renderY);

        // 铺不透明背景，阻断下层文字/控件渗透
        g.fill(cpX, renderY, cpX + pw, renderY + ph, 0xFFFAF7EF);
        theme.embeddedColorPicker.render(g, mouseX, mouseY, partialTick);
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
            int toggleCardContentH = Math.max(TOGGLE_HEIGHT, titleLineHeight + 2 + subtitleLineHeight);
            int simpleCardHeight = CARD_PADDING + toggleCardContentH + CARD_PADDING;
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
            
            // 称号功能卡片
            cardY = layout.toScreenY(layout.titleRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.list.title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.list.title.subtitle"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
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
            drawScaledText(g, Component.translatable("screen.neotab.footer.tps.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // MSPT 信息卡片
            cardY = layout.toScreenY(layout.footerMsptRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.mspt"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.footer.mspt.tooltip"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 在线人数卡片
            cardY = layout.toScreenY(layout.footerOnlineRowY());
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), simpleCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.footer.online"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            drawScaledText(g, Component.translatable("screen.neotab.footer.online.tooltip"),
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
            Component titleText = Component.translatable("screen.neotab.theme.preset_title");
            g.drawString(this.font, titleText,
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题文字（限制宽度，自动换行）
            drawScaledText(g, Component.translatable("screen.neotab.theme.preset_subtitle"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            // 主题选择器按钮由widget系统绘制
            
            // 如果选中了"自定义"主题，显示自定义配置卡片
            if ("custom".equals(theme.selectedThemeId)) {
                // 自定义配置卡片（放在最上面，包含重置按钮）
                cardY = layout.toScreenY(layout.customResetRowY());
                int resetCardHeight = CARD_PADDING * 2 + titleLineHeight + 2 + subtitleLineHeight + 8;  // 增加高度以容纳确认/取消按钮
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), resetCardHeight);
                
                // 计算文字垂直居中位置
                int textBlockHeight = titleLineHeight + 2 + subtitleLineHeight;
                int textStartY = cardY + (resetCardHeight - textBlockHeight) / 2;
                
                // 标题（左对齐，垂直居中）
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.config_title"),
                        layout.left() + CARD_PADDING, textStartY,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                // 副标题（左对齐，垂直居中）
                drawScaledText(g, Component.translatable("screen.neotab.custom_theme.config_subtitle"),
                        layout.left() + CARD_PADDING, textStartY + titleLineHeight + 2,
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                // 重置按钮由widget系统绘制（靠右边框，基于卡片高度垂直居中）
                // 确认/取消按钮在重置按钮下方，总宽度与重置按钮一致
                
                // 动画效果卡片（标题 + 右侧开关）
                cardY = layout.toScreenY(layout.customAnimationRowY());
                int animToggleCardH = Math.max(CARD_PADDING + titleLineHeight + CARD_PADDING,
                                               CARD_PADDING + 14 + CARD_PADDING);
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), animToggleCardH);
                g.drawString(this.font, Component.translatable("screen.neotab.theme.animation_title"),
                        layout.left() + CARD_PADDING, cardY + (animToggleCardH - titleLineHeight) / 2,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                // 开关由 widget 系统绘制

                // 动画速率卡片（标题 + 副标题 + 右侧速率按钮）
                cardY = layout.toScreenY(layout.customAnimSpeedRowY());
                int animSpeedCardH = Math.max(CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING,
                                              CARD_PADDING + 18 + CARD_PADDING);
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), animSpeedCardH);
                g.drawString(this.font, Component.translatable("screen.neotab.theme.animation_speed_title"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                drawScaledText(g, Component.translatable("screen.neotab.theme.animation_subtitle"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                // 速率按钮由 widget 系统绘制
                
                // 颜色配置：每个颜色项独立卡片
                java.util.List<Integer> borderColors = theme.customThemeConfig != null ? 
                    theme.customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
                
                int swatchSize = 24;  // 色块大小（缩小）
                int hexInputH = 24;   // HEX输入框高度（缩小）
                int previewH = 8;     // 预览块高度
                int contentGap = 8;   // 标题与内容之间的间距
                int previewGap = 8;   // 颜色行与预览块之间的间距
                
                // 计算卡片高度（标题 + 间距 + 色块行 + 间距 + 预览块）
                int colorCardH = CARD_PADDING + titleLineHeight + contentGap + swatchSize + previewGap + previewH + CARD_PADDING;
                int CARD_GAP = 8;
                
                // 背景颜色卡片
                cardY = layout.toScreenY(layout.customBgColorRowY());
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), colorCardH);
                
                // 绘制标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.background_color"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                
                // 绘制色块（由widget系统绘制customBackgroundColorButton）
                if (theme.customBackgroundColorButton != null && theme.customBackgroundColorButton.visible) {
                    int swatchX = theme.customBackgroundColorButton.getX();
                    int swatchY = theme.customBackgroundColorButton.getY();
                    int bgColor = theme.customThemeConfig != null ? theme.customThemeConfig.getBackgroundColor() : 0xFFFFFFFF;
                    
                    // 棋盘格背景（用于显示透明度）
                    g.fill(swatchX, swatchY, swatchX + swatchSize / 2, swatchY + swatchSize / 2, 0xFFCCCCCC);
                    g.fill(swatchX + swatchSize / 2, swatchY, swatchX + swatchSize, swatchY + swatchSize / 2, 0xFF999999);
                    g.fill(swatchX, swatchY + swatchSize / 2, swatchX + swatchSize / 2, swatchY + swatchSize, 0xFF999999);
                    g.fill(swatchX + swatchSize / 2, swatchY + swatchSize / 2, swatchX + swatchSize, swatchY + swatchSize, 0xFFCCCCCC);
                    // 实际颜色
                    g.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, bgColor);
                    // 边框
                    AEStyleRenderer.drawOutline(g, swatchX, swatchY, swatchSize, swatchSize, 0xFFB7AF9B, 2);
                }
                
                // HEX输入框由renderHexInputBox统一渲染，这里不需要绘制背景
                
                // 绘制预览块（在卡片底部）
                int bgPreviewY = cardY + colorCardH - CARD_PADDING - previewH;
                int previewX = layout.left() + CARD_PADDING;
                int previewW = layout.contentWidth() - CARD_PADDING * 2;
                int bgColor = theme.customThemeConfig != null ? theme.customThemeConfig.getBackgroundColor() : 0xFFFFFFFF;
                AEStyleRenderer.drawOutline(g, previewX, bgPreviewY, previewW, previewH, 0xFFC8C0AD, 1);
                g.fill(previewX + 1, bgPreviewY + 1, previewX + previewW - 1, bgPreviewY + previewH - 1, bgColor);
                
                // 外层边框颜色卡片
                cardY = layout.toScreenY(layout.customOuterBorderRowY());
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), colorCardH);
                
                // 绘制标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.border_outer_color"),
                        layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                
                // 绘制色块（由widget系统绘制customBorderOuterFactorButton）
                if (theme.customBorderOuterFactorButton != null && theme.customBorderOuterFactorButton.visible) {
                    int swatchX = theme.customBorderOuterFactorButton.getX();
                    int swatchY = theme.customBorderOuterFactorButton.getY();
                    int outerColor = theme.customThemeConfig != null ? theme.customThemeConfig.getBorderOuterColor() : 0xFFB7AF9B;
                    
                    // 棋盘格背景
                    g.fill(swatchX, swatchY, swatchX + swatchSize / 2, swatchY + swatchSize / 2, 0xFFCCCCCC);
                    g.fill(swatchX + swatchSize / 2, swatchY, swatchX + swatchSize, swatchY + swatchSize / 2, 0xFF999999);
                    g.fill(swatchX, swatchY + swatchSize / 2, swatchX + swatchSize / 2, swatchY + swatchSize, 0xFF999999);
                    g.fill(swatchX + swatchSize / 2, swatchY + swatchSize / 2, swatchX + swatchSize, swatchY + swatchSize, 0xFFCCCCCC);
                    // 实际颜色
                    g.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, outerColor);
                    // 边框
                    AEStyleRenderer.drawOutline(g, swatchX, swatchY, swatchSize, swatchSize, 0xFFB7AF9B, 2);
                }
                
                // HEX输入框由renderHexInputBox统一渲染，这里不需要绘制背景
                
                // 绘制预览块（在卡片底部）
                int outerPreviewY = cardY + colorCardH - CARD_PADDING - previewH;
                int outerColor = theme.customThemeConfig != null ? theme.customThemeConfig.getBorderOuterColor() : 0xFFB7AF9B;
                AEStyleRenderer.drawOutline(g, previewX, outerPreviewY, previewW, previewH, 0xFFC8C0AD, 1);
                g.fill(previewX + 1, outerPreviewY + 1, previewX + previewW - 1, outerPreviewY + previewH - 1, outerColor);
                
                // 边框颜色大卡片（一个卡片包含所有边框颜色项，添加按钮在右上角）
                int borderItemH = THEME_OPTION_HEIGHT + 12;  // 每个颜色行高度（增加4像素）
                int borderItemGap = 8;  // 行间距
                int borderCardInnerH = titleLineHeight + 2 + subtitleLineHeight + 8;
                borderCardInnerH += borderColors.size() * (borderItemH + borderItemGap);
                // 添加按钮现在在右上角，不需要为底部按钮预留空间
                int borderBigCardH = CARD_PADDING + borderCardInnerH + CARD_PADDING;
                int borderBigCardY = layout.toScreenY(layout.customAddBorderColorRowY());
                AEStyleRenderer.drawConfigModuleCard(g, layout.left(), borderBigCardY, layout.contentWidth(), borderBigCardH);
                
                // 标题和副标题（标题限制宽度，为右上角的添加按钮留出空间）
                int borderTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 100 - 8;  // 减去按钮宽度和间距
                Component titleComp = Component.translatable("screen.neotab.theme.border_colors_title");
                String titleStr = titleComp.getString();
                if (this.font.width(titleStr) > borderTitleMaxWidth) {
                    // 标题太长，截断并添加省略号
                    titleStr = this.font.plainSubstrByWidth(titleStr, borderTitleMaxWidth - this.font.width("...")) + "...";
                }
                g.drawString(this.font, titleStr,
                        layout.left() + CARD_PADDING, borderBigCardY + CARD_PADDING,
                        AEStyleRenderer.COLOR_MODULE_TITLE, false);
                drawScaledText(g, Component.translatable("screen.neotab.custom_theme.border_colors"),
                        layout.left() + CARD_PADDING, borderBigCardY + CARD_PADDING + titleLineHeight + 2,
                        AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
                
                // 每个颜色行（白色内嵌行）
                int rowStartY = borderBigCardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
                int rowW = layout.contentWidth() - CARD_PADDING * 2;
                int borderSwatchSize = 16;  // 边框颜色色块固定大小（小巧，参考HTML原型）
                for (int i = 0; i < borderColors.size(); i++) {
                    int color = borderColors.get(i);
                    int rowY = rowStartY + i * (borderItemH + borderItemGap);
                    boolean borderHov = mouseX >= layout.left() + CARD_PADDING && mouseX < layout.left() + CARD_PADDING + rowW
                                     && mouseY >= rowY && mouseY < rowY + borderItemH;
                    // 白色行背景
                    AEStyleRenderer.drawOutline(g, layout.left() + CARD_PADDING, rowY, rowW, borderItemH, AEStyleRenderer.COLOR_PANEL_BORDER, 1);
                    g.fill(layout.left() + CARD_PADDING + 1, rowY + 1, layout.left() + CARD_PADDING + rowW - 1, rowY + borderItemH - 1,
                            borderHov ? 0xFFFAF7EF : 0xFFFFFFFF);
                    
                    // 左侧：颜色色块（固定 16x16，垂直居中）
                    int swatchX = layout.left() + CARD_PADDING + 8;
                    int swatchY = rowY + (borderItemH - borderSwatchSize) / 2;
                    // 棋盘格背景
                    g.fill(swatchX, swatchY, swatchX + borderSwatchSize / 2, swatchY + borderSwatchSize / 2, 0xFFCCCCCC);
                    g.fill(swatchX + borderSwatchSize / 2, swatchY, swatchX + borderSwatchSize, swatchY + borderSwatchSize / 2, 0xFF999999);
                    g.fill(swatchX, swatchY + borderSwatchSize / 2, swatchX + borderSwatchSize / 2, swatchY + borderSwatchSize, 0xFF999999);
                    g.fill(swatchX + borderSwatchSize / 2, swatchY + borderSwatchSize / 2, swatchX + borderSwatchSize, swatchY + borderSwatchSize, 0xFFCCCCCC);
                    g.fill(swatchX, swatchY, swatchX + borderSwatchSize, swatchY + borderSwatchSize, color);
                    AEStyleRenderer.drawOutline(g, swatchX, swatchY, borderSwatchSize, borderSwatchSize, AEStyleRenderer.COLOR_OUTLINE, 1);
                    
                    // 中间：hex输入框（在scissor外单独渲染，这里不绘制）
                    
                    // 右侧：红色 × 删除按钮（由widget系统处理点击，这里只绘制图标）
                    int delX = layout.left() + CARD_PADDING + rowW - 20;
                    int delY = rowY + (borderItemH - this.font.lineHeight) / 2;
                    g.drawString(this.font, "×", delX + 6, delY, 0xFFB34242, false);
                }
                
                // 添加边框颜色按钮（在大卡片内底部）
                if (borderColors.size() < 7) {
                    int addBtnY = rowStartY + borderColors.size() * (borderItemH + borderItemGap);
                    // 添加按钮由widget系统绘制
                }
            }
            
            // 血量显示分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.theme.health_section"),
                    layout.left(), layout.toScreenY(layout.healthSectionHeaderY()), layout.right());
            
            // 显示效果卡片
            cardY = layout.toScreenY(layout.healthModeRowY());
            int toggleCardContentH2 = Math.max(INPUT_HEIGHT, titleLineHeight + 2 + subtitleLineHeight);
            int healthCardHeight = CARD_PADDING + toggleCardContentH2 + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), healthCardHeight);
            
            // 标题（限制宽度）
            int healthTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 80 - 8;  // 减去按钮宽度
            g.drawString(this.font, Component.translatable("screen.neotab.theme.health_mode_title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题（自动换行）
            drawWrappedScaledText(g, Component.translatable("screen.neotab.theme.health_mode_subtitle").getString(),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    healthTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            // 血量模式按钮由widget系统绘制
            
            // 布局分列分区标题
            AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.theme.layout_section"),
                    layout.left(), layout.toScreenY(layout.layoutSectionHeaderY()), layout.right());
            
            // 启用分列卡片
            cardY = layout.toScreenY(layout.layoutEnabledRowY());
            int layoutEnabledCardHeight = CARD_PADDING + toggleCardContentH2 + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutEnabledCardHeight);
            
            // 标题（限制宽度）
            int layoutTitleMaxWidth = layout.contentWidth() - CARD_PADDING * 2 - 56 - 8;
            g.drawString(this.font, Component.translatable("screen.neotab.theme.layout_enabled_title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            // 副标题
            drawWrappedScaledText(g, Component.translatable("screen.neotab.theme.layout_enabled_subtitle").getString(),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 展示列数卡片
            cardY = layout.toScreenY(layout.layoutColumnsRowY());
            int layoutColumnsCardHeight = CARD_PADDING + Math.max(INPUT_HEIGHT, titleLineHeight + 2 + subtitleLineHeight) + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutColumnsCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.theme.layout_columns_title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            drawWrappedScaledText(g, Component.translatable("screen.neotab.theme.layout_columns_subtitle").getString(),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 展示行数卡片
            cardY = layout.toScreenY(layout.layoutRowsRowY());
            int layoutRowsCardHeight = CARD_PADDING + Math.max(INPUT_HEIGHT, titleLineHeight + 2 + subtitleLineHeight) + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutRowsCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.theme.layout_rows_title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            
            drawWrappedScaledText(g, Component.translatable("screen.neotab.theme.layout_rows_subtitle").getString(),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    layoutTitleMaxWidth, AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            
            // 提示卡片（无按钮）
            cardY = layout.toScreenY(layout.layoutHintRowY());
            // 多行副标题需要计算高度
            String hintText = "当玩家数超过容量时，TAB 列表将自动分页显示。\n快捷键：TAB + 左/右箭头键翻页。";
            int hintLines = 2;  // 两行文字
            int layoutHintCardHeight = CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * hintLines * 0.82f) + CARD_PADDING;
            AEStyleRenderer.drawConfigModuleCard(g, layout.left(), cardY, layout.contentWidth(), layoutHintCardHeight);
            
            g.drawString(this.font, Component.translatable("screen.neotab.theme.layout_hint_title"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING,
                    AEStyleRenderer.COLOR_MODULE_TITLE, false);
            // 绘制多行副标题
            drawScaledText(g, Component.translatable("screen.neotab.theme.layout_hint_line1"),
                    layout.left() + CARD_PADDING, cardY + CARD_PADDING + titleLineHeight + 2,
                    AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
            drawScaledText(g, Component.translatable("screen.neotab.theme.layout_hint_line2"),
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
        AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.permissions.global_section"),
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
        int cardHeight = CARD_PADDING + Math.max(TOGGLE_HEIGHT, titleLineHeight + 2 + subtitleLineHeight) + CARD_PADDING;
        
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
        AEStyleRenderer.drawSectionHeader(g, this.font, Component.translatable("screen.neotab.permissions.personal_section"),
                layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;
        
        // 添加玩家卡片
        int addPlayerCardY = layout.toScreenY(y);
        int addPlayerCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + INPUT_HEIGHT + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), addPlayerCardY, layout.contentWidth(), addPlayerCardHeight);
        
        g.drawString(this.font, Component.translatable("screen.neotab.permissions.add_player_title"),
                layout.left() + CARD_PADDING, addPlayerCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.translatable("screen.neotab.permissions.add_player_subtitle"),
                layout.left() + CARD_PADDING, addPlayerCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        // 输入框和添加按钮由widget系统绘制
        
        y += addPlayerCardHeight + CARD_GAP;
        
        // 玩家列表显示卡片
        int playerListCardY = layout.toScreenY(y);
        int playerListContentHeight;
        if (permissions.targetPlayers.isEmpty()) {
            playerListContentHeight = INPUT_HEIGHT;  // 与单个标签高度一致
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
        
        g.drawString(this.font, Component.translatable("screen.neotab.permissions.player_list_title"),
                layout.left() + CARD_PADDING, playerListCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.translatable("screen.neotab.permissions.player_list_subtitle"),
                layout.left() + CARD_PADDING, playerListCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        
        // 玩家列表内容区域
        int listContentY = playerListCardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
        if (permissions.targetPlayers.isEmpty()) {
            // 空状态提示（居中显示）
            drawScaledText(g, Component.translatable("screen.neotab.permissions.player_list_empty"),
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
        
        g.drawString(this.font, Component.translatable("screen.neotab.permissions.hint_title"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.translatable("screen.neotab.permissions.hint_line1"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        drawScaledText(g, Component.translatable("screen.neotab.permissions.hint_line2"),
                layout.left() + CARD_PADDING, hintCardY + CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 0.82f),
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        
        y += hintCardHeight + 16;
        
        // 应用权限设置卡片
        int applySettingsCardY = layout.toScreenY(y);
        int applySettingsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + INPUT_HEIGHT + CARD_PADDING;
        AEStyleRenderer.drawConfigModuleCard(g, layout.left(), applySettingsCardY, layout.contentWidth(), applySettingsCardHeight);
        
        g.drawString(this.font, Component.translatable("screen.neotab.permissions.apply_title"),
                layout.left() + CARD_PADDING, applySettingsCardY + CARD_PADDING,
                AEStyleRenderer.COLOR_MODULE_TITLE, false);
        drawScaledText(g, Component.translatable("screen.neotab.permissions.apply_subtitle"),
                layout.left() + CARD_PADDING, applySettingsCardY + CARD_PADDING + titleLineHeight + 2,
                AEStyleRenderer.COLOR_MODULE_SUBTITLE, 0.82f);
        // 两个应用按钮由widget系统绘制
        
        y += applySettingsCardHeight + 16;
        
        // 保存按钮（由widget系统绘制，这里不需要绘制卡片）
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            // 颜色选择器和hex输入框在scissor外单独渲染，这里跳过
            if (r == theme.embeddedColorPicker) continue;
            // HEX输入框需要特殊处理，先跳过（包括背景颜色、外层边框颜色、边框颜色的HEX输入框）
            if (r instanceof net.minecraft.client.gui.components.EditBox box) {
                if (box == theme.customBackgroundHexInput || 
                    box == theme.customBorderOuterHexInput ||
                    theme.customBorderHexInputs.contains(box)) {
                    continue;
                }
                // 玩家搜索框：绘制白色背景
                if (box == permissions.playerSearchBox && box.visible) {
                    // 绘制白色背景
                    g.fill(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), 0xFFFFFFFF);
                    // 绘制边框
                    int borderColor = box.isFocused() ? 0xFF8B8B8B : 0xFFA0A0A0;
                    g.fill(box.getX() - 1, box.getY() - 1, box.getX() + box.getWidth() + 1, box.getY(), borderColor);  // 上
                    g.fill(box.getX() - 1, box.getY() + box.getHeight(), box.getX() + box.getWidth() + 1, box.getY() + box.getHeight() + 1, borderColor);  // 下
                    g.fill(box.getX() - 1, box.getY(), box.getX(), box.getY() + box.getHeight(), borderColor);  // 左
                    g.fill(box.getX() + box.getWidth(), box.getY(), box.getX() + box.getWidth() + 1, box.getY() + box.getHeight(), borderColor);  // 右
                }
            }
            
            // 检查widget的visible属性
            if (r instanceof AbstractWidget widget && !widget.visible) continue;

            if (r instanceof CycleButton<?> cb) {
                NeoTabConfigScreenRenderer.renderAECycleButton(g, this.font, cb, mouseX, mouseY);
            } else if (r instanceof Button btn) {
                int themeIndex = theme.themeOptionButtons.indexOf(btn);
                if (themeIndex >= 0) {
                    NeoTabConfigScreenRenderer.renderThemeOptionButton(g, this.font, btn, theme.themeOptionIds.get(themeIndex), theme.selectedThemeId, mouseX, mouseY);
                } else if (btn == theme.customBackgroundColorButton) {
                    // 背景颜色色块按钮：由卡片渲染代码手动绘制，这里跳过（避免重复绘制）
                } else if (theme.customBorderColorButtons.contains(btn)) {
                    int bi = theme.customBorderColorButtons.indexOf(btn);
                    if (bi % 2 == 0) {
                        // 颜色选择按钮：由卡片渲染代码绘制，这里跳过（避免重复绘制）
                    } else {
                        // 删除按钮：不绘制（× 图标已在卡片渲染中手动绘制）
                    }
                } else if (btn == theme.customBorderOuterFactorButton) {
                    // 外层边框颜色色块按钮：由卡片渲染代码手动绘制，这里跳过（避免重复绘制）
                } else if (btn == theme.resetToDefaultButton) {
                    // 重置按钮：使用红色危险样式
                    renderDangerButton(g, this.font, btn, mouseX, mouseY);
                } else {
                    NeoTabConfigScreenRenderer.renderAEButton(g, this.font, btn, mouseX, mouseY);
                }
            } else {
                r.render(g, mouseX, mouseY, partialTick);
            }
        }
        
        // 最后渲染HEX输入框，确保它们在最上层（仅在主题标签页显示）
        if (activeTab == ConfigTab.THEME && "custom".equals(theme.selectedThemeId)) {
            // 渲染背景颜色HEX输入框
            if (theme.customBackgroundHexInput != null && theme.customBackgroundHexInput.visible) {
                renderHexInputBox(g, theme.customBackgroundHexInput, mouseX, mouseY);
            }
            
            // 渲染外层边框颜色HEX输入框
            if (theme.customBorderOuterHexInput != null && theme.customBorderOuterHexInput.visible) {
                renderHexInputBox(g, theme.customBorderOuterHexInput, mouseX, mouseY);
            }
            
            // 渲染边框颜色HEX输入框
            for (net.minecraft.client.gui.components.EditBox box : theme.customBorderHexInputs) {
                if (box.visible) {
                    renderHexInputBox(g, box, mouseX, mouseY);
                }
            }
        }
    }
    
    // 统一的HEX输入框渲染方法（与边框颜色使用相同的逻辑）
    private void renderHexInputBox(GuiGraphics g, net.minecraft.client.gui.components.EditBox box, int mouseX, int mouseY) {
        // ── Hex 输入框：scissor 内渲染 + 文字左对齐 + 选中背景（修复版）──────────────────────
        // 白色背景（左侧延伸 4px，视觉上形成内边距）
        int bx = box.getX() - 5;
        int by = box.getY() - 1;
        int bw = box.getWidth() + 6;
        int bh = box.getHeight() + 2;
        g.fill(bx,     by,     bx + bw,     by + bh,     AEStyleRenderer.COLOR_PANEL_BORDER);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0xFFFFFFFF);
        
        // 色值文字左对齐，左边距小于右边距
        String value = box.getValue();
        int leftPadding = 4;  // 左边距
        int textX = box.getX() + leftPadding;
        int textY = box.getY() + (box.getHeight() - this.font.lineHeight) / 2;
        
        // 只在聚焦时处理选中和光标
        if (box.isFocused()) {
            String highlighted = box.getHighlighted();
            
            // 有选中文字时才进行复杂的选中渲染
            if (!highlighted.isEmpty()) {
                int cursorPos = box.getCursorPosition();
                int highlightLen = highlighted.length();
                
                // 快速计算选中范围（优化：减少字符串比较）
                int selStart = Math.max(0, Math.min(cursorPos - highlightLen, cursorPos));
                int selEnd = Math.min(value.length(), Math.max(cursorPos, cursorPos + highlightLen));
                
                // 验证选中范围是否有效
                if (selStart < selEnd && selEnd <= value.length()) {
                    String beforeHighlight = value.substring(0, selStart);
                    String afterHighlight = value.substring(selEnd);
                    
                    int highlightX = textX + this.font.width(beforeHighlight);
                    int highlightW = this.font.width(highlighted);
                    
                    // 绘制选中背景
                    g.fill(highlightX, textY - 1, highlightX + highlightW, textY + this.font.lineHeight, 0xFF3399FF);
                    
                    // 分段绘制文字
                    g.drawString(this.font, beforeHighlight, textX, textY, 0xFF222222, false);
                    g.drawString(this.font, highlighted, highlightX, textY, 0xFFFFFFFF, false);
                    g.drawString(this.font, afterHighlight, highlightX + highlightW, textY, 0xFF222222, false);
                } else {
                    // 选中范围无效，回退到普通渲染
                    g.drawString(this.font, value, textX, textY, 0xFF222222, false);
                }
            } else {
                // 没有选中，正常绘制文字
                g.drawString(this.font, value, textX, textY, 0xFF222222, false);
                
                // 绘制闪烁光标（修复位置）
                if (((System.currentTimeMillis() >> 9) & 1) == 0) {  // 每512ms切换一次
                    int cursorPos = Math.min(value.length(), Math.max(0, box.getCursorPosition()));
                    int cursorX = textX + (cursorPos > 0 ? this.font.width(value.substring(0, cursorPos)) : 0);
                    g.fill(cursorX, textY - 1, cursorX + 1, textY + this.font.lineHeight + 1, 0xFF444444);
                }
            }
        } else {
            // 未聚焦，直接绘制文字（最快路径）
            g.drawString(this.font, value, textX, textY, 0xFF222222, false);
        }
    }

    // 危险按钮渲染方法（红色样式，用于重置等危险操作）
    private void renderDangerButton(GuiGraphics g, Font font, Button btn, int mouseX, int mouseY) {
        if (btn == null || !btn.visible) return;
        
        int x = btn.getX();
        int y = btn.getY();
        int w = btn.getWidth();
        int h = btn.getHeight();
        boolean hovered = btn.isMouseOver(mouseX, mouseY);
        
        // 红色边框和背景
        int borderColor = hovered ? 0xFFC48888 : 0xFFD4A0A0;
        int bgColor = hovered ? 0xFFF2DEDE : 0xFFFFFFFF;
        int textColor = 0xFFB34242;  // 红色文字
        
        // 绘制边框
        g.fill(x, y, x + w, y + h, borderColor);
        // 绘制背景
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        // 绘制文字（居中）
        Component message = btn.getMessage();
        int textWidth = font.width(message);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - font.lineHeight) / 2;
        g.drawString(font, message, textX, textY, textColor, false);
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
        
        // 计算面板高度（横向布局：宽度 > 高度）
        // 先计算面板宽度
        int tempPanelX = tabBarX - 2;
        int tempPanelW = (right + 8 + SCROLL_TRACK_W + 4) - tempPanelX;
        int maxPanelH = (int)(tempPanelW / 1.37);  // HTML比例 820:600 ≈ 1.37:1
        int availablePanelH = this.height - 8 - 8;  // 上下各留8px边距
        int panelH = Math.min(maxPanelH, availablePanelH);
        
        int buttonY = 8 + panelH - 10 - INPUT_HEIGHT;  // 按钮位于面板底部
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

        // 卡片布局常量
        int CARD_PADDING = 10;  // 卡片内边距
        int CARD_GAP = 8;  // 卡片之间的间距（恢复为HTML中的8px）
        int TITLE_LINE_HEIGHT = font.lineHeight;  // 标题行高度（约9px）
        int SUBTITLE_LINE_HEIGHT = font.lineHeight;  // 副标题行高度（约9px）
        // 带开关的卡片内容高度：取开关高度和文字高度的最大值
        int TOGGLE_CARD_CONTENT_H = Math.max(TOGGLE_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT);
        int TOGGLE_CARD_CONTENT_H_NO_SUB = Math.max(TOGGLE_HEIGHT, TITLE_LINE_HEIGHT);
        
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
        
        // 更好的延迟卡片（有副标题+开关）：高度取开关高度和文字高度的最大值
        int betterPingRowY = y;
        int betterPingCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
        y = betterPingRowY + betterPingCardHeight + CARD_GAP;
        
        // 在线时长卡片
        int onlineDurationRowY = y;
        int onlineDurationCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
        y = onlineDurationRowY + onlineDurationCardHeight + CARD_GAP;
        
        // 称号功能卡片
        int titleRowY = y;
        int titleCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
        y = titleRowY + titleCardHeight + CARD_GAP;
        
        // 玩家血量卡片
        int healthDisplayRowY = y;
        int healthDisplayCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
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
        int footerTpsCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
        y = footerTpsRowY + footerTpsCardHeight + CARD_GAP;
        
        // MSPT 信息卡片
        int footerMsptRowY = y;
        int footerMsptCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
        y = footerMsptRowY + footerMsptCardHeight + CARD_GAP;
        
        // 在线人数卡片
        int footerOnlineRowY = y;
        int footerOnlineCardHeight = CARD_PADDING + TOGGLE_CARD_CONTENT_H + CARD_PADDING;
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
            int resetCardHeight = CARD_PADDING * 2 + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;  // 增加高度以容纳确认/取消按钮
            themeY = customResetRowY + resetCardHeight + CARD_GAP;
            
            // 动画效果卡片（只有标题 + 开关，无副标题）
            customAnimationRowY = themeY;
            int animToggleCardH = CARD_PADDING + TITLE_LINE_HEIGHT + CARD_PADDING;
            // 确保卡片高度能容纳 toggle（14px）
            animToggleCardH = Math.max(animToggleCardH, CARD_PADDING + 14 + CARD_PADDING);
            themeY = customAnimationRowY + animToggleCardH + CARD_GAP;

            // 动画速率卡片（标题 + 副标题 + 速率按钮）
            customAnimSpeedRowY = themeY;
            int animSpeedCardH = CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + CARD_PADDING;
            animSpeedCardH = Math.max(animSpeedCardH, CARD_PADDING + 18 + CARD_PADDING);
            themeY = customAnimSpeedRowY + animSpeedCardH + CARD_GAP;
            
            // 颜色配置：每个颜色项独立卡片（固定高度，不展开/折叠）
            java.util.List<Integer> borderColors = theme.customThemeConfig != null ? 
                theme.customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
            
            int swatchSize = 24;  // 色块大小（缩小）
            int previewH = 8;     // 预览块高度
            int contentGap = 8;   // 标题与内容之间的间距
            int previewGap = 8;   // 颜色行与预览块之间的间距
            
            // 计算卡片高度（标题 + 间距 + 色块行 + 间距 + 预览块）
            int colorCardH = CARD_PADDING + TITLE_LINE_HEIGHT + contentGap + swatchSize + previewGap + previewH + CARD_PADDING;
            
            // 背景颜色卡片
            customBgColorRowY = themeY;
            themeY += colorCardH + CARD_GAP;
            
            // 外层边框颜色卡片
            customOuterBorderRowY = themeY;
            themeY += colorCardH + CARD_GAP;
            
            // 边框颜色大卡片（一个卡片包含所有边框颜色项，添加按钮在右上角）
            // 高度 = padding + 标题行 + 副标题 + 间距 + n×(itemH+gap) + padding
            customAddBorderColorRowY = themeY;
            int borderItemH = THEME_OPTION_HEIGHT + 12;  // 每个颜色行高度（含上下内边距，增加4像素）
            int borderItemGap = 8;  // 行间距
            int borderCardInnerH = TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;  // 标题+副标题
            borderCardInnerH += borderColors.size() * (borderItemH + borderItemGap);
            // 添加按钮现在在右上角，不需要为底部按钮预留空间
            int borderBigCardH = CARD_PADDING + borderCardInnerH + CARD_PADDING;
            themeY += borderBigCardH + SECTION_GAP;
        }
        
        // 血量显示分区
        int healthSectionHeaderY = themeY;
        themeY += SECTION_HEADER_HEIGHT;
        
        // 显示效果卡片
        int healthModeRowY = themeY;
        int healthModeCardHeight = CARD_PADDING + Math.max(INPUT_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT) + CARD_PADDING;
        themeY = healthModeRowY + healthModeCardHeight + SECTION_GAP;
        
        // 布局分列分区
        int layoutSectionHeaderY = themeY;
        themeY += SECTION_HEADER_HEIGHT;
        
        // 启用分列卡片
        int layoutEnabledRowY = themeY;
        int layoutEnabledCardHeight = CARD_PADDING + Math.max(TOGGLE_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT) + CARD_PADDING;
        themeY = layoutEnabledRowY + layoutEnabledCardHeight + CARD_GAP;
        
        // 展示列数卡片
        int layoutColumnsRowY = themeY;
        int layoutColumnsCardHeight = CARD_PADDING + Math.max(INPUT_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT) + CARD_PADDING;
        themeY = layoutColumnsRowY + layoutColumnsCardHeight + CARD_GAP;
        
        // 展示行数卡片
        int layoutRowsRowY = themeY;
        int layoutRowsCardHeight = CARD_PADDING + Math.max(INPUT_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT) + CARD_PADDING;
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
        int permCardHeight = CARD_PADDING + Math.max(TOGGLE_HEIGHT, TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT) + CARD_PADDING;
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
            playerListContentHeight = INPUT_HEIGHT;  // 与单个标签高度一致
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
        int CONTENT_BOTTOM_PADDING = 16;  // 内容区域底部留白，避免最后一个卡片紧贴按钮栏
        if (activeTab == ConfigTab.PAGE_CONFIG) contentHeight = footerRowY + CONTENT_BOTTOM_PADDING;
        else if (activeTab == ConfigTab.PERMISSIONS) contentHeight = permissionsContentHeight + CONTENT_BOTTOM_PADDING;
        else contentHeight = themeY + CONTENT_BOTTOM_PADDING;  // 主题Tab使用themeY作为内容高度
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
            panelCenterX + 5, panelCenterX - buttonWidth - 5, tabBarX,
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 优先处理HEX输入框的键盘事件
        if (activeTab == ConfigTab.THEME && "custom".equals(theme.selectedThemeId)) {
            for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                if (hexBox.visible && hexBox.isFocused()) {
                    return hexBox.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 优先处理HEX输入框的字符输入
        if (activeTab == ConfigTab.THEME && "custom".equals(theme.selectedThemeId)) {
            for (net.minecraft.client.gui.components.EditBox hexBox : theme.customBorderHexInputs) {
                if (hexBox.visible && hexBox.isFocused()) {
                    return hexBox.charTyped(codePoint, modifiers);
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
}

