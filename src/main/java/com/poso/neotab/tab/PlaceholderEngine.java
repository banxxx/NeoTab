package com.poso.neotab.tab;

import com.poso.neotab.text.RichTextEngine;
import net.minecraft.network.chat.Component;

/**
 * 占位符替换引擎。
 *
 * <p>当前实现是最直接的字符串替换，优点是足够简单、容易维护。
 * 当你后续要加入更复杂的模板语法时，可以把这里替换成真正的解析器。</p>
 */
public final class PlaceholderEngine {
    /** 用于 UI 中展示给管理员看的占位符帮助文本。 */
    public static final String HELP_TEXT = """
        玩家信息:
        %player_name%  %player_ping%
        %viewer_name%  %viewer_ping%
        
        服务器状态:
        %online%  %max_players%
        %tps%  %mspt%
        
        内存使用:
        %memory_used%  %memory_max%
        %memory_total%  %memory_percent%
        
        运行时间:
        %uptime%  %uptime_days%  %uptime_hours%
        
        世界信息:
        %world_time%  %world_day%  %loaded_chunks%
        """;

    /**
     * 所有占位符的 key，与 {@link #buildValues(PlaceholderContext)} 返回的 value 数组一一对应。
     *
     * <p>使用静态常量数组替代每次调用都 new 的 LinkedHashMap，
     * 彻底消除 Map 和 Entry 对象的分配。</p>
     */
    private static final String[] KEYS = {
        "%player_name%", "%player_ping%",
        "%viewer_name%", "%viewer_ping%",
        "%online%", "%max_players%",
        "%tps%", "%mspt%",
        "%memory_used%", "%memory_max%", "%memory_total%", "%memory_percent%",
        "%uptime%", "%uptime_days%", "%uptime_hours%",
        "%world_time%", "%world_day%", "%loaded_chunks%"
    };

    private PlaceholderEngine() {
    }

    /** 渲染多行文本，主要用于 TAB 头部/底部。 */
    public static Component renderMultiline(String template, PlaceholderContext context) {
        String resolved = replacePlaceholders(template, context);
        return resolved.isBlank() ? Component.empty() : RichTextEngine.parseMultiline(template, resolved);
    }

    /** 渲染单行文本，主要用于玩家名，顺便把换行压平。 */
    public static Component renderSingleLine(String template, PlaceholderContext context) {
        String resolved = replacePlaceholders(template, context);
        return resolved.isBlank() ? Component.empty() : RichTextEngine.parseSingleLine(template, resolved);
    }

    /**
     * 按当前上下文把模板中的占位符替换成真实值。
     *
     * <p>性能优化：</p>
     * <ul>
     *   <li>使用静态 KEYS 数组 + 按需构建 values 数组，避免每次 new LinkedHashMap</li>
     *   <li>使用 StringBuilder 避免多次字符串拼接产生的临时对象</li>
     *   <li>只在模板中实际出现某个占位符时才计算对应的值（懒求值）</li>
     * </ul>
     */
    private static String replacePlaceholders(String template, PlaceholderContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(template);
        // 懒求值：只在模板中出现对应占位符时才计算值，避免无用的格式化调用
        String[] values = null;

        for (int k = 0; k < KEYS.length; k++) {
            String placeholder = KEYS[k];
            int index = result.indexOf(placeholder);
            if (index == -1) {
                continue; // 模板中没有此占位符，跳过
            }
            // 首次需要值时才构建 values 数组（懒初始化）
            if (values == null) {
                values = buildValues(context);
            }
            String value = values[k];
            // 替换所有出现的该占位符
            do {
                result.replace(index, index + placeholder.length(), value);
                index += value.length();
                index = result.indexOf(placeholder, index);
            } while (index != -1);
        }

        return result.toString();
    }

    /**
     * 构建与 {@link #KEYS} 一一对应的值数组。
     *
     * <p>只在模板中确实存在占位符时才调用，避免无用的格式化开销。</p>
     */
    private static String[] buildValues(PlaceholderContext context) {
        return new String[] {
            // 玩家信息
            context.subject().getGameProfile().getName(),
            Integer.toString(context.playerPing()),
            context.viewer().getGameProfile().getName(),
            Integer.toString(context.viewerPing()),
            // 服务器状态
            Integer.toString(context.metrics().onlinePlayers()),
            Integer.toString(context.metrics().maxPlayers()),
            context.metrics().tpsText(),
            context.metrics().msptText(),
            // 内存使用
            Long.toString(context.metrics().usedMemoryMB()),
            Long.toString(context.metrics().maxMemoryMB()),
            context.metrics().memoryText(),
            context.metrics().memoryPercentText(),
            // 服务器运行时间
            context.metrics().uptimeText(),
            context.metrics().uptimeDaysText(),
            context.metrics().uptimeHoursText(),
            // 世界信息
            context.metrics().worldTimeText(),
            context.metrics().worldDayText(),
            context.metrics().loadedChunksText()
        };
    }
}
