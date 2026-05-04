package com.poso.neotab.network;

import com.poso.neotab.NeoTab;
import com.poso.neotab.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * NeoTab 网络管理器。
 * 
 * <p>负责注册所有网络包和管理网络通信。</p>
 * 
 * <p>从 NeoForge 1.21.1 移植到 Forge 1.20.1 的主要变化：</p>
 * <ul>
 *     <li>从 Payload 系统改为传统的 Packet 系统</li>
 *     <li>使用 SimpleChannel 进行网络包注册</li>
 *     <li>每个包需要实现 encode/decode/handle 静态方法</li>
 * </ul>
 */
public final class NeoTabNetwork {
    
    /** 网络协议版本 */
    private static final String PROTOCOL_VERSION = "1";
    
    /** 网络通道实例 */
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NeoTab.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    /** 包ID计数器 */
    private static int packetId = 0;
    
    private NeoTabNetwork() {
    }
    
    /**
     * 注册所有网络包。
     * 
     * <p>必须在 FMLCommonSetupEvent 中调用。</p>
     */
    public static void register() {
        NeoTab.LOGGER.info("Registering NeoTab network packets...");
        
        // 配置相关网络包
        INSTANCE.registerMessage(packetId++, OpenConfigScreenPacket.class,
            OpenConfigScreenPacket::encode,
            OpenConfigScreenPacket::decode,
            OpenConfigScreenPacket::handle);
            
        INSTANCE.registerMessage(packetId++, OpenCustomizeScreenPacket.class,
            OpenCustomizeScreenPacket::encode,
            OpenCustomizeScreenPacket::decode,
            OpenCustomizeScreenPacket::handle);
            
        INSTANCE.registerMessage(packetId++, SaveConfigPacket.class,
            SaveConfigPacket::encode,
            SaveConfigPacket::decode,
            SaveConfigPacket::handle);
            
        INSTANCE.registerMessage(packetId++, SavePlayerConfigPacket.class,
            SavePlayerConfigPacket::encode,
            SavePlayerConfigPacket::decode,
            SavePlayerConfigPacket::handle);
        
        // 数据同步网络包
        INSTANCE.registerMessage(packetId++, SyncTabConfigPacket.class,
            SyncTabConfigPacket::encode,
            SyncTabConfigPacket::decode,
            SyncTabConfigPacket::handle);
            
        INSTANCE.registerMessage(packetId++, SyncCustomizePolicyPacket.class,
            SyncCustomizePolicyPacket::encode,
            SyncCustomizePolicyPacket::decode,
            SyncCustomizePolicyPacket::handle);
            
        INSTANCE.registerMessage(packetId++, SyncOnlineDurationsPacket.class,
            SyncOnlineDurationsPacket::encode,
            SyncOnlineDurationsPacket::decode,
            SyncOnlineDurationsPacket::handle);
            
        INSTANCE.registerMessage(packetId++, SyncPlayerHealthPacket.class,
            SyncPlayerHealthPacket::encode,
            SyncPlayerHealthPacket::decode,
            SyncPlayerHealthPacket::handle);
        
        NeoTab.LOGGER.info("Registered {} NeoTab network packets", packetId);
    }
}