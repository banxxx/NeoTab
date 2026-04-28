package com.poso.neotab.client.screen;

import com.poso.neotab.client.gui.AEStyleRenderer;
import com.poso.neotab.client.widget.ColorPickerWidget;
import com.poso.neotab.client.widget.ImprovedRichTextEditBox;
import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.theme.TabThemeRegistry;
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

import java.util.ArrayList;
import java.util.List;

/** NeoTab 闂佹澘绉堕悿鍡涙偩瀹€鍕〃闁挎稑鑻稊蹇旂瑹?Tab 闁哄秴绻愰崹蹇涘箲椤厾鐟忓☉鎿冧簻閸ㄥ酣宕犻幁鎺嗗亾?*/
public class NeoTabConfigScreen extends Screen {
    // Tab 闁哄鐭俊?
    private enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme");
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
    // 自定义主题配置组件
    private Button customBackgroundColorButton;  // 背景颜色按钮（可选中）
    private Button customBorderOuterFactorButton;  // 外边框深度按钮
    private CycleButton<Boolean> customAnimationToggle;  // 动画开关
    private final List<Button> customBorderColorButtons = new ArrayList<>();  // 边框颜色按钮列表（可选中）
    private Button addCustomBorderColorButton;  // 添加边框颜色按钮
    private com.poso.neotab.theme.CustomThemeConfig customThemeConfig;  // 自定义主题配置
    // 嵌入式颜色选择器
    private ColorPickerWidget embeddedColorPicker;  // 嵌入式颜色选择器（始终显示在右侧）
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

    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
    }

    @Override
    protected void init() {
        clearWidgets();
        this.themeOptionButtons.clear();
        this.themeOptionIds.clear();
        this.selectedThemeId = TabThemeRegistry.get(initialConfig.tabTheme()).id();
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
        this.healthDisplayEnabled = addRenderableWidget(newToggle(layout.toggleX(), initialConfig.healthDisplayEnabled()));
        this.healthDisplayMode = addRenderableWidget(newHealthModeButton(layout.toggleX(), initialConfig.healthDisplayMode()));
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
        this.customThemeConfig = com.poso.neotab.theme.CustomThemeManager.get();
        
        // 计算按钮宽度（只占左半边）
        int customButtonWidth = (layout.themeSelectorWidth() - THEME_LIST_INSET * 2 - 10) / 2;
        
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
        
        // 外边框深度因子按钮
        this.customBorderOuterFactorButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.border_outer_factor", customThemeConfig.getBorderOuterColorFactor()),
                button -> {
                    int current = customThemeConfig.getBorderOuterColorFactor();
                    int next = switch (current) {
                        case 20 -> 30;
                        case 30 -> 40;
                        case 40 -> 50;
                        case 50 -> 20;
                        default -> 40;
                    };
                    customThemeConfig.setBorderOuterColorFactor(next);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    button.setMessage(Component.translatable("screen.neotab.custom_theme.border_outer_factor", next));
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        
        // 动画开关
        this.customAnimationToggle = addRenderableWidget(CycleButton.booleanBuilder(
                Component.translatable("screen.neotab.custom_theme.animation.on"),
                Component.translatable("screen.neotab.custom_theme.animation.off"))
            .withInitialValue(customThemeConfig.isAnimationEnabled())
            .create(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT,
                    Component.translatable("screen.neotab.custom_theme.animation"),
                    (btn, value) -> {
                        customThemeConfig.setAnimationEnabled(value);
                        com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    }));
        
        // 创建嵌入式颜色选择器（始终显示在右侧）
        int pickerX = layout.left() + customButtonWidth + 20;  // 左侧按钮宽度 + 间距
        int pickerY = 0;  // 稍后在 applyWidgetLayout 中设置
        this.embeddedColorPicker = new ColorPickerWidget(pickerX, pickerY, this.font, 
            customThemeConfig.getBackgroundColor(), 
            color -> {
                // 颜色变化时的回调 - 根据当前选中的项更新颜色
                if ("background".equals(currentSelectedColorType)) {
                    customThemeConfig.setBackgroundColor(color);
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
            });
        addRenderableWidget(this.embeddedColorPicker);
        
        // 默认选中背景颜色
        currentSelectedColorType = "background";
        currentSelectedBorderIndex = -1;
        
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
        
        // 初始化边框颜色按钮（在所有组件初始化完成后）
        rebuildCustomBorderColorButtons();
        
        clampScroll(layout);
        applyWidgetLayout(layout);
        syncTabWidgetVisibility();
    }

    /** 闁哄秷顫夊畵浣姐亹閹惧啿顤呮繝纰樺亾锟?Tab 闁哄嫬澧介妵?闂傚懏鍔樺Λ宀€鈧數鎳撶花鏌ュ箳瑜屽▎銏ゅΥ?*/
    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;
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
        for (Button button : this.themeOptionButtons) {
            button.visible = theme;
        }
        // 只有选中 custom 主题时才显示配置组件
        customBackgroundColorButton.visible = theme && "custom".equals(selectedThemeId);
        customBorderOuterFactorButton.visible = theme && "custom".equals(selectedThemeId);
        customAnimationToggle.visible = theme && "custom".equals(selectedThemeId);
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
        
        // 优先检查颜色选择器的点击（避免被左侧按钮拦截）
        // 检查鼠标是否在颜色选择器的区域内
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
        
        // 然后让其他子组件处理点击事件
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;
        
        // 如果子组件没有处理，再检查 Tab 按钮点击
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
        renderHoveredTooltip(g, mouseX, mouseY, layout);
    }

    /** 缂備焦锚閸╂顔忛敂鎸庢珷 Tab 闁哄秴楠忕槐姗滶2 濡炲瀛╅悧鎼佹晬婢跺牃锟?*/
    private void renderTabBar(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        int x = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;  // 闁圭顦甸幐鍐差啅閿曚胶鐝堕悹?
        int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;  // 闁圭顦甸幐宕団偓纭呮鐎规娊鏁嶉崼婊呯憹閻℃帒鎳庨崵?Tab 闁哄秴绻愯ぐ鍛婃綇閻у摜锟?

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
            AEStyleRenderer.drawSectionHeader(g, this.font,
                    Component.translatable("screen.neotab.section.health"),
                    layout.left(), layout.toScreenY(layout.themeSectionHeaderY()), layout.right());
            drawSettingRow(g, Component.translatable("screen.neotab.theme.health_mode"), layout.healthModeLabelBounds(), mouseX, mouseY);
            drawSettingRow(g, Component.translatable("screen.neotab.theme.tab_theme"),   layout.themeSelectLabelBounds(), mouseX, mouseY);
            renderThemeSelectorBackground(g, layout);
        }
        // Render scrollable widgets inside the clipped viewport
        renderScrollableWidgets(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        renderScrollbar(g, layout);
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
                } else if (button == customBorderOuterFactorButton || button == addCustomBorderColorButton) {
                    // 使用 AE 风格渲染其他按钮
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
        if (!button.visible) return;
        boolean hovered = button.isMouseOver(mouseX, mouseY);
        int bx = button.getX();
        int by = button.getY();
        int bw = button.getWidth();
        int bh = button.getHeight();
        
        if (selected) {
            // 选中状态：使用凹陷面板样式
            AEStyleRenderer.drawSunkenPanel(g, bx, by, bw, bh, AEStyleRenderer.COLOR_BUTTON_HOVER, 1);
        } else {
            // 未选中状态：使用普通按钮样式
            AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        }
        
        int textColor = selected 
            ? AEStyleRenderer.COLOR_SECTION_TEXT 
            : hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(this.font, button.getMessage(), bx + bw / 2, by + (bh - this.font.lineHeight) / 2, textColor);
    }

    private void renderThemeSelectorBackground(GuiGraphics g, Layout layout) {
        AEStyleRenderer.drawInputBackground(
            g,
            layout.left(),
            layout.toScreenY(layout.themeSelectorY()),
            layout.themeSelectorWidth(),
            layout.themeSelectorHeight()
        );
    }

    /**
     * 锟?AE2 濡炲瀛╅悧鍝ョ磼濡搫锟?CycleButton锟?
     * 闁煎啿鏈▍娆撴偨?AEStyleRenderer闁挎稑鏈弸鍐偓娑欘殜椤や線鎳濋崣澶屽锟?ON/OFF 闁绘鍩栭埀顑跨鐏忣垶宕氶崱鎰ㄥ亾?
     */
    private void renderAECycleButton(GuiGraphics g, CycleButton<?> cb, int mouseX, int mouseY) {
        if (!cb.visible) return;
        boolean hovered = cb.isMouseOver(mouseX, mouseY);
        int bx = cb.getX(), by = cb.getY(), bw = cb.getWidth(), bh = cb.getHeight();
        // AE2 濡炲瀛╅悧鎼佹嚄鐏炵偓锟?
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        // 闁哄倸娲ら悺褔鏁嶅顒€鐏查柡鍌ゅ幗濡叉悂宕ラ敂鑳 Boolean ON/OFF
        Component msg = cb.getMessage();
        String msgStr = msg.getString();
        int textColor;
        if (msgStr.equals("ON") || msgStr.equals("on")) {
            textColor = AEStyleRenderer.COLOR_ON;
        } else if (msgStr.equals("OFF") || msgStr.equals("off")) {
            textColor = AEStyleRenderer.COLOR_OFF;
        } else {
            // 閻㈩垽闄勯悥锝囩驳閸撗勭暠闁圭顦甸幐鎶芥晬閸繍锟?TPS ON闁挎稑顧€缁变即寮紙鐘电Ъ闁活潿鍔嶇€垫粓鏌﹂鑺ョ€悗娑欘殙锟?
            textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        }
        g.drawCenteredString(this.font, msg, bx + bw / 2, by + (bh - this.font.lineHeight) / 2, textColor);
    }

    private void renderFixedWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // AE2 濡炲瀛╅悧鍝ョ磼濡搫锟?Done/Cancel 闁圭顦甸幐鎶芥晬閸綆娲柣鈺傜墪鐢偊鎮ч崼鐔哄鐎殿喖楠忕槐?
        renderAEButton(g, this.doneButton, mouseX, mouseY);
        renderAEButton(g, this.cancelButton, mouseX, mouseY);
    }

    /**
     * 锟?AE2 濡炲瀛╅悧鍝ョ磼濡搫鐓戝☉鎾亾锟?Button闁挎稑鐭侀々顐︽儎閺嵮冩枾闁绘鐗婄憰鍡涘蓟閹炬墎锟?
     * 闁稿繐鐗忕划顖炲礆?AE2 闁煎啿鏈▍娆撴晬鐏炶棄鏅欓悹浣叉櫅鐢偊锟?Button 婵炴挸寮堕悡瀣棘閸パ呮憻闁挎稑鐗婇弸鍐偓娑欘殜椤や線鎳濋懠顒佹殸闁告鍠撴晶妤佸緞閸曨厽鍊為柨娑橆槶锟?
     */
    private void renderAEButton(GuiGraphics g, Button btn, int mouseX, int mouseY) {
        if (btn == null || !btn.visible) return;
        boolean hovered = btn.isMouseOver(mouseX, mouseY);
        int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
        // 缂備焦锚锟?AE2 濡炲瀛╅悧鎼佹嚄鐏炵偓鐝柨娑樼墣椤╊偊鎯勯弽褍鏂ч柣妤€鐗炵槐?        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        // 缂備焦锚閸╂寮崶褏鎽熼柨娑樼墕閻櫕绋夐銊хAE2 闁圭顦甸幐鎶藉棘閸パ呮憻闁肩顕滅槐?
        AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        int textColor = hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        g.drawCenteredString(this.font, btn.getMessage(), bx + bw / 2, by + (bh - this.font.lineHeight) / 2, textColor);
    }

    private void renderThemeOptionButton(GuiGraphics g, Button btn, String themeId, int mouseX, int mouseY) {
        if (!btn.visible) return;
        boolean hovered = btn.isMouseOver(mouseX, mouseY);
        boolean selected = themeId.equals(this.selectedThemeId);
        int bx = btn.getX();
        int by = btn.getY();
        int bw = btn.getWidth();
        int bh = btn.getHeight();

        if (selected) {
            AEStyleRenderer.drawSunkenPanel(g, bx, by, bw, bh, AEStyleRenderer.COLOR_BUTTON_HOVER, 1);
        } else {
            AEStyleRenderer.drawButton(g, bx, by, bw, bh, hovered);
        }

        int indicatorX = bx + 6;
        int indicatorY = by + (bh - THEME_INDICATOR_SIZE) / 2;
        AEStyleRenderer.drawOutline(g, indicatorX, indicatorY, THEME_INDICATOR_SIZE, THEME_INDICATOR_SIZE,
            AEStyleRenderer.COLOR_OUTLINE, 1);
        g.fill(indicatorX + 1, indicatorY + 1, indicatorX + THEME_INDICATOR_SIZE - 1, indicatorY + THEME_INDICATOR_SIZE - 1, 0xFF7A8090);
        if (selected) {
            g.fill(indicatorX + 2, indicatorY + 2, indicatorX + THEME_INDICATOR_SIZE - 2, indicatorY + THEME_INDICATOR_SIZE - 2,
                AEStyleRenderer.COLOR_ON);
        }

        int textColor = selected
            ? AEStyleRenderer.COLOR_SECTION_TEXT
            : hovered ? AEStyleRenderer.COLOR_BUTTON_TEXT_HOVER : AEStyleRenderer.COLOR_BUTTON_TEXT;
        int textX = indicatorX + THEME_INDICATOR_SIZE + 8;
        int textY = by + (bh - this.font.lineHeight) / 2;
        g.drawString(this.font, btn.getMessage(), textX, textY, textColor, false);
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

    /** 婵犲﹥鑹炬慨鈺呭级闄囧娲焼閹炬剚鍟嶉幖杈捐缁辨瑦锟?mouseClicked 濞ｅ洦绻冪€垫梹绋夐埀顒勬嚊鏉堝墽绀嗛柕?*/
    private static final int SCROLL_TRACK_W = 14;

    private void renderScrollbar(GuiGraphics g, Layout layout) {
        if (layout.maxScroll() <= 0) return;
        // 婵犲﹥鑹炬慨鈺呭级閿涘嫭褰涢悹鎰綑閸炲鈧湱鎳撶亸顖炲矗閸忓懏娅犻柨娑樼灱閺嗏偓 8px 闂傚倻顥愮粣?
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

    // drawSectionHeader 鐎规瓕灏缓鑲╃矓鐠囨彃锟?AEStyleRenderer.drawSectionHeader()

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

    /** 閻㈩垽绠戦悾顒勫极鐎涙鍨肩紒娑樺⒔濞堟垵顕ｉ埀顒勫礂閾忣偄鐦婚梺绛嬪櫙缁辨繈寮崶褏鎽熼柡鍕⒔閵囨岸宕烽妸锕€鐦婚梺绛嬪枛閸炴挳鏌堥…鎺旂閻庣妫勭€规娊宕￠悩鍐插К闁轰焦娼欓崹顏堝Υ?*/
    private CycleButton<Boolean> newLabeledToggle(int x, int width, boolean initialValue, Component label) {
        return CycleButton.onOffBuilder(initialValue)
            .create(x, 0, width, INPUT_HEIGHT, label);
    }

    /** 閻炴稈鍋撻梺鎻掔箲濡绮堥悜妯绘珡闁哄绮岄崹蹇涘箲閵忊€崇樆闂佺瓔鍣槐娆戔偓鐟版湰锟?/ 闁告娲滅€氼參鏁嶆径鍫氬亾?*/
    private CycleButton<HealthDisplayMode> newHealthModeButton(int x, HealthDisplayMode initialValue) {
        return CycleButton.<HealthDisplayMode>builder(mode -> Component.translatable(
                    mode == HealthDisplayMode.FULL
                        ? "screen.neotab.theme.health_mode.full"
                        : "screen.neotab.theme.health_mode.compact"))
            .withValues(HealthDisplayMode.FULL, HealthDisplayMode.COMPACT)
            .withInitialValue(initialValue)
            .displayOnlyValue()
            .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY);
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
            if (layout.themeSelectLabelBounds().contains(mouseX, mouseY))
                return new HoverTarget(Component.translatable("screen.neotab.theme.tab_theme.tooltip"));
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
        placeScrollableWidget(this.healthDisplayEnabled, layout.toggleX(),           layout.toScreenY(layout.healthDisplayRowY()));
        placeScrollableWidget(this.footerCustomInput,    layout.left(),              layout.toScreenY(layout.footerCustomInputY()));
        // THEME tab 闁硅矇鍌涱偨
        placeScrollableWidget(this.healthDisplayMode,    layout.toggleX(),           layout.toScreenY(layout.healthModeRowY()));
        for (int i = 0; i < this.themeOptionButtons.size(); i++) {
            Button button = this.themeOptionButtons.get(i);
            button.setX(layout.left() + THEME_LIST_INSET);
            button.setY(layout.toScreenY(layout.themeSelectorY() + THEME_LIST_INSET + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            button.setWidth(layout.themeSelectorWidth() - THEME_LIST_INSET * 2);
        }
        // 自定义主题配置组件（放在主题选择器下方）
        int customConfigStartY = layout.themeSelectorY() + layout.themeSelectorHeight() + THEME_LIST_TOP_GAP;
        int customConfigY = customConfigStartY;
        
        // 计算按钮宽度（只占左半边）
        int customButtonWidth = (layout.themeSelectorWidth() - THEME_LIST_INSET * 2 - 10) / 2;
        
        // 背景颜色按钮
        if (customBackgroundColorButton != null) {
            customBackgroundColorButton.setX(layout.left() + THEME_LIST_INSET);
            customBackgroundColorButton.setY(layout.toScreenY(customConfigY));
            customBackgroundColorButton.setWidth(customButtonWidth);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
        
        // 外边框深度按钮
        if (customBorderOuterFactorButton != null) {
            customBorderOuterFactorButton.setX(layout.left() + THEME_LIST_INSET);
            customBorderOuterFactorButton.setY(layout.toScreenY(customConfigY));
            customBorderOuterFactorButton.setWidth(customButtonWidth);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
        
        // 动画开关
        if (customAnimationToggle != null) {
            customAnimationToggle.setX(layout.left() + THEME_LIST_INSET);
            customAnimationToggle.setY(layout.toScreenY(customConfigY));
            customAnimationToggle.setWidth(customButtonWidth);
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 10;  // 额外间距
        
        // 嵌入式颜色选择器（放在右侧）
        if (embeddedColorPicker != null) {
            int pickerX = layout.left() + THEME_LIST_INSET + customButtonWidth + 20;
            int pickerY = layout.toScreenY(customConfigStartY);
            embeddedColorPicker.setX(pickerX);
            embeddedColorPicker.setY(pickerY);
        }
        
        // 边框颜色按钮列表
        List<Integer> borderColors = customThemeConfig.getBorderColors();
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
        
        Layout layout = buildLayout();
        List<Integer> colors = customThemeConfig.getBorderColors();
        
        // 计算按钮宽度（只占左半边）
        int customButtonWidth = (layout.themeSelectorWidth() - THEME_LIST_INSET * 2 - 10) / 2;
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
                }
            ).bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT).build());
        }
        
        // 更新可见性
        syncTabWidgetVisibility();
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
            this.healthDisplayEnabled.getValue(),
            this.healthDisplayMode.getValue(),
            this.selectedThemeId,
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
     * 闂佹彃绉堕悾濠氬极閺夋垹鐐婂銈囨暬濞间即鎯冮崟顐ゎ伌閻忕偐鍋撻柕?
     * Tab 闁哄秴绻愬畷浼村箲椤旈棿绠〒?TAB_BAR_WIDTH 闁稿秴绻掔粈宀勬晬鐏炶棄鏁堕悗鍦嚀鐏忣垶宕烽妸銉ュ緭闁告瑥鍘栭弲鍫曞Υ?
     */
    private Layout buildLayout() {
        // Tab 闁哄秴绻楅幑锝嗘叏?X闁挎稒鑹鹃惈鍡涚嵁閺囩喐瀵滄鐐插暱閻櫕绋夐鐐村€甸柨娑樿嫰閸炲鈧湱鎳撶亸顖氼啅閿曚胶鐝堕柛鎰Т缁舵艾锟?TAB_BAR_WIDTH
        int contentWidth = Math.min(MAX_CONTENT_WIDTH, this.width - CONTENT_SIDE_PADDING - TAB_BAR_WIDTH);
        // 闁轰胶绻濈紞瀣锤濡ゅ绀凾ab锟?+ 闁告劕鎳庨鎰板礌閻氬绀嗛悘鐐叉噸锟?
        int totalWidth = TAB_BAR_WIDTH + TAB_CONTENT_GAP + contentWidth;
        int blockLeft = (this.width - totalWidth) / 2;
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

        // Theme tab layout
        int themeSectionHeaderY = CONTENT_TOP_PADDING;
        int healthModeRowY      = themeSectionHeaderY + SECTION_HEADER_HEIGHT;
        int themeSelectRowY     = healthModeRowY + ROW_HEIGHT + ROW_GAP;
        int themeSelectorY      = themeSelectRowY + ROW_HEIGHT + THEME_LIST_TOP_GAP;
        int themeSelectorWidth  = contentWidth - 6;
        int themeSelectorHeight = themeSelectorHeight(TabThemeRegistry.ids().size());

        // Content height for the active tab
        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            contentHeight = footerRowY + ROW_HEIGHT;
        } else {
            // Theme tab: health mode plus theme selector
            contentHeight = themeSelectorY + themeSelectorHeight;
            // 如果选中 custom 主题，需要为配置组件预留空间
            if ("custom".equals(selectedThemeId)) {
                contentHeight += THEME_LIST_TOP_GAP;
                // 背景颜色按钮
                contentHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
                // 外边框深度按钮
                contentHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
                // 动画开关
                contentHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 10;
                // 边框颜色按钮列表
                int borderColorCount = customThemeConfig.getBorderColors().size();
                contentHeight += borderColorCount * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP);
                // 添加按钮（如果显示）
                if (borderColorCount < 7) {
                    contentHeight += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
                }
            }
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
            themeSectionHeaderY, healthModeRowY, themeSelectRowY, themeSelectorY, themeSelectorWidth, themeSelectorHeight,
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
            labelBounds(Component.translatable("screen.neotab.theme.health_mode"), left, viewportTop - this.scrollOffset + healthModeRowY, labelWidth, this.font),
            labelBounds(Component.translatable("screen.neotab.theme.tab_theme"),   left, viewportTop - this.scrollOffset + themeSelectRowY,  labelWidth, this.font)
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
        int themeSectionHeaderY, int healthModeRowY, int themeSelectRowY,
        int themeSelectorY, int themeSelectorWidth, int themeSelectorHeight,
        LabelBounds topTitleLabelBounds, LabelBounds topContentLabelBounds,
        LabelBounds betterPingLabelBounds, LabelBounds onlineDurationLabelBounds,
        LabelBounds titleLabelBounds, LabelBounds healthDisplayLabelBounds,
        LabelBounds footerCustomLabelBounds,
        LabelBounds footerFirstLabelBounds, LabelBounds footerSecondLabelBounds,
        LabelBounds footerThirdLabelBounds,
        LabelBounds healthModeLabelBounds,
        LabelBounds themeSelectLabelBounds
    ) {
        private int toScreenY(int contentY) {
            return this.viewportTop - this.scrollOffset + contentY;
        }
        private int scissorLeft() { return Math.max(0, this.left - 2); }
        private int scissorRight() { return this.right + 10; }
    }
}

