package com.poso.neotab.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.poso.neotab.NeoTab;
import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.OpenConfigScreenPayload;
import com.poso.neotab.network.payload.OpenCustomizeScreenPayload;
import com.poso.neotab.permission.NeoTabPermissions;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoTab 命令注册。
 *
 * <p>命令列表：</p>
 * <ul>
 *     <li>{@code /neotab config}     — 管理员：打开服务器配置界面</li>
 *     <li>{@code /neotab reload}     — 管理员：从磁盘重载配置</li>
 *     <li>{@code /neotab customize}  — 玩家：打开个人自定义界面</li>
 * </ul>
 */
public final class NeoTabCommands {

    private NeoTabCommands() {
    }

    /** 把命令树挂到 Brigadier 调度器中。 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neotab")
            // 管理员命令
            .then(Commands.literal("config")
                .requires(NeoTabPermissions::canConfigure)
                .executes(context -> openConfigScreen(context.getSource())))
            .then(Commands.literal("reload")
                .requires(NeoTabPermissions::canConfigure)
                .executes(context -> reload(context.getSource())))
            // 玩家自定义命令
            .then(Commands.literal("customize")
                .requires(src -> src.getEntity() instanceof ServerPlayer p
                    && NeoTabPermissions.canCustomize(p))
                .executes(context -> openCustomizeScreen(context.getSource())))
        );
    }

    // ── 管理员命令 ────────────────────────────────────────────────────────────

    /**
     * 打开管理员配置界面。
     *
     * <p>这个界面由服务端命令触发，但最终会通过网络包在客户端打开，
     * 所以执行者必须是玩家而不是控制台。</p>
     */
    private static int openConfigScreen(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.neotab.player_only"));
            return 0;
        }

        TabConfig config = NeoTab.service().getConfig();
        PacketDistributor.sendToPlayer(player, new OpenConfigScreenPayload(config));
        source.sendSuccess(() -> Component.translatable("command.neotab.config_opened"), false);
        return Command.SINGLE_SUCCESS;
    }

    /** 从磁盘重新读取配置。 */
    private static int reload(CommandSourceStack source) {
        NeoTab.service().reload(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.neotab.reloaded"), true);
        return Command.SINGLE_SUCCESS;
    }

    // ── 玩家命令 ──────────────────────────────────────────────────────────────

    /**
     * 打开玩家个人自定义界面。
     *
     * <p>服务端计算好该玩家的有效策略和当前个人配置，
     * 一并打包发送给客户端，客户端收到后直接打开界面。</p>
     */
    private static int openCustomizeScreen(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.neotab.player_only"));
            return 0;
        }

        // 计算该玩家的有效策略
        PlayerCustomizePolicy policy = NeoTabPermissions.resolvePolicy(
            player, NeoTab.service().getConfig()
        );

        // 获取该玩家的当前个人配置
        PlayerTabConfig personalConfig = NeoTab.service().getPlayerConfig(player.getUUID());

        PacketDistributor.sendToPlayer(player, new OpenCustomizeScreenPayload(personalConfig, policy));
        source.sendSuccess(() -> Component.translatable("command.neotab.customize_opened"), false);
        return Command.SINGLE_SUCCESS;
    }
}
