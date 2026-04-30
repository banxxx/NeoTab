import sys

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "r", encoding="utf-8") as f:
    content = f.read()

errors = []

# ── 1. Add permissions layout to applyWidgetLayout (before doneButton) ────────
old1 = """        placeScrollableWidget(this.footerTpsEnabled,     layout.footerFirstColumnX(),  layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerMsptEnabled,    layout.footerSecondColumnX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerOnlineEnabled,  layout.footerThirdColumnX(),  layout.toScreenY(layout.footerRowY()));
        this.doneButton.setX(layout.doneButtonX());"""

new1 = """        placeScrollableWidget(this.footerTpsEnabled,     layout.footerFirstColumnX(),  layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerMsptEnabled,    layout.footerSecondColumnX(), layout.toScreenY(layout.footerRowY()));
        placeScrollableWidget(this.footerOnlineEnabled,  layout.footerThirdColumnX(),  layout.toScreenY(layout.footerRowY()));

        // ── Permissions tab layout ────────────────────────────────────────────
        if (!globalPolicyToggles.isEmpty()) {
            // Policy field labels (key order matches PlayerCustomizePolicy constructor)
            String[] policyKeys = {
                "screen.neotab.policy.top_title_toggle",   "screen.neotab.policy.top_title_edit",
                "screen.neotab.policy.top_content_toggle", "screen.neotab.policy.top_content_edit",
                "screen.neotab.policy.ping_toggle",        "screen.neotab.policy.duration_toggle",
                "screen.neotab.policy.title_toggle",       "screen.neotab.policy.health_toggle",
                "screen.neotab.policy.health_mode",        "screen.neotab.policy.footer_custom",
                "screen.neotab.policy.footer_tps",         "screen.neotab.policy.footer_mspt",
                "screen.neotab.policy.footer_online",      "screen.neotab.policy.theme",
                "screen.neotab.policy.refresh_interval"
            };
            int permY = CONTENT_TOP_PADDING + SECTION_HEADER_HEIGHT; // after global section header
            for (int i = 0; i < globalPolicyToggles.size(); i++) {
                placeScrollableWidget(globalPolicyToggles.get(i), layout.toggleX(), layout.toScreenY(permY));
                permY += ROW_HEIGHT + ROW_GAP;
            }
            // Personal policy section
            permY += SECTION_HEADER_HEIGHT; // personal section header
            if (playerSearchBox != null) {
                placeScrollableWidget(playerSearchBox, layout.left(), layout.toScreenY(permY));
            }
            permY += ROW_HEIGHT + ROW_GAP;
            for (int i = 0; i < personalPolicyToggles.size(); i++) {
                placeScrollableWidget(personalPolicyToggles.get(i), layout.toggleX(), layout.toScreenY(permY));
                permY += ROW_HEIGHT + ROW_GAP;
            }
        }

        this.doneButton.setX(layout.doneButtonX());"""

if old1 in content:
    content = content.replace(old1, new1, 1)
    print("Step 1 OK: permissions layout in applyWidgetLayout")
else:
    errors.append("Step 1 FAIL")

# ── 2. Fix contentHeight calculation to handle PERMISSIONS tab ────────────────
old2 = """        // Content height for the active tab
        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            contentHeight = footerRowY + ROW_HEIGHT;
        } else {
            // Theme tab: 主题选择器 + 自定义配置（如有）+ 血量显示section + 布局分列section
            contentHeight = layoutButtonsY + INPUT_HEIGHT;
        }"""

new2 = """        // Content height for the active tab
        // Permissions tab: global section header + 15 rows + personal section header + search row + 15 rows
        int permissionsContentHeight = SECTION_HEADER_HEIGHT
            + 15 * (ROW_HEIGHT + ROW_GAP)
            + SECTION_HEADER_HEIGHT
            + (ROW_HEIGHT + ROW_GAP)
            + 15 * (ROW_HEIGHT + ROW_GAP);

        int contentHeight;
        if (activeTab == ConfigTab.PAGE_CONFIG) {
            contentHeight = footerRowY + ROW_HEIGHT;
        } else if (activeTab == ConfigTab.PERMISSIONS) {
            contentHeight = CONTENT_TOP_PADDING + permissionsContentHeight;
        } else {
            // Theme tab: 主题选择器 + 自定义配置（如有）+ 血量显示section + 布局分列section
            contentHeight = layoutButtonsY + INPUT_HEIGHT;
        }"""

if old2 in content:
    content = content.replace(old2, new2, 1)
    print("Step 2 OK: contentHeight handles PERMISSIONS tab")
else:
    errors.append("Step 2 FAIL")

# ── 3. Add renderPermissionsContent method (before renderScrollableWidgets) ───
old3 = """    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {"""

