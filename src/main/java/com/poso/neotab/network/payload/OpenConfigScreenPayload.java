package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.screen.NeoTabConfigScreen;
import com.poso.neotab.config.TabConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -> 客户端：打开配置界面。
 *
 * <p>包里直接携带当前生效配置，客户端收到后无需再额外拉取一次数据。</p>
 */
public record OpenConfigScreenPayload(TabConfig config) implements CustomPacketPayload {
    /** 自定义包类型 ID。 */
    public static final Type<OpenConfigScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "open_config_screen"));

    /** 用最直接的方式把 TabConfig 编解码进网络缓冲区。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigScreenPayload> STREAM_CODEC = CustomPacketPayload.codec(
        OpenConfigScreenPayload::write,
        OpenConfigScreenPayload::read
    );

    @Override
    public Type<OpenConfigScreenPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        config.write(buffer);
    }

    private static OpenConfigScreenPayload read(RegistryFriendlyByteBuf buffer) {
        return new OpenConfigScreenPayload(TabConfig.read(buffer));
    }

    /** 客户端收到后立即打开 NeoTab 配置界面。 */
    public static void handle(OpenConfigScreenPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NeoTabConfigScreen(minecraft.screen, payload.config));
    }
}
