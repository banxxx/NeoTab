package com.poso.neotab.event;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NeoTab 客户端事件处理。
 *
 * <p>处理客户端相关的游戏事件，包括：</p>
 * <ul>
 *     <li>客户端 Tick 事件 - 更新 GUI 缩放监听和布局调整</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)
public final class NeoTabClientEvents {

    private NeoTabClientEvents() {
    }

    /**
     * 客户端 Tick 事件处理。
     * 用于监听 GUI 缩放变化并重新计算布局。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            NeoTabClientState.onClientTick(minecraft);
        }
    }
}
