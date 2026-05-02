package com.poso.neotab.client.screen;

import net.minecraft.network.chat.Component;

/**
 * Layout data classes for NeoTabConfigScreen.
 * Contains the Layout record, LabelBounds record, and HoverTarget record.
 */
public final class NeoTabConfigScreenLayout {

    private NeoTabConfigScreenLayout() {}

    /**
     * Immutable snapshot of all layout coordinates for a single render frame.
     */
    public record Layout(
        int contentWidth, int left, int right, int toggleX, int labelWidth,
        int scrollOffset, int viewportTop, int viewportBottom, int buttonBarTop,
        int topSectionHeaderY, int topTitleRowY, int topTitleInputY,
        int topContentRowY, int topContentInputY,
        int listSectionHeaderY, int betterPingRowY, int onlineDurationRowY, int titleRowY, int healthDisplayRowY,
        int footerSectionHeaderY, int footerCustomRowY, int footerCustomInputY, 
        int footerTpsRowY, int footerMsptRowY, int footerOnlineRowY, int footerRowY,
        int footerFirstColumnX, int footerSecondColumnX, int footerThirdColumnX,
        int footerFirstToggleX, int footerSecondToggleX, int footerThirdToggleX,
        int footerColumnWidth,
        int contentHeight, int maxScroll, int buttonWidth, int buttonY,
        int doneButtonX, int cancelButtonX,
        int tabBarX,
        int themeSectionHeaderY, int themeSelectorY, int themeSelectorWidth, int themeSelectorHeight,
        int customAnimationRowY, int customAnimSpeedRowY, int customBgColorRowY, 
        int customOuterBorderRowY, int customResetRowY, int customAddBorderColorRowY,
        int healthSectionHeaderY, int healthModeRowY,
        int layoutSectionHeaderY, int layoutButtonsY,
        int layoutEnabledRowY, int layoutColumnsRowY, int layoutRowsRowY, int layoutHintRowY,
        LabelBounds topTitleLabelBounds, LabelBounds topContentLabelBounds,
        LabelBounds betterPingLabelBounds, LabelBounds onlineDurationLabelBounds,
        LabelBounds titleLabelBounds, LabelBounds healthDisplayLabelBounds,
        LabelBounds footerCustomLabelBounds,
        LabelBounds footerFirstLabelBounds, LabelBounds footerSecondLabelBounds,
        LabelBounds footerThirdLabelBounds,
        LabelBounds healthModeLabelBounds
    ) {
        /** Convert content-space Y to screen-space Y (applies scroll offset). */
        public int toScreenY(int contentY) {
            return this.viewportTop - this.scrollOffset + contentY;
        }

        public int scissorLeft() { return Math.max(0, this.left - 2); }

        public int scissorRight() { return this.right + 10; }
        
        /** 获取边框颜色行的Y坐标（动态计算，基于折叠高度的近似值）
         *  注意：渲染代码应直接计算展开状态下的Y坐标，此方法仅供参考。 */
        public int customBorderColorRowY(int index) {
            int CARD_PADDING = 10;
            int THEME_OPTION_HEIGHT = 20;
            int CARD_GAP = 8;
            int collapsedCardH = CARD_PADDING + THEME_OPTION_HEIGHT + CARD_PADDING;
            return customOuterBorderRowY + collapsedCardH + CARD_GAP + index * (collapsedCardH + CARD_GAP);
        }
    }

    /**
     * Bounding box for a label, used for hover/tooltip detection.
     */
    public record LabelBounds(int x, int y, int width, int height) {
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY <= this.y + this.height;
        }
    }

    /**
     * A tooltip target shown when the mouse hovers over a label.
     */
    public record HoverTarget(Component tooltip) {}
}
