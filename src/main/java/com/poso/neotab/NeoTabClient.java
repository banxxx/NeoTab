package com.poso.neotab;

import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 客户端入口。
 *
 * <p>当前它只保留了一个轻量入口，主要作用是：</p>
 * <ul>
 *     <li>保证客户端侧类加载正常</li>
 *     <li>作为后续扩展客户端功能的挂点</li>
 * </ul>
 *
 * <p>例如未来如果你要做：</p>
 * <ul>
 *     <li>自定义 TAB 渲染</li>
 *     <li>本地缓存 UI 状态</li>
 *     <li>增加 TAB 页面动画或拖拽布局</li>
 * </ul>
 * 都可以继续放在客户端模块里。</p>
 */
@Mod(value = NeoTab.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)
public class NeoTabClient {
    public NeoTabClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 这里只做最小初始化输出，便于确认客户端侧模组已加载。
        NeoTab.LOGGER.info("NeoTab client bootstrap for {}", Minecraft.getInstance().getUser().getName());
    }

    /**
     * 客户端断开连接时清空同步过来的配置快照，
     * 避免进入下一个世界时沿用旧服务器状态。
     */
    @SubscribeEvent
    static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        NeoTabClientState.reset();
    }
}
