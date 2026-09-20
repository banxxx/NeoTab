package com.poso.neotab.client.tab;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.UUID;

/**
 * TAB 列表血量区域渲染辅助类。
 *
 * <p>负责：</p>
 * <ul>
 *   <li>计算所有玩家共用的统一血量区宽度（保证心形对齐）</li>
 *   <li>渲染单个玩家的血量图标和数字</li>
 *   <li>计算延迟文本颜色</li>
 * </ul>
 *
 * <p>除心形合批所需的临时矩阵引用（仅渲染线程、begin/end 成对使用）外，
 * 所有方法均为静态，由 {@link com.poso.neotab.mixin.client.PlayerTabOverlayMixin}
 * 在需要时调用。</p>
 */
public final class TabHealthRenderer {

    // ── 尺寸常量（与 Mixin 中保持一致）────────────────────────────────────────
    public static final int HEART_SIZE  = 8;
    public static final int HEART_STEP  = 7;
    public static final int MAX_HEARTS  = 10;
    public static final int HEARTS_W    = MAX_HEARTS * HEART_STEP + (HEART_SIZE - HEART_STEP);
    public static final int SECTION_GAP = 4;

    private TabHealthRenderer() {}

    // ─────────────────────────────────────────────────────────────────────────
    // 血量区宽度计算
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 计算所有玩家共用的统一血量区宽度。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>若所有玩家 maxHealth ≤ 20：血量区 = 10颗心的宽度（正常模式，最多10颗）</li>
     *   <li>若任意玩家 maxHealth > 20：血量区 = 10颗心 + SECTION_GAP + 最大数字宽度</li>
     * </ul>
     * <p>这样所有玩家的心形起始位置相同，视觉上完全对齐。</p>
     *
     * @param font       客户端字体
     * @param onlinePlayers 当前在线玩家集合
     */
    public static int calcUnifiedHealthAreaW(Font font, Collection<PlayerInfo> onlinePlayers) {
        var config = NeoTabClientState.getCurrentConfig();

        // COMPACT 模式：1颗心 + 间距 + 数字，宽度固定
        if (config.healthDisplayMode() == com.poso.neotab.config.HealthDisplayMode.COMPACT) {
            int numW = font.width("x999"); // 保守估计 3 位数
            for (var pi : onlinePlayers) {
                float h = NeoTabClientState.getPlayerHealth(pi.getProfile().getId());
                int w = font.width("x" + (int) h);
                if (w > numW) numW = w;
            }
            return HEART_SIZE + SECTION_GAP + numW;
        }

        // FULL 模式
        boolean anyOverLimit = false;
        int maxNumW = 0;

        for (var pi : onlinePlayers) {
            UUID pid = pi.getProfile().getId();
            float mh = NeoTabClientState.getPlayerMaxHealth(pid);
            float h  = NeoTabClientState.getPlayerHealth(pid);
            if (mh > 20f || h > 20f) {
                anyOverLimit = true;
                int numW = font.width("x" + (int) h);
                if (numW > maxNumW) maxNumW = numW;
            }
        }

        if (anyOverLimit) {
            int minNumW = font.width("x99");
            return HEARTS_W + SECTION_GAP + Math.max(maxNumW, minNumW);
        } else {
            return HEARTS_W;
        }
    }

