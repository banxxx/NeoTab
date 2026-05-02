package com.poso.neotab.mixin.client;

import com.poso.neotab.client.widget.NoCountMultiLineEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 针对 {@link NoCountMultiLineEditBox} 的渲染定制：
 * <ul>
 *   <li>去除文字阴影</li>
 *   <li>加深文字颜色（原版 0xFFE0E0E0 浅灰 → 0xFF2C2C2C 深灰）</li>
 *   <li>将选区背景色改为莫奈绿（原版纯蓝 → 半透明绿）</li>
 * </ul>
 */
@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin {

    /** 输入文字颜色：深灰，替换原版的浅灰白 0xFFE0E0E0 */
    private static final int TEXT_COLOR = 0xFF2C2C2C;

    /** 莫奈绿选区背景色：半透明，替换原版的纯蓝 0xFF0000FF */
    private static final int SELECTION_COLOR = 0x806B9E7A;

    /**
     * 拦截文字渲染：去除阴影 + 替换颜色。
     */
    @Redirect(
        method = "renderContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"
        )
    )
    private int neotab$drawStringNoShadow(GuiGraphics guiGraphics,
                                          Font font, String text, int x, int y, int color) {
        if (!((Object) this instanceof NoCountMultiLineEditBox)) {
            return guiGraphics.drawString(font, text, x, y, color);
        }
        // 替换文字颜色（原版 color 参数为 -2039584 即 0xFFE0E0E0 浅灰白）
        // 光标闪烁字符 "_" 使用单独的颜色常量，不在此替换范围内，保持原样
        int actualColor = (color == -2039584) ? TEXT_COLOR : color;
        // +1 补偿无阴影时返回值比有阴影少 1px 的差值，避免光标闪烁时后半段文字跳动
        return guiGraphics.drawString(font, text, x, y, actualColor, false) + 1;
    }

    /**
     * 拦截选区背景渲染：将原版纯蓝改为莫奈绿。
     * 选区高亮由私有方法 renderHighlight 负责，在其中拦截 fill 调用。
     */
    @Redirect(
        method = "renderHighlight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
        )
    )
    private void neotab$fillSelectionColor(GuiGraphics guiGraphics,
                                           RenderType renderType, int x1, int y1, int x2, int y2, int color) {
        if (!((Object) this instanceof NoCountMultiLineEditBox)) {
            guiGraphics.fill(renderType, x1, y1, x2, y2, color);
            return;
        }
        guiGraphics.fill(renderType, x1, y1, x2, y2, SELECTION_COLOR);
    }
}
