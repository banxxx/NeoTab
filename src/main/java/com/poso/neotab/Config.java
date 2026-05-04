package com.poso.neotab;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * NeoTab 配置类。
 * 
 * <p>这里将放置 NeoTab 的配置项，目前为空，等待后续添加具体配置。</p>
 */
@Mod.EventBusSubscriber(modid = NeoTab.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // TODO: 添加 NeoTab 特定的配置项

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // TODO: 加载 NeoTab 配置
    }
}
