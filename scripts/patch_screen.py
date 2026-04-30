import sys

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "r", encoding="utf-8") as f:
    content = f.read()

# ── Step 1: Add ScreenMode enum + PERMISSIONS tab ────────────────────────────
old1 = """    private enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme");
        final String langKey;
        ConfigTab(String langKey) { this.langKey = langKey; }
        Component label() { return Component.translatable(langKey); }
    }"""

new1 = """    /**
     * Screen mode.
     * ADMIN: full config + permissions tab (only visible to admins).
     * PLAYER: personal customization, restricted by policy (locked widgets shown greyed out).
     */
    public enum ScreenMode { ADMIN, PLAYER }

    private enum ConfigTab {
        PAGE_CONFIG("screen.neotab.tab.page_config"),
        THEME("screen.neotab.tab.theme"),
        PERMISSIONS("screen.neotab.tab.permissions");  // admin only
        final String langKey;
        ConfigTab(String langKey) { this.langKey = langKey; }
        Component label() { return Component.translatable(langKey); }
    }"""

if old1 in content:
    content = content.replace(old1, new1, 1)
    print("Step 1 OK: ScreenMode + PERMISSIONS tab added")
else:
    print("Step 1 FAIL: ConfigTab enum not found")
    sys.exit(1)

# ── Step 2: Add mode/policy/personalConfig fields + new constructors ─────────
old2 = """    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
    }"""

new2 = """    /** Current screen mode (ADMIN or PLAYER). */
    private final ScreenMode screenMode;
    /** Effective customize policy for this player (used in PLAYER mode). */
    private final PlayerCustomizePolicy policy;
    /** Player's personal config (used in PLAYER mode). */
    private final com.poso.neotab.config.PlayerTabConfig personalConfig;

    // ── Permissions tab widgets ───────────────────────────────────────────────
    /** Global policy toggle buttons (one per policy field). */
    private final List<CycleButton<Boolean>> globalPolicyToggles = new ArrayList<>();
    /** Personal policy section: player name search box. */
    private EditBox playerSearchBox;
    /** Personal policy section: search result label. */
    private String playerSearchResult = null;
    /** UUID of the player currently being edited in personal policy. */
    private java.util.UUID editingPlayerUUID = null;
    /** Personal policy toggle buttons for the currently searched player. */
    private final List<CycleButton<Boolean>> personalPolicyToggles = new ArrayList<>();

    /** Admin constructor (full config, no restrictions). */
    public NeoTabConfigScreen(Screen parent, TabConfig config) {
        super(Component.translatable("screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = config;
        this.screenMode = ScreenMode.ADMIN;
        this.policy = com.poso.neotab.permission.PlayerCustomizePolicy.unlocked();
        this.personalConfig = null;
    }

    /** Player constructor (personal customization, restricted by policy). */
    public NeoTabConfigScreen(Screen parent, TabConfig serverConfig,
                              ScreenMode mode, PlayerCustomizePolicy policy,
                              com.poso.neotab.config.PlayerTabConfig personalConfig) {
        super(Component.translatable(
            mode == ScreenMode.PLAYER ? "screen.neotab.title.player" : "screen.neotab.title"));
        this.parent = parent;
        this.initialConfig = serverConfig;
        this.screenMode = mode;
        this.policy = policy != null ? policy : com.poso.neotab.permission.PlayerCustomizePolicy.locked();
        this.personalConfig = personalConfig;
    }"""

if old2 in content:
    content = content.replace(old2, new2, 1)
    print("Step 2 OK: new fields + constructors added")
else:
    print("Step 2 FAIL: constructor not found")
    sys.exit(1)

# ── Step 3: Update init() to handle PLAYER mode initial values ───────────────
old3 = """    @Override
    protected void init() {
        clearWidgets();
        this.themeOptionButtons.clear();
        this.themeOptionIds.clear();
        this.selectedThemeId = TabThemeRegistry.get(initialConfig.tabTheme()).id();
        Layout layout = buildLayout();
        initPageConfigWidgets(layout);
        initThemeWidgets(layout);
        initFooterAndFinalize(layout);
    }"""

new3 = """    @Override
    protected void init() {
        clearWidgets();
        this.themeOptionButtons.clear();
        this.themeOptionIds.clear();
        this.globalPolicyToggles.clear();
        this.personalPolicyToggles.clear();
        // In PLAYER mode, use personal config values where available; fall back to server config
        com.poso.neotab.config.TabConfig effectiveInit = (screenMode == ScreenMode.PLAYER && personalConfig != null)
            ? personalConfig.mergeInto(initialConfig, policy)
            : initialConfig;
        this.selectedThemeId = TabThemeRegistry.get(effectiveInit.tabTheme()).id();
        Layout layout = buildLayout();
        initPageConfigWidgets(layout, effectiveInit);
        initThemeWidgets(layout);
        if (screenMode == ScreenMode.ADMIN) {
            initPermissionsWidgets(layout);
        }
        initFooterAndFinalize(layout, effectiveInit);
    }"""

if old3 in content:
    content = content.replace(old3, new3, 1)
    print("Step 3 OK: init() updated")
else:
    print("Step 3 FAIL: init() not found")
    sys.exit(1)

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "w", encoding="utf-8") as f:
    f.write(content)

print("All steps done, file written.")
