package com.poso.neotab.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
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
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> clz = Class.forName("com.poso.neotab.network.client.ClientPacketHandlers");
                        Method method = clz.getMethod("handleSyncOnlineDurations", Map.class);
                        method.invoke(null, packet.onlineDurations);
                    } catch (Exception e) {
                        com.poso.neotab.NeoTab.LOGGER.error("Failed to sync online durations", e);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
    
    public Map<UUID, String> getOnlineDurations() {
        return onlineDurations;
    }
}