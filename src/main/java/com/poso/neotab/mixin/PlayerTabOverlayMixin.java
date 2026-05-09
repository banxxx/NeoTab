package com.poso.neotab.mixin;

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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
 * TAB 列表渲染 Mixin（Forge 1.20.1 版本）。
 * 移植自 NeoForge 1.21.1 版本，适配 1.20.1 API 差异。
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

    // ── 列宽缓存 ───────────────────────────────────────────────────────────────
    private int     cachedRequiredSpace       = -1;
    private int     cachedHealthAreaW         = -1;
    private int     lastMaxHealthDigits       = -1;
    private int     lastPlayerCount           = -1;
    private boolean lastBetterPingEnabled     = false;
    private boolean lastOnlineDurationEnabled = false;
    private boolean lastHealthDisplayEnabled  = false;
    private com.poso.neotab.config.HealthDisplayMode lastHealthDisplayMode = null;

    // ── 队伍快照缓冲区 ─────────────────────────────────────────────────────────
    private String[] neotab$teamSnapshotBuffer = new String[80];

    // ── 玩家列表排序缓存 ───────────────────────────────────────────────────────
    private List<PlayerInfo> neotab$cachedSortedPlayers  = null;
    private int              neotab$lastCachedPlayerCount = -1;
    private String           neotab$lastTeamSnapshot      = null;
    private int              neotab$teamCheckCounter      = 0;

    // ── UUID→PlayerInfo 快速查找表 ─────────────────────────────────────────────
    private final Map<UUID, PlayerInfo> neotab$playerInfoMap = new HashMap<>();

    // ── TAB 背景边界缓存 ───────────────────────────────────────────────────────
    private int tabBackgroundLeft   = -1;
    private int tabBackgroundTop    = -1;
    private int tabBackgroundRight  = -1;
    private int tabBackgroundBottom = -1;

    // ── 分页状态 ───────────────────────────────────────────────────────────────
    private boolean neotab$needsPagination  = false;
    private int     neotab$totalPlayerCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // 分页：拦截玩家列表，只返回当前页的子集
    // ─────────────────────────────────────────────────────────────────────────

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getPlayerInfos()Ljava/util/List;")
    )
    private List<PlayerInfo> neotab$filterPlayersForPage(PlayerTabOverlay self) {
        Collection<PlayerInfo> online = this.minecraft.player.connection.getListedOnlinePlayers();
        int currentCount = online.size();

        boolean needRebuild = neotab$cachedSortedPlayers == null
                || currentCount != neotab$lastCachedPlayerCount;

        if (!needRebuild) {
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
            if (neotab$cachedSortedPlayers == null) {
                neotab$cachedSortedPlayers = new ArrayList<>(80);
            } else {
                neotab$cachedSortedPlayers.clear();
            }
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
            NeoTabClientState.recalculatePages(all.size());
            neotab$needsPagination = false;
            return all;
        }

        int perPage = layout.playersPerPage();
        neotab$totalPlayerCount = all.size();
        NeoTabClientState.recalculatePages(all.size());
        int totalPages = NeoTabClientState.getTotalPages();
        neotab$needsPagination = totalPages > 1;

        if (!neotab$needsPagination) {
            return all;
        }

        int page = NeoTabClientState.getCurrentPage();
        int from = page * perPage;
        int to   = Math.min(from + perPage, all.size());
        if (from >= all.size()) {
            NeoTabClientState.goToPage(totalPages - 1);
            from = (totalPages - 1) * perPage;
            to   = all.size();
        }
        return all.subList(from, to);
    }

    private String neotab$buildTeamSnapshot(Collection<PlayerInfo> players) {
        int size = players.size();
        if (neotab$teamSnapshotBuffer.length < size) {
            neotab$teamSnapshotBuffer = new String[Math.max(size, 80)];
        }
        int i = 0;
        for (PlayerInfo pi : players) {
            String team = pi.getTeam() != null ? ((PlayerTeam) pi.getTeam()).getName() : "";
            neotab$teamSnapshotBuffer[i++] = pi.getProfile().getId().hashCode() + ":" + team;
        }
        Arrays.sort(neotab$teamSnapshotBuffer, 0, size);
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
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
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

    @ModifyConstant(method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", constant = @Constant(intValue = 20))
    private int neotab$adjustMaxRowsPerColumn(int original) {
        TabLayoutConfig layout = TabLayoutConfig.get();
        if (!layout.isEnabled()) return original;
        int displayRows = layout.getRowsPerColumn();
        return displayRows > 0 ? displayRows : original;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 边框 + 翻页箭头（TAIL 注入）
    // ─────────────────────────────────────────────────────────────────────────

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", at = @At("TAIL"))
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

        tabBackgroundLeft   = -1;
        tabBackgroundTop    = -1;
        tabBackgroundRight  = -1;
        tabBackgroundBottom = -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 右对齐信息渲染（血量 / 延迟 / 在线时长）
    // ─────────────────────────────────────────────────────────────────────────

    @Inject(method = "renderPingIcon(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V", at = @At("HEAD"), cancellable = true)
    private void neotab$renderRightAlignedInfo(
            GuiGraphics g, int width, int x, int y,
            PlayerInfo playerInfo, CallbackInfo ci) {

        var config = NeoTabClientState.getCurrentConfig();
        Font font  = this.minecraft.font;

        boolean hasPing     = config.betterPingEnabled();
        boolean hasDuration = config.onlineDurationEnabled();
        boolean hasHealth   = config.healthDisplayEnabled();

        // DEBUG: log to confirm mixin is firing and config values
        if (hasPing || hasDuration || hasHealth) {
            com.poso.neotab.NeoTab.LOGGER.info("neotab$renderRightAlignedInfo FIRING: ping={} dur={} health={} player={}",
                hasPing, hasDuration, hasHealth, playerInfo.getProfile().getName());
        }

        // 只要任意 NeoTab 功能启用，就取消原版 renderPingIcon，
        // 避免原版延迟图标与自定义内容重叠导致无效果
        if (hasPing || hasDuration || hasHealth) {
            ci.cancel();
        }
        if (!hasPing && !hasDuration && !hasHealth) return;

        UUID pid = playerInfo.getProfile().getId();

        String pingText      = "";
        String durationText  = "";
        int    pingColor     = 0xFFFFFF;
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

    // @Inject(
    //         method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
    //         at = @At(value = "INVOKE",
    //                 target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;renderPingIcon(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"),
    //         cancellable = true
    // )
    // private void neotab$onRenderPingIconCalled(PlayerTabOverlay self,
    //                                            GuiGraphics g, int width, int x, int y,
    //                                            PlayerInfo info, CallbackInfo ci) {
    //     var config = NeoTabClientState.getCurrentConfig();
    //     Font font = this.minecraft.font;
    //
    //     boolean hasPing     = config.betterPingEnabled();
    //     boolean hasDuration = config.onlineDurationEnabled();
    //     boolean hasHealth   = config.healthDisplayEnabled();
    //
    //     com.poso.neotab.NeoTab.LOGGER.info("neotab$onRenderPingIconCalled FIRING: ping={} dur={} health={} player={}",
    //             hasPing, hasDuration, hasHealth, info.getProfile().getName());
    //
    //     // 如果没有任何 NeoTab 功能开启，则回退到原版渲染（不取消调用，直接 return）
    //     if (!hasPing && !hasDuration && !hasHealth) {
    //         return; // 让原版 renderPingIcon 正常执行
    //     }
    //
    //     // 取消原版调用
    //     ci.cancel();
    //
    //     UUID pid = info.getProfile().getId();
    //
    //     String pingText      = "";
    //     String durationText  = "";
    //     int    pingColor     = 0xFFFFFF;
    //     int    durationColor = ChatFormatting.AQUA.getColor() != null
    //             ? ChatFormatting.AQUA.getColor() : 0x55FFFF;
    //
    //     if (hasPing) {
    //         int latency = info.getLatency();
    //         pingText  = latency + "ms";
    //         pingColor = TabHealthRenderer.getPingColor(latency);
    //     }
    //     if (hasDuration) {
    //         durationText = NeoTabClientState.getOnlineDuration(pid);
    //     }
    //
    //     float health    = hasHealth ? NeoTabClientState.getPlayerHealth(pid)    : 0f;
    //     float maxHealth = hasHealth ? NeoTabClientState.getPlayerMaxHealth(pid) : 0f;
    //
    //     int healthAreaW = hasHealth ? getOrCalcHealthAreaW(font) : 0;
    //     int pingW       = hasPing     ? font.width(pingText)     : 0;
    //     int durationW   = hasDuration ? font.width(durationText) : 0;
    //
    //     int gapHealthRight = hasHealth && (hasPing || hasDuration) ? TabHealthRenderer.SECTION_GAP : 0;
    //     int gapPingDur     = hasPing   && hasDuration              ? TabHealthRenderer.SECTION_GAP : 0;
    //
    //     int totalW    = healthAreaW + gapHealthRight + pingW + gapPingDur + durationW;
    //     int rightEdge = x + width;
    //     if (!hasPing) rightEdge -= TabColumnWidthCalculator.PING_ICON_W;
    //
    //     int cx = rightEdge - totalW;
    //
    //     if (hasHealth) {
    //         TabHealthRenderer.renderHealth(g, font, cx, y, health, maxHealth, healthAreaW);
    //         cx += healthAreaW + gapHealthRight;
    //     }
    //     if (hasPing) {
    //         g.drawString(font, pingText, cx, y, pingColor, false);
    //         cx += pingW + gapPingDur;
    //     }
    //     if (hasDuration) {
    //         g.drawString(font, durationText, cx, y, durationColor, false);
    //     }
    // }

    // ─────────────────────────────────────────────────────────────────────────
    // 列宽调整（拦截常量 13）
    // ─────────────────────────────────────────────────────────────────────────

    @ModifyConstant(method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", constant = @Constant(intValue = 13))
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

    private void neotab$rebuildPlayerInfoMap() {
        neotab$playerInfoMap.clear();
        if (this.minecraft.getConnection() != null) {
            for (var pi : this.minecraft.getConnection().getOnlinePlayers()) {
                neotab$playerInfoMap.put(pi.getProfile().getId(), pi);
            }
        }
    }

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
