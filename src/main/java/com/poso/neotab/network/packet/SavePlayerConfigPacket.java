package com.poso.neotab.network.packet;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.PlayerTabConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 保存玩家个人配置网络包。
 * 
 * <p>客户端发送给服务端，提交玩家修改的个人配置。</p>
 */
public class SavePlayerConfigPacket {
    
    private final PlayerTabConfig playerConfig;
    
    public SavePlayerConfigPacket(PlayerTabConfig playerConfig) {
        this.playerConfig = playerConfig;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SavePlayerConfigPacket packet, FriendlyByteBuf buf) {
        // 序列化玩家配置对象
        packet.playerConfig.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SavePlayerConfigPacket decode(FriendlyByteBuf buf) {
        PlayerTabConfig playerConfig = PlayerTabConfig.read(buf);
        return new SavePlayerConfigPacket(playerConfig);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SavePlayerConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleServerSide(SavePlayerConfigPacket packet, ServerPlayer sender) {
        try {
            // 委托给服务层处理，包含策略验证
            NeoTab.service().savePlayerConfig(sender, packet.playerConfig);
        } catch (Exception e) {
            NeoTab.LOGGER.error("Error handling SavePlayerConfigPacket from player {}: {}", 
                sender.getName().getString(), e.getMessage());
        }
    }
    
    public PlayerTabConfig getPlayerConfig() {
        return playerConfig;
    }
}