package com.poso.neotab.theme;

/**
 * TAB 列表主题定义。
 *
 * <p>每个主题包含一组颜色参数，控制 TAB 列表的视觉外观。
 * 颜色格式均为 ARGB 整数（0xAARRGGBB）。</p>
 *
 * <p>可自定义的参数：</p>
 * <ul>
 *   <li>{@link #backgroundColor}     - 整体背景色（覆盖在原版背景之上）</li>
 *   <li>{@link #rowEvenColor}         - 偶数行背景色</li>
 *   <li>{@link #rowOddColor}          - 奇数行背景色</li>
 *   <li>{@link #playerNameColor}      - 玩家名称文字颜色（0 = 使用原版颜色）</li>
 * </ul>
 */
public record TabTheme(
    String id,
    int backgroundColor,
    int rowEvenColor,
    int rowOddColor,
    int playerNameColor
) {
    /** 原版主题：不修改任何颜色，完全使用原版渲染。 */
    public static final TabTheme VANILLA = new TabTheme(
        "vanilla",
        0x00000000,  // 完全透明，不覆盖
        0x00000000,
        0x00000000,
        0
    );

    /** 是否为原版主题（不做任何覆盖）。 */
    public boolean isVanilla() {
        return "vanilla".equals(id);
    }
}
