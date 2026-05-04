package com.poso.neotab.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步玩家血量网络包。
 * 
 * <p>服务端发送给客户端，同步所有玩家的血量数据。</p>
 */
public class SyncPlayerHealthPacket {
    
    private final Map<UUID, Float> playerHealths;
    private final Map<UUID, Float> playerMaxHealths;
    
    public SyncPlayerHealthPacket(Map<UUID, Float> playerHealths, Map<UUID, Float> playerMaxHealths) {
        this.playerHealths = playerHealths;
        this.playerMaxHealths = playerMaxHealths;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SyncPlayerHealthPacket packet, FriendlyByteBuf buf) {
        // 写入当前血量Map
        buf.writeInt(packet.playerHealths.size());
        for (Map.Entry<UUID, Float> entry : packet.playerHealths.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
        
        // 写入最大血量Map
        buf.writeInt(packet.playerMaxHealths.size());
        for (Map.Entry<UUID, Float> entry : packet.playerMaxHealths.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SyncPlayerHealthPacket decode(FriendlyByteBuf buf) {
        // 读取当前血量Map
        int healthSize = buf.readInt();
        Map<UUID, Float> playerHealths = new HashMap<>(healthSize);
        for (int i = 0; i < healthSize; i++) {
            UUID playerId = buf.readUUID();
            Float health = buf.readFloat();
            playerHealths.put(playerId, health);
        }
        
        // 读取最大血量Map
        int maxHealthSize = buf.readInt();
        Map<UUID, Float> playerMaxHealths = new HashMap<>(maxHealthSize);
        for (int i = 0; i < maxHealthSize; i++) {
            UUID playerId = buf.readUUID();
            Float maxHealth = buf.readFloat();
            playerMaxHealths.put(playerId, maxHealth);
        }
        
        return new SyncPlayerHealthPacket(playerHealths, playerMaxHealths);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SyncPlayerHealthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 确保在客户端线程执行
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClientSide(packet);
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 客户端处理逻辑。
     */
    private static void handleClientSide(SyncPlayerHealthPacket packet) {
        // 更新客户端状态管理器中的血量数据
        com.poso.neotab.client.NeoTabClientState.updatePlayerHealths(
            packet.playerHealths, packet.playerMaxHealths);
    }
    
    public Map<UUID, Float> getPlayerHealths() {
        return playerHealths;
    }
    
    public Map<UUID, Float> getPlayerMaxHealths() {
        return playerMaxHealths;
    }
}