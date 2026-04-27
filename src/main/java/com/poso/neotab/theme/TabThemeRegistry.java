package com.poso.neotab.theme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置主题注册表。
 *
 * <p>所有预设主题在这里定义，通过 {@link #get(String)} 按 ID 获取。</p>
 */
public final class TabThemeRegistry {

    private static final Map<String, TabTheme> THEMES = new LinkedHashMap<>();

    static {
        register(TabTheme.VANILLA);

        // ── 暗夜主题 ──────────────────────────────────────────────────────────
        // 莫奈风格配色：深色半透明背景，偶数行略深，奇数行略浅，营造层次感
        register(new TabTheme(
            "dark",
            0xE0352842,   // TAB 面板背景：深莫奈紫（半透明）#352842
            0x50201830,   // 偶数行：深莫奈紫（更深）
            0x30403050,   // 奇数行：浅莫奈紫（轻微高亮）
            0xFFE8D4F0    // 玩家名：浅莫奈粉紫
        ));
    }

    private TabThemeRegistry() {}

    private static void register(TabTheme theme) {
        THEMES.put(theme.id(), theme);
    }

    /** 按 ID 获取主题，未找到时返回原版主题。 */
    public static TabTheme get(String id) {
        return THEMES.getOrDefault(id, TabTheme.VANILLA);
    }

    /** 获取所有主题 ID 列表（保持注册顺序）。 */
    public static java.util.List<String> ids() {
        return new java.util.ArrayList<>(THEMES.keySet());
    }
}
