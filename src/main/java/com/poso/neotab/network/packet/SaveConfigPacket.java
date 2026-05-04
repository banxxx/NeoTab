package com.poso.neotab.network.packet;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.TabConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 保存服务器配置网络包。
 * 
 * <p>客户端发送给服务端，提交管理员修改的服务器配置。</p>
 */
public class SaveConfigPacket {
    
    private final TabConfig config;
    
    public SaveConfigPacket(TabConfig config) {
        this.config = config;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SaveConfigPacket packet, FriendlyByteBuf buf) {
        // 序列化配置对象
        packet.config.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SaveConfigPacket decode(FriendlyByteBuf buf) {
        TabConfig config = TabConfig.read(buf);
        return new SaveConfigPacket(config);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SaveConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 确保在服务端线程执行
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    handleServerSide(packet, sender);
                }
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * 服务端处理逻辑。
     */
    private static void handleServerSide(SaveConfigPacket packet, ServerPlayer sender) {
        try {
            // 委托给服务层处理，包含权限验证
            NeoTab.service().updateConfig(sender, packet.config);
        } catch (Exception e) {
            NeoTab.LOGGER.error("Error handling SaveConfigPacket from player {}: {}", 
                sender.getName().getString(), e.getMessage());
        }
    }
    
    public TabConfig getConfig() {
        return config;
    }
}