package com.poso.neotab.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修正单人世界里按 Tab 不显示玩家列表的问题。
 *
 * <p>原版逻辑在“本地服务器 + 在线人数 <= 1 + 没有 LIST 记分板目标”时，
 * 会直接隐藏玩家列表。对于 NeoTab 来说，即使只有一个玩家，也可能已经
 * 有 header/footer 这类服务端信息需要展示，因此这里改为：
 * 只要 NeoTab 已经给 TAB 设置了可见文本，就允许继续渲染。</p>
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private PlayerTabOverlay tabList;

    /** 上一帧右键是否已按下，用于边沿检测防止连续触发。 */
    private boolean neotab$wasRightClickDown = false;
    /** 上一帧左键是否已按下，用于翻页点击边沿检测。 */
    private boolean neotab$wasLeftClickDown = false;

    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void neotab$renderTabList(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);

        boolean forcedVisibleByNeoTab = neotab$hasNeoTabContent();
        
        // 检测 Tab+右键 切换固定状态
        if (this.minecraft.options.keyPlayerList.isDown() && this.minecraft.options.keyUse.isDown()) {
            // 右键刚按下时切换固定状态（防止连续触发）
            if (!neotab$wasRightClickDown) {
                boolean pinned = com.poso.neotab.client.NeoTabClientState.toggleTabPinned();
                neotab$wasRightClickDown = true;
                // 在聊天栏显示提示（overlay=true 显示在动作栏，不污染聊天记录）
                if (this.minecraft.player != null) {
                    net.minecraft.network.chat.Component msg = pinned
                        ? net.minecraft.network.chat.Component.translatable("message.neotab.tab_pinned")
                        : net.minecraft.network.chat.Component.translatable("message.neotab.tab_unpinned");
                    this.minecraft.player.displayClientMessage(msg, true);
                }
            }
        } else {
            neotab$wasRightClickDown = false;
        }

        // 检测左键点击翻页箭头
        boolean leftDown = this.minecraft.options.keyAttack.isDown();
        if (leftDown && !neotab$wasLeftClickDown) {
            int bl = com.poso.neotab.client.NeoTabClientState.getTabBoundsLeft();
            if (bl != -1 && com.poso.neotab.client.NeoTabClientState.getTotalPages() > 1) {
                double mx = this.minecraft.mouseHandler.xpos()
                        * this.minecraft.getWindow().getGuiScaledWidth()
                        / this.minecraft.getWindow().getScreenWidth();
                double my = this.minecraft.mouseHandler.ypos()
                        * this.minecraft.getWindow().getGuiScaledHeight()
                        / this.minecraft.getWindow().getScreenHeight();
                com.poso.neotab.client.NeoTabClientState.handlePageArrowClick(mx, my);
            }
        }
        neotab$wasLeftClickDown = leftDown;
        
        boolean shouldHide = !this.minecraft.options.keyPlayerList.isDown()
            && !com.poso.neotab.client.NeoTabClientState.isTabPinned()
            || this.minecraft.isLocalServer()
            && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1
            && objective == null
            && !forcedVisibleByNeoTab
            && !com.poso.neotab.client.NeoTabClientState.isTabPinned();

        if (shouldHide) {
            this.tabList.setVisible(false);
        } else {
            this.tabList.setVisible(true);
            this.tabList.render(guiGraphics, guiGraphics.guiWidth(), scoreboard, objective);
        }

        callbackInfo.cancel();
    }

    /**
     * 判断当前 TAB 是否有 NeoTab 下发的有效内容。
     *
     * <p>这里不依赖字符串模板本身，而是读取客户端当前真正收到的
     * header/footer 文本。这样既兼容配置变更，也不会误判未同步状态。</p>
     */
    private boolean neotab$hasNeoTabContent() {
        PlayerTabOverlayAccessor accessor = (PlayerTabOverlayAccessor) this.tabList;
        return neotab$hasText(accessor.neotab$getHeader()) || neotab$hasText(accessor.neotab$getFooter());
    }

    /**
     * 判断 Component 是否包含可见文本内容。
     * 使用 equals(Component.empty()) 替代 getString().isBlank()，避免触发完整序列化。
     */
    private boolean neotab$hasText(Component component) {
        return component != null && !component.equals(net.minecraft.network.chat.Component.empty());
    }
}
