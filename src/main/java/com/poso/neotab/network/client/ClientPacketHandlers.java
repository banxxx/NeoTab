package com.poso.neotab.network.client;

import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.client.screen.NeoTabConfigScreen;
import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    // 管理员版 —— 打开完整配置界面
    public static void handleOpenConfigScreen(TabConfig config) {
        Minecraft.getInstance().setScreen(
                new NeoTabConfigScreen(Minecraft.getInstance().screen, config)
        );
    }

    // 玩家版 —— 打开受限配置界面（同管理员界面，但某些项被禁用/隐藏）
    public static void handleOpenCustomizeScreen(TabConfig serverConfig,
                                                 PlayerTabConfig personalConfig,
                                                 PlayerCustomizePolicy policy) {
        Minecraft.getInstance().setScreen(
                new NeoTabConfigScreen(
                        Minecraft.getInstance().screen,
                        serverConfig,
                        NeoTabConfigScreen.ScreenMode.PLAYER,
                        policy,
                        personalConfig
                )
        );
    }

    public static void handleSyncTabConfig(TabConfig config) {
        NeoTabClientState.updateConfig(config);
    }

    public static void handleSyncCustomizePolicy(PlayerCustomizePolicy policy) {
        NeoTabClientState.updatePolicy(policy);
    }

    public static void handleSyncOnlineDurations(Map<UUID, String> durations) {
        NeoTabClientState.updateOnlineDurations(durations);
    }

    public static void handleSyncPlayerHealth(Map<UUID, Float> healths,
                                              Map<UUID, Float> maxHealths) {
        NeoTabClientState.updatePlayerHealths(healths, maxHealths);
    }
}