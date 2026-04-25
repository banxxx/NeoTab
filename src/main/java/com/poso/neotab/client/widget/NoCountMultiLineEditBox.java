package com.poso.neotab.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poso.neotab.client.gui.AEStyleRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 去掉右下角字数统计的多行输入框。
 *
 * <p>保留原版 {@link MultiLineEditBox} 的文本渲染、光标、选区和滚动行为，
 * 只移除字数统计那一行额外装饰，避免影响正常的编辑体验。</p>
 *
 * <p>同时将背景替换为 AE2 风格（凹陷蓝灰色），覆盖原版的黑色 sprite 背景。</p>
 */
public class NoCountMultiLineEditBox extends MultiLineEditBox {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final int SCROLLBAR_SPACE = 6;  // 为滚动条预留的空间

    public NoCountMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message) {
        // 为滚动条预留空间，减小输入框的实际宽度
        super(font, x, y, width - SCROLLBAR_SPACE, height, placeholder, message);
    }

    /**
     * 重写背景渲染，用 AE2 风格凹陷背景替代原版黑色 sprite。
     */
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        // 凹陷边框（顶/左深色，底/右高光）
        AEStyleRenderer.drawSunkenBorder(guiGraphics, x, y, w, h, 1);
        // 内部填充 AE2 输入框背景色
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF7A8090);
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
        if (this.scrollbarVisible()) {
            int scrollbarHeight = Math.max(32, Math.min((int) ((float) (this.getHeight() * this.getHeight()) / (float) (this.getInnerHeight() + 4)), this.getHeight()));
            int scrollbarY = Math.max(this.getY(), (int) this.scrollAmount() * (this.getHeight() - scrollbarHeight) / Math.max(1, this.getMaxScrollAmount()) + this.getY());
            RenderSystem.enableBlend();
            // 滚动条宽度为4像素，位置在预留空间内
            guiGraphics.blitSprite(SCROLLER_SPRITE, this.getX() + this.getWidth() + 2, scrollbarY, 4, scrollbarHeight);
            RenderSystem.disableBlend();
        }
    }
}