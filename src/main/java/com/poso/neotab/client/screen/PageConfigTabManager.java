package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import com.poso.neotab.util.ScreenAccessHelper;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Manages the Page Config tab widgets for NeoTabConfigScreen.
 */
public class PageConfigTabManager {

    private static final int ROW_HEIGHT             = 24;
    private static final int INPUT_HEIGHT           = 20;
    private static final int TITLE_INPUT_HEIGHT     = 60;
    private static final int MULTILINE_INPUT_HEIGHT = 60;
    private static final int TOGGLE_WIDTH           = 26;  // 适合Minecraft GUI比例的开关宽�?
    private static final int TOGGLE_HEIGHT          = 14;  // 适合Minecraft GUI比例的开关高�?
    private static final int LAYOUT_BUTTON_WIDTH    = 80;

    private final NeoTabConfigScreen screen;

    // ── Page Config tab fields ────────────────────────────────────────────────
    CycleButton<Boolean> topTitleEnabled;
    ImprovedRichTextMultiLineEditBox topTitleInput;
    CycleButton<Boolean> topContentEnabled;
    ImprovedRichTextMultiLineEditBox topContentInput;
    CycleButton<Boolean> betterPingEnabled;
    CycleButton<Boolean> onlineDurationEnabled;
    CycleButton<Boolean> titleEnabled;
    CycleButton<Boolean> healthDisplayEnabled;
    CycleButton<HealthDisplayMode> healthDisplayMode;
    ImprovedRichTextMultiLineEditBox footerCustomInput;
    CycleButton<Boolean> footerTpsEnabled;
    CycleButton<Boolean> footerMsptEnabled;
    CycleButton<Boolean> footerOnlineEnabled;

    // ── Theme tab layout fields (also managed here per original structure) ────
    CycleButton<Boolean> layoutEnabledToggle;
    Button layoutColumnsButton;
    Button layoutRowsButton;

    PageConfigTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void initPageConfigWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig cfg, PlayerCustomizePolicy policy) {
        int CARD_PADDING = 10;  // 卡片内边�?
        int inputWidth = layout.contentWidth() - CARD_PADDING * 2;  // 输入框宽�?= 卡片宽度 - 左右padding
        
        this.topTitleEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.topTitleEnabled()));
        applyPolicyToWidget(topTitleEnabled, policy.allowTopTitleToggle(), screen.getScreenMode(), policy);

        this.topTitleInput = ScreenAccessHelper.addWidget(screen, new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, TITLE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.top_title_hint"), 
            Component.translatable("screen.neotab.top.title")));
        this.topTitleInput.setMaxVisibleLength(TabConfig.MAX_TOP_TITLE_LENGTH);
        this.topTitleInput.setValue(cfg.topTitleText());
        applyPolicyToWidget(topTitleInput, policy.allowTopTitleEdit(), screen.getScreenMode(), policy);

        this.topContentEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.topContentEnabled()));
        applyPolicyToWidget(topContentEnabled, policy.allowTopContentToggle(), screen.getScreenMode(), policy);

        this.topContentInput = ScreenAccessHelper.addWidget(screen, new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, MULTILINE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.top_content_hint"), 
            Component.translatable("screen.neotab.top.content")));
        this.topContentInput.setMaxVisibleLength(TabConfig.MAX_TOP_CONTENT_LENGTH);
        this.topContentInput.setAutoResize(true);
        this.topContentInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.topContentInput.setValue(cfg.topContentText());
        applyPolicyToWidget(topContentInput, policy.allowTopContentEdit(), screen.getScreenMode(), policy);

        this.betterPingEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.betterPingEnabled()));
        applyPolicyToWidget(betterPingEnabled, policy.allowPingDisplayToggle(), screen.getScreenMode(), policy);

        this.onlineDurationEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.onlineDurationEnabled()));
        applyPolicyToWidget(onlineDurationEnabled, policy.allowDurationToggle(), screen.getScreenMode(), policy);

        this.titleEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.titleEnabled()));
        applyPolicyToWidget(titleEnabled, policy.allowTitleToggle(), screen.getScreenMode(), policy);

        this.healthDisplayEnabled = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.healthDisplayEnabled()));
        applyPolicyToWidget(healthDisplayEnabled, policy.allowHealthDisplayToggle(), screen.getScreenMode(), policy);

        this.healthDisplayMode = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newHealthModeButton(layout.toggleX(), cfg.healthDisplayMode(),
                screen::adjustLayoutConfigToCurrentLimits));
        applyPolicyToWidget(healthDisplayMode, policy.allowHealthModeChange(), screen.getScreenMode(), policy);

        // Layout columns/rows buttons
        com.poso.neotab.config.TabLayoutConfig layoutCfg = com.poso.neotab.config.TabLayoutConfig.get();

        this.layoutEnabledToggle = ScreenAccessHelper.addWidget(screen, 
            CycleButton.onOffBuilder(layoutCfg.isEnabled())
                .displayOnlyValue()
                .create(layout.toggleX(), 0, TOGGLE_WIDTH, TOGGLE_HEIGHT, CommonComponents.EMPTY,
                    (button, enabled) -> {
                        com.poso.neotab.config.TabLayoutConfig lc = com.poso.neotab.config.TabLayoutConfig.get();
                        lc.setEnabled(enabled);
                        com.poso.neotab.config.TabLayoutConfig.save(lc);
                        if (layoutColumnsButton != null) layoutColumnsButton.active = enabled;
                        if (layoutRowsButton    != null) layoutRowsButton.active    = enabled;
                    }));

        this.layoutColumnsButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.literal(String.valueOf(layoutCfg.getColumns())),
                button -> {
                    com.poso.neotab.config.TabLayoutConfig lc = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = lc.getColumns();
                    int maxColumns = lc.getMaxColumns();
                    int next = current >= maxColumns ? 1 : current + 1;
                    lc.setColumns(next);
                    com.poso.neotab.config.TabLayoutConfig.save(lc);
                    button.setMessage(Component.literal(String.valueOf(next)));
                })
            .bounds(layout.left(), 0, 60, INPUT_HEIGHT)
            .build());
        this.layoutColumnsButton.active = layoutCfg.isEnabled();

        this.layoutRowsButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.literal(String.valueOf(layoutCfg.getRowsPerColumn())),
                button -> {
                    com.poso.neotab.config.TabLayoutConfig lc = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = lc.getRowsPerColumn();
                    int maxRows = lc.getMaxRows();
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
                    if (next == current && current >= maxRows) {
                        next = 5;
                    }
                    lc.setRowsPerColumn(next);
                    com.poso.neotab.config.TabLayoutConfig.save(lc);
                    button.setMessage(Component.literal(String.valueOf(next)));
                })
            .bounds(layout.left(), 0, 60, INPUT_HEIGHT)
            .build());
        this.layoutRowsButton.active = layoutCfg.isEnabled();
    }

    void initFooterWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig cfg, PlayerCustomizePolicy policy) {
        int CARD_PADDING = 10;  // 卡片内边�?
        int inputWidth = layout.contentWidth() - CARD_PADDING * 2;
        
        this.footerCustomInput = ScreenAccessHelper.addWidget(screen, new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, MULTILINE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.footer_custom_hint"), 
            Component.translatable("screen.neotab.footer.custom")));
        this.footerCustomInput.setMaxVisibleLength(TabConfig.MAX_FOOTER_CUSTOM_LENGTH);
        this.footerCustomInput.setAutoResize(true);
        this.footerCustomInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.footerCustomInput.setValue(cfg.footerCustomText());
        applyPolicyToWidget(footerCustomInput, policy.allowFooterCustomEdit(), screen.getScreenMode(), policy);

        this.footerTpsEnabled = ScreenAccessHelper.addWidget(screen, NeoTabConfigWidgetFactory.newToggle(
            layout.toggleX(), cfg.footerTpsEnabled()));
        applyPolicyToWidget(footerTpsEnabled, policy.allowFooterTpsToggle(), screen.getScreenMode(), policy);

        this.footerMsptEnabled = ScreenAccessHelper.addWidget(screen, NeoTabConfigWidgetFactory.newToggle(
            layout.toggleX(), cfg.footerMsptEnabled()));
        applyPolicyToWidget(footerMsptEnabled, policy.allowFooterMsptToggle(), screen.getScreenMode(), policy);

        this.footerOnlineEnabled = ScreenAccessHelper.addWidget(screen, NeoTabConfigWidgetFactory.newToggle(
            layout.toggleX(), cfg.footerOnlineEnabled()));
        applyPolicyToWidget(footerOnlineEnabled, policy.allowFooterOnlineToggle(), screen.getScreenMode(), policy);
    }

    /**
     * Apply policy restriction to a widget.
     */
    static void applyPolicyToWidget(AbstractWidget widget, boolean allowed,
                                    NeoTabConfigScreen.ScreenMode screenMode,
                                    PlayerCustomizePolicy policy) {
        if (screenMode == NeoTabConfigScreen.ScreenMode.ADMIN) return;
        if (!allowed) {
            widget.active = false;
        }
    }

    /**
     * Adjust layout config to current limits (called when health mode changes or GUI scale changes).
     */
    void adjustLayoutConfigToCurrentLimits() {
        com.poso.neotab.config.TabLayoutConfig layoutCfg = com.poso.neotab.config.TabLayoutConfig.get();
        int maxColumns = layoutCfg.getMaxColumns();
        int maxRows = layoutCfg.getMaxRows();
        int currentColumns = layoutCfg.getColumns();
        int currentRows = layoutCfg.getRowsPerColumn();
        boolean needsAdjust = false;
        if (currentColumns > maxColumns) {
            layoutCfg.setColumns(maxColumns);
            needsAdjust = true;
        }
        if (currentRows > maxRows) {
            layoutCfg.setRowsPerColumn(maxRows);
            needsAdjust = true;
        }
        if (needsAdjust) {
            com.poso.neotab.config.TabLayoutConfig.save(layoutCfg);
            if (layoutColumnsButton != null) {
                layoutColumnsButton.setMessage(Component.literal(String.valueOf(layoutCfg.getColumns())));
            }
            if (layoutRowsButton != null) {
                layoutRowsButton.setMessage(Component.literal(String.valueOf(layoutCfg.getRowsPerColumn())));
            }
        }
    }
    /** Apply layout positions to all page config and footer widgets. */
    void applyLayout(NeoTabConfigScreenLayout.Layout layout) {
        int CARD_PADDING = 10;  // 卡片内边�?
        int titleH = screen.font().lineHeight;  // �?px
        int subtitleH = screen.font().lineHeight;  // �?px
        
        // 计算各类卡片高度（与buildLayoutImpl保持一致）
        int cardH_withSub    = CARD_PADDING + Math.max(TOGGLE_HEIGHT, titleH + 2 + subtitleH) + CARD_PADDING;
        int cardH_noSub      = CARD_PADDING + Math.max(TOGGLE_HEIGHT, titleH) + CARD_PADDING;
        
        // 辅助方法：计算开关在卡片中垂直居中的Y坐标
        // toggleCenterY = cardTop + (cardHeight - TOGGLE_HEIGHT) / 2
        
        // 标题信息卡片（有输入框）：开关放在顶部，不居�?
        p(topTitleEnabled,       layout.toggleX(),            layout.toScreenY(layout.topTitleRowY() + CARD_PADDING));
        p(topTitleInput,         layout.left() + CARD_PADDING, layout.toScreenY(layout.topTitleInputY()));
        
        // 内容信息卡片（有输入框）：开关放在顶部，不居�?
        p(topContentEnabled,     layout.toggleX(),            layout.toScreenY(layout.topContentRowY() + CARD_PADDING));
        p(topContentInput,       layout.left() + CARD_PADDING, layout.toScreenY(layout.topContentInputY()));
        
        // 玩家列表卡片（只有开关）：开关垂直居中于卡片
        p(betterPingEnabled,     layout.toggleX(), layout.toScreenY(layout.betterPingRowY()     + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        p(onlineDurationEnabled, layout.toggleX(), layout.toScreenY(layout.onlineDurationRowY() + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        p(titleEnabled,          layout.toggleX(), layout.toScreenY(layout.titleRowY()          + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        p(healthDisplayEnabled,  layout.toggleX(), layout.toScreenY(layout.healthDisplayRowY()  + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        
        // 底部自定义信息卡片（有输入框）：不居�?
        p(footerCustomInput,     layout.left() + CARD_PADDING, layout.toScreenY(layout.footerCustomInputY()));
        
        // 底部TPS/MSPT/在线人数卡片（只有开关）：垂直居�?
        p(footerTpsEnabled,      layout.toggleX(), layout.toScreenY(layout.footerTpsRowY()    + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        p(footerMsptEnabled,     layout.toggleX(), layout.toScreenY(layout.footerMsptRowY()   + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        p(footerOnlineEnabled,   layout.toggleX(), layout.toScreenY(layout.footerOnlineRowY() + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        
        // 主题页面的控件（只有开�?按钮）：垂直居中
        // healthDisplayMode 是宽按钮�?0px），使用INPUT_HEIGHT，垂直居�?
        // 需要向左偏移以对齐右边缘（60px�?vs 26px宽的toggleX�?
        int healthModeButtonW = 60;
        int cardH_withSub_inputH = CARD_PADDING + Math.max(INPUT_HEIGHT, titleH + 2 + subtitleH) + CARD_PADDING;
        int healthModeX = layout.right() - 6 - healthModeButtonW;
        p(healthDisplayMode, healthModeX, layout.toScreenY(layout.healthModeRowY() + (cardH_withSub_inputH - INPUT_HEIGHT) / 2));
        if (healthDisplayMode != null) {
            healthDisplayMode.setWidth(healthModeButtonW);
        }
        if (layoutEnabledToggle != null) p(layoutEnabledToggle, layout.toggleX(), layout.toScreenY(layout.layoutEnabledRowY()  + (cardH_withSub - TOGGLE_HEIGHT) / 2));
        if (layoutColumnsButton != null) {
            layoutColumnsButton.setWidth(60);
            p(layoutColumnsButton, layout.right() - 6 - 60, layout.toScreenY(layout.layoutColumnsRowY() + (cardH_withSub_inputH - INPUT_HEIGHT) / 2));
        }
        if (layoutRowsButton    != null) {
            layoutRowsButton.setWidth(60);
            p(layoutRowsButton,    layout.right() - 6 - 60, layout.toScreenY(layout.layoutRowsRowY()    + (cardH_withSub_inputH - INPUT_HEIGHT) / 2));
        }
    }

    private void p(net.minecraft.client.gui.components.AbstractWidget w, int x, int y) { 
        if (w != null) {
            w.setX(x); 
            w.setY(y); 
        }
    }

}
