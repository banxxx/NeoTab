package com.poso.neotab.mixin.client;

import com.poso.neotab.client.NeoTabClientState;
import com.poso.neotab.config.TabLayoutConfig;
import com.poso.neotab.client.tab.TabBorderRenderer;
import com.poso.neotab.client.tab.TabColumnWidthCalculator;
import com.poso.neotab.client.tab.TabHealthRenderer;
import com.poso.neotab.theme.TabTheme;
import com.poso.neotab.theme.TabThemeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TAB 列表渲染 Mixin。
 *
 * <p>职责：Mixin 注入点的编排与状态管理。具体的渲染和计算逻辑已拆分到：</p>
 * <ul>
 *   <li>{@link TabHealthRenderer}   — 血量区宽度计算与血量图标渲染</li>
 *   <li>{@link TabColumnWidthCalculator} — 列宽计算</li>
 *   <li>{@link TabBorderRenderer}   — 彩虹边框与翻页箭头渲染</li>
 * </ul>
 *
 * <p>布局（从左到右）：[用户名] [NAME_GAP] [血量区（固定宽度）] [SECTION_GAP] [延迟] [SECTION_GAP] [在线时长] [原版信号图标或留白]</p>
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(
                    info -> info.getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing(info -> net.minecraft.Optionull.mapOrDefault(info.getTeam(), PlayerTeam::getName, ""))
            .thenComparing(info -> info.getProfile().getName(), String::compareToIgnoreCase);

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private Component header;
    @Shadow @Nullable private Component footer;
    @Shadow public abstract Component getNameForDisplay(PlayerInfo playerInfo);

    // ── 列宽缓存 ───────────────────────────────────────────────────────────────
    private int     cachedRequiredSpace       = -1;
    private int     cachedHealthAreaW         = -1;
    private int     lastMaxHealthDigits       = -1;
    private int     lastPlayerCount           = -1;
    private boolean lastBetterPingEnabled     = false;
    private boolean lastOnlineDurationEnabled = false;
    private boolean lastHealthDisplayEnabled  = false;
    private com.poso.neotab.config.HealthDisplayMode lastHealthDisplayMode = null;
    
    // ── 队伍快照缓冲区（性能优化：复用数组，避免重复分配）──────────────────────
    private String[] neotab$teamSnapshotBuffer = new String[80];

    // ── 玩家列表排序缓存 ───────────────────────────────────────────────────────
    private List<PlayerInfo> neotab$cachedSortedPlayers  = null;
    private int              neotab$lastCachedPlayerCount = -1;
    private String           neotab$lastTeamSnapshot      = null;
    /** 每 20 帧检查一次队伍变化，避免每帧构建快照字符串。 */
    private int              neotab$teamCheckCounter      = 0;

    // ── 列宽计算用的 UUID→PlayerInfo 快速查找表 ────────────────────────────────
    private final Map<UUID, PlayerInfo> neotab$playerInfoMap = new HashMap<>();

    // ── TAB 背景边界缓存（用于绘制边框） ──────────────────────────────────────
    private int tabBackgroundLeft   = -1;
    private int tabBackgroundTop    = -1;
    private int tabBackgroundRight  = -1;
    private int tabBackgroundBottom = -1;

    // ── 分页状态 ───────────────────────────────────────────────────────────────
    private boolean neotab$needsPagination  = false;
    private int     neotab$totalPlayerCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // 主题背景注入（HEAD）
    // ─────────────────────────────────────────────────────────────────────────

    @Inject(method = "render", at = @At("HEAD"))
    private void neotab$renderThemeBackground(
            GuiGraphics g, int screenWidth,
            net.minecraft.world.scores.Scoreboard scoreboard,
            net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {
        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());
        if (theme.isVanilla()) return;

        // TODO: 后续通过 @Shadow 获取精确坐标，目前占位
        if (false && theme.backgroundColor() != 0) {
            g.fill(0, 0, screenWidth, this.minecraft.getWindow().getGuiScaledHeight(), theme.backgroundColor());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 分页：拦截玩家列表，只返回当前页的子集
    // ─────────────────────────────────────────────────────────────────────────

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getPlayerInfos()Ljava/util/List;")
    )
    private List<PlayerInfo> neotab$filterPlayersForPage(PlayerTabOverlay self) {
        Collection<PlayerInfo> online = this.minecraft.player.connection.getListedOnlinePlayers();
        int currentCount = online.size();

        boolean needRebuild = neotab$cachedSortedPlayers == null
                || currentCount != neotab$lastCachedPlayerCount;

        if (!needRebuild) {
            // 玩家数量未变，每 20 帧检查一次队伍快照
            neotab$teamCheckCounter++;
            if (neotab$teamCheckCounter >= 20) {
                neotab$teamCheckCounter = 0;
                String teamSnapshot = neotab$buildTeamSnapshot(online);
                if (!teamSnapshot.equals(neotab$lastTeamSnapshot)) {
                    needRebuild = true;
                    neotab$lastTeamSnapshot = teamSnapshot;
                }
            }
        } else {
            neotab$lastTeamSnapshot = neotab$buildTeamSnapshot(online);
            neotab$teamCheckCounter = 0;
        }

        if (needRebuild) {
            // 性能优化：复用 ArrayList，避免每次重建都分配新对象
            if (neotab$cachedSortedPlayers == null) {
                neotab$cachedSortedPlayers = new ArrayList<>(80);
            } else {
                neotab$cachedSortedPlayers.clear();
            }
            
            // 手动排序并添加，避免 Stream 的额外开销
            List<PlayerInfo> temp = new ArrayList<>(online);
            temp.sort(PLAYER_COMPARATOR);
            int limit = Math.min(80, temp.size());
            for (int i = 0; i < limit; i++) {
                neotab$cachedSortedPlayers.add(temp.get(i));
            }
            
            neotab$lastCachedPlayerCount = currentCount;
        }

        List<PlayerInfo> all = neotab$cachedSortedPlayers;
        TabLayoutConfig layout = TabLayoutConfig.get();

        if (!layout.isEnabled()) {
            neotab$totalPlayerCount = all.size();
            NeoTabClientState.setTotalPages(1);
            neotab$needsPagination = false;
            return all;
        }

        int perPage = layout.playersPerPage();
        neotab$totalPlayerCount = all.size();
        int totalPages = Math.max(1, (all.size() + perPage - 1) / perPage);
        NeoTabClientState.setTotalPages(totalPages);
        neotab$needsPagination = totalPages > 1;

        if (!neotab$needsPagination) {
            return all;
        }

        int page = NeoTabClientState.getCurrentPage();
        int from = page * perPage;
        int to   = Math.min(from + perPage, all.size());
        if (from >= all.size()) {
            NeoTabClientState.setCurrentPage(totalPages - 1);
            from = (totalPages - 1) * perPage;
            to   = all.size();
        }
        return all.subList(from, to);
    }

    /** 
     * 构建队伍快照字符串，用于检测玩家队伍变化（每 20 帧调用一次）。
     * 
     * <p>性能优化：</p>
     * <ul>
     *   <li>复用缓冲区数组，避免每次分配新数组</li>
     *   <li>使用 UUID.hashCode() 替代 toString()，减少字符串分配</li>
     *   <li>使用 StringBuilder 替代 String.join()，减少临时对象</li>
     * </ul>
     */
    private String neotab$buildTeamSnapshot(Collection<PlayerInfo> players) {
        int size = players.size();
        
        // 扩容缓冲区（如果需要）
        if (neotab$teamSnapshotBuffer.length < size) {
            neotab$teamSnapshotBuffer = new String[Math.max(size, 80)];
        }
        
        // 构建快照条目
        int i = 0;
        for (PlayerInfo pi : players) {
            String team = pi.getTeam() != null ? ((PlayerTeam) pi.getTeam()).getName() : "";
            // 使用 hashCode 替代 toString()，减少字符串分配
            neotab$teamSnapshotBuffer[i++] = pi.getProfile().getId().hashCode() + ":" + team;
        }
        
        // 排序（只排序有效部分）
        Arrays.sort(neotab$teamSnapshotBuffer, 0, size);
        
        // 使用 StringBuilder 拼接，避免 String.join() 的额外开销
        StringBuilder sb = new StringBuilder(size * 40);
        for (int j = 0; j < size; j++) {
            if (j > 0) sb.append('|');
            sb.append(neotab$teamSnapshotBuffer[j]);
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 背景填充拦截（捕获 TAB 边界 + 主题背景色替换）
    // ─────────────────────────────────────────────────────────────────────────

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")
    )
    private void neotab$redirectTabBackgroundFill(GuiGraphics guiGraphics,
                                                   int minX, int minY, int maxX, int maxY, int color) {
        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());

        if (!theme.isVanilla() && "custom".equals(theme.id()) && color == Integer.MIN_VALUE) {
            color = com.poso.neotab.theme.CustomThemeManager.get().getBackgroundColor();
        } else if (!theme.isVanilla() && color == Integer.MIN_VALUE && theme.backgroundColor() != 0) {
            color = theme.backgroundColor();
        }

        if (color == Integer.MIN_VALUE || (!theme.isVanilla() && (theme.backgroundColor() != 0 || "custom".equals(theme.id())))) {
            if (tabBackgroundLeft   == -1 || minX < tabBackgroundLeft)   tabBackgroundLeft   = minX;
            if (tabBackgroundTop    == -1 || minY < tabBackgroundTop)    tabBackgroundTop    = minY;
            if (tabBackgroundRight  == -1 || maxX > tabBackgroundRight)  tabBackgroundRight  = maxX;
            if (tabBackgroundBottom == -1 || maxY > tabBackgroundBottom) tabBackgroundBottom = maxY;
        }

        guiGraphics.fill(minX, minY, maxX, maxY, color);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 列数调整（每列最大行数）
    // ─────────────────────────────────────────────────────────────────────────

    @ModifyConstant(method = "render", constant = @Constant(intValue = 20))
    private int neotab$adjustMaxRowsPerColumn(int original) {
        TabLayoutConfig layout = TabLayoutConfig.get();
        if (!layout.isEnabled()) return original;
        int displayRows = layout.getRowsPerColumn();
        return displayRows > 0 ? displayRows : original;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 边框 + 翻页箭头（TAIL 注入）
    // ─────────────────────────────────────────────────────────────────────────

    @Inject(method = "render", at = @At("TAIL"))
    private void neotab$renderBorderAndArrows(
            GuiGraphics guiGraphics, int width,
            net.minecraft.world.scores.Scoreboard scoreboard,
            net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {

        var config = NeoTabClientState.getCurrentConfig();
        TabTheme theme = TabThemeRegistry.get(config.tabTheme());

        if (tabBackgroundLeft != -1 && tabBackgroundTop != -1
                && tabBackgroundRight != -1 && tabBackgroundBottom != -1) {

            int extraH = neotab$needsPagination
                    ? TabBorderRenderer.PAGE_ARROW_W + TabBorderRenderer.TAB_CONTENT_PADDING : 0;

            int pl = tabBackgroundLeft   - TabBorderRenderer.TAB_CONTENT_PADDING - extraH;
            int pt = tabBackgroundTop    - TabBorderRenderer.TAB_CONTENT_PADDING;
            int pr = tabBackgroundRight  + TabBorderRenderer.TAB_CONTENT_PADDING + extraH;
            int pb = tabBackgroundBottom + TabBorderRenderer.TAB_CONTENT_PADDING;

            final boolean isCustomTheme = "custom".equals(theme.id());
            int bgColor;
            if (isCustomTheme) {
                bgColor = com.poso.neotab.theme.CustomThemeManager.get().getBackgroundColor();
            } else if (!theme.isVanilla() && theme.backgroundColor() != 0) {
                bgColor = theme.backgroundColor();
            } else {
                bgColor = Integer.MIN_VALUE;
            }

            // 填充四条 padding 边带
            guiGraphics.fill(pl, pt, pr, tabBackgroundTop,    bgColor);
            guiGraphics.fill(pl, tabBackgroundBottom, pr, pb, bgColor);
            guiGraphics.fill(pl, tabBackgroundTop, tabBackgroundLeft,  tabBackgroundBottom, bgColor);
            guiGraphics.fill(tabBackgroundRight, tabBackgroundTop, pr, tabBackgroundBottom, bgColor);

            if (isCustomTheme) {
                TabBorderRenderer.drawRainbowBorder(guiGraphics, pl, pt, pr, pb);
            }

            if (neotab$needsPagination) {
                TabBorderRenderer.drawPageArrows(guiGraphics, pl, pt, pr, pb, bgColor);
                NeoTabClientState.setTabBounds(pl, pt, pr, pb);
            }
        }

        // 重置边界缓存，为下一帧做准备
        tabBackgroundLeft   = -1;
        tabBackgroundTop    = -1;
        tabBackgroundRight  = -1;
        tabBackgroundBottom = -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 右对齐信息渲染（血量 / 延迟 / 在线时长）
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

        if (hasPing) ci.cancel();
        if (!hasPing && !hasDuration && !hasHealth) return;

        UUID pid = playerInfo.getProfile().getId();

        String pingText     = "";
        String durationText = "";
        int    pingColor    = 0xFFFFFF;
        int    durationColor = ChatFormatting.AQUA.getColor() != null
                ? ChatFormatting.AQUA.getColor() : 0x55FFFF;

        if (hasPing) {
            int latency = playerInfo.getLatency();
            pingText  = latency + "ms";
            pingColor = TabHealthRenderer.getPingColor(latency);
        }
        if (hasDuration) {
            durationText = NeoTabClientState.getOnlineDuration(pid);
        }

        float health    = hasHealth ? NeoTabClientState.getPlayerHealth(pid)    : 0f;
        float maxHealth = hasHealth ? NeoTabClientState.getPlayerMaxHealth(pid) : 0f;

        int healthAreaW = hasHealth ? getOrCalcHealthAreaW(font) : 0;
        int pingW       = hasPing     ? font.width(pingText)     : 0;
        int durationW   = hasDuration ? font.width(durationText) : 0;

        int gapHealthRight = hasHealth && (hasPing || hasDuration) ? TabHealthRenderer.SECTION_GAP : 0;
        int gapPingDur     = hasPing   && hasDuration              ? TabHealthRenderer.SECTION_GAP : 0;

        int totalW    = healthAreaW + gapHealthRight + pingW + gapPingDur + durationW;
        int rightEdge = x + width;
        if (!hasPing) rightEdge -= TabColumnWidthCalculator.PING_ICON_W;

        int cx = rightEdge - totalW;

        if (hasHealth) {
            TabHealthRenderer.renderHealth(g, font, cx, y, health, maxHealth, healthAreaW);
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
    // 列宽调整（拦截常量 13）
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
            neotab$rebuildPlayerInfoMap();
            Collection<PlayerInfo> players = this.minecraft.getConnection() != null
                    ? this.minecraft.getConnection().getOnlinePlayers()
                    : List.of();
            if (config.healthDisplayEnabled()) {
                cachedHealthAreaW   = TabHealthRenderer.calcUnifiedHealthAreaW(font, players);
                lastMaxHealthDigits = TabHealthRenderer.currentMaxHealthDigits(players);
            } else {
                cachedHealthAreaW   = -1;
                lastMaxHealthDigits = -1;
            }
            cachedRequiredSpace       = TabColumnWidthCalculator.calcRequiredSpace(
                    font, config, players, cachedHealthAreaW, neotab$playerInfoMap);
            lastBetterPingEnabled     = config.betterPingEnabled();
            lastOnlineDurationEnabled = config.onlineDurationEnabled();
            lastHealthDisplayEnabled  = config.healthDisplayEnabled();
            lastHealthDisplayMode     = config.healthDisplayMode();
            lastPlayerCount           = currentCount;
        } else if (config.healthDisplayEnabled()) {
            Collection<PlayerInfo> players = this.minecraft.getConnection() != null
                    ? this.minecraft.getConnection().getOnlinePlayers()
                    : List.of();
            int digits = TabHealthRenderer.currentMaxHealthDigits(players);
            if (digits != lastMaxHealthDigits) {
                Font font = this.minecraft.font;
                neotab$rebuildPlayerInfoMap();
                cachedHealthAreaW   = TabHealthRenderer.calcUnifiedHealthAreaW(font, players);
                cachedRequiredSpace = TabColumnWidthCalculator.calcRequiredSpace(
                        font, config, players, cachedHealthAreaW, neotab$playerInfoMap);
                lastMaxHealthDigits = digits;
            }
        }

        return cachedRequiredSpace;
    }

    /** 重建 UUID→PlayerInfo 快速查找表，在需要重新计算列宽时调用一次。 */
    private void neotab$rebuildPlayerInfoMap() {
        neotab$playerInfoMap.clear();
        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                neotab$playerInfoMap.put(pi.getProfile().getId(), pi);
            }
        }
    }

    /** 获取缓存的统一血量区宽度，若未缓存则立即计算。 */
    private int getOrCalcHealthAreaW(Font font) {
        if (cachedHealthAreaW == -1) {
            Collection<PlayerInfo> players = this.minecraft.getConnection() != null
                    ? this.minecraft.getConnection().getOnlinePlayers()
                    : List.of();
            cachedHealthAreaW = TabHealthRenderer.calcUnifiedHealthAreaW(font, players);
        }
        return cachedHealthAreaW;
    }
}
