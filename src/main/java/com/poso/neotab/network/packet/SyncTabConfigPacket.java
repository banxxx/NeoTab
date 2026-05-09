package com.poso.neotab.network.packet;

import com.poso.neotab.config.TabConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * 同步TAB配置网络包。
 * 
 * <p>服务端发送给客户端，同步当前的TAB配置数据。</p>
 */
public class SyncTabConfigPacket {
    
    private final TabConfig config;
    
    public SyncTabConfigPacket(TabConfig config) {
        this.config = config;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SyncTabConfigPacket packet, FriendlyByteBuf buf) {
        // 序列化配置对象
        packet.config.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SyncTabConfigPacket decode(FriendlyByteBuf buf) {
        TabConfig config = TabConfig.read(buf);
        return new SyncTabConfigPacket(config);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SyncTabConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> clz = Class.forName("com.poso.neotab.network.client.ClientPacketHandlers");
                        Method method = clz.getMethod("handleSyncTabConfig", TabConfig.class);
                        method.invoke(null, packet.config);
                    } catch (Exception e) {
                        com.poso.neotab.NeoTab.LOGGER.error("Failed to sync tab config", e);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }

    public TabConfig getConfig() {
        return config;
    }
}