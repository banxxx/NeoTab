package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.PlayerTabConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端：保存玩家个人配置。
 *
 * <p>注意：客户端只是提交"想保存的个人配置"，
 * 服务端收到后会在 {@link com.poso.neotab.service.NeoTabService#savePlayerConfig}
 * 中进行二次校验，拒绝玩家修改不被策略允许的字段。</p>
 */
public record SavePlayerConfigPayload(PlayerTabConfig config) implements CustomPacketPayload {

    public static final Type<SavePlayerConfigPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "save_player_config")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SavePlayerConfigPayload> STREAM_CODEC =
        CustomPacketPayload.codec(
            SavePlayerConfigPayload::write,
            SavePlayerConfigPayload::read
        );

    @Override
    public Type<SavePlayerConfigPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        config.write(buffer);
    }

    private static SavePlayerConfigPayload read(RegistryFriendlyByteBuf buffer) {
        return new SavePlayerConfigPayload(PlayerTabConfig.read(buffer));
    }

    /**
     * 服务端收到后交给业务服务进行校验和保存。
     *
     * <p>服务端会重新解析该玩家的有效策略，过滤掉不被允许的字段，
     * 然后保存合法部分并重新同步有效配置给该玩家。</p>
     */
    public static void handle(SavePlayerConfigPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            NeoTab.service().savePlayerConfig(player, payload.config);
        }
    }
}
