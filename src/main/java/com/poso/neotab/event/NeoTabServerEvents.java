package com.poso.neotab.event;

import com.poso.neotab.NeoTab;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NeoTab 服务端事件处理。
 * 
 * <p>处理服务端相关的游戏事件，包括：</p>
 * <ul>
 *     <li>服务端 Tick 事件 - 定时刷新 TAB 内容</li>
 *     <li>玩家加入/离开事件 - 管理玩家配置和显示</li>
 *     <li>服务端启动/停止事件 - 管理服务生命周期</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {
    
    private NeoTabServerEvents() {
    }
    
    /**
     * 服务端 Tick 事件处理。
     * 
     * <p>从 NeoForge 1.21.1 移植适配：</p>
     * <ul>
     *     <li>ServerTickEvent.Post → TickEvent.ServerTickEvent</li>
     *     <li>添加 phase 检查确保只在 END 阶段执行</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 只在 tick 结束阶段处理，避免重复执行
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        MinecraftServer server = event.getServer();
        if (server != null) {
            try {
                NeoTab.service().onServerTick(server);
            } catch (Exception e) {
                NeoTab.LOGGER.error("Error during server tick processing: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 玩家加入服务器事件处理。
     * 
     * <p>玩家加入时需要：</p>
     * <ol>
     *     <li>加载该玩家的个人配置</li>
     *     <li>同步有效配置和策略给客户端</li>
     *     <li>刷新所有玩家的 TAB 显示</li>
     * </ol>
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                NeoTab.service().onPlayerJoined(player);
                NeoTab.LOGGER.debug("Player {} joined, TAB updated", player.getName().getString());
            } catch (Exception e) {
                NeoTab.LOGGER.error("Error handling player join for {}: {}", 
                    player.getName().getString(), e.getMessage());
            }
        }
    }
    
    /**
     * 玩家离开服务器事件处理。
     * 
     * <p>玩家离开时需要：</p>
     * <ol>
     *     <li>清理该玩家的配置缓存</li>
     *     <li>清理称号缓存避免内存泄漏</li>
     *     <li>刷新剩余玩家的 TAB 显示</li>
     * </ol>
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                NeoTab.service().onPlayerLeft(player);
                NeoTab.LOGGER.debug("Player {} left, TAB updated", player.getName().getString());
            } catch (Exception e) {
                NeoTab.LOGGER.error("Error handling player leave for {}: {}", 
                    player.getName().getString(), e.getMessage());
            }
        }
    }
}