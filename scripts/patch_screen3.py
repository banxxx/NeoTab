import sys

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "r", encoding="utf-8") as f:
    content = f.read()

errors = []

# ── Step 5: Update initFooterAndFinalize body ─────────────────────────────────
old5 = """        this.footerCustomInput.setValue(cfg.footerCustomText());
        this.footerTpsEnabled = addRenderableWidget(newLabeledToggle(layout.footerFirstColumnX(), layout.footerColumnWidth(), initialConfig.footerTpsEnabled(), Component.translatable("screen.neotab.footer.tps")));
        this.footerMsptEnabled = addRenderableWidget(newLabeledToggle(layout.footerSecondColumnX(), layout.footerColumnWidth(), initialConfig.footerMsptEnabled(), Component.translatable("screen.neotab.footer.mspt")));
        this.footerOnlineEnabled = addRenderableWidget(newLabeledToggle(layout.footerThirdColumnX(), layout.footerColumnWidth(), initialConfig.footerOnlineEnabled(), Component.translatable("screen.neotab.footer.online")));"""

new5 = """        this.footerCustomInput.setValue(cfg.footerCustomText());
        applyPolicyToWidget(footerCustomInput, policy.allowFooterCustomEdit());
        this.footerTpsEnabled = addRenderableWidget(newLabeledToggle(layout.footerFirstColumnX(), layout.footerColumnWidth(), cfg.footerTpsEnabled(), Component.translatable("screen.neotab.footer.tps")));
        applyPolicyToWidget(footerTpsEnabled, policy.allowFooterTpsToggle());
        this.footerMsptEnabled = addRenderableWidget(newLabeledToggle(layout.footerSecondColumnX(), layout.footerColumnWidth(), cfg.footerMsptEnabled(), Component.translatable("screen.neotab.footer.mspt")));
        applyPolicyToWidget(footerMsptEnabled, policy.allowFooterMsptToggle());
        this.footerOnlineEnabled = addRenderableWidget(newLabeledToggle(layout.footerThirdColumnX(), layout.footerColumnWidth(), cfg.footerOnlineEnabled(), Component.translatable("screen.neotab.footer.online")));
        applyPolicyToWidget(footerOnlineEnabled, policy.allowFooterOnlineToggle());"""

if old5 in content:
    content = content.replace(old5, new5, 1)
    print("Step 5 OK")
else:
    errors.append("Step 5 FAIL: footer body not found")

# ── Step 6: syncTabWidgetVisibility ──────────────────────────────────────────
old6 = """    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;"""

new6 = """    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : globalPolicyToggles) btn.visible = perms;
        for (CycleButton<Boolean> btn : personalPolicyToggles) btn.visible = perms;
        if (playerSearchBox != null) playerSearchBox.visible = perms;"""

if old6 in content:
    content = content.replace(old6, new6, 1)
    print("Step 6 OK")
else:
    errors.append("Step 6 FAIL: syncTabWidgetVisibility not found")

# ── Step 7: renderTabBar ──────────────────────────────────────────────────────
old7 = """        for (int i = 0; i < ConfigTab.values().length; i++) {
                ConfigTab tab = ConfigTab.values()[i];
                int btnY    = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);"""

new7 = """        int tabIndex = 0;
            for (ConfigTab tab : ConfigTab.values()) {
                // PERMISSIONS tab is only visible in ADMIN mode
                if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;
                int btnY    = VIEWPORT_TOP + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);"""

if old7 in content:
    content = content.replace(old7, new7, 1)
    print("Step 7a OK")
else:
    errors.append("Step 7a FAIL")

old7b = """                g.drawString(font, label, textX, textY, textColor, false);
            }"""
new7b = """                g.drawString(font, label, textX, textY, textColor, false);
                tabIndex++;
            }"""

# Only replace the first occurrence (inside renderTabBar)
idx = content.find(old7b)
if idx != -1:
    content = content[:idx] + new7b + content[idx+len(old7b):]
    print("Step 7b OK")
else:
    errors.append("Step 7b FAIL")

