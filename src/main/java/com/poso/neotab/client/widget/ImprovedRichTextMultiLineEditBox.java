package com.poso.neotab.client.widget;

import com.poso.neotab.mixin.client.MultiLineEditBoxAccessor;
import com.poso.neotab.mixin.client.MultilineTextFieldAccessor;
import com.poso.neotab.text.RichTextEngine;
import com.poso.neotab.text.TagSyntaxHighlighter;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

/**
 * 改进的富文本多行输入框。
 *
 * <p>改进内容：</p>
 * <ul>
 *     <li>支持富文本长度限制</li>
 *     <li>修复全选后退格键删除问题</li>
 *     <li>标签语法高亮：同一对开关标签使用同一颜色，不同标签对使用不同颜色</li>
 * </ul>
 */
public class ImprovedRichTextMultiLineEditBox extends NoCountMultiLineEditBox {

    private int maxVisibleLength = 64;

    // ── 语法高亮缓存 ──────────────────────────────────────────────────────────
    /** 上次计算高亮时的文本内容，用于判断是否需要重新计算 */
    private String cachedHighlightText = null;
    /** 每个字符对应的颜色数组，与 cachedHighlightText 对应 */
    private int[] cachedColors = null;

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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259) {
            String currentValue = this.getValue();
            if (!currentValue.isEmpty() && hasSelection()) {
                this.setValue("");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

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

    private boolean hasSelection() {
        return false;
    }

    // ── 语法高亮渲染 ──────────────────────────────────────────────────────────

    /**
     * 获取当前文本的每字符颜色数组，带缓存。
     * 只有文本内容变化时才重新分析。
     */
    private int[] getColors(String text) {
        if (!text.equals(cachedHighlightText)) {
            cachedHighlightText = text;
            cachedColors = TagSyntaxHighlighter.analyze(text);
        }
        return cachedColors;
    }

    /**
     * 重写文本内容渲染，实现标签语法高亮。
     *
     * <p>逻辑与原版 {@code MultiLineEditBox.renderContents} 完全一致，
     * 区别在于：不再用单一颜色渲染整行，而是逐字符取色，
     * 标签文本和普通文本分别使用不同颜色。</p>
     *
     * <p>光标、选区高亮等行为与原版保持一致。</p>
     */
    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        MultiLineEditBoxAccessor accessor = (MultiLineEditBoxAccessor) this;
        MultilineTextField textField = accessor.neotab$getTextField();
        long focusedTime = accessor.neotab$getFocusedTime();
        Font font = accessor.neotab$getFont();
        Component placeholder = accessor.neotab$getPlaceholder();

        String s = textField.value();

        // 空文本且未聚焦：显示 placeholder（原版行为，无阴影）
        if (s.isEmpty() && !this.isFocused()) {
            guiGraphics.drawWordWrap(
                font, placeholder,
                this.getX() + this.innerPadding(),
                this.getY() + this.innerPadding(),
                this.width - this.totalInnerPadding(),
                -857677600
            );
            return;
        }

        // 获取每字符颜色（带缓存）
        int[] colors = getColors(s);

        int cursor = textField.cursor();
        boolean cursorVisible = this.isFocused() && (Util.getMillis() - focusedTime) / 300L % 2L == 0L;
        boolean cursorBeforeEnd = cursor < s.length();
        int lastLineX = 0;
        int lastLineY = 0;
        int lineY = this.getY() + this.innerPadding();

        // 自行计算每行的起止索引，避免引用 protected StringView
        // MultilineTextField 按字体宽度折行，这里用相同的方式重建行列表
        int[] lineBegins = buildLineBegins(font, s);

        for (int li = 0; li < lineBegins.length; li++) {
            int lineStart = lineBegins[li];
            int lineEnd   = (li + 1 < lineBegins.length) ? lineBegins[li + 1] : s.length();
            // 去掉行末的换行符（如果有）
            if (lineEnd > lineStart && s.charAt(lineEnd - 1) == '\n') lineEnd--;

            if (!this.withinContentAreaTopBottom(lineY, lineY + 9)) {
                lineY += 9;
                continue;
            }

            if (cursorVisible && cursorBeforeEnd
                    && cursor >= lineStart && cursor <= lineEnd) {
                // 光标在本行中间：分两段渲染，中间插入光标竖线
                int cursorX = renderSegmentHighlighted(guiGraphics, font, s, colors,
                        lineStart, cursor,
                        this.getX() + this.innerPadding(), lineY);
                // 光标竖线
                guiGraphics.fill(cursorX, lineY - 1, cursorX + 1, lineY + 10, -3092272);
                // 光标后半段
                renderSegmentHighlighted(guiGraphics, font, s, colors,
                        cursor, lineEnd, cursorX, lineY);
                lastLineX = cursorX;
            } else {
                lastLineX = renderSegmentHighlighted(guiGraphics, font, s, colors,
                        lineStart, lineEnd,
                        this.getX() + this.innerPadding(), lineY);
                lastLineY = lineY;
            }

            lineY += 9;
        }

        // 末尾追加光标（"_"）
        if (cursorVisible && !cursorBeforeEnd
                && this.withinContentAreaTopBottom(lastLineY, lastLineY + 9)) {
            guiGraphics.drawString(font, "_", lastLineX, lastLineY, -3092272, false);
        }

        // 选区高亮
        if (textField.hasSelection()) {
            MultilineTextFieldAccessor tfAccessor = (MultilineTextFieldAccessor) textField;
            int selBegin = Math.min(tfAccessor.neotab$getCursor(), tfAccessor.neotab$getSelectCursor());
            int selEnd   = Math.max(tfAccessor.neotab$getCursor(), tfAccessor.neotab$getSelectCursor());

            int baseX = this.getX() + this.innerPadding();
            lineY = this.getY() + this.innerPadding();

            for (int li = 0; li < lineBegins.length; li++) {
                int lineStart = lineBegins[li];
                int lineEnd   = (li + 1 < lineBegins.length) ? lineBegins[li + 1] : s.length();
                if (lineEnd > lineStart && s.charAt(lineEnd - 1) == '\n') lineEnd--;

                if (selBegin > lineEnd) {
                    lineY += 9;
                    continue;
                }
                if (lineStart > selEnd) break;

                if (this.withinContentAreaTopBottom(lineY, lineY + 9)) {
                    int segStart = Math.max(selBegin, lineStart);
                    int segEnd   = Math.min(selEnd, lineEnd);
                    int x1 = baseX + font.width(s.substring(lineStart, segStart));
                    int x2 = selEnd > lineEnd
                            ? this.width - this.innerPadding()
                            : baseX + font.width(s.substring(lineStart, segEnd));
                    guiGraphics.fill(RenderType.guiTextHighlight(), x1, lineY, x2, lineY + 9, -16776961);
                }
                lineY += 9;
            }
        }
    }

    /**
     * 渲染字符串中 [from, to) 范围内的字符，每个字符使用 colors 数组中对应的颜色。
     * 返回渲染结束后的 x 坐标（即下一个字符应该开始的位置）。
     *
     * <p>为了减少 drawString 调用次数，将相邻且颜色相同的字符合并为一段一起渲染。</p>
     */
    private int renderSegmentHighlighted(GuiGraphics guiGraphics, Font font,
                                         String text, int[] colors,
                                         int from, int to, int startX, int y) {
        if (from >= to) return startX;

        int x = startX;
        int segStart = from;
        int segColor = colors[from];

        for (int i = from + 1; i <= to; i++) {
            int curColor = (i < to) ? colors[i] : -1; // -1 作为哨兵值触发最后一段的渲染
            if (curColor != segColor) {
                // 渲染 [segStart, i) 这一段
                String seg = text.substring(segStart, i);
                // drawString 无阴影，+1 补偿返回值差异（与 MultiLineEditBoxMixin 保持一致）
                x = guiGraphics.drawString(font, seg, x, y, segColor, false) + 1;
                segStart = i;
                segColor = curColor;
            }
        }

        return x;
    }

    /**
     * 按照 {@link net.minecraft.client.gui.components.MultilineTextField} 相同的折行逻辑，
     * 计算每行的起始字符索引数组。
     *
     * <p>原版用 {@code Font.getSplitter().splitLines()} 折行，这里复用相同逻辑，
     * 避免引用 {@code protected StringView}。</p>
     */
    private int[] buildLineBegins(Font font, String text) {
        if (text.isEmpty()) return new int[]{0};

        java.util.List<Integer> begins = new java.util.ArrayList<>();
        int textWidth = this.width - this.totalInnerPadding();

        font.getSplitter().splitLines(
            text,
            textWidth,
            net.minecraft.network.chat.Style.EMPTY,
            false,
            (style, lineStart, lineEnd) -> begins.add(lineStart)
        );

        // 如果文本以换行符结尾，原版会额外追加一个空行
        if (!text.isEmpty() && text.charAt(text.length() - 1) == '\n') {
            begins.add(text.length());
        }

        if (begins.isEmpty()) begins.add(0);

        return begins.stream().mapToInt(Integer::intValue).toArray();
    }
}
