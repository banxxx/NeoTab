package com.poso.neotab.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步在线时长网络包。
 * 
 * <p>服务端发送给客户端，同步所有玩家的在线时长数据。</p>
 */
public class SyncOnlineDurationsPacket {
    
    private final Map<UUID, String> onlineDurations;
    
    public SyncOnlineDurationsPacket(Map<UUID, String> onlineDurations) {
        this.onlineDurations = onlineDurations;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SyncOnlineDurationsPacket packet, FriendlyByteBuf buf) {
        // 写入Map大小
        buf.writeInt(packet.onlineDurations.size());
        
        // 写入每个条目
        for (Map.Entry<UUID, String> entry : packet.onlineDurations.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SyncOnlineDurationsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<UUID, String> onlineDurations = new HashMap<>(size);
        
        for (int i = 0; i < size; i++) {
            UUID playerId = buf.readUUID();
            String duration = buf.readUtf();
            onlineDurations.put(playerId, duration);
        }
        
        return new SyncOnlineDurationsPacket(onlineDurations);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SyncOnlineDurationsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleClientSide(SyncOnlineDurationsPacket packet) {
        // 更新客户端状态管理器中的在线时长数据
        com.poso.neotab.client.NeoTabClientState.updateOnlineDurations(packet.onlineDurations);
    }
    
    public Map<UUID, String> getOnlineDurations() {
        return onlineDurations;
    }
}