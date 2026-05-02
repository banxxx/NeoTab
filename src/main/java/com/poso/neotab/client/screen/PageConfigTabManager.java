package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ImprovedRichTextMultiLineEditBox;
import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
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
    private static final int TOGGLE_WIDTH           = 56;
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
        int CARD_PADDING = 10;  // 卡片内边距
        int inputWidth = layout.contentWidth() - CARD_PADDING * 2;  // 输入框宽度 = 卡片宽度 - 左右padding
        
        this.topTitleEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.topTitleEnabled()));
        applyPolicyToWidget(topTitleEnabled, policy.allowTopTitleToggle(), screen.getScreenMode(), policy);

        this.topTitleInput = screen.addWidget(new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, TITLE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.top_title_hint"), 
            Component.translatable("screen.neotab.top.title")));
        this.topTitleInput.setMaxVisibleLength(TabConfig.MAX_TOP_TITLE_LENGTH);
        this.topTitleInput.setValue(cfg.topTitleText());
        applyPolicyToWidget(topTitleInput, policy.allowTopTitleEdit(), screen.getScreenMode(), policy);

        this.topContentEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.topContentEnabled()));
        applyPolicyToWidget(topContentEnabled, policy.allowTopContentToggle(), screen.getScreenMode(), policy);

        this.topContentInput = screen.addWidget(new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, MULTILINE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.top_content_hint"), 
            Component.translatable("screen.neotab.top.content")));
        this.topContentInput.setMaxVisibleLength(TabConfig.MAX_TOP_CONTENT_LENGTH);
        this.topContentInput.setAutoResize(true);
        this.topContentInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.topContentInput.setValue(cfg.topContentText());
        applyPolicyToWidget(topContentInput, policy.allowTopContentEdit(), screen.getScreenMode(), policy);

        this.betterPingEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.betterPingEnabled()));
        applyPolicyToWidget(betterPingEnabled, policy.allowPingDisplayToggle(), screen.getScreenMode(), policy);

        this.onlineDurationEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.onlineDurationEnabled()));
        applyPolicyToWidget(onlineDurationEnabled, policy.allowDurationToggle(), screen.getScreenMode(), policy);

        this.titleEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.titleEnabled()));
        applyPolicyToWidget(titleEnabled, policy.allowTitleToggle(), screen.getScreenMode(), policy);

        this.healthDisplayEnabled = screen.addWidget(
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), cfg.healthDisplayEnabled()));
        applyPolicyToWidget(healthDisplayEnabled, policy.allowHealthDisplayToggle(), screen.getScreenMode(), policy);

        this.healthDisplayMode = screen.addWidget(
            NeoTabConfigWidgetFactory.newHealthModeButton(layout.toggleX(), cfg.healthDisplayMode(),
                screen::adjustLayoutConfigToCurrentLimits));
        applyPolicyToWidget(healthDisplayMode, policy.allowHealthModeChange(), screen.getScreenMode(), policy);

        // Layout columns/rows buttons
        com.poso.neotab.config.TabLayoutConfig layoutCfg = com.poso.neotab.config.TabLayoutConfig.get();

        this.layoutEnabledToggle = screen.addWidget(
            CycleButton.onOffBuilder(layoutCfg.isEnabled())
                .displayOnlyValue()
                .create(layout.toggleX(), 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                    (button, enabled) -> {
                        com.poso.neotab.config.TabLayoutConfig lc = com.poso.neotab.config.TabLayoutConfig.get();
                        lc.setEnabled(enabled);
                        com.poso.neotab.config.TabLayoutConfig.save(lc);
                        if (layoutColumnsButton != null) layoutColumnsButton.active = enabled;
                        if (layoutRowsButton    != null) layoutRowsButton.active    = enabled;
                    }));

        this.layoutColumnsButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.layout.columns", layoutCfg.getColumns()),
                button -> {
                    com.poso.neotab.config.TabLayoutConfig lc = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = lc.getColumns();
                    int maxColumns = lc.getMaxColumns();
                    int next = current >= maxColumns ? 1 : current + 1;
                    lc.setColumns(next);
                    com.poso.neotab.config.TabLayoutConfig.save(lc);
                    button.setMessage(Component.translatable("screen.neotab.layout.columns", next));
                })
            .bounds(layout.left(), 0, TOGGLE_WIDTH, INPUT_HEIGHT)
            .build());
        this.layoutColumnsButton.active = layoutCfg.isEnabled();

        this.layoutRowsButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.layout.rows", layoutCfg.getRowsPerColumn()),
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
                    button.setMessage(Component.translatable("screen.neotab.layout.rows", next));
                })
            .bounds(layout.left(), 0, TOGGLE_WIDTH, INPUT_HEIGHT)
            .build());
        this.layoutRowsButton.active = layoutCfg.isEnabled();
    }

    void initFooterWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig cfg, PlayerCustomizePolicy policy) {
        int CARD_PADDING = 10;  // 卡片内边距
        int inputWidth = layout.contentWidth() - CARD_PADDING * 2;
        
        this.footerCustomInput = screen.addWidget(new ImprovedRichTextMultiLineEditBox(
            screen.font(), layout.left(), 0, inputWidth, MULTILINE_INPUT_HEIGHT,
            Component.translatable("screen.neotab.input.footer_custom_hint"), 
            Component.translatable("screen.neotab.footer.custom")));
        this.footerCustomInput.setMaxVisibleLength(TabConfig.MAX_FOOTER_CUSTOM_LENGTH);
        this.footerCustomInput.setAutoResize(true);
        this.footerCustomInput.setMaxHeight(MULTILINE_INPUT_HEIGHT * 2);
        this.footerCustomInput.setValue(cfg.footerCustomText());
        applyPolicyToWidget(footerCustomInput, policy.allowFooterCustomEdit(), screen.getScreenMode(), policy);

        this.footerTpsEnabled = screen.addWidget(NeoTabConfigWidgetFactory.newToggle(
            layout.toggleX(), cfg.footerTpsEnabled()));
        applyPolicyToWidget(footerTpsEnabled, policy.allowFooterTpsToggle(), screen.getScreenMode(), policy);

        this.footerMsptEnabled = screen.addWidget(NeoTabConfigWidgetFactory.newToggle(
            layout.toggleX(), cfg.footerMsptEnabled()));
        applyPolicyToWidget(footerMsptEnabled, policy.allowFooterMsptToggle(), screen.getScreenMode(), policy);

        this.footerOnlineEnabled = screen.addWidget(NeoTabConfigWidgetFactory.newToggle(
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
            widget.setTooltip(Tooltip.create(
                Component.translatable("screen.neotab.locked_by_server")));
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
                layoutColumnsButton.setMessage(Component.translatable("screen.neotab.layout.columns", layoutCfg.getColumns()));
            }
            if (layoutRowsButton != null) {
                layoutRowsButton.setMessage(Component.translatable("screen.neotab.layout.rows", layoutCfg.getRowsPerColumn()));
            }
        }
    }
    /** Apply layout positions to all page config and footer widgets. */
    void applyLayout(NeoTabConfigScreenLayout.Layout layout) {
        int CARD_PADDING = 10;  // 卡片内边距
        
        // 标题信息卡片：开关在标题行右侧，输入框在副标题下方
        p(topTitleEnabled,       layout.toggleX(),            layout.toScreenY(layout.topTitleRowY() + CARD_PADDING));
        p(topTitleInput,         layout.left() + CARD_PADDING, layout.toScreenY(layout.topTitleInputY()));
        
        // 内容信息卡片
        p(topContentEnabled,     layout.toggleX(),            layout.toScreenY(layout.topContentRowY() + CARD_PADDING));
        p(topContentInput,       layout.left() + CARD_PADDING, layout.toScreenY(layout.topContentInputY()));
        
        // 玩家列表卡片（只有开关，无输入框）
        p(betterPingEnabled,     layout.toggleX(),            layout.toScreenY(layout.betterPingRowY() + CARD_PADDING));
        p(onlineDurationEnabled, layout.toggleX(),            layout.toScreenY(layout.onlineDurationRowY() + CARD_PADDING));
        p(titleEnabled,          layout.toggleX(),            layout.toScreenY(layout.titleRowY() + CARD_PADDING));
        p(healthDisplayEnabled,  layout.toggleX(),            layout.toScreenY(layout.healthDisplayRowY() + CARD_PADDING));
        
        // 底部自定义信息卡片
        p(footerCustomInput,     layout.left() + CARD_PADDING, layout.toScreenY(layout.footerCustomInputY()));
        
        // 主题页面的控件（卡片内定位）
        p(healthDisplayMode,     layout.toggleX(),            layout.toScreenY(layout.healthModeRowY() + CARD_PADDING));
        if (layoutEnabledToggle != null) p(layoutEnabledToggle, layout.toggleX(), layout.toScreenY(layout.layoutEnabledRowY() + CARD_PADDING));
        if (layoutColumnsButton != null) p(layoutColumnsButton, layout.toggleX(), layout.toScreenY(layout.layoutColumnsRowY() + CARD_PADDING));
        if (layoutRowsButton    != null) p(layoutRowsButton,    layout.toggleX(), layout.toScreenY(layout.layoutRowsRowY() + CARD_PADDING));
        
        // 底部三个卡片中的开关（位置在卡片内的右上角）
        p(footerTpsEnabled,      layout.toggleX(),            layout.toScreenY(layout.footerTpsRowY() + CARD_PADDING));
        p(footerMsptEnabled,     layout.toggleX(),            layout.toScreenY(layout.footerMsptRowY() + CARD_PADDING));
        p(footerOnlineEnabled,   layout.toggleX(),            layout.toScreenY(layout.footerOnlineRowY() + CARD_PADDING));
    }

    private void p(net.minecraft.client.gui.components.AbstractWidget w, int x, int y) { w.setX(x); w.setY(y); }

}
