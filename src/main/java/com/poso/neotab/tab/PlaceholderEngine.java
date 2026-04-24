package com.poso.neotab.tab;

import com.poso.neotab.text.RichTextEngine;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private PlaceholderEngine() {
    }

    /** 渲染多行文本，主要用于 TAB 头部/底部。 */
    public static Component renderMultiline(String template, PlaceholderContext context) {
        String resolved = replacePlaceholders(template, context);
        return resolved.isBlank() ? Component.empty() : RichTextEngine.parseMultiline(resolved);
    }

    /** 渲染单行文本，主要用于玩家名，顺便把换行压平。 */
    public static Component renderSingleLine(String template, PlaceholderContext context) {
        String resolved = replacePlaceholders(template, context);
        return resolved.isBlank() ? Component.empty() : RichTextEngine.parseSingleLine(resolved);
    }

    /**
     * 按当前上下文把模板中的占位符替换成真实值。
     * 
     * <p>性能优化：使用 StringBuilder 避免多次字符串拼接产生的临时对象。</p>
     */
    private static String replacePlaceholders(String template, PlaceholderContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        
        // 使用 StringBuilder 进行高效的字符串替换，避免多次创建中间 String 对象
        StringBuilder result = new StringBuilder(template);
        Map<String, String> placeholders = values(context);
        
        // 遍历所有占位符进行替换
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            
            // 查找并替换所有出现的占位符
            int index = 0;
            while ((index = result.indexOf(placeholder, index)) != -1) {
                result.replace(index, index + placeholder.length(), value);
                index += value.length(); // 移动到替换后的位置，避免重复匹配
            }
        }
        
        return result.toString();
    }

    /**
     * 当前支持的占位符集合。
     *
     * <p>这里故意使用 LinkedHashMap，便于保持替换顺序稳定，
     * 后续调试打印时也更容易看清楚。</p>
     * 
     * <p>所有占位符都通过原版高效 API 获取，不会产生额外的性能开销。</p>
     */
    private static Map<String, String> values(PlaceholderContext context) {
        Map<String, String> values = new LinkedHashMap<>();
        
        // 玩家信息
        values.put("%player_name%", context.subject().getGameProfile().getName());
        values.put("%player_ping%", Integer.toString(context.playerPing()));
        values.put("%viewer_name%", context.viewer().getGameProfile().getName());
        values.put("%viewer_ping%", Integer.toString(context.viewerPing()));
        
        // 服务器状态
        values.put("%online%", Integer.toString(context.metrics().onlinePlayers()));
        values.put("%max_players%", Integer.toString(context.metrics().maxPlayers()));
        values.put("%tps%", context.metrics().tpsText());
        values.put("%mspt%", context.metrics().msptText());
        
        // 内存使用
        values.put("%memory_used%", Long.toString(context.metrics().usedMemoryMB()));
        values.put("%memory_max%", Long.toString(context.metrics().maxMemoryMB()));
        values.put("%memory_total%", context.metrics().memoryText());
        values.put("%memory_percent%", context.metrics().memoryPercentText());
        
        // 服务器运行时间
        values.put("%uptime%", context.metrics().uptimeText());
        values.put("%uptime_days%", context.metrics().uptimeDaysText());
        values.put("%uptime_hours%", context.metrics().uptimeHoursText());
        
        // 世界信息
        values.put("%world_time%", context.metrics().worldTimeText());
        values.put("%world_day%", context.metrics().worldDayText());
        values.put("%loaded_chunks%", context.metrics().loadedChunksText());
        
        return values;
    }
}
