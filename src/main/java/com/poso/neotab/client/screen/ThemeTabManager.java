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
    net.minecraft.client.gui.components.EditBox customBackgroundHexInput;  // 背景颜色HEX输入框
    Button customBorderOuterFactorButton;
    net.minecraft.client.gui.components.EditBox customBorderOuterHexInput;  // 外层边框颜色HEX输入框
    CycleButton<Boolean> customAnimationToggle;
    Button customAnimationSpeedButton;
    Button resetToDefaultButton;
    Button resetConfirmButton;
    Button resetCancelButton;
    boolean showResetConfirmation = false;
    final List<Button> customBorderColorButtons = new ArrayList<>();
    final List<net.minecraft.client.gui.components.EditBox> customBorderHexInputs = new ArrayList<>();
    Button addCustomBorderColorButton;
    CustomThemeConfig customThemeConfig;
    ColorPickerWidget embeddedColorPicker;
    int cachedColorPickerWidth = 158;
    int cachedCustomButtonWidth = 60;
    String currentSelectedColorType = null;
    int currentSelectedBorderIndex = -1;
    // 颜色选择器浮动位置（相对于屏幕坐标）
    int colorPickerFloatX = 0;
    int colorPickerFloatY = 0;

    ThemeTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void initThemeWidgets(NeoTabConfigScreenLayout.Layout layout) {
        this.customThemeConfig = com.poso.neotab.theme.CustomThemeManager.get();

        int CARD_PADDING = 10;
        int availableWidth = layout.contentWidth() - CARD_PADDING * 2;
        int customButtonWidth = availableWidth;  // 全宽按钮（无左右分栏）

        // 颜色选择器宽度：适配卡片全宽
        int baseColorPickerWidth = 158;
        float maxScale = 1.0f;
        float minScale = 0.6f;
        float colorPickerScale = Math.max(minScale, Math.min(maxScale, (float) availableWidth / baseColorPickerWidth));
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

        // Background color button (色块按钮，用于打开颜色选择器)
        this.customBackgroundColorButton = screen.addWidget(Button.builder(
                Component.empty(),  // 空文本，因为我们会自定义渲染
                button -> {
                    // 点击色块打开/关闭颜色选择器
                    if ("background".equals(currentSelectedColorType)) {
                        currentSelectedColorType = null;
                        currentSelectedBorderIndex = -1;
                    } else {
                        currentSelectedColorType = "background";
                        currentSelectedBorderIndex = -1;
                        if (embeddedColorPicker != null) {
                            embeddedColorPicker.setColor(customThemeConfig.getBackgroundColor());
                        }
                    }
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, 24, 24)  // 色块大小：24x24（缩小）
            .build());

        // Background color HEX input
        this.customBackgroundHexInput = new net.minecraft.client.gui.components.EditBox(
            screen.font(), layout.left(), 0, 90, 24,  // 宽90px，高24px（缩小）
            Component.empty());
        this.customBackgroundHexInput.setMaxLength(9);  // #AARRGGBB = 9字符
        this.customBackgroundHexInput.setValue(String.format("#%08X", customThemeConfig.getBackgroundColor()));
        this.customBackgroundHexInput.setBordered(false);
        this.customBackgroundHexInput.setEditable(true);
        this.customBackgroundHexInput.setCanLoseFocus(true);
        this.customBackgroundHexInput.setFocused(false);
        this.customBackgroundHexInput.setResponder(text -> {
            try {
                String clean = text.startsWith("#") ? text.substring(1) : text;
                if (clean.isEmpty()) {
                    customThemeConfig.setBackgroundColor(0xFFFFFFFF);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "background".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(0xFFFFFFFF);
                    }
                    return;
                }
                if (clean.length() == 6) {
                    int parsed = (int)(0xFF000000L | Long.parseLong(clean, 16));
                    customThemeConfig.setBackgroundColor(parsed);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "background".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(parsed);
                    }
                } else if (clean.length() == 8) {
                    int parsed = (int) Long.parseLong(clean, 16);
                    customThemeConfig.setBackgroundColor(parsed);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "background".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(parsed);
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
        screen.addWidget(this.customBackgroundHexInput);

        // Outer border color button (色块按钮，用于打开颜色选择器)
        this.customBorderOuterFactorButton = screen.addWidget(Button.builder(
                Component.empty(),  // 空文本，因为我们会自定义渲染
                button -> {
                    // 点击色块打开/关闭颜色选择器
                    if ("outer_border".equals(currentSelectedColorType)) {
                        currentSelectedColorType = null;
                        currentSelectedBorderIndex = -1;
                    } else {
                        currentSelectedColorType = "outer_border";
                        currentSelectedBorderIndex = -1;
                        if (embeddedColorPicker != null) {
                            embeddedColorPicker.setColor(customThemeConfig.getBorderOuterColor());
                        }
                    }
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, 24, 24)  // 色块大小：24x24（缩小）
            .build());

        // Outer border color HEX input
        this.customBorderOuterHexInput = new net.minecraft.client.gui.components.EditBox(
            screen.font(), layout.left(), 0, 90, 24,  // 宽90px，高24px（缩小）
            Component.empty());
        this.customBorderOuterHexInput.setMaxLength(9);  // #AARRGGBB = 9字符
        this.customBorderOuterHexInput.setValue(String.format("#%08X", customThemeConfig.getBorderOuterColor()));
        this.customBorderOuterHexInput.setBordered(false);
        this.customBorderOuterHexInput.setEditable(true);
        this.customBorderOuterHexInput.setCanLoseFocus(true);
        this.customBorderOuterHexInput.setFocused(false);
        this.customBorderOuterHexInput.setResponder(text -> {
            try {
                String clean = text.startsWith("#") ? text.substring(1) : text;
                if (clean.isEmpty()) {
                    customThemeConfig.setBorderOuterColor(0xFFB7AF9B);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "outer_border".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(0xFFB7AF9B);
                    }
                    return;
                }
                if (clean.length() == 6) {
                    int parsed = (int)(0xFF000000L | Long.parseLong(clean, 16));
                    customThemeConfig.setBorderOuterColor(parsed);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "outer_border".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(parsed);
                    }
                } else if (clean.length() == 8) {
                    int parsed = (int) Long.parseLong(clean, 16);
                    customThemeConfig.setBorderOuterColor(parsed);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (embeddedColorPicker != null && "outer_border".equals(currentSelectedColorType)) {
                        embeddedColorPicker.setColor(parsed);
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
        screen.addWidget(this.customBorderOuterHexInput);

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

        // Reset to default button (with icon)
        this.resetToDefaultButton = screen.addWidget(Button.builder(
                Component.literal("↺ ").append(Component.translatable("screen.neotab.custom_theme.reset_to_default")),
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

        // Embedded color picker (positioned inline below selected card by applyLayout)
        int pickerX = layout.left() + CARD_PADDING;
        int pickerY = 0;
        this.embeddedColorPicker = new ColorPickerWidget(pickerX, pickerY, screen.font(),
            0xFFFFFFFF,
            color -> {
                if ("background".equals(currentSelectedColorType)) {
                    customThemeConfig.setBackgroundColor(color);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    // 同步更新HEX输入框
                    if (customBackgroundHexInput != null) {
                        customBackgroundHexInput.setValue(String.format("#%08X", color));
                    }
                } else if ("outer_border".equals(currentSelectedColorType)) {
                    customThemeConfig.setBorderOuterColor(color);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    // 同步更新HEX输入框
                    if (customBorderOuterHexInput != null) {
                        customBorderOuterHexInput.setValue(String.format("#%08X", color));
                    }
                } else if (currentSelectedColorType != null && currentSelectedColorType.startsWith("border_")) {
                    if (currentSelectedBorderIndex >= 0) {
                        List<Integer> colors = new ArrayList<>(customThemeConfig.getBorderColors());
                        if (currentSelectedBorderIndex < colors.size()) {
                            colors.set(currentSelectedBorderIndex, color);
                            customThemeConfig.setBorderColors(colors);
                            com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                            
                            // 同步更新对应的HEX输入框
                            if (currentSelectedBorderIndex < customBorderHexInputs.size()) {
                                net.minecraft.client.gui.components.EditBox hexBox = customBorderHexInputs.get(currentSelectedBorderIndex);
                                hexBox.setValue(String.format("#%08X", color));
                            }
                        }
                    }
                }
            }, colorPickerScale, false);  // 设置为false，不显示内部HEX输入框
        screen.addWidget(this.embeddedColorPicker);

        currentSelectedColorType = null;
        currentSelectedBorderIndex = -1;
    }

    void rebuildCustomBorderColorButtons() {
        for (Button button : customBorderColorButtons) {
            screen.removeWidget(button);
        }
        customBorderColorButtons.clear();

        for (net.minecraft.client.gui.components.EditBox box : customBorderHexInputs) {
            screen.removeWidget(box);
        }
        customBorderHexInputs.clear();

        if (addCustomBorderColorButton != null) {
            screen.removeWidget(addCustomBorderColorButton);
            addCustomBorderColorButton = null;
        }

        if (customThemeConfig == null) {
            return;
        }

        NeoTabConfigScreenLayout.Layout layout = screen.buildLayout();
        List<Integer> colors = customThemeConfig.getBorderColors();

        int CARD_PADDING = 10;
        int delBtnW = 20;
        int hexInputW = 70;  // hex输入框宽度（减小）

        for (int i = 0; i < colors.size(); i++) {
            final int index = i;
            final int color = colors.get(i);

            // 颜色选择按钮（色块区域，点击打开颜色选择器）
            Button colorButton = screen.addWidget(Button.builder(
                Component.empty(),
                button -> {
                    String colorType = "border_" + index;
                    if (colorType.equals(currentSelectedColorType)) {
                        currentSelectedColorType = null;
                        currentSelectedBorderIndex = -1;
                    } else {
                        currentSelectedColorType = colorType;
                        currentSelectedBorderIndex = index;
                        if (embeddedColorPicker != null) {
                            embeddedColorPicker.setColor(color);
                        }
                    }
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                    screen.syncVisibility();
                }
            ).bounds(layout.left(), 0, 28, 28).build());
            customBorderColorButtons.add(colorButton);

            // 删除按钮
            Button deleteButton = screen.addWidget(Button.builder(
                Component.literal("×"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                    newColors.remove(index);
                    customThemeConfig.setBorderColors(newColors);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    if (currentSelectedBorderIndex == index) {
                        currentSelectedColorType = null;
                        currentSelectedBorderIndex = -1;
                    } else if (currentSelectedBorderIndex > index) {
                        currentSelectedBorderIndex--;
                        currentSelectedColorType = "border_" + currentSelectedBorderIndex;
                    }
                    rebuildCustomBorderColorButtons();
                    NeoTabConfigScreenLayout.Layout newLayout = screen.buildLayout();
                    screen.applyLayout(newLayout);
                }
            ).bounds(layout.left(), 0, delBtnW, 20).build());
            customBorderColorButtons.add(deleteButton);

            // Hex输入框（支持透明度，格式 #AARRGGBB）
            final net.minecraft.client.gui.components.EditBox hexBox =
                new net.minecraft.client.gui.components.EditBox(
                    screen.font(), layout.left(), 0, hexInputW, 20,
                    Component.empty());
            hexBox.setMaxLength(9);  // #AARRGGBB = 9字符
            hexBox.setValue(String.format("#%08X", color));
            hexBox.setBordered(false);  // 禁用默认深色背景，我们自己绘制白色背景
            hexBox.setEditable(true);   // 确保可编辑
            hexBox.setCanLoseFocus(true);  // 允许失去焦点
            hexBox.setFocused(false);   // 初始状态不聚焦
            hexBox.visible = false;     // 初始状态不可见，由syncVisibility控制
            hexBox.setResponder(text -> {
                try {
                    String clean = text.startsWith("#") ? text.substring(1) : text;
                    
                    // 如果输入为空或无效，重置为默认白色
                    if (clean.isEmpty()) {
                        int defaultColor = 0xFFFFFFFF;
                        List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                        if (index < newColors.size()) {
                            newColors.set(index, defaultColor);
                            customThemeConfig.setBorderColors(newColors);
                            com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                            if (embeddedColorPicker != null && ("border_" + index).equals(currentSelectedColorType)) {
                                embeddedColorPicker.setColor(defaultColor);
                            }
                        }
                        return;
                    }
                    
                    if (clean.length() == 6) {
                        int parsed = (int)(0xFF000000L | Long.parseLong(clean, 16));
                        List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                        if (index < newColors.size()) {
                            newColors.set(index, parsed);
                            customThemeConfig.setBorderColors(newColors);
                            com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                            // 同步更新颜色选择器
                            if (embeddedColorPicker != null && ("border_" + index).equals(currentSelectedColorType)) {
                                embeddedColorPicker.setColor(parsed);
                            }
                        }
                    } else if (clean.length() == 8) {
                        int parsed = (int) Long.parseLong(clean, 16);
                        List<Integer> newColors = new ArrayList<>(customThemeConfig.getBorderColors());
                        if (index < newColors.size()) {
                            newColors.set(index, parsed);
                            customThemeConfig.setBorderColors(newColors);
                            com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                            // 同步更新颜色选择器
                            if (embeddedColorPicker != null && ("border_" + index).equals(currentSelectedColorType)) {
                                embeddedColorPicker.setColor(parsed);
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {}
            });
            hexBox.visible = false;
            screen.addWidget(hexBox);
            customBorderHexInputs.add(hexBox);
        }

        if (colors.size() < 7) {
            // 添加按钮：紧凑样式，不全宽
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
                }
            ).bounds(layout.left(), 0, 120, 20).build());
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
        int CARD_GAP = 8;
        int TITLE_LINE_HEIGHT = 9;  // 约等于font.lineHeight
        int COLOR_ITEM_HEIGHT = THEME_OPTION_HEIGHT;  // 颜色行高度
        int PICKER_GAP = 6;  // 颜色行与颜色选择器之间的间距
        
        // 主题选择器按钮定位到卡片内（标题+副标题下方）
        int themeListStartY = layout.themeSelectorY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + 8;
        for (int i = 0; i < themeOptionButtons.size(); i++) {
            Button btn = themeOptionButtons.get(i);
            btn.setX(layout.left() + CARD_PADDING);
            btn.setY(layout.toScreenY(themeListStartY + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            btn.setWidth(layout.contentWidth() - CARD_PADDING * 2);
        }
        
        // 自定义主题配置控件定位
        if ("custom".equals(selectedThemeId)) {
            // 重置为默认按钮（靠右边框，基于卡片高度垂直居中）
            if (resetToDefaultButton != null) {
                int resetBtnW = 80;  // 按钮宽度（包含图标和文字）
                int resetBtnH = 18;  // 按钮高度（稍小一点，更精致）
                int resetCardY = layout.customResetRowY();
                int titleH = 9;  // 标题行高度
                int subtitleH = 9;  // 副标题行高度
                int gap = 2;  // 标题与副标题之间的间距
                int resetCardHeight = CARD_PADDING * 2 + titleH + gap + subtitleH + 8;  // 增加一些高度以容纳确认/取消按钮
                
                // 靠右边框位置：卡片右边界 - padding - 按钮宽度
                resetToDefaultButton.setX(layout.right() - CARD_PADDING - resetBtnW);
                // 基于卡片高度垂直居中：卡片顶部 + (卡片高度 - 按钮高度) / 2
                resetToDefaultButton.setY(layout.toScreenY(resetCardY + (resetCardHeight - resetBtnH) / 2));
                resetToDefaultButton.setWidth(resetBtnW);
                resetToDefaultButton.setHeight(resetBtnH);
            }
            
            // 动画效果卡片内的控件（动画开关和速度按钮在同一行，各占一半）
            int animY = layout.customAnimationRowY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + 8;
            int animAvailableWidth = layout.contentWidth() - CARD_PADDING * 2;
            int animGap = 4;
            int animButtonWidth = (animAvailableWidth - animGap) / 2;
            
            if (customAnimationToggle != null) {
                customAnimationToggle.setX(layout.left() + CARD_PADDING);
                customAnimationToggle.setY(layout.toScreenY(animY));
                customAnimationToggle.setWidth(animButtonWidth);
            }
            
            if (customAnimationSpeedButton != null) {
                customAnimationSpeedButton.setX(layout.left() + CARD_PADDING + animButtonWidth + animGap);
                customAnimationSpeedButton.setY(layout.toScreenY(animY));
                customAnimationSpeedButton.setWidth(animButtonWidth);
            }
            
            // ── 颜色配置：每个颜色项独立卡片 ──
            int cardW = layout.contentWidth();
            int btnW = cardW - CARD_PADDING * 2;
            
            // 计算颜色选择器高度
            int colorPickerH = embeddedColorPicker != null ? embeddedColorPicker.getHeight() : 130;
            int collapsedCardH = CARD_PADDING + COLOR_ITEM_HEIGHT + CARD_PADDING;
            int expandedCardH = CARD_PADDING + COLOR_ITEM_HEIGHT + PICKER_GAP + colorPickerH + CARD_PADDING;
            
            // 背景颜色卡片（色块 + HEX输入框布局，固定高度）
            int bgCardY = layout.customBgColorRowY();
            int swatchSize = 24;  // 色块大小（缩小）
            int hexInputW = 90;   // HEX输入框宽度（缩小）
            int gap = 12;  // 色块与输入框之间的间距
            int titleH = 9;  // 标题行高度（约等于font.lineHeight）
            int contentGap = 8;  // 标题与内容之间的间距
            
            if (customBackgroundColorButton != null) {
                // 色块按钮：标题下方，左侧
                customBackgroundColorButton.setX(layout.left() + CARD_PADDING);
                customBackgroundColorButton.setY(layout.toScreenY(bgCardY + CARD_PADDING + titleH + contentGap));
                customBackgroundColorButton.setWidth(swatchSize);
                customBackgroundColorButton.setHeight(swatchSize);
            }
            
            if (customBackgroundHexInput != null) {
                // HEX输入框：色块右侧
                customBackgroundHexInput.setX(layout.left() + CARD_PADDING + swatchSize + gap);
                customBackgroundHexInput.setY(layout.toScreenY(bgCardY + CARD_PADDING + titleH + contentGap));
                customBackgroundHexInput.setWidth(hexInputW);
                customBackgroundHexInput.setHeight(swatchSize);
            }
            
            // 外层边框颜色卡片（色块 + HEX输入框布局，固定高度）
            int outerCardY = layout.customOuterBorderRowY();
            if (customBorderOuterFactorButton != null) {
                // 色块按钮：标题下方，左侧
                customBorderOuterFactorButton.setX(layout.left() + CARD_PADDING);
                customBorderOuterFactorButton.setY(layout.toScreenY(outerCardY + CARD_PADDING + titleH + contentGap));
                customBorderOuterFactorButton.setWidth(swatchSize);
                customBorderOuterFactorButton.setHeight(swatchSize);
            }
            
            if (customBorderOuterHexInput != null) {
                // HEX输入框：色块右侧
                customBorderOuterHexInput.setX(layout.left() + CARD_PADDING + swatchSize + gap);
                customBorderOuterHexInput.setY(layout.toScreenY(outerCardY + CARD_PADDING + titleH + contentGap));
                customBorderOuterHexInput.setWidth(hexInputW);
                customBorderOuterHexInput.setHeight(swatchSize);
            }
            
            // 颜色选择器浮动定位（在色块下方）
            if (embeddedColorPicker != null && embeddedColorPicker.visible) {
                if ("background".equals(currentSelectedColorType) && customBackgroundColorButton != null) {
                    embeddedColorPicker.setX(customBackgroundColorButton.getX());
                    embeddedColorPicker.setY(customBackgroundColorButton.getY() + swatchSize + 4);
                } else if ("outer_border".equals(currentSelectedColorType) && customBorderOuterFactorButton != null) {
                    embeddedColorPicker.setX(customBorderOuterFactorButton.getX());
                    embeddedColorPicker.setY(customBorderOuterFactorButton.getY() + swatchSize + 4);
                }
            }
            
            // 边框颜色大卡片（一个卡片包含所有边框颜色项 + 添加按钮）
            java.util.List<Integer> borderColors = customThemeConfig != null ? 
                customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
            int borderItemH = THEME_OPTION_HEIGHT + 12;  // 每个颜色行高度（增加4像素）
            int borderItemGap = 8;  // 行间距
            int borderCardInnerH = TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + 8;  // 标题+副标题
            int rowStartY = layout.customAddBorderColorRowY() + CARD_PADDING + borderCardInnerH;
            int delBtnW = 20;
            int borderSwatchSize = 16;  // 边框颜色色块固定大小（小巧，参考HTML原型 color-preview）
            int hexInputH = 14;   // 输入框固定高度（小巧）
            int borderHexInputW = 70;  // 边框颜色HEX输入框宽度
            
            for (int i = 0; i < borderColors.size(); i++) {
                int bi = i * 2;
                int rowY = rowStartY + i * (borderItemH + borderItemGap);
                int swatchX = layout.left() + CARD_PADDING + 8;
                int swatchY = rowY + (borderItemH - borderSwatchSize) / 2;
                
                if (bi < customBorderColorButtons.size()) {
                    // 色块按钮（点击打开颜色选择器）
                    Button colorBtn = customBorderColorButtons.get(bi);
                    colorBtn.setX(swatchX);
                    colorBtn.setY(layout.toScreenY(swatchY));
                    colorBtn.setWidth(borderSwatchSize);
                    colorBtn.setHeight(borderSwatchSize);
                    
                    // 删除按钮：右侧，垂直居中
                    if (bi + 1 < customBorderColorButtons.size()) {
                        Button delBtn = customBorderColorButtons.get(bi + 1);
                        delBtn.setX(layout.left() + CARD_PADDING + btnW - delBtnW);
                        delBtn.setY(layout.toScreenY(rowY + (borderItemH - hexInputH) / 2));
                        delBtn.setWidth(delBtnW);
                        delBtn.setHeight(hexInputH);
                    }
                }
                
                // Hex输入框（色块右侧，垂直居中于行内）
                if (i < customBorderHexInputs.size()) {
                    net.minecraft.client.gui.components.EditBox hexBox = customBorderHexInputs.get(i);
                    hexBox.setX(swatchX + borderSwatchSize + 8);
                    // 垂直居中：固定高度，居中于borderItemH行内
                    hexBox.setY(layout.toScreenY(rowY + (borderItemH - hexInputH) / 2));
                    hexBox.setWidth(borderHexInputW);
                    hexBox.setHeight(hexInputH);
                    hexBox.visible = true;  // 确保可见
                }
                
                // 颜色选择器：浮动在色块下方（当此项被选中时）
                String colorType = "border_" + i;
                if (colorType.equals(currentSelectedColorType) && embeddedColorPicker != null) {
                    // 定位在色块正下方
                    embeddedColorPicker.setX(swatchX);
                    embeddedColorPicker.setY(layout.toScreenY(swatchY) + swatchSize + 4);
                    colorPickerFloatX = swatchX;
                    colorPickerFloatY = layout.toScreenY(swatchY) + swatchSize + 4;
                }
            }
            
            // 添加边框颜色按钮（在大卡片右上角，与标题同一行）
            if (addCustomBorderColorButton != null) {
                int addBtnW = 100;  // 按钮宽度
                int addBtnH = 18;   // 按钮高度（稍小一点，更精致）
                int borderCardY = layout.customAddBorderColorRowY();
                // 右上角位置：卡片右边界 - padding - 按钮宽度
                addCustomBorderColorButton.setX(layout.right() - CARD_PADDING - addBtnW);
                // 与标题同一行：卡片顶部 + padding + (标题高度 - 按钮高度) / 2，实现垂直居中
                addCustomBorderColorButton.setY(layout.toScreenY(borderCardY + CARD_PADDING + (TITLE_LINE_HEIGHT - addBtnH) / 2));
                addCustomBorderColorButton.setWidth(addBtnW);
                addCustomBorderColorButton.setHeight(addBtnH);
            }
            
            // 重置确认/取消按钮（弹出式，总宽度与重置按钮一致）
            if (showResetConfirmation && resetToDefaultButton != null) {
                int resetBtnW = 80;  // 重置按钮宽度
                int confirmCancelBtnW = resetBtnW / 2;  // 每个按钮宽度为重置按钮的一半
                int confirmCancelBtnH = 18;
                int btnGap = 2;  // 确认和取消按钮之间的间距
                
                // 实际每个按钮宽度需要减去间距
                int actualBtnW = (resetBtnW - btnGap) / 2;
                
                // 确认和取消按钮位于重置按钮正下方，总宽度与重置按钮一致
                int baseX = resetToDefaultButton.getX();
                int baseY = resetToDefaultButton.getY() + resetToDefaultButton.getHeight() + 4;  // 重置按钮下方4px
                
                if (resetCancelButton != null) { 
                    resetCancelButton.setX(baseX); 
                    resetCancelButton.setY(baseY); 
                    resetCancelButton.setWidth(actualBtnW); 
                    resetCancelButton.setHeight(confirmCancelBtnH); 
                }
                if (resetConfirmButton != null) { 
                    resetConfirmButton.setX(baseX + actualBtnW + btnGap); 
                    resetConfirmButton.setY(baseY); 
                    resetConfirmButton.setWidth(actualBtnW); 
                    resetConfirmButton.setHeight(confirmCancelBtnH); 
                }
            }
        }
    }

}
