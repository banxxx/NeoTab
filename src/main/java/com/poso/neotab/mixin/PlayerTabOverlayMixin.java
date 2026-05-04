package com.poso.neotab.mixin;

import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * PlayerTabOverlay Mixin - TAB列表渲染核心修改。
 * 
 * <p>从 NeoForge 1.21.1 移植到 Forge 1.20.1 的主要适配：</p>
 * <ul>
 *     <li>验证目标类 PlayerTabOverlay 在 1.20.1 中的结构</li>
 *     <li>检查目标方法的签名是否匹配</li>
 *     <li>更新 Mixin 注入点和参数</li>
 * </ul>
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    
    /**
     * 修改玩家列表以支持分页显示。
     * 
     * <p>在渲染玩家列表之前，根据当前页码过滤玩家列表。</p>
     */
    @ModifyVariable(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        ordinal = 0
    )
    private List<PlayerInfo> neotab$filterPlayersForPagination(List<PlayerInfo> players) {
        if (players == null || players.isEmpty()) {
            return players;
        }
        
        // 获取当前分页信息
        int currentPage = NeoTabClientState.getCurrentPage();
        int playersPerPage = NeoTabClientState.getPlayersPerPage();
        int totalPlayers = players.size();
        
        // 更新总页数
        NeoTabClientState.recalculatePages(totalPlayers);
        
        // 计算当前页的玩家范围
        int startIndex = currentPage * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, totalPlayers);
        
        // 如果起始索引超出范围，返回空列表
        if (startIndex >= totalPlayers) {
            return List.of();
        }
        
        // 返回当前页的玩家子列表
        return players.subList(startIndex, endIndex);
    }
    
    /**
     * 在TAB列表渲染后添加分页信息显示。
     */
    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("TAIL")
    )
    private void neotab$renderPaginationInfo(GuiGraphics guiGraphics, int width, 
            net.minecraft.world.scores.Scoreboard scoreboard, 
            net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        
        int totalPages = NeoTabClientState.getTotalPages();
        if (totalPages <= 1) {
            return; // 只有一页时不显示分页信息
        }
        
        int currentPage = NeoTabClientState.getCurrentPage();
        
        // 构建分页信息文本
        Component pageInfo = Component.literal(String.format("第 %d/%d 页 (←/→ 翻页)", 
            currentPage + 1, totalPages));
        
        // 计算渲染位置（屏幕底部中央）
        int textWidth = net.minecraft.client.Minecraft.getInstance().font.width(pageInfo);
        int x = (width - textWidth) / 2;
        int y = guiGraphics.guiHeight() - 20;
        
        // 渲染分页信息
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, 
            pageInfo, x, y, 0xFFFFFF);
    }
}