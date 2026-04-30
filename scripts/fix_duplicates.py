with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "r", encoding="utf-8") as f:
    content = f.read()

# Fix duplicate syncTabWidgetVisibility header
bad = """    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : globalPolicyToggles) btn.visible = perms;
        for (CycleButton<Boolean> btn : personalPolicyToggles) btn.visible = perms;
        if (playerSearchBox != null) playerSearchBox.visible = perms;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : globalPolicyToggles) btn.visible = perms;
        for (CycleButton<Boolean> btn : personalPolicyToggles) btn.visible = perms;
        if (playerSearchBox != null) playerSearchBox.visible = perms;"""

good = """    private void syncTabWidgetVisibility() {
        boolean page  = activeTab == ConfigTab.PAGE_CONFIG;
        boolean theme = activeTab == ConfigTab.THEME;
        boolean perms = activeTab == ConfigTab.PERMISSIONS;
        // Permissions tab widgets
        for (CycleButton<Boolean> btn : globalPolicyToggles) btn.visible = perms;
        for (CycleButton<Boolean> btn : personalPolicyToggles) btn.visible = perms;
        if (playerSearchBox != null) playerSearchBox.visible = perms;"""

if bad in content:
    content = content.replace(bad, good, 1)
    print("Fixed duplicate syncTabWidgetVisibility")
else:
    print("No duplicate found (already clean)")

with open("src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java", "w", encoding="utf-8") as f:
    f.write(content)
