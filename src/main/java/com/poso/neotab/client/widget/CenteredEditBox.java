package com.poso.neotab.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 自定义EditBox，文字垂直居中显示，带清除按钮
 */
public class CenteredEditBox extends EditBox {
    
    private final Font fontRenderer;
    private Component hint;
    private static final int CLEAR_BUTTON_WIDTH = 16;  // 清除按钮宽度
    private static final int CLEAR_BUTTON_PADDING = 4;  // 清除按钮右边距
    
    public CenteredEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.fontRenderer = font;
    }
    
    @Override
    public void setHint(Component hint) {
        super.setHint(hint);
        this.hint = hint;
    }
    
    /**
     * 检查鼠标是否在清除按钮上
     */
    public boolean isMouseOverClearButton(double mouseX, double mouseY) {
        if (!this.isVisible() || this.getValue().isEmpty()) {
            return false;
        }
        int clearX = this.getX() + this.width - CLEAR_BUTTON_WIDTH - CLEAR_BUTTON_PADDING;
        int clearY = this.getY() + (this.height - CLEAR_BUTTON_WIDTH) / 2;
        return mouseX >= clearX && mouseX < clearX + CLEAR_BUTTON_WIDTH 
            && mouseY >= clearY && mouseY < clearY + CLEAR_BUTTON_WIDTH;
    }
    
    /**
     * 处理清除按钮点击
     */
    public boolean handleClearButtonClick(double mouseX, double mouseY) {
        if (isMouseOverClearButton(mouseX, mouseY)) {
            this.setValue("");
            return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理Ctrl+A全选
        if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            this.setCursorPosition(this.getValue().length());
            this.setHighlightPos(0);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不渲染默认背景，背景和边框由外部绘制
        if (!this.isVisible()) {
            return;
        }
        
        String displayText = this.getValue();
        
        // 计算文字垂直居中位置
        int textY = this.getY() + (this.height - 8) / 2;  // 8是字体高度
        int textX = this.getX() + 4;  // 左边距4px
        
        // 计算文字可用宽度（如果有清除按钮，需要留出空间）
        int textMaxWidth = this.width - 8;  // 默认左右各4px边距
        if (!displayText.isEmpty()) {
            textMaxWidth -= (CLEAR_BUTTON_WIDTH + CLEAR_BUTTON_PADDING + 4);  // 为清除按钮留出空间
        }
        
        // 绘制文字或提示
        if (!displayText.isEmpty()) {
            // 检查是否有选中的文字
            String highlighted = this.getHighlighted();
            
            if (!highlighted.isEmpty() && this.isFocused()) {
                // 有选中文字时，需要特殊处理
                int cursorPos = this.getCursorPosition();
                int highlightLen = highlighted.length();
                int highlightStart = Math.min(cursorPos, cursorPos - highlightLen);
                int highlightEnd = Math.max(cursorPos, cursorPos - highlightLen);
                
                // 绘制选中前的文字
                if (highlightStart > 0) {
                    String before = displayText.substring(0, highlightStart);
                    guiGraphics.drawString(this.fontRenderer, before, textX, textY, 0xFF000000, false);
                }
                
                // 绘制选中的文字（蓝色背景 + 白色文字）
                String selectedText = displayText.substring(highlightStart, highlightEnd);
                int selectedX = textX + this.fontRenderer.width(displayText.substring(0, highlightStart));
                int selectedWidth = this.fontRenderer.width(selectedText);
                guiGraphics.fill(selectedX, textY - 1, selectedX + selectedWidth, textY + 9, 0xFF3399FF);
                guiGraphics.drawString(this.fontRenderer, selectedText, selectedX, textY, 0xFFFFFFFF, false);
                
                // 绘制选中后的文字
                if (highlightEnd < displayText.length()) {
                    String after = displayText.substring(highlightEnd);
                    int afterX = selectedX + selectedWidth;
                    guiGraphics.drawString(this.fontRenderer, after, afterX, textY, 0xFF000000, false);
                }
            } else {
                // 没有选中文字，正常绘制
                guiGraphics.drawString(this.fontRenderer, displayText, textX, textY, 0xFF000000, false);
            }
            
            // 绘制光标
            if (this.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorPos = Math.min(this.getCursorPosition(), displayText.length());
                int cursorX = textX + this.fontRenderer.width(displayText.substring(0, cursorPos));
                guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, 0xFF000000);
            }
            
            // 绘制清除按钮（"×"符号）
            int clearX = this.getX() + this.width - CLEAR_BUTTON_WIDTH - CLEAR_BUTTON_PADDING;
            int clearY = this.getY() + (this.height - CLEAR_BUTTON_WIDTH) / 2;
            boolean hovered = isMouseOverClearButton(mouseX, mouseY);
            
            // 绘制圆形背景（悬停时显示）
            if (hovered) {
                int centerX = clearX + CLEAR_BUTTON_WIDTH / 2;
                int centerY = clearY + CLEAR_BUTTON_WIDTH / 2;
                int radius = 7;
                guiGraphics.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0xFFFFE0E0);
            }
            
            // 绘制"×"符号（使用字体渲染）
            int xColor = hovered ? 0xFFCC0000 : 0xFFFF6666;  // 悬停时深红色，否则浅红色
            String xSymbol = "×";
            int xWidth = this.fontRenderer.width(xSymbol);
            int xHeight = this.fontRenderer.lineHeight;
            int xX = clearX + (CLEAR_BUTTON_WIDTH - xWidth) / 2;
            int xY = clearY + (CLEAR_BUTTON_WIDTH - xHeight) / 2;
            
            guiGraphics.drawString(this.fontRenderer, xSymbol, xX, xY, xColor, false);
            
        } else if (!this.isFocused() && this.hint != null) {
            // 没有文字且没有焦点时显示提示（灰色）
            guiGraphics.drawString(this.fontRenderer, this.hint, textX, textY, 0xFF808080, false);
        } else if (this.isFocused()) {
            // 有焦点但没有文字时显示光标
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                guiGraphics.fill(textX, textY - 1, textX + 1, textY + 9, 0xFF000000);
            }
        }
    }
}