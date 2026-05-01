package com.poso.neotab.client.screen;

import com.poso.neotab.config.TabConfig;
import com.poso.neotab.network.payload.SaveConfigPayload;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Permissions tab widgets for NeoTabConfigScreen.
 */
public class PermissionsTabManager {

    private static final int ROW_HEIGHT   = 24;
    private static final int INPUT_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 56;
    private static final int ROW_GAP      = 10;

    private final NeoTabConfigScreen screen;

    // ── Permissions tab fields ────────────────────────────────────────────────
    final List<CycleButton<Boolean>> globalPolicyToggles = new ArrayList<>();
    Button permTargetModeButton;
    boolean permTargetIsPlayer = false;
    EditBox playerSearchBox;
    Button permAddButton;
    final List<String> playerSuggestions = new ArrayList<>();
    final LinkedHashMap<UUID, String> targetPlayers = new LinkedHashMap<>();
    final List<Button> targetPlayerRemoveButtons = new ArrayList<>();
    UUID editingPlayerUUID = null;
    final List<CycleButton<Boolean>> personalPolicyToggles = new ArrayList<>();
    Button permSaveButton;

    PermissionsTabManager(NeoTabConfigScreen screen) {
        this.screen = screen;
    }

    void clear() {
        globalPolicyToggles.clear();
        personalPolicyToggles.clear();
        playerSuggestions.clear();
        targetPlayerRemoveButtons.clear();
        permSaveButton = null;
    }

