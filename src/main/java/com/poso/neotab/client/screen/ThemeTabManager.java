package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ColorPickerWidget;
import com.poso.neotab.theme.CustomThemeConfig;
import com.poso.neotab.theme.TabThemeRegistry;
import com.poso.neotab.util.ScreenAccessHelper;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
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

    // Custom theme config widgets
    Button customBackgroundColorButton;
    EditBox customBackgroundHexInput;  // 背景颜色HEX输入�?
    Button customBorderOuterFactorButton;
    EditBox customBorderOuterHexInput;  // 外层边框颜色HEX输入�?
    CycleButton<Boolean> customAnimationToggle;
    Button customAnimationSpeedButton;
    Button resetToDefaultButton;
    Button resetConfirmButton;
    Button resetCancelButton;
    boolean showResetConfirmation = false;
    final List<Button> customBorderColorButtons = new ArrayList<>();
    final List<EditBox> customBorderHexInputs = new ArrayList<>();
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
    // 防止颜色选择器→HEX输入框→颜色选择器的循环更新
    boolean updatingHexFromPicker = false;

    ThemeTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void initThemeWidgets(NeoTabConfigScreenLayout.Layout layout) {
        this.customThemeConfig = com.poso.neotab.theme.CustomThemeManager.get();

        int CARD_PADDING = 10;
        int availableWidth = layout.contentWidth() - CARD_PADDING * 2;
        int customButtonWidth = availableWidth;  // 全宽按钮（无左右分栏�?

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
            Button optionButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                    Component.translatable("screen.neotab.theme." + themeId),
                    button -> {
                        this.selectedThemeId = themeId;
                        screen.syncVisibility();
                    })
                .bounds(layout.left(), 0, layout.themeSelectorWidth() - THEME_LIST_INSET * 2, THEME_OPTION_HEIGHT)
                .build());
            optionButton.visible = false;  // 初始化时隐藏，由syncTabWidgetVisibility控制显示
            this.themeOptionButtons.add(optionButton);
            this.themeOptionIds.add(themeId);
        }

        // Background color button (色块按钮，用于打开颜色选择�?
        this.customBackgroundColorButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.empty(),  // 空文本，因为我们会自定义渲染
                button -> {
                    // 点击色块打开/关闭颜色选择�?
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
            .bounds(layout.left(), 0, 24, 24)  // 色块大小�?4x24（缩小）
            .build());
        this.customBackgroundColorButton.visible = false;  // 初始化时隐藏

        // Background color HEX input
        this.customBackgroundHexInput = new EditBox(
            screen.font(), layout.left(), 0, 90, 24,  // �?0px，高24px（缩小）
            Component.empty());
        this.customBackgroundHexInput.setMaxLength(9);  // #AARRGGBB = 9字符
        this.customBackgroundHexInput.setValue(String.format("#%08X", customThemeConfig.getBackgroundColor()));
        this.customBackgroundHexInput.setBordered(false);
        this.customBackgroundHexInput.setEditable(true);
        this.customBackgroundHexInput.setCanLoseFocus(true);
        this.customBackgroundHexInput.setFocused(false);
        this.customBackgroundHexInput.setResponder(text -> {
            if (updatingHexFromPicker) return;  // 防止颜色选择器→HEX→颜色选择器的循环
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
        this.customBackgroundHexInput.visible = false;  // 初始化时隐藏
        ScreenAccessHelper.addWidget(screen, this.customBackgroundHexInput);

        // Outer border color button (色块按钮，用于打开颜色选择器)
        this.customBorderOuterFactorButton = ScreenAccessHelper.addWidget(screen, Button.builder(
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
        this.customBorderOuterFactorButton.visible = false;  // 初始化时隐藏

        // Outer border color HEX input
        this.customBorderOuterHexInput = new EditBox(
            screen.font(), layout.left(), 0, 90, 24,  // 宽90px，高24px（缩小）
            Component.empty());
        this.customBorderOuterHexInput.setMaxLength(9);  // #AARRGGBB = 9字符
        this.customBorderOuterHexInput.setValue(String.format("#%08X", customThemeConfig.getBorderOuterColor()));
        this.customBorderOuterHexInput.setBordered(false);
        this.customBorderOuterHexInput.setEditable(true);
        this.customBorderOuterHexInput.setCanLoseFocus(true);
        this.customBorderOuterHexInput.setFocused(false);
        this.customBorderOuterHexInput.setResponder(text -> {
            if (updatingHexFromPicker) return;  // 防止颜色选择器→HEX→颜色选择器的循环
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
        this.customBorderOuterHexInput.visible = false;  // 初始化时隐藏
        ScreenAccessHelper.addWidget(screen, this.customBorderOuterHexInput);

        // 动画效果开关（与其他toggle完全一致的创建方式）
        this.customAnimationToggle = ScreenAccessHelper.addWidget(screen, 
            NeoTabConfigWidgetFactory.newToggle(layout.toggleX(), customThemeConfig.isAnimationEnabled(),
                (btn, value) -> {
                    customThemeConfig.setAnimationEnabled(value);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                }));
        this.customAnimationToggle.visible = false;  // 初始化时隐藏

        // Animation speed button
        this.customAnimationSpeedButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.literal(String.format("%dx", customThemeConfig.getAnimationSpeed())),
                button -> {
                    int current = customThemeConfig.getAnimationSpeed();
                    int next = current >= 3 ? 1 : current + 1;
                    customThemeConfig.setAnimationSpeed(next);
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    button.setMessage(Component.literal(String.format("%dx", next)));
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        this.customAnimationSpeedButton.visible = false;  // 初始化时隐藏

        // Reset to default button (with icon)
        this.resetToDefaultButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.literal("↺ ").append(Component.translatable("screen.neotab.custom_theme.reset_to_default")),
                button -> {
                    showResetConfirmation = true;
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        this.resetToDefaultButton.visible = false;  // 初始化时隐藏

        // Reset confirm button (initially hidden)
        this.resetConfirmButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_confirm"),
                button -> {
                    this.customThemeConfig = com.poso.neotab.theme.CustomThemeConfig.defaults();
                    com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                    showResetConfirmation = false;
                    // 只重建自定义主题相关控件，不调用 reinit()
                    // reinit() 会从 initialConfig 重读 selectedThemeId，导致主题选择被重置
                    screen.rebuildCustomThemeWidgets();
                })
            .bounds(layout.left(), 0, 50, 18)
            .build());
        this.resetConfirmButton.visible = false;  // 初始化时隐藏

        // Reset cancel button (initially hidden)
        this.resetCancelButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.translatable("screen.neotab.custom_theme.reset_cancel"),
                button -> {
                    showResetConfirmation = false;
                    screen.syncVisibility();
                })
            .bounds(layout.left(), 0, 50, 18)
            .build());
        this.resetCancelButton.visible = false;  // 初始化时隐藏

        // Add custom border color button
        this.addCustomBorderColorButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                Component.literal("+ ").append(Component.translatable("screen.neotab.custom_theme.add_border_color")),
                button -> {
                    List<Integer> colors = new ArrayList<>(customThemeConfig.getBorderColors());
                    if (colors.size() < 7) {
                        colors.add(0xFFFFFFFF);
                        customThemeConfig.setBorderColors(colors);
                        com.poso.neotab.theme.CustomThemeManager.save(customThemeConfig);
                        screen.rebuildCustomThemeWidgets();
                    }
                })
            .bounds(layout.left(), 0, customButtonWidth, THEME_OPTION_HEIGHT)
            .build());
        this.addCustomBorderColorButton.visible = false;  // 初始化时隐藏

        // Embedded color picker (positioned inline below selected card by applyLayout)
        int pickerX = layout.left() + CARD_PADDING;
        int pickerY = 0;
        this.embeddedColorPicker = new ColorPickerWidget(pickerX, pickerY, screen.font(),
            0xFFFFFFFF,
            color -> {
                if ("background".equals(currentSelectedColorType)) {
                    customThemeConfig.setBackgroundColor(color);
                    // 不在拖动时保存，由 mouseReleased 触发保存
                    if (customBackgroundHexInput != null) {
                        updatingHexFromPicker = true;
                        customBackgroundHexInput.setValue(String.format("#%08X", color));
                        updatingHexFromPicker = false;
                    }
                } else if ("outer_border".equals(currentSelectedColorType)) {
                    customThemeConfig.setBorderOuterColor(color);
                    if (customBorderOuterHexInput != null) {
                        updatingHexFromPicker = true;
                        customBorderOuterHexInput.setValue(String.format("#%08X", color));
                        updatingHexFromPicker = false;
                    }
                } else if (currentSelectedColorType != null && currentSelectedColorType.startsWith("border_")) {
                    if (currentSelectedBorderIndex >= 0) {
                        java.util.List<Integer> colors = new java.util.ArrayList<>(customThemeConfig.getBorderColors());
                        if (currentSelectedBorderIndex < colors.size()) {
                            colors.set(currentSelectedBorderIndex, color);
                            customThemeConfig.setBorderColors(colors);
                            if (currentSelectedBorderIndex < customBorderHexInputs.size()) {
                                updatingHexFromPicker = true;
                                customBorderHexInputs.get(currentSelectedBorderIndex)
                                    .setValue(String.format("#%08X", color));
                                updatingHexFromPicker = false;
                            }
                        }
                    }
                }
            }, colorPickerScale, false);
        this.embeddedColorPicker.visible = false;  // 初始化时隐藏
        ScreenAccessHelper.addWidget(screen, this.embeddedColorPicker);

        currentSelectedColorType = null;
        currentSelectedBorderIndex = -1;
    }

    void rebuildCustomBorderColorButtons() {
        for (Button button : customBorderColorButtons) {
            screen.removeWidgetDirect(button);
        }
        customBorderColorButtons.clear();

        for (EditBox box : customBorderHexInputs) {
            screen.removeWidgetDirect(box);
        }
        customBorderHexInputs.clear();

        if (addCustomBorderColorButton != null) {
            screen.removeWidgetDirect(addCustomBorderColorButton);
            addCustomBorderColorButton = null;
        }

        if (customThemeConfig == null) {
            return;
        }

        NeoTabConfigScreenLayout.Layout layout = screen.buildLayout();
        List<Integer> colors = customThemeConfig.getBorderColors();

        int CARD_PADDING = 10;
        int delBtnW = 20;
        int hexInputW = 70;  // hex输入框宽度（减小�?

        for (int i = 0; i < colors.size(); i++) {
            final int index = i;
            final int color = colors.get(i);

            // 颜色选择按钮（色块区域，点击打开颜色选择器）
            Button colorButton = screen.addWidgetDirect(Button.builder(
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

            // Hex输入框（支持透明度，格式 #AARRGGBB）�?
            final EditBox hexBox = new EditBox(
                    screen.font(), layout.left(), 0, hexInputW, 20,
                    Component.empty());
            hexBox.setMaxLength(9);  // #AARRGGBB = 9字符
            hexBox.setValue(String.format("#%08X", color));
            hexBox.setBordered(false);  // 禁用默认深色背景，我们自己绘制白色背�?
            hexBox.setEditable(true);   // 确保可编�?
            hexBox.setCanLoseFocus(true);  // 允许失去焦点
            hexBox.setFocused(false);   // 初始状态不聚焦
            hexBox.visible = false;     // 初始状态不可见，由syncVisibility控制
            hexBox.setResponder(text -> {
                if (updatingHexFromPicker) return;  // 防止颜色选择器→HEX→颜色选择器的循环
                try {
                    String clean = text.startsWith("#") ? text.substring(1) : text;
                    
                    // 如果输入为空或无效，重置为默认白�?
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
                            // 同步更新颜色选择�?
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
                            // 同步更新颜色选择�?
                            if (embeddedColorPicker != null && ("border_" + index).equals(currentSelectedColorType)) {
                                embeddedColorPicker.setColor(parsed);
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {}
            });
            hexBox.visible = false;
            screen.addWidgetDirect(hexBox);
            customBorderHexInputs.add(hexBox);
        }

        if (colors.size() < 7) {
            // 添加按钮：紧凑样式，不全�?
            this.addCustomBorderColorButton = screen.addWidgetDirect(Button.builder(
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
        int COLOR_ITEM_HEIGHT = THEME_OPTION_HEIGHT;  // 颜色行高�?
        int PICKER_GAP = 6;  // 颜色行与颜色选择器之间的间距
        
        // 主题选择器按钮定位到卡片内（标题+副标题下方）
        int themeListStartY = layout.themeSelectorY() + CARD_PADDING + TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + 8;
        for (int i = 0; i < themeOptionButtons.size(); i++) {
            Button btn = themeOptionButtons.get(i);
            btn.setX(layout.left() + CARD_PADDING);
            btn.setY(layout.toScreenY(themeListStartY + i * (THEME_OPTION_HEIGHT + THEME_OPTION_GAP)));
            btn.setWidth(layout.contentWidth() - CARD_PADDING * 2);
        }
        
        // 自定义主题配置控件定�?
        if ("custom".equals(selectedThemeId)) {
            // 重置为默认按钮（靠右边框，基于卡片高度垂直居中）
            if (resetToDefaultButton != null) {
                int resetBtnW = 80;  // 按钮宽度（包含图标和文字�?
                int resetBtnH = 18;  // 按钮高度（稍小一点，更精致）
                int resetCardY = layout.customResetRowY();
                int titleH = 9;  // 标题行高�?
                int subtitleH = 9;  // 副标题行高度
                int gap = 2;  // 标题与副标题之间的间�?
                int resetCardHeight = CARD_PADDING * 2 + titleH + gap + subtitleH + 8;  // 增加一些高度以容纳确认/取消按钮
                
                // 靠右边框位置：卡片右边界 - padding - 按钮宽度
                resetToDefaultButton.setX(layout.right() - CARD_PADDING - resetBtnW);
                // 基于卡片高度垂直居中：卡片顶�?+ (卡片高度 - 按钮高度) / 2
                resetToDefaultButton.setY(layout.toScreenY(resetCardY + (resetCardHeight - resetBtnH) / 2));
                resetToDefaultButton.setWidth(resetBtnW);
                resetToDefaultButton.setHeight(resetBtnH);
            }
            
            // 动画效果卡片：开关右对齐，垂直居�?
            int animToggleCardH = Math.max(CARD_PADDING + TITLE_LINE_HEIGHT + CARD_PADDING, CARD_PADDING + 14 + CARD_PADDING);
            int animToggleCenterY = layout.customAnimationRowY() + (animToggleCardH - 14) / 2;
            if (customAnimationToggle != null) {
                customAnimationToggle.setX(layout.toggleX());
                customAnimationToggle.setY(layout.toScreenY(animToggleCenterY));
                customAnimationToggle.setWidth(26);
                customAnimationToggle.setHeight(14);
            }

            // 动画速率卡片：速率按钮右对齐，垂直居中
            int animSpeedCardH = Math.max(CARD_PADDING + TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + CARD_PADDING, CARD_PADDING + 18 + CARD_PADDING);
            int animSpeedCenterY = layout.customAnimSpeedRowY() + (animSpeedCardH - 18) / 2;
            if (customAnimationSpeedButton != null) {
                customAnimationSpeedButton.setX(layout.right() - CARD_PADDING - 80);
                customAnimationSpeedButton.setY(layout.toScreenY(animSpeedCenterY));
                customAnimationSpeedButton.setWidth(80);
                customAnimationSpeedButton.setHeight(18);
            }
            
            // 背景颜色卡片（色块 + HEX输入框布局，固定高度）
            int bgCardY = layout.customBgColorRowY();
            int swatchSize = 24;  // 色块大小（缩小）
            int hexInputW = 90;   // HEX输入框宽度（缩小�?
            int gap = 12;  // 色块与输入框之间的间�?
            int titleH = 9;  // 标题行高度（约等于font.lineHeight�?
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

            // 外层边框颜色卡片
            int outerCardY = layout.customOuterBorderRowY();
            if (customBorderOuterFactorButton != null) {
                customBorderOuterFactorButton.setX(layout.left() + CARD_PADDING);
                customBorderOuterFactorButton.setY(layout.toScreenY(outerCardY + CARD_PADDING + titleH + contentGap));
                customBorderOuterFactorButton.setWidth(swatchSize);
                customBorderOuterFactorButton.setHeight(swatchSize);
            }
            if (customBorderOuterHexInput != null) {
                customBorderOuterHexInput.setX(layout.left() + CARD_PADDING + swatchSize + gap);
                customBorderOuterHexInput.setY(layout.toScreenY(outerCardY + CARD_PADDING + titleH + contentGap));
                customBorderOuterHexInput.setWidth(hexInputW);
                customBorderOuterHexInput.setHeight(swatchSize);
            }

            // 边框颜色列表（customBorderColorButtons 每个颜色一个色块按钮，索引 i 对应颜色 i）
            java.util.List<Integer> borderColors = customThemeConfig != null ?
                customThemeConfig.getBorderColors() : new java.util.ArrayList<>();
            int borderItemH = THEME_OPTION_HEIGHT + 12;
            int borderItemGap = 8;
            int borderCardInnerH = TITLE_LINE_HEIGHT + 2 + TITLE_LINE_HEIGHT + 8;
            int rowStartY = layout.customAddBorderColorRowY() + CARD_PADDING + borderCardInnerH;
            int borderSwatchSize = 16;
            int hexInputH = 14;
            int borderHexInputW = 70;

            for (int i = 0; i < borderColors.size(); i++) {
                int rowY = rowStartY + i * (borderItemH + borderItemGap);
                int swatchX = layout.left() + CARD_PADDING + 8;
                int swatchY = rowY + (borderItemH - borderSwatchSize) / 2;

                // 色块按钮（索引 i，一对一）
                if (i < customBorderColorButtons.size()) {
                    Button colorBtn = customBorderColorButtons.get(i);
                    colorBtn.setX(swatchX);
                    colorBtn.setY(layout.toScreenY(swatchY));
                    colorBtn.setWidth(borderSwatchSize);
                    colorBtn.setHeight(borderSwatchSize);
                }

                // HEX 输入框
                if (i < customBorderHexInputs.size()) {
                    EditBox hexBox = customBorderHexInputs.get(i);
                    hexBox.setX(swatchX + borderSwatchSize + 8);
                    hexBox.setY(layout.toScreenY(rowY + (borderItemH - hexInputH) / 2));
                    hexBox.setWidth(borderHexInputW);
                    hexBox.setHeight(hexInputH);
                }

                // 颜色选择器定位（当此项被选中时）
                String colorType = "border_" + i;
                if (colorType.equals(currentSelectedColorType) && embeddedColorPicker != null) {
                    embeddedColorPicker.setX(swatchX);
                    embeddedColorPicker.setY(layout.toScreenY(swatchY) + borderSwatchSize + 4);
                    colorPickerFloatX = swatchX;
                    colorPickerFloatY = layout.toScreenY(swatchY) + borderSwatchSize + 4;
                }
            }

            // 添加边框颜色按钮
            if (addCustomBorderColorButton != null) {
                int addBtnW = 100;
                int addBtnH = 18;
                int borderCardY = layout.customAddBorderColorRowY();
                addCustomBorderColorButton.setX(layout.right() - CARD_PADDING - addBtnW);
                addCustomBorderColorButton.setY(layout.toScreenY(borderCardY + CARD_PADDING + (TITLE_LINE_HEIGHT - addBtnH) / 2));
                addCustomBorderColorButton.setWidth(addBtnW);
                addCustomBorderColorButton.setHeight(addBtnH);
            }

            // 重置确认/取消按钮
            if (showResetConfirmation && resetToDefaultButton != null) {
                int resetBtnW = 80;
                int btnGap = 2;
                int actualBtnW = (resetBtnW - btnGap) / 2;
                int confirmCancelBtnH = 18;
                int baseX = resetToDefaultButton.getX();
                int baseY = resetToDefaultButton.getY() + resetToDefaultButton.getHeight() + 4;
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
