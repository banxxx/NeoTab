package com.poso.neotab.network.packet;

import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步自定义策略网络包。
 * 
 * <p>服务端发送给客户端，同步玩家的自定义权限策略。</p>
 */
public class SyncCustomizePolicyPacket {
    
    private final PlayerCustomizePolicy policy;
    
    public SyncCustomizePolicyPacket(PlayerCustomizePolicy policy) {
        this.policy = policy;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(SyncCustomizePolicyPacket packet, FriendlyByteBuf buf) {
        // 序列化策略对象
        packet.policy.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static SyncCustomizePolicyPacket decode(FriendlyByteBuf buf) {
        PlayerCustomizePolicy policy = PlayerCustomizePolicy.read(buf);
        return new SyncCustomizePolicyPacket(policy);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(SyncCustomizePolicyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleClientSide(SyncCustomizePolicyPacket packet) {
        // 更新客户端状态管理器中的策略
        com.poso.neotab.client.NeoTabClientState.updatePolicy(packet.policy);
    }
    
    public PlayerCustomizePolicy getPolicy() {
        return policy;
    }
}