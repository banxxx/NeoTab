package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.TabConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端：保存配置。
 *
 * <p>注意：客户端只是提交“想保存的配置”，真正是否允许保存，
 * 必须由服务端再次校验权限。</p>
 */
public record SaveConfigPayload(TabConfig config) implements CustomPacketPayload {
    public static final Type<SaveConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "save_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfigPayload> STREAM_CODEC = CustomPacketPayload.codec(
        SaveConfigPayload::write,
        SaveConfigPayload::read
    );

    @Override
    public Type<SaveConfigPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        config.write(buffer);
    }

    private static SaveConfigPayload read(RegistryFriendlyByteBuf buffer) {
        return new SaveConfigPayload(TabConfig.read(buffer));
    }

    /** 服务端收到后交给业务服务保存并广播。 */
    public static void handle(SaveConfigPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            NeoTab.service().updateConfig(player, payload.config);
        }
    }
}