new3 = """    /**
     * Render the permissions configuration tab content.
     * Shows section headers and labels for each policy field.
     * The actual toggle widgets are rendered by renderScrollableWidgets.
     */
    private void renderPermissionsContent(GuiGraphics g, int mouseX, int mouseY, Layout layout) {
        // Policy field display names (order matches PlayerCustomizePolicy constructor)
        String[] policyKeys = {
            "screen.neotab.policy.top_title_toggle",   "screen.neotab.policy.top_title_edit",
            "screen.neotab.policy.top_content_toggle", "screen.neotab.policy.top_content_edit",
            "screen.neotab.policy.ping_toggle",        "screen.neotab.policy.duration_toggle",
            "screen.neotab.policy.title_toggle",       "screen.neotab.policy.health_toggle",
            "screen.neotab.policy.health_mode",        "screen.neotab.policy.footer_custom",
            "screen.neotab.policy.footer_tps",         "screen.neotab.policy.footer_mspt",
            "screen.neotab.policy.footer_online",      "screen.neotab.policy.theme",
            "screen.neotab.policy.refresh_interval"
        };

        int y = CONTENT_TOP_PADDING;

        // ── Global policy section ─────────────────────────────────────────────
        AEStyleRenderer.drawSectionHeader(g, this.font,
            Component.translatable("screen.neotab.policy.global"),
            layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;

        int labelWidth = layout.right() - layout.left() - TOGGLE_WIDTH - 8;
        for (String key : policyKeys) {
            int screenY = layout.toScreenY(y);
            // Draw label
            Component label = Component.translatable(key);
            int textY = screenY + (INPUT_HEIGHT - this.font.lineHeight) / 2;
            g.drawString(this.font, label, layout.left(), textY, LABEL_COLOR, false);
            y += ROW_HEIGHT + ROW_GAP;
        }

        // ── Personal policy section ───────────────────────────────────────────
        y += SECTION_GAP;
        AEStyleRenderer.drawSectionHeader(g, this.font,
            Component.translatable("screen.neotab.policy.personal"),
            layout.left(), layout.toScreenY(y), layout.right());
        y += SECTION_HEADER_HEIGHT;

        // Search result label
        if (playerSearchResult != null) {
            int screenY = layout.toScreenY(y - ROW_HEIGHT - ROW_GAP + 2);
            g.drawString(this.font, Component.literal(playerSearchResult),
                layout.left(), screenY, 0xFFAAAAAA, false);
        }

        // Personal policy labels (only shown when a player is being edited)
        if (editingPlayerUUID != null) {
            y += ROW_HEIGHT + ROW_GAP; // skip search box row
            for (String key : policyKeys) {
                int screenY = layout.toScreenY(y);
                Component label = Component.translatable(key);
                int textY = screenY + (INPUT_HEIGHT - this.font.lineHeight) / 2;
                g.drawString(this.font, label, layout.left(), textY, LABEL_COLOR, false);
                y += ROW_HEIGHT + ROW_GAP;
            }
        }
    }

    private void renderScrollableWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {"""

if old3 in content:
    content = content.replace(old3, new3, 1)
    print("Step 3 OK: renderPermissionsContent added")
else:
    errors.append("Step 3 FAIL")

# ── 4. Add player search button next to search box in initPermissionsWidgets ──
old4 = """        // Player search box
        this.playerSearchBox = addRenderableWidget(
            new EditBox(this.font, layout.left(), 0, layout.contentWidth() - TOGGLE_WIDTH - 8, INPUT_HEIGHT,
                Component.translatable("screen.neotab.policy.personal")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));"""

new4 = """        // Player search box + search button
        int searchBoxWidth = layout.contentWidth() - TOGGLE_WIDTH - 8 - 60;
        this.playerSearchBox = addRenderableWidget(
            new EditBox(this.font, layout.left(), 0, searchBoxWidth, INPUT_HEIGHT,
                Component.translatable("screen.neotab.policy.personal")));
        this.playerSearchBox.setMaxLength(40);
        this.playerSearchBox.setHint(Component.translatable("screen.neotab.policy.search_hint"));
        // Search button
        addRenderableWidget(Button.builder(
            Component.translatable("screen.neotab.policy.search"),
            btn -> {
                String name = playerSearchBox.getValue().trim();
                if (name.isEmpty()) return;
                // Search online players first, then check server-side policies by UUID
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    mc.getConnection().getOnlinePlayers().stream()
                        .filter(p -> p.getProfile().getName().equalsIgnoreCase(name))
                        .findFirst()
                        .ifPresentOrElse(
                            p -> {
                                playerSearchResult = p.getProfile().getName();
                                refreshPersonalPolicyToggles(p.getProfile().getId());
                                syncTabWidgetVisibility();
                            },
                            () -> {
                                playerSearchResult = "Not found: " + name;
                                editingPlayerUUID = null;
                                for (CycleButton<Boolean> t : personalPolicyToggles) t.visible = false;
                                syncTabWidgetVisibility();
                            }
                        );
                }
            })
            .bounds(layout.left() + searchBoxWidth + 4, 0, 56, INPUT_HEIGHT)
            .build());"""

if old4 in content:
    content = content.replace(old4, new4, 1)
    print("Step 4 OK: search button added")
else:
    errors.append("Step 4 FAIL")

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "w", encoding="utf-8") as f:
    f.write(content)

if errors:
    for e in errors: print(e)
    sys.exit(1)
else:
    print("All steps done.")
