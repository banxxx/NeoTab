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
    private static final int SCROLLBAR_WIDTH = 4;  // 滚动条宽度
    private static final int SCROLLBAR_PADDING = 2;  // 滚动条与边框的间距
    private static final int SCROLLBAR_TOTAL_WIDTH = SCROLLBAR_WIDTH + SCROLLBAR_PADDING * 2;  // 滚动条总宽度（包括左右padding）
    
    private final int fullWidth;  // 完整宽度（包括滚动条区域）
    private boolean isDraggingScrollbar = false;  // 是否正在拖动滚动条

    public NoCountMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message) {
        // 为文本区域减去滚动条宽度，避免文字被遮挡
        super(font, x, y, width - SCROLLBAR_TOTAL_WIDTH, height, placeholder, message);
        this.fullWidth = width;
    }
    
    /**
     * 检查鼠标是否在滚动条区域内。
     */
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (!this.scrollbarVisible()) {
            return false;
        }
        int scrollbarX = this.getX() + this.fullWidth - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
            && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverScrollbar(mouseX, mouseY)) {
            // 在滚动条区域点击，处理滚动条拖动
            isDraggingScrollbar = true;
            // 计算点击位置对应的滚动偏移
            int verticalPadding = 2;
            int availableHeight = this.getHeight() - verticalPadding * 2;
            int scrollbarHeight = Math.max(32, Math.min(
                (int) ((float) (availableHeight * availableHeight) / (float) (this.getInnerHeight() + 4)), 
                availableHeight));
            int relativeY = (int) mouseY - this.getY() - verticalPadding;
            int maxScroll = this.getMaxScrollAmount();
            if (maxScroll > 0) {
                int newScroll = (int) ((relativeY - scrollbarHeight / 2.0) * maxScroll / (availableHeight - scrollbarHeight));
                this.setScrollAmount(Math.max(0, Math.min(newScroll, maxScroll)));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            // 拖动滚动条
            int verticalPadding = 2;
            int availableHeight = this.getHeight() - verticalPadding * 2;
            int scrollbarHeight = Math.max(32, Math.min(
                (int) ((float) (availableHeight * availableHeight) / (float) (this.getInnerHeight() + 4)), 
                availableHeight));
            int relativeY = (int) mouseY - this.getY() - verticalPadding;
            int maxScroll = this.getMaxScrollAmount();
            if (maxScroll > 0) {
                int newScroll = (int) ((relativeY - scrollbarHeight / 2.0) * maxScroll / (availableHeight - scrollbarHeight));
                this.setScrollAmount(Math.max(0, Math.min(newScroll, maxScroll)));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 重写背景渲染，用 AE2 风格凹陷背景替代原版黑色 sprite。
     * 同时为滚动条区域预留空间。
     */
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        int x = getX(), y = getY(), w = fullWidth, h = getHeight();
        
        // HTML原型中的输入框样式：白色背景，中等边框
        int bgColor = 0xFFFFFFFF;  // 白色背景 (--bg-input)
        int borderColor = 0xFFC8C0AD;  // 中等边框色 (--border-medium)
        
        // 绘制边框
        guiGraphics.fill(x, y, x + w, y + 1, borderColor);  // 顶部边框
        guiGraphics.fill(x, y, x + 1, y + h, borderColor);  // 左侧边框
        guiGraphics.fill(x + w - 1, y, x + w, y + h, borderColor);  // 右侧边框
        guiGraphics.fill(x, y + h - 1, x + w, y + h, borderColor);  // 底部边框
        
        // 内部填充白色背景
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        // 如果有滚动条，在滚动条区域绘制一个稍浅的背景，避免文字被遮挡
        if (this.scrollbarVisible()) {
            int scrollbarAreaX = x + w - SCROLLBAR_TOTAL_WIDTH;
            guiGraphics.fill(scrollbarAreaX, y + 1, x + w - 1, y + h - 1, 0xFFF3EFE4);  // 浅米色背景
        }
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
        if (this.scrollbarVisible()) {
            int verticalPadding = 2;  // 滚动条与顶部和底部的间距
            int availableHeight = this.getHeight() - verticalPadding * 2;  // 可用高度（减去上下间距）
            
            int scrollbarHeight = Math.max(32, Math.min(
                (int) ((float) (availableHeight * availableHeight) / (float) (this.getInnerHeight() + 4)), 
                availableHeight));
            
            int scrollbarY = Math.max(
                this.getY() + verticalPadding, 
                (int) this.scrollAmount() * (availableHeight - scrollbarHeight) / Math.max(1, this.getMaxScrollAmount()) + this.getY() + verticalPadding);
            
            RenderSystem.enableBlend();
            // 滚动条位置：使用fullWidth计算，距离右边框2px
            int scrollbarX = this.getX() + this.fullWidth - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
            guiGraphics.blitSprite(SCROLLER_SPRITE, scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight);
            RenderSystem.disableBlend();
        }
    }
}