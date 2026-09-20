package com.poso.neotab.theme;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 自定义主题管理器。
 * 
 * <p>负责加载和保存自定义主题配置文件。</p>
 */
public class CustomThemeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomThemeManager.class);
    private static final String CUSTOM_THEME_FILE = "custom_theme.json";

    private static CustomThemeConfig currentConfig = null;

    // 外部编辑检测：记录加载/保存时配置文件的时间戳，get() 低频探测文件是否被外部修改
    private static long recordedMtime = -1L;
    private static long lastMtimeProbe = 0L;
    private static final long MTIME_PROBE_INTERVAL_MS = 1000L;
    
    /**
     * 获取自定义主题配置文件路径
     */
    public static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("neotab")
                .resolve(CUSTOM_THEME_FILE);
    }
    
    /**
     * 加载自定义主题配置
     */
    public static CustomThemeConfig load() {
        if (currentConfig != null) {
            return currentConfig;
        }
        
        try {
            currentConfig = CustomThemeConfig.loadFromFile(getConfigPath());
            LOGGER.info("Loaded custom theme config from {}", getConfigPath());
        } catch (IOException e) {
            LOGGER.warn("Failed to load custom theme config, using defaults", e);
            currentConfig = CustomThemeConfig.defaults();
        }
        recordedMtime = probeMtime();
        lastMtimeProbe = System.currentTimeMillis();
        
        return currentConfig;
    }

    /**
     * 保存自定义主题配置
     */
    public static void save(CustomThemeConfig config) {
        try {
            config.saveToFile(getConfigPath());
            currentConfig = config;
            recordedMtime = probeMtime();
            lastMtimeProbe = System.currentTimeMillis();
            LOGGER.info("Saved custom theme config to {}", getConfigPath());
        } catch (IOException e) {
            LOGGER.error("Failed to save custom theme config", e);
        }
    }
    
    /**
     * 获取当前配置（如果未加载则加载）
     */
    public static CustomThemeConfig get() {
        if (currentConfig == null) {
            return load();
        }
        refreshIfChangedExternally();
        return currentConfig;
    }

    private static long probeMtime() {
        try {
            Path path = getConfigPath();
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * 文件被外部修改（手改 JSON / 其他程序写入）时重新读取。
     * 字段就地拷贝到现有实例，保持所有持有 get() 引用方的身份有效。
     */
    private static void refreshIfChangedExternally() {
        long now = System.currentTimeMillis();
        if (now - lastMtimeProbe < MTIME_PROBE_INTERVAL_MS) return;
        lastMtimeProbe = now;
        long mtime = probeMtime();
        if (mtime == -1L || mtime == recordedMtime) return;
        try {
            CustomThemeConfig fresh = CustomThemeConfig.loadFromFile(getConfigPath());
            currentConfig.setBackgroundColor(fresh.getBackgroundColor());
            currentConfig.setBorderColors(fresh.getBorderColors());
            currentConfig.setBorderOuterColor(fresh.getBorderOuterColor());
            currentConfig.setAnimationEnabled(fresh.isAnimationEnabled());
            currentConfig.setAnimationSpeed(fresh.getAnimationSpeed());
            recordedMtime = mtime;
            LOGGER.info("Custom theme config changed externally, reloaded from {}", getConfigPath());
        } catch (IOException e) {
            LOGGER.warn("Failed to reload custom theme config after external change", e);
        }
    }
    
    /**
     * 重置为默认配置
     */
    public static void reset() {
        currentConfig = CustomThemeConfig.defaults();
        save(currentConfig);
    }
}
