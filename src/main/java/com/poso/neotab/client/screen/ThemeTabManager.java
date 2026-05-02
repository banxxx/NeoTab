package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ColorPickerWidget;
import com.poso.neotab.theme.CustomThemeConfig;
import com.poso.neotab.theme.TabThemeRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Theme tab widgets for NeoTabConfigScreen.
 */
public class ThemeTabManager {

    private static final int THEME_OPTION_HEIGHT = 20;
    private static final int THEME_OPTION_GAP    = 4;
    private static final int THEME_LIST_INSET    = 4;
    private static final int THEME_LIST_TOP_GAP  = 4;

    private final NeoTabConfigScreen screen;

    // ── Theme tab fields ──────────────────────────────────────────────────────
    final List<Button> themeOptionButtons = new ArrayList<>();
    final List<String> themeOptionIds = new ArrayList<>();
    String selectedThemeId = "vanilla";

    // Layout buttons (shared with page config tab visually, but belong to theme tab)
    // These are actually owned by PageConfigTabManager; ThemeTabManager only references them
    // for visibility control. See NeoTabConfigScreen.syncTabWidgetVisibility().

    // Custom theme config widgets
    Button customBackgroundColorButton;
    Button customBorderOuterFactorButton;
    CycleButton<Boolean> customAnimationToggle;
    Button customAnimationSpeedButton;
    Button resetToDefaultButton;
    Button resetConfirmButton;
    Button resetCancelButton;
    boolean showResetConfirmation = false;
    final List<Button> customBorderColorButtons = new ArrayList<>();
    Button addCustomBorderColorButton;
    CustomThemeConfig customThemeConfig;
    ColorPickerWidget embeddedColorPicker;
    int cachedColorPickerWidth = 158;
    int cachedCustomButtonWidth = 60;
    String currentSelectedColorType = null;
    int currentSelectedBorderIndex = -1;

    ThemeTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void initThemeWidgets(NeoTabConfigScreenLayout.Layout layout) {
        this.customThemeConfig = com.poso.neotab.theme.CustomThemeManager.get();

        int availableWidth = layout.themeSelectorWidth() - THEME_LIST_INSET * 2;
        int customButtonWidth = Math.max(60, availableWidth / 2);

        int colorPickerGap = 10;
        int pickerStartX = layout.left() + THEME_LIST_INSET + customButtonWidth + colorPickerGap;
        int contentRightBoundary = layout.left() + layout.themeSelectorWidth() - THEME_LIST_INSET;
        int spaceForColorPicker = contentRightBoundary - pickerStartX;

        int baseColorPickerWidth = 158;
        float maxScale = 1.0f;
        float minScale = 0.6f;
        float colorPickerScale = Math.max(minScale, Math.min(maxScale, (float) spaceForColorPicker / baseColorPickerWidth));
        int scaledColorPickerWidth = (int) (baseColorPickerWidth * colorPickerScale);

        this.cachedColorPickerWidth = scaledColorPickerWidth;
        this.cachedCustomButtonWidth = customButtonWidth;

        // Theme option buttons
        for (String themeId : TabThemeRegistry.ids()) {
            Button optionButton = screen.addWidget(Button.builder(
                    Component.translatable("screen.neotab.theme." + themeId),
                    button -> {
                        this.selectedThemeId = themeId;
                        screen.syncVisibility();
                    })
                .bounds(layout.left(), 0, layout.themeSelectorWidth() - THEME_LIST_INSET * 2, THEME_OPTION_HEIGHT)
                .build());
            this.themeOptionButtons.add(optionButton);
            this.themeOptionIds.add(themeId);
        }

        // Background color button
        this.customBackgroundColorButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.background_color"),
                button -> {
                    currentSelectedColorType = "background";
                    currentSelectedBorderIndex = -1;
                    if (embeddedColorPicker != null) {
                        embeddedColorPicker.setColor(customThemeConfig.getBackgroundColor());
                    }
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());

        // Outer border color button
        this.customBorderOuterFactorButton = screen.addWidget(Button.builder(
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

        // Animation toggle (half width)
        int animHalfW = (customButtonWidth - 4) / 2;
        this.customAnimationToggle = screen.addWidget(CycleButton.booleanBuilder(
                Component.translatable("screen.neotab.custom_theme.animation.on"),
                Component.translatable("screen.neotab.custom_theme.animation.off"))
            .withInitialValue(customThemeConfig.isAnimationEnabled())
            .create(layout.left(), 0, animHalfW, THEME_OPTION_HEIGHT,
                    Component.translatable("screen.neotab.custom_theme.animation"),
                    (btn, value) -> {
                        customThemeConfig.setAnimationEnabled(value);
                        com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    }));

        // Animation speed button
        this.customAnimationSpeedButton = screen.addWidget(Button.builder(
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

        // Reset to default button
        this.resetToDefaultButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_to_default"),
                button -> {
                    showResetConfirmation = true;
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());

        // Reset confirm button (initially hidden)
        this.resetConfirmButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_confirm"),
                button -> {
                    this.customThemeConfig = com.poso.neotab.theme.CustomThemeConfig.defaults();
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    showResetConfirmation = false;
                    screen.reinit();
                })
            .bounds(layout.left(), 0, 50, 18)
            .build());

        // Reset cancel button (initially hidden)
        this.resetCancelButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_cancel"),
                button -> {
                    showResetConfirmation = false;
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, 50, 18)
            .build());

        // Embedded color picker
        int pickerX = layout.left() + THEME_LIST_INSET + customButtonWidth + colorPickerGap;
        int pickerY = 0;
        this.embeddedColorPicker = new ColorPickerWidget(pickerX, pickerY, screen.font(),
            0xFFFFFFFF,
            color -> {
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
            }, colorPickerScale);
        screen.addWidget(this.embeddedColorPicker);

        currentSelectedColorType = null;
        currentSelectedBorderIndex = -1;
    }

    void rebuildCustomBorderColorButtons() {
        for (Button button : customBorderColorButtons) {
            screen.removeWidget(button);
        }
        customBorderColorButtons.clear();

        if (addCustomBorderColorButton != null) {
            screen.removeWidget(addCustomBorderColorButton);
            addCustomBorderColorButton = null;
        }

        if (customThemeConfig == null) {
            return;
        }

        NeoTabConfigScreenLayout.Layout layout = screen.buildLayout();
        List<Integer> colors = customThemeConfig.getBorderColors();

        int customButtonWidth = cachedCustomButtonWidth;
        int colorButtonWidth = customButtonWidth - 25;

        for (int i = 0; i < colors.size(); i++) {
            final int index = i;
            final int color = colors.get(i);

            Button colorButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.border_color", i + 1),
                button -> {
                    currentSelectedColorType = "border_" + index;
                    currentSelectedBorderIndex = index;
                    if (embeddedColorPicker != null) {
                        embeddedColorPicker.setColor(color);
                    }
                }
            ).bounds(layout.left(), 0, colorButtonWidth, THEME_OPTION_HEIGHT).build());
            customBorderColorButtons.add(colorButton);

            Button deleteButton = screen.addWidget(Button.builder(
                Component.literal("×"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                    newColors.remove(index);
                    customThemeConfig.setBorderColors(newColors);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
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
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                }
            ).bounds(layout.left(), 0, 20, THEME_OPTION_HEIGHT).build());
            customBorderColorButtons.add(deleteButton);
        }

