package com.poso.neotab.network;

import com.poso.neotab.NeoTab;
import com.poso.neotab.network.payload.OpenConfigScreenPayload;
import com.poso.neotab.network.payload.SaveConfigPayload;
import com.poso.neotab.network.payload.SyncOnlineDurationsPayload;
import com.poso.neotab.network.payload.SyncPlayerHealthPayload;
import com.poso.neotab.network.payload.SyncTabConfigPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 自定义网络包注册中心。
 *
 * <p>目前包含：</p>
 * <ul>
 *     <li>服务端 -> 客户端：打开配置界面</li>
 *     <li>服务端 -> 客户端：同步配置状态</li>
 *     <li>服务端 -> 客户端：同步在线时长数据</li>
 *     <li>客户端 -> 服务端：保存配置</li>
 * </ul>
 */
public final class NeoTabPayloads {
    private NeoTabPayloads() {
    }

    /** 在 MOD 总线上注册 play 阶段的双向配置相关数据包。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenConfigScreenPayload.TYPE, OpenConfigScreenPayload.STREAM_CODEC, OpenConfigScreenPayload::handle);
        registrar.playToClient(SyncTabConfigPayload.TYPE, SyncTabConfigPayload.STREAM_CODEC, SyncTabConfigPayload::handle);
        registrar.playToClient(SyncOnlineDurationsPayload.TYPE, SyncOnlineDurationsPayload.STREAM_CODEC, SyncOnlineDurationsPayload::handle);
        registrar.playToClient(SyncPlayerHealthPayload.TYPE, SyncPlayerHealthPayload.STREAM_CODEC, SyncPlayerHealthPayload::handle);
        registrar.playToServer(SaveConfigPayload.TYPE, SaveConfigPayload.STREAM_CODEC, SaveConfigPayload::handle);
    }
}
