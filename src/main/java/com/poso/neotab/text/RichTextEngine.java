package com.poso.neotab.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * NeoTab 自定义富文本解析器。
 *
 * <p>这一层的目标不是替代 Minecraft 原版 {@link Component}，
 * 而是给管理界面提供一个更适合手输的轻量语法，再把结果转换成原版文本组件。</p>
 *
 * <p>当前已支持的标签：</p>
 * <ul>
 *     <li>{@code <color #RRGGBB>} / {@code </color>}</li>
 *     <li>{@code <color #AARRGGBB>} / {@code </color>}</li>
 *     <li>{@code <gradient #RRGGBB,#RRGGBB>} / {@code </gradient>} - 静态渐变颜色，支持多个颜色值</li>
 *     <li>{@code <bold>} / {@code </bold>}</li>
 *     <li>{@code <italic>} / {@code </italic>}</li>
 *     <li>{@code <underlined>} / {@code </underlined>}</li>
 *     <li>{@code <strikethrough>} / {@code </strikethrough>}</li>
 *     <li>{@code <obfuscated>} / {@code </obfuscated>}</li>
 *     <li>{@code <reset>}</li>
 * </ul>
 *
 * <p>说明：8 位颜色会按 ARGB 读取。NeoTab 会保留 alpha 语法，
 * 但原版文本组件渲染层对透明度支持有限，因此当前最稳定的仍然是 RGB 颜色。</p>
 * 
 * <p>渐变标签示例：</p>
 * <ul>
 *     <li>{@code <gradient #FF0000,#0000FF>渐变文字</gradient>} - 红色到蓝色的静态渐变</li>
 *     <li>{@code <gradient #FF0000,#00FF00,#0000FF>彩虹文字</gradient>} - 红绿蓝三色静态渐变</li>
 * </ul>
 */
public final class RichTextEngine {
    /**
     * 富文本解析结果缓存。
     * 
     * <p>性能优化：缓存已解析的富文本，避免重复解析相同的模板。
     * 使用 ConcurrentHashMap 保证线程安全，因为可能在多个线程中调用。</p>
     * 
     * <p>缓存键格式：rawText + "|" + singleLine</p>
     */
    private static final Map<String, Component> PARSE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 缓存大小限制，防止内存泄漏。
     * 
     * <p>当缓存超过此大小时，清空缓存。这是一个简单的策略，
     * 适用于配置变更不频繁的场景。</p>
     */
    private static final int MAX_CACHE_SIZE = 100;
    
    private RichTextEngine() {
    }
    
    /**
     * 清空解析缓存。
     * 
     * <p>在配置变更时应该调用此方法，确保使用最新的模板。</p>
     */
    public static void clearCache() {
        PARSE_CACHE.clear();
    }

    /**
     * 解析多行富文本。
     * 
     * <p>性能优化：使用缓存避免重复解析相同的文本。</p>
     */
    public static Component parseMultiline(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return Component.empty();
        }
        
        // 生成缓存键
        String cacheKey = rawText + "|false";
        
        // 尝试从缓存获取
        Component cached = PARSE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，执行解析
        Component result = parseInternal(rawText, false);
        
        // 存入缓存（检查缓存大小）
        if (PARSE_CACHE.size() >= MAX_CACHE_SIZE) {
            PARSE_CACHE.clear(); // 简单策略：超过限制就清空
        }
        PARSE_CACHE.put(cacheKey, result);
        
        return result;
    }

    /**
     * 解析单行富文本，同时把换行压平成空格。
     * 
     * <p>性能优化：使用缓存避免重复解析相同的文本。</p>
     */
    public static Component parseSingleLine(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return Component.empty();
        }
        
        // 预处理：压平换行
        String normalized = rawText.replace('\n', ' ').replace('\r', ' ');
        
        // 生成缓存键
        String cacheKey = normalized + "|true";
        
        // 尝试从缓存获取
        Component cached = PARSE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，执行解析
        Component result = parseInternal(normalized, true);
        
        // 存入缓存（检查缓存大小）
        if (PARSE_CACHE.size() >= MAX_CACHE_SIZE) {
            PARSE_CACHE.clear(); // 简单策略：超过限制就清空
        }
        PARSE_CACHE.put(cacheKey, result);
        
        return result;
    }

    /**
     * 计算“真实可见文本”的长度，标签本身不计入。
     */
    public static int visibleLength(String rawText) {
        String text = rawText == null ? "" : rawText;
        int length = 0;

        for (int i = 0; i < text.length(); ) {
            ParsedTag tag = tryParseTag(text, i);
            if (tag != null) {
                i = tag.endIndex();
                continue;
            }

            length++;
            i++;
        }

        return length;
    }

    /**
     * 按“可见文本长度”截断字符串，同时尽量保留标签结构。
     *
     * <p>这样输入里的标签不会占用字符额度。</p>
     */
    public static String trimToVisibleLength(String rawText, int maxVisibleLength, boolean singleLine) {
        String text = rawText == null ? "" : rawText;
        StringBuilder builder = new StringBuilder();
        Deque<String> openedTags = new ArrayDeque<>();
        int visibleLength = 0;

        for (int i = 0; i < text.length(); ) {
            ParsedTag tag = tryParseTag(text, i);
            if (tag != null) {
                builder.append(tag.rawText());
                trackTag(openedTags, tag);
                i = tag.endIndex();
                continue;
            }

            if (visibleLength >= maxVisibleLength) {
                break;
            }

            char current = text.charAt(i);
            builder.append(singleLine && (current == '\n' || current == '\r') ? ' ' : current);
            visibleLength++;
            i++;
        }

        while (!openedTags.isEmpty()) {
            builder.append("</").append(openedTags.pop()).append('>');
        }

        return builder.toString();
    }

    private static Component parseInternal(String rawText, boolean singleLine) {
        String text = rawText == null ? "" : rawText;
        MutableComponent root = Component.empty();
        StringBuilder plainText = new StringBuilder();
        StyleState currentState = StyleState.EMPTY;
        Deque<OpenTag> tagStack = new ArrayDeque<>();

        for (int i = 0; i < text.length(); ) {
            ParsedTag tag = tryParseTag(text, i);
            if (tag == null) {
                char current = text.charAt(i);
                plainText.append(singleLine && (current == '\n' || current == '\r') ? ' ' : current);
                i++;
                continue;
            }

            flushPlainText(root, plainText, currentState);

            if (tag.kind() == TagKind.RESET) {
                currentState = StyleState.EMPTY;
                tagStack.clear();
            } else if (tag.kind() == TagKind.OPEN) {
                StyleState nextState = applyOpenTag(currentState, tag);
                if (nextState == null) {
                    plainText.append(tag.rawText());
                } else {
                    tagStack.push(new OpenTag(tag.name(), currentState));
                    currentState = nextState;
                }
            } else if (tag.kind() == TagKind.CLOSE) {
                if (!tagStack.isEmpty() && tagStack.peek().name().equals(tag.name())) {
                    currentState = tagStack.pop().previousState();
                } else {
                    plainText.append(tag.rawText());
                }
            }

            i = tag.endIndex();
        }

        flushPlainText(root, plainText, currentState);
        return root;
    }

    /**
     * 将累积的纯文本刷新到组件树中。
     * 
     * <p>性能优化：对于渐变文本，预先计算所有字符的样式，减少重复计算。</p>
     */
    private static void flushPlainText(MutableComponent root, StringBuilder plainText, StyleState styleState) {
        if (plainText.isEmpty()) {
            return;
        }

        String text = plainText.toString();
        
        // 如果当前状态包含静态渐变
        if (styleState.hasGradient()) {
            int textLength = text.length();
            
            // 性能优化：预先计算所有字符的颜色，避免在循环中重复计算
            int[] colors = new int[textLength];
            for (int i = 0; i < textLength; i++) {
                colors[i] = interpolateGradientColor(i, textLength, styleState.gradientColors());
            }
            
            // 逐字符添加到组件树
            for (int i = 0; i < textLength; i++) {
                char c = text.charAt(i);
                Style charStyle = styleState.toStyleForGradientCharWithColor(colors[i]);
                root.append(Component.literal(String.valueOf(c)).setStyle(charStyle));
            }
        } else {
            // 普通情况，整个文本使用相同样式
            root.append(Component.literal(text).withStyle(styleState.toStyle()));
        }
        
        plainText.setLength(0);
    }

    private static StyleState applyOpenTag(StyleState currentState, ParsedTag tag) {
        return switch (tag.name()) {
            case "color" -> {
                Integer parsedColor = parseColor(tag.argument());
                yield parsedColor == null ? null : currentState.withColor(parsedColor);
            }
            case "gradient" -> {
                int[] gradientColors = parseGradientColors(tag.argument());
                yield gradientColors == null ? null : currentState.withGradient(gradientColors);
            }
            case "bold" -> currentState.withBold(true);
            case "italic" -> currentState.withItalic(true);
            case "underlined" -> currentState.withUnderlined(true);
            case "strikethrough" -> currentState.withStrikethrough(true);
            case "obfuscated" -> currentState.withObfuscated(true);
            default -> null;
        };
    }

    private static Integer parseColor(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }

        String normalized = argument.trim();
        if (!normalized.startsWith("#")) {
            return null;
        }

        String hex = normalized.substring(1);
        try {
            if (hex.length() == 6) {
                return Integer.parseUnsignedInt(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    /**
     * 解析渐变颜色参数。
     * 
     * <p>支持格式：#RRGGBB,#RRGGBB 或 #RRGGBB,#RRGGBB,#RRGGBB 等多个颜色值</p>
     * 
     * @param argument 渐变颜色参数字符串
     * @return 颜色数组，如果解析失败返回null
     */
    private static int[] parseGradientColors(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }

        String[] colorStrings = argument.split(",");
        if (colorStrings.length < 2) {
            return null;
        }

        int[] colors = new int[colorStrings.length];
        for (int i = 0; i < colorStrings.length; i++) {
            Integer color = parseColor(colorStrings[i].trim());
            if (color == null) {
                return null;
            }
            colors[i] = color;
        }

        return colors;
    }

    /**
     * 计算渐变颜色插值。
     * 
     * <p>根据字符在文本中的位置，在多个颜色之间进行线性插值。</p>
     * 
     * @param charIndex 当前字符的索引
     * @param totalChars 总字符数
     * @param colors 渐变颜色数组
     * @return 插值后的颜色值
     */
    private static int interpolateGradientColor(int charIndex, int totalChars, int[] colors) {
        if (colors == null || colors.length < 2 || totalChars <= 0) {
            return colors != null && colors.length > 0 ? colors[0] : 0xFFFFFF;
        }

        // 单个字符的情况，使用第一个颜色
        if (totalChars == 1) {
            return colors[0];
        }

        // 计算当前字符在整个渐变中的位置（0.0 到 1.0）
        float position = (float) charIndex / (totalChars - 1);

        // 计算在哪两个颜色之间插值
        float segmentSize = 1.0f / (colors.length - 1);
        int segmentIndex = Math.min((int) (position / segmentSize), colors.length - 2);
        float segmentPosition = (position - segmentIndex * segmentSize) / segmentSize;

        // 获取两个相邻的颜色
        int color1 = colors[segmentIndex];
        int color2 = colors[segmentIndex + 1];

        // 分离RGB分量
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        // 线性插值
        int r = (int) (r1 + (r2 - r1) * segmentPosition);
        int g = (int) (g1 + (g2 - g1) * segmentPosition);
        int b = (int) (b1 + (b2 - b1) * segmentPosition);

        // 组合成最终颜色
        return (r << 16) | (g << 8) | b;
    }

    private static ParsedTag tryParseTag(String text, int startIndex) {
        if (startIndex >= text.length() || text.charAt(startIndex) != '<') {
            return null;
        }

        int endIndex = text.indexOf('>', startIndex);
        if (endIndex < 0) {
            return null;
        }

        String rawText = text.substring(startIndex, endIndex + 1);
        String content = rawText.substring(1, rawText.length() - 1).trim();
        if (content.isEmpty()) {
            return null;
        }

        if ("reset".equalsIgnoreCase(content)) {
            return new ParsedTag(TagKind.RESET, "reset", null, rawText, endIndex + 1);
        }

        if (content.charAt(0) == '/') {
            String tagName = normalizeTagName(content.substring(1));
            return isSupportedTagName(tagName) ? new ParsedTag(TagKind.CLOSE, tagName, null, rawText, endIndex + 1) : null;
        }

        String tagName;
        String argument = null;
        int splitIndex = firstSeparator(content);
        if (splitIndex >= 0) {
            tagName = normalizeTagName(content.substring(0, splitIndex));
            argument = content.substring(splitIndex + 1).trim();
            if (argument.startsWith("=")) {
                argument = argument.substring(1).trim();
            }
        } else {
            tagName = normalizeTagName(content);
        }

        return isSupportedTagName(tagName) ? new ParsedTag(TagKind.OPEN, tagName, argument, rawText, endIndex + 1) : null;
    }

    private static int firstSeparator(String content) {
        int spaceIndex = content.indexOf(' ');
        int equalIndex = content.indexOf('=');
        if (spaceIndex < 0) {
            return equalIndex;
        }
        if (equalIndex < 0) {
            return spaceIndex;
        }
        return Math.min(spaceIndex, equalIndex);
    }

    private static String normalizeTagName(String rawTagName) {
        return rawTagName == null ? "" : rawTagName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isSupportedTagName(String tagName) {
        return switch (tagName) {
            case "color", "gradient", "bold", "italic", "underlined", "strikethrough", "obfuscated" -> true;
            default -> false;
        };
    }

    private static void trackTag(Deque<String> openedTags, ParsedTag tag) {
        if (tag.kind() == TagKind.OPEN && !"reset".equals(tag.name())) {
            openedTags.push(tag.name());
        } else if (tag.kind() == TagKind.CLOSE && !openedTags.isEmpty() && openedTags.peek().equals(tag.name())) {
            openedTags.pop();
        } else if (tag.kind() == TagKind.RESET) {
            openedTags.clear();
        }
    }

    private enum TagKind {
        OPEN,
        CLOSE,
        RESET
    }

    private record ParsedTag(TagKind kind, String name, String argument, String rawText, int endIndex) {
    }

    private record OpenTag(String name, StyleState previousState) {
    }

    /**
     * 当前生效的富文本样式快照。
     *
     * <p>后续你要扩展更多标签时，只需要继续给这个状态加字段，
     * 然后在 {@link #applyOpenTag(StyleState, ParsedTag)} 里接进去即可。</p>
     */
    private record StyleState(
        Integer color,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated,
        int[] gradientColors  // 静态渐变颜色数组
    ) {
        private static final StyleState EMPTY = new StyleState(null, false, false, false, false, false, null);

        private StyleState withColor(Integer nextColor) {
            return new StyleState(nextColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, null);
        }

        private StyleState withGradient(int[] gradientColors) {
            return new StyleState(null, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, gradientColors);
        }

        private StyleState withBold(boolean nextBold) {
            return new StyleState(this.color, nextBold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.gradientColors);
        }

        private StyleState withItalic(boolean nextItalic) {
            return new StyleState(this.color, this.bold, nextItalic, this.underlined, this.strikethrough, this.obfuscated, this.gradientColors);
        }

        private StyleState withUnderlined(boolean nextUnderlined) {
            return new StyleState(this.color, this.bold, this.italic, nextUnderlined, this.strikethrough, this.obfuscated, this.gradientColors);
        }

        private StyleState withStrikethrough(boolean nextStrikethrough) {
            return new StyleState(this.color, this.bold, this.italic, this.underlined, nextStrikethrough, this.obfuscated, this.gradientColors);
        }

        private StyleState withObfuscated(boolean nextObfuscated) {
            return new StyleState(this.color, this.bold, this.italic, this.underlined, this.strikethrough, nextObfuscated, this.gradientColors);
        }

        private Style toStyle() {
            Style style = Style.EMPTY;
            if (this.color != null) {
                style = style.withColor(TextColor.fromRgb(this.color));
            }
            if (this.bold) {
                style = style.withBold(true);
            }
            if (this.italic) {
                style = style.withItalic(true);
            }
            if (this.underlined) {
                style = style.withUnderlined(true);
            }
            if (this.strikethrough) {
                style = style.withStrikethrough(true);
            }
            if (this.obfuscated) {
                style = style.withObfuscated(true);
            }
            return style;
        }

        /**
         * 为渐变文字中的特定字符创建样式（使用预计算的颜色）。
         * 
         * <p>性能优化：接收预计算的颜色值，避免重复计算插值。</p>
         */
        private Style toStyleForGradientCharWithColor(int interpolatedColor) {
            Style style = Style.EMPTY.withColor(TextColor.fromRgb(interpolatedColor));
            
            if (this.bold) {
                style = style.withBold(true);
            }
            if (this.italic) {
                style = style.withItalic(true);
            }
            if (this.underlined) {
                style = style.withUnderlined(true);
            }
            if (this.strikethrough) {
                style = style.withStrikethrough(true);
            }
            if (this.obfuscated) {
                style = style.withObfuscated(true);
            }
            return style;
        }

        /**
         * 检查当前状态是否包含静态渐变。
         */
        private boolean hasGradient() {
            return this.gradientColors != null && this.gradientColors.length >= 2;
        }
    }
}
