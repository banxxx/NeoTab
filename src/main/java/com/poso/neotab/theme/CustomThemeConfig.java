package com.poso.neotab.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义主题配置。
 * 
 * <p>支持独立文件存储，方便用户分享主题配置。</p>
 */
public class CustomThemeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /** 背景颜色 (ARGB) */
    private int backgroundColor;
    
    /** 边框颜色数组（最多7种颜色，ARGB格式） */
    private List<Integer> borderColors;
    
    /** 边框外圈颜色 (ARGB) */
    private int borderOuterColorFactor;
    
    /** 是否启用动画效果 */
    private boolean animationEnabled;
    
    /** 默认配置（莫奈风格） */
    public static CustomThemeConfig defaults() {
        CustomThemeConfig config = new CustomThemeConfig();
        config.backgroundColor = 0xE0352842;
        config.borderColors = new ArrayList<>();
        config.borderColors.add(0xFFFF9999);  // 莫奈粉红
        config.borderColors.add(0xFFFFCC99);  // 莫奈杏色
        config.borderColors.add(0xFFFFFF99);  // 莫奈米黄
        config.borderColors.add(0xFF99FFCC);  // 莫奈薄荷绿
        config.borderColors.add(0xFF99DDFF);  // 莫奈天蓝
        config.borderColors.add(0xFF9999FF);  // 莫奈淡紫
        config.borderColors.add(0xFFCC99FF);  // 莫奈紫罗兰
        config.borderOuterColorFactor = 40;   // 外圈颜色深度因子 (0-100)
        config.animationEnabled = true;
        return config;
    }
    
    public int getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public List<Integer> getBorderColors() {
        return new ArrayList<>(borderColors);
    }
    
    public void setBorderColors(List<Integer> borderColors) {
        this.borderColors = new ArrayList<>(borderColors);
    }
    
    public int getBorderOuterColorFactor() {
        return borderOuterColorFactor;
    }
    
    public void setBorderOuterColorFactor(int borderOuterColorFactor) {
        this.borderOuterColorFactor = Math.clamp(borderOuterColorFactor, 0, 100);
    }
    
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }
    
    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }
    
    /**
     * 序列化为 JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("backgroundColor", String.format("#%08X", backgroundColor));
        
        JsonArray colorsArray = new JsonArray();
        for (int color : borderColors) {
            colorsArray.add(String.format("#%08X", color));
        }
        json.add("borderColors", colorsArray);
        
        json.addProperty("borderOuterColorFactor", borderOuterColorFactor);
        json.addProperty("animationEnabled", animationEnabled);
        
        return json;
    }
    
    /**
     * 从 JSON 反序列化
     */
    public static CustomThemeConfig fromJson(JsonObject json) {
        CustomThemeConfig config = new CustomThemeConfig();
        
        // 背景颜色
        if (json.has("backgroundColor")) {
            config.backgroundColor = parseColor(json.get("backgroundColor").getAsString());
        } else {
            config.backgroundColor = defaults().backgroundColor;
        }
        
        // 边框颜色数组
        config.borderColors = new ArrayList<>();
        if (json.has("borderColors") && json.get("borderColors").isJsonArray()) {
            JsonArray colorsArray = json.getAsJsonArray("borderColors");
            for (int i = 0; i < Math.min(colorsArray.size(), 7); i++) {
                config.borderColors.add(parseColor(colorsArray.get(i).getAsString()));
            }
        }
        
        // 如果颜色数量为0，添加一个与背景颜色相同的边框色（与背景融为一体）
        if (config.borderColors.isEmpty()) {
            config.borderColors.add(config.backgroundColor);
        }
        
        // 外圈颜色因子
        if (json.has("borderOuterColorFactor")) {
            config.borderOuterColorFactor = json.get("borderOuterColorFactor").getAsInt();
        } else {
            config.borderOuterColorFactor = defaults().borderOuterColorFactor;
        }
        
        // 动画开关
        if (json.has("animationEnabled")) {
            config.animationEnabled = json.get("animationEnabled").getAsBoolean();
        } else {
            config.animationEnabled = defaults().animationEnabled;
        }
        
        return config;
    }
    
    /**
     * 保存到文件
     */
    public void saveToFile(Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        String jsonString = GSON.toJson(toJson());
        Files.writeString(filePath, jsonString);
    }
    
    /**
     * 从文件加载
     */
    public static CustomThemeConfig loadFromFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            return defaults();
        }
        
        try {
            String jsonString = Files.readString(filePath);
            // 检查JSON字符串是否为空或无效
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return defaults();
            }
            
            JsonObject json = GSON.fromJson(jsonString, JsonObject.class);
            // 检查解析结果是否为null
            if (json == null) {
                return defaults();
            }
            
            return fromJson(json);
        } catch (com.google.gson.JsonSyntaxException e) {
            // JSON格式错误，返回默认配置
            System.err.println("Failed to parse custom theme config: " + e.getMessage());
            return defaults();
        } catch (Exception e) {
            // 其他错误，抛出IOException
            throw new IOException("Failed to load custom theme config", e);
        }
    }
    
    /**
     * 解析颜色字符串（支持 #RRGGBB 和 #AARRGGBB 格式）
     */
    private static int parseColor(String colorStr) {
        try {
            String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
            
            if (hex.length() == 6) {
                // #RRGGBB -> #FFRRGGBB
                return (int) Long.parseLong("FF" + hex, 16);
            } else if (hex.length() == 8) {
                // #AARRGGBB
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException e) {
            // 解析失败，返回默认颜色
        }
        
        return 0xFFFFFFFF; // 默认白色
    }
}
