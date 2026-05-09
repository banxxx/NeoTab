package com.poso.neotab.client.screen;

import com.poso.neotab.NeoTab;
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
    boolean permTargetIsPlayer = false;
    EditBox playerSearchBox;
    Button permAddButton;
    final List<String> playerSuggestions = new ArrayList<>();
    int dropdownScrollOffset = 0;
    final LinkedHashMap<UUID, String> targetPlayers = new LinkedHashMap<>();
    final List<Button> targetPlayerRemoveButtons = new ArrayList<>();
    UUID editingPlayerUUID = null;
    final List<CycleButton<Boolean>> personalPolicyToggles = new ArrayList<>();
    Button permSaveButton;
    Button applyToAllButton;
    Button applyToAddedButton;

    // 新增：覆盖个人策略勾选框
    CycleButton<Boolean> overridePersonalPolicyToggle;

    PermissionsTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void clear() {
        globalPolicyToggles.clear();
        personalPolicyToggles.clear();
        playerSuggestions.clear();
        targetPlayerRemoveButtons.clear();
        permSaveButton = null;
        applyToAllButton = null;
        applyToAddedButton = null;
        overridePersonalPolicyToggle = null;
    }

    void initPermissionsWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig initialConfig) {
        PlayerCustomizePolicy global = initialConfig.globalPolicy();

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

        // Apply to all players button
        this.applyToAllButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.permissions.apply_to_all"),
                        btn -> applyToAllPlayers(initialConfig))
                .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
                .build());
        this.applyToAllButton.visible = false;
        this.applyToAllButton.active = true;

        // Apply to added players button
        this.applyToAddedButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.permissions.apply_to_added"),
                        btn -> applyToAddedPlayers(initialConfig))
                .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
                .build());
        this.applyToAddedButton.visible = false;
        this.applyToAddedButton.active = true;

        // Save permissions button (obsolete, kept hidden)
        this.permSaveButton = ScreenAccessHelper.addWidget(screen, Button.builder(
                        Component.translatable("screen.neotab.permissions.save_config"),
                        btn -> savePermissions(initialConfig))
                .bounds(layout.left(), 0, 120, INPUT_HEIGHT)
                .build());
        this.permSaveButton.visible = false;
        this.permSaveButton.active = true;
    }

    void savePermissions(TabConfig initialConfig) {
        TabConfig config = new TabConfig(
                initialConfig.topTitleEnabled(),
                initialConfig.topTitleText(),
                initialConfig.topContentEnabled(),
                initialConfig.topContentText(),
                initialConfig.betterPingEnabled(),
                initialConfig.onlineDurationEnabled(),
                initialConfig.titleEnabled(),
                initialConfig.healthDisplayEnabled(),
                initialConfig.healthDisplayMode(),
                initialConfig.tabTheme(),
                initialConfig.footerCustomText(),
                initialConfig.footerTpsEnabled(),
                initialConfig.footerMsptEnabled(),
                initialConfig.footerOnlineEnabled(),
                initialConfig.refreshIntervalTicks(),
                buildGlobalPolicyFromToggles(),
                buildPlayerPoliciesFromToggles()
        ).sanitized();
        com.poso.neotab.network.NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new SaveConfigPacket(config));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable("message.neotab.permissions_saved"));
        }
    }

    void applyToAllPlayers(TabConfig initialConfig) {
        for (int i = 0; i < globalPolicyToggles.size(); i++) {
            NeoTab.LOGGER.info("Toggle {} value: {}", i, globalPolicyToggles.get(i).getValue());
        }
        TabConfig config = new TabConfig(
                initialConfig.topTitleEnabled(),
                initialConfig.topTitleText(),
                initialConfig.topContentEnabled(),
                initialConfig.topContentText(),
                initialConfig.betterPingEnabled(),
                initialConfig.onlineDurationEnabled(),
                initialConfig.titleEnabled(),
                initialConfig.healthDisplayEnabled(),
                initialConfig.healthDisplayMode(),
                initialConfig.tabTheme(),
                initialConfig.footerCustomText(),
                initialConfig.footerTpsEnabled(),
                initialConfig.footerMsptEnabled(),
                initialConfig.footerOnlineEnabled(),
                initialConfig.refreshIntervalTicks(),
                buildGlobalPolicyFromToggles(),
                // 根据勾选框决定是否清空个人策略
                overridePersonalPolicyToggle.getValue() ? new HashMap<>() : initialConfig.playerPolicies()
        ).sanitized();
        NeoTab.LOGGER.info("Build policy from toggles: {}", buildGlobalPolicyFromToggles());
        com.poso.neotab.network.NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new SaveConfigPacket(config));
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
        TabConfig config = new TabConfig(
                initialConfig.topTitleEnabled(),
                initialConfig.topTitleText(),
                initialConfig.topContentEnabled(),
                initialConfig.topContentText(),
                initialConfig.betterPingEnabled(),
                initialConfig.onlineDurationEnabled(),
                initialConfig.titleEnabled(),
                initialConfig.healthDisplayEnabled(),
                initialConfig.healthDisplayMode(),
                initialConfig.tabTheme(),
                initialConfig.footerCustomText(),
                initialConfig.footerTpsEnabled(),
                initialConfig.footerMsptEnabled(),
                initialConfig.footerOnlineEnabled(),
                initialConfig.refreshIntervalTicks(),
                initialConfig.globalPolicy(),  // 保持全局策略不变
                buildPlayerPoliciesFromToggles()  // 只更新已添加玩家的策略
        ).sanitized();
        com.poso.neotab.network.NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new SaveConfigPacket(config));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    Component.literal("§a已应用权限设置到 " + targetPlayers.size() + " 个玩家"));
        }
    }

    void rebuildTargetPlayerButtons(NeoTabConfigScreenLayout.Layout layout) {
        for (Button btn : targetPlayerRemoveButtons) ScreenAccessHelper.removeWidget(screen, btn);
        targetPlayerRemoveButtons.clear();
        boolean perms = screen.getActiveTab() == NeoTabConfigScreen.ConfigTab.PERMISSIONS;
        for (UUID uuid : targetPlayers.keySet()) {
            Button removeBtn = ScreenAccessHelper.addWidget(screen, Button.builder(
                            Component.literal("×"),
                            btn -> {
                                targetPlayers.remove(uuid);
                                if (uuid.equals(editingPlayerUUID)) {
                                    editingPlayerUUID = null;
                                    loadPolicyToggles(screen.getInitialConfig());
                                }
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

    void loadPolicyToggles(TabConfig initialConfig) {
        PlayerCustomizePolicy p;
        if (permTargetIsPlayer && editingPlayerUUID != null) {
            p = initialConfig.playerPolicies().getOrDefault(editingPlayerUUID,
                    PlayerCustomizePolicy.locked());
        } else {
            p = initialConfig.globalPolicy();
        }
        boolean[] values = {
                p.allowTopTitleToggle(),    p.allowTopTitleEdit(),
                p.allowTopContentToggle(),  p.allowTopContentEdit(),
                p.allowPingDisplayToggle(), p.allowDurationToggle(),
                p.allowTitleToggle(),       p.allowHealthDisplayToggle(),
                p.allowHealthModeChange(),  p.allowFooterCustomEdit(),
                p.allowFooterTpsToggle(),   p.allowFooterMsptToggle(),
                p.allowFooterOnlineToggle(), p.allowThemeChange(),
                // p.allowRefreshIntervalChange()
        };
        for (int i = 0; i < Math.min(values.length, globalPolicyToggles.size()); i++) {
            CycleButton<Boolean> toggle = globalPolicyToggles.get(i);
            if (!toggle.getValue().equals(values[i])) toggle.onPress();
        }
    }

    void refreshPersonalPolicyToggles(UUID uuid, TabConfig initialConfig) {
        this.editingPlayerUUID = uuid;
        loadPolicyToggles(initialConfig);
        NeoTabConfigScreenLayout.Layout layout = screen.buildLayout();
        screen.applyLayout(layout);
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

    Map<UUID, PlayerCustomizePolicy> buildPlayerPoliciesFromToggles() {
        Map<UUID, PlayerCustomizePolicy> policies = new HashMap<>();
        if (!targetPlayers.isEmpty()) {
            PlayerCustomizePolicy policy = buildGlobalPolicyFromToggles();
            for (UUID uuid : targetPlayers.keySet()) {
                policies.put(uuid, policy);
            }
        }
        return policies;
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
        if (overridePersonalPolicyToggle != null) {
            // 开关放在左侧
            overridePersonalPolicyToggle.setX(layout.left() + CARD_PADDING);
            overridePersonalPolicyToggle.setY(layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8);
            overridePersonalPolicyToggle.setWidth(TOGGLE_WIDTH);
            overridePersonalPolicyToggle.setHeight(TOGGLE_HEIGHT);
            overridePersonalPolicyToggle.visible = true;
        }

        // 应用按钮 - 放在勾选框下方
        int buttonWidth = (layout.contentWidth() - CARD_PADDING * 2 - 8) / 2;
        if (applyToAllButton != null) {
            applyToAllButton.setX(layout.left() + CARD_PADDING);
            applyToAllButton.setY(applyButtonY);
            applyToAllButton.setWidth(buttonWidth);
            applyToAllButton.visible = true;
        }
        if (applyToAddedButton != null) {
            applyToAddedButton.setX(layout.left() + CARD_PADDING + buttonWidth + 8);
            applyToAddedButton.setY(applyButtonY);
            applyToAddedButton.setWidth(buttonWidth);
            applyToAddedButton.visible = true;
        }

        y += applySettingsCardHeight + 16;

        if (permSaveButton != null) {
            permSaveButton.visible = false;
        }
    }
}