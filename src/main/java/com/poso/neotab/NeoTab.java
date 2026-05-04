package com.poso.neotab;

import com.mojang.logging.LogUtils;
import com.poso.neotab.command.NeoTabCommands;
import com.poso.neotab.network.NeoTabNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 模组主入口。
 *
 * <p>这个类只做两件事：</p>
 * <ul>
 *     <li>持有全局唯一的服务对象</li>
 *     <li>在 MOD 事件总线上注册网络包</li>
 * </ul>
 *
 * <p>把真正的业务逻辑下沉到 service / event / network / client 包中，
 * 方便后续继续扩展，例如：</p>
 * <ul>
 *     <li>新增更多占位符</li>
 *     <li>新增排序、分组、前后缀</li>
 *     <li>改成真正自定义的 TAB 列布局</li>
 * </ul>
 */
@Mod(NeoTab.MODID)
public class NeoTab
{
    /** 模组 ID，必须与 mods.toml 保持一致。 */
    public static final String MODID = "neotab";

    /** 统一日志入口，后续调试服务端同步、命令、UI 时都走这里。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 全局服务实例，提供核心业务逻辑访问入口。 */
    private static com.poso.neotab.service.NeoTabService serviceInstance;

    /**
     * 获取全局服务实例。
     * 
     * @return NeoTab 核心服务实例
     * @throws IllegalStateException 如果服务尚未初始化
     */
    public static com.poso.neotab.service.NeoTabService service() {
        if (serviceInstance == null) {
            throw new IllegalStateException("NeoTab service not initialized yet");
        }
        return serviceInstance;
    }

    public NeoTab(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // 网络包注册将在 commonSetup 中添加
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 初始化服务实例
        serviceInstance = new com.poso.neotab.service.NeoTabService();
        
        // NeoTab 初始化逻辑
        LOGGER.info("NeoTab is initializing...");
        
        // 注册网络包
        NeoTabNetwork.register();
    }

    /**
     * 注册命令事件处理。
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NeoTabCommands.register(event.getDispatcher());
    }

    /**
     * 服务端启动完成事件处理。
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            serviceInstance.onServerStarted(server);
            LOGGER.info("NeoTab server initialization completed");
        } catch (Exception e) {
            LOGGER.error("Error during NeoTab server startup: {}", e.getMessage());
        }
    }

    /**
     * 服务端停止事件处理。
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        try {
            if (serviceInstance != null) {
                serviceInstance.onServerStopped();
            }
            LOGGER.info("NeoTab server cleanup completed");
        } catch (Exception e) {
            LOGGER.error("Error during NeoTab server cleanup: {}", e.getMessage());
        }
    }
}
