package com.poso.neotab.mixin.client;

import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.theme.TabTheme;
import com.poso.neotab.theme.TabThemeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
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
public abstract class PlayerTabOverlayMixin {
    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(
            info -> info.getGameMode() == GameType.SPECTATOR ? 1 : 0
        )
        .thenComparing(info -> net.minecraft.Optionull.mapOrDefault(info.getTeam(), PlayerTeam::getName, ""))
        .thenComparing(info -> info.getProfile().getName(), String::compareToIgnoreCase);

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private Component header;
    @Shadow @Nullable private Component footer;
    @Shadow public abstract Component getNameForDisplay(PlayerInfo playerInfo);

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
    private com.poso.neotab.config.HealthDisplayMode lastHealthDisplayMode = null;

    // ── TAB 背景边界缓存（用于绘制边框） ──────────────────────────────────────
    private int tabBackgroundLeft   = -1;
    private int tabBackgroundTop    = -1;
    private int tabBackgroundRight  = -1;
    private int tabBackgroundBottom = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // 主题背景注入
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 在原版 TAB 列表渲染完成后，叠加主题背景色。
     *
     * <p>注入点选 TAIL（原版渲染全部完成后），这样主题背景会覆盖在原版背景之上，
     * 但在玩家名和图标之下——因为原版是先画背景再画内容，我们在最后再画一层背景，
     * 实际上会盖住内容。</p>
     *
     * <p>因此改用 HEAD 注入，在原版渲染之前先画主题背景，原版内容正常叠加在上面。</p>
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void neotab$renderThemeBackground(
            GuiGraphics g, int screenWidth,
            net.minecraft.world.scores.Scoreboard scoreboard,
            net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {
        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());
        if (theme.isVanilla()) return;

        // 获取 TAB 列表的渲染区域
        // 原版在 render 方法里计算列数、列宽、总宽高，我们无法直接拿到这些值，
        // 但可以通过 @Shadow 或在 TAIL 注入时用 GuiGraphics 的 pose 矩阵获取。
        // 最简单的方式：在 HEAD 注入时，用全屏半透明矩形覆盖，
        // 但这会影响整个屏幕。更精确的做法需要 @Shadow 访问内部字段。
        // 当前实现：仅在非原版主题时，在 TAB 区域画一层背景色。
        // 由于无法在 HEAD 时知道 TAB 的精确坐标，先用全屏覆盖测试效果。
        // TODO: 后续通过 @Shadow 获取精确坐标
        if (false && theme.backgroundColor() != 0) {
            // 全屏覆盖一层主题背景色（测试用，后续精确化）
            g.fill(0, 0, screenWidth, this.minecraft.getWindow().getGuiScaledHeight(), theme.backgroundColor());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 渲染注入
    // ─────────────────────────────────────────────────────────────────────────

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")
    )
    private void neotab$redirectTabBackgroundFill(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, int color) {
        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());
        
        // 如果是自定义主题，从配置文件读取背景颜色
        if (!theme.isVanilla() && "custom".equals(theme.id()) && color == Integer.MIN_VALUE) {
            com.poso.neotab.theme.CustomThemeConfig themeConfig = com.poso.neotab.theme.CustomThemeManager.get();
            color = themeConfig.getBackgroundColor();
        } else if (!theme.isVanilla() && color == Integer.MIN_VALUE && theme.backgroundColor() != 0) {
            color = theme.backgroundColor();
        }
        
        // 捕获 TAB 背景的实际边界（用于后续绘制边框）
        // Integer.MIN_VALUE 是原版 TAB 背景的颜色标识
        if (color == Integer.MIN_VALUE || (!theme.isVanilla() && (theme.backgroundColor() != 0 || "custom".equals(theme.id())))) {
            // 更新边界：取所有背景矩形的最大范围
            if (tabBackgroundLeft == -1 || minX < tabBackgroundLeft) {
                tabBackgroundLeft = minX;
            }
            if (tabBackgroundTop == -1 || minY < tabBackgroundTop) {
                tabBackgroundTop = minY;
            }
            if (tabBackgroundRight == -1 || maxX > tabBackgroundRight) {
                tabBackgroundRight = maxX;
            }
            if (tabBackgroundBottom == -1 || maxY > tabBackgroundBottom) {
                tabBackgroundBottom = maxY;
            }
        }
        
        guiGraphics.fill(minX, minY, maxX, maxY, color);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void neotab$renderRainbowBorder(
            GuiGraphics guiGraphics, int width,
            net.minecraft.world.scores.Scoreboard scoreboard,
            net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {

        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());
        if (!"custom".equals(theme.id())) return;

        // 使用捕获的实际背景边界绘制边框
        if (tabBackgroundLeft != -1 && tabBackgroundTop != -1 && 
            tabBackgroundRight != -1 && tabBackgroundBottom != -1) {
            neotab$drawRainbowBorder(guiGraphics, tabBackgroundLeft, tabBackgroundTop, 
                                     tabBackgroundRight, tabBackgroundBottom);
        }
        
        // 重置边界缓存，为下一帧做准备
        tabBackgroundLeft = -1;
        tabBackgroundTop = -1;
        tabBackgroundRight = -1;
        tabBackgroundBottom = -1;
    }

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
             || (config.healthDisplayEnabled()  != lastHealthDisplayEnabled)
             || (config.healthDisplayMode()     != lastHealthDisplayMode);

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
            lastHealthDisplayMode     = config.healthDisplayMode();
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
        var config = NeoTabClientState.getCurrentConfig();
        // COMPACT 模式：1颗心 + 间距 + 数字，宽度固定
        if (config.healthDisplayMode() == com.poso.neotab.config.HealthDisplayMode.COMPACT) {
            int numW = font.width("x999"); // 保守估计 3 位数
            // 动态取当前最宽数字
            if (this.minecraft.getConnection() != null) {
                for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                    float h = NeoTabClientState.getPlayerHealth(pi.getProfile().getId());
                    int w = font.width("x" + (int) h);
                    if (w > numW) numW = w;
                }
            }
            return HEART_SIZE + SECTION_GAP + numW;
        }

