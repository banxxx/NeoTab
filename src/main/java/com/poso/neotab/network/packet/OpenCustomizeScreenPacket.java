package com.poso.neotab.network.packet;

import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * 打开个人自定义界面网络包。
 * 
 * <p>服务端发送给客户端，指示客户端打开玩家个人自定义界面。</p>
 */
public class OpenCustomizeScreenPacket {

    private final TabConfig serverConfig;
    private final PlayerTabConfig playerConfig;
    private final PlayerCustomizePolicy policy;
    
    public OpenCustomizeScreenPacket(TabConfig serverConfig, PlayerTabConfig playerConfig, PlayerCustomizePolicy policy) {
        this.serverConfig = serverConfig;
        this.playerConfig = playerConfig;
        this.policy = policy;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(OpenCustomizeScreenPacket packet, FriendlyByteBuf buf) {
        packet.serverConfig.write(buf);
        // 序列化玩家配置
        packet.playerConfig.write(buf);
        // 序列化策略
        packet.policy.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static OpenCustomizeScreenPacket decode(FriendlyByteBuf buf) {
        TabConfig serverConfig = TabConfig.read(buf);
        PlayerTabConfig playerConfig = PlayerTabConfig.read(buf);
        PlayerCustomizePolicy policy = PlayerCustomizePolicy.read(buf);
        return new OpenCustomizeScreenPacket(serverConfig, playerConfig, policy);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(OpenCustomizeScreenPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> clz = Class.forName(
                                "com.poso.neotab.network.client.ClientPacketHandlers");
                        Method method = clz.getMethod("handleOpenCustomizeScreen",
                                TabConfig.class,
                                PlayerTabConfig.class,
                                PlayerCustomizePolicy.class);
                        method.invoke(null,
                                packet.serverConfig,
                                packet.playerConfig,
                                packet.policy);
                    } catch (Exception e) {
                        com.poso.neotab.NeoTab.LOGGER
                                .error("Failed to open player config screen", e);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }

    public TabConfig getServerConfig() {
        return serverConfig;
    }

    public PlayerTabConfig getPlayerConfig() {
        return playerConfig;
    }
    
    public PlayerCustomizePolicy getPolicy() {
        return policy;
    }
}