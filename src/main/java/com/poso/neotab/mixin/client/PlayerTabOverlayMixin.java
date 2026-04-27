package com.poso.neotab.mixin.client;

import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 在启用"更好的延迟显示"、"在线时长显示"或"血量显示"时，处理右对齐显示和列宽度调整。
 *
 * <p>布局（从左到右）：[用户名] [NAME_GAP] [血量区（固定宽度）] [SECTION_GAP] [延迟] [SECTION_GAP] [在线时长] [原版信号图标或留白]</p>
 *
 * <p>血量区宽度在所有玩家之间统一，保证心形图标始终对齐：
 * 若服务器上有任何玩家 maxHealth > 20，则所有玩家都使用"10颗心 + 间距 + 数字"的宽度，
 * 数字区域宽度取所有玩家中最宽的那个。</p>
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Shadow @Final private Minecraft minecraft;

    // ── 原版心形 sprite ────────────────────────────────────────────────────────
    private static final ResourceLocation HEART_FULL      = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF      = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");

    // ── 尺寸常量 ───────────────────────────────────────────────────────────────
    /** 心形图标尺寸（px） */
    private static final int HEART_SIZE   = 8;
    /** 相邻心形之间的步进（px） */
    private static final int HEART_STEP   = 7;
    /** 最多显示的心数 */
    private static final int MAX_HEARTS   = 10;
    /** 10 颗心的总像素宽度 */
    private static final int HEARTS_W     = MAX_HEARTS * HEART_STEP + (HEART_SIZE - HEART_STEP);
    /** 原版信号图标宽度（px） */
    private static final int PING_ICON_W  = 13;
    /** 各段之间的间距（px） */
    private static final int SECTION_GAP  = 4;
    /** 血量图标与用户名之间的最小保留间距（px），只加在列宽里 */
    private static final int NAME_GAP     = 8;

    // ── 列宽缓存 ───────────────────────────────────────────────────────────────
    private int     cachedRequiredSpace       = -1;
    /**
     * 缓存的统一血量区宽度。
     * 所有玩家共用同一个值，保证心形对齐。
     * -1 表示需要重新计算。
     */
    private int     cachedHealthAreaW         = -1;
    /** 上次计算时所有玩家中最大的血量数字位数，用于检测位数变化触发重算。 */
    private int     lastMaxHealthDigits       = -1;
    private int     lastPlayerCount           = -1;
    private boolean lastBetterPingEnabled     = false;
    private boolean lastOnlineDurationEnabled = false;
    private boolean lastHealthDisplayEnabled  = false;

    // ─────────────────────────────────────────────────────────────────────────
    // 渲染注入
    // ─────────────────────────────────────────────────────────────────────────

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void neotab$renderRightAlignedInfo(
            GuiGraphics g, int width, int x, int y,
            PlayerInfo playerInfo, CallbackInfo ci) {

        var config = NeoTabClientState.getCurrentConfig();
        Font font  = this.minecraft.font;

        boolean hasPing     = config.betterPingEnabled();
        boolean hasDuration = config.onlineDurationEnabled();
        boolean hasHealth   = config.healthDisplayEnabled();

        // betterPing 开启时取消原版信号图标渲染
        if (hasPing) ci.cancel();

        if (!hasPing && !hasDuration && !hasHealth) return;

        UUID pid = playerInfo.getProfile().getId();

        // ── 准备各段文本 ──────────────────────────────────────────────────────
        String pingText      = "";
        String durationText  = "";
        int    pingColor     = 0xFFFFFF;
        int    durationColor = ChatFormatting.AQUA.getColor() != null
                               ? ChatFormatting.AQUA.getColor() : 0x55FFFF;

        if (hasPing) {
            int latency = playerInfo.getLatency();
            pingText  = latency + "ms";
            pingColor = getPingColor(latency);
        }
        if (hasDuration) {
            durationText = NeoTabClientState.getOnlineDuration(pid);
        }

        float health    = hasHealth ? NeoTabClientState.getPlayerHealth(pid)    : 0f;
        float maxHealth = hasHealth ? NeoTabClientState.getPlayerMaxHealth(pid) : 0f;

        // ── 计算各段宽度（血量区使用统一宽度保证对齐）────────────────────────
        // 如果缓存失效则重新计算（通常已由 @ModifyConstant 更新过）
        int healthAreaW = hasHealth ? getOrCalcHealthAreaW(font) : 0;
        int pingW       = hasPing     ? font.width(pingText)     : 0;
        int durationW   = hasDuration ? font.width(durationText) : 0;

        int gapHealthRight = hasHealth && (hasPing || hasDuration) ? SECTION_GAP : 0;
        int gapPingDur     = hasPing   && hasDuration              ? SECTION_GAP : 0;

        int totalW = healthAreaW + gapHealthRight + pingW + gapPingDur + durationW;

        // ── 确定右边界 ────────────────────────────────────────────────────────
        int rightEdge = x + width;
        if (!hasPing) {
            rightEdge -= PING_ICON_W;
        }

        // ── 渲染 ──────────────────────────────────────────────────────────────
        int cx = rightEdge - totalW;

        if (hasHealth) {
            renderHealth(g, font, cx, y, health, maxHealth, healthAreaW);
            cx += healthAreaW + gapHealthRight;
        }

        if (hasPing) {
            g.drawString(font, pingText, cx, y, pingColor, false);
            cx += pingW + gapPingDur;
        }

        if (hasDuration) {
            g.drawString(font, durationText, cx, y, durationColor, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 列宽调整
    // ─────────────────────────────────────────────────────────────────────────

    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int neotab$adjustPingIconSpace(int original) {
        var config = NeoTabClientState.getCurrentConfig();

        if (!config.betterPingEnabled() && !config.onlineDurationEnabled() && !config.healthDisplayEnabled()) {
            return original;
        }

        boolean configChanged =
                (config.betterPingEnabled()     != lastBetterPingEnabled)
             || (config.onlineDurationEnabled() != lastOnlineDurationEnabled)
             || (config.healthDisplayEnabled()  != lastHealthDisplayEnabled);

        int currentCount = 0;
        if (this.minecraft.getConnection() != null) {
            currentCount = this.minecraft.getConnection().getOnlinePlayers().size();
        }

        if (configChanged || currentCount != lastPlayerCount || cachedRequiredSpace == -1) {
            Font font = this.minecraft.font;
            // 先计算统一血量区宽度，再计算列宽
            if (config.healthDisplayEnabled()) {
                cachedHealthAreaW  = calcUnifiedHealthAreaW(font);
                lastMaxHealthDigits = currentMaxHealthDigits();
            } else {
                cachedHealthAreaW   = -1;
                lastMaxHealthDigits = -1;
            }
            cachedRequiredSpace       = calcRequiredSpace(font, config);
            lastBetterPingEnabled     = config.betterPingEnabled();
            lastOnlineDurationEnabled = config.onlineDurationEnabled();
            lastHealthDisplayEnabled  = config.healthDisplayEnabled();
            lastPlayerCount           = currentCount;
        } else if (config.healthDisplayEnabled()) {
            // 配置和玩家数量未变，但检查血量数字位数是否变化
            int digits = currentMaxHealthDigits();
            if (digits != lastMaxHealthDigits) {
                Font font = this.minecraft.font;
                cachedHealthAreaW   = calcUnifiedHealthAreaW(font);
                cachedRequiredSpace = calcRequiredSpace(font, config);
                lastMaxHealthDigits = digits;
            }
        }

        return cachedRequiredSpace;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助：统一血量区宽度
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
     */
    private int calcUnifiedHealthAreaW(Font font) {
        boolean anyOverLimit = false;
        int maxNumW = 0;

        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                UUID pid = pi.getProfile().getId();
                float mh = NeoTabClientState.getPlayerMaxHealth(pid);
                float h  = NeoTabClientState.getPlayerHealth(pid);
                if (mh > 20f || h > 20f) {
                    anyOverLimit = true;
                    // 只用当前血量决定数字宽度，动态跟随实际显示的数字位数
                    int numW = font.width("x" + (int) h);
                    if (numW > maxNumW) maxNumW = numW;
                }
            }
        }

        if (anyOverLimit) {
            // 超出上限模式：10颗心 + 间距 + 数字区域
            // 数字区域宽度 = max(当前最宽数字, "99" 的宽度)，保底 2 位数，动态扩展到 3/4 位数
            int minNumW = font.width("x99");
            return HEARTS_W + SECTION_GAP + Math.max(maxNumW, minNumW);
        } else {
            // 正常模式：固定 10 颗心的宽度
            return HEARTS_W;
        }
    }

    /** 获取缓存的统一血量区宽度，若未缓存则立即计算。 */
    private int getOrCalcHealthAreaW(Font font) {
        if (cachedHealthAreaW == -1) {
            cachedHealthAreaW = calcUnifiedHealthAreaW(font);
        }
        return cachedHealthAreaW;
    }

    /**
     * 计算当前所有玩家中最大的血量数字位数。
     * 用于检测位数变化，触发列宽重算。
     */
    private int currentMaxHealthDigits() {
        int maxDigits = 2; // 保底 2 位
        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                float h = NeoTabClientState.getPlayerHealth(pi.getProfile().getId());
                if (h > 20f) {
                    int digits = String.valueOf((int) h).length();
                    if (digits > maxDigits) maxDigits = digits;
                }
            }
        }
        return maxDigits;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助：列宽计算
    // ─────────────────────────────────────────────────────────────────────────

    private int calcRequiredSpace(Font font, com.poso.neotab.config.TabConfig config) {
        int maxW = 0;

        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                maxW = Math.max(maxW, calcRowWidth(font, config, pi.getProfile().getId()));
            }
        }

        if (maxW == 0) {
            maxW = calcRowWidthFallback(font, config);
        }

        // 血量显示开启时额外加 NAME_GAP，把列撑宽，让用户名和血量之间有足够间距
        int extra = config.healthDisplayEnabled() ? NAME_GAP : 0;
        return maxW + extra + 10;
    }

    /** 计算单个玩家行的内容宽度（使用统一血量区宽度）。 */
    private int calcRowWidth(Font font, com.poso.neotab.config.TabConfig config, UUID pid) {
        int w = 0;

        if (config.healthDisplayEnabled()) {
            // 使用统一血量区宽度，所有玩家相同
            w += cachedHealthAreaW > 0 ? cachedHealthAreaW : calcUnifiedHealthAreaW(font);
            if (config.betterPingEnabled() || config.onlineDurationEnabled()) {
                w += SECTION_GAP;
            }
        }

        if (config.betterPingEnabled()) {
            w += font.width(getLatencyText(pid));
            if (config.onlineDurationEnabled()) w += SECTION_GAP;
        }

        if (config.onlineDurationEnabled()) {
            // betterPing 未开启时，渲染侧会扣掉 PING_ICON_W 给信号图标，
            // 列宽里也需要把这 13px 算进去，否则时长会贴着用户名
            if (!config.betterPingEnabled()) {
                w += PING_ICON_W;
            }
            w += font.width(NeoTabClientState.getOnlineDuration(pid));
        }

        return w;
    }

    /** 当没有玩家数据时的保守宽度估计。 */
    private int calcRowWidthFallback(Font font, com.poso.neotab.config.TabConfig config) {
        int w = 0;
        if (config.healthDisplayEnabled()) {
            w += HEARTS_W; // 保守估计：正常模式
            if (config.betterPingEnabled() || config.onlineDurationEnabled()) w += SECTION_GAP;
        }
        if (config.betterPingEnabled()) {
            w += font.width("999ms");
            if (config.onlineDurationEnabled()) w += SECTION_GAP;
        }
        if (config.onlineDurationEnabled()) {
            if (!config.betterPingEnabled()) {
                w += PING_ICON_W;
            }
            w += font.width("99d23h");
        }
        return w;
    }

    /** 获取指定玩家的延迟文本（用于宽度计算）。 */
    private String getLatencyText(UUID pid) {
        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                if (pi.getProfile().getId().equals(pid)) {
                    return pi.getLatency() + "ms";
                }
            }
        }
        return "999ms";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助：血量渲染
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 渲染血量区域，使用统一的 healthAreaW 保证心形对齐。
     *
     * <p>正常模式（maxHealth ≤ 20）：心形图标从 startX 开始，支持半心，数字区域留空。</p>
     * <p>超出上限模式（maxHealth > 20）：10 颗满心 + 固定偏移处的整数数字。</p>
     *
     * @param healthAreaW 统一血量区宽度（由 calcUnifiedHealthAreaW 计算）
     */
    private void renderHealth(GuiGraphics g, Font font, int startX, int y,
                               float health, float maxHealth, int healthAreaW) {
        if (maxHealth > 20f || health > 20f) {
            // 超出上限：10 颗满心，心形从 startX 开始
            for (int i = 0; i < MAX_HEARTS; i++) {
                int hx = startX + i * HEART_STEP;
                g.blitSprite(HEART_CONTAINER, hx, y, HEART_SIZE, HEART_SIZE);
                g.blitSprite(HEART_FULL,      hx, y, HEART_SIZE, HEART_SIZE);
            }
            // 数字画在固定偏移处（HEARTS_W + SECTION_GAP），与心形右对齐
            int numX = startX + HEARTS_W + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
        } else {
            // 正常模式：心形从 startX 开始，数字区域留空（已在 healthAreaW 中预留）
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
    // 辅助：延迟颜色
    // ─────────────────────────────────────────────────────────────────────────

    private int getPingColor(int latency) {
        if (latency < 100)      return ChatFormatting.GREEN.getColor()  != null ? ChatFormatting.GREEN.getColor()  : 0x55FF55;
        else if (latency < 200) return ChatFormatting.YELLOW.getColor() != null ? ChatFormatting.YELLOW.getColor() : 0xFFFF55;
        else if (latency < 350) return ChatFormatting.GOLD.getColor()   != null ? ChatFormatting.GOLD.getColor()   : 0xFFAA00;
        else                    return ChatFormatting.RED.getColor()    != null ? ChatFormatting.RED.getColor()    : 0xFF5555;
    }
}
