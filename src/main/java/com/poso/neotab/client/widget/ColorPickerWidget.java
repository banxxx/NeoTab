package com.poso.neotab.client.widget;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/**
 * 完整的颜色选择器组件。
 * 
 * <p>支持 HSV 色彩空间选择和十六进制输入。</p>
 */
public class ColorPickerWidget extends AbstractWidget {
    private static final int HUE_BAR_WIDTH = 12;  // 色相条宽度
    private static final int SV_PANEL_SIZE = 100;  // 面板尺寸
    private static final int PREVIEW_SIZE = 20;    // 预览框大小
    private static final int HEX_INPUT_WIDTH = 70; // 输入框宽度
    private static final int COMPONENT_GAP = 6;    // 组件间距
    private static final int ALPHA_BAR_WIDTH = 12; // 透明度条宽度（垂直）
    private static final int BORDER_PADDING = 8;   // 外边框内边距
    private static final int HEX_LABEL_WIDTH = 30; // HEX标签宽度
    // 布局：[SV面板][间距][色相条][间距][预览框+Alpha条]
    private static final int TOTAL_WIDTH = SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP + Math.max(PREVIEW_SIZE, ALPHA_BAR_WIDTH) + BORDER_PADDING * 2;
    private static final int TOTAL_HEIGHT = SV_PANEL_SIZE + COMPONENT_GAP + 20 + BORDER_PADDING * 2; // HSV面板 + 间距 + HEX输入框 + 边距
    
    private final Font font;
    private final Consumer<Integer> onColorChanged;
    private EditBox hexInput;
    
    // HSV 颜色值 (0-1)
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private int alpha = 255;
    
    // 当前颜色 (ARGB)
    private int currentColor;
    
    // 拖拽状态
    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    
    // 防止循环更新的标志
    private boolean updatingFromMouse = false;
    
    // 纹理缓存
    private DynamicTexture svPanelTexture;
    private ResourceLocation svPanelLocation;
    private float cachedHue = -1.0f;  // 缓存的色相值
    
    // 色相条纹理缓存（永久，只初始化一次）
    private DynamicTexture hueBarTexture;
    private ResourceLocation hueBarLocation;
    
    // 棋盘格纹理缓存（永久，只初始化一次）
    private DynamicTexture checkerboardTexture;
    private ResourceLocation checkerboardLocation;
    
    // Alpha条纹理缓存
    private DynamicTexture alphaBarTexture;
    private ResourceLocation alphaBarLocation;
    
    // 拖拽时的优化
    private long lastTextureUpdateTime = 0;
    private static final long TEXTURE_UPDATE_INTERVAL_MS = 16;  // 约60fps的更新频率
    
    public ColorPickerWidget(int x, int y, Font font, int initialColor, Consumer<Integer> onColorChanged) {
        super(x, y, TOTAL_WIDTH, TOTAL_HEIGHT, Component.empty());
        this.font = font;
        this.onColorChanged = onColorChanged;
        this.currentColor = initialColor;
        
        // 从 ARGB 转换为 HSV
        rgbToHsv(initialColor);
        
        // 创建十六进制输入框
        this.hexInput = new EditBox(font, 0, 0, HEX_INPUT_WIDTH, 18, Component.empty());
        this.hexInput.setMaxLength(9); // #AARRGGBB
        this.hexInput.setValue(colorToHex(initialColor));
        this.hexInput.setResponder(this::onHexInputChanged);
        this.hexInput.setEditable(true);  // 确保输入框可编辑
        this.hexInput.setCanLoseFocus(true);  // 允许失去焦点
        
        // 更新输入框位置
        updateHexInputPosition();
        
        // 初始化纹理缓存（永久纹理）
        initHueBarTexture();
        initCheckerboardTexture();
        initAlphaBarTexture();
        
        // 初始化 SV 面板纹理
        updateSVPanelTexture();
    }
    
    @Override
    public void setX(int x) {
        super.setX(x);
        updateHexInputPosition();
    }
    
    @Override
    public void setY(int y) {
        super.setY(y);
        updateHexInputPosition();
    }
    
