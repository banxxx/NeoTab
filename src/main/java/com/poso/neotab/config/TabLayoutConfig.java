package com.poso.neotab.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poso.neotab.NeoTab;
import com.poso.neotab.util.MathUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端本地 TAB 布局配置（列数、行数）。
 *
 * <p>这是纯客户端设置，不需要服务端同步，保存在 .minecraft/config/neotab-layout.json。</p>
 */
public final class TabLayoutConfig {

    /** 是否启用自定义布局 */
    private boolean enabled;
    /** 展示列数 */
    private int columns;
    /** 每列展示行数 */
    private int rowsPerColumn;

    public static final int MIN_COLUMNS      = 1;
    public static final int MIN_ROWS         = 1;
    public static final int DEFAULT_COLUMNS  = 3;
    public static final int DEFAULT_ROWS     = 20;
    public static final boolean DEFAULT_ENABLED = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "neotab-layout.json";

    private static TabLayoutConfig INSTANCE;

    private TabLayoutConfig(boolean enabled, int columns, int rowsPerColumn) {
        // 注意：这里不做限制检查，因为限制是动态的
        // 实际限制在 setColumns 和 setRowsPerColumn 中应用
        this.enabled      = enabled;
        this.columns      = columns;
        this.rowsPerColumn = rowsPerColumn;
    }

    public static TabLayoutConfig defaults() {
        return new TabLayoutConfig(DEFAULT_ENABLED, DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public boolean isEnabled()    { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public int getColumns()       { return columns; }
    public int getRowsPerColumn() { return rowsPerColumn; }

    /**
     * 设置列数，会根据当前血量显示模式动态限制。
     * 
     * @param v 期望的列数
     */
    public void setColumns(int v) {
        // 获取当前血量显示模式
        HealthDisplayMode healthMode = com.poso.neotab.client.NeoTabClientState.getCurrentConfig().healthDisplayMode();
        int maxColumns = TabLayoutLimits.getMaxColumns(healthMode);
        this.columns = MathUtils.clamp(v, MIN_COLUMNS, maxColumns);
    }
    
    /**
     * 设置行数，会根据当前屏幕尺寸动态限制。
     * 
     * @param v 期望的行数
     */
    public void setRowsPerColumn(int v) {
        int maxRows = TabLayoutLimits.getMaxRows();
        this.rowsPerColumn = MathUtils.clamp(v, MIN_ROWS, maxRows);
    }
    
    /**
     * 获取当前配置下的最大列数限制。
     * 
     * @return 最大列数
     */
    public int getMaxColumns() {
        HealthDisplayMode healthMode = com.poso.neotab.client.NeoTabClientState.getCurrentConfig().healthDisplayMode();
        return TabLayoutLimits.getMaxColumns(healthMode);
    }
    
    /**
     * 获取当前配置下的最大行数限制。
     * 
     * @return 最大行数
     */
    public int getMaxRows() {
        return TabLayoutLimits.getMaxRows();
    }

    /** 每页最多显示的玩家数 = columns × rowsPerColumn */
    public int playersPerPage() { return columns * rowsPerColumn; }

    // ── 持久化 ────────────────────────────────────────────────────────────────

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    public static TabLayoutConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static void invalidate() {
        INSTANCE = null;
    }

    private static TabLayoutConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            TabLayoutConfig def = defaults();
            save(def);
            return def;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            boolean en   = json.has("enabled")       ? json.get("enabled").getAsBoolean()      : DEFAULT_ENABLED;
            int cols = json.has("columns")      ? json.get("columns").getAsInt()      : DEFAULT_COLUMNS;
            int rows = json.has("rowsPerColumn") ? json.get("rowsPerColumn").getAsInt() : DEFAULT_ROWS;
            TabLayoutConfig config = new TabLayoutConfig(en, cols, rows);
            // 加载后立即应用当前限制，确保值在合理范围内
            config.setColumns(cols);
            config.setRowsPerColumn(rows);
            return config;
        } catch (Exception e) {
            NeoTab.LOGGER.error("Failed to load neotab-layout.json", e);
            return defaults();
        }
    }

    public static void save(TabLayoutConfig cfg) {
        Path path = configPath();
        JsonObject json = new JsonObject();
        json.addProperty("enabled",      cfg.enabled);
        json.addProperty("columns",      cfg.columns);
        json.addProperty("rowsPerColumn", cfg.rowsPerColumn);
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(json, w);
            }
        } catch (IOException e) {
            NeoTab.LOGGER.error("Failed to save neotab-layout.json", e);
        }
        INSTANCE = cfg;
    }
}