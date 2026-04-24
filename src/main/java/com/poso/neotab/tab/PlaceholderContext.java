package com.poso.neotab.tab;

import com.poso.neotab.service.TabMetrics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 占位符渲染上下文。
 *
 * <p>之所以同时保留 viewer 和 subject，是为了区分：</p>
 * <ul>
 *     <li>"谁在看这个 TAB"</li>
 *     <li>"TAB 当前这一行代表谁"</li>
 * </ul>
 *
 * <p>目前玩家名模板里两者通常相同，但以后如果你做"查看者相关信息"
 * 或"管理员看到额外数据"的功能，这个结构会非常有用。</p>
 */
public record PlaceholderContext(
    MinecraftServer server,
    TabMetrics metrics,
    ServerPlayer viewer,
    ServerPlayer subject
) {
    /** 生成用于顶部/底部文本的查看者上下文。 */
    public static PlaceholderContext forViewer(MinecraftServer server, ServerPlayer viewer) {
        TabMetrics metrics = TabMetrics.sample(server);
        return new PlaceholderContext(server, metrics, viewer, viewer);
    }
    
    /**
     * 生成用于顶部/底部文本的查看者上下文（使用预先采样的指标）。
     * 
     * <p>性能优化：接收预先采样的指标数据，避免重复采样。</p>
     */
    public static PlaceholderContext forViewerWithMetrics(MinecraftServer server, ServerPlayer viewer, TabMetrics metrics) {
        return new PlaceholderContext(server, metrics, viewer, viewer);
    }

    /** 生成为玩家名模板使用的"目标玩家"上下文。 */
    public static PlaceholderContext forSubject(MinecraftServer server, ServerPlayer subject) {
        return forViewer(server, subject);
    }

    /** 当前行对应玩家的延迟。 */
    public int playerPing() {
        return subject.connection.latency();
    }

    /** 当前查看者自己的延迟。 */
    public int viewerPing() {
        return viewer.connection.latency();
    }
}
