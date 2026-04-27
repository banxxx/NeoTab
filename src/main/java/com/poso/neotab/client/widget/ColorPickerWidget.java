package com.poso.neotab.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/**
 * 完整的颜色选择器组件。
 * 
 * <p>支持 HSV 色彩空间选择和十六进制输入。</p>
 */
public class ColorPickerWidget extends AbstractWidget {
    private static final int HUE_BAR_WIDTH = 20;
    private static final int SV_PANEL_SIZE = 150;
    private static final int PREVIEW_SIZE = 30;
    private static final int HEX_INPUT_WIDTH = 80;
    private static final int COMPONENT_GAP = 8;
    private static final int TOTAL_WIDTH = SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP + PREVIEW_SIZE;
    private static final int TOTAL_HEIGHT = SV_PANEL_SIZE;
    
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
    
    public ColorPickerWidget(int x, int y, Font font, int initialColor, Consumer<Integer> onColorChanged) {
        super(x, y, TOTAL_WIDTH, TOTAL_HEIGHT, Component.empty());
        this.font = font;
        this.onColorChanged = onColorChanged;
        this.currentColor = initialColor;
        
        // 从 ARGB 转换为 HSV
        rgbToHsv(initialColor);
        
        // 创建十六进制输入框
        this.hexInput = new EditBox(font, x + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP, 
                                     y + PREVIEW_SIZE + 4, HEX_INPUT_WIDTH, 20, Component.empty());
        this.hexInput.setMaxLength(9); // #AARRGGBB
        this.hexInput.setValue(colorToHex(initialColor));
        this.hexInput.setResponder(this::onHexInputChanged);
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制 SV 面板
        renderSVPanel(graphics);
        
        // 绘制色相条
        renderHueBar(graphics);
        
        // 绘制预览框
        renderPreview(graphics);
        
        // 绘制十六进制输入框
        hexInput.render(graphics, mouseX, mouseY, partialTick);
        
        // 绘制 RGB 值
        renderRGBValues(graphics);
        
        // 绘制 SV 面板的选择指示器
        renderSVIndicator(graphics);
        
        // 绘制色相条的选择指示器
        renderHueIndicator(graphics);
    }
    
    private void renderSVPanel(GuiGraphics graphics) {
        int panelX = getX();
        int panelY = getY();
        
        // 绘制饱和度-亮度面板（从左到右：白色到纯色，从上到下：纯色到黑色）
        for (int y = 0; y < SV_PANEL_SIZE; y++) {
            for (int x = 0; x < SV_PANEL_SIZE; x++) {
                float s = (float) x / SV_PANEL_SIZE;
                float v = 1.0f - (float) y / SV_PANEL_SIZE;
                int color = hsvToRgb(hue, s, v, 255);
                graphics.fill(panelX + x, panelY + y, panelX + x + 1, panelY + y + 1, color);
            }
        }
        
        // 绘制边框
        graphics.renderOutline(panelX, panelY, SV_PANEL_SIZE, SV_PANEL_SIZE, 0xFF888888);
    }
    
    private void renderHueBar(GuiGraphics graphics) {
        int barX = getX() + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY();
        
        // 绘制色相条（从上到下：红->黄->绿->青->蓝->品红->红）
        for (int y = 0; y < SV_PANEL_SIZE; y++) {
            float h = (float) y / SV_PANEL_SIZE;
            int color = hsvToRgb(h, 1.0f, 1.0f, 255);
            graphics.fill(barX, barY + y, barX + HUE_BAR_WIDTH, barY + y + 1, color);
        }
        
        // 绘制边框
        graphics.renderOutline(barX, barY, HUE_BAR_WIDTH, SV_PANEL_SIZE, 0xFF888888);
    }
    
    private void renderPreview(GuiGraphics graphics) {
        int previewX = getX() + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int previewY = getY();
        
        // 绘制棋盘格背景（用于显示透明度）
        renderCheckerboard(graphics, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE);
        
        // 绘制当前颜色
        graphics.fill(previewX, previewY, previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE, currentColor);
        
        // 绘制边框
        graphics.renderOutline(previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, 0xFF888888);
    }
    
    private void renderRGBValues(GuiGraphics graphics) {
        int textX = getX() + SV_PANEL_SIZE + COMPONENT_GAP + HUE_BAR_WIDTH + COMPONENT_GAP;
        int textY = getY() + PREVIEW_SIZE + 28;
        
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;
        int a = (currentColor >> 24) & 0xFF;
        
        graphics.drawString(font, "R: " + r, textX, textY, 0xFFFFFFFF, false);
        graphics.drawString(font, "G: " + g, textX, textY + 10, 0xFFFFFFFF, false);
        graphics.drawString(font, "B: " + b, textX, textY + 20, 0xFFFFFFFF, false);
        graphics.drawString(font, "A: " + a, textX, textY + 30, 0xFFFFFFFF, false);
    }
    
