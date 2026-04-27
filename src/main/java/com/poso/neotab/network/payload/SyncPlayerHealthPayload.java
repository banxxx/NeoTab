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
 * 同步所有在线玩家的血量数据到客户端。
 *
 * <p>同时传输当前血量和最大血量，客户端据此判断是否超出原版上限（20），
 * 超出时在心形图标后追加整数数字。</p>
 */
public record SyncPlayerHealthPayload(
    Map<UUID, Float> playerHealths,
    Map<UUID, Float> playerMaxHealths
) implements CustomPacketPayload {

    public static final Type<SyncPlayerHealthPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "sync_player_health")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerHealthPayload> STREAM_CODEC =
        CustomPacketPayload.codec(SyncPlayerHealthPayload::write, SyncPlayerHealthPayload::read);

    @Override
    public Type<SyncPlayerHealthPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(playerHealths.size());
        for (Map.Entry<UUID, Float> entry : playerHealths.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeFloat(entry.getValue());
            buffer.writeFloat(playerMaxHealths.getOrDefault(entry.getKey(), 20.0f));
        }
    }

    private static SyncPlayerHealthPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<UUID, Float> healths    = new HashMap<>(size);
        Map<UUID, Float> maxHealths = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID id        = buffer.readUUID();
            float health   = buffer.readFloat();
            float maxHealth = buffer.readFloat();
            healths.put(id, health);
            maxHealths.put(id, maxHealth);
        }
        return new SyncPlayerHealthPayload(healths, maxHealths);
    }

    public static void handle(SyncPlayerHealthPayload payload, IPayloadContext context) {
        NeoTabClientState.setPlayerHealths(payload.playerHealths(), payload.playerMaxHealths());
    }
}
