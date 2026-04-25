package com.poso.neotab;

import com.mojang.logging.LogUtils;
import com.poso.neotab.network.NeoTabPayloads;
import com.poso.neotab.service.NeoTabService;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * 模组主入口。
 *
 * <p>这个类只做两件事：</p>
 * <ul>
 *     <li>持有全局唯一的服务对象 {@link NeoTabService}</li>
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
public class NeoTab {
    /** 模组 ID，必须与 mods.toml 保持一致。 */
    public static final String MODID = "neotab";

    /** 统一日志入口，后续调试服务端同步、命令、UI 时都走这里。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 全局唯一业务服务对象，负责配置加载、刷新和应用 TAB 内容。 */
    private static final NeoTabService SERVICE = new NeoTabService();

    public NeoTab(IEventBus modEventBus, ModContainer modContainer) {
        // 注册自定义网络包，客户端打开配置界面和保存配置都依赖这里。
        modEventBus.addListener(NeoTabPayloads::register);
    }

    /**
     * 暴露服务对象，供事件、命令、网络处理器统一访问。
     */
    public static NeoTabService service() {
        return SERVICE;
    }
}
