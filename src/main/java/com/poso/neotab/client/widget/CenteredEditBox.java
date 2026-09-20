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
            int len = displayText.length();
            int cursor = Math.min(this.getCursorPosition(), len);

            // 选区定位：getHighlighted() 只给出选中文本，选择方向决定它在光标左侧还是右侧
            String selected = this.getHighlighted();
            int selStart = cursor;
            int selEnd = cursor;
            if (!selected.isEmpty()) {
                int hl = selected.length();
                if (cursor >= hl && displayText.startsWith(selected, cursor - hl)) {
                    selStart = cursor - hl;
                    selEnd = cursor;
                } else if (displayText.startsWith(selected, cursor)) {
                    selStart = cursor;
                    selEnd = cursor + hl;
                }
            }

            // 水平滚动：聚焦时回溯起点，保证光标（或选区末端）落在可视宽度内
            int anchor = this.isFocused() ? Math.min(Math.max(cursor, selEnd), len) : 0;
            int startIdx = anchor;
            int accW = 0;
            int scrollBudget = Math.max(4, textMaxWidth - 2);
            while (startIdx > 0) {
                int charW = this.fontRenderer.width(String.valueOf(displayText.charAt(startIdx - 1)));
                if (accW + charW > scrollBudget) break;
                accW += charW;
                startIdx--;
            }

            // 可视片段：从 startIdx 起、按像素宽裁剪，防止溢出边框/压住清除按钮
            String view = this.fontRenderer.plainSubstrByWidth(displayText.substring(startIdx), textMaxWidth);
            int viewEnd = startIdx + view.length();

            // 先整体绘制，再叠加选区高亮（白字蓝底）
            guiGraphics.drawString(this.fontRenderer, view, textX, textY, 0xFF000000, false);
            if (this.isFocused()) {
                int a = Math.min(Math.max(selStart - startIdx, 0), view.length());
                int b = Math.min(Math.max(selEnd - startIdx, 0), view.length());
                if (b > a) {
                    String selView = view.substring(a, b);
                    int selX = textX + this.fontRenderer.width(view.substring(0, a));
                    guiGraphics.fill(selX, textY - 1, selX + this.fontRenderer.width(selView), textY + 9, 0xFF3399FF);
                    guiGraphics.drawString(this.fontRenderer, selView, selX, textY, 0xFFFFFFFF, false);
                }

                // 光标：按可视片段换算 X，保证不出界
                if ((System.currentTimeMillis() / 500) % 2 == 0 && cursor <= viewEnd) {
                    int c = Math.max(cursor - startIdx, 0);
                    int cursorX = textX + this.fontRenderer.width(view.substring(0, Math.min(c, view.length())));
                    guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, 0xFF000000);
                }
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