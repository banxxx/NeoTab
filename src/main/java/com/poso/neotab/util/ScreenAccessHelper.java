package com.poso.neotab.util;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

/**
 * Screen 访问辅助类。
 * 
 * <p>直接使用 Screen 的公共方法，避免反射问题。</p>
 */
public class ScreenAccessHelper {
    
    /**
     * 添加控件到 Screen（同时添加到事件监听和渲染列表）。
     * 
     * @param screen 目标 Screen
     * @param widget 要添加的控件
     * @param <T> 控件类型
     * @return 添加的控件
     */
    @SuppressWarnings("unchecked")
    public static <T extends GuiEventListener & NarratableEntry> T addWidget(Screen screen, T widget) {
        // 直接调用 Screen 的 addRenderableWidget 方法（这是一个 protected 方法，但我们的 NeoTabConfigScreen 继承了 Screen）
        // 由于我们无法直接访问 protected 方法，我们需要让 NeoTabConfigScreen 提供一个公共包装方法
        
        // 如果是 AbstractWidget，需要添加到渲染列表
        if (widget instanceof AbstractWidget) {
            // 使用类型转换技巧：Screen 的 addRenderableWidget 返回的就是传入的 widget
            return (T) addRenderableWidgetInternal(screen, (AbstractWidget) widget);
        } else {
            // 对于非 AbstractWidget，只添加到事件监听列表
            return (T) addWidgetInternal(screen, widget);
        }
    }
    
    /**
     * 内部方法：添加可渲染的 widget。
     * 这个方法会被 NeoTabConfigScreen 的公共方法调用。
     */
    private static <T extends AbstractWidget> T addRenderableWidgetInternal(Screen screen, T widget) {
        // 这里我们需要一个不同的策略
        // 让我们直接在 NeoTabConfigScreen 中暴露这个方法
        if (screen instanceof com.poso.neotab.client.screen.NeoTabConfigScreen) {
            return ((com.poso.neotab.client.screen.NeoTabConfigScreen) screen).addRenderableWidgetPublic(widget);
        }
        throw new IllegalArgumentException("Screen must be NeoTabConfigScreen");
    }
    
    /**
     * 内部方法：添加 widget 到事件监听列表。
     */
    private static <T extends GuiEventListener & NarratableEntry> T addWidgetInternal(Screen screen, T widget) {
        if (screen instanceof com.poso.neotab.client.screen.NeoTabConfigScreen) {
            return ((com.poso.neotab.client.screen.NeoTabConfigScreen) screen).addWidgetPublic(widget);
        }
        throw new IllegalArgumentException("Screen must be NeoTabConfigScreen");
    }
    
    /**
     * 从 Screen 移除控件。
     * 
     * @param screen 目标 Screen
     * @param widget 要移除的控件
     */
    public static void removeWidget(Screen screen, GuiEventListener widget) {
        if (screen instanceof com.poso.neotab.client.screen.NeoTabConfigScreen) {
            ((com.poso.neotab.client.screen.NeoTabConfigScreen) screen).removeWidgetPublic(widget);
        }
    }
}
