package com.poso.neotab.theme;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        
        return currentConfig;
    }
    
    /**
     * 保存自定义主题配置
     */
    public static void save(CustomThemeConfig config) {
        try {
            config.saveToFile(getConfigPath());
            currentConfig = config;
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
        return currentConfig;
    }
    
    /**
     * 重置为默认配置
     */
    public static void reset() {
        currentConfig = CustomThemeConfig.defaults();
        save(currentConfig);
    }
}
