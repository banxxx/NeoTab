package com.poso.neotab.client.widget;

import com.poso.neotab.text.RichTextEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * 支持富文本长度限制的多行输入框。
 *
 * <p>这个输入框按可见文本长度而不是实际字符长度来限制输入，
 * 这样富文本标签就不会占用用户的输入长度限制。同时去掉了右下角的字数统计。</p>
 */
public class RichTextMultiLineEditBox extends NoCountMultiLineEditBox {
    private int maxVisibleLength = 64;

    public RichTextMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message) {
        super(font, x, y, width, height, placeholder, message);
    }

    /**
     * 设置最大可见文本长度（不包括富文本标签）。
     */
    public void setMaxVisibleLength(int maxVisibleLength) {
        this.maxVisibleLength = maxVisibleLength;
        // 清除原版的字符长度限制，我们用自己的逻辑
        super.setCharacterLimit(Integer.MAX_VALUE);
    }

    @Override
    public void setCharacterLimit(int characterLimit) {
        // 重定向到我们的可见长度设置
        setMaxVisibleLength(characterLimit);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 检查输入后是否会超过可见长度限制
        String currentValue = this.getValue();
        String newValue = currentValue + codePoint;

        if (RichTextEngine.visibleLength(newValue) > maxVisibleLength) {
            return false;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void setValue(String text) {
        // 确保设置的值不超过可见长度限制
        String trimmedText = RichTextEngine.trimToVisibleLength(text, maxVisibleLength, false);
        super.setValue(trimmedText);
    }
}