package com.poso.neotab.mixin.client;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor Mixin for {@link MultilineTextField}，暴露选区的起止索引。
 *
 * <p>{@code MultilineTextField.getSelected()} 返回的是 {@code protected} 的
 * {@code StringView} record，外部包无法直接引用其方法。
 * 这里直接暴露底层的 {@code cursor} 和 {@code selectCursor} 字段，
 * 在外部自行计算选区范围。</p>
 */
@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {

    @Accessor("cursor")
    int neotab$getCursor();

    @Accessor("selectCursor")
    int neotab$getSelectCursor();
}
