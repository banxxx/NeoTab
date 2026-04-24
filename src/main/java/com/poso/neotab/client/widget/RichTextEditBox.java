package com.poso.neotab.client.widget;

import com.poso.neotab.text.RichTextEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 支持富文本长度限制的单行输入框。
 *
 * <p>这个输入框按可见文本长度而不是实际字符长度来限制输入，
 * 这样富文本标签就不会占用用户的输入长度限制。</p>
 */
public class RichTextEditBox extends EditBox {
    private int maxVisibleLength = 32;

    public RichTextEditBox(Font font, int x, int y, int width, int height, Component message) {
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
}