    /**
     * 更新十六进制输入框的位置
     */
    private void updateHexInputPosition() {
        if (hexInput != null) {
            // HEX输入框放在HSV面板下方
            int inputX = getX() + BORDER_PADDING + HEX_LABEL_WIDTH;
            int inputY = getY() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP;
            hexInput.setX(inputX);
            hexInput.setY(inputY);
        }
    }
    
    /**
     * 初始化色相条纹理（永久，只调用一次）
     */
    private void initHueBarTexture() {
        NativeImage image = new NativeImage(1, SV_PANEL_SIZE, false);
        
        for (int y = 0; y < SV_PANEL_SIZE; y++) {
            float h = (float) y / SV_PANEL_SIZE;
            int color = hsvToRgb(h, 1.0f, 1.0f, 255);
            // NativeImage 使用 ABGR 格式
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            image.setPixelRGBA(0, y, (a << 24) | (b << 16) | (g << 8) | r);
        }
        
        hueBarTexture = new DynamicTexture(image);
        hueBarLocation = Minecraft.getInstance().getTextureManager()
            .register("neotab_color_picker_hue", hueBarTexture);
    }
    
    /**
     * 初始化棋盘格纹理（永久，只调用一次）
     */
    private void initCheckerboardTexture() {
        int squareSize = 4;
        int textureSize = squareSize * 2;  // 2x2 的棋盘格图案
        NativeImage image = new NativeImage(textureSize, textureSize, false);
        
        for (int y = 0; y < textureSize; y++) {
            for (int x = 0; x < textureSize; x++) {
                boolean isLight = ((x / squareSize) + (y / squareSize)) % 2 == 0;
                int color = isLight ? 0xFFCCCCCC : 0xFF999999;
                // NativeImage 使用 ABGR 格式
                int a = (color >> 24) & 0xFF;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        
        checkerboardTexture = new DynamicTexture(image);
        checkerboardLocation = Minecraft.getInstance().getTextureManager()
            .register("neotab_color_picker_checker", checkerboardTexture);
    }
    
    /**
     * 初始化透明度条纹理（垂直）
     */
    private void initAlphaBarTexture() {
        int barHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP; // 透明度条高度 = 色相条高度 - 预览框高度 - 间距
        NativeImage image = new NativeImage(1, barHeight, false);
        
        // 创建从不透明到透明的渐变（从上到下：255→0）
        // 使用非线性渐变（平方函数），让透明度在较低值时变化更快
        for (int y = 0; y < barHeight; y++) {
            float ratio = (float) y / barHeight;
            // 使用平方函数：ratio^2，让透明度变化更平缓，棋盘格在更低位置才明显
            float adjustedRatio = ratio * ratio;
            int alpha = 255 - (int) (adjustedRatio * 255);  // 从255到0
            int rgb = currentColor & 0x00FFFFFF;
            int color = (alpha << 24) | rgb;
            
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            image.setPixelRGBA(0, y, (a << 24) | (b << 16) | (g << 8) | r);
        }
        
        alphaBarTexture = new DynamicTexture(image);
        alphaBarLocation = Minecraft.getInstance().getTextureManager()
            .register("neotab_color_picker_alpha", alphaBarTexture);
    }
    
    /**
     * 更新透明度条纹理（当颜色改变时调用）
     */
    private void updateAlphaBarTexture() {
        if (alphaBarTexture != null) {
            alphaBarTexture.close();
        }
        
        int barHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP;
        NativeImage image = new NativeImage(1, barHeight, false);
        
        // 使用当前颜色的RGB值（不含alpha）
        int rgb = currentColor & 0x00FFFFFF;
        
        for (int y = 0; y < barHeight; y++) {
            float ratio = (float) y / barHeight;
            // 使用平方函数：ratio^2
            float adjustedRatio = ratio * ratio;
            int alpha = 255 - (int) (adjustedRatio * 255);  // 从255到0
            int color = (alpha << 24) | rgb;
            
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            image.setPixelRGBA(0, y, (a << 24) | (b << 16) | (g << 8) | r);
        }
        
        alphaBarTexture = new DynamicTexture(image);
        alphaBarLocation = Minecraft.getInstance().getTextureManager()
            .register("neotab_color_picker_alpha", alphaBarTexture);
    }
    
    /**
     * 更新 SV 面板纹理（只在色相改变时调用）
     */
    private void updateSVPanelTexture() {
        // 如果色相没有改变，不需要重新生成纹理
        if (Math.abs(cachedHue - hue) < 0.001f && svPanelTexture != null) {
            return;
        }
        
        cachedHue = hue;
        
        // 释放旧纹理
        if (svPanelTexture != null) {
            svPanelTexture.close();
        }
        
        // 创建新纹理
        NativeImage image = new NativeImage(SV_PANEL_SIZE, SV_PANEL_SIZE, false);
        
        for (int y = 0; y < SV_PANEL_SIZE; y++) {
            for (int x = 0; x < SV_PANEL_SIZE; x++) {
                float s = (float) x / SV_PANEL_SIZE;
                float v = 1.0f - (float) y / SV_PANEL_SIZE;
                int color = hsvToRgb(hue, s, v, 255);
                // NativeImage 使用 ABGR 格式
                int a = (color >> 24) & 0xFF;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        
        svPanelTexture = new DynamicTexture(image);
        svPanelLocation = Minecraft.getInstance().getTextureManager()
            .register("neotab_color_picker_sv", svPanelTexture);
    }
    
    /**
     * 释放纹理资源
     */
    public void releaseTexture() {
        if (svPanelTexture != null) {
            svPanelTexture.close();
            svPanelTexture = null;
        }
        if (hueBarTexture != null) {
            hueBarTexture.close();
            hueBarTexture = null;
        }
        if (checkerboardTexture != null) {
            checkerboardTexture.close();
            checkerboardTexture = null;
        }
        if (alphaBarTexture != null) {
            alphaBarTexture.close();
            alphaBarTexture = null;
        }
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制外边框背景 - 使用AE风格的内容区背景色
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF9AA0AC);
        // 使用AE风格的轮廓色
        graphics.renderOutline(getX(), getY(), width, height, 0xFF3A3A3A);
        
        // 绘制 SV 面板
        renderSVPanel(graphics);
        
        // 绘制色相条
        renderHueBar(graphics);
        
        // 绘制预览框
        renderPreview(graphics);
        
        // 绘制透明度条
        renderAlphaBar(graphics);
        
        // 绘制 HEX 标签
        graphics.drawString(font, "HEX", getX() + BORDER_PADDING, 
            getY() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP + 5, 
            0xFFFFFFFF, false);
        
        // 绘制十六进制输入框
        hexInput.render(graphics, mouseX, mouseY, partialTick);
        
        // 绘制 SV 面板的选择指示器
        renderSVIndicator(graphics);
        
        // 绘制色相条的选择指示器
        renderHueIndicator(graphics);
        
        // 绘制透明度条的选择指示器
        renderAlphaIndicator(graphics);
    }
    
    private void renderSVPanel(GuiGraphics graphics) {
        int panelX = getX() + BORDER_PADDING;
        int panelY = getY() + BORDER_PADDING;
        
        // 使用缓存的纹理绘制（性能优化）
        if (svPanelLocation != null) {
            RenderSystem.setShaderTexture(0, svPanelLocation);
            graphics.blit(svPanelLocation, panelX, panelY, 0, 0, SV_PANEL_SIZE, SV_PANEL_SIZE, SV_PANEL_SIZE, SV_PANEL_SIZE);
        }
        
        // 绘制边框
        graphics.renderOutline(panelX, panelY, SV_PANEL_SIZE, SV_PANEL_SIZE, 0xFF888888);
    }
    
    private void renderHueBar(GuiGraphics graphics) {
        int barX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY() + BORDER_PADDING;
        
        // 使用缓存的纹理绘制色相条（性能优化）
        if (hueBarLocation != null) {
            RenderSystem.setShaderTexture(0, hueBarLocation);
            // 将1像素宽的纹理拉伸到 HUE_BAR_WIDTH 宽度
            graphics.blit(hueBarLocation, barX, barY, 0, 0, HUE_BAR_WIDTH, SV_PANEL_SIZE, 1, SV_PANEL_SIZE);
        }
        
        // 绘制边框
        graphics.renderOutline(barX, barY, HUE_BAR_WIDTH, SV_PANEL_SIZE, 0xFF888888);
    }
    
    private void renderAlphaBar(GuiGraphics graphics) {
        int barX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int barY = getY() + BORDER_PADDING + PREVIEW_SIZE + COMPONENT_GAP; // 预览框下方
        int barHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP; // 与色相条底部对齐
        
        // 绘制棋盘格背景
        renderCheckerboard(graphics, barX, barY, ALPHA_BAR_WIDTH, barHeight);
        
        // 启用混合模式，使透明度条的透明部分能显示出棋盘格
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 使用缓存的纹理绘制透明度条（垂直）
        if (alphaBarLocation != null) {
            RenderSystem.setShaderTexture(0, alphaBarLocation);
            // 将1像素宽的纹理拉伸到 ALPHA_BAR_WIDTH 宽度
            graphics.blit(alphaBarLocation, barX, barY, 0, 0, ALPHA_BAR_WIDTH, barHeight, 1, barHeight);
        }
        
        // 恢复渲染状态
        RenderSystem.disableBlend();
        
        // 绘制边框
        graphics.renderOutline(barX, barY, ALPHA_BAR_WIDTH, barHeight, 0xFF888888);
    }
    
    private void renderPreview(GuiGraphics graphics) {
        int previewX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int previewY = getY() + BORDER_PADDING;
        
        // 绘制棋盘格背景（用于显示透明度）
        renderCheckerboard(graphics, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE);
        
        // 绘制当前颜色
        graphics.fill(previewX, previewY, previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE, currentColor);
        
        // 绘制边框
        graphics.renderOutline(previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, 0xFF888888);
    }
    
    private void renderRGBValues(GuiGraphics graphics) {
        int textX = getX() + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int textY = getY() + PREVIEW_SIZE + 24;  // 调整位置
        
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;
        int a = (currentColor >> 24) & 0xFF;
        
        // 使用更紧凑的布局
        graphics.drawString(font, "R:" + r, textX, textY, 0xFFFFFFFF, false);
        graphics.drawString(font, "G:" + g, textX, textY + 8, 0xFFFFFFFF, false);
        graphics.drawString(font, "B:" + b, textX, textY + 16, 0xFFFFFFFF, false);
        graphics.drawString(font, "A:" + a, textX, textY + 24, 0xFFFFFFFF, false);
    }
    
    private void renderSVIndicator(GuiGraphics graphics) {
        int panelX = getX() + BORDER_PADDING;
        int panelY = getY() + BORDER_PADDING;
        float centerX = panelX + (saturation * SV_PANEL_SIZE);
        float centerY = panelY + ((1.0f - value) * SV_PANEL_SIZE);
        
        // 使用 OpenGL 绘制平滑的圆形双环光标（缩小尺寸）
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        
        // 外圈：黑色圆环（半径 4，线宽 1.2）- 缩小
        drawSmoothCircleGL(poseStack, centerX, centerY, 4.0f, 1.2f, 0.0f, 0.0f, 0.0f, 1.0f);
        // 次层：白色圆环（半径 3，线宽 1.0）- 缩小
        drawSmoothCircleGL(poseStack, centerX, centerY, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        
        poseStack.popPose();
    }
    
    /**
     * 使用 OpenGL 绘制平滑的圆环
     * 
     * @param poseStack 变换矩阵栈
     * @param centerX 圆心 X 坐标
     * @param centerY 圆心 Y 坐标
     * @param radius 半径
     * @param lineWidth 线宽
     * @param r 红色分量 (0-1)
     * @param g 绿色分量 (0-1)
     * @param b 蓝色分量 (0-1)
     * @param a 透明度 (0-1)
     */
    private void drawSmoothCircleGL(PoseStack poseStack, float centerX, float centerY, 
                                     float radius, float lineWidth, 
                                     float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        
        // 启用混合和平滑
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // 绘制圆环（使用三角形条带）
        int segments = 64; // 圆的分段数，越多越平滑
        
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            // 外圈顶点
            float outerX = centerX + cos * (radius + lineWidth / 2);
            float outerY = centerY + sin * (radius + lineWidth / 2);
            buffer.addVertex(matrix, outerX, outerY, 0).setColor(r, g, b, a);
            
            // 内圈顶点
            float innerX = centerX + cos * (radius - lineWidth / 2);
            float innerY = centerY + sin * (radius - lineWidth / 2);
            buffer.addVertex(matrix, innerX, innerY, 0).setColor(r, g, b, a);
        }
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        RenderSystem.disableBlend();
    }
    
    private void renderHueIndicator(GuiGraphics graphics) {
        int barX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY() + BORDER_PADDING;
        int indicatorY = barY + (int) (hue * SV_PANEL_SIZE);
        
        // 绘制水平指示器（缩小尺寸）
        graphics.fill(barX - 1, indicatorY - 1, barX, indicatorY + 2, 0xFFFFFFFF);
        graphics.fill(barX + HUE_BAR_WIDTH, indicatorY - 1, barX + HUE_BAR_WIDTH + 1, indicatorY + 2, 0xFFFFFFFF);
        
        // 黑色边框
        graphics.fill(barX - 2, indicatorY - 2, barX - 1, indicatorY + 3, 0xFF000000);
        graphics.fill(barX + HUE_BAR_WIDTH + 1, indicatorY - 2, barX + HUE_BAR_WIDTH + 2, indicatorY + 3, 0xFF000000);
    }
    
    private void renderAlphaIndicator(GuiGraphics graphics) {
        int barX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int barY = getY() + BORDER_PADDING + PREVIEW_SIZE + COMPONENT_GAP;
        int barHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP;
        
        // 计算指示器位置（使用平方函数匹配渐变）
        float alphaRatio = (float) alpha / 255.0f;  // 0.0 到 1.0
        float adjustedRatio = 1.0f - alphaRatio;  // 反转
        float positionRatio = adjustedRatio * adjustedRatio;  // 平方
        int indicatorY = barY + (int) (positionRatio * barHeight);
        
        // 绘制水平指示器（与色相条样式一致）
        graphics.fill(barX - 1, indicatorY - 1, barX, indicatorY + 2, 0xFFFFFFFF);
        graphics.fill(barX + ALPHA_BAR_WIDTH, indicatorY - 1, barX + ALPHA_BAR_WIDTH + 1, indicatorY + 2, 0xFFFFFFFF);
        
        // 黑色边框
        graphics.fill(barX - 2, indicatorY - 2, barX - 1, indicatorY + 3, 0xFF000000);
        graphics.fill(barX + ALPHA_BAR_WIDTH + 1, indicatorY - 2, barX + ALPHA_BAR_WIDTH + 2, indicatorY + 3, 0xFF000000);
    }
    
    private void renderCheckerboard(GuiGraphics graphics, int x, int y, int width, int height) {
        // 使用缓存的纹理绘制棋盘格（性能优化）
        if (checkerboardLocation != null) {
            RenderSystem.setShaderTexture(0, checkerboardLocation);
            // 平铺纹理填充整个区域
            int squareSize = 4;
            int textureSize = squareSize * 2;
            for (int dy = 0; dy < height; dy += textureSize) {
                for (int dx = 0; dx < width; dx += textureSize) {
                    int drawWidth = Math.min(textureSize, width - dx);
                    int drawHeight = Math.min(textureSize, height - dy);
                    graphics.blit(checkerboardLocation, x + dx, y + dy, 0, 0, drawWidth, drawHeight, textureSize, textureSize);
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        // 检查组件是否可见
        if (!this.visible) return false;
        
        // 检查是否点击了十六进制输入框
        if (hexInput.mouseClicked(mouseX, mouseY, button)) {
            // 设置输入框为焦点，确保能接收键盘输入
            hexInput.setFocused(true);
            return true;
        }
        
        // 如果点击了其他地方，取消输入框的焦点
        hexInput.setFocused(false);
        
        // 检查是否点击了 SV 面板
        int panelX = getX() + BORDER_PADDING;
        int panelY = getY() + BORDER_PADDING;
        if (mouseX >= panelX && mouseX < panelX + SV_PANEL_SIZE &&
            mouseY >= panelY && mouseY < panelY + SV_PANEL_SIZE) {
            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        
        // 检查是否点击了色相条
        int barX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY() + BORDER_PADDING;
        if (mouseX >= barX && mouseX < barX + HUE_BAR_WIDTH &&
            mouseY >= barY && mouseY < barY + SV_PANEL_SIZE) {
            draggingHue = true;
            updateHueFromMouse(mouseY);
            return true;
        }
        
        // 检查是否点击了透明度条
        int alphaBarX = getX() + BORDER_PADDING + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int alphaBarY = getY() + BORDER_PADDING + PREVIEW_SIZE + COMPONENT_GAP;
        int alphaBarHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP;
        if (mouseX >= alphaBarX && mouseX < alphaBarX + ALPHA_BAR_WIDTH &&
            mouseY >= alphaBarY && mouseY < alphaBarY + alphaBarHeight) {
            draggingAlpha = true;
            updateAlphaFromMouse(mouseY);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSV) {
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        
        if (draggingHue) {
            updateHueFromMouse(mouseY);
            return true;
        }
        
        if (draggingAlpha) {
            updateAlphaFromMouse(mouseY);  // 修复：透明度条是垂直的，应该使用Y坐标
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 拖拽结束时，确保纹理已更新到最新状态
        if (draggingHue && Math.abs(cachedHue - hue) > 0.001f) {
            updateSVPanelTexture();
        }
        
        draggingSV = false;
        draggingHue = false;
        draggingAlpha = false;
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return hexInput.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return hexInput.charTyped(codePoint, modifiers);
    }
    
    private void updateSVFromMouse(double mouseX, double mouseY) {
        int panelX = getX() + BORDER_PADDING;
        int panelY = getY() + BORDER_PADDING;
        
        saturation = Mth.clamp((float) (mouseX - panelX) / SV_PANEL_SIZE, 0.0f, 1.0f);
        value = 1.0f - Mth.clamp((float) (mouseY - panelY) / SV_PANEL_SIZE, 0.0f, 1.0f);
        
        // 设置标志，防止循环更新
        updatingFromMouse = true;
        updateColor();
        updatingFromMouse = false;
    }
    
    private void updateHueFromMouse(double mouseY) {
        int barY = getY() + BORDER_PADDING;
        
        // 计算相对位置（0.0 到 1.0）
        float relativeY = (float) (mouseY - barY) / SV_PANEL_SIZE;
        
        // 严格限制在 [0.0, 1.0) 范围内，注意不包括1.0
        // 这样可以避免色相值回绕的问题
        float newHue = Mth.clamp(relativeY, 0.0f, 0.999f);
        
        // 设置标志，防止循环更新
        updatingFromMouse = true;
        
        // 只有色相改变时才更新
        if (Math.abs(newHue - hue) > 0.001f) {
            hue = newHue;
            
            // 限制纹理更新频率（约60fps）
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTextureUpdateTime >= TEXTURE_UPDATE_INTERVAL_MS) {
                updateSVPanelTexture();
                lastTextureUpdateTime = currentTime;
            }
        }
        
        updateColor();
        
        // 清除标志
        updatingFromMouse = false;
    }
    
    private void updateAlphaFromMouse(double mouseY) {
        int barY = getY() + BORDER_PADDING + PREVIEW_SIZE + COMPONENT_GAP;
        int barHeight = SV_PANEL_SIZE - PREVIEW_SIZE - COMPONENT_GAP;
        
        float ratio = Mth.clamp((float) (mouseY - barY) / barHeight, 0.0f, 1.0f);
        // 使用平方根反转平方函数：sqrt(ratio)
        float adjustedRatio = (float) Math.sqrt(ratio);
        alpha = (int) ((1.0f - adjustedRatio) * 255);  // 从上到下：255→0
        
        // 设置标志，防止循环更新
        updatingFromMouse = true;
        updateColor();
        updatingFromMouse = false;
    }
    
    private void updateColor() {
        currentColor = hsvToRgb(hue, saturation, value, alpha);
        hexInput.setValue(colorToHex(currentColor));
        
        // 更新透明度条纹理（因为颜色的RGB部分改变了）
        updateAlphaBarTexture();
        
        if (onColorChanged != null) {
            onColorChanged.accept(currentColor);
        }
    }

    private void onHexInputChanged(String hexStr) {
        // ✅ 关键修复：鼠标拖动时是代码主动调用 setValue()，不应反向回算 HSV
        // 否则 HSV→RGB 的整数截断会在 hue≈1.0 处造成 rgbToHsv 回算得 hue=0
        if (updatingFromMouse) return;

        try {
            int color = hexToColor(hexStr);
            currentColor = color;
            float oldHue = hue;
            rgbToHsv(color);
            if (Math.abs(oldHue - hue) > 0.001f) {
                updateSVPanelTexture();
            }
            updateAlphaBarTexture();
            if (onColorChanged != null) {
                onColorChanged.accept(currentColor);
            }
        } catch (Exception e) {
            // 无效的十六进制输入，忽略
        }
    }
    
    /**
     * RGB 转 HSV
     */
    private void rgbToHsv(int argb) {
        alpha = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        
        // 计算亮度
        value = max;
        
        // 当亮度很低时（接近黑色），保持当前的色相和饱和度，避免不稳定
        if (max < 0.01f) {
            // 接近黑色，保持当前的hue和saturation不变
            // 只更新value
            return;
        }
        
        // 计算饱和度
        saturation = delta / max;
        
        // 当饱和度很低时（接近灰色/白色），保持当前的色相，避免不稳定
        if (saturation < 0.01f) {
            // 接近灰色或白色，保持当前的hue不变
            return;
        }
        
        // 计算色相
        if (delta == 0) {
            // 理论上不会到这里，因为上面已经检查了saturation
            hue = 0;
        } else if (max == rf) {
            hue = ((gf - bf) / delta) / 6.0f;
            if (hue < 0) hue += 1.0f;
        } else if (max == gf) {
            hue = (2.0f + (bf - rf) / delta) / 6.0f;
        } else {
            hue = (4.0f + (rf - gf) / delta) / 6.0f;
        }
        
        // 确保色相值在 [0.0, 0.999f] 范围内，避免回绕问题
        hue = Mth.clamp(hue, 0.0f, 0.999f);
    }
    
    /**
     * HSV 转 RGB
     */
    private int hsvToRgb(float h, float s, float v, int a) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
            default: r = g = b = 0; break;
        }
        
        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);
        
        return (a << 24) | (ri << 16) | (gi << 8) | bi;
    }
    
    /**
     * 颜色转十六进制字符串
     */
    private String colorToHex(int argb) {
        return String.format("#%08X", argb);
    }
    
    /**
     * 十六进制字符串转颜色
     */
    private int hexToColor(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        
        if (cleaned.length() == 6) {
            // #RRGGBB -> #FFRRGGBB
            return (int) Long.parseLong("FF" + cleaned, 16);
        } else if (cleaned.length() == 8) {
            // #AARRGGBB
            return (int) Long.parseLong(cleaned, 16);
        }
        
        throw new IllegalArgumentException("Invalid hex color: " + hex);
    }
    
    public int getColor() {
        return currentColor;
    }
    
    public void setColor(int color) {
        // 如果正在从鼠标更新，不要重新计算HSV（防止循环更新）
        if (updatingFromMouse) {
            return;
        }
        
        this.currentColor = color;
        float oldHue = hue;
        rgbToHsv(color);
        hexInput.setValue(colorToHex(color));
        
        // 如果色相改变了，更新纹理
        if (Math.abs(oldHue - hue) > 0.001f) {
            updateSVPanelTexture();
        }
        
        // 更新透明度条纹理
        updateAlphaBarTexture();
    }
    
    public EditBox getHexInput() {
        return hexInput;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, 
                   Component.literal("Color Picker: " + colorToHex(currentColor)));
    }
}
