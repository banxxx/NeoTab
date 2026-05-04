package com.poso.neotab.mixin.client;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor Mixin for {@link MultiLineEditBox}，暴露私有字段供子类使用。
 */
@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {

    @Accessor("textField")
    MultilineTextField neotab$getTextField();

    @Accessor("focusedTime")
    long neotab$getFocusedTime();

    @Accessor("font")
    net.minecraft.client.gui.Font neotab$getFont();

    @Accessor("placeholder")
    net.minecraft.network.chat.Component neotab$getPlaceholder();
}