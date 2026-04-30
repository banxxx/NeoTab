package com.poso.neotab.network;

import com.poso.neotab.network.payload.OpenConfigScreenPayload;
import com.poso.neotab.network.payload.OpenCustomizeScreenPayload;
import com.poso.neotab.network.payload.SaveConfigPayload;
import com.poso.neotab.network.payload.SavePlayerConfigPayload;
import com.poso.neotab.network.payload.SyncCustomizePolicyPayload;
import com.poso.neotab.network.payload.SyncOnlineDurationsPayload;
import com.poso.neotab.network.payload.SyncPlayerHealthPayload;
import com.poso.neotab.network.payload.SyncTabConfigPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 自定义网络包注册中心。
 *
 * <p>服务端 -> 客户端：</p>
 * <ul>
 *     <li>{@link OpenConfigScreenPayload}      — 打开管理员配置界面</li>
 *     <li>{@link OpenCustomizeScreenPayload}   — 打开玩家个人自定义界面</li>
 *     <li>{@link SyncTabConfigPayload}         — 同步有效配置（含个人覆盖）</li>
 *     <li>{@link SyncCustomizePolicyPayload}   — 同步玩家有效自定义策略</li>
 *     <li>{@link SyncOnlineDurationsPayload}   — 同步在线时长数据</li>
 *     <li>{@link SyncPlayerHealthPayload}      — 同步玩家血量数据</li>
 * </ul>
 *
 * <p>客户端 -> 服务端：</p>
 * <ul>
 *     <li>{@link SaveConfigPayload}            — 管理员保存服务器配置</li>
 *     <li>{@link SavePlayerConfigPayload}      — 玩家保存个人配置</li>
 * </ul>
 */
public final class NeoTabPayloads {

    private NeoTabPayloads() {
    }

    /** 在 MOD 总线上注册 play 阶段的所有自定义数据包。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        // 版本号升级到 "2"，因为新增了网络包，需要确保客户端和服务端版本一致
        var registrar = event.registrar("2");

        // 服务端 -> 客户端
        registrar.playToClient(
            OpenConfigScreenPayload.TYPE,
            OpenConfigScreenPayload.STREAM_CODEC,
            OpenConfigScreenPayload::handle);
        registrar.playToClient(
            OpenCustomizeScreenPayload.TYPE,
            OpenCustomizeScreenPayload.STREAM_CODEC,
            OpenCustomizeScreenPayload::handle);
        registrar.playToClient(
            SyncTabConfigPayload.TYPE,
            SyncTabConfigPayload.STREAM_CODEC,
            SyncTabConfigPayload::handle);
        registrar.playToClient(
            SyncCustomizePolicyPayload.TYPE,
            SyncCustomizePolicyPayload.STREAM_CODEC,
            SyncCustomizePolicyPayload::handle);
        registrar.playToClient(
            SyncOnlineDurationsPayload.TYPE,
            SyncOnlineDurationsPayload.STREAM_CODEC,
            SyncOnlineDurationsPayload::handle);
        registrar.playToClient(
            SyncPlayerHealthPayload.TYPE,
            SyncPlayerHealthPayload.STREAM_CODEC,
            SyncPlayerHealthPayload::handle);

        // 客户端 -> 服务端
        registrar.playToServer(
            SaveConfigPayload.TYPE,
            SaveConfigPayload.STREAM_CODEC,
            SaveConfigPayload::handle);
        registrar.playToServer(
            SavePlayerConfigPayload.TYPE,
            SavePlayerConfigPayload.STREAM_CODEC,
            SavePlayerConfigPayload::handle);
    }
}