# ── Step 8: mouseClicked tab switching ───────────────────────────────────────
old8 = """            if (mouseX >= tabBtnX && mouseX <= tabBtnX + tabBtnW) {
                ConfigTab[] tabs = ConfigTab.values();
                for (int i = 0; i < tabs.length; i++) {
                    int btnY = VIEWPORT_TOP + i * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                    if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                        switchTab(tabs[i]);
                        return true;
                    }
                }
            }"""

new8 = """            if (mouseX >= tabBtnX && mouseX <= tabBtnX + tabBtnW) {
                int tabIndex = 0;
                for (ConfigTab tab : ConfigTab.values()) {
                    if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;
                    int btnY = VIEWPORT_TOP + tabIndex * (TAB_BUTTON_HEIGHT + TAB_BUTTON_GAP);
                    if (mouseY >= btnY && mouseY <= btnY + TAB_BUTTON_HEIGHT) {
                        // Extra permission check when clicking PERMISSIONS tab
                        if (tab == ConfigTab.PERMISSIONS) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player == null || !mc.player.hasPermissions(2)) {
                                mc.player.sendSystemMessage(Component.translatable("message.neotab.no_permission"));
                                return true;
                            }
                        }
                        switchTab(tab);
                        return true;
                    }
                    tabIndex++;
                }
            }"""

if old8 in content:
    content = content.replace(old8, new8, 1)
    print("Step 8 OK")
else:
    errors.append("Step 8 FAIL: mouseClicked tab switching not found")

# ── Step 9: save() ────────────────────────────────────────────────────────────
old9 = """    private void save() {
        TabConfig config = new TabConfig(
            this.topTitleEnabled.getValue(),
            this.topTitleInput.getValue(),
            this.topContentEnabled.getValue(),
            this.topContentInput.getValue(),
            this.betterPingEnabled.getValue(),
            this.onlineDurationEnabled.getValue(),
            this.titleEnabled.getValue(),
            this.healthDisplayEnabled.getValue(),
            this.healthDisplayMode.getValue(),
            this.selectedThemeId,
            this.footerCustomInput.getValue(),
            this.footerTpsEnabled.getValue(),
            this.footerMsptEnabled.getValue(),
            this.footerOnlineEnabled.getValue(),
            this.initialConfig.refreshIntervalTicks(),
            // 策略字段从初始配置继承，管理员保存时不丢失已有策略
            this.initialConfig.globalPolicy(),
            this.initialConfig.playerPolicies()
        ).sanitized();
        PacketDistributor.sendToServer(new SaveConfigPayload(config));
        onClose();
    }"""

