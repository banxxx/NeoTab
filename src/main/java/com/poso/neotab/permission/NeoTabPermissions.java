package com.poso.neotab.permission;

import com.poso.neotab.NeoTab;
import com.poso.neotab.config.TabConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.UUID;

/**
 * 权限定义与权限检查。
 *
 * <p>包含两类权限：</p>
 * <ul>
 *     <li>管理员权限：{@link #CONFIGURE}，控制是否可以打开管理员配置界面</li>
 *     <li>玩家自定义权限：{@code CUSTOMIZE_*}，控制玩家可以自定义哪些 TAB 选项</li>
 * </ul>
 *
 * <p>默认策略：</p>
 * <ul>
 *     <li>如果 Permission API 正常可用，则走权限节点</li>
 *     <li>如果当前环境没有正确初始化权限系统，则回退到原版 OP 2 级权限（管理员）
 *         或 false（玩家自定义权限，由服务器策略单独控制）</li>
 * </ul>
 */
public final class NeoTabPermissions {

    // ── 管理员权限 ────────────────────────────────────────────────────────────

    /** 管理员配置权限节点。 */
    public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
        NeoTab.MODID, "configure", PermissionTypes.BOOLEAN,
        (player, uuid, contexts) -> player != null && player.hasPermissions(2)
    );

    // ── 玩家自定义权限：总开关 ────────────────────────────────────────────────

    /** 允许玩家打开个人自定义界面（总开关，默认所有玩家可用）。 */
    public static final PermissionNode<Boolean> CUSTOMIZE = new PermissionNode<>(
        NeoTab.MODID, "customize", PermissionTypes.BOOLEAN,
        (player, uuid, contexts) -> true
    );

    // ── 玩家自定义权限：顶部信息 ──────────────────────────────────────────────

    public static final PermissionNode<Boolean> CUSTOMIZE_TOP_TITLE_TOGGLE = customizeNode(
        "customize.top_title.toggle");
    public static final PermissionNode<Boolean> CUSTOMIZE_TOP_TITLE_EDIT = customizeNode(
        "customize.top_title.edit");
    public static final PermissionNode<Boolean> CUSTOMIZE_TOP_CONTENT_TOGGLE = customizeNode(
        "customize.top_content.toggle");
    public static final PermissionNode<Boolean> CUSTOMIZE_TOP_CONTENT_EDIT = customizeNode(
        "customize.top_content.edit");

    // ── 玩家自定义权限：玩家列表 ──────────────────────────────────────────────

    public static final PermissionNode<Boolean> CUSTOMIZE_PING = customizeNode(
        "customize.ping");
    public static final PermissionNode<Boolean> CUSTOMIZE_DURATION = customizeNode(
        "customize.duration");
    public static final PermissionNode<Boolean> CUSTOMIZE_TITLE = customizeNode(
        "customize.title");
    public static final PermissionNode<Boolean> CUSTOMIZE_HEALTH_TOGGLE = customizeNode(
        "customize.health.toggle");
    public static final PermissionNode<Boolean> CUSTOMIZE_HEALTH_MODE = customizeNode(
        "customize.health.mode");

    // ── 玩家自定义权限：底部信息 ──────────────────────────────────────────────

    public static final PermissionNode<Boolean> CUSTOMIZE_FOOTER_CUSTOM = customizeNode(
        "customize.footer.custom");
    public static final PermissionNode<Boolean> CUSTOMIZE_FOOTER_TPS = customizeNode(
        "customize.footer.tps");
    public static final PermissionNode<Boolean> CUSTOMIZE_FOOTER_MSPT = customizeNode(
        "customize.footer.mspt");
    public static final PermissionNode<Boolean> CUSTOMIZE_FOOTER_ONLINE = customizeNode(
        "customize.footer.online");

    // ── 玩家自定义权限：主题 ──────────────────────────────────────────────────

    public static final PermissionNode<Boolean> CUSTOMIZE_THEME = customizeNode(
        "customize.theme");

    // ── 所有自定义权限节点的数组，供注册时使用 ────────────────────────────────

    @SuppressWarnings("unchecked")
    public static final PermissionNode<Boolean>[] ALL_CUSTOMIZE_NODES = new PermissionNode[] {
        CUSTOMIZE,
        CUSTOMIZE_TOP_TITLE_TOGGLE, CUSTOMIZE_TOP_TITLE_EDIT,
        CUSTOMIZE_TOP_CONTENT_TOGGLE, CUSTOMIZE_TOP_CONTENT_EDIT,
        CUSTOMIZE_PING, CUSTOMIZE_DURATION, CUSTOMIZE_TITLE,
        CUSTOMIZE_HEALTH_TOGGLE, CUSTOMIZE_HEALTH_MODE,
        CUSTOMIZE_FOOTER_CUSTOM, CUSTOMIZE_FOOTER_TPS,
        CUSTOMIZE_FOOTER_MSPT, CUSTOMIZE_FOOTER_ONLINE,
        CUSTOMIZE_THEME
    };

    private NeoTabPermissions() {
    }

    // ── 管理员权限判断 ────────────────────────────────────────────────────────

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

    // ── 玩家自定义权限判断 ────────────────────────────────────────────────────

    /** 判断玩家是否可以打开个人自定义界面。 */
    public static boolean canCustomize(ServerPlayer player) {
        try {
            return PermissionAPI.getPermission(player, CUSTOMIZE);
        } catch (Exception exception) {
            return true; // 没有权限管理模组时默认允许
        }
    }

    /**
     * 获取玩家实际生效的自定义策略。
     *
     * <p>优先级（从高到低）：</p>
     * <ol>
     *     <li>玩家 OP 等级 ≥ 2 → 返回完全开放策略（兼容单人世界）</li>
     *     <li>个人专属策略（UUID 匹配）→ 直接使用，不再叠加权限节点</li>
     *     <li>全局策略 AND 权限节点 → 两者都允许才开放</li>
     * </ol>
     *
     * @param player       玩家对象
     * @param serverConfig 当前服务器配置（含策略）
     * @return 该玩家实际生效的自定义策略
     */
    public static PlayerCustomizePolicy resolvePolicy(ServerPlayer player, TabConfig serverConfig) {
        // 1. OP ≥ 2 直接返回完全开放（单人世界房主也适用）
        if (player.hasPermissions(2)) {
            return PlayerCustomizePolicy.unlocked();
        }

        UUID uuid = player.getUUID();

        // 2. 个人专属策略优先
        PlayerCustomizePolicy personal = serverConfig.playerPolicies().get(uuid);
        if (personal != null) {
            return personal;
        }

        // 3. 全局策略 AND 权限节点
        PlayerCustomizePolicy global = serverConfig.globalPolicy();
        PlayerCustomizePolicy fromNodes = new PlayerCustomizePolicy(
            perm(player, CUSTOMIZE_TOP_TITLE_TOGGLE),
            perm(player, CUSTOMIZE_TOP_TITLE_EDIT),
            perm(player, CUSTOMIZE_TOP_CONTENT_TOGGLE),
            perm(player, CUSTOMIZE_TOP_CONTENT_EDIT),
            perm(player, CUSTOMIZE_PING),
            perm(player, CUSTOMIZE_DURATION),
            perm(player, CUSTOMIZE_TITLE),
            perm(player, CUSTOMIZE_HEALTH_TOGGLE),
            perm(player, CUSTOMIZE_HEALTH_MODE),
            perm(player, CUSTOMIZE_FOOTER_CUSTOM),
            perm(player, CUSTOMIZE_FOOTER_TPS),
            perm(player, CUSTOMIZE_FOOTER_MSPT),
            perm(player, CUSTOMIZE_FOOTER_ONLINE),
            perm(player, CUSTOMIZE_THEME),
            false // refreshInterval 不开放给玩家
        );

        return global.and(fromNodes);
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────────────

    /**
     * 查询单个权限节点，没有权限管理模组时回退为 true。
     * 玩家自定义权限在没有权限管理模组时，完全由服务器策略控制。
     */
    private static boolean perm(ServerPlayer player, PermissionNode<Boolean> node) {
        try {
            return PermissionAPI.getPermission(player, node);
        } catch (Exception e) {
            return true; // 没有权限管理模组时默认允许，由服务器配置控制
        }
    }

    /**
     * 创建一个默认值为 false 的玩家自定义权限节点。
     */
    private static PermissionNode<Boolean> customizeNode(String path) {
        return new PermissionNode<>(
            NeoTab.MODID, path, PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> false
        );
    }
}