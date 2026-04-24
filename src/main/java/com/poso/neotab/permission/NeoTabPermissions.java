package com.poso.neotab.permission;

import com.poso.neotab.NeoTab;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * 权限定义与权限检查。
 *
 * <p>当前只定义一个核心权限：配置 NeoTab。</p>
 *
 * <p>默认策略：</p>
 * <ul>
 *     <li>如果 Permission API 正常可用，则走权限节点</li>
 *     <li>如果当前环境没有正确初始化权限系统，则回退到原版 OP 2 级权限</li>
 * </ul>
 */
public final class NeoTabPermissions {
    /** 管理员配置权限节点。 */
    public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
        ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "configure"),
        PermissionTypes.BOOLEAN,
        (player, uuid, contexts) -> player != null && player.hasPermissions(2)
    ).setInformation(
        Component.literal("Configure NeoTab"),
        Component.literal("Open the NeoTab admin UI and save server tab settings.")
    );

    private NeoTabPermissions() {
    }

    /** 供命令系统使用的权限判断。 */
    public static boolean canConfigure(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(2);
        }

        return canConfigure(player);
    }

    /** 供网络包和服务层使用的权限判断。 */
    public static boolean canConfigure(ServerPlayer player) {
        try {
            return PermissionAPI.getPermission(player, CONFIGURE);
        } catch (Exception exception) {
            return player.hasPermissions(2);
        }
    }
}
