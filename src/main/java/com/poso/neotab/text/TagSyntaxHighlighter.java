package com.poso.neotab.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 输入框标签语法高亮分析器。
 *
 * <p>扫描富文本字符串，为每个字符分配一个显示颜色：</p>
 * <ul>
 *   <li>标签文本（如 {@code <color #FF0000>} 和对应的 {@code </color>}）
 *       使用同一种高亮颜色，不同嵌套层级的标签对使用不同颜色</li>
 *   <li>普通文本使用默认颜色</li>
 * </ul>
 *
 * <p>颜色分配规则：按标签对的"出现顺序"循环取色，同一对开关标签共享同一颜色。
 * 嵌套标签各自独立分配颜色，不受外层影响。</p>
 */
public final class TagSyntaxHighlighter {

    /** 普通文本颜色（深灰，与 MultiLineEditBoxMixin.TEXT_COLOR 一致） */
    public static final int COLOR_PLAIN_TEXT = 0xFF2C2C2C;

    /**
     * 标签高亮颜色调色板。
     * 选取了一组柔和、易于区分的颜色，与米白色背景搭配良好。
     */
    private static final int[] TAG_COLORS = {
        0xFF2E86C1,  // 蓝色
        0xFF27AE60,  // 绿色
        0xFFD35400,  // 橙色
        0xFF8E44AD,  // 紫色
        0xFFC0392B,  // 红色
        0xFF16A085,  // 青绿色
        0xFF2C3E50,  // 深蓝灰
        0xFFB7950B,  // 金黄色
    };

    private TagSyntaxHighlighter() {}

    /**
     * 分析文本，返回每个字符对应的显示颜色数组。
     *
     * <p>返回数组长度与输入字符串长度相同，每个元素是对应字符的 ARGB 颜色值。</p>
     *
     * @param text 原始富文本字符串
     * @return 每个字符的颜色数组
     */
    public static int[] analyze(String text) {
        if (text == null || text.isEmpty()) {
            return new int[0];
        }

        int len = text.length();
        int[] colors = new int[len];

        // 第一遍：找出所有有效标签的位置和配对关系
        List<TagSpan> spans = buildTagSpans(text);

        // 第二遍：为每个字符分配颜色
        // 先把所有字符设为普通文本颜色
        for (int i = 0; i < len; i++) {
            colors[i] = COLOR_PLAIN_TEXT;
        }

        // 再把标签范围内的字符设为对应的高亮颜色
        for (TagSpan span : spans) {
            int color = TAG_COLORS[span.colorIndex % TAG_COLORS.length];
            for (int i = span.start; i < span.end; i++) {
                colors[i] = color;
            }
        }

        return colors;
    }

    /**
     * 扫描文本，构建所有有效标签的位置和颜色索引列表。
     *
     * <p>算法：</p>
     * <ol>
     *   <li>顺序扫描，遇到开标签时压栈，记录其位置和分配的颜色索引</li>
     *   <li>遇到闭标签时，从栈中找到匹配的开标签，将两者都记录为同一颜色</li>
     *   <li>颜色索引按"开标签出现顺序"递增分配，与嵌套深度无关</li>
     * </ol>
     */
    private static List<TagSpan> buildTagSpans(String text) {
        List<TagSpan> result = new ArrayList<>();
        // 栈：存放尚未匹配到闭标签的开标签信息
        Deque<PendingOpenTag> stack = new ArrayDeque<>();
        int colorCounter = 0;

        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) != '<') {
                i++;
                continue;
            }

            int tagEnd = text.indexOf('>', i);
            if (tagEnd < 0) {
                break;
            }

            String rawTag = text.substring(i, tagEnd + 1);
            String content = rawTag.substring(1, rawTag.length() - 1).trim();

            if (content.isEmpty()) {
                i++;
                continue;
            }

            if (isResetTag(content)) {
                // <reset> 标签：单独着色，清空栈
                result.add(new TagSpan(i, tagEnd + 1, colorCounter++));
                stack.clear();
                i = tagEnd + 1;
                continue;
            }

            if (content.charAt(0) == '/') {
                // 闭标签
                String tagName = normalizeTagName(content.substring(1));
                if (isSupportedTagName(tagName)) {
                    // 从栈中找到最近的同名开标签
                    PendingOpenTag matched = findAndRemove(stack, tagName);
                    if (matched != null) {
                        // 开标签和闭标签共享同一颜色索引
                        result.add(new TagSpan(matched.start, matched.end, matched.colorIndex));
                        result.add(new TagSpan(i, tagEnd + 1, matched.colorIndex));
                    }
                    // 没有匹配的开标签：不着色（当作普通文本）
                }
                i = tagEnd + 1;
                continue;
            }

            // 开标签
            String tagName = extractTagName(content);
            if (isSupportedTagName(tagName)) {
                int assignedColor = colorCounter++;
                stack.push(new PendingOpenTag(tagName, i, tagEnd + 1, assignedColor));
            }
            i = tagEnd + 1;
        }

        // 栈中剩余的未匹配开标签：单独着色（没有对应的闭标签）
        for (PendingOpenTag pending : stack) {
            result.add(new TagSpan(pending.start, pending.end, pending.colorIndex));
        }

        return result;
    }

    private static String extractTagName(String content) {
        // content 是 "<" 和 ">" 之间的内容，如 "color #FF0000" 或 "bold"
        int spaceIdx = content.indexOf(' ');
        int equalIdx = content.indexOf('=');
        int splitIdx = -1;
        if (spaceIdx >= 0 && equalIdx >= 0) splitIdx = Math.min(spaceIdx, equalIdx);
        else if (spaceIdx >= 0) splitIdx = spaceIdx;
        else if (equalIdx >= 0) splitIdx = equalIdx;

        String name = splitIdx >= 0 ? content.substring(0, splitIdx) : content;
        return normalizeTagName(name);
    }

    private static String normalizeTagName(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean isResetTag(String content) {
        return "reset".equalsIgnoreCase(content.trim());
    }

    private static boolean isSupportedTagName(String tagName) {
        return switch (tagName) {
            case "color", "gradient", "bold", "italic",
                 "underlined", "strikethrough", "obfuscated" -> true;
            default -> false;
        };
    }

    /**
     * 从栈中找到最近的同名开标签并移除，返回该标签；未找到返回 null。
     * 注意：只移除找到的那一个，不影响其他标签。
     */
    private static PendingOpenTag findAndRemove(Deque<PendingOpenTag> stack, String tagName) {
        // 用临时列表保存弹出的元素，找到后再压回去
        List<PendingOpenTag> temp = new ArrayList<>();
        PendingOpenTag found = null;

        while (!stack.isEmpty()) {
            PendingOpenTag top = stack.pop();
            if (top.name.equals(tagName)) {
                found = top;
                break;
            }
            temp.add(top);
        }

        // 把弹出的其他元素压回去（保持原顺序）
        for (int i = temp.size() - 1; i >= 0; i--) {
            stack.push(temp.get(i));
        }

        return found;
    }

    /** 标签文本的位置和颜色索引 */
    private record TagSpan(int start, int end, int colorIndex) {}

    /** 等待匹配闭标签的开标签信息 */
    private record PendingOpenTag(String name, int start, int end, int colorIndex) {}
}