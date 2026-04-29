package com.poso.neotab.client.screen;

import com.poso.neotab.theme.CustomThemeConfig;
import com.poso.neotab.theme.CustomThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义主题配置界面。
 * 
 * <p>允许用户自定义边框颜色、背景颜色和动画效果。</p>
 */
public class CustomThemeConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int COLOR_PREVIEW_SIZE = 20;
    
    private final Screen parent;
    private CustomThemeConfig config;
    
    // UI 组件
    private Button backgroundColorButton;
    private Button borderOuterFactorButton;
    private CycleButton<Boolean> animationToggle;
    private final List<Button> borderColorButtons = new ArrayList<>();
    private Button addBorderColorButton;
    private Button saveButton;
    private Button cancelButton;
    private Button resetButton;
    
    public CustomThemeConfigScreen(Screen parent) {
        super(Component.translatable("screen.neotab.custom_theme.title"));
        this.parent = parent;
        this.config = CustomThemeManager.get();
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        int currentY = startY;
        
        // 标题
        // (在 render 方法中绘制)
        
        // 背景颜色按钮
        this.backgroundColorButton = Button.builder(
            Component.translatable("screen.neotab.custom_theme.background_color"),
            button -> openColorPicker(config.getBackgroundColor(), color -> {
                config.setBackgroundColor(color);
                updateButtonLabels();
            })
        ).bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(this.backgroundColorButton);
        currentY += BUTTON_HEIGHT + BUTTON_GAP + 10;
        
        // 外层边框颜色（已迁移到主配置界面颜色选择器，此处保留按钮但改为提示文字）
        this.borderOuterFactorButton = Button.builder(
            Component.translatable("screen.neotab.custom_theme.border_outer_color"),
            button -> { /* 功能已迁移到主配置界面 */ }
        ).bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(this.borderOuterFactorButton);
        currentY += BUTTON_HEIGHT + BUTTON_GAP + 10;
        
        // 动画开关
        this.animationToggle = CycleButton.booleanBuilder(
            Component.translatable("screen.neotab.custom_theme.animation.on"),
            Component.translatable("screen.neotab.custom_theme.animation.off")
        ).withInitialValue(config.isAnimationEnabled())
         .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                 Component.translatable("screen.neotab.custom_theme.animation"),
                 (button, value) -> config.setAnimationEnabled(value));
        addRenderableWidget(this.animationToggle);
        currentY += BUTTON_HEIGHT + BUTTON_GAP + 20;
        
        // 边框颜色列表标题
        currentY += 10; // 为标题留空间
        
        // 边框颜色按钮列表
        rebuildBorderColorButtons(currentY);
        
        // 底部按钮
        int bottomY = this.height - 30;
        
        this.saveButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            CustomThemeManager.save(config);
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 105, bottomY, 100, 20).build();
        addRenderableWidget(this.saveButton);
        
        this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 5, bottomY, 100, 20).build();
        addRenderableWidget(this.cancelButton);
        
        this.resetButton = Button.builder(
            Component.translatable("screen.neotab.custom_theme.reset"),
            button -> {
                config = CustomThemeConfig.defaults();
                CustomThemeManager.save(config);
                this.minecraft.setScreen(new CustomThemeConfigScreen(parent));
            }
        ).bounds(centerX - BUTTON_WIDTH / 2, bottomY - 25, BUTTON_WIDTH, 20).build();
        addRenderableWidget(this.resetButton);
        
        updateButtonLabels();
    }
    
    private void rebuildBorderColorButtons(int startY) {
        // 移除旧按钮
        for (Button button : borderColorButtons) {
            removeWidget(button);
        }
        borderColorButtons.clear();
        
        // 移除添加按钮
        if (addBorderColorButton != null) {
            removeWidget(addBorderColorButton);
        }
        
        int centerX = this.width / 2;
        int currentY = startY;
        
        List<Integer> colors = config.getBorderColors();
        for (int i = 0; i < colors.size(); i++) {
            final int index = i;
            final int color = colors.get(i);
            
            // 颜色按钮（左侧）
            Button colorButton = Button.builder(
                Component.translatable("screen.neotab.custom_theme.border_color", i + 1),
                button -> openColorPicker(color, newColor -> {
                    List<Integer> newColors = new ArrayList<>(config.getBorderColors());
                    newColors.set(index, newColor);
                    config.setBorderColors(newColors);
                    rebuildBorderColorButtons(startY);
                })
            ).bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH - 25, BUTTON_HEIGHT).build();
            addRenderableWidget(colorButton);
            borderColorButtons.add(colorButton);
            
            // 删除按钮（右侧）
            Button deleteButton = Button.builder(
                Component.literal("×"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(config.getBorderColors());
                    newColors.remove(index);
                    config.setBorderColors(newColors);
                    rebuildBorderColorButtons(startY);
                }
            ).bounds(centerX + BUTTON_WIDTH / 2 - 20, currentY, 20, BUTTON_HEIGHT).build();
            addRenderableWidget(deleteButton);
            borderColorButtons.add(deleteButton);
            
            currentY += BUTTON_HEIGHT + BUTTON_GAP;
        }
        
        // 添加颜色按钮（最多7个）
        if (colors.size() < 7) {
            this.addBorderColorButton = Button.builder(
                Component.translatable("screen.neotab.custom_theme.add_border_color"),
                button -> {
                    List<Integer> newColors = new ArrayList<>(config.getBorderColors());
                    newColors.add(0xFFFFFFFF); // 默认白色
                    config.setBorderColors(newColors);
                    rebuildBorderColorButtons(startY);
                }
            ).bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            addRenderableWidget(this.addBorderColorButton);
        }
    }
    
    private void openColorPicker(int initialColor, java.util.function.Consumer<Integer> onColorSelected) {
        this.minecraft.setScreen(new ColorPickerScreen(this, initialColor, color -> {
            onColorSelected.accept(color);
            this.minecraft.setScreen(this);
        }));
    }
    
    private void updateButtonLabels() {
        if (backgroundColorButton != null) {
            backgroundColorButton.setMessage(
                Component.translatable("screen.neotab.custom_theme.background_color")
            );
        }
        
        if (borderOuterFactorButton != null) {
            borderOuterFactorButton.setMessage(
                Component.translatable("screen.neotab.custom_theme.border_outer_color")
            );
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        
        // 绘制边框颜色列表标题
        int centerX = this.width / 2;
        int borderColorTitleY = 40 + BUTTON_HEIGHT + BUTTON_GAP + 10 + BUTTON_HEIGHT + BUTTON_GAP + 10 + BUTTON_HEIGHT + BUTTON_GAP + 20;
        graphics.drawCenteredString(this.font, 
            Component.translatable("screen.neotab.custom_theme.border_colors"), 
            centerX, borderColorTitleY, 0xFFFFFFFF);
        
        // 绘制颜色预览
        renderColorPreviews(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderColorPreviews(GuiGraphics graphics) {
        int centerX = this.width / 2;
        
        // 背景颜色预览
        if (backgroundColorButton != null) {
            int previewX = centerX + BUTTON_WIDTH / 2 + 5;
            int previewY = backgroundColorButton.getY();
            renderColorPreview(graphics, previewX, previewY, config.getBackgroundColor());
        }
        
        // 边框颜色预览
        List<Integer> colors = config.getBorderColors();
        for (int i = 0; i < Math.min(colors.size(), borderColorButtons.size() / 2); i++) {
            Button button = borderColorButtons.get(i * 2); // 每个颜色有2个按钮（颜色+删除）
            int previewX = centerX - BUTTON_WIDTH / 2 - COLOR_PREVIEW_SIZE - 5;
            int previewY = button.getY();
            renderColorPreview(graphics, previewX, previewY, colors.get(i));
        }
    }
    
    private void renderColorPreview(GuiGraphics graphics, int x, int y, int color) {
        // 绘制棋盘格背景
        for (int dy = 0; dy < COLOR_PREVIEW_SIZE; dy += 4) {
            for (int dx = 0; dx < COLOR_PREVIEW_SIZE; dx += 4) {
                boolean isLight = ((dx / 4) + (dy / 4)) % 2 == 0;
                int bgColor = isLight ? 0xFFCCCCCC : 0xFF999999;
                graphics.fill(x + dx, y + dy, 
                             x + Math.min(dx + 4, COLOR_PREVIEW_SIZE), 
                             y + Math.min(dy + 4, COLOR_PREVIEW_SIZE), 
                             bgColor);
            }
        }
        
        // 绘制颜色
        graphics.fill(x, y, x + COLOR_PREVIEW_SIZE, y + COLOR_PREVIEW_SIZE, color);
        
        // 绘制边框
        graphics.renderOutline(x, y, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, 0xFF888888);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