    void initPermissionsWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig initialConfig) {
        PlayerCustomizePolicy global = initialConfig.globalPolicy();

        // Target mode toggle button
        this.permTargetModeButton = screen.addWidget(Button.builder(
            Component.translatable(permTargetIsPlayer
                ? "screen.neotab.policy.mode_player"
                : "screen.neotab.policy.mode_all"),
            btn -> {
                permTargetIsPlayer = !permTargetIsPlayer;
                btn.setMessage(Component.translatable(permTargetIsPlayer
                    ? "screen.neotab.policy.mode_player"
                    : "screen.neotab.policy.mode_all"));
                if (playerSearchBox != null) {
                    playerSearchBox.active = permTargetIsPlayer;
                    playerSearchBox.setEditable(permTargetIsPlayer);
                    if (!permTargetIsPlayer) {
                        playerSearchBox.setValue("");
                        playerSearchBox.setHint(Component.translatable("screen.neotab.policy.input_disabled"));
                    } else {
                        playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));
                    }
                }
                if (permAddButton != null) permAddButton.active = permTargetIsPlayer;
                loadPolicyToggles(initialConfig);
                screen.syncVisibility();
                NeoTabConfigScreenLayout.Layout l = screen.buildLayout();
                screen.applyLayout(l);
            })
            .bounds(layout.left(), 0, 80, INPUT_HEIGHT)
            .build());
        this.permTargetModeButton.visible = false;

        // Player search box
        int modeButtonWidth = 80;
        int addButtonWidth  = 40;
        int searchBoxWidth  = layout.contentWidth() - modeButtonWidth - addButtonWidth - 12;
        this.playerSearchBox = screen.addWidget(
            new EditBox(screen.font(), layout.left() + modeButtonWidth + 4, 0,
                searchBoxWidth, INPUT_HEIGHT,
                Component.translatable("screen.neotab.policy.search_hint")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));
        this.playerSearchBox.active = permTargetIsPlayer;
        this.playerSearchBox.setEditable(permTargetIsPlayer);
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
                        .limit(5)
                        .forEach(playerSuggestions::add);
                }
            }
        });

        // Add button
        this.permAddButton = screen.addWidget(Button.builder(
            Component.translatable("screen.neotab.policy.add"),
            btn -> {
                if (!permTargetIsPlayer || playerSearchBox == null) return;
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
            .bounds(layout.left() + modeButtonWidth + 4 + searchBoxWidth + 4, 0, addButtonWidth, INPUT_HEIGHT)
            .build());
        this.permAddButton.active = permTargetIsPlayer;
        this.permAddButton.visible = false;

        // Policy toggles (15 fields)
        boolean[] globalValues = {
            global.allowTopTitleToggle(),    global.allowTopTitleEdit(),
            global.allowTopContentToggle(),  global.allowTopContentEdit(),
            global.allowPingDisplayToggle(), global.allowDurationToggle(),
            global.allowTitleToggle(),       global.allowHealthDisplayToggle(),
            global.allowHealthModeChange(),  global.allowFooterCustomEdit(),
            global.allowFooterTpsToggle(),   global.allowFooterMsptToggle(),
            global.allowFooterOnlineToggle(), global.allowThemeChange(),
            global.allowRefreshIntervalChange()
        };
        for (boolean val : globalValues) {
            CycleButton<Boolean> toggle = screen.addWidget(
                CycleButton.onOffBuilder(val)
                    .displayOnlyValue()
                    .create(layout.toggleX(), 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                        (btn, v) -> { /* value read on save */ }));
            toggle.visible = false;
            globalPolicyToggles.add(toggle);
        }

        // Save permissions button
        this.permSaveButton = screen.addWidget(Button.builder(
            Component.translatable("screen.neotab.policy.save"),
            btn -> savePermissions(initialConfig))
            .bounds(layout.left(), 0, 120, INPUT_HEIGHT)
            .build());
        this.permSaveButton.visible = false;
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
            buildGlobalPolicyFromToggles(initialConfig),
            buildPlayerPoliciesFromToggles(initialConfig)
        ).sanitized();
        PacketDistributor.sendToServer(new SaveConfigPayload(config));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                Component.translatable("message.neotab.permissions_saved"));
        }
    }

    void rebuildTargetPlayerButtons(NeoTabConfigScreenLayout.Layout layout) {
        for (Button btn : targetPlayerRemoveButtons) screen.removeWidget(btn);
        targetPlayerRemoveButtons.clear();
        boolean perms = screen.getActiveTab() == NeoTabConfigScreen.ConfigTab.PERMISSIONS;
        for (UUID uuid : targetPlayers.keySet()) {
            Button removeBtn = screen.addWidget(Button.builder(
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
            removeBtn.visible = perms && permTargetIsPlayer;
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
            p.allowRefreshIntervalChange()
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

    PlayerCustomizePolicy buildGlobalPolicyFromToggles(TabConfig initialConfig) {
        if (globalPolicyToggles.size() < 15) {
            return initialConfig.globalPolicy();
        }
        PlayerCustomizePolicy built = new PlayerCustomizePolicy(
            globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
            globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
            globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
            globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
            globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
            globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
            globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
            globalPolicyToggles.get(14).getValue()
        );
        if (permTargetIsPlayer && editingPlayerUUID != null) {
            return initialConfig.globalPolicy();
        }
        return built;
    }

    Map<UUID, PlayerCustomizePolicy> buildPlayerPoliciesFromToggles(TabConfig initialConfig) {
        Map<UUID, PlayerCustomizePolicy> policies = new java.util.HashMap<>(initialConfig.playerPolicies());
        if (!permTargetIsPlayer || globalPolicyToggles.size() < 15) {
            return policies;
        }
        PlayerCustomizePolicy current = new PlayerCustomizePolicy(
            globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
            globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
            globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
            globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
            globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
            globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
            globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
            globalPolicyToggles.get(14).getValue()
        );
        for (UUID uuid : targetPlayers.keySet()) {
            policies.put(uuid, current);
        }
        return policies;
    }
    /** Apply layout positions to all permissions tab widgets. */
    void applyLayout(NeoTabConfigScreenLayout.Layout layout, int rowHeight, int rowGap, int inputHeight,
                     int contentTopPadding, int sectionHeaderHeight, int toggleWidth) {
        if (globalPolicyToggles.isEmpty()) return;
        int modeButtonWidth = 80, addButtonWidth = 40;
        int searchBoxWidth = layout.contentWidth() - modeButtonWidth - addButtonWidth - 12;
        int permY = contentTopPadding;
        if (permTargetModeButton != null) { permTargetModeButton.setX(layout.left()); permTargetModeButton.setY(layout.toScreenY(permY)); permTargetModeButton.setWidth(modeButtonWidth); }
        if (playerSearchBox != null) { playerSearchBox.setX(layout.left() + modeButtonWidth + 4); playerSearchBox.setY(layout.toScreenY(permY)); playerSearchBox.setWidth(searchBoxWidth); }
        if (permAddButton != null) { permAddButton.setX(layout.left() + modeButtonWidth + 4 + searchBoxWidth + 4); permAddButton.setY(layout.toScreenY(permY)); }
        permY += rowHeight + rowGap + rowHeight;
        int tagAreaH = inputHeight;
        if (permTargetIsPlayer && !targetPlayers.isEmpty()) {
            int tagX = layout.left() + 3, tagY = layout.toScreenY(permY) + 2, tagH = tagAreaH - 2, tagPadX = 4, removeW = 14, btnIdx = 0;
            net.minecraft.client.gui.Font font = screen.font();
            for (UUID uuid : targetPlayers.keySet()) {
                if (btnIdx >= targetPlayerRemoveButtons.size()) break;
                String name = targetPlayers.get(uuid);
                int nameW = font.width(name), tagW = nameW + tagPadX * 2 + removeW + 2;
                if (tagX + tagW > layout.right() - 8) break;
                Button removeBtn = targetPlayerRemoveButtons.get(btnIdx);
                removeBtn.setX(tagX + tagPadX + nameW + 2); removeBtn.setY(tagY + (tagH - inputHeight) / 2); removeBtn.setWidth(removeW); removeBtn.setHeight(inputHeight);
                tagX += tagW + 3; btnIdx++;
            }
        }
        permY += tagAreaH + 4 + sectionHeaderHeight;
        int itemGap = 4, itemW = (layout.contentWidth() - 6 - itemGap) / 2, itemH = inputHeight + 2, itemRowGap = 2, togglePad = 3;
        for (int i = 0; i < globalPolicyToggles.size(); i++) {
            int col = i % 2, row = i / 2;
            int rowContentY = permY + row * (itemH + itemRowGap);
            int itemX = layout.left() + col * (itemW + itemGap);
            int itemScreenY = layout.toScreenY(rowContentY);
            CycleButton<Boolean> toggle = globalPolicyToggles.get(i);
            toggle.setX(itemX + itemW - toggleWidth - togglePad);
            toggle.setY(itemScreenY + (itemH - inputHeight) / 2);
        }
        int permRowCount = (globalPolicyToggles.size() + 1) / 2;
        int saveButtonY = permY + permRowCount * (itemH + itemRowGap) + rowGap;
        if (permSaveButton != null) { permSaveButton.setX(layout.left()); permSaveButton.setY(layout.toScreenY(saveButtonY)); permSaveButton.setWidth(120); }
    }

}