new9 = """    private void save() {
        if (screenMode == ScreenMode.PLAYER) {
            // PLAYER mode: build and send personal config
            com.poso.neotab.config.PlayerTabConfig playerCfg = new com.poso.neotab.config.PlayerTabConfig(
                Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : java.util.UUID.randomUUID(),
                policy.allowTopTitleToggle()      ? this.topTitleEnabled.getValue()       : null,
                policy.allowTopTitleEdit()        ? this.topTitleInput.getValue()         : null,
                policy.allowTopContentToggle()    ? this.topContentEnabled.getValue()     : null,
                policy.allowTopContentEdit()      ? this.topContentInput.getValue()       : null,
                policy.allowPingDisplayToggle()   ? this.betterPingEnabled.getValue()     : null,
                policy.allowDurationToggle()      ? this.onlineDurationEnabled.getValue() : null,
                policy.allowTitleToggle()         ? this.titleEnabled.getValue()          : null,
                policy.allowHealthDisplayToggle() ? this.healthDisplayEnabled.getValue()  : null,
                policy.allowHealthModeChange()    ? this.healthDisplayMode.getValue()     : null,
                policy.allowFooterCustomEdit()    ? this.footerCustomInput.getValue()     : null,
                policy.allowFooterTpsToggle()     ? this.footerTpsEnabled.getValue()      : null,
                policy.allowFooterMsptToggle()    ? this.footerMsptEnabled.getValue()     : null,
                policy.allowFooterOnlineToggle()  ? this.footerOnlineEnabled.getValue()   : null,
                policy.allowThemeChange()         ? this.selectedThemeId                 : null
            );
            PacketDistributor.sendToServer(new SavePlayerConfigPayload(playerCfg));
        } else {
            // ADMIN mode: build and send server config (includes policy from permissions tab)
            TabConfig config = new TabConfig(
                this.topTitleEnabled.getValue(),
                this.topTitleInput.getValue(),
                this.topContentEnabled.getValue(),
                this.topContentInput.getValue(),
                this.betterPingEnabled.getValue(),
                this.onlineDurationEnabled.getValue(),
                this.titleEnabled.getValue(),
                this.healthDisplayEnabled.getValue(),
                this.healthDisplayMode.getValue(),
                this.selectedThemeId,
                this.footerCustomInput.getValue(),
                this.footerTpsEnabled.getValue(),
                this.footerMsptEnabled.getValue(),
                this.footerOnlineEnabled.getValue(),
                this.initialConfig.refreshIntervalTicks(),
                buildGlobalPolicyFromToggles(),
                buildPlayerPoliciesFromToggles()
            ).sanitized();
            PacketDistributor.sendToServer(new SaveConfigPayload(config));
        }
        onClose();
    }

    /**
     * Build global policy from the permissions tab toggles.
     * Falls back to initialConfig.globalPolicy() if toggles not yet initialized.
     */
    private com.poso.neotab.permission.PlayerCustomizePolicy buildGlobalPolicyFromToggles() {
        if (globalPolicyToggles.size() < 15) {
            return initialConfig.globalPolicy();
        }
        return new com.poso.neotab.permission.PlayerCustomizePolicy(
            globalPolicyToggles.get(0).getValue(),  globalPolicyToggles.get(1).getValue(),
            globalPolicyToggles.get(2).getValue(),  globalPolicyToggles.get(3).getValue(),
            globalPolicyToggles.get(4).getValue(),  globalPolicyToggles.get(5).getValue(),
            globalPolicyToggles.get(6).getValue(),  globalPolicyToggles.get(7).getValue(),
            globalPolicyToggles.get(8).getValue(),  globalPolicyToggles.get(9).getValue(),
            globalPolicyToggles.get(10).getValue(), globalPolicyToggles.get(11).getValue(),
            globalPolicyToggles.get(12).getValue(), globalPolicyToggles.get(13).getValue(),
            globalPolicyToggles.get(14).getValue()
        );
    }

    /**
     * Build player policies map from the permissions tab personal policy toggles.
     * Falls back to initialConfig.playerPolicies() if no player is being edited.
     */
    private java.util.Map<java.util.UUID, com.poso.neotab.permission.PlayerCustomizePolicy> buildPlayerPoliciesFromToggles() {
        java.util.Map<java.util.UUID, com.poso.neotab.permission.PlayerCustomizePolicy> policies =
            new java.util.HashMap<>(initialConfig.playerPolicies());
        if (editingPlayerUUID != null && personalPolicyToggles.size() >= 15) {
            com.poso.neotab.permission.PlayerCustomizePolicy personal = new com.poso.neotab.permission.PlayerCustomizePolicy(
                personalPolicyToggles.get(0).getValue(),  personalPolicyToggles.get(1).getValue(),
                personalPolicyToggles.get(2).getValue(),  personalPolicyToggles.get(3).getValue(),
                personalPolicyToggles.get(4).getValue(),  personalPolicyToggles.get(5).getValue(),
                personalPolicyToggles.get(6).getValue(),  personalPolicyToggles.get(7).getValue(),
                personalPolicyToggles.get(8).getValue(),  personalPolicyToggles.get(9).getValue(),
                personalPolicyToggles.get(10).getValue(), personalPolicyToggles.get(11).getValue(),
                personalPolicyToggles.get(12).getValue(), personalPolicyToggles.get(13).getValue(),
                personalPolicyToggles.get(14).getValue()
            );
            policies.put(editingPlayerUUID, personal);
        }
        return policies;
    }"""

if old9 in content:
    content = content.replace(old9, new9, 1)
    print("Step 9 OK")
else:
    errors.append("Step 9 FAIL: save() not found")

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "w", encoding="utf-8") as f:
    f.write(content)

if errors:
    for e in errors:
        print(e)
    sys.exit(1)
else:
    print("All steps done.")
