package com.poso.neotab.network.packet;

import com.poso.neotab.config.TabConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开配置界面网络包。
 * 
 * <p>服务端发送给客户端，指示客户端打开管理员配置界面。</p>
 */
public class OpenConfigScreenPacket {
    
    private final TabConfig config;
    
    public OpenConfigScreenPacket(TabConfig config) {
        this.config = config;
    }
    
    /**
     * 编码数据到缓冲区。
     */
    public static void encode(OpenConfigScreenPacket packet, FriendlyByteBuf buf) {
        // 序列化配置对象
        packet.config.write(buf);
    }
    
    /**
     * 从缓冲区解码数据。
     */
    public static OpenConfigScreenPacket decode(FriendlyByteBuf buf) {
        TabConfig config = TabConfig.read(buf);
        return new OpenConfigScreenPacket(config);
    }
    
    /**
     * 处理网络包。
     */
    public static void handle(OpenConfigScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleClientSide(OpenConfigScreenPacket packet) {
        // 在客户端打开配置界面
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.setScreen(
            new com.poso.neotab.client.screen.NeoTabConfigScreen(
                minecraft.screen,  // parent screen
                packet.config      // config
            )
        );
    }
    
    public TabConfig getConfig() {
        return config;
    }
}