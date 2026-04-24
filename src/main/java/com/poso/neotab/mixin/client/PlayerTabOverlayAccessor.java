package com.poso.neotab.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问原版 PlayerTabOverlay 的私有 header/footer 字段。
 *
 * <p>原版没有提供 getter，但我们需要知道当前 TAB 是否已经存在
 * 由服务端下发的顶部/底部文本，这样才能在单人测试环境中决定
 * 是否强制允许 Tab 列表显示。</p>
 */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor("header")
    Component neotab$getHeader();

    @Accessor("footer")
    Component neotab$getFooter();
}