    /**
     * 计算当前所有玩家中最大的血量数字位数。
     * 用于检测位数变化，触发列宽重算。
     *
     * @param onlinePlayers 当前在线玩家集合
     */
    public static int currentMaxHealthDigits(Collection<PlayerInfo> onlinePlayers) {
        int maxDigits = 2; // 保底 2 位
        for (var pi : onlinePlayers) {
            float h = NeoTabClientState.getPlayerHealth(pi.getProfile().getId());
            if (h > 20f) {
                int digits = String.valueOf((int) h).length();
                if (digits > maxDigits) maxDigits = digits;
            }
        }
        return maxDigits;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 血量渲染
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 渲染血量区域，使用统一的 healthAreaW 保证心形对齐。
     *
     * <p>正常模式（maxHealth ≤ 20）：心形图标从 startX 开始，支持半心，数字区域留空。</p>
     * <p>超出上限模式（maxHealth > 20）：10 颗满心 + 固定偏移处的整数数字。</p>
     *
     * @param g           渲染上下文
     * @param font        客户端字体
     * @param startX      血量区起始 X 坐标
     * @param y           行 Y 坐标
     * @param health      当前血量
     * @param maxHealth   最大血量
     * @param healthAreaW 统一血量区宽度（由 calcUnifiedHealthAreaW 计算）
     */
    public static void renderHealth(GuiGraphics g, Font font, int startX, int y,
                                    float health, float maxHealth, int healthAreaW) {
        var config = NeoTabClientState.getCurrentConfig();

        // 方案C：同一行内的所有心形合并为一次顶点提交。
        // GuiGraphics.blit 底层（innerBlit，反编译确认）每调用一次就完整执行
        // setShaderTexture + setShader + begin + 4顶点 + drawWithShader（独立上传+绘制），
        // 每帧数百颗心形即数百次状态切换与绘制调用；合并后每行只剩 1 次。
        // 顶点写入顺序、UV 计算均严格复刻 innerBlit，覆盖关系与视觉效果不变。

        if (config.healthDisplayMode() == com.poso.neotab.config.HealthDisplayMode.COMPACT) {
            // COMPACT 模式：1颗心 + 数字
            BufferBuilder buf = beginHeartBatch(g);
            addHeartQuad(buf, HEART_EMPTY_CONTAINER_U, startX, y);  // 空容器
            addHeartQuad(buf, HEART_FULL_U, startX, y);             // 满心
            endHeartBatch(buf);
            int numX = startX + HEART_SIZE + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
            return;
        }

        // FULL 模式
        if (maxHealth > 20f || health > 20f) {
            BufferBuilder buf = beginHeartBatch(g);
            for (int i = 0; i < MAX_HEARTS; i++) {
                int hx = startX + i * HEART_STEP;
                addHeartQuad(buf, HEART_EMPTY_CONTAINER_U, hx, y);  // 空容器
                addHeartQuad(buf, HEART_FULL_U, hx, y);             // 满心
            }
            endHeartBatch(buf);
            int numX = startX + HEARTS_W + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
        } else {
            int fullHearts = (int) (health / 2.0f);
            boolean hasHalf = (health % 2.0f) >= 1.0f;
            int total = Math.max(1, Math.min(MAX_HEARTS, fullHearts + (hasHalf ? 1 : 0)));

            BufferBuilder buf = beginHeartBatch(g);
            for (int i = 0; i < total; i++) {
                int hx = startX + i * HEART_STEP;
                addHeartQuad(buf, HEART_EMPTY_CONTAINER_U, hx, y);  // 空容器
                if (i < fullHearts) {
                    addHeartQuad(buf, HEART_FULL_U, hx, y);         // 满心
                } else if (hasHalf) {
                    addHeartQuad(buf, HEART_HALF_U, hx, y);         // 半心
                }
            }
            endHeartBatch(buf);
        }
    }

    // ── 原版 gui_icons.png 纹理位置 ───────────────────────────────────────────
    private static final ResourceLocation GUI_ICONS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");

    // gui_icons.png 中心形图标的 UV 坐标（原版 1.20.1 PlayerTabOverlay 中的常量值）
    // 每颗心 9x9 像素，在 256x256 的纹理图中
    private static final int HEART_EMPTY_CONTAINER_U = 16;  // 空心容器
    private static final int HEART_FULL_U             = 52;  // 满心
    private static final int HEART_HALF_U             = 61;  // 半心
    private static final int HEART_V                  = 0;   // V 坐标（第一行）
    private static final int HEART_TEX_W              = 9;
    private static final int HEART_TEX_H              = 9;

    /**
     * 开始一批心形绘制：复刻 GuiGraphics.blit 底层 innerBlit 的状态设置
     * （绑定 gui_icons.png、position_tex 着色器、POSITION_TEX 顶点格式，z=0）。
     * 返回的 BufferBuilder 必须与 {@link #endHeartBatch} 成对使用。
     */
    private static BufferBuilder beginHeartBatch(GuiGraphics g) {
        RenderSystem.setShaderTexture(0, GUI_ICONS);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        heartPose = g.pose().last().pose();
        return buf;
    }

    private static void endHeartBatch(BufferBuilder buf) {
        heartPose = null;
        // 与 innerBlit 相同：end() 生成 RenderedBuffer 后立即上传并绘制
        BufferUploader.drawWithShader(buf.end());
    }

    /** begin~end 区间内暂存当前 GUI 矩阵，避免每颗心重复穿过 PoseStack 取矩阵。 */
    private static Matrix4f heartPose;

    /**
     * 向当前批次写入一颗 9×9 心形四边形（使用原版 gui_icons.png，支持资源包替换）。
     * 顶点顺序严格复刻 innerBlit 反编译结果：(x,y)→(x,y+9)→(x+9,y+9)→(x+9,y)，
     * UV 为 [u,u+9]×[v,v+9] 归一化到 256×256；gui 渲染开启背面剔除，绕向写反会被剔除。
     *
     * @param typeU  心形在 gui_icons.png 中的 U 坐标（空心/满心/半心）
     * @param x      屏幕 X 坐标
     * @param y      屏幕 Y 坐标
     */
    private static void addHeartQuad(BufferBuilder buf, int typeU, int x, int y) {
        float u0 = typeU * (1.0F / 256.0F);
        float u1 = (typeU + HEART_TEX_W) * (1.0F / 256.0F);
        float v0 = HEART_V * (1.0F / 256.0F);
        float v1 = (HEART_V + HEART_TEX_H) * (1.0F / 256.0F);
        Matrix4f pose = heartPose;
        buf.vertex(pose, x,               y,               0.0F).uv(u0, v0).endVertex();
        buf.vertex(pose, x,               y + HEART_TEX_H, 0.0F).uv(u0, v1).endVertex();
        buf.vertex(pose, x + HEART_TEX_W, y + HEART_TEX_H, 0.0F).uv(u1, v1).endVertex();
        buf.vertex(pose, x + HEART_TEX_W, y,               0.0F).uv(u1, v0).endVertex();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 延迟颜色
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 根据延迟值返回对应的颜色。
     *
     * @param latency 延迟（毫秒）
     * @return ARGB 颜色值
     */
    public static int getPingColor(int latency) {
        if (latency < 100)      return ChatFormatting.GREEN.getColor()  != null ? ChatFormatting.GREEN.getColor()  : 0x55FF55;
        else if (latency < 200) return ChatFormatting.YELLOW.getColor() != null ? ChatFormatting.YELLOW.getColor() : 0xFFFF55;
        else if (latency < 350) return ChatFormatting.GOLD.getColor()   != null ? ChatFormatting.GOLD.getColor()   : 0xFFAA00;
        else                    return ChatFormatting.RED.getColor()    != null ? ChatFormatting.RED.getColor()    : 0xFF5555;
    }
}