        // FULL 模式（原有逻辑）
        boolean anyOverLimit = false;
        int maxNumW = 0;

        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                UUID pid = pi.getProfile().getId();
                float mh = NeoTabClientState.getPlayerMaxHealth(pid);
                float h  = NeoTabClientState.getPlayerHealth(pid);
                if (mh > 20f || h > 20f) {
                    anyOverLimit = true;
                    int numW = font.width("x" + (int) h);
                    if (numW > maxNumW) maxNumW = numW;
                }
            }
        }

        if (anyOverLimit) {
            int minNumW = font.width("x99");
            return HEARTS_W + SECTION_GAP + Math.max(maxNumW, minNumW);
        } else {
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

        // 血量显示开启时额外加 NAME_GAP，这部分对应渲染时 rightEdge 扣掉的 NAME_GAP
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
        var config = NeoTabClientState.getCurrentConfig();

        if (config.healthDisplayMode() == com.poso.neotab.config.HealthDisplayMode.COMPACT) {
            // COMPACT 模式：1颗心 + 数字
            g.blitSprite(HEART_CONTAINER, startX, y, HEART_SIZE, HEART_SIZE);
            g.blitSprite(HEART_FULL,      startX, y, HEART_SIZE, HEART_SIZE);
            int numX = startX + HEART_SIZE + SECTION_GAP;
            g.drawString(font, "x" + (int) health, numX, y, 0xFFFFFF, false);
            return;
        }

        // FULL 模式（原有逻辑）
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
    // 辅助：延迟颜色
    // ─────────────────────────────────────────────────────────────────────────

    private int getPingColor(int latency) {
        if (latency < 100)      return ChatFormatting.GREEN.getColor()  != null ? ChatFormatting.GREEN.getColor()  : 0x55FF55;
        else if (latency < 200) return ChatFormatting.YELLOW.getColor() != null ? ChatFormatting.YELLOW.getColor() : 0xFFFF55;
        else if (latency < 350) return ChatFormatting.GOLD.getColor()   != null ? ChatFormatting.GOLD.getColor()   : 0xFFAA00;
        else                    return ChatFormatting.RED.getColor()    != null ? ChatFormatting.RED.getColor()    : 0xFF5555;
    }

    private void neotab$drawRainbowBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        // 从自定义主题配置加载颜色
        com.poso.neotab.theme.CustomThemeConfig themeConfig = com.poso.neotab.theme.CustomThemeManager.get();
        
        // 获取边框颜色数组
        java.util.List<Integer> borderColorsList = themeConfig.getBorderColors();
        int[] rainbowColors = borderColorsList.stream().mapToInt(Integer::intValue).toArray();
        
        // 如果没有配置颜色，使用默认值
        if (rainbowColors.length == 0) {
            rainbowColors = new int[]{
                0xFFFF9999,  // 增强莫奈粉红
                0xFFFFCC99,  // 增强莫奈杏色
                0xFFFFFF99,  // 增强莫奈米黄
                0xFF99FFCC,  // 增强莫奈薄荷绿
                0xFF99DDFF,  // 增强莫奈天蓝
                0xFF9999FF,  // 增强莫奈淡紫
                0xFFCC99FF   // 增强莫奈紫罗兰
            };
        }

        // 获取动画参数
        long currentTime = System.currentTimeMillis();
        float flowOffset = themeConfig.isAnimationEnabled() ? (currentTime % 5000) / 5000.0F : 0.0F; // 5秒完成一次流动循环
        float breathe = themeConfig.isAnimationEnabled() 
            ? (float) (0.85F + 0.15F * Math.sin(currentTime / 1000.0)) 
            : 1.0F; // 优化呼吸效果：0.85-1.0，避免过暗

        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        
        // 外边框深度因子（从配置读取，范围 0-100，转换为 0.0-1.0）
        float outerFactor = themeConfig.getBorderOuterColorFactor() / 100.0F;

        // 绘制外层深色边框（上边）
        for (int x = left - 1; x < right + 1; x++) {
            int color = neotab$getAnimatedRainbowColor(x - left + 1, width + 2, rainbowColors, flowOffset);
            int darkColor = neotab$darkenColor(color, outerFactor * breathe);
            guiGraphics.fill(x, top - 1, x + 1, top, darkColor);
        }

        // 绘制外层深色边框（下边）
        for (int x = left - 1; x < right + 1; x++) {
            int color = neotab$getAnimatedRainbowColor(x - left + 1, width + 2, rainbowColors, flowOffset);
            int darkColor = neotab$darkenColor(color, outerFactor * breathe);
            guiGraphics.fill(x, bottom, x + 1, bottom + 1, darkColor);
        }

        // 绘制外层深色边框（左边）
        for (int y = top - 1; y < bottom + 1; y++) {
            int color = neotab$getAnimatedRainbowColor(y - top + 1, height + 2, rainbowColors, flowOffset);
            int darkColor = neotab$darkenColor(color, outerFactor * breathe);
            guiGraphics.fill(left - 1, y, left, y + 1, darkColor);
        }

        // 绘制外层深色边框（右边）
        for (int y = top - 1; y < bottom + 1; y++) {
            int color = neotab$getAnimatedRainbowColor(y - top + 1, height + 2, rainbowColors, flowOffset);
            int darkColor = neotab$darkenColor(color, outerFactor * breathe);
            guiGraphics.fill(right, y, right + 1, y + 1, darkColor);
        }

        // 绘制内层彩虹边框（上下边）
        for (int x = left; x < right; x++) {
            int color = neotab$getAnimatedRainbowColor(x - left, width, rainbowColors, flowOffset);
            color = neotab$applyBreathe(color, breathe);
            guiGraphics.fill(x, top, x + 1, top + 1, color);
            guiGraphics.fill(x, bottom - 1, x + 1, bottom, color);
        }

        // 绘制内层彩虹边框（左右边）
        for (int y = top; y < bottom; y++) {
            int color = neotab$getAnimatedRainbowColor(y - top, height, rainbowColors, flowOffset);
            color = neotab$applyBreathe(color, breathe);
            guiGraphics.fill(left, y, left + 1, y + 1, color);
            guiGraphics.fill(right - 1, y, right, y + 1, color);
        }
    }

    /**
     * 根据位置获取插值后的彩虹颜色（在固定颜色之间平滑过渡）
     * @param position 当前位置
     * @param total 总长度
     * @param colors 颜色数组
     * @return 插值后的颜色值
     */
    private int neotab$getInterpolatedRainbowColor(int position, int total, int[] colors) {
        float progress = (float) position / (float) total;
        int colorCount = colors.length;
        
        // 计算在哪两个颜色之间
        float scaledProgress = progress * colorCount;
        int index1 = (int) scaledProgress % colorCount;
        int index2 = (index1 + 1) % colorCount;
        float localProgress = scaledProgress - (int) scaledProgress;
        
        // 在两个颜色之间插值
        int color1 = colors[index1];
        int color2 = colors[index2];
        
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * localProgress);
        int g = (int) (g1 + (g2 - g1) * localProgress);
        int b = (int) (b1 + (b2 - b1) * localProgress);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 根据位置和时间偏移获取动画彩虹颜色（流动效果）
     * @param position 当前位置
     * @param total 总长度
     * @param colors 颜色数组
     * @param flowOffset 流动偏移量（0.0-1.0）
     * @return 插值后的颜色值
     */
    private int neotab$getAnimatedRainbowColor(int position, int total, int[] colors, float flowOffset) {
        float progress = ((float) position / (float) total + flowOffset) % 1.0F;
        int colorCount = colors.length;
        
        // 计算在哪两个颜色之间
        float scaledProgress = progress * colorCount;
        int index1 = (int) scaledProgress % colorCount;
        int index2 = (index1 + 1) % colorCount;
        float localProgress = scaledProgress - (int) scaledProgress;
        
        // 在两个颜色之间插值
        int color1 = colors[index1];
        int color2 = colors[index2];
        
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * localProgress);
        int g = (int) (g1 + (g2 - g1) * localProgress);
        int b = (int) (b1 + (b2 - b1) * localProgress);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 应用呼吸效果到颜色
     * @param color 原始颜色
     * @param breathe 呼吸因子（0.7-1.0）
     * @return 应用呼吸效果后的颜色
     */
    private int neotab$applyBreathe(int color, float breathe) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        r = (int) (r * breathe);
        g = (int) (g * breathe);
        b = (int) (b * breathe);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 将颜色变暗
     * @param color 原始颜色
     * @param factor 变暗因子（0.0-1.0，越小越暗）
     * @return 变暗后的颜色
     */
    private int neotab$darkenColor(int color, float factor) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        r = (int) (r * factor);
        g = (int) (g * factor);
        b = (int) (b * factor);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
