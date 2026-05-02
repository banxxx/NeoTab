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
    int dropdownScrollOffset = 0;  // 下拉菜单滚动偏移
    final LinkedHashMap<UUID, String> targetPlayers = new LinkedHashMap<>();
    final List<Button> targetPlayerRemoveButtons = new ArrayList<>();
    UUID editingPlayerUUID = null;
    final List<CycleButton<Boolean>> personalPolicyToggles = new ArrayList<>();
    Button permSaveButton;
    Button applyToAllButton;  // 应用到全部玩家按钮
    Button applyToAddedButton;  // 应用到已添加玩家按钮

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
    }

    void initPermissionsWidgets(NeoTabConfigScreenLayout.Layout layout, TabConfig initialConfig) {
        PlayerCustomizePolicy global = initialConfig.globalPolicy();

        // Player search box（新设计中始终可用，不需要模式切换）
        this.playerSearchBox = screen.addWidget(
            new EditBox(screen.font(), layout.left(), 0,
                layout.contentWidth(), INPUT_HEIGHT,
                Component.literal("输入玩家名称...")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.literal("输入玩家名称..."));
        this.playerSearchBox.active = true;  // 始终可用
        this.playerSearchBox.setEditable(true);  // 始终可编辑
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
                        // 移除limit限制，显示所有匹配的玩家
                        .forEach(playerSuggestions::add);
                }
            }
        });

        // Add button
        this.permAddButton = screen.addWidget(Button.builder(
            Component.literal("添加"),
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
        this.permAddButton.active = true;  // 始终可用
        this.permAddButton.visible = false;

        // Policy toggles (14 fields - 移除了最后一个"刷新间隔"权限)
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
            CycleButton<Boolean> toggle = screen.addWidget(
                CycleButton.onOffBuilder(val)
                    .displayOnlyValue()
                    .create(layout.toggleX(), 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                        (btn, v) -> { /* value read on save */ }));
            toggle.visible = false;
            toggle.active = true;  // 确保可点击
            globalPolicyToggles.add(toggle);
        }

        // Save permissions button
        this.permSaveButton = screen.addWidget(Button.builder(
            Component.literal("保存权限配置"),
            btn -> savePermissions(initialConfig))
            .bounds(layout.left(), 0, 120, INPUT_HEIGHT)
            .build());
        this.permSaveButton.visible = false;
        this.permSaveButton.active = true;  // 确保可点击
        
        // Apply to all players button (应用到全部玩家)
        this.applyToAllButton = screen.addWidget(Button.builder(
            Component.literal("应用到全部玩家"),
            btn -> applyToAllPlayers(initialConfig))
            .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
            .build());
        this.applyToAllButton.visible = false;
        this.applyToAllButton.active = true;
        
        // Apply to added players button (应用到已添加玩家)
        this.applyToAddedButton = screen.addWidget(Button.builder(
            Component.literal("应用到已添加玩家"),
            btn -> applyToAddedPlayers(initialConfig))
            .bounds(layout.left(), 0, 140, INPUT_HEIGHT)
            .build());
        this.applyToAddedButton.visible = false;
        this.applyToAddedButton.active = true;
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
    
    void applyToAllPlayers(TabConfig initialConfig) {
        // 应用到全部玩家：使用全局策略
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
                Component.literal("§a已应用权限设置到全部玩家"));
        }
    }
    
    void applyToAddedPlayers(TabConfig initialConfig) {
        // 应用到已添加玩家：只更新已添加玩家的权限
        if (targetPlayers.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                    Component.literal("§c请先添加玩家后再应用权限设置"));
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
            buildPlayerPoliciesFromToggles(initialConfig)  // 只更新玩家策略
        ).sanitized();
        PacketDistributor.sendToServer(new SaveConfigPayload(config));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                Component.literal("§a已应用权限设置到 " + targetPlayers.size() + " 个玩家"));
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
            removeBtn.visible = perms;  // 在权限配置页面始终可见
            removeBtn.active = true;  // 确保可点击
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
        
        int CARD_PADDING = 10;
        int CARD_GAP = 8;
        int titleLineHeight = 9;  // font.lineHeight
        int subtitleLineHeight = 9;
        
        int y = contentTopPadding;
        
        // 全局策略分区标题
        y += sectionHeaderHeight;
        
        // 全局策略权限卡片（2列网格布局）
        int cardWidth = (layout.contentWidth() - CARD_GAP) / 2;
        int cardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + CARD_PADDING;
        
        for (int i = 0; i < Math.min(14, globalPolicyToggles.size()); i++) {  // 只有14个权限项
            int col = i % 2;
            int row = i / 2;
            
            int cardX = layout.left() + col * (cardWidth + CARD_GAP);
            int cardY = layout.toScreenY(y + row * (cardHeight + CARD_GAP));
            
            // 定位开关按钮到卡片右上角
            CycleButton<Boolean> toggle = globalPolicyToggles.get(i);
            toggle.setX(cardX + cardWidth - CARD_PADDING - toggleWidth);
            toggle.setY(cardY + CARD_PADDING);
            toggle.setWidth(toggleWidth);
        }
        
        // 移动到下一行（7行卡片）
        int rowCount = (14 + 1) / 2;  // 向上取整
        y += rowCount * (cardHeight + CARD_GAP) - CARD_GAP + 16;  // 最后一行不需要GAP，然后加SECTION_GAP
        
        // 指定玩家策略分区标题
        y += sectionHeaderHeight;
        
        // 添加玩家卡片
        int addPlayerCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + inputHeight + CARD_PADDING;
        int inputY = layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
        
        // 输入框和添加按钮（在卡片内容区域）
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
        
        // 玩家列表显示卡片
        // 计算标签布局所需的高度
        int tagHeight = inputHeight;
        int tagGap = 6;
        int maxWidth = layout.contentWidth() - CARD_PADDING * 2;
        int tagX = 0;  // 相对于内容区域的X坐标
        int tagY = 0;  // 相对于内容区域的Y坐标
        int playerListContentHeight;
        
        if (targetPlayers.isEmpty()) {
            playerListContentHeight = 30;  // 降低空状态高度
        } else {
            // 计算标签布局的实际高度
            for (java.util.Map.Entry<java.util.UUID, String> entry : targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = 50;  // 估算，实际会在渲染时计算
                try {
                    nameWidth = screen.font().width(playerName);
                } catch (Exception e) {
                    // 如果无法获取字体，使用估算值
                }
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                
                // 检查是否需要换行
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                
                tagX += tagWidth + tagGap;
            }
            // 内容高度 = 最后一行的Y坐标 + 标签高度（不需要额外的CARD_PADDING）
            playerListContentHeight = tagY + tagHeight;
        }
        
        int playerListCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + playerListContentHeight + CARD_PADDING;
        
        // 定位玩家列表中的删除按钮（标签样式，右对齐）
        if (!targetPlayers.isEmpty()) {
            int listContentY = layout.toScreenY(y) + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8;
            tagX = 0;
            tagY = 0;
            int buttonIndex = 0;
            
            for (java.util.Map.Entry<java.util.UUID, String> entry : targetPlayers.entrySet()) {
                String playerName = entry.getValue();
                int nameWidth = screen.font().width(playerName);
                int deleteButtonWidth = 16;
                int tagPadding = 6;
                int tagWidth = tagPadding + nameWidth + 4 + deleteButtonWidth + tagPadding;
                
                // 检查是否需要换行
                if (tagX + tagWidth > maxWidth && tagX > 0) {
                    tagX = 0;
                    tagY += tagHeight + tagGap;
                }
                
                // 定位删除按钮到标签右侧（内部右对齐）
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
        
        // 提示卡片（无控件）
        int hintCardHeight = CARD_PADDING + titleLineHeight + 2 + (int)(subtitleLineHeight * 2 * 0.82f) + CARD_PADDING;
        y += hintCardHeight + 16;  // 提示卡片后加间距
        
        // 应用权限设置卡片
        int applySettingsCardHeight = CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8 + inputHeight + CARD_PADDING;
        int applySettingsCardY = layout.toScreenY(y);
        
        // 应用到全部玩家按钮（左侧）
        if (applyToAllButton != null) {
            int buttonWidth = (layout.contentWidth() - CARD_PADDING * 2 - 8) / 2;  // 两个按钮平分宽度
            applyToAllButton.setX(layout.left() + CARD_PADDING);
            applyToAllButton.setY(applySettingsCardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8);
            applyToAllButton.setWidth(buttonWidth);
            applyToAllButton.visible = true;
        }
        
        // 应用到已添加玩家按钮（右侧）
        if (applyToAddedButton != null) {
            int buttonWidth = (layout.contentWidth() - CARD_PADDING * 2 - 8) / 2;
            applyToAddedButton.setX(layout.left() + CARD_PADDING + buttonWidth + 8);
            applyToAddedButton.setY(applySettingsCardY + CARD_PADDING + titleLineHeight + 2 + subtitleLineHeight + 8);
            applyToAddedButton.setWidth(buttonWidth);
            applyToAddedButton.visible = true;
        }
        
        y += applySettingsCardHeight + 16;
        
        // 保存按钮（页面底部，已废弃）
        if (permSaveButton != null) {
            permSaveButton.visible = false;  // 隐藏旧的保存按钮
        }
        
        // 隐藏不再使用的控件
        if (permTargetModeButton != null) permTargetModeButton.visible = false;
        if (permSaveButton != null) permSaveButton.visible = false;
    }

}
