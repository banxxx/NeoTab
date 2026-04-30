package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.client.screen.NeoTabConfigScreen;
import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -> 客户端：打开玩家个人自定义界面。
 *
 * <p>由 {@code /neotab customize} 命令触发，服务端将当前玩家的
 * 个人配置和有效策略一并打包发送，客户端收到后直接打开界面，
 * 无需再额外请求数据。</p>
 */
public record OpenCustomizeScreenPayload(
    PlayerTabConfig personalConfig,
    PlayerCustomizePolicy policy
) implements CustomPacketPayload {

    public static final Type<OpenCustomizeScreenPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "open_customize_screen")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCustomizeScreenPayload> STREAM_CODEC =
        CustomPacketPayload.codec(
            OpenCustomizeScreenPayload::write,
            OpenCustomizeScreenPayload::read
        );

    @Override
    public Type<OpenCustomizeScreenPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        personalConfig.write(buffer);
        policy.write(buffer);
    }

    private static OpenCustomizeScreenPayload read(RegistryFriendlyByteBuf buffer) {
        PlayerTabConfig config = PlayerTabConfig.read(buffer);
        PlayerCustomizePolicy policy = PlayerCustomizePolicy.read(buffer);
        return new OpenCustomizeScreenPayload(config, policy);
    }

    /**
     * 客户端收到后：
     * <ol>
     *     <li>更新本地策略缓存</li>
     *     <li>更新本地个人配置缓存</li>
     *     <li>打开玩家自定义界面（PLAYER 模式）</li>
     * </ol>
     */
    public static void handle(OpenCustomizeScreenPayload payload, IPayloadContext context) {
        NeoTabClientState.setCurrentPolicy(payload.policy);
        NeoTabClientState.setPersonalConfig(payload.personalConfig);

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(
            new NeoTabConfigScreen(
                minecraft.screen,
                NeoTabClientState.getCurrentConfig(),
                NeoTabConfigScreen.ScreenMode.PLAYER,
                payload.policy,
                payload.personalConfig
            )
        );
    }
}
