package com.poso.neotab.client.screen;

import com.poso.neotab.client.gui.AEStyleRenderer;
import com.poso.neotab.client.widget.ColorPickerWidget;
import com.poso.neotab.client.widget.ImprovedRichTextEditBox;
import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import com.poso.neotab.theme.TabThemeRegistry;
import com.poso.neotab.network.payload.SaveConfigPayload;
import com.poso.neotab.network.payload.SavePlayerConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** NeoTab 闂佹澘绉堕悿鍡涙偩瀹€鍕〃闁挎稑鑻稊蹇旂瑹?Tab 闁哄秴绻愰崹蹇涘箲椤厾鐟忓☉鎿冧簻閸ㄥ酣宕犻幁鎺嗗亾?*/
public class NeoTabConfigScreen extends Screen {
    // Tab 闁哄鐭俊?
    /**
     * Screen mode.
     * ADMIN: full config + permissions tab (only visible to admins).
     * PLAYER: personal customization, restricted by policy (locked widgets shown greyed out).
     */
    public enum ScreenMode { ADMIN, PLAYER }

    private enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme"),
        PERMISSIONS("screen.neotab.tab.permissions");  // admin only
        final String langKey;
        ConfigTab(String langKey) { this.langKey = langKey; }
        Component label() { return Component.translatable(langKey); }
    }

    // 閻㈩垰鍟惇顒傛暜閹间礁锟?
    private static final int MAX_CONTENT_WIDTH      = 360;
    private static final int CONTENT_SIDE_PADDING   = 32;
    private static final int ROW_HEIGHT              = 24;
    private static final int INPUT_HEIGHT            = 20;
    private static final int TITLE_INPUT_HEIGHT      = 60;   // 锟?40闁挎稑鑻·鍐礉?1/2
    private static final int MULTILINE_INPUT_HEIGHT  = 60;   // 锟?40闁挎稑鑻·鍐礉?1/2
    private static final int TOGGLE_WIDTH            = 56;
    private static final int LAYOUT_BUTTON_WIDTH     = 80;  // 布局按钮使用更大的宽度
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
    /** 闁告劕鎳庨鎰板礌濞差亗鈧﹪鏌堥妸銉ユ暥閺夊牏顢婄粣娑㈡晬瀹€鍐惧敤"濡炪倕鐖奸崕瀛樼┍閳╁啩锟?闁哄秴娲。鑺ョ▔鎼存繄鐟愰弶鍫ｎ潐椤㈠绌卞┑鍥х槷闂傚倻顥愮粣娑㈠Υ?*/
    private static final int CONTENT_TOP_PADDING     = 8;
    // Tab 闁哄秴绻愰悥鍫曟煂?
    private static final int TAB_BAR_WIDTH           = 72;
    private static final int TAB_CONTENT_GAP          = 8;   // Tab 闁哄秴绻嬬粭宀勫礃閸涱収鍟囬柛鏍濞堟垿姊荤壕瀣崺
    private static final int TAB_BUTTON_HEIGHT        = 24;
    private static final int TAB_BUTTON_GAP           = 4;
    private static final int TAB_BUTTON_LEFT_PADDING  = 6;   // Tab 闁圭顦甸幐铏▔鎼粹€茬閺夊牏顢婄粣娑㈡儍閸曨垱锛熼悹?
    // 濡増绮忔竟濠勬暜閹间礁娅ら柨娑樼墔缁绘岸鎮惧▎鎴炴殢濞存粌楠搁崥瀣偓鍦缁辨繄鈧湱鍋ゅ顖氥€掗崣澶屽帬鐎规瓕灏缓鑲╃矓鐠囨彃锟?AEStyleRenderer锟?
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
    // Tab 闁哄倸娲ら悺褎锛愬鍡楊棌闁挎稑鐗婄缓鍝勶拷?婵炴潙鎳撴竟濠囨晬瀹€鍕婵犵鍋撴繛?婵烇綀绮炬竟濠囨晬?
    private static final int TAB_TEXT_ACTIVE      = 0xFFE8ECF0;  // 婵犵鍋撴繛韫串缁辨澘霉閸涙澘锟?
    private static final int TAB_TEXT_INACTIVE    = 0xFF3A3A3A;  // 闂傚牏鍋炵缓鍝劽烘导娆戠獥婵烇綀绮炬竟?

    // 闁绘鍩栭埀顑跨閻⊙冣枔?
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
    private CycleButton<Boolean> healthDisplayEnabled;
    private CycleButton<HealthDisplayMode> healthDisplayMode;
    private final List<Button> themeOptionButtons = new ArrayList<>();
    private final List<String> themeOptionIds = new ArrayList<>();
    private String selectedThemeId = "vanilla";
    // 布局分列配置组件
    private CycleButton<Boolean> layoutEnabledToggle;
    private Button layoutColumnsButton;
    private Button layoutRowsButton;
    // 自定义主题配置组件
    private Button customBackgroundColorButton;  // 背景颜色按钮（可选中）
    private Button customBorderOuterFactorButton;  // 外边框深度按钮
    private CycleButton<Boolean> customAnimationToggle;  // 动画开关
    private Button customAnimationSpeedButton;           // 动画速率按钮
    private Button resetToDefaultButton;  // 重置默认按钮
    private Button resetConfirmButton;  // 重置确认按钮
    private Button resetCancelButton;  // 重置取消按钮
    private boolean showResetConfirmation = false;  // 是否显示重置确认按钮
    private final List<Button> customBorderColorButtons = new ArrayList<>();  // 边框颜色按钮列表（可选中）
    private Button addCustomBorderColorButton;  // 添加边框颜色按钮
    private com.poso.neotab.theme.CustomThemeConfig customThemeConfig;  // 自定义主题配置
    // 嵌入式颜色选择器
    private ColorPickerWidget embeddedColorPicker;  // 嵌入式颜色选择器（始终显示在右侧）
    private int cachedColorPickerWidth = 158;   // init() 中计算的实际颜色选择器宽度（含缩放）
    private int cachedCustomButtonWidth = 60;   // init() 中计算的左侧按钮宽度
    private String currentSelectedColorType = null;  // 当前选中的颜色类型：null, "background", "border_0", "border_1", ...
    private int currentSelectedBorderIndex = -1;  // 当前选中的边框颜色索引
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

    /** Current screen mode (ADMIN or PLAYER). */
    private final ScreenMode screenMode;
    /** Effective customize policy for this player (used in PLAYER mode). */
    private final PlayerCustomizePolicy policy;
    /** Player's personal config (used in PLAYER mode). */
    private final com.poso.neotab.config.PlayerTabConfig personalConfig;

    // ── Permissions tab widgets ───────────────────────────────────────────────
    /** 权限开关列表（15个，对应 PlayerCustomizePolicy 的所有字段）。 */
    private final List<CycleButton<Boolean>> globalPolicyToggles = new ArrayList<>();
    /** 目标模式切换按钮：所有玩家 / 指定玩家。 */
    private Button permTargetModeButton;
    /** 当前目标模式：true = 指定玩家，false = 所有玩家。 */
    private boolean permTargetIsPlayer = false;
    /** 玩家名称输入框（指定玩家模式下可用）。 */
    private EditBox playerSearchBox;
    /** 添加按钮。 */
    private Button permAddButton;
    /** 联想下拉列表中的候选玩家名（最多5个）。 */
    private final List<String> playerSuggestions = new ArrayList<>();
    /** 已添加到指定玩家列表的 UUID → 名称映射（保持插入顺序）。 */
    private final java.util.LinkedHashMap<java.util.UUID, String> targetPlayers = new java.util.LinkedHashMap<>();
    /** 指定玩家列表中每个玩家的删除按钮。 */
    private final List<Button> targetPlayerRemoveButtons = new ArrayList<>();
    /** 当前正在编辑的玩家 UUID（null = 所有玩家模式）。 */
    private java.util.UUID editingPlayerUUID = null;
    /** 个人策略开关（与 globalPolicyToggles 共用，根据 permTargetIsPlayer 决定读写哪套数据）。 */
    private final List<CycleButton<Boolean>> personalPolicyToggles = new ArrayList<>();
    /** 权限配置保存按钮。 */
    private Button permSaveButton;

    /** Admin constructor (full config, no restrictions). */
    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
        this.screenMode = ScreenMode.ADMIN;
        this.policy = com.poso.neotab.permission.PlayerCustomizePolicy.unlocked();
        this.personalConfig = null;
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
    }

    @Override
    protected void init() {
        clearWidgets();
        this.themeOptionButtons.clear();
        this.themeOptionIds.clear();
        this.globalPolicyToggles.clear();
        this.personalPolicyToggles.clear();
        this.playerSuggestions.clear();
        this.targetPlayerRemoveButtons.clear();
        this.permSaveButton = null;
        // 重置 activeTab 到合法值（防止 resize 时 PERMISSIONS tab 在 PLAYER 模式下残留）
        if (activeTab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) {
            activeTab = ConfigTab.PAGE_CONFIG;
        }
        // In PLAYER mode, use personal config values where available; fall back to server config
        com.poso.neotab.config.TabConfig effectiveInit = (screenMode == ScreenMode.PLAYER && personalConfig != null)
            ? personalConfig.mergeInto(initialConfig, policy)
            : initialConfig;
        this.selectedThemeId = TabThemeRegistry.get(effectiveInit.tabTheme()).id();
        Layout layout = buildLayout();
        initPageConfigWidgets(layout, effectiveInit);
        initThemeWidgets(layout);
        if (screenMode == ScreenMode.ADMIN) {
            initPermissionsWidgets(layout);
        }
        initFooterAndFinalize(layout, effectiveInit);
    }


    private void initPageConfigWidgets(Layout layout, com.poso.neotab.config.TabConfig cfg) {
        this.topTitleEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.topTitleEnabled()));
        applyPolicyToWidget(topTitleEnabled, policy.allowTopTitleToggle());
        this.topTitleInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), TITLE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.top.title")));
        this.topTitleInput.setMaxVisibleLength(TabConfig.MAX_TOP_TITLE_LENGTH);
        this.topTitleInput.setValue(cfg.topTitleText());
        applyPolicyToWidget(topTitleInput, policy.allowTopTitleEdit());
        this.topContentEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.topContentEnabled()));
        applyPolicyToWidget(topContentEnabled, policy.allowTopContentToggle());
        this.topContentInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), MULTILINE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.top.content")));
        this.topContentInput.setMaxVisibleLength(TabConfig.MAX_TOP_CONTENT_LENGTH);
        this.topContentInput.setAutoResize(true);
        this.topContentInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.topContentInput.setValue(cfg.topContentText());
        applyPolicyToWidget(topContentInput, policy.allowTopContentEdit());
        this.betterPingEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.betterPingEnabled()));
        applyPolicyToWidget(betterPingEnabled, policy.allowPingDisplayToggle());
        this.onlineDurationEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.onlineDurationEnabled()));
        applyPolicyToWidget(onlineDurationEnabled, policy.allowDurationToggle());
        this.titleEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.titleEnabled()));
        applyPolicyToWidget(titleEnabled, policy.allowTitleToggle());
        this.healthDisplayEnabled = addRenderableWidget(newToggle(layout.toggleX(), cfg.healthDisplayEnabled()));
        applyPolicyToWidget(healthDisplayEnabled, policy.allowHealthDisplayToggle());
        this.healthDisplayMode = addRenderableWidget(newHealthModeButton(layout.toggleX(), cfg.healthDisplayMode()));
        applyPolicyToWidget(healthDisplayMode, policy.allowHealthModeChange());
        // 布局分列配置按钮 - 并排显示，无文字标签
        com.poso.neotab.config.TabLayoutConfig layoutCfg = com.poso.neotab.config.TabLayoutConfig.get();
        // 布局分列开关 - 放在 section header 行右侧（toggleX 位置）
        this.layoutEnabledToggle = addRenderableWidget(
            CycleButton.onOffBuilder(layoutCfg.isEnabled())
                .displayOnlyValue()
                .create(layout.toggleX(), 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                    (button, enabled) -> {
                        com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                        layoutCfgInner.setEnabled(enabled);
                        com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);
                        // 同步列/行按钮的可交互状态
                        if (layoutColumnsButton != null) layoutColumnsButton.active = enabled;
                        if (layoutRowsButton    != null) layoutRowsButton.active    = enabled;
                    }));
        this.layoutColumnsButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.layout.columns", layoutCfg.getColumns()),
                button -> {
                    com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = layoutCfgInner.getColumns();
                    int maxColumns = layoutCfgInner.getMaxColumns();
                    // 循环递增，超过最大值时回到1
                    int next = current >= maxColumns ? 1 : current + 1;
                    layoutCfgInner.setColumns(next);
                    com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);
                    button.setMessage(Component.translatable("screen.neotab.layout.columns", next));
                })
            .bounds(layout.left(), 0, LAYOUT_BUTTON_WIDTH, INPUT_HEIGHT)
            .build());
        this.layoutColumnsButton.active = layoutCfg.isEnabled();
        this.layoutRowsButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.layout.rows", layoutCfg.getRowsPerColumn()),
                button -> {
                    com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = layoutCfgInner.getRowsPerColumn();
                    int maxRows = layoutCfgInner.getMaxRows();
                    // 循环：5 → 10 → 15 → 20 → 25 → 30 → 35 → 40 → 5
                    // 但不超过当前最大值
                    int next = switch (current) {
                        case 5  -> Math.min(10, maxRows);
                        case 10 -> Math.min(15, maxRows);
                        case 15 -> Math.min(20, maxRows);
                        case 20 -> Math.min(25, maxRows);
                        case 25 -> Math.min(30, maxRows);
                        case 30 -> Math.min(35, maxRows);
                        case 35 -> Math.min(40, maxRows);
                        case 40 -> 5;
                        default -> 5;
                    };
                    // 如果next等于current（说明已达到最大值），则回到5
                    if (next == current && current >= maxRows) {
                        next = 5;
                    }
                    layoutCfgInner.setRowsPerColumn(next);
                    com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);
                    button.setMessage(Component.translatable("screen.neotab.layout.rows", next));
                })
            .bounds(layout.left(), 0, LAYOUT_BUTTON_WIDTH, INPUT_HEIGHT)
            .build());
        this.layoutRowsButton.active = layoutCfg.isEnabled();
        for (String themeId : TabThemeRegistry.ids()) {
            Button optionButton = addRenderableWidget(Button.builder(
                    Component.translatable("screen.neotab.theme." + themeId),
                    button -> {
                        this.selectedThemeId = themeId;
                        syncTabWidgetVisibility();  // 切换主题时更新可见性
                    })
                .bounds(layout.left(), 0, layout.themeSelectorWidth() - THEME_LIST_INSET * 2, THEME_OPTION_HEIGHT)
                .build());
            this.themeOptionButtons.add(optionButton);
            this.themeOptionIds.add(themeId);
        }
        
        // 加载自定义主题配置
    }

    private void initThemeWidgets(Layout layout) {
        this.customThemeConfig = com.poso.neotab.theme.CustomThemeManager.get();
        
        // 计算按钮宽度和颜色选择器位置，确保不超出屏幕右边界
        int availableWidth = layout.themeSelectorWidth() - THEME_LIST_INSET * 2;
        
        // 左侧按钮宽度固定为可用宽度的一半（不受颜色选择器影响）
        int customButtonWidth = Math.max(60, availableWidth / 2);
        
        // 颜色选择器只使用按钮右边的剩余空间
        int colorPickerGap = 10;
        int pickerStartX = layout.left() + THEME_LIST_INSET + customButtonWidth + colorPickerGap;
        int contentRightBoundary = layout.left() + layout.themeSelectorWidth() - THEME_LIST_INSET;
        int spaceForColorPicker = contentRightBoundary - pickerStartX;
        
        // 根据可用空间计算颜色选择器的缩放比例
        int baseColorPickerWidth = 158;  // 基础宽度（scale=1.0时）
        float maxScale = 1.0f;
        float minScale = 0.6f;
        float colorPickerScale = Math.max(minScale, Math.min(maxScale, (float) spaceForColorPicker / baseColorPickerWidth));
        int scaledColorPickerWidth = (int) (baseColorPickerWidth * colorPickerScale);
        
        // 缓存到字段，供 applyWidgetLayout 使用（避免两处计算不一致）
        this.cachedColorPickerWidth = scaledColorPickerWidth;
        this.cachedCustomButtonWidth = customButtonWidth;
        
        // 自定义主题配置组件
        // 背景颜色按钮（可选中）
        this.customBackgroundColorButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.background_color"),
                button -> {
                    // 选中背景颜色
                    currentSelectedColorType = "background";
                    currentSelectedBorderIndex = -1;
                    if (embeddedColorPicker != null) {
                        embeddedColorPicker.setColor(customThemeConfig.getBackgroundColor());
                    }
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        
        // 外层边框颜色按钮（可选中，与背景颜色按钮交互方式完全一致）
        this.customBorderOuterFactorButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.border_outer_color"),
                button -> {
                    currentSelectedColorType = "outer_border";
                    currentSelectedBorderIndex = -1;
                    if (embeddedColorPicker != null) {
                        embeddedColorPicker.setColor(customThemeConfig.getBorderOuterColor());
                    }
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        
        // 动画开关（宽度缩小为原来的一半，与速率按钮并排）
        int animHalfW = (customButtonWidth - 4) / 2;  // 4px 为两按钮间距
        this.customAnimationToggle = addRenderableWidget(CycleButton.booleanBuilder(
                Component.translatable("screen.neotab.custom_theme.animation.on"),
                Component.translatable("screen.neotab.custom_theme.animation.off"))
            .withInitialValue(customThemeConfig.isAnimationEnabled())
            .create(layout.left(), 0, animHalfW, THEME_OPTION_HEIGHT,
                    Component.translatable("screen.neotab.custom_theme.animation"),
                    (btn, value) -> {
                        customThemeConfig.setAnimationEnabled(value);
                        com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    }));

        // 动画速率按钮（紧跟动画开关右侧，占据剩余空间）
        this.customAnimationSpeedButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.animation_speed",
                        customThemeConfig.getAnimationSpeed()),
                button -> {
                    int cur = customThemeConfig.getAnimationSpeed();
                    int next = cur >= 3 ? 1 : cur + 1;
                    customThemeConfig.setAnimationSpeed(next);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    button.setMessage(Component.translatable(
                            "screen.neotab.custom_theme.animation_speed", next));
                })
            .bounds(layout.left(), 0, animHalfW, THEME_OPTION_HEIGHT)
            .build());
        
        // 重置默认按钮
        this.resetToDefaultButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_to_default"),
                button -> {
                    // 显示确认/取消按钮
                    showResetConfirmation = true;
                    syncTabWidgetVisibility();
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        
        // 重置确认按钮（初始隐藏）
        this.resetConfirmButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_confirm"),
                button -> {
                    // 执行重置操作
                    this.customThemeConfig = com.poso.neotab.theme.CustomThemeConfig.defaults();
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    
                    // 隐藏确认按钮
                    showResetConfirmation = false;
                    
                    // 重新初始化整个界面以应用所有更改
                    this.init();
                })
            .bounds(layout.left(), 0, 50, 18)  // 固定宽度50，高度18
            .build());
        
        // 重置取消按钮（初始隐藏）
        this.resetCancelButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_cancel"),
                button -> {
                    // 隐藏确认按钮
                    showResetConfirmation = false;
                    syncTabWidgetVisibility();
                })
            .bounds(layout.left(), 0, 50, 18)  // 固定宽度50，高度18
            .build());
        
        // 创建嵌入式颜色选择器（使用计算出的缩放比例）
        // 初始X位置：按钮右边 + gap（applyWidgetLayout 会精确设置）
        int pickerX = layout.left() + THEME_LIST_INSET + customButtonWidth + colorPickerGap;
        int pickerY = 0;  // 稍后在 applyWidgetLayout 中设置
        this.embeddedColorPicker = new ColorPickerWidget(pickerX, pickerY, this.font, 
            0xFFFFFFFF,  // 默认显示白色，只有选中颜色按钮后才显示对应颜色
            color -> {
                // 颜色变化时的回调 - 根据当前选中的项更新颜色
                if ("background".equals(currentSelectedColorType)) {
                    customThemeConfig.setBackgroundColor(color);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                } else if ("outer_border".equals(currentSelectedColorType)) {
                    customThemeConfig.setBorderOuterColor(color);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                } else if (currentSelectedColorType != null && currentSelectedColorType.startsWith("border_")) {
                    if (currentSelectedBorderIndex >= 0) {
                        List<Integer> colors = new ArrayList<>(customThemeConfig.getBorderColors());
                        if (currentSelectedBorderIndex < colors.size()) {
                            colors.set(currentSelectedBorderIndex, color);
                            customThemeConfig.setBorderColors(colors);
                            com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                        }
                    }
                }
            }, colorPickerScale);  // 使用计算出的缩放比例
        addRenderableWidget(this.embeddedColorPicker);
        
        // 不默认选中任何颜色
        currentSelectedColorType = null;
        currentSelectedBorderIndex = -1;
        
    }

    // ── Policy helper ─────────────────────────────────────────────────────────

    /**
     * Apply policy restriction to a widget.
     * If allowed = false (PLAYER mode and policy locked), the widget is disabled and
     * a tooltip is shown explaining it is controlled by the server administrator.
     * In ADMIN mode, widgets are always active regardless of policy.
     */
    private void applyPolicyToWidget(AbstractWidget widget, boolean allowed) {
        if (screenMode == ScreenMode.ADMIN) return; // admin always has full access
        if (!allowed) {
            widget.active = false;
            widget.setTooltip(Tooltip.create(
                Component.translatable("screen.neotab.locked_by_server")));
        }
    }

    // ── Permissions tab ───────────────────────────────────────────────────────

    /**
     * Initialize the permissions configuration tab widgets.
     * Only called in ADMIN mode.
     *
     * Layout:
     *   Section: Global Policy
     *     [toggle] Top title toggle
     *     [toggle] Top title edit
     *     ... (15 policy fields total)
     *   Section: Personal Policy
     *     [search box] Player name  [Search button]
     *     (if player found: same 15 toggles for that player)
     */
    /**
     * 权限配置界面初始化（重构版）。
     *
     * 顶部区域：
     *   [所有玩家/指定玩家 切换] [输入框（指定玩家时可用）] [添加]
     *   [指定玩家列表（指定玩家模式下显示）]
     *
     * 权限列表：
     *   每行两个权限项，每项用边框背景围起来，格式：[名称] [ON/OFF]
     */
    private void initPermissionsWidgets(Layout layout) {
        com.poso.neotab.permission.PlayerCustomizePolicy global = initialConfig.globalPolicy();

        // ── 目标模式切换按钮 ──────────────────────────────────────────────────
        this.permTargetModeButton = addRenderableWidget(Button.builder(
            Component.translatable(permTargetIsPlayer
                ? "screen.neotab.policy.mode_player"
                : "screen.neotab.policy.mode_all"),
            btn -> {
                permTargetIsPlayer = !permTargetIsPlayer;
                btn.setMessage(Component.translatable(permTargetIsPlayer
                    ? "screen.neotab.policy.mode_player"
                    : "screen.neotab.policy.mode_all"));
                // 切换到"所有玩家"时禁用输入框
                if (playerSearchBox != null) {
                    playerSearchBox.active = permTargetIsPlayer;
                    playerSearchBox.setEditable(permTargetIsPlayer);
                    if (!permTargetIsPlayer) {
                        playerSearchBox.setValue("");
                        playerSearchBox.setHint(Component.translatable("screen.neotab.policy.input_disabled"));
                    } else {
                        playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));
                    }
                }
                if (permAddButton != null) permAddButton.active = permTargetIsPlayer;
                // 切换时重新加载开关值
                loadPolicyToggles();
                syncTabWidgetVisibility();
                Layout l = buildLayout();
                applyWidgetLayout(l);
            })
            .bounds(layout.left(), 0, 80, INPUT_HEIGHT)
            .build());
        this.permTargetModeButton.visible = false;

        // ── 玩家名称输入框（联想搜索）────────────────────────────────────────
        int modeButtonWidth = 80;
        int addButtonWidth  = 40;
        int searchBoxWidth  = layout.contentWidth() - modeButtonWidth - addButtonWidth - 12;
        this.playerSearchBox = addRenderableWidget(
            new EditBox(this.font, layout.left() + modeButtonWidth + 4, 0,
                searchBoxWidth, INPUT_HEIGHT,
                Component.translatable("screen.neotab.policy.search_hint")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));
        this.playerSearchBox.active = permTargetIsPlayer;
        this.playerSearchBox.setEditable(permTargetIsPlayer);
        this.playerSearchBox.visible = false;
        // 输入时更新联想列表
        this.playerSearchBox.setResponder(text -> {
            playerSuggestions.clear();
            if (!text.isBlank()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    String lower = text.toLowerCase();
                    mc.getConnection().getOnlinePlayers().stream()
                        .map(p -> p.getProfile().getName())
                        .filter(name -> name.toLowerCase().contains(lower))
                        .limit(5)
                        .forEach(playerSuggestions::add);
                }
            }
        });

        // ── 添加按钮 ──────────────────────────────────────────────────────────
        this.permAddButton = addRenderableWidget(Button.builder(
            Component.translatable("screen.neotab.policy.add"),
            btn -> {
                if (!permTargetIsPlayer || playerSearchBox == null) return;
                String name = playerSearchBox.getValue().trim();
                if (name.isEmpty()) return;
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    mc.getConnection().getOnlinePlayers().stream()
                        .filter(p -> p.getProfile().getName().equalsIgnoreCase(name))
                        .findFirst()
                        .ifPresent(p -> {
                            java.util.UUID uuid = p.getProfile().getId();
                            if (!targetPlayers.containsKey(uuid)) {
                                targetPlayers.put(uuid, p.getProfile().getName());
                                rebuildTargetPlayerButtons(buildLayout());
                                syncTabWidgetVisibility();
                                applyWidgetLayout(buildLayout());
                            }
                            playerSearchBox.setValue("");
                            playerSuggestions.clear();
                        });
                }
            })
            .bounds(layout.left() + modeButtonWidth + 4 + searchBoxWidth + 4, 0, addButtonWidth, INPUT_HEIGHT)
            .build());
        this.permAddButton.active = permTargetIsPlayer;
        this.permAddButton.visible = false;

        // ── 权限开关（15个，每行两个）────────────────────────────────────────
        boolean[] globalValues = {
            global.allowTopTitleToggle(),    global.allowTopTitleEdit(),
            global.allowTopContentToggle(),  global.allowTopContentEdit(),
            global.allowPingDisplayToggle(), global.allowDurationToggle(),
            global.allowTitleToggle(),       global.allowHealthDisplayToggle(),
            global.allowHealthModeChange(),  global.allowFooterCustomEdit(),
            global.allowFooterTpsToggle(),   global.allowFooterMsptToggle(),
            global.allowFooterOnlineToggle(), global.allowThemeChange(),
            global.allowRefreshIntervalChange()
        };
        for (boolean val : globalValues) {
            CycleButton<Boolean> toggle = addRenderableWidget(
                CycleButton.onOffBuilder(val)
                    .displayOnlyValue()
                    .create(layout.toggleX(), 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                        (btn, v) -> { /* value read on save */ }));
            toggle.visible = false;
            globalPolicyToggles.add(toggle);
        }

        // ── 保存权限按钮 ──────────────────────────────────────────────────────
        this.permSaveButton = addRenderableWidget(Button.builder(
            Component.translatable("screen.neotab.policy.save"),
            btn -> savePermissions())
            .bounds(layout.left(), 0, 120, INPUT_HEIGHT)
            .build());
        this.permSaveButton.visible = false;
    }

    /**
     * 保存权限配置并发送到服务端。
     * 只更新策略字段，不改变其他 TAB 配置。
     */
    private void savePermissions() {
        com.poso.neotab.config.TabConfig config = new com.poso.neotab.config.TabConfig(
            initialConfig.topTitleEnabled(),
            initialConfig.topTitleText(),
            initialConfig.topContentEnabled(),
            initialConfig.topContentText(),
            initialConfig.betterPingEnabled(),
            initialConfig.onlineDurationEnabled(),
            initialConfig.titleEnabled(),
            initialConfig.healthDisplayEnabled(),
            initialConfig.healthDisplayMode(),
            initialConfig.tabTheme(),
            initialConfig.footerCustomText(),
            initialConfig.footerTpsEnabled(),
            initialConfig.footerMsptEnabled(),
            initialConfig.footerOnlineEnabled(),
            initialConfig.refreshIntervalTicks(),
            buildGlobalPolicyFromToggles(),
            buildPlayerPoliciesFromToggles()
        ).sanitized();
        PacketDistributor.sendToServer(new SaveConfigPayload(config));
        // 不关闭界面，让管理员继续操作
        net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(
            net.minecraft.network.chat.Component.translatable("message.neotab.permissions_saved"));
    }

    /**
     * 重建指定玩家列表的删除按钮。
     */
    private void rebuildTargetPlayerButtons(Layout layout) {
        for (Button btn : targetPlayerRemoveButtons) removeWidget(btn);
        targetPlayerRemoveButtons.clear();
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        for (java.util.UUID uuid : targetPlayers.keySet()) {
            Button removeBtn = addRenderableWidget(Button.builder(
                Component.literal("×"),
                btn -> {
                    targetPlayers.remove(uuid);
                    if (uuid.equals(editingPlayerUUID)) {
                        editingPlayerUUID = null;
                        loadPolicyToggles();
                    }
                    rebuildTargetPlayerButtons(buildLayout());
                    syncTabWidgetVisibility();
                    applyWidgetLayout(buildLayout());
                })
                .bounds(layout.left(), 0, 18, INPUT_HEIGHT)
                .build());
            // 根据当前 tab 状态直接设置 visible，不统一设为 false
            removeBtn.visible = perms && permTargetIsPlayer;
            targetPlayerRemoveButtons.add(removeBtn);
        }
    }

    /**
     * 将当前目标（全局或指定玩家）的策略值加载到开关中。
     */
    private void loadPolicyToggles() {
        com.poso.neotab.permission.PlayerCustomizePolicy p;
        if (permTargetIsPlayer && editingPlayerUUID != null) {
            p = initialConfig.playerPolicies().getOrDefault(editingPlayerUUID,
                com.poso.neotab.permission.PlayerCustomizePolicy.locked());
        } else {
            p = initialConfig.globalPolicy();
        }
        boolean[] values = {
            p.allowTopTitleToggle(),    p.allowTopTitleEdit(),
            p.allowTopContentToggle(),  p.allowTopContentEdit(),
            p.allowPingDisplayToggle(), p.allowDurationToggle(),
            p.allowTitleToggle(),       p.allowHealthDisplayToggle(),
            p.allowHealthModeChange(),  p.allowFooterCustomEdit(),
            p.allowFooterTpsToggle(),   p.allowFooterMsptToggle(),
            p.allowFooterOnlineToggle(), p.allowThemeChange(),
            p.allowRefreshIntervalChange()
        };
        for (int i = 0; i < Math.min(values.length, globalPolicyToggles.size()); i++) {
            CycleButton<Boolean> toggle = globalPolicyToggles.get(i);
            if (!toggle.getValue().equals(values[i])) toggle.onPress();
        }
    }
    /**
     * Refresh personal policy toggles for the given player UUID.
     * Called when the admin searches for a player and selects them.
     */
    /**
     * 已废弃，逻辑已合并到 loadPolicyToggles()。
     * 保留此方法签名以防其他地方有调用。
     */
    private void refreshPersonalPolicyToggles(java.util.UUID uuid) {
        this.editingPlayerUUID = uuid;
        loadPolicyToggles();
        Layout layout = buildLayout();
        applyWidgetLayout(layout);
    }

    private void initFooterAndFinalize(Layout layout, com.poso.neotab.config.TabConfig cfg) {
        this.footerCustomInput = addRenderableWidget(new ImprovedRichTextMultiLineEditBox(this.font, layout.left(), 0, layout.contentWidth(), MULTILINE_INPUT_HEIGHT,
            CommonComponents.EMPTY, Component.translatable("screen.neotab.footer.custom")));
        this.footerCustomInput.setMaxVisibleLength(TabConfig.MAX_FOOTER_CUSTOM_LENGTH);
        this.footerCustomInput.setAutoResize(true);
        this.footerCustomInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.footerCustomInput.setValue(cfg.footerCustomText());
        applyPolicyToWidget(footerCustomInput, policy.allowFooterCustomEdit());
        this.footerTpsEnabled = addRenderableWidget(newLabeledToggle(layout.footerFirstColumnX(), layout.footerColumnWidth(), cfg.footerTpsEnabled(), Component.translatable("screen.neotab.footer.tps")));
        applyPolicyToWidget(footerTpsEnabled, policy.allowFooterTpsToggle());
        this.footerMsptEnabled = addRenderableWidget(newLabeledToggle(layout.footerSecondColumnX(), layout.footerColumnWidth(), cfg.footerMsptEnabled(), Component.translatable("screen.neotab.footer.mspt")));
        applyPolicyToWidget(footerMsptEnabled, policy.allowFooterMsptToggle());
        this.footerOnlineEnabled = addRenderableWidget(newLabeledToggle(layout.footerThirdColumnX(), layout.footerColumnWidth(), cfg.footerOnlineEnabled(), Component.translatable("screen.neotab.footer.online")));
        applyPolicyToWidget(footerOnlineEnabled, policy.allowFooterOnlineToggle());
        this.doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
            .bounds(layout.doneButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());
        this.cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), INPUT_HEIGHT)
            .build());
        
        // 初始化边框颜色按钮（在所有组件初始化完成后）
        rebuildCustomBorderColorButtons();
        
        // 检查并调整布局配置到当前限制范围内（处理GUI缩放变化）
        adjustLayoutConfigToCurrentLimits();
        
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : globalPolicyToggles) btn.visible = perms;
        if (permTargetModeButton != null) permTargetModeButton.visible = perms;
        if (playerSearchBox != null) playerSearchBox.visible = perms;
        if (permAddButton != null) permAddButton.visible = perms;
        for (Button btn : targetPlayerRemoveButtons) btn.visible = perms && permTargetIsPlayer;
        if (permSaveButton != null) permSaveButton.visible = perms;
        topTitleEnabled.visible       = page;
        topTitleInput.visible         = page;
        topContentEnabled.visible     = page;
        topContentInput.visible       = page;
        betterPingEnabled.visible     = page;
        onlineDurationEnabled.visible = page;
        titleEnabled.visible          = page;
        healthDisplayEnabled.visible  = page;
        footerCustomInput.visible     = page;
        footerTpsEnabled.visible      = page;
        footerMsptEnabled.visible     = page;
        footerOnlineEnabled.visible   = page;
        // Theme tab widgets
        healthDisplayMode.visible     = theme;
        if (layoutEnabledToggle != null) layoutEnabledToggle.visible = theme;
        if (layoutColumnsButton != null) layoutColumnsButton.visible = theme;
        if (layoutRowsButton    != null) layoutRowsButton.visible    = theme;
        for (Button button : this.themeOptionButtons) {
            button.visible = theme;
        }
        // 只有选中 custom 主题时才显示配置组件
        customBackgroundColorButton.visible = theme && "custom".equals(selectedThemeId);
        customBorderOuterFactorButton.visible = theme && "custom".equals(selectedThemeId);
        customAnimationToggle.visible = theme && "custom".equals(selectedThemeId);
        if (customAnimationSpeedButton != null) {
            customAnimationSpeedButton.visible = theme && "custom".equals(selectedThemeId);
        }
        if (resetToDefaultButton != null) {
            resetToDefaultButton.visible = theme && "custom".equals(selectedThemeId);
        }
        if (resetConfirmButton != null) {
            resetConfirmButton.visible = theme && "custom".equals(selectedThemeId) && showResetConfirmation;
        }
        if (resetCancelButton != null) {
            resetCancelButton.visible = theme && "custom".equals(selectedThemeId) && showResetConfirmation;
        }
        for (Button button : customBorderColorButtons) {
            button.visible = theme && "custom".equals(selectedThemeId);
        }
        if (addCustomBorderColorButton != null) {
            addCustomBorderColorButton.visible = theme && "custom".equals(selectedThemeId);
        }
        // 颜色选择器在选中 custom 主题时始终显示
        if (embeddedColorPicker != null) {
            embeddedColorPicker.visible = theme && "custom".equals(selectedThemeId);
        }
    }

    /** 闁告帒娲﹀畷鏌ュ礆閻楀牆鐦归悗?Tab闁挎稑鐭傞崳鍝ョ磾椤旂晫娉婇柛鏂诲妼閼荤喖宕氶柨瀣厐闁硅矇鍌涱偨闁告瑯鍨甸～鍡涘箑瑜嬮埀?*/
    private void switchTab(ConfigTab tab) {
        if (activeTab == tab) return;
        activeTab = tab;
        scrollOffset = 0;
        
        // 切换Tab时隐藏重置确认按钮
        if (showResetConfirmation) {
            showResetConfirmation = false;
        }
        
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
        // CycleButton 闁诡噮鍓氱拠鐐哄籍鐠佸湱绀夐柣銏や憾閵嗗妫冮姀鐙€妲遍柣鐐叉缁挳宕濋…鎺旂闂傚啫寮堕娑橆嚕閳ь剟宕楃€圭媭娼堕悹鍥跺灠閸ㄥ繘骞戦～顔剧
        if (hoveredScrollableWidget(mouseX, mouseY) instanceof CycleButton<?>) {
            if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
            setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
            return true;
        }
        // 閺夊牊鎸搁崣鍡楊浖閸℃ê浜炬繛鎼枟濡炲倿鏁嶅顒€娑ч柡鍫濐樀缁卞爼寮介崶褎韬弶鍫熸尭閸欏棗顩奸崱妤€鏁堕梺顔哄妽缁挳宕濋妸锔借拫闁告牕鎼悡娆撳箥瀹ュ牜鍞ㄩ弶鍫熸尭閸欏棗顩奸崱妤婃П闁荤偛妫寸槐?
        // 闁稿繑婀圭紞鎴﹀箚閸涱厼鏋屽☉鎾亾鐎垫澘顑囬弫杈ㄣ亜閻㈠憡妗ㄥ璺哄閹﹪鏁嶇仦鑲╃閻犲洣绶氶妴澶愭閵忥紕娉婇柛鏂诲妽缁侊箓锟?
        AbstractWidget hovered = hoveredScrollableWidget(mouseX, mouseY);
        if (hovered instanceof ImprovedRichTextMultiLineEditBox input) {
            // 閺夊牊鎸搁崣鍡楊浖閸℃鏁堕梺顔哄妽缁挳宕濋妸锔借拫闁告牕鎼悡娆撴晬濮濐摣tX()+getWidth() 锟?getX()+getWidth()+8
            boolean onInputScrollbar = mouseX >= input.getX() + input.getWidth()
                    && mouseX <= input.getX() + input.getWidth() + 8;
            if (onInputScrollbar) {
                // 閻犱讲鏅炵欢顓㈠礂閵夛富鏀遍柤濂変簻缁讳焦寰勯崟顓熷€為柛鎰嚇閸庢潙顭ㄥ顒€袟
                return input.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            // 濞戞挸绉村﹢顏呮綇閹惧啿寮虫俊妤€妫欑划鎾礉閵婏附钂嬪☉鎾愁煭缁辨繃绂嶉妶鍥╄埗濡炪倗鏁诲鏉款煥濮橆剙袟
        }
        // 濡炪倗鏁诲鏉款煥濮橆剙袟
        if (!isInsideViewport(mouseX, mouseY, layout) || layout.maxScroll() <= 0) return false;
        setScrollOffset(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), layout);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = buildLayout();
        
        // 闁瑰嚖闄勯崺鍛償閺囥垹鍔ラ柟绋款樀閹告娊锟?
        if (mouseY >= layout.buttonBarTop()) {
            // 点击了底部按钮区域，隐藏重置确认按钮
            if (showResetConfirmation) {
                showResetConfirmation = false;
                syncTabWidgetVisibility();
            }
            
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
        
        // 先检查主题选择器按钮（包括原版、自定义等）
        if (activeTab == ConfigTab.THEME) {
            for (Button themeBtn : themeOptionButtons) {
                if (themeBtn.visible && themeBtn.isMouseOver(mouseX, mouseY)) {
                    // 点击了主题按钮，隐藏重置确认按钮
                    if (showResetConfirmation) {
                        showResetConfirmation = false;
                        syncTabWidgetVisibility();
                    }
                    return themeBtn.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        
        // 然后检查左侧的自定义主题配置按钮
        if ("custom".equals(selectedThemeId) && activeTab == ConfigTab.THEME) {
            // 检查重置确认按钮
            if (resetConfirmButton != null && resetConfirmButton.visible && 
                resetConfirmButton.isMouseOver(mouseX, mouseY)) {
                return resetConfirmButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 检查重置取消按钮
            if (resetCancelButton != null && resetCancelButton.visible && 
                resetCancelButton.isMouseOver(mouseX, mouseY)) {
                return resetCancelButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 检查重置按钮
            if (resetToDefaultButton != null && resetToDefaultButton.visible && 
                resetToDefaultButton.isMouseOver(mouseX, mouseY)) {
                return resetToDefaultButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 点击了其他按钮，隐藏重置确认按钮
            if (showResetConfirmation) {
                showResetConfirmation = false;
                syncTabWidgetVisibility();
            }
            
            // 检查动画开关
            if (customAnimationToggle != null && customAnimationToggle.visible && 
                customAnimationToggle.isMouseOver(mouseX, mouseY)) {
                return customAnimationToggle.mouseClicked(mouseX, mouseY, button);
            }
            
            // 检查外边框深度按钮
            if (customBorderOuterFactorButton != null && customBorderOuterFactorButton.visible && 
                customBorderOuterFactorButton.isMouseOver(mouseX, mouseY)) {
                return customBorderOuterFactorButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 检查背景颜色按钮
            if (customBackgroundColorButton != null && customBackgroundColorButton.visible && 
                customBackgroundColorButton.isMouseOver(mouseX, mouseY)) {
                return customBackgroundColorButton.mouseClicked(mouseX, mouseY, button);
            }
            
            // 检查边框颜色按钮
            for (Button btn : customBorderColorButtons) {
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                    return btn.mouseClicked(mouseX, mouseY, button);
                }
            }
            
            // 检查添加按钮
            if (addCustomBorderColorButton != null && addCustomBorderColorButton.visible && 
                addCustomBorderColorButton.isMouseOver(mouseX, mouseY)) {
                return addCustomBorderColorButton.mouseClicked(mouseX, mouseY, button);
            }
        }
        
        // 然后检查颜色选择器的点击
        if (embeddedColorPicker != null && embeddedColorPicker.visible) {
            int pickerX = embeddedColorPicker.getX();
            int pickerY = embeddedColorPicker.getY();
            int pickerW = embeddedColorPicker.getWidth();
            int pickerH = embeddedColorPicker.getHeight();
            
            if (mouseX >= pickerX && mouseX < pickerX + pickerW &&
                mouseY >= pickerY && mouseY < pickerY + pickerH) {
                // 在颜色选择器区域内，让它处理点击
                boolean result = embeddedColorPicker.mouseClicked(mouseX, mouseY, button);
                if (result) {
                    // 设置焦点到颜色选择器，这样后续的拖动事件会正确路由
                    setFocused(embeddedColorPicker);
                    // 如果是左键点击，设置为拖动状态
                    if (button == 0) {
                        setDragging(true);
                    }
                    return true;
                }
            }
        }
        
        // 处理权限 tab 联想下拉列表点击
        if (activeTab == ConfigTab.PERMISSIONS && permTargetIsPlayer
                && playerSearchBox != null && playerSearchBox.isFocused()
                && !playerSuggestions.isEmpty() && button == 0) {
            int dropX = playerSearchBox.getX();
            int dropY = playerSearchBox.getY() + INPUT_HEIGHT + 1;
            int dropW = playerSearchBox.getWidth();
            int itemH = INPUT_HEIGHT - 2;
            for (int i = 0; i < playerSuggestions.size(); i++) {
                int itemY = dropY + 1 + i * itemH;
                if (mouseX >= dropX && mouseX < dropX + dropW
                        && mouseY >= itemY && mouseY < itemY + itemH) {
                    playerSearchBox.setValue(playerSuggestions.get(i));
                    playerSuggestions.clear();
                    return true;
                }
            }
        }

        // 处理指定玩家列表中的玩家名点击（切换编辑目标）
        if (activeTab == ConfigTab.PERMISSIONS && permTargetIsPlayer && button == 0) {
            Layout permLayout = buildLayout();
            int permY = CONTENT_TOP_PADDING + ROW_HEIGHT + ROW_GAP;
            if (!targetPlayers.isEmpty()) {
                permY += ROW_HEIGHT; // 列表标题
                for (java.util.Map.Entry<java.util.UUID, String> entry : targetPlayers.entrySet()) {
                    int rowY = permLayout.toScreenY(permY);
                    int nameX = permLayout.left() + 22;
                    int nameW = permLayout.right() - nameX - 6;
                    if (mouseX >= nameX && mouseX < nameX + nameW
                            && mouseY >= rowY && mouseY < rowY + INPUT_HEIGHT) {
                        editingPlayerUUID = entry.getKey();
                        loadPolicyToggles();
                        return true;
                    }
                    permY += ROW_HEIGHT + 2;
                }
            }
        }

        // 然后让其他子组件处理点击事件
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;
        
        // 如果子组件没有处理，再检查 Tab 按钮点击
        if (button == 0 && mouseY >= VIEWPORT_TOP) {
            int tabBtnX = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int tabBtnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;
            if (mouseX >= tabBtnX && mouseX <= tabBtnX + tabBtnW) {
                int tabIndex = 0;
                for (ConfigTab tab : ConfigTab.values()) {
                    if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;
                    int btnY = VIEWPORT_TOP + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                    if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                        // Extra permission check when clicking PERMISSIONS tab
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
        
        // 婵犲﹥鑹炬慨鈺呭级閿涘嫬浠柛鎴滅串缁辨瑩宕搁幇顓犲灱锟?renderScrollbar 濞ｅ洦绻冪€垫梹绋夐埀顒勬嚊鏉堝墽锟?
        if (button == 0 && layout.maxScroll() > 0) {
            int thumbX    = layout.right() + 8;
            int trackTop  = layout.viewportTop();
            int trackBottom = layout.viewportBottom();
            int trackH    = trackBottom - trackTop;
            int thumbPad  = 2;
            int thumbW    = SCROLL_TRACK_W;
            int thumbSize = thumbW + 6;  // 闂傗偓閹稿孩鐓欑憸鑸灪缁箓宕稿Δ鍛蒋锟?
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
        
        // 优先让颜色选择器处理释放事件
        if (embeddedColorPicker != null && embeddedColorPicker.visible) {
            if (embeddedColorPicker.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            Layout layout = buildLayout();
            int trackH    = layout.viewportBottom() - layout.viewportTop();
            int thumbW    = SCROLL_TRACK_W;
            int thumbSize = thumbW + 6;  // 闂傗偓閹稿孩鐓欑憸鑸灪缁箓宕稿Δ鍛蒋锟?
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

        // AE2 濡炲瀛╅悧鍛婄▔婵犳碍妗ㄩ柡澶婂皡缁辩増绂掓惔銏㈠灱濡増锚鐏忣垱銇勯崼鏇炲姤鐎殿喒鍋撳┑顔碱儜缁辨繄鈧妫勭€硅櫕绋夋惔鈥虫暥閻庡湱鎳撶亸顖溾偓闈涚秺缂嶅牓鏁嶉崼婵囧創Tab锟?闂傚倻顥愮粣?闁告劕鎳庨鎰板礌?婵犲﹥鑹炬慨鈺呭级閳藉懐锟?
        int scrollTrackW = 14;
        int panelX = layout.tabBarX() - 2;
        int panelY = 8;  // 闁哄秴娲。浠嬪棘閸パ呮憻濞戞挸锕ラ弻鐔兼偩濞嗗繒姣岄梺鎻掔箳閳规牠锟?
        int panelW = (layout.right() + 8 + scrollTrackW + 4) - panelX;
        int panelBottomMargin = 8;  // 闂傚牄鍨哄妯绘償閺囥垹鍔ュ☉鎾冲閻栧爼骞嬭箛鏇犲炊闁告瑱绲垮▓鎴︽⒒绾惧锟?
        int panelH = this.height - panelY - panelBottomMargin;
        AEStyleRenderer.drawMainPanel(g, panelX, panelY, panelW, panelH);

        // 闁哄秴娲。浠嬪棘閸パ呮憻闁挎稑鐗嗗﹢顏堟閵忊剝绶查柛鎰嫅缁辨繈宕濋悩鐢电厫闁挎稑濂旂粭澶屾暜閿曞倹鞋鐟滅増鍞荤槐婵嗐€掗崨顔界彴闁哄嫬澧介妵姘舵晬?
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
        // 联想下拉列表在所有内容之上渲染（不受 scissor 裁剪）
        renderPlayerSuggestionDropdown(g, mouseX, mouseY);
        renderHoveredTooltip(g, mouseX, mouseY, layout);
    }

    /** 缂備焦锚閸╂顔忛敂鎸庢珷 Tab 闁哄秴楠忕槐姗滶2 濡炲瀛╅悧鎼佹晬婢跺牃锟?*/
    /**
     * 在所有内容之上渲染联想下拉列表（不受 scissor 裁剪，始终在最上层）。
     */
    private void renderPlayerSuggestionDropdown(GuiGraphics g, int mouseX, int mouseY) {
        if (activeTab != ConfigTab.PERMISSIONS) return;
        if (!permTargetIsPlayer || playerSearchBox == null) return;
        if (!playerSearchBox.isFocused() || playerSuggestions.isEmpty()) return;

        int dropX = playerSearchBox.getX();
        int dropY = playerSearchBox.getY() + INPUT_HEIGHT + 1;
        int dropW = playerSearchBox.getWidth();
        int itemH = INPUT_HEIGHT - 2;
        int totalH = playerSuggestions.size() * itemH + 2;

        // 背景
        g.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + totalH + 1,
            AEStyleRenderer.COLOR_OUTLINE);
        g.fill(dropX, dropY, dropX + dropW, dropY + totalH, 0xFF2A2A2A);

        for (int i = 0; i < playerSuggestions.size(); i++) {
            int itemY = dropY + 1 + i * itemH;
            boolean hovered = mouseX >= dropX && mouseX < dropX + dropW
                && mouseY >= itemY && mouseY < itemY + itemH;
            if (hovered) g.fill(dropX, itemY, dropX + dropW, itemY + itemH, 0xFF334466);
            g.drawString(this.font, playerSuggestions.get(i),
                dropX + 4, itemY + (itemH - this.font.lineHeight) / 2,
                hovered ? 0xFF55FF55 : AEStyleRenderer.COLOR_LABEL, false);
        }
    }

    private void renderTabBar(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        Renderer.renderTabBar(g, this.font, this.activeTab, layout, mouseX, mouseY, this.screenMode);
    }

    private void renderScrollableContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, Layout layout) {
        // AE2 濡炲瀛╅悧鎼佹晬濮樿京甯涢柛鎺曟硾閸炲鈧湱鎳撶亸顖炲礄瑜版帗顏為柤鍐叉湰濞呮瑩鏁嶉崼婊呯憹闁告牕鎳庨幆鍫濐煥濮橆剙袟闁哄鈧啿闅橀柛鈺冨櫐缁辨繂顭ㄥ顒€袟闁哄鈧櫕韬鑸电墵閸庢挳锟?
        int contentAreaX = layout.left() - 4;
        int contentAreaY = layout.viewportTop() - 2;
        int contentAreaW = layout.right() - layout.left() + 8;
        int contentAreaH = layout.viewportBottom() - layout.viewportTop() + 4;
        AEStyleRenderer.drawContentArea(g, contentAreaX, contentAreaY, contentAreaW, contentAreaH);

        // 鐎殿喒鍋撻柛?scissor闁挎稑鏈晶宥夊嫉婢跺﹥鍊电紓渚囧幘缁垶宕氱拋鍦闁告牕鎳忕€氼厽娼忛幘鍐插汲婵℃妫滈崕妤呭疾椤栥倗绀嗛梺顔挎瑜板牏鎲楁担绋款梾缂佹拝闄勫?
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
            drawSettingRow(g, Component.translatable("screen.neotab.list.health_display"), layout.healthDisplayLabelBounds(), mouseX, mouseY);

            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.footer"),
                    layout.left(), layout.toScreenY(layout.footerSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.footer.custom"), layout.footerCustomLabelBounds(), mouseX, mouseY);
        } else if (activeTab == ConfigTab.THEME) {
            // TAB主题section（放在最上面）
            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.theme.tab_theme"),
                    layout.left(), layout.toScreenY(layout.themeSectionHeaderY()), layout.right());
            renderThemeSelectorBackground(g, layout);
            
            // 血量显示section（放在TAB主题下面）
            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.health"),
                    layout.left(), layout.toScreenY(layout.healthSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.theme.health_mode"), layout.healthModeLabelBounds(), mouseX, mouseY);

            // 布局分列section
            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.layout"),
                    layout.left(), layout.toScreenY(layout.layoutSectionHeaderY()), layout.right());
            
            // 如果选中自定义主题，渲染分类标题
            if ("custom".equals(selectedThemeId)) {
                int customConfigStartY = layout.themeSelectorY() + layout.themeSelectorHeight() + THEME_LIST_TOP_GAP;
                int customConfigY = customConfigStartY;
                
                // 跳过重置按钮
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;
                
                // 增加与重置按钮的间距
                customConfigY += 15;
                
                // 动画分类标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.animation"), 
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), 
                    AEStyleRenderer.COLOR_SECTION_TEXT, false);
                
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8; // 跳过动画按钮 + 分类间距
                
                // 背景分类标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.background"), 
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), 
                    AEStyleRenderer.COLOR_SECTION_TEXT, false);
                
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8; // 跳过背景按钮 + 分类间距
                
                // 外边框分类标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.outer_border"), 
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), 
                    AEStyleRenderer.COLOR_SECTION_TEXT, false);
                
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8; // 跳过外边框按钮 + 分类间距
                
                // 边框分类标题
                g.drawString(this.font, Component.translatable("screen.neotab.custom_theme.category.border"), 
                    layout.left() + THEME_LIST_INSET, layout.toScreenY(customConfigY - 12), 
                    AEStyleRenderer.COLOR_SECTION_TEXT, false);
            }
        } else if (activeTab == ConfigTab.PERMISSIONS) {
            renderPermissionsContent(g, mouseX, mouseY, layout);
        }
        // 渲染所有可滚动 widget（开关、输入框、按钮等）
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        renderScrollbar(g, layout);
    }

    /**
     * Render the permissions configuration tab content.
     * Shows section headers and labels for each policy field.
     * The actual toggle widgets are rendered by renderScrollableWidgets.
     */
    /**
     * 渲染权限配置界面内容（重构版）。
     *
     * 顶部：目标模式切换 + 输入框 + 添加按钮
     * 指定玩家列表（指定玩家模式下）
     * 权限列表：每行两个权限项，每项用边框背景围起来
     */
    private void renderPermissionsContent(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
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

        // ── 顶部行（目标模式切换 + 输入框 + 添加按钮）由 widget 自己渲染 ──────
        // 联想下拉列表由 renderPlayerSuggestionDropdown 在最上层单独渲染
        y += ROW_HEIGHT + ROW_GAP;

        // ── 指定玩家列表（始终预留空间，指定玩家模式下显示内容）────────────────
        {
            // 列表标题行
            int titleY = layout.toScreenY(y) + (INPUT_HEIGHT - this.font.lineHeight) / 2;
            if (permTargetIsPlayer) {
                g.drawString(this.font,
                    Component.translatable("screen.neotab.policy.target_list"),
                    layout.left(), titleY, AEStyleRenderer.COLOR_SECTION_TEXT, false);
            } else {
                // 所有玩家模式：显示灰色占位文字
                g.drawString(this.font,
                    Component.translatable("screen.neotab.policy.target_list_hint"),
                    layout.left(), titleY, 0xFF666666, false);
            }
            y += ROW_HEIGHT;

            // 玩家标签区域（固定高度，流式横向排列）
            int tagAreaY = layout.toScreenY(y);
            int tagAreaH = INPUT_HEIGHT;  // 紧凑高度，与 buildLayout 保持一致
            // 绘制标签区域背景
            AEStyleRenderer.drawSunkenPanel(g, layout.left(), tagAreaY,
                layout.contentWidth() - 6, tagAreaH, AEStyleRenderer.COLOR_CONTENT_BG, 1);

            if (permTargetIsPlayer) {
                if (targetPlayers.isEmpty()) {
                    // 空列表提示
                    int hintY = tagAreaY + (tagAreaH - this.font.lineHeight) / 2;
                    g.drawString(this.font,
                        Component.translatable("screen.neotab.policy.no_targets"),
                        layout.left() + 4, hintY, 0xFF888888, false);
                } else {
                    // 流式横向排列玩家标签
                    int tagX = layout.left() + 3;
                    int tagY = tagAreaY + 2;
                    int tagH = tagAreaH - 2;  // 留出 1px 上下边距
                    int tagPadX = 4;
                    int removeW = 14;  // 与 applyWidgetLayout 保持一致
                    for (java.util.Map.Entry<java.util.UUID, String> entry : targetPlayers.entrySet()) {
                        boolean isEditing = entry.getKey().equals(editingPlayerUUID);
                        String name = entry.getValue();
                        int nameW = this.font.width(name);
                        int tagW = nameW + tagPadX * 2 + removeW + 2;
                        // 超出右边界换行（简单处理：截断显示）
                        if (tagX + tagW > layout.right() - 8) break;
                        // 标签背景
                        int tagBg = isEditing ? 0xFF334466 : 0xFF4A4E58;
                        g.fill(tagX, tagY, tagX + tagW, tagY + tagH, tagBg);
                        AEStyleRenderer.drawOutline(g, tagX, tagY, tagW, tagH,
                            isEditing ? 0xFF5577AA : AEStyleRenderer.COLOR_OUTLINE, 1);
                        // 玩家名
                        int nameColor = isEditing ? 0xFF55FF55 : AEStyleRenderer.COLOR_LABEL;
                        g.drawString(this.font, name,
                            tagX + tagPadX, tagY + (tagH - this.font.lineHeight) / 2,
                            nameColor, false);
                        // × 符号（删除按钮由 widget 渲染，这里只画文字位置参考）
                        tagX += tagW + 3;
                    }
                }
            }
            y += tagAreaH + 4;  // 与 buildLayout 的 permTargetListHeight 保持一致
        }

        // ── 权限列表分区标题 ──────────────────────────────────────────────────
        String sectionKey = permTargetIsPlayer && editingPlayerUUID != null
            ? "screen.neotab.policy.section_player"
            : "screen.neotab.policy.section_all";
        AEStyleRenderer.drawSectionHeader(g, this.font,
            Component.translatable(sectionKey),
            layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;

        // ── 权限项（每行两个，每项用边框背景围起来）──────────────────────────
        // 每个权限项宽度 = (contentWidth - gap) / 2
        int itemGap = 4;
        int itemW = (layout.contentWidth() - 6 - itemGap) / 2;
        int itemH = INPUT_HEIGHT + 2;  // 与 applyWidgetLayout 保持一致
        int labelPad = 4;

        for (int i = 0; i < policyKeys.length; i++) {
            int col = i % 2;
            int row = i / 2;
            if (col == 0 && i > 0) y += itemH + 2;  // 换行间距（与 applyWidgetLayout 的 itemRowGap 一致）

            int itemX = layout.left() + col * (itemW + itemGap);
            int itemY = layout.toScreenY(y);

            // 边框背景（AE2 凸起面板风格）
            AEStyleRenderer.drawRaisedPanelNoOutline(g, itemX, itemY, itemW, itemH,
                AEStyleRenderer.COLOR_BUTTON_BG, 1);

            // 权限名称标签
            Component label = Component.translatable(policyKeys[i]);
            int textY = itemY + (itemH - this.font.lineHeight) / 2;
            g.drawString(this.font, label, itemX + labelPad, textY,
                AEStyleRenderer.COLOR_LABEL, false);

            // 开关由 widget 自己渲染，这里只需要确保位置正确（在 applyWidgetLayout 中设置）
        }
    }
    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            if (r instanceof CycleButton<?> cb) {
                renderAECycleButton(g, cb, mouseX, mouseY);
            } else if (r instanceof Button button) {
                int themeIndex = this.themeOptionButtons.indexOf(button);
                if (themeIndex >= 0) {
                    renderThemeOptionButton(g, button, this.themeOptionIds.get(themeIndex), mouseX, mouseY);
                } else if (button == customBackgroundColorButton) {
                    // 渲染背景颜色按钮（可能被选中）
                    boolean selected = "background".equals(currentSelectedColorType);
                    renderSelectableColorButton(g, button, selected, mouseX, mouseY);
                } else if (customBorderColorButtons.contains(button)) {
                    // 渲染边框颜色按钮（可能被选中）
                    int buttonIndex = customBorderColorButtons.indexOf(button);
                    if (buttonIndex % 2 == 0) {  // 只处理颜色按钮，不处理删除按钮
                        int colorIndex = buttonIndex / 2;
                        boolean selected = ("border_" + colorIndex).equals(currentSelectedColorType);
                        renderSelectableColorButton(g, button, selected, mouseX, mouseY);
                    } else {
                        // 删除按钮使用普通 AE 风格
                        renderAEButton(g, button, mouseX, mouseY);
                    }
                } else if (button == customBorderOuterFactorButton) {
                    boolean selected = "outer_border".equals(currentSelectedColorType);
                    renderSelectableColorButton(g, button, selected, mouseX, mouseY);
                } else if (button == customBorderOuterFactorButton || button == addCustomBorderColorButton || button == resetToDefaultButton || button == resetConfirmButton || button == resetCancelButton || button == customAnimationSpeedButton) {
                    // 使用 AE 风格渲染其他按钮（包括重置按钮和确认/取消按钮）
                    renderAEButton(g, button, mouseX, mouseY);
                } else if (button == layoutColumnsButton || button == layoutRowsButton) {
                    renderAEButton(g, button, mouseX, mouseY);
                } else if (button == permTargetModeButton || button == permAddButton
                        || button == permSaveButton
                        || targetPlayerRemoveButtons.contains(button)) {
                    // 权限配置界面按钮使用 AE 风格
                    renderAEButton(g, button, mouseX, mouseY);
                } else {
                    r.render(g, mouseX, mouseY, partialTick);
                }
            } else {
                // Input background is handled inside the multiline edit box renderer
                r.render(g, mouseX, mouseY, partialTick);
            }
        }
    }
    
    /**
     * 渲染可选中的颜色按钮
     */
    private void renderSelectableColorButton(GuiGraphics g, Button button, boolean selected, int mouseX, int mouseY) {
        Renderer.renderSelectableColorButton(g, this.font, button, selected, mouseX, mouseY);
    }

    private void renderThemeSelectorBackground(GuiGraphics g, Layout layout) {
        Renderer.renderThemeSelectorBackground(g, layout);
    }

    /**
     * 锟?AE2 濡炲瀛╅悧鍝ョ磼濡搫锟?CycleButton锟?
     * 闁煎啿鏈▍娆撴偨?AEStyleRenderer闁挎稑鏈弸鍐偓娑欘殜椤や線鎳濋崣澶屽锟?ON/OFF 闁绘鍩栭埀顑跨鐏忣垶宕氶崱鎰ㄥ亾?
     */
    private void renderAECycleButton(GuiGraphics g, CycleButton<?> cb, int mouseX, int mouseY) {
        Renderer.renderAECycleButton(g, this.font, cb, mouseX, mouseY);
    }

    private void renderFixedWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Renderer.renderAEButton(g, this.font, this.doneButton, mouseX, mouseY);
        Renderer.renderAEButton(g, this.font, this.cancelButton, mouseX, mouseY);
    }

    /**
     * 锟?AE2 濡炲瀛╅悧鍝ョ磼濡搫鐓戝☉鎾亾锟?Button闁挎稑鐭侀々顐︽儎閺嵮冩枾闁绘鐗婄憰鍡涘蓟閹炬墎锟?
     * 闁稿繐鐗忕划顖炲礆?AE2 闁煎啿鏈▍娆撴晬鐏炶棄鏅欓悹浣叉櫅鐢偊锟?Button 婵炴挸寮堕悡瀣棘閸パ呮憻闁挎稑鐗婇弸鍐偓娑欘殜椤や線鎳濋懠顒佹殸闁告鍠撴晶妤佸緞閸曨厽鍊為柨娑橆槶锟?
     */
    private void renderAEButton(GuiGraphics g, Button btn, int mouseX, int mouseY) {
        Renderer.renderAEButton(g, this.font, btn, mouseX, mouseY);
    }

    private void renderThemeOptionButton(GuiGraphics g, Button btn, String themeId, int mouseX, int mouseY) {
        Renderer.renderThemeOptionButton(g, this.font, btn, themeId, this.selectedThemeId, mouseX, mouseY);
    }

    private void renderButtonBar(GuiGraphics g, Layout layout) {
        Renderer.renderButtonBar(g, layout, this.height);
    }

    /** 婵犲﹥鑹炬慨鈺呭级闄囧娲焼閹炬剚鍟嶉幖杈捐缁辨瑦锟?mouseClicked 濞ｅ洦绻冪€垫梹绋夐埀顒勬嚊鏉堝墽绀嗛柕?*/
    private static final int SCROLL_TRACK_W = 14;

    private void renderScrollbar(GuiGraphics g, Layout layout) {
        Renderer.renderScrollbar(g, layout, this.scrollOffset);
    }

    private static final int TOOLTIP_MAX_WIDTH = 200;

    private void renderHoveredTooltip(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        Renderer.renderHoveredTooltip(g, this.font, hoveredTarget(mouseX, mouseY, layout), mouseX, mouseY);
    }

    // drawSectionHeader 鐎规瓕灏缓鑲╃矓鐠囨彃锟?AEStyleRenderer.drawSectionHeader()

    private void drawSettingRow(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        Renderer.drawLabel(g, this.font, label, bounds, mouseX, mouseY);
    }

    private void drawFooterOption(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        Renderer.drawLabel(g, this.font, label, bounds, mouseX, mouseY);
    }

    private void drawLabel(GuiGraphics g, Component label, LabelBounds bounds, int mouseX, int mouseY) {
        Renderer.drawLabel(g, this.font, label, bounds, mouseX, mouseY);
    }

    private CycleButton<Boolean> newToggle(int x, boolean initialValue) {
        return NeoTabConfigWidgetFactory.newToggle(x, initialValue);
    }

    /** 閻㈩垽绠戦悾顒勫极鐎涙鍨肩紒娑樺⒔濞堟垵顕ｉ埀顒勫礂閾忣偄鐦婚梺绛嬪櫙缁辨繈寮崶褏鎽熼柡鍕⒔閵囨岸宕烽妸锕€鐦婚梺绛嬪枛閸炴挳鏌堥…鎺旂閻庣妫勭€规娊宕￠悩鍐插К闁轰焦娼欓崹顏堝Υ?*/
    private CycleButton<Boolean> newLabeledToggle(int x, int width, boolean initialValue, Component label) {
        return NeoTabConfigWidgetFactory.newLabeledToggle(x, width, initialValue, label);
    }

    /** 閻炴稈鍋撻梺鎻掔箲濡绮堥悜妯绘珡闁哄绮岄崹蹇涘箲閵忊€崇樆闂佺瓔鍣槐娆戔偓鐟版湰锟?/ 闁告娲滅€氼參鏁嶆径鍫氬亾?*/
    private CycleButton<HealthDisplayMode> newHealthModeButton(int x, HealthDisplayMode initialValue) {
        return NeoTabConfigWidgetFactory.newHealthModeButton(x, initialValue, this::adjustLayoutConfigToCurrentLimits);
    }
    
    /**
     * 根据当前的GUI缩放和血量显示模式，自动调整布局配置到限制范围内。
     * 
     * <p>当限制变小时（GUI缩放变大或血量模式从单独变完整），
     * 如果当前配置超出新的限制，会自动调整到最大允许值。</p>
     */
    private void adjustLayoutConfigToCurrentLimits() {
        com.poso.neotab.config.TabLayoutConfig layoutCfg = com.poso.neotab.config.TabLayoutConfig.get();
        
        // 获取当前的最大限制
        int maxColumns = layoutCfg.getMaxColumns();
        int maxRows = layoutCfg.getMaxRows();
        
        // 获取当前配置的值
        int currentColumns = layoutCfg.getColumns();
        int currentRows = layoutCfg.getRowsPerColumn();
        
        // 检查是否需要调整
        boolean needsAdjust = false;
        
        if (currentColumns > maxColumns) {
            layoutCfg.setColumns(maxColumns);
            needsAdjust = true;
        }
        
        if (currentRows > maxRows) {
            layoutCfg.setRowsPerColumn(maxRows);
            needsAdjust = true;
        }
        
        // 如果有调整，保存配置并更新按钮显示
        if (needsAdjust) {
            com.poso.neotab.config.TabLayoutConfig.save(layoutCfg);
            
            // 更新按钮文字
            if (layoutColumnsButton != null) {
                layoutColumnsButton.setMessage(Component.translatable("screen.neotab.layout.columns", layoutCfg.getColumns()));
            }
            if (layoutRowsButton != null) {
                layoutRowsButton.setMessage(Component.translatable("screen.neotab.layout.rows", layoutCfg.getRowsPerColumn()));
            }
        }
    }

    private static int themeSelectorHeight(int optionCount) {
        int rows = Math.max(optionCount, 1);
        return THEME_LIST_INSET * 2
            + rows * THEME_OPTION_HEIGHT
            + Math.max(0, rows - 1) * THEME_OPTION_GAP;
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
            if (layout.healthDisplayLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.list.health_display.tooltip"));
            if (layout.footerCustomLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.footer.custom.tooltip"));
        } else if (activeTab == ConfigTab.THEME) {
            if (layout.healthModeLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.theme.health_mode.tooltip"));
            // 布局分列开关 tooltip
            if (layoutEnabledToggle != null && layoutEnabledToggle.isMouseOver(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.layout.enabled.tooltip"));
            // 列数/行数按钮 tooltip
            if (layoutColumnsButton != null && layoutColumnsButton.isMouseOver(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.layout.columns.tooltip"));
            if (layoutRowsButton != null && layoutRowsButton.isMouseOver(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.layout.rows.tooltip"));
        }
        return null;
    }

    private boolean isInsideViewport(double mouseX, double mouseY, Layout layout) {
        // 闁告瑥鐤囩粩鐔兼偩鐏炴儳鈷栭悘鐐存礀閸╁矂宕犻崨顓熷創婵犲﹥鑹炬慨鈺呭级闄囧娲焼閹惧啿闅橀柛鈺冨櫐缁辨獧ight + 8 閻犙冨槻椤劙鏁嶇仦绛嬪晬 SCROLL_TRACK_W锟?
        return mouseX >= layout.left() && mouseX <= layout.right() + 8 + SCROLL_TRACK_W
            && mouseY >= layout.viewportTop() && mouseY <= layout.viewportBottom();
    }

    private AbstractWidget hoveredScrollableWidget(double mouseX, double mouseY) {
        for (Renderable r : this.renderables) {
            if (r == this.doneButton || r == this.cancelButton) continue;
            // 不可见的 widget 不参与鼠标事件检测，防止遮挡其他 tab 的组件
            if (r instanceof AbstractWidget w && w.visible && w.isMouseOver(mouseX, mouseY)) return w;
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
        placeScrollableWidget(this.healthDisplayEnabled, layout.toggleX(),           layout.toScreenY(layout.healthDisplayRowY()));
        placeScrollableWidget(this.footerCustomInput,    layout.left(),              layout.toScreenY(layout.footerCustomInputY()));
        // THEME tab 闁硅矇鍌涱偨
        placeScrollableWidget(this.healthDisplayMode,    layout.toggleX(),           layout.toScreenY(layout.healthModeRowY()));
        // 布局分列开关 + 按钮 - 同一行并排：[开关] [列数按钮] [行数按钮]
        if (layoutEnabledToggle != null) placeScrollableWidget(layoutEnabledToggle, layout.left(), layout.toScreenY(layout.layoutButtonsY()));
        if (layoutColumnsButton != null) placeScrollableWidget(layoutColumnsButton, layout.left() + TOGGLE_WIDTH + 6, layout.toScreenY(layout.layoutButtonsY()));
        if (layoutRowsButton    != null) placeScrollableWidget(layoutRowsButton,    layout.left() + TOGGLE_WIDTH + 6 + LAYOUT_BUTTON_WIDTH + 10, layout.toScreenY(layout.layoutButtonsY()));
        for (int i = 0; i < this.themeOptionButtons.size(); i++) {
            Button button = this.themeOptionButtons.get(i);
            button.setX(layout.left() + THEME_LIST_INSET);
            button.setY(layout.toScreenY(layout.themeSelectorY() + THEME_LIST_INSET + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            button.setWidth(layout.themeSelectorWidth() - THEME_LIST_INSET * 2);
        }
        // 自定义主题配置组件（紧跟在主题选择器下方，血量显示section之前）
        int customConfigStartY = layout.themeSelectorY() + layout.themeSelectorHeight() + THEME_LIST_TOP_GAP;
        int customConfigY = customConfigStartY;
        
        // 计算按钮宽度（使用 init() 中缓存的值，与颜色选择器宽度保持一致）
        int customButtonWidth = cachedCustomButtonWidth;
        
        // === 重置默认按钮 ===
        if (resetToDefaultButton != null) {
            resetToDefaultButton.setX(layout.left() + THEME_LIST_INSET);
            resetToDefaultButton.setY(layout.toScreenY(customConfigY));
            resetToDefaultButton.setWidth(customButtonWidth);
        }
        
        // === 重置确认和取消按钮（浮动在重置按钮右下角，不占用布局空间）===
        if (showResetConfirmation && resetToDefaultButton != null) {
            int confirmCancelButtonWidth = 50;  // 固定宽度
            int confirmCancelButtonHeight = 18; // 稍小的高度
            
            // 计算位置：在重置按钮的右下角
            int baseX = resetToDefaultButton.getX() + resetToDefaultButton.getWidth();
            int baseY = resetToDefaultButton.getY() + resetToDefaultButton.getHeight();
            
            // 取消按钮（左侧）
            if (resetCancelButton != null) {
                resetCancelButton.setX(baseX - confirmCancelButtonWidth * 2);  // 紧贴，无间距
                resetCancelButton.setY(baseY);
                resetCancelButton.setWidth(confirmCancelButtonWidth);
                resetCancelButton.setHeight(confirmCancelButtonHeight);
            }
            
            // 确认按钮（右侧）
            if (resetConfirmButton != null) {
                resetConfirmButton.setX(baseX - confirmCancelButtonWidth);
                resetConfirmButton.setY(baseY);
                resetConfirmButton.setWidth(confirmCancelButtonWidth);
                resetConfirmButton.setHeight(confirmCancelButtonHeight);
            }
        }
        
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 按钮高度 + 间距
        
        // === 动画分类 ===
        customConfigY += 15; // 增加与上方组件的间距

        // 动画开关 + 速率按钮并排
        int animHalfW = (customButtonWidth - 4) / 2;
        if (customAnimationToggle != null) {
            customAnimationToggle.setX(layout.left() + THEME_LIST_INSET);
            customAnimationToggle.setY(layout.toScreenY(customConfigY));
            customAnimationToggle.setWidth(animHalfW);
        }
        if (customAnimationSpeedButton != null) {
            customAnimationSpeedButton.setX(layout.left() + THEME_LIST_INSET + animHalfW + 4);
            customAnimationSpeedButton.setY(layout.toScreenY(customConfigY));
            customAnimationSpeedButton.setWidth(animHalfW);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 分类间距

        // === 背景分类 ===
        customConfigY += 8; // 分类标题间距

        // 背景颜色按钮
        if (customBackgroundColorButton != null) {
            customBackgroundColorButton.setX(layout.left() + THEME_LIST_INSET);
            customBackgroundColorButton.setY(layout.toScreenY(customConfigY));
            customBackgroundColorButton.setWidth(customButtonWidth);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 分类间距

        // === 外边框分类 ===
        customConfigY += 8; // 分类标题间距

        // 外边框颜色按钮
        if (customBorderOuterFactorButton != null) {
            customBorderOuterFactorButton.setX(layout.left() + THEME_LIST_INSET);
            customBorderOuterFactorButton.setY(layout.toScreenY(customConfigY));
            customBorderOuterFactorButton.setWidth(customButtonWidth);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 分类间距
        
        // === 边框分类 ===
        customConfigY += 8; // 分类标题间距
        
        // 嵌入式颜色选择器（放在右侧）
        if (embeddedColorPicker != null) {
            int gap = 10; // 与左侧按钮的间距
            int maxRight = layout.right() - THEME_LIST_INSET;
            int candidateX = layout.left() + THEME_LIST_INSET + customButtonWidth + gap;
            boolean fits = (candidateX + cachedColorPickerWidth <= maxRight);

            int pickerX, pickerY, pickerWidth;
            if (fits) {
                // 足够并排：放在右侧，位置与重置按钮等顶端对齐
                pickerX = candidateX;
                pickerY = layout.toScreenY(customConfigStartY);
                pickerWidth = cachedColorPickerWidth;
            } else {
                // 空间不足：换行，独占一行
                pickerX = layout.left() + THEME_LIST_INSET;
                pickerY = layout.toScreenY(customConfigY); // 背景按钮下方
                int availableWidth = maxRight - pickerX;
                pickerWidth = Math.min(availableWidth, cachedColorPickerWidth);
                // 占用一行高度，更新 customConfigY
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
            embeddedColorPicker.setX(pickerX);
            embeddedColorPicker.setY(pickerY);
            embeddedColorPicker.setWidth(pickerWidth);
            // 如果 ColorPickerWidget 内部不支持 setWidth，需要后续在渲染中应用缩放；
            // 可在此处添加缩放因子记录，并在 render 时用 pose.scale 处理。
        }
        
        // 边框颜色按钮列表
        List<Integer> borderColors = customThemeConfig != null ? customThemeConfig.getBorderColors() : new ArrayList<>();
        for (int i = 0; i < borderColors.size(); i++) {
            int buttonIndex = i * 2;  // 每个颜色有2个按钮（颜色+删除）
            if (buttonIndex < customBorderColorButtons.size()) {
                // 颜色按钮
                Button colorButton = customBorderColorButtons.get(buttonIndex);
                int colorButtonWidth = customButtonWidth - 25;
                colorButton.setX(layout.left() + THEME_LIST_INSET);
                colorButton.setY(layout.toScreenY(customConfigY));
                colorButton.setWidth(colorButtonWidth);
                
                // 删除按钮
                if (buttonIndex + 1 < customBorderColorButtons.size()) {
                    Button deleteButton = customBorderColorButtons.get(buttonIndex + 1);
                    deleteButton.setX(layout.left() + THEME_LIST_INSET + colorButtonWidth + 5);
                    deleteButton.setY(layout.toScreenY(customConfigY));
                    deleteButton.setWidth(20);
                }
                
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
        }
        
        // 添加边框颜色按钮
        if (addCustomBorderColorButton != null) {
            addCustomBorderColorButton.setX(layout.left() + THEME_LIST_INSET);
            addCustomBorderColorButton.setY(layout.toScreenY(customConfigY));
            addCustomBorderColorButton.setWidth(customButtonWidth);
        }
        
        placeScrollableWidget(this.footerTpsEnabled,     layout.footerFirstColumnX(),  layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerMsptEnabled,    layout.footerSecondColumnX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerOnlineEnabled,  layout.footerThirdColumnX(),  layout.toScreenY(layout.footerRowY()));

        // ── Permissions tab layout ────────────────────────────────────────────
        // 只在权限 tab 激活时才计算布局，避免影响其他 tab
        if (!globalPolicyToggles.isEmpty() && activeTab == ConfigTab.PERMISSIONS) {
            int modeButtonWidth = 80;
            int addButtonWidth  = 40;
            int searchBoxWidth  = layout.contentWidth() - modeButtonWidth - addButtonWidth - 12;

            int permY = CONTENT_TOP_PADDING;

            // 目标模式切换按钮
            if (permTargetModeButton != null) {
                permTargetModeButton.setX(layout.left());
                permTargetModeButton.setY(layout.toScreenY(permY));
                permTargetModeButton.setWidth(modeButtonWidth);
            }
            // 输入框
            if (playerSearchBox != null) {
                playerSearchBox.setX(layout.left() + modeButtonWidth + 4);
                playerSearchBox.setY(layout.toScreenY(permY));
                playerSearchBox.setWidth(searchBoxWidth);
            }
            // 添加按钮
            if (permAddButton != null) {
                permAddButton.setX(layout.left() + modeButtonWidth + 4 + searchBoxWidth + 4);
                permAddButton.setY(layout.toScreenY(permY));
            }
            permY += ROW_HEIGHT + ROW_GAP;

            // 指定玩家列表（始终预留空间）
            permY += ROW_HEIGHT; // 列表标题行
            int tagAreaH2 = INPUT_HEIGHT;  // 紧凑标签区域，与 buildLayout 保持一致
            // 删除按钮：流式横向排列，与标签对齐
            if (permTargetIsPlayer && !targetPlayers.isEmpty()) {
                int tagX2 = layout.left() + 3;
                int tagY2 = layout.toScreenY(permY) + 2;
                int tagH2 = tagAreaH2 - 2;  // 留出 1px 上下边距
                int tagPadX2 = 4;
                int removeW2 = 14;  // 稍大一点，更容易点击
                int btnIdx = 0;
                for (java.util.UUID uuid : targetPlayers.keySet()) {
                    if (btnIdx >= targetPlayerRemoveButtons.size()) break;
                    String name = targetPlayers.get(uuid);
                    int nameW2 = this.font.width(name);
                    int tagW2 = nameW2 + tagPadX2 * 2 + removeW2 + 2;
                    if (tagX2 + tagW2 > layout.right() - 8) break;
                    Button removeBtn = targetPlayerRemoveButtons.get(btnIdx);
                    removeBtn.setX(tagX2 + tagPadX2 + nameW2 + 2);
                    removeBtn.setY(tagY2 + (tagH2 - INPUT_HEIGHT) / 2);
                    removeBtn.setWidth(removeW2);
                    removeBtn.setHeight(INPUT_HEIGHT);
                    tagX2 += tagW2 + 3;
                    btnIdx++;
                }
            }
            permY += tagAreaH2 + 4;  // 与 buildLayout 的 permTargetListHeight 保持一致

            // 权限列表分区标题
            permY += SECTION_HEADER_HEIGHT;

            // 权限项（每行两个，开关放在每个 item 右侧）
            int itemGap = 4;
            int itemW = (layout.contentWidth() - 6 - itemGap) / 2;
            int itemH = INPUT_HEIGHT + 2;  // 紧凑高度
            int itemRowGap = 2;
            int togglePad = 3;

            for (int i = 0; i < globalPolicyToggles.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                // 正确计算：permY 是当前行起始内容坐标，row * (itemH + itemRowGap) 是行偏移
                int rowContentY = permY + row * (itemH + itemRowGap);
                int itemX = layout.left() + col * (itemW + itemGap);
                int itemScreenY = layout.toScreenY(rowContentY);
                // 开关放在 item 右侧，垂直居中
                int toggleX = itemX + itemW - TOGGLE_WIDTH - togglePad;
                int toggleY = itemScreenY + (itemH - INPUT_HEIGHT) / 2;
                placeScrollableWidget(globalPolicyToggles.get(i), toggleX, toggleY);
            }

            // 保存权限按钮（放在权限列表下方）
            int permRowCount2 = (globalPolicyToggles.size() + 1) / 2;
            int saveButtonY = permY + permRowCount2 * (itemH + itemRowGap) + ROW_GAP;
            if (permSaveButton != null) {
                permSaveButton.setX(layout.left());
                permSaveButton.setY(layout.toScreenY(saveButtonY));
                permSaveButton.setWidth(120);
            }
        }

        this.doneButton.setX(layout.doneButtonX());
        this.doneButton.setY(layout.buttonY());
        this.cancelButton.setX(layout.cancelButtonX());
        this.cancelButton.setY(layout.buttonY());
    }

    private void placeScrollableWidget(AbstractWidget w, int x, int y) {
        w.setX(x); w.setY(y);
    }

    /**
     * 重建边框颜色按钮列表
     */
    private void rebuildCustomBorderColorButtons() {
        // 移除旧按钮
        for (Button button : customBorderColorButtons) {
            removeWidget(button);
        }
        customBorderColorButtons.clear();
        
        // 移除添加按钮
        if (addCustomBorderColorButton != null) {
            removeWidget(addCustomBorderColorButton);
            addCustomBorderColorButton = null;
        }
        
        // 如果 customThemeConfig 为 null，直接返回
        if (customThemeConfig == null) {
            return;
        }
        
        Layout layout = buildLayout();
        List<Integer> colors = customThemeConfig.getBorderColors();
        
        // 计算按钮宽度（使用 init() 中缓存的值，与颜色选择器宽度保持一致）
        int customButtonWidth = cachedCustomButtonWidth;
        int colorButtonWidth = customButtonWidth - 25;
        
        // 创建边框颜色按钮
        for (int i = 0; i < colors.size(); i++) {
            final int index = i;
            final int color = colors.get(i);
            
            // 颜色按钮（左侧，占大部分宽度）- 可选中
            Button colorButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.border_color", i + 1),
                button -> {
                    // 选中这个边框颜色
                    currentSelectedColorType = "border_" + index;
                    currentSelectedBorderIndex = index;
                    if (embeddedColorPicker != null) {
                        embeddedColorPicker.setColor(color);
                    }
                }
            ).bounds(layout.left(), 0, colorButtonWidth, THEME_OPTION_HEIGHT).build());
            customBorderColorButtons.add(colorButton);
            
            // 删除按钮（右侧）
            Button deleteButton = addRenderableWidget(Button.builder(
                Component.literal("×"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                    newColors.remove(index);
                    customThemeConfig.setBorderColors(newColors);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    // 如果删除的是当前选中的颜色，切换到背景颜色
                    if (currentSelectedBorderIndex == index) {
                        currentSelectedColorType = "background";
                        currentSelectedBorderIndex = -1;
                        if (embeddedColorPicker != null) {
                            embeddedColorPicker.setColor(customThemeConfig.getBackgroundColor());
                        }
                    } else if (currentSelectedBorderIndex > index) {
                        currentSelectedBorderIndex--;
                        currentSelectedColorType = "border_" + currentSelectedBorderIndex;
                    }
                    rebuildCustomBorderColorButtons();
                    Layout newLayout = buildLayout();
                    applyWidgetLayout(newLayout);
                }
            ).bounds(layout.left(), 0, 20, THEME_OPTION_HEIGHT).build());
            customBorderColorButtons.add(deleteButton);
        }
        
        // 添加颜色按钮（最多7个）
        if (colors.size() < 7) {
            this.addCustomBorderColorButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.add_border_color"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                    newColors.add(0xFFFFFFFF); // 默认白色
                    customThemeConfig.setBorderColors(newColors);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    rebuildCustomBorderColorButtons();
                    Layout newLayout = buildLayout();
                    applyWidgetLayout(newLayout);
                    // 自动滚动到底部，确保新添加的按钮可见
                    if (newLayout.maxScroll() > 0) {
                        setScrollOffset(newLayout.maxScroll(), newLayout);
                    }
                }
            ).bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT).build());
        }
        
        // 更新可见性
        syncTabWidgetVisibility();
    }
    
    private void save() {
        if (screenMode == ScreenMode.PLAYER) {
            // PLAYER mode: build and send personal config
            com.poso.neotab.config.PlayerTabConfig playerCfg = new com.poso.neotab.config.PlayerTabConfig(
                Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : java.util.UUID.randomUUID(),
                policy.allowTopTitleToggle()      ? this.topTitleEnabled.getValue()      : null,
                policy.allowTopTitleEdit()        ? this.topTitleInput.getValue()        : null,
                policy.allowTopContentToggle()    ? this.topContentEnabled.getValue()    : null,
                policy.allowTopContentEdit()      ? this.topContentInput.getValue()      : null,
                policy.allowPingDisplayToggle()   ? this.betterPingEnabled.getValue()    : null,
                policy.allowDurationToggle()      ? this.onlineDurationEnabled.getValue() : null,
                policy.allowTitleToggle()         ? this.titleEnabled.getValue()         : null,
                policy.allowHealthDisplayToggle() ? this.healthDisplayEnabled.getValue() : null,
                policy.allowHealthModeChange()    ? this.healthDisplayMode.getValue()    : null,
                policy.allowFooterCustomEdit()    ? this.footerCustomInput.getValue()    : null,
                policy.allowFooterTpsToggle()     ? this.footerTpsEnabled.getValue()     : null,
                policy.allowFooterMsptToggle()    ? this.footerMsptEnabled.getValue()    : null,
                policy.allowFooterOnlineToggle()  ? this.footerOnlineEnabled.getValue()  : null,
                policy.allowThemeChange()         ? this.selectedThemeId                : null
            );
            PacketDistributor.sendToServer(new SavePlayerConfigPayload(playerCfg));
        } else {
            // ADMIN mode: build and send server config (includes policy from permissions tab)
            TabConfig config = new TabConfig(
                this.topTitleEnabled.getValue(),
                this.topTitleInput.getValue(),
                this.topContentEnabled.getValue(),
                this.topContentInput.getValue(),
                this.betterPingEnabled.getValue(),
                this.onlineDurationEnabled.getValue(),
                this.titleEnabled.getValue(),
                this.healthDisplayEnabled.getValue(),
                this.healthDisplayMode.getValue(),
                this.selectedThemeId,
                this.footerCustomInput.getValue(),
                this.footerTpsEnabled.getValue(),
                this.footerMsptEnabled.getValue(),
                this.footerOnlineEnabled.getValue(),
                this.initialConfig.refreshIntervalTicks(),
                buildGlobalPolicyFromToggles(),
                buildPlayerPoliciesFromToggles()
            ).sanitized();
            PacketDistributor.sendToServer(new SaveConfigPayload(config));
        }
        onClose();
    }

    /**
     * Build global policy from the permissions tab toggles.
     * Falls back to initialConfig.globalPolicy() if no toggles exist (e.g. permissions tab not opened).
     */
    private com.poso.neotab.permission.PlayerCustomizePolicy buildGlobalPolicyFromToggles() {
        if (globalPolicyToggles.size() < 15) {
            return initialConfig.globalPolicy();
        }
        com.poso.neotab.permission.PlayerCustomizePolicy built = new com.poso.neotab.permission.PlayerCustomizePolicy(
            globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
            globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
            globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
            globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
            globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
            globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
            globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
            globalPolicyToggles.get(14).getValue()
        );
        // 如果当前正在编辑某个玩家，开关值属于该玩家的个人策略，不是全局策略
        // 全局策略保持 initialConfig 中的值
        if (permTargetIsPlayer && editingPlayerUUID != null) {
            return initialConfig.globalPolicy();
        }
        return built;
    }

    /**
     * 构建玩家策略 Map。
     * 在指定玩家模式下，将当前开关值应用到所有 targetPlayers 中的玩家。
     * 在所有玩家模式下，保持 initialConfig 中的 playerPolicies 不变。
     */
    private java.util.Map<java.util.UUID, com.poso.neotab.permission.PlayerCustomizePolicy> buildPlayerPoliciesFromToggles() {
        java.util.Map<java.util.UUID, com.poso.neotab.permission.PlayerCustomizePolicy> policies =
            new java.util.HashMap<>(initialConfig.playerPolicies());

        if (!permTargetIsPlayer || globalPolicyToggles.size() < 15) {
            return policies;
        }

        // 从开关读取当前策略值
        com.poso.neotab.permission.PlayerCustomizePolicy current = new com.poso.neotab.permission.PlayerCustomizePolicy(
            globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
            globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
            globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
            globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
            globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
            globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
            globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
            globalPolicyToggles.get(14).getValue()
        );

        // 将当前策略应用到所有指定玩家
        for (java.util.UUID uuid : targetPlayers.keySet()) {
            policies.put(uuid, current);
        }
        return policies;
    }

    /**
     * 闂佹彃绉堕悾濠氬极閺夋垹鐐婂銈囨暬濞间即鎯冮崟顐ゎ伌閻忕偐鍋撻柕?
     * Tab 闁哄秴绻愬畷浼村箲椤旈棿绠〒?TAB_BAR_WIDTH 闁稿秴绻掔粈宀勬晬鐏炶棄鏁堕悗鍦嚀鐏忣垶宕烽妸銉ュ緭闁告瑥鍘栭弲鍫曞Υ?
     */
    private Layout buildLayout() {
        // Tab 闁哄秴绻楅幑锝嗘叏?X闁挎稒鑹鹃惈鍡涚嵁閺囩喐瀵滄鐐插暱閻櫕绋夐鐐村€甸柨娑樿嫰閸炲鈧湱鎳撶亸顖氼啅閿曚胶鐝堕柛鎰Т缁舵艾锟?TAB_BAR_WIDTH
        // 根据屏幕宽度动态调整内容区域宽度，确保在不同GUI缩放下都能正常显示
        // 计算可用宽度：屏幕宽度 - 左右边距 - Tab栏宽度 - Tab与内容间距 - 滚动条宽度 - 额外边距
        int minSidePadding = 16;  // 最小边距
        int scrollbarAndMargin = SCROLL_TRACK_W + 20;  // 滚动条宽度 + 额外边距
        int availableWidth = this.width - minSidePadding * 2 - TAB_BAR_WIDTH - TAB_CONTENT_GAP - scrollbarAndMargin;
        
        // 内容宽度：在可用宽度和最大宽度之间取较小值
        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(200, availableWidth));
        // 闁轰胶绻濈紞瀣锤濡ゅ绀凾ab锟?+ 闁告劕鎳庨鎰板礌閻氬绀嗛悘鐐叉噸锟?
        // 计算总宽度和居中位置
        int totalWidth = TAB_BAR_WIDTH + TAB_CONTENT_GAP + contentWidth + scrollbarAndMargin;
        int blockLeft = Math.max(minSidePadding, (this.width - totalWidth) / 2);
        int tabBarX = blockLeft;
        int left = blockLeft + TAB_BAR_WIDTH + TAB_CONTENT_GAP;
        int right = left + contentWidth;
        // toggleX 閻庨潧缍婄紞鍫ュ礆閹峰瞼缈婚柛蹇嬪劜椤㈠鈧湱鍋ゅ顖炲矗鐎圭姷鐝舵俊妤€妫寸槐娆愭綇閹惧啿寮虫俊妤€妫楅鏃€鎯旈敃鈧崳娲储?NoCountMultiLineEditBox 锟?SCROLLBAR_SPACE=6锟?
        int toggleX = right - 6 - TOGGLE_WIDTH;

        int buttonWidth = Math.min(150, (contentWidth - 10) / 2);
        int panelBottomMargin = 8;
        int buttonY = this.height - panelBottomMargin - 10 - INPUT_HEIGHT;  // 闂傚牄鍨哄妯绘償閺囥垹锟?- 闂傚倻顥愮粣?- 闁圭顦甸幐铏殗濡搫锟?
        int buttonBarTop = buttonY - 6;
        int viewportTop = VIEWPORT_TOP;
        int viewportBottom = Math.max(viewportTop + 40, buttonBarTop - VIEWPORT_BOTTOM_MARGIN);

        // PAGE_CONFIG闁挎稒鐭粭浣圭▔椤忓嫬鐎婚柛鏍細缁绘稓绱掗鐔风瑩闁告帗顨愮槐锟?闁秆勫姈閻栵絿妲愰姘潱
        // 濡炪倕鐖奸崕鎾偩濞嗗繐鏁堕弶鍫㈩攰缁愭盯鏁嶅畝鍐惧敤"濡炪倕鐖奸崕瀛樼┍閳╁啩锟?闁哄秴娲。鑺ョ▔鎼粹€虫暥閻庡湱鎳撶亸顖涚▔婵犲懐鐝舵俊妤€妫旂换姘跺箰娓氣偓濡法锟?
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
        y += ROW_HEIGHT + ROW_GAP;
        int healthDisplayRowY = y;
        y += ROW_HEIGHT + SECTION_GAP;

        int footerSectionHeaderY = y;
        y += SECTION_HEADER_HEIGHT;
        int footerCustomRowY = y;
        int footerCustomInputY = footerCustomRowY + ROW_HEIGHT;
        y = footerCustomInputY + MULTILINE_INPUT_HEIGHT + ROW_GAP;
        int footerRowY = y;

        // 閹煎瓨娲熼崕瀛樼▔婢跺﹤鐏欓悗纭呮鐎规娊鏁嶅顓涘亾鐠囧樊鍟嶅☉鎾虫唉缁额參宕楅妷锔绘敱闁告瑥鐤囩粩鐔奉浖閸℃鍤犲缁樺姧缁辨獑ontentWidth - 6闁挎稑濂旂粭?SCROLLBAR_SPACE 濞戞挴鍋撻柤鐤彧锟?
        int footerTotalWidth = contentWidth - 6;
        int footerColumnWidth = (footerTotalWidth - FOOTER_COLUMN_GAP * 2) / 3;
        int footerFirstColumnX  = left;
        int footerSecondColumnX = left + footerColumnWidth + FOOTER_COLUMN_GAP;
        int footerThirdColumnX  = left + (footerColumnWidth + FOOTER_COLUMN_GAP) * 2;

        int labelWidth = Math.max(80, contentWidth - 6 - TOGGLE_WIDTH - 8);

        // Theme tab layout - TAB主题section在最上面，血量显示section在最下面
        int themeSectionHeaderY = CONTENT_TOP_PADDING;
        int themeSelectorY      = themeSectionHeaderY + SECTION_HEADER_HEIGHT;
        int themeSelectorWidth  = contentWidth - 6;
        int themeSelectorHeight = themeSelectorHeight(TabThemeRegistry.ids().size());
        
        // 自定义主题配置组件紧跟在主题选择器下面
        int customConfigBaseY = themeSelectorY + themeSelectorHeight + THEME_LIST_TOP_GAP;
        int customConfigHeight = 0;
        if ("custom".equals(selectedThemeId) && customThemeConfig != null) {
            customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5; // 重置默认按钮
            customConfigHeight += 15 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5; // 动画
            customConfigHeight += 8 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 背景
            customConfigHeight += 8 + THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5;  // 外边框

            // 判断颜色选择器是否需要换行
            int customButtonWidth = Math.max(60, (themeSelectorWidth - THEME_LIST_INSET * 2) / 2);
            int pickerX = left + THEME_LIST_INSET + customButtonWidth + 10;
            int maxRight = right - THEME_LIST_INSET;
            if (pickerX + 158 > maxRight) {
                // 换行将占用额外一行高度
                customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
            customConfigHeight += 8; // 边框分类标题
            List<Integer> borderColors = customThemeConfig.getBorderColors();
            if (borderColors != null) {
                int borderColorCount = borderColors.size();
                customConfigHeight += borderColorCount * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP);
                if (borderColorCount < 7) {
                    customConfigHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
                }
            }
        }
        
        // 血量显示section在自定义配置下面（或主题选择器下面，如果没有自定义配置）
        int healthSectionHeaderY = customConfigBaseY + customConfigHeight + SECTION_GAP;
        int healthModeRowY       = healthSectionHeaderY + SECTION_HEADER_HEIGHT;

        // 布局分列section（在血量显示section下面）
        int layoutSectionHeaderY = healthModeRowY + ROW_HEIGHT + SECTION_GAP;
        int layoutButtonsY       = layoutSectionHeaderY + SECTION_HEADER_HEIGHT;  // 直接放按钮，无文字标签

        // Content height for the active tab
        // 权限 tab 内容高度：顶部行 + 指定玩家列表（可变）+ 分区标题 + 权限项（8行，每行两个）
        // 指定玩家列表：始终预留固定高度（标题行 + 标签区域 + 间距）
        // 使用紧凑高度，减少占用空间
        int permTagAreaH = INPUT_HEIGHT;  // 紧凑标签区域
        int permTargetListHeight = ROW_HEIGHT + permTagAreaH + 4;  // 标题 + 标签区域 + 小间距
        int permItemH = INPUT_HEIGHT + 2;  // 与 applyWidgetLayout 保持一致
        int permRowCount = (15 + 1) / 2; // 8行（15个权限，每行2个）
        int permissionsContentHeight = CONTENT_TOP_PADDING
            + (ROW_HEIGHT + ROW_GAP)       // 顶部行
            + permTargetListHeight          // 指定玩家列表
            + SECTION_HEADER_HEIGHT         // 分区标题
            + permRowCount * (permItemH + 2) // 权限项（itemRowGap=2）
            + ROW_GAP + INPUT_HEIGHT + 4;   // 保存权限按钮

        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            contentHeight = footerRowY + ROW_HEIGHT;
        } else if (activeTab == ConfigTab.PERMISSIONS) {
            contentHeight = permissionsContentHeight;
        } else {
            // Theme tab: 主题选择器 + 自定义配置（如有）+ 血量显示section + 布局分列section
            contentHeight = layoutButtonsY + INPUT_HEIGHT;
        }
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));

        // 闂傚牄鍨哄妯荤▔椤撶偟锟?X闁挎稑鐗呯粭?render 闁哄倽顫夌涵鑸电▔椤撶姵锟?panelX/panelW 閻犱緤绱曢悾缁樼┍濠靛洤鐦☉鎾亾闁肩柉鎻槐?
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
            listSectionHeaderY, betterPingRowY, onlineDurationRowY, titleRowY, healthDisplayRowY,
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
            themeSectionHeaderY, themeSelectorY, themeSelectorWidth, themeSelectorHeight,
            healthSectionHeaderY, healthModeRowY,
            layoutSectionHeaderY, layoutButtonsY,
            labelBounds(Component.translatable("screen.neotab.top.title"),        left, viewportTop - this.scrollOffset + topTitleRowY,       labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.top.content"),      left, viewportTop - this.scrollOffset + topContentRowY,     labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.better_ping"), left, viewportTop - this.scrollOffset + betterPingRowY,     labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.online_duration"), left, viewportTop - this.scrollOffset + onlineDurationRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.title"),       left, viewportTop - this.scrollOffset + titleRowY,          labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.list.health_display"), left, viewportTop - this.scrollOffset + healthDisplayRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.custom"),    left, viewportTop - this.scrollOffset + footerCustomRowY,   labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.tps"),    footerFirstColumnX,  viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.mspt"),   footerSecondColumnX, viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.footer.online"), footerThirdColumnX,  viewportTop - this.scrollOffset + footerRowY, footerColumnWidth - TOGGLE_WIDTH - 6, this.font),
            labelBounds(Component.translatable("screen.neotab.theme.health_mode"), left, viewportTop - this.scrollOffset + healthModeRowY, labelWidth, this.font)
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
        int listSectionHeaderY, int betterPingRowY, int onlineDurationRowY, int titleRowY, int healthDisplayRowY,
        int footerSectionHeaderY, int footerCustomRowY, int footerCustomInputY, int footerRowY,
        int footerFirstColumnX, int footerSecondColumnX, int footerThirdColumnX,
        int footerFirstToggleX, int footerSecondToggleX, int footerThirdToggleX,
        int footerColumnWidth,
        int contentHeight, int maxScroll, int buttonWidth, int buttonY,
        int doneButtonX, int cancelButtonX,
        int tabBarX,
        int themeSectionHeaderY, int themeSelectorY, int themeSelectorWidth, int themeSelectorHeight,
        int healthSectionHeaderY, int healthModeRowY,
        int layoutSectionHeaderY, int layoutButtonsY,
        LabelBounds topTitleLabelBounds, LabelBounds topContentLabelBounds,
        LabelBounds betterPingLabelBounds, LabelBounds onlineDurationLabelBounds,
        LabelBounds titleLabelBounds, LabelBounds healthDisplayLabelBounds,
        LabelBounds footerCustomLabelBounds,
        LabelBounds footerFirstLabelBounds, LabelBounds footerSecondLabelBounds,
        LabelBounds footerThirdLabelBounds,
        LabelBounds healthModeLabelBounds
    ) {
        private int toScreenY(int contentY) {
            return this.viewportTop - this.scrollOffset + contentY;
        }
        private int scissorLeft() { return Math.max(0, this.left - 2); }
        private int scissorRight() { return this.right + 10; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 渲染工具类（静态嵌套类）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 配置界面渲染工具。
     *
     * <p>将所有纯渲染逻辑从 {@link NeoTabConfigScreen} 中抽离，
     * 使主屏幕类专注于状态管理、事件处理和布局计算。</p>
     *
     * <p>作为静态嵌套类，可直接访问外部类的所有 private 成员，
     * 无需修改任何访问修饰符。</p>
     */
    static final class Renderer {

        private Renderer() {}

        // ── Tab 栏 ────────────────────────────────────────────────────────────

        static void renderTabBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                                 ConfigTab activeTab, Layout layout,
                                 int mouseX, int mouseY, ScreenMode screenMode) {
            int x    = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;

            int tabIndex = 0;
            for (ConfigTab tab : ConfigTab.values()) {
                // PERMISSIONS tab is only visible in ADMIN mode
                if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;
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

        static void renderThemeSelectorBackground(GuiGraphics g, Layout layout) {
            AEStyleRenderer.drawInputBackground(
                    g,
                    layout.left(),
                    layout.toScreenY(layout.themeSelectorY()),
                    layout.themeSelectorWidth(),
                    layout.themeSelectorHeight()
            );
        }

        // ── 主题选项按钮 ──────────────────────────────────────────────────────

        static void renderThemeOptionButton(GuiGraphics g, net.minecraft.client.gui.Font font,
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

        static void renderSelectableColorButton(GuiGraphics g, net.minecraft.client.gui.Font font,
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

        static void renderAECycleButton(GuiGraphics g, net.minecraft.client.gui.Font font,
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

        static void renderAEButton(GuiGraphics g, net.minecraft.client.gui.Font font,
                                   Button btn, int mouseX, int mouseY) {
            if (btn == null || !btn.visible) return;
            boolean hovered = btn.isMouseOver(mouseX, mouseY);
            int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
            AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
            int textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
            g.drawCenteredString(font, btn.getMessage(), bx + bw / 2, by + (bh - font.lineHeight) / 2, textColor);
        }

        // ── 底部按钮栏 ────────────────────────────────────────────────────────

        static void renderButtonBar(GuiGraphics g, Layout layout, int screenHeight) {
            int panelX = layout.tabBarX() - 2;
            int panelW = (layout.right() + 8 + SCROLL_TRACK_W + 4) - panelX;
            int panelBottom = screenHeight - 8;
            AEStyleRenderer.drawButtonBar(g, panelX + 1, layout.buttonBarTop(),
                    panelW - 2, panelBottom - layout.buttonBarTop());
        }

        // ── 滚动条 ────────────────────────────────────────────────────────────

        static void renderScrollbar(GuiGraphics g, Layout layout, int scrollOffset) {
            if (layout.maxScroll() <= 0) return;
            AEStyleRenderer.drawScrollbar(g,
                    layout.right() + 8, layout.viewportTop(), layout.viewportBottom(),
                    SCROLL_TRACK_W, scrollOffset, layout.maxScroll());
        }

        // ── 悬停提示 ──────────────────────────────────────────────────────────

        static void renderHoveredTooltip(GuiGraphics g, net.minecraft.client.gui.Font font,
                                         HoverTarget ht, int mouseX, int mouseY) {
            if (ht == null) return;
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(ht.tooltip(), 200);
            g.renderTooltip(font, lines, mouseX, mouseY);
        }

        // ── 标签行 ────────────────────────────────────────────────────────────

        static void drawLabel(GuiGraphics g, net.minecraft.client.gui.Font font,
                              Component label, LabelBounds bounds,
                              int mouseX, int mouseY) {
            boolean hovered = bounds.contains(mouseX, mouseY);
            int color  = hovered ? AEStyleRenderer.COLOR_LABEL_HOVER : AEStyleRenderer.COLOR_LABEL;
            int labelY = bounds.y() + (INPUT_HEIGHT - font.lineHeight) / 2 + 1;
            g.drawString(font, label, bounds.x(), labelY, color, false);
        }
    }
}

