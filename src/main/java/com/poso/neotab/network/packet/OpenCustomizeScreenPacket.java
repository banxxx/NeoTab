package com.poso.neotab.network.packet;

import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开个人自定义界面网络包。
 * 
 * <p>服务端发送给客户端，指示客户端打开玩家个人自定义界面。</p>
 */
public class OpenCustomizeScreenPacket {
    
    private final PlayerTabConfig playerConfig;
    private final PlayerCustomizePolicy policy;
    
    public OpenCustomizeScreenPacket(PlayerTabConfig playerConfig, PlayerCustomizePolicy policy) {
        this.playerConfig = playerConfig;
        this.policy = policy;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(OpenCustomizeScreenPacket packet, FriendlyByteBuf buf) {
        // 序列化玩家配置
        packet.playerConfig.write(buf);
        // 序列化策略
        packet.policy.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static OpenCustomizeScreenPacket decode(FriendlyByteBuf buf) {
        PlayerTabConfig playerConfig = PlayerTabConfig.read(buf);
        PlayerCustomizePolicy policy = PlayerCustomizePolicy.read(buf);
        return new OpenCustomizeScreenPacket(playerConfig, policy);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(OpenCustomizeScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleClientSide(OpenCustomizeScreenPacket packet) {
        // 在客户端打开个人自定义界面
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new com.poso.neotab.client.screen.NeoTabCustomizeScreen(
                packet.playerConfig, packet.policy));
    }
    
    public PlayerTabConfig getPlayerConfig() {
        return playerConfig;
    }
    
    public PlayerCustomizePolicy getPolicy() {
        return policy;
    }
}