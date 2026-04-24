package com.poso.neotab.event;

import com.poso.neotab.NeoTab;
import com.poso.neotab.command.NeoTabCommands;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.NeoTabPermissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

/**
 * 服务端事件入口。
 *
 * <p>这个类把 NeoForge 事件与 NeoTabService 串起来：</p>
 * <ul>
 *     <li>命令注册</li>
 *     <li>权限节点注册</li>
 *     <li>服务端生命周期</li>
 *     <li>玩家进出事件</li>
 *     <li>TAB 玩家名格式化</li>
 * </ul>
 */
@EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {
    private NeoTabServerEvents() {
    }

    /** 注册 /neotab 命令。 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NeoTabCommands.register(event.getDispatcher());
    }

    /** 向 NeoForge 权限系统注册 NeoTab 的权限节点。 */
    @SubscribeEvent
    public static void onPermissionNodesGathered(PermissionGatherEvent.Nodes event) {
        event.addNodes(NeoTabPermissions.CONFIGURE);
    }

    /** 服务端启动完成后载入配置。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NeoTab.service().onServerStarted(event.getServer());
    }

    /** 服务端关闭时清理状态。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NeoTab.service().onServerStopped();
    }

    /** 驱动服务端定时刷新逻辑。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        NeoTab.service().onServerTick(event.getServer());
    }

    /** 玩家进入后立即应用 NeoTab 内容。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoTab.service().onPlayerJoined(player);
        }
    }

    /** 玩家退出后重新计算在线人数等占位符。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoTab.service().onPlayerLeft(player);
        }
    }

    /**
     * 自定义 TAB 玩家名显示。
     *
     * <p>这里只改“玩家在 TAB 中这一行的显示名”，不会影响聊天栏名称、
     * 头顶名字或记分板名字。</p>
     */
    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TabConfig config = NeoTab.service().getConfig();
        MutableComponent playerName = PlayerTeam.formatNameForTeam(player.getTeam(), Component.literal(player.getGameProfile().getName())).copy();

        // 如果启用了称号功能，在名称前添加称号
        if (config.titleEnabled()) {
            // 获取称号文本（支持富文本标签）
            String titleText = getTitleForPlayer(player);
            if (titleText != null && !titleText.isEmpty()) {
                // 使用RichTextEngine渲染称号文本，支持color和gradient标签
                Component titleComponent = com.poso.neotab.text.RichTextEngine.parseSingleLine(titleText);
                // 称号 + 空格 + 玩家名称，保持玩家名称原有颜色
                playerName = Component.empty().append(titleComponent).append(" ").append(playerName);
            }
        }

        // 服务端不添加延迟和在线时长到displayName
        // 这些信息会在客户端的renderPingIcon位置右对齐显示
        // 这样可以避免重叠问题，并实现真正的右对齐

        // 原版旁观者在 TAB 中使用斜体，这里保持同样的视觉行为。
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            playerName = playerName.copy().withStyle(ChatFormatting.ITALIC);
        }

        event.setDisplayName(playerName);
    }

    /**
     * 获取玩家的称号文本。
     * 
     * <p>此方法通过 NeoTab API 系统获取称号数据。其他模组可以通过以下方式提供称号：</p>
     * <ul>
     *     <li>实现 {@link com.poso.neotab.api.TitleProvider} 接口并注册</li>
     *     <li>监听 {@link com.poso.neotab.api.event.GetPlayerTitleEvent} 事件</li>
     * </ul>
     * 
     * @param player 玩家对象
     * @return 称号文本，支持富文本标签，如果没有称号则返回null
     */
    private static String getTitleForPlayer(ServerPlayer player) {
        // 首先尝试通过API系统获取真实的称号数据
        String title = com.poso.neotab.api.NeoTabAPI.getPlayerTitle(player);
        if (title != null && !title.isEmpty()) {
            return title;
        }
        
        // 如果没有其他模组提供称号，使用模拟数据进行测试
        // 在生产环境中，这部分代码可以移除
        return getSimulatedTitle(player);
    }
    
    /**
     * 生成模拟称号数据用于测试。
     * 
     * <p>此方法仅用于测试目的，在实际部署时可以移除。</p>
     * 
     * @param player 玩家对象
     * @return 模拟的称号文本
     */
    private static String getSimulatedTitle(ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        
        // 根据玩家名称生成不同的测试称号
        return switch (playerName.hashCode() % 6) {
            case 0 -> "<color #FFD700>『黄金骑士』</color>";  // 金色称号
            case 1 -> "<color #FF6B6B>『红莲战士』</color>";  // 红色称号
            case 2 -> "<gradient #00FF00,#0080FF>『翡翠法师』</gradient>";  // 绿蓝渐变称号
            case 3 -> "<color #9370DB>『紫晶学者』</color>";  // 紫色称号
            case 4 -> "<gradient #FF1493,#FFD700>『彩虹英雄』</gradient>";  // 粉金渐变称号
            case 5 -> null;  // 模拟没有称号的玩家
            default -> "<color #87CEEB>『天空守护者』</color>";  // 天蓝色称号
        };
    }

    /**
     * 按延迟区间返回颜色。
     *
     * <p>暂定四档：</p>
     * <ul>
     *     <li>0 - 99ms：绿色</li>
     *     <li>100 - 199ms：黄色</li>
     *     <li>200 - 349ms：橙色（金色）</li>
     *     <li>350ms 及以上：红色</li>
     * </ul>
     */
    private static ChatFormatting colorForPing(int latency) {
        if (latency < 100) {
            return ChatFormatting.GREEN;
        }
        if (latency < 200) {
            return ChatFormatting.YELLOW;
        }
        if (latency < 350) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }
}
