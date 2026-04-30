with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "r", encoding="utf-8") as f:
    content = f.read()

# ── Fix 1: rename cfg -> layoutCfgInner inside lambdas to avoid conflict ──────
# There are 3 lambdas that declare `cfg` locally inside initPageConfigWidgets
# Replace each occurrence of the lambda-local variable

content = content.replace(
    """                    (button, enabled) -> {
                        com.poso.neotab.config.TabLayoutConfig cfg = com.poso.neotab.config.TabLayoutConfig.get();
                        cfg.setEnabled(enabled);
                        com.poso.neotab.config.TabLayoutConfig.save(cfg);""",
    """                    (button, enabled) -> {
                        com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                        layoutCfgInner.setEnabled(enabled);
                        com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);""",
    1
)

content = content.replace(
    """                button -> {
                    com.poso.neotab.config.TabLayoutConfig cfg = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = cfg.getColumns();
                    int maxColumns = cfg.getMaxColumns();
                    // 循环递增，超过最大值时回到1
                    int next = current >= maxColumns ? 1 : current + 1;
                    cfg.setColumns(next);
                    com.poso.neotab.config.TabLayoutConfig.save(cfg);
                    button.setMessage(Component.translatable("screen.neotab.layout.columns", next));
                })""",
    """                button -> {
                    com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = layoutCfgInner.getColumns();
                    int maxColumns = layoutCfgInner.getMaxColumns();
                    // 循环递增，超过最大值时回到1
                    int next = current >= maxColumns ? 1 : current + 1;
                    layoutCfgInner.setColumns(next);
                    com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);
                    button.setMessage(Component.translatable("screen.neotab.layout.columns", next));
                })""",
    1
)

# The rows button lambda - find and replace the cfg variable
old_rows = """                button -> {
                    com.poso.neotab.config.TabLayoutConfig cfg = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = cfg.getRowsPerColumn();
                    int maxRows = cfg.getMaxRows();"""
new_rows = """                button -> {
                    com.poso.neotab.config.TabLayoutConfig layoutCfgInner = com.poso.neotab.config.TabLayoutConfig.get();
                    int current = layoutCfgInner.getRowsPerColumn();
                    int maxRows = layoutCfgInner.getMaxRows();"""

if old_rows in content:
    content = content.replace(old_rows, new_rows, 1)
    # Also fix the cfg.setRowsPerColumn and cfg.save calls in the same lambda
    content = content.replace(
        "                    cfg.setRowsPerColumn(next);\n                    com.poso.neotab.config.TabLayoutConfig.save(cfg);\n",
        "                    layoutCfgInner.setRowsPerColumn(next);\n                    com.poso.neotab.config.TabLayoutConfig.save(layoutCfgInner);\n",
        1
    )
    print("Fix 1 OK: lambda cfg -> layoutCfgInner")
else:
    print("Fix 1 FAIL: rows lambda not found")

# ── Fix 2: pass screenMode to Renderer.renderTabBar ──────────────────────────
# The static Renderer.renderTabBar needs screenMode as a parameter

# 2a: Update the static method signature
old_sig = """        static void renderTabBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                                 ConfigTab activeTab, Layout layout,
                                 int mouseX, int mouseY) {
            int x    = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;

            int tabIndex = 0;
            for (ConfigTab tab : ConfigTab.values()) {
                // PERMISSIONS tab is only visible in ADMIN mode
                if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;"""

new_sig = """        static void renderTabBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                                 ConfigTab activeTab, Layout layout,
                                 int mouseX, int mouseY, ScreenMode screenMode) {
            int x    = layout.tabBarX() + TAB_BUTTON_LEFT_PADDING;
            int btnW = TAB_BAR_WIDTH - TAB_BUTTON_LEFT_PADDING;

            int tabIndex = 0;
            for (ConfigTab tab : ConfigTab.values()) {
                // PERMISSIONS tab is only visible in ADMIN mode
                if (tab == ConfigTab.PERMISSIONS && screenMode != ScreenMode.ADMIN) continue;"""

if old_sig in content:
    content = content.replace(old_sig, new_sig, 1)
    print("Fix 2a OK: renderTabBar signature updated")
else:
    print("Fix 2a FAIL: renderTabBar signature not found")

# 2b: Update the call site in renderTabBar instance method
old_call = "        Renderer.renderTabBar(g, this.font, this.activeTab, layout, mouseX, mouseY);"
new_call = "        Renderer.renderTabBar(g, this.font, this.activeTab, layout, mouseX, mouseY, this.screenMode);"

if old_call in content:
    content = content.replace(old_call, new_call, 1)
    print("Fix 2b OK: renderTabBar call site updated")
else:
    print("Fix 2b FAIL: renderTabBar call site not found")

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "w", encoding="utf-8") as f:
    f.write(content)

print("Done.")
