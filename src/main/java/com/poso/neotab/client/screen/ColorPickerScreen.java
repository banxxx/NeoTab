package com.poso.neotab.client.screen;

import com.poso.neotab.client.widget.ColorPickerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 颜色选择器对话框。
 */
public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final int initialColor;
    private final Consumer<Integer> onColorSelected;
    private ColorPickerWidget colorPicker;
    private Button confirmButton;
    private Button cancelButton;
    
    public ColorPickerScreen(Screen parent, int initialColor, Consumer<Integer> onColorSelected) {
        super(Component.translatable("screen.neotab.color_picker.title"));
        this.parent = parent;
        this.initialColor = initialColor;
        this.onColorSelected = onColorSelected;
    }
    
    @Override
    protected void init() {
        int pickerX = (this.width - 228) / 2;  // 228 是颜色选择器的总宽度
        int pickerY = (this.height - 150) / 2 - 20;
        
        // 创建颜色选择器
        this.colorPicker = new ColorPickerWidget(pickerX, pickerY, this.font, initialColor, color -> {
            // 颜色变化时的回调（可选）
        });
        addRenderableWidget(this.colorPicker);
        
        // 确认按钮
        int buttonY = pickerY + 160;
        this.confirmButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            if (onColorSelected != null) {
                onColorSelected.accept(colorPicker.getColor());
            }
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 105, buttonY, 100, 20).build();
        addRenderableWidget(this.confirmButton);
        
        // 取消按钮
        this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 + 5, buttonY, 100, 20).build();
        addRenderableWidget(this.cancelButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制半透明背景
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        
        // 绘制所有组件
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
