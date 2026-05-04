package com.poso.neotab.client.tab;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * TAB 列表边框与翻页箭头渲染辅助类。
 *
 * <p>负责：</p>
 * <ul>
 *   <li>自定义主题的彩虹边框绘制（含动画、呼吸效果）</li>
 *   <li>翻页箭头绘制（含背景色自适应和阴影）</li>
 *   <li>颜色插值工具方法</li>
 * </ul>
 *
 * <p>所有方法均为静态，不持有任何状态。</p>
 */
public final class TabBorderRenderer {

    // ── 翻页箭头纹理 ──────────────────────────────────────────────────────────
    private static final ResourceLocation CHEVRON_LEFT  =
            new ResourceLocation("neotab", "textures/gui/chevron_left.png");
    private static final ResourceLocation CHEVRON_RIGHT =
            new ResourceLocation("neotab", "textures/gui/chevron_right.png");

    /** 纹理原始尺寸（80×128，目标渲染尺寸 PAGE_ARROW_W × PAGE_ARROW_H 的 8 倍） */
    private static final int CHEVRON_TEX_W = 80;
    private static final int CHEVRON_TEX_H = 128;

    /** 翻页箭头区域宽度（px） */
    public static final int PAGE_ARROW_W = 10;
    /** 翻页箭头区域高度（px） */
    public static final int PAGE_ARROW_H = 16;
    /** TAB 边框与内容之间的内边距（px） */
    public static final int TAB_CONTENT_PADDING = 3;

    private TabBorderRenderer() {}

    // ─────────────────────────────────────────────────────────────────────────
    // 翻页箭头
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 绘制翻页箭头（左右两侧居中）。
     *
     * <p>高分辨率 PNG blit 缩放方案：纹理 80×128，渲染到 PAGE_ARROW_W×PAGE_ARROW_H，
     * GPU 双线性过滤自动平滑，视觉效果接近矢量图。</p>
     *
     * @param g       渲染上下文
     * @param left    TAB 背景左边界（含 padding）
     * @param top     TAB 背景上边界（含 padding）
     * @param right   TAB 背景右边界（含 padding）
     * @param bottom  TAB 背景下边界（含 padding）
     * @param bgColor 当前 TAB 背景色 ARGB，Integer.MIN_VALUE 表示原版半透明黑背景
     */
    public static void drawPageArrows(GuiGraphics g, int left, int top, int right, int bottom, int bgColor) {
        int page  = NeoTabClientState.getCurrentPage();
        int total = NeoTabClientState.getTotalPages();

        boolean hasLeft  = page > 0;
        boolean hasRight = page < total - 1;
        if (!hasLeft && !hasRight) return;

        int centerY = (top + bottom) / 2;
        int arrowY  = centerY - PAGE_ARROW_H / 2;

        // ── 根据背景色亮度选择图标颜色 ────────────────────────────────────────
        float iconR, iconG, iconB;
        if (bgColor != Integer.MIN_VALUE) {
            float r  = ((bgColor >> 16) & 0xFF) / 255f;
            float gv = ((bgColor >>  8) & 0xFF) / 255f;
            float b  = ( bgColor        & 0xFF) / 255f;
            // 感知亮度公式（ITU-R BT.709）
            float luminance = 0.2126f * r + 0.7152f * gv + 0.0722f * b;
            if (luminance > 0.5f) {
                // 背景偏亮 → 图标用深色（深灰 #2A2A2A）
                iconR = 0x2A / 255f; iconG = 0x2A / 255f; iconB = 0x2A / 255f;
            } else {
                // 背景偏暗 → 图标用浅色（AE2 高光色 #C8CCD4）
                iconR = 200f/255f; iconG = 204f/255f; iconB = 212f/255f;
            }
        } else {
            // 原版主题：背景半透明，固定用浅色
            iconR = 200f/255f; iconG = 204f/255f; iconB = 212f/255f;
        }
        float iconA = 0.70f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (hasLeft) {
            int arrowX = left + TAB_CONTENT_PADDING;
            RenderSystem.setShaderColor(0f, 0f, 0f, 0.35f);
            g.blit(CHEVRON_LEFT, arrowX + 1, arrowY + 1, PAGE_ARROW_W, PAGE_ARROW_H,
                    0, 0, CHEVRON_TEX_W, CHEVRON_TEX_H, CHEVRON_TEX_W, CHEVRON_TEX_H);
            RenderSystem.setShaderColor(iconR, iconG, iconB, iconA);
            g.blit(CHEVRON_LEFT, arrowX, arrowY, PAGE_ARROW_W, PAGE_ARROW_H,
                    0, 0, CHEVRON_TEX_W, CHEVRON_TEX_H, CHEVRON_TEX_W, CHEVRON_TEX_H);
        }

        if (hasRight) {
            int arrowX = right - TAB_CONTENT_PADDING - PAGE_ARROW_W;
            RenderSystem.setShaderColor(0f, 0f, 0f, 0.35f);
            g.blit(CHEVRON_RIGHT, arrowX + 1, arrowY + 1, PAGE_ARROW_W, PAGE_ARROW_H,
                    0, 0, CHEVRON_TEX_W, CHEVRON_TEX_H, CHEVRON_TEX_W, CHEVRON_TEX_H);
            RenderSystem.setShaderColor(iconR, iconG, iconB, iconA);
            g.blit(CHEVRON_RIGHT, arrowX, arrowY, PAGE_ARROW_W, PAGE_ARROW_H,
                    0, 0, CHEVRON_TEX_W, CHEVRON_TEX_H, CHEVRON_TEX_W, CHEVRON_TEX_H);
        }

        // 恢复默认颜色，避免影响后续渲染
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 彩虹边框
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 绘制自定义主题的彩虹边框（含动画流动和呼吸效果）。
     *
     * @param guiGraphics 渲染上下文
     * @param left        边框左边界
     * @param top         边框上边界
     * @param right       边框右边界
     * @param bottom      边框下边界
     */
    public static void drawRainbowBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        com.poso.neotab.theme.CustomThemeConfig themeConfig = com.poso.neotab.theme.CustomThemeManager.get();

        java.util.List<Integer> borderColorsList = themeConfig.getBorderColors();
        int[] rainbowColors = borderColorsList.stream().mapToInt(Integer::intValue).toArray();

        if (rainbowColors.length == 0) {
            rainbowColors = new int[]{ themeConfig.getBackgroundColor() };
        }

        long currentTime = System.currentTimeMillis();
        int speed = themeConfig.getAnimationSpeed();
        long cycleDuration = speed == 1 ? 10000L : speed == 3 ? 2500L : 5000L;
        float flowOffset = themeConfig.isAnimationEnabled()
                ? (currentTime % cycleDuration) / (float) cycleDuration : 0.0F;
        float breathe = themeConfig.isAnimationEnabled()
                ? (float) (0.85F + 0.15F * Math.sin(currentTime / 1000.0))
                : 1.0F;

        int width  = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);

