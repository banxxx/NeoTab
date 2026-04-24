package com.poso.neotab.client.widget;

import com.poso.neotab.text.RichTextEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 改进的富文本单行输入框。
 *
 * <p>改进内容：</p>
 * <ul>
 *     <li>支持富文本长度限制</li>
 *     <li>修复全选后退格键删除问题</li>
 *     <li>更好的用户体验</li>
 * </ul>
 */
public class ImprovedRichTextEditBox extends EditBox {
    private int maxVisibleLength = 32;

    public ImprovedRichTextEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    /**
     * 设置最大可见文本长度（不包括富文本标签）。
     */
    public void setMaxVisibleLength(int maxVisibleLength) {
        this.maxVisibleLength = maxVisibleLength;
        // 清除原版的字符长度限制，我们用自己的逻辑
        super.setMaxLength(Integer.MAX_VALUE);
    }

    @Override
    public void setMaxLength(int maxLength) {
        // 重定向到我们的可见长度设置
        setMaxVisibleLength(maxLength);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 修复全选后退格键删除问题
        if (keyCode == 259 && this.canConsumeInput()) { // 259 是退格键
            String highlighted = this.getHighlighted();
            if (!highlighted.isEmpty()) {
                // 如果有选中文本，直接删除选中的部分
                String currentValue = this.getValue();
                int cursorPos = this.getCursorPosition();
                int selectionStart = cursorPos - highlighted.length();
                
                String newValue = currentValue.substring(0, selectionStart) + 
                                currentValue.substring(cursorPos);
                this.setValue(newValue);
                this.setCursorPosition(selectionStart);
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.canConsumeInput()) {
            return false;
        }

        // 检查输入后是否会超过可见长度限制
        String currentValue = this.getValue();
        String newValue = new StringBuilder(currentValue)
            .insert(this.getCursorPosition(), codePoint)
            .toString();

        if (RichTextEngine.visibleLength(newValue) > maxVisibleLength) {
            return false;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void insertText(String textToInsert) {
        if (!this.canConsumeInput() || textToInsert.isEmpty()) {
            return;
        }

        // 检查插入后是否会超过可见长度限制
        String currentValue = this.getValue();
        String newValue = new StringBuilder(currentValue)
            .insert(this.getCursorPosition(), textToInsert)
            .toString();

        if (RichTextEngine.visibleLength(newValue) > maxVisibleLength) {
            // 尝试截断插入的文本
            String trimmedValue = RichTextEngine.trimToVisibleLength(newValue, maxVisibleLength, true);
            if (!trimmedValue.equals(currentValue)) {
                super.setValue(trimmedValue);
                // 将光标移动到插入位置的末尾
                int insertLength = trimmedValue.length() - currentValue.length();
                super.setCursorPosition(this.getCursorPosition() + insertLength);
            }
            return;
        }

        super.insertText(textToInsert);
    }

    @Override
    public void setValue(String text) {
        // 确保设置的值不超过可见长度限制
        String trimmedText = RichTextEngine.trimToVisibleLength(text, maxVisibleLength, true);
        super.setValue(trimmedText);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        // 移除提示图标，保持简洁的界面
    }
}