        if (colors.size() < 7) {
            this.addCustomBorderColorButton = screen.addWidget(Button.builder(
                Component.translatable("screen.neotab.custom_theme.add_border_color"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                    newColors.add(0xFFFFFFFF);
                    customThemeConfig.setBorderColors(newColors);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    rebuildCustomBorderColorButtons();
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                    // 移除自动滚动到底部的代码，保持当前滚动位置
                }
            ).bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT).build());
        }

        screen.syncVisibility();
    }

    static int themeSelectorHeight(int optionCount) {
        int rows = Math.max(optionCount, 1);
        return THEME_LIST_INSET * 2
            + rows * THEME_OPTION_HEIGHT
            + Math.max(0, rows - 1) * THEME_OPTION_GAP;
    }
    /** Apply layout positions to all theme tab widgets. */
    void applyLayout(NeoTabConfigScreenLayout.Layout layout) {
        int CARD_PADDING = 10;
        int TITLE_LINE_HEIGHT = 9;  // 约等于font.lineHeight
        int SUBTITLE_LINE_HEIGHT = 9;
        
        // 主题选择器按钮定位到卡片内（标题+副标题下方）
        int themeListStartY = layout.themeSelectorY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
        for (int i = 0; i < themeOptionButtons.size(); i++) {
            Button btn = themeOptionButtons.get(i);
            btn.setX(layout.left() + CARD_PADDING);
            btn.setY(layout.toScreenY(themeListStartY + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            btn.setWidth(layout.contentWidth() - CARD_PADDING * 2);
        }
        
        // 自定义主题配置控件定位
        if ("custom".equals(selectedThemeId)) {
            // 重置为默认按钮（独立卡片，右上角）
            if (resetToDefaultButton != null) {
                resetToDefaultButton.setX(layout.toggleX());
                resetToDefaultButton.setY(layout.toScreenY(layout.customResetRowY() + CARD_PADDING));
                resetToDefaultButton.setWidth(56);
            }
            
            // 动画效果卡片内的控件（动画开关和速度按钮在同一行，各占一半）
            int animY = layout.customAnimationRowY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
            // 计算可用宽度：从卡片左边到右边，减去padding
            int animAvailableWidth = layout.contentWidth() - CARD_PADDING * 2;
            int animGap = 4;  // 两个按钮之间的间距
            int animButtonWidth = (animAvailableWidth - animGap) / 2;  // 每个按钮占一半
            
            // 动画开关（左半部分）
            if (customAnimationToggle != null) {
                customAnimationToggle.setX(layout.left() + CARD_PADDING);
                customAnimationToggle.setY(layout.toScreenY(animY));
                customAnimationToggle.setWidth(animButtonWidth);
            }
            
            // 动画速度按钮（右半部分）
            if (customAnimationSpeedButton != null) {
                customAnimationSpeedButton.setX(layout.left() + CARD_PADDING + animButtonWidth + animGap);
                customAnimationSpeedButton.setY(layout.toScreenY(animY));
                customAnimationSpeedButton.setWidth(animButtonWidth);
            }
            
            // 颜色配置卡片内的控件（左右分栏布局）
            // 计算左右分栏尺寸
            int leftColumnWidth = (int)(layout.contentWidth() * 0.45f);  // 左侧占45%
            int rightColumnWidth = layout.contentWidth() - leftColumnWidth - CARD_PADDING * 3;  // 右侧占剩余空间
            int leftColumnX = layout.left() + CARD_PADDING;
            int rightColumnX = leftColumnX + leftColumnWidth + CARD_PADDING;
            
            // 左侧列起始Y坐标
            int leftY = layout.customBgColorRowY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
            
            // 背景颜色按钮（左侧）
            if (customBackgroundColorButton != null) {
                customBackgroundColorButton.setX(leftColumnX);
                customBackgroundColorButton.setY(layout.toScreenY(leftY));
                customBackgroundColorButton.setWidth(leftColumnWidth);
                leftY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
            
            // 外层边框颜色按钮（左侧）
            if (customBorderOuterFactorButton != null) {
                customBorderOuterFactorButton.setX(leftColumnX);
                customBorderOuterFactorButton.setY(layout.toScreenY(leftY));
                customBorderOuterFactorButton.setWidth(leftColumnWidth);
                leftY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
            
            // 边框颜色标题（左侧）
            leftY += TITLE_LINE_HEIGHT + 4;
            
            // 边框颜色按钮（左侧，每个颜色一行，带删除按钮）
            java.util.List<Integer> borderColors = customThemeConfig != null ? 
                customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
            for (int i = 0; i < borderColors.size(); i++) {
                int bi = i * 2;
                if (bi < customBorderColorButtons.size()) {
                    // 颜色选择按钮（左侧，占大部分宽度）
                    Button colorBtn = customBorderColorButtons.get(bi);
                    int colorBtnWidth = leftColumnWidth - 25;
                    colorBtn.setX(leftColumnX);
                    colorBtn.setY(layout.toScreenY(leftY));
                    colorBtn.setWidth(colorBtnWidth);
                    
                    // 删除按钮（颜色按钮右侧）
                    if (bi + 1 < customBorderColorButtons.size()) {
                        Button delBtn = customBorderColorButtons.get(bi + 1);
                        delBtn.setX(leftColumnX + colorBtnWidth + 2);
                        delBtn.setY(layout.toScreenY(leftY));
                        delBtn.setWidth(20);
                    }
                    leftY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
                }
            }
            
            // 添加边框颜色按钮（左侧）
            if (addCustomBorderColorButton != null) {
                addCustomBorderColorButton.setX(leftColumnX);
                addCustomBorderColorButton.setY(layout.toScreenY(leftY));
                addCustomBorderColorButton.setWidth(leftColumnWidth);
            }
            
            // 颜色选择器（右侧列）
            if (embeddedColorPicker != null) {
                int pickerY = layout.customBgColorRowY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + SUBTITLE_LINE_HEIGHT + 8;
                embeddedColorPicker.setX(rightColumnX);
                embeddedColorPicker.setY(layout.toScreenY(pickerY));
                // visible属性由syncTabWidgetVisibility()管理，不在这里设置
            }
            
            // 重置确认/取消按钮（弹出式）
            if (showResetConfirmation && resetToDefaultButton != null) {
                int cw = 50, ch = 18;
                int baseX = resetToDefaultButton.getX() + resetToDefaultButton.getWidth();
                int baseY = resetToDefaultButton.getY() + resetToDefaultButton.getHeight();
                if (resetCancelButton != null) { 
                    resetCancelButton.setX(baseX - cw * 2); 
                    resetCancelButton.setY(baseY); 
                    resetCancelButton.setWidth(cw); 
                    resetCancelButton.setHeight(ch); 
                }
                if (resetConfirmButton != null) { 
                    resetConfirmButton.setX(baseX - cw); 
                    resetConfirmButton.setY(baseY); 
                    resetConfirmButton.setWidth(cw); 
                    resetConfirmButton.setHeight(ch); 
                }
            }
        }
    }

}
