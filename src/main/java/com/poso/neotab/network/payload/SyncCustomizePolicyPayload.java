package com.poso.neotab.network.payload;

import com.poso.neotab.NeoTab;
import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 -> 客户端：同步当前玩家的有效自定义策略。
 *
 * <p>服务端在发送此包之前已经通过 {@code NeoTabPermissions.resolvePolicy()}
 * 计算好了该玩家的最终策略（OP / 个人专属 / 全局策略 AND 权限节点），
 * 客户端直接使用，无需再做任何计算。</p>
 *
 * <p>客户端收到后存入 {@link NeoTabClientState#setCurrentPolicy}，
 * 用于决定个人自定义界面中哪些组件可交互。</p>
 */
public record SyncCustomizePolicyPayload(PlayerCustomizePolicy policy) implements CustomPacketPayload {

    public static final Type<SyncCustomizePolicyPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "sync_customize_policy")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCustomizePolicyPayload> STREAM_CODEC =
        CustomPacketPayload.codec(
            SyncCustomizePolicyPayload::write,
            SyncCustomizePolicyPayload::read
        );

    @Override
    public Type<SyncCustomizePolicyPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        policy.write(buffer);
    }

    private static SyncCustomizePolicyPayload read(RegistryFriendlyByteBuf buffer) {
        return new SyncCustomizePolicyPayload(PlayerCustomizePolicy.read(buffer));
    }

    /** 客户端收到后更新本地策略缓存。 */
    public static void handle(SyncCustomizePolicyPayload payload, IPayloadContext context) {
        NeoTabClientState.setCurrentPolicy(payload.policy);
    }
}
