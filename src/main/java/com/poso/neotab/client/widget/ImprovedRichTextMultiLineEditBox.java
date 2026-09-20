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

    /** 原始字符硬上限的放宽系数：格式代码（§x）2 个原始字符不占可见位，留出余量。 */
    private static final int RAW_CHAR_MULTIPLIER = 4;
    private static final int RAW_CHAR_MINIMUM = 256;

    private int maxVisibleLength = 64;

    public ImprovedRichTextMultiLineEditBox(Font font, int x, int y, int width, int height,
                                            Component placeholder, Component message) {
        super(font, x, y, width, height, placeholder, message);
    }

    // ── 长度限制 ──────────────────────────────────────────────────────────────

    public void setMaxVisibleLength(int maxVisibleLength) {
        this.maxVisibleLength = maxVisibleLength;
        // 保留一个宽松的原始字符硬上限，防止粘贴一次性灌入超长文本；
        // 精确的可见长度限制由输入后校验（enforceVisibleLimit）完成
        super.setCharacterLimit(Math.max(RAW_CHAR_MINIMUM, maxVisibleLength * RAW_CHAR_MULTIPLIER));
    }

    @Override
    public void setCharacterLimit(int characterLimit) {
        setMaxVisibleLength(characterLimit);
    }

    public void setAutoResize(boolean autoResize) {}

    public void setMaxHeight(int maxHeight) {}

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        if (handled) {
            enforceVisibleLimit();
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled) {
            enforceVisibleLimit();
        }
        return handled;
    }

    /**
     * 后校验截断：打字、回车换行、Ctrl+V 粘贴都会改变 value，统一在改动后按可见长度截断。
     * 相比"输入前用 currentValue+ch 估算"，替换选区时净长度变短不会被误拦。
     */
    private void enforceVisibleLimit() {
        String current = getValue();
        if (RichTextEngine.visibleLength(current) > maxVisibleLength) {
            setValue(current); // setValue 覆写内部已做 trimToVisibleLength
        }
    }

    @Override
    public void setValue(String text) {
        String trimmedText = RichTextEngine.trimToVisibleLength(text, maxVisibleLength, false);
        super.setValue(trimmedText);
    }
}
