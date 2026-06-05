package com.poso.neotab.client.widget;

import com.poso.neotab.text.RichTextEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * 改进的富文本多行输入框。
 *
 * <p>改进内容：</p>
 * <ul>
 *     <li>支持富文本长度限制</li>
 *     <li>修复全选后退格键删除问题</li>
 * </ul>
 */
public class ImprovedRichTextMultiLineEditBox extends NoCountMultiLineEditBox {

    private int maxVisibleLength = 64;

    public ImprovedRichTextMultiLineEditBox(Font font, int x, int y, int width, int height,
                                            Component placeholder, Component message) {
        super(font, x, y, width, height, placeholder, message);
    }

    // ── 长度限制 ──────────────────────────────────────────────────────────────

    public void setMaxVisibleLength(int maxVisibleLength) {
        this.maxVisibleLength = maxVisibleLength;
        super.setCharacterLimit(Integer.MAX_VALUE);
    }

    @Override
    public void setCharacterLimit(int characterLimit) {
        setMaxVisibleLength(characterLimit);
    }

    public void setAutoResize(boolean autoResize) {}

    public void setMaxHeight(int maxHeight) {}

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        String currentValue = this.getValue();
        String newValue = currentValue + codePoint;
        if (RichTextEngine.visibleLength(newValue) > maxVisibleLength) {
            return false;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void setValue(String text) {
        String trimmedText = RichTextEngine.trimToVisibleLength(text, maxVisibleLength, false);
        super.setValue(trimmedText);
    }
}
