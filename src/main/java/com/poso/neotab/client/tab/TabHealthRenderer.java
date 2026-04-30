package com.poso.neotab.client.tab;

import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

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
 * <p>所有方法均为静态，不持有任何状态，由 {@link com.poso.neotab.mixin.client.PlayerTabOverlayMixin}
 * 在需要时调用。</p>
 */
public final class TabHealthRenderer {

    // ── 原版心形 sprite ────────────────────────────────────────────────────────
    public static final ResourceLocation HEART_FULL      = ResourceLocation.withDefaultNamespace("hud/heart/full");
    public static final ResourceLocation HEART_HALF      = ResourceLocation.withDefaultNamespace("hud/heart/half");
    public static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");

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

        if (config.healthDisplayMode() == com.poso.neotab.config.HealthDisplayMode.COMPACT) {
            // COMPACT 模式：1颗心 + 数字
            g.blitSprite(HEART_CONTAINER, startX, y, HEART_SIZE, HEART_SIZE);
            g.blitSprite(HEART_FULL,      startX, y, HEART_SIZE, HEART_SIZE);
            int numX = startX + HEART_SIZE + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
            return;
        }

        // FULL 模式
        if (maxHealth > 20f || health > 20f) {
            for (int i = 0; i < MAX_HEARTS; i++) {
                int hx = startX + i * HEART_STEP;
                g.blitSprite(HEART_CONTAINER, hx, y, HEART_SIZE, HEART_SIZE);
                g.blitSprite(HEART_FULL,      hx, y, HEART_SIZE, HEART_SIZE);
            }
            int numX = startX + HEARTS_W + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
        } else {
            int fullHearts = (int) (health / 2.0f);
            boolean hasHalf = (health % 2.0f) >= 1.0f;
            int total = Math.max(1, Math.min(MAX_HEARTS, fullHearts + (hasHalf ? 1 : 0)));

            for (int i = 0; i < total; i++) {
                int hx = startX + i * HEART_STEP;
                g.blitSprite(HEART_CONTAINER, hx, y, HEART_SIZE, HEART_SIZE);
                if (i < fullHearts) {
                    g.blitSprite(HEART_FULL, hx, y, HEART_SIZE, HEART_SIZE);
                } else if (hasHalf) {
                    g.blitSprite(HEART_HALF, hx, y, HEART_SIZE, HEART_SIZE);
                }
            }
        }
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
