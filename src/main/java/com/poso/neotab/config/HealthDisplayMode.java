package com.poso.neotab.config;

/**
 * 血量显示效果模式。
 *
 * <ul>
 *   <li>{@link #FULL}    - 完整模式：显示最多 10 颗心，超出上限时追加数字</li>
 *   <li>{@link #COMPACT} - 单独模式：只显示 1 颗心 + 血量数字，节省空间</li>
 * </ul>
 */
public enum HealthDisplayMode {
    FULL,
    COMPACT;

    /** 序列化为字符串（用于 JSON 持久化）。 */
    public String toId() {
        return name().toLowerCase();
    }

    /** 从字符串反序列化，未知值回退到 FULL。 */
    public static HealthDisplayMode fromId(String id) {
        if (id == null) return FULL;
        return switch (id.toLowerCase()) {
            case "compact" -> COMPACT;
            default        -> FULL;
        };
    }
}