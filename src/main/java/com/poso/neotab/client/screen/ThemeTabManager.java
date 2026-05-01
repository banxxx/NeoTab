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
                    if (newLayout.maxScroll() > 0) {
                        screen.setScrollOffset(newLayout.maxScroll(), newLayout);
                    }
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
        for (int i = 0; i < themeOptionButtons.size(); i++) {
            Button btn = themeOptionButtons.get(i);
            btn.setX(layout.left() + THEME_LIST_INSET);
            btn.setY(layout.toScreenY(layout.themeSelectorY() + THEME_LIST_INSET + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            btn.setWidth(layout.themeSelectorWidth() - THEME_LIST_INSET * 2);
        }
        int customConfigStartY = layout.themeSelectorY() + layout.themeSelectorHeight() + THEME_LIST_TOP_GAP;
        int customConfigY = customConfigStartY;
        int customButtonWidth = cachedCustomButtonWidth;
        if (resetToDefaultButton != null) {
            resetToDefaultButton.setX(layout.left() + THEME_LIST_INSET);
            resetToDefaultButton.setY(layout.toScreenY(customConfigY));
            resetToDefaultButton.setWidth(customButtonWidth);
        }
        if (showResetConfirmation && resetToDefaultButton != null) {
            int cw = 50, ch = 18;
            int baseX = resetToDefaultButton.getX() + resetToDefaultButton.getWidth();
            int baseY = resetToDefaultButton.getY() + resetToDefaultButton.getHeight();
            if (resetCancelButton != null) { resetCancelButton.setX(baseX - cw * 2); resetCancelButton.setY(baseY); resetCancelButton.setWidth(cw); resetCancelButton.setHeight(ch); }
            if (resetConfirmButton != null) { resetConfirmButton.setX(baseX - cw); resetConfirmButton.setY(baseY); resetConfirmButton.setWidth(cw); resetConfirmButton.setHeight(ch); }
        }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 15;
        int animHalfW = (customButtonWidth - 4) / 2;
        if (customAnimationToggle != null) { customAnimationToggle.setX(layout.left() + THEME_LIST_INSET); customAnimationToggle.setY(layout.toScreenY(customConfigY)); customAnimationToggle.setWidth(animHalfW); }
        if (customAnimationSpeedButton != null) { customAnimationSpeedButton.setX(layout.left() + THEME_LIST_INSET + animHalfW + 4); customAnimationSpeedButton.setY(layout.toScreenY(customConfigY)); customAnimationSpeedButton.setWidth(animHalfW); }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
        if (customBackgroundColorButton != null) { customBackgroundColorButton.setX(layout.left() + THEME_LIST_INSET); customBackgroundColorButton.setY(layout.toScreenY(customConfigY)); customBackgroundColorButton.setWidth(customButtonWidth); }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
        if (customBorderOuterFactorButton != null) { customBorderOuterFactorButton.setX(layout.left() + THEME_LIST_INSET); customBorderOuterFactorButton.setY(layout.toScreenY(customConfigY)); customBorderOuterFactorButton.setWidth(customButtonWidth); }
        customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP + 5 + 8;
        if (embeddedColorPicker != null) {
            int gap = 10, maxRight = layout.right() - THEME_LIST_INSET;
            int candidateX = layout.left() + THEME_LIST_INSET + customButtonWidth + gap;
            boolean fits = (candidateX + cachedColorPickerWidth <= maxRight);
            int pickerX, pickerY, pickerWidth;
            if (fits) { pickerX = candidateX; pickerY = layout.toScreenY(customConfigStartY); pickerWidth = cachedColorPickerWidth; }
            else { pickerX = layout.left() + THEME_LIST_INSET; pickerY = layout.toScreenY(customConfigY); pickerWidth = Math.min(maxRight - pickerX, cachedColorPickerWidth); customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP; }
            embeddedColorPicker.setX(pickerX); embeddedColorPicker.setY(pickerY); embeddedColorPicker.setWidth(pickerWidth);
        }
        java.util.List<Integer> borderColors = customThemeConfig != null ? customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
        for (int i = 0; i < borderColors.size(); i++) {
            int bi = i * 2;
            if (bi < customBorderColorButtons.size()) {
                int colorBtnW = customButtonWidth - 25;
                Button colorBtn = customBorderColorButtons.get(bi);
                colorBtn.setX(layout.left() + THEME_LIST_INSET); colorBtn.setY(layout.toScreenY(customConfigY)); colorBtn.setWidth(colorBtnW);
                if (bi + 1 < customBorderColorButtons.size()) {
                    Button delBtn = customBorderColorButtons.get(bi + 1);
                    delBtn.setX(layout.left() + THEME_LIST_INSET + colorBtnW + 5); delBtn.setY(layout.toScreenY(customConfigY)); delBtn.setWidth(20);
                }
                customConfigY += THEME_OPTION_HEIGHT + THEME_OPTION_GAP;
            }
        }
        if (addCustomBorderColorButton != null) {
            addCustomBorderColorButton.setX(layout.left() + THEME_LIST_INSET);
            addCustomBorderColorButton.setY(layout.toScreenY(customConfigY));
            addCustomBorderColorButton.setWidth(customButtonWidth);
        }
    }

}
