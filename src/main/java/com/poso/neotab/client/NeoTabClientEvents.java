package com.poso.neotab.client;

import com.poso.neotab.NeoTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NeoTab 客户端事件处理。
 * 
 * <p>处理客户端相关的游戏事件，包括：</p>
 * <ul>
 *     <li>客户端 Tick 事件 - 处理快捷键和状态更新</li>
 *     <li>键盘输入事件 - TAB+左右箭头翻页功能</li>
 *     <li>GUI 渲染事件 - 强制显示玩家列表（单人模式支持）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)
public final class NeoTabClientEvents {
    
    /** 上一帧右键是否已按下，用于边沿检测防止连续触发。 */
    private static boolean wasRightClickDown = false;
    /** 上一帧左键是否已按下，用于翻页点击边沿检测。 */
    private static boolean wasLeftClickDown = false;
    
    private NeoTabClientEvents() {
    }
    
    /**
     * 客户端 Tick 事件处理。
     * 
     * <p>从 NeoForge 1.21.1 移植适配：</p>
     * <ul>
     *     <li>ClientTickEvent.Post → TickEvent.ClientTickEvent</li>
     *     <li>添加 phase 检查确保只在 END 阶段执行</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 只在 tick 结束阶段处理，避免重复执行
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            try {
                // 客户端状态管理tick处理
                com.poso.neotab.client.NeoTabClientState.onClientTick(minecraft);
                
                // 处理快捷键输入（TAB+左右箭头翻页）
                handleKeyboardInput(minecraft);
            } catch (Exception e) {
                NeoTab.LOGGER.error("Error during client tick processing: {}", e.getMessage());
            }
        }
    }
    
    /**
     * GUI 渲染后事件处理 - 强制显示玩家列表。
     * 
     * <p>修正单人世界里按 Tab 不显示玩家列表的问题。</p>
     * <p>原版逻辑在"本地服务器 + 在线人数 <= 1"时会直接隐藏玩家列表。
     * 对于 NeoTab 来说，即使只有一个玩家，也需要显示玩家列表以便测试和使用。</p>
     */
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 只在游戏中处理（不在暂停菜单等界面）
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        
        boolean tabKeyDown = minecraft.options.keyPlayerList.isDown();
        
        // 检测 Tab+右键 切换固定状态
        if (tabKeyDown && minecraft.options.keyUse.isDown()) {
            if (!wasRightClickDown) {
                boolean pinned = NeoTabClientState.toggleTabPinned();
                wasRightClickDown = true;
                if (minecraft.player != null) {
                    Component msg = pinned
                        ? Component.literal("Tab列表已固定")
                        : Component.literal("Tab列表已取消固定");
                    minecraft.player.displayClientMessage(msg, true);
                }
            }
        } else {
            wasRightClickDown = false;
        }

        // 检测左键点击翻页箭头
        boolean leftDown = minecraft.options.keyAttack.isDown();
        if (leftDown && !wasLeftClickDown) {
            int bl = NeoTabClientState.getTabBoundsLeft();
            if (bl != -1 && NeoTabClientState.getTotalPages() > 1) {
                double mx = minecraft.mouseHandler.xpos()
                        * minecraft.getWindow().getGuiScaledWidth()
                        / minecraft.getWindow().getScreenWidth();
                double my = minecraft.mouseHandler.ypos()
                        * minecraft.getWindow().getGuiScaledHeight()
                        / minecraft.getWindow().getScreenHeight();
                NeoTabClientState.handlePageArrowClick(mx, my);
            }
        }
        wasLeftClickDown = leftDown;
        
        // 判断是否应该显示Tab列表
        boolean tabPinned = NeoTabClientState.isTabPinned();
        boolean shouldShow = tabKeyDown || tabPinned;

        // 强制显示玩家列表
        if (shouldShow) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            minecraft.gui.getTabList().setVisible(true);
            minecraft.gui.getTabList().render(guiGraphics, 
                minecraft.getWindow().getGuiScaledWidth(), 
                minecraft.level.getScoreboard(), 
                null);
        } else {
            minecraft.gui.getTabList().setVisible(false);
        }
    }
    
    /**
     * 处理快捷键输入。
     * 
     * <p>实现 TAB+左右箭头键的翻页功能：</p>
     * <ul>
     *     <li>TAB+左箭头：上一页</li>
     *     <li>TAB+右箭头：下一页</li>
     * </ul>
     */
    private static void handleKeyboardInput(Minecraft minecraft) {
        // 检查TAB键是否按下（玩家列表显示时）
        if (minecraft.options.keyPlayerList.isDown()) {
            // 检查左箭头键
            if (minecraft.options.keyLeft.consumeClick()) {
                if (com.poso.neotab.client.NeoTabClientState.prevPage()) {
                    // 翻页成功，可以添加音效或其他反馈
                }
            }
            // 检查右箭头键
            else if (minecraft.options.keyRight.consumeClick()) {
                if (com.poso.neotab.client.NeoTabClientState.nextPage()) {
                    // 翻页成功，可以添加音效或其他反馈
                }
            }
        }
    }
}