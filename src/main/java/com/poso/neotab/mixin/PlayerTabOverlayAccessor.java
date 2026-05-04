package com.poso.neotab.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PlayerTabOverlay 访问器 - 访问私有字段和方法。
 * 
 * <p>从 NeoForge 1.21.1 移植到 Forge 1.20.1 的主要适配：</p>
 * <ul>
 *     <li>验证目标字段在 1.20.1 中是否存在</li>
 *     <li>更新字段名称（如果有变化）</li>
 *     <li>检查访问器方法的返回类型</li>
 * </ul>
 */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    
    /**
     * 访问TAB列表是否可见的字段。
     * 
     * <p>注意：字段名可能在不同版本间有变化，需要根据实际情况调整。</p>
     */
    @Accessor("visible")
    boolean neotab$isVisible();
    
    /**
     * 设置TAB列表是否可见。
     */
    @Accessor("visible")
    void neotab$setVisible(boolean visible);
    
    /**
     * 访问TAB列表的header文本。
     */
    @Accessor("header")
    Component neotab$getHeader();
    
    /**
     * 访问TAB列表的footer文本。
     */
    @Accessor("footer")
    Component neotab$getFooter();
}