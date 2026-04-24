package com.poso.neotab.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.poso.neotab.NeoTab;
import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.OpenConfigScreenPayload;
import com.poso.neotab.permission.NeoTabPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoTab 命令注册。
 *
 * <p>当前只保留两个管理员命令：</p>
 * <ul>
 *     <li>/neotab config - 打开配置界面</li>
 *     <li>/neotab reload - 从磁盘重载配置</li>
 * </ul>
 */
public final class NeoTabCommands {
    private NeoTabCommands() {
    }

    /** 把命令树挂到 Brigadier 调度器中。 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neotab")
            .then(Commands.literal("config")
                .requires(NeoTabPermissions::canConfigure)
                .executes(context -> openConfigScreen(context.getSource())))
            .then(Commands.literal("reload")
                .requires(NeoTabPermissions::canConfigure)
                .executes(context -> reload(context.getSource()))));
    }

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
}