    private void renderSVIndicator(GuiGraphics graphics) {
        int panelX = getX();
        int panelY = getY();
        int indicatorX = panelX + (int) (saturation * SV_PANEL_SIZE);
        int indicatorY = panelY + (int) ((1.0f - value) * SV_PANEL_SIZE);
        
        // 绘制十字指示器
        graphics.fill(indicatorX - 5, indicatorY, indicatorX + 5, indicatorY + 1, 0xFFFFFFFF);
        graphics.fill(indicatorX, indicatorY - 5, indicatorX + 1, indicatorY + 5, 0xFFFFFFFF);
        
        // 绘制黑色外边框
        graphics.fill(indicatorX - 6, indicatorY - 1, indicatorX - 5, indicatorY + 2, 0xFF000000);
        graphics.fill(indicatorX + 5, indicatorY - 1, indicatorX + 6, indicatorY + 2, 0xFF000000);
        graphics.fill(indicatorX - 1, indicatorY - 6, indicatorX + 2, indicatorY - 5, 0xFF000000);
        graphics.fill(indicatorX - 1, indicatorY + 5, indicatorX + 2, indicatorY + 6, 0xFF000000);
    }
    
    private void renderHueIndicator(GuiGraphics graphics) {
        int barX = getX() + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY();
        int indicatorY = barY + (int) (hue * SV_PANEL_SIZE);
        
        // 绘制水平指示器
        graphics.fill(barX - 2, indicatorY - 1, barX, indicatorY + 2, 0xFFFFFFFF);
        graphics.fill(barX + HUE_BAR_WIDTH, indicatorY - 1, barX + HUE_BAR_WIDTH + 2, indicatorY + 2, 0xFFFFFFFF);
        
        // 黑色边框
        graphics.fill(barX - 3, indicatorY - 2, barX - 2, indicatorY + 3, 0xFF000000);
        graphics.fill(barX + HUE_BAR_WIDTH + 2, indicatorY - 2, barX + HUE_BAR_WIDTH + 3, indicatorY + 3, 0xFF000000);
    }
    
    private void renderCheckerboard(GuiGraphics graphics, int x, int y, int width, int height) {
        int squareSize = 4;
        for (int dy = 0; dy < height; dy += squareSize) {
            for (int dx = 0; dx < width; dx += squareSize) {
                boolean isLight = ((dx / squareSize) + (dy / squareSize)) % 2 == 0;
                int color = isLight ? 0xFFCCCCCC : 0xFF999999;
                graphics.fill(x + dx, y + dy, 
                             x + Math.min(dx + squareSize, width), 
                             y + Math.min(dy + squareSize, height), 
                             color);
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        // 检查是否点击了十六进制输入框
        if (hexInput.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // 检查是否点击了 SV 面板
        int panelX = getX();
        int panelY = getY();
        if (mouseX >= panelX && mouseX < panelX + SV_PANEL_SIZE &&
            mouseY >= panelY && mouseY < panelY + SV_PANEL_SIZE) {
            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        
        // 检查是否点击了色相条
        int barX = getX() + SV_PANEL_SIZE + COMPONENT_GAP;
        int barY = getY();
        if (mouseX >= barX && mouseX < barX + HUE_BAR_WIDTH &&
            mouseY >= barY && mouseY < barY + SV_PANEL_SIZE) {
            draggingHue = true;
            updateHueFromMouse(mouseY);
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
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSV = false;
        draggingHue = false;
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
        int panelX = getX();
        int panelY = getY();
        
        saturation = Mth.clamp((float) (mouseX - panelX) / SV_PANEL_SIZE, 0.0f, 1.0f);
        value = 1.0f - Mth.clamp((float) (mouseY - panelY) / SV_PANEL_SIZE, 0.0f, 1.0f);
        
        updateColor();
    }
    
    private void updateHueFromMouse(double mouseY) {
        int barY = getY();
        hue = Mth.clamp((float) (mouseY - barY) / SV_PANEL_SIZE, 0.0f, 1.0f);
        updateColor();
    }
    
    private void updateColor() {
        currentColor = hsvToRgb(hue, saturation, value, alpha);
        hexInput.setValue(colorToHex(currentColor));
        
        if (onColorChanged != null) {
            onColorChanged.accept(currentColor);
        }
    }
    
    private void onHexInputChanged(String hexStr) {
        try {
            int color = hexToColor(hexStr);
            currentColor = color;
            rgbToHsv(color);
            
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
        
        // 计算色相
        if (delta == 0) {
            hue = 0;
        } else if (max == rf) {
            hue = ((gf - bf) / delta) / 6.0f;
            if (hue < 0) hue += 1.0f;
        } else if (max == gf) {
            hue = (2.0f + (bf - rf) / delta) / 6.0f;
        } else {
            hue = (4.0f + (rf - gf) / delta) / 6.0f;
        }
        
        // 计算饱和度
        saturation = max == 0 ? 0 : delta / max;
        
        // 计算亮度
        value = max;
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
        this.currentColor = color;
        rgbToHsv(color);
        hexInput.setValue(colorToHex(color));
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
