package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 同步所有在线玩家的在线时长数据到客户端。
 *
 * <p>这个数据包会定期发送给客户端，让客户端能够在TAB列表中
 * 显示真实的在线时长，而不是占位符。</p>
 */
public record SyncOnlineDurationsPayload(Map<UUID, String> onlineDurations) implements CustomPacketPayload {
    public static final Type<SyncOnlineDurationsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "sync_online_durations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOnlineDurationsPayload> STREAM_CODEC = CustomPacketPayload.codec(
        SyncOnlineDurationsPayload::write,
        SyncOnlineDurationsPayload::read
    );

    @Override
    public Type<SyncOnlineDurationsPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(onlineDurations.size());
        for (Map.Entry<UUID, String> entry : onlineDurations.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeUtf(entry.getValue(), 16); // 最大16字符，足够"99d23h"这样的格式
        }
    }

    private static SyncOnlineDurationsPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<UUID, String> durations = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID playerId = buffer.readUUID();
            String duration = buffer.readUtf(16);
            durations.put(playerId, duration);
        }
        return new SyncOnlineDurationsPayload(durations);
    }

    public static void handle(SyncOnlineDurationsPayload payload, IPayloadContext context) {
        NeoTabClientState.setOnlineDurations(payload.onlineDurations);
    }
}