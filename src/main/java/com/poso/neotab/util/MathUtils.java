package com.poso.neotab.util;

/**
 * 数学工具类 - 提供Java 17兼容的方法。
 */
public final class MathUtils {
    
    private MathUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 将值限制在指定范围内（Java 17兼容版本）。
     * 
     * @param value 要限制的值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的值
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在指定范围内（Java 17兼容版本）。
     * 
     * @param value 要限制的值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的值
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在指定范围内（Java 17兼容版本）。
     * 
     * @param value 要限制的值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的值
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在指定范围内（Java 17兼容版本）。
     * 
     * @param value 要限制的值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的值
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}