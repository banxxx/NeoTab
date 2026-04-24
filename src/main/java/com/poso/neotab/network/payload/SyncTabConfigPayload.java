package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.config.TabConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -> 客户端：同步当前生效配置。
 *
 * <p>该包与“打开配置界面”不同，它面向全体在线玩家，
 * 主要用于客户端渲染层读取配置状态。</p>
 */
public record SyncTabConfigPayload(TabConfig config) implements CustomPacketPayload {
    public static final Type<SyncTabConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "sync_tab_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTabConfigPayload> STREAM_CODEC = CustomPacketPayload.codec(
        SyncTabConfigPayload::write,
        SyncTabConfigPayload::read
    );

    @Override
    public Type<SyncTabConfigPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        config.write(buffer);
    }

    private static SyncTabConfigPayload read(RegistryFriendlyByteBuf buffer) {
        return new SyncTabConfigPayload(TabConfig.read(buffer));
    }

    public static void handle(SyncTabConfigPayload payload, IPayloadContext context) {
        NeoTabClientState.setCurrentConfig(payload.config);
    }
}
