package com.poso.neotab.client.screen;

import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.packet.SaveConfigPacket;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import com.poso.neotab.util.ScreenAccessHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PermissionsTabManager {

    private static final int ROW_HEIGHT   = 24;
    private static final int INPUT_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 26;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int ROW_GAP      = 10;

    private final NeoTabConfigScreen screen;

    // ── Permissions tab fields ────────────────────────────────────────────────
    final List<CycleButton<Boolean>> globalPolicyToggles = new ArrayList<>();
    EditBox playerSearchBox;
    Button permAddButton;
    final List<String> playerSuggestions = new ArrayList<>();
    int dropdownScrollOffset = 0;
    final LinkedHashMap<UUID, String> targetPlayers = new LinkedHashMap<>();
    final List<Button> targetPlayerRemoveButtons = new ArrayList<>();
    Button applyToAllButton;
    Button applyToAddedButton;

    // Apply 按钮二次确认状态：首次点击武装，CONFIRM_WINDOW_MS 内再次点击才真正下发
    private static final long APPLY_CONFIRM_WINDOW_MS = 4000L;
    private long applyAllArmedUntil = 0L;
    private long applyAddedArmedUntil = 0L;

    // 新增：覆盖个人策略勾选框
    CycleButton<Boolean> overridePersonalPolicyToggle;

    /**
     * 会话内的个人专属策略视图：初始快照 + 本界面"应用"按钮的更新。
     * 完成保存时写回这个视图，避免用开屏快照抹掉/回退已有的个人策略。
     */
    Map<UUID, PlayerCustomizePolicy> playerPoliciesView = new HashMap<>();

    PermissionsTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    Map<UUID, PlayerCustomizePolicy> getPlayerPoliciesView() {
        return playerPoliciesView;
    }

    void clear() {
        globalPolicyToggles.clear();
        playerSuggestions.clear();
        targetPlayerRemoveButtons.clear();
        applyToAllButton = null;
        applyToAddedButton = null;
        overridePersonalPolicyToggle = null;
        applyAllArmedUntil = 0L;
        applyAddedArmedUntil = 0L;
    }

    void initPermissionsWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig initialConfig) {
        PlayerCustomizePolicy global = initialConfig.globalPolicy();
        this.playerPoliciesView = new HashMap<>(initialConfig.playerPolicies());

        // Player search box
        this.playerSearchBox = ScreenAccessHelper.addWidget(screen,
                new com.poso.neotab.client.widget.CenteredEditBox(screen.font(), layout.left(), 0,
                        layout.contentWidth(), INPUT_HEIGHT,
                        Component.translatable("screen.neotab.input.player_search_hint")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.translatable("screen.neotab.input.player_search_hint"));
        this.playerSearchBox.setBordered(false);
        this.playerSearchBox.setTextColor(0xFF000000);
        this.playerSearchBox.active = true;
        this.playerSearchBox.setEditable(true);
        this.playerSearchBox.visible = false;
        this.playerSearchBox.setResponder(text -> {
            playerSuggestions.clear();
            if (!text.isBlank()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    String lower = text.toLowerCase();
                    mc.getConnection().getOnlinePlayers().stream()
                            .map(p -> p.getProfile().getName())
                            .filter(name -> name.toLowerCase().contains(lower))
                            .forEach(playerSuggestions::add);
                }
            }
        });

        // Add button
        this.permAddButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.policy.add"),
                        btn -> {
                            if (playerSearchBox == null) return;
                            String name = playerSearchBox.getValue().trim();
                            if (name.isEmpty()) return;
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getConnection() != null) {
                                mc.getConnection().getOnlinePlayers().stream()
                                        .filter(p -> p.getProfile().getName().equalsIgnoreCase(name))
                                        .findFirst()
                                        .ifPresent(p -> {
                                            UUID uuid = p.getProfile().getId();
                                            if (!targetPlayers.containsKey(uuid)) {
                                                targetPlayers.put(uuid, p.getProfile().getName());
                                                rebuildTargetPlayerButtons(screen.buildLayout());
                                                screen.syncVisibility();
                                                screen.applyLayout(screen.buildLayout());
                                            }
                                            playerSearchBox.setValue("");
                                            playerSuggestions.clear();
                                        });
                            }
                        })
                .bounds(layout.left(), 0, 60, INPUT_HEIGHT)
                .build());
        this.permAddButton.active = true;
        this.permAddButton.visible = false;

        // Global policy toggles
        boolean[] globalValues = {
                global.allowTopTitleToggle(),    global.allowTopTitleEdit(),
                global.allowTopContentToggle(),  global.allowTopContentEdit(),
                global.allowPingDisplayToggle(), global.allowDurationToggle(),
                global.allowTitleToggle(),       global.allowHealthDisplayToggle(),
                global.allowHealthModeChange(),  global.allowFooterCustomEdit(),
                global.allowFooterTpsToggle(),   global.allowFooterMsptToggle(),
                global.allowFooterOnlineToggle(), global.allowThemeChange()
        };
        for (boolean val : globalValues) {
            CycleButton<Boolean> toggle = ScreenAccessHelper.addWidget(screen,
                    CycleButton.onOffBuilder(val)
                            .displayOnlyValue()
                            .create(layout.toggleX(), 0, TOGGLE_WIDTH, TOGGLE_HEIGHT, CommonComponents.EMPTY,
                                    (btn, v) -> { /* value read on save */ }));
            toggle.visible = false;
            toggle.active = true;
            globalPolicyToggles.add(toggle);
        }

        // 覆盖个人策略勾选框
        this.overridePersonalPolicyToggle = ScreenAccessHelper.addWidget(screen,
                CycleButton.onOffBuilder(false)
                        .displayOnlyValue()
                        .create(layout.left(), 0, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                                Component.translatable("screen.neotab.permissions.override_personal"),
                                (btn, v) -> { /* just toggle state */ }));
        this.overridePersonalPolicyToggle.visible = false;

        // Apply to all players button（二次确认，见 onApplyToAllClicked）
        this.applyToAllButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.permissions.apply_to_all"),
                        btn -> onApplyToAllClicked(initialConfig, btn))
                .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
                .build());
        this.applyToAllButton.visible = false;
        this.applyToAllButton.active = true;

        // Apply to added players button（二次确认，见 onApplyToAddedClicked）
        this.applyToAddedButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.permissions.apply_to_added"),
                        btn -> onApplyToAddedClicked(initialConfig, btn))
                .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
                .build());
        this.applyToAddedButton.visible = false;
        this.applyToAddedButton.active = true;
    }

    private void onApplyToAllClicked(TabConfig initialConfig, Button btn) {
        long now = System.currentTimeMillis();
        if (now < applyAllArmedUntil) {
            applyAllArmedUntil = 0L;
            btn.setMessage(Component.translatable("screen.neotab.permissions.apply_to_all"));
            applyToAllPlayers(initialConfig);
            return;
        }
        applyAllArmedUntil = now + APPLY_CONFIRM_WINDOW_MS;
        btn.setMessage(Component.translatable("screen.neotab.permissions.confirm_apply"));
    }

    private void onApplyToAddedClicked(TabConfig initialConfig, Button btn) {
        long now = System.currentTimeMillis();
        if (now < applyAddedArmedUntil) {
            applyAddedArmedUntil = 0L;
            btn.setMessage(Component.translatable("screen.neotab.permissions.apply_to_added"));
            applyToAddedPlayers(initialConfig);
            return;
        }
        applyAddedArmedUntil = now + APPLY_CONFIRM_WINDOW_MS;
        btn.setMessage(Component.translatable("screen.neotab.permissions.confirm_apply"));
    }

    /** 由 Screen.tick 驱动：确认窗口超时后恢复按钮文案。 */
    void tickApplyArmed() {
        long now = System.currentTimeMillis();
        if (applyAllArmedUntil != 0L && now >= applyAllArmedUntil) {
            applyAllArmedUntil = 0L;
            if (applyToAllButton != null) {
                applyToAllButton.setMessage(Component.translatable("screen.neotab.permissions.apply_to_all"));
            }
        }
        if (applyAddedArmedUntil != 0L && now >= applyAddedArmedUntil) {
            applyAddedArmedUntil = 0L;
            if (applyToAddedButton != null) {
                applyToAddedButton.setMessage(Component.translatable("screen.neotab.permissions.apply_to_added"));
            }
        }
    }

    void applyToAllPlayers(TabConfig initialConfig) {
        PlayerCustomizePolicy policy = buildGlobalPolicyFromToggles();
        // 勾选覆盖个人策略时同步清空视图，否则保持现状
        Map<UUID, PlayerCustomizePolicy> policies = overridePersonalPolicyToggle.getValue()
                ? new HashMap<>()
                : new HashMap<>(playerPoliciesView);
        TabConfig config = copyWithPolicy(initialConfig, policy, policies);
        com.poso.neotab.network.NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new SaveConfigPacket(config));
        this.playerPoliciesView = policies;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    Component.translatable("message.neotab.permissions.applied_to_all"));
        }
    }

    void applyToAddedPlayers(TabConfig initialConfig) {
        if (targetPlayers.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.translatable("message.neotab.permissions.no_players_added"));
            }
            return;
        }
        // 以当前开关值为模板，只写入选中的玩家，其余个人策略保持不动
        PlayerCustomizePolicy policy = buildGlobalPolicyFromToggles();
        Map<UUID, PlayerCustomizePolicy> policies = new HashMap<>(playerPoliciesView);
        for (UUID uuid : targetPlayers.keySet()) {
            policies.put(uuid, policy);
        }
        TabConfig config = copyWithPolicy(initialConfig, initialConfig.globalPolicy(), policies);
        com.poso.neotab.network.NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new SaveConfigPacket(config));
        this.playerPoliciesView = policies;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    Component.translatable("message.neotab.permissions.applied_to_players", targetPlayers.size()));
        }
    }

    private static TabConfig copyWithPolicy(TabConfig base, PlayerCustomizePolicy globalPolicy,
                                            Map<UUID, PlayerCustomizePolicy> playerPolicies) {
        return new TabConfig(
                base.topTitleEnabled(),
                base.topTitleText(),
                base.topContentEnabled(),
                base.topContentText(),
                base.betterPingEnabled(),
                base.onlineDurationEnabled(),
                base.titleEnabled(),
                base.healthDisplayEnabled(),
                base.healthDisplayMode(),
                base.tabTheme(),
                base.footerCustomText(),
                base.footerTpsEnabled(),
                base.footerMsptEnabled(),
                base.footerOnlineEnabled(),
                base.refreshIntervalTicks(),
                globalPolicy,
                playerPolicies
        ).sanitized();
    }

    void rebuildTargetPlayerButtons(NeoTabConfigScreenLayout.Layout layout) {
        for (Button btn : targetPlayerRemoveButtons) ScreenAccessHelper.removeWidget(screen, btn);
        targetPlayerRemoveButtons.clear();
        boolean perms = screen.getActiveTab() == NeoTabConfigScreen.ConfigTab.PERMISSIONS;
        for (UUID uuid : targetPlayers.keySet()) {
            Button removeBtn = ScreenAccessHelper.addWidget(screen, Button.builder(
                            Component.literal("×"),
                            btn -> {
                                // 只移出目标列表；策略开关是全局/模板状态，不应因删除目标而重置
                                targetPlayers.remove(uuid);
                                rebuildTargetPlayerButtons(screen.buildLayout());
                                screen.syncVisibility();
                                screen.applyLayout(screen.buildLayout());
                            })
                    .bounds(layout.left(), 0, 18, INPUT_HEIGHT)
                    .build());
            removeBtn.visible = perms;
            removeBtn.active = true;
            targetPlayerRemoveButtons.add(removeBtn);
        }
    }

    PlayerCustomizePolicy buildGlobalPolicyFromToggles() {
        if (globalPolicyToggles.size() < 14) {
            return PlayerCustomizePolicy.locked();
        }
        // 直接从 UI 开关读取当前值
        return new PlayerCustomizePolicy(
                globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
                globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
                globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
                globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
                globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
                globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
                globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
                false
        );
    }

    /** Apply layout positions to all permissions tab widgets. */
    void applyLayout(NeoTabConfigScreenLayout.Layout layout, int rowHeight, int rowGap, int inputHeight,
                     int contentTopPadding, int sectionHeaderHeight, int toggleWidth) {
        if (globalPolicyToggles.isEmpty()) return;

        int CARD_PADDING = 10;
        int CARD_GAP = 8;
        int titleLineHeight = 9;
        int subtitleLineHeight = 9;

        int y = contentTopPadding;
        y += sectionHeaderHeight;

        // Position global policy toggles (2-column grid)
        int cardWidth = (layout.contentWidth() - CARD_GAP) / 2;
        int cardHeight = CARD_PADDING + Math.max(TOGGLE_HEIGHT, titleLineHeight + 2 + subtitleLineHeight) + CARD_PADDING;

        for (int i = 0; i < Math.min(14, globalPolicyToggles.size()); i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = layout.left() + col * (cardWidth + CARD_GAP);
            int cardY = layout.toScreenY(y + row * (cardHeight + CARD_GAP));
            CycleButton<Boolean> toggle = globalPolicyToggles.get(i);
            toggle.setX(cardX + cardWidth - CARD_PADDING - toggleWidth);
            toggle.setY(cardY + (cardHeight - TOGGLE_HEIGHT) / 2);
            toggle.setWidth(toggleWidth);
        }

        int rowCount = (14 + 1) / 2;
        y += rowCount * (cardHeight + CARD_GAP) - CARD_GAP + 16; // after section gap

        y += sectionHeaderHeight; // personal section header

        // Add player card
        int addPlayerCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + inputHeight + CARD_PADDING;
        int inputY = layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
        int addButtonWidth = 60;
        int inputWidth = layout.contentWidth() - CARD_PADDING * 2 - addButtonWidth - 8;

        if (playerSearchBox != null) {
            playerSearchBox.setX(layout.left() + CARD_PADDING);
            playerSearchBox.setY(inputY);
            playerSearchBox.setWidth(inputWidth);
        }
        if (permAddButton != null) {
            permAddButton.setX(layout.left() + CARD_PADDING + inputWidth + 8);
            permAddButton.setY(inputY);
            permAddButton.setWidth(addButtonWidth);
        }
        y += addPlayerCardHeight + CARD_GAP;

        // Player list card (tags and remove buttons)
        int playerListContentHeight;
        if (targetPlayers.isEmpty()) {
            playerListContentHeight = inputHeight;
        } else {
            int tagHeight = inputHeight;
            int tagGap = 6;
            int maxWidth = layout.contentWidth() - CARD_PADDING * 2;
            int tagX = 0, tagY = 0;
            for (Map.Entry<UUID, String> entry : targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = screen.font().width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                tagX += tagWidth + tagGap;
            }
            playerListContentHeight = tagY + tagHeight;
        }
        int playerListCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + playerListContentHeight + CARD_PADDING;

        // Position remove buttons
        if (!targetPlayers.isEmpty()) {
            int listContentY = layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
            int tagHeight = inputHeight;
            int tagGap = 6;
            int maxWidth = layout.contentWidth() - CARD_PADDING * 2;
            int tagX = 0, tagY = 0;
            int buttonIndex = 0;
            for (Map.Entry<UUID, String> entry : targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = screen.font().width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                if (buttonIndex < targetPlayerRemoveButtons.size()) {
                    Button removeBtn = targetPlayerRemoveButtons.get(buttonIndex);
                    removeBtn.setX(layout.left() + CARD_PADDING + tagX + tagWidth - deleteButtonWidth - tagPadding);
                    removeBtn.setY(listContentY + tagY + (tagHeight - deleteButtonWidth) / 2);
                    removeBtn.setWidth(deleteButtonWidth);
                    removeBtn.setHeight(deleteButtonWidth);
                }
                tagX += tagWidth + tagGap;
                buttonIndex++;
            }
        }
        y += playerListCardHeight + CARD_GAP;

        // Hint card (no widgets)
        int hintCardHeight = CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 2 * 0.82f) + CARD_PADDING;
        y += hintCardHeight + 16;

        // Apply settings card
        int applySettingsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + TOGGLE_HEIGHT + 4 + inputHeight + CARD_PADDING;
        int applyButtonY = layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + TOGGLE_HEIGHT + 4;;

        // 覆盖个人策略勾选框 - 放在标题/副标题下方，开关在文字前面
        boolean perms = screen.getActiveTab() == NeoTabConfigScreen.ConfigTab.PERMISSIONS;
        if (overridePersonalPolicyToggle != null) {
            // 开关放在左侧
            overridePersonalPolicyToggle.setX(layout.left() + CARD_PADDING);
            overridePersonalPolicyToggle.setY(layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8);
            overridePersonalPolicyToggle.setWidth(TOGGLE_WIDTH);
            overridePersonalPolicyToggle.setHeight(TOGGLE_HEIGHT);
            overridePersonalPolicyToggle.visible = perms;
        }

        // 应用按钮 - 放在勾选框下方
        int buttonWidth = (layout.contentWidth() - CARD_PADDING * 2 - 8) / 2;
        if (applyToAllButton != null) {
            applyToAllButton.setX(layout.left() + CARD_PADDING);
            applyToAllButton.setY(applyButtonY);
            applyToAllButton.setWidth(buttonWidth);
            applyToAllButton.visible = perms;
        }
        if (applyToAddedButton != null) {
            applyToAddedButton.setX(layout.left() + CARD_PADDING + buttonWidth + 8);
            applyToAddedButton.setY(applyButtonY);
            applyToAddedButton.setWidth(buttonWidth);
            applyToAddedButton.visible = perms;
        }

        y += applySettingsCardHeight + 16;
    }
}
