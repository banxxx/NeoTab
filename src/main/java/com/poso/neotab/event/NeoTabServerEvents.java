package com.poso.neotab.event;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.TabConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoTab 服务端事件处理。
 */
@Mod.EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {

    private NeoTabServerEvents() {
    }

    /** 注册 /neotab 命令。 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.poso.neotab.command.NeoTabCommands.register(event.getDispatcher());
    }

    /**
     * 注册权限节点。
     * 
     * <p>Forge 1.20.1 使用不同的权限系统，不需要像 NeoForge 那样通过事件注册。
     * 权限节点在 NeoTabPermissions 中定义后即可直接使用。</p>
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Forge 1.20.1 的权限节点在定义时就已经注册到系统中
        // 不需要额外的事件处理
        NeoTab.LOGGER.info("NeoTab permission nodes registered");
    }

    /** 服务端启动完成后载入配置。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NeoTab.service().onServerStarted(event.getServer());
    }

    /** 服务端关闭时清理状态。 */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NeoTab.service().onServerStopped();
    }

    /** 驱动服务端定时刷新逻辑。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server != null) {
            NeoTab.service().onServerTick(server);
        }
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
     * 自定义 TAB 玩家名显示（称号功能）。
     *
     * <p>这里只改"玩家在 TAB 中这一行的显示名"，不会影响聊天栏名称、
     * 头顶名字或记分板名字。</p>
     */
    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TabConfig config = NeoTab.service().getConfig();
        MutableComponent playerName = PlayerTeam.formatNameForTeam(
                player.getTeam(),
                Component.literal(player.getGameProfile().getName())
        ).copy();

        // 如果启用了称号功能，在名称前添加称号
        if (config.titleEnabled()) {
            String titleText = getTitleForPlayer(player);
            if (titleText != null && !titleText.isEmpty()) {
                Component titleComponent = com.poso.neotab.text.RichTextEngine.parseSingleLine(titleText, titleText);
                playerName = Component.empty().append(titleComponent).append(" ").append(playerName);
            }
        }

        // 原版旁观者在 TAB 中使用斜体
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            playerName = playerName.copy().withStyle(ChatFormatting.ITALIC);
        }

        event.setDisplayName(playerName);
    }

    private static String getTitleForPlayer(ServerPlayer player) {
        return com.poso.neotab.api.NeoTabAPI.getPlayerTitle(player);
    }
}