        int outerColor = applyBreathe(themeConfig.getBorderOuterColor(), breathe);

        // 外层边框（上下左右各一像素）
        for (int x = left - 1; x < right + 1; x++) {
            guiGraphics.fill(x, top - 1,  x + 1, top,      outerColor);
            guiGraphics.fill(x, bottom,   x + 1, bottom + 1, outerColor);
        }
        for (int y = top - 1; y < bottom + 1; y++) {
            guiGraphics.fill(left - 1, y, left,    y + 1, outerColor);
            guiGraphics.fill(right,    y, right + 1, y + 1, outerColor);
        }

        // 内层彩虹边框（上下边）
        for (int x = left; x < right; x++) {
            int color = applyBreathe(getAnimatedRainbowColor(x - left, width, rainbowColors, flowOffset), breathe);
            guiGraphics.fill(x, top,      x + 1, top + 1,    color);
            guiGraphics.fill(x, bottom - 1, x + 1, bottom,   color);
        }

        // 内层彩虹边框（左右边）
        for (int y = top; y < bottom; y++) {
            int color = applyBreathe(getAnimatedRainbowColor(y - top, height, rainbowColors, flowOffset), breathe);
            guiGraphics.fill(left,      y, left + 1,  y + 1, color);
            guiGraphics.fill(right - 1, y, right,     y + 1, color);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 颜色工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 根据位置和时间偏移获取动画彩虹颜色（流动效果）。
     *
     * @param position   当前位置
     * @param total      总长度
     * @param colors     颜色数组
     * @param flowOffset 流动偏移量（0.0-1.0）
     * @return 插值后的颜色值
     */
    public static int getAnimatedRainbowColor(int position, int total, int[] colors, float flowOffset) {
        float progress = ((float) position / (float) total + flowOffset) % 1.0F;
        int colorCount = colors.length;

        float scaledProgress = progress * colorCount;
        int index1 = (int) scaledProgress % colorCount;
        int index2 = (index1 + 1) % colorCount;
        float localProgress = scaledProgress - (int) scaledProgress;

        int color1 = colors[index1];
        int color2 = colors[index2];

        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF,
            g1 = (color1 >>  8) & 0xFF, b1 =  color1        & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF,
            g2 = (color2 >>  8) & 0xFF, b2 =  color2        & 0xFF;

        int a = (int) (a1 + (a2 - a1) * localProgress);
        int r = (int) (r1 + (r2 - r1) * localProgress);
        int g = (int) (g1 + (g2 - g1) * localProgress);
        int b = (int) (b1 + (b2 - b1) * localProgress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 根据位置获取插值后的彩虹颜色（静态，无流动效果）。
     *
     * @param position 当前位置
     * @param total    总长度
     * @param colors   颜色数组
     * @return 插值后的颜色值
     */
    public static int getInterpolatedRainbowColor(int position, int total, int[] colors) {
        return getAnimatedRainbowColor(position, total, colors, 0f);
    }

    /**
     * 应用呼吸效果到颜色（调整 RGB 亮度，保留原始 alpha）。
     *
     * @param color   原始颜色 ARGB
     * @param breathe 呼吸因子（0.85-1.0）
     * @return 应用呼吸效果后的颜色
     */
    public static int applyBreathe(int color, float breathe) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * breathe);
        int g = (int) (((color >>  8) & 0xFF) * breathe);
        int b = (int) (( color        & 0xFF) * breathe);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 将颜色变暗。
     *
     * @param color  原始颜色
     * @param factor 变暗因子（0.0-1.0，越小越暗）
     * @return 变暗后的颜色（alpha 固定为 0xFF）
     */
    public static int darkenColor(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >>  8) & 0xFF) * factor);
        int b = (int) (( color        & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
