package com.poso.neotab.api;

import com.poso.neotab.api.event.GetPlayerTitleEvent;
import com.poso.neotab.NeoTab;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoTab 公共API。
 * 
 * <p>其他模组可以通过此类与 NeoTab 进行交互，提供称号数据等功能。</p>
 */
public final class NeoTabAPI {
    
    private static final List<TitleProvider> titleProviders = new ArrayList<>();
    
    /**
     * 称号缓存。
     * 
     * <p>性能优化：缓存玩家称号，避免每次刷新都调用所有提供者和事件。
     * 使用 ConcurrentHashMap 保证线程安全。</p>
     */
    private static final Map<UUID, String> titleCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存过期时间（毫秒）。
     * 
     * <p>默认 5 秒，平衡性能和实时性。</p>
     */
    private static final long CACHE_EXPIRE_TIME = 5000L;
    
    /**
     * 缓存时间戳。
     */
    private static final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();

    /**
     * 称号缓存大小上限。
     *
     * <p>P2 优化：防止大量玩家短暂进出时 Map 无限增长导致内存泄漏。
     * 超过上限时清空全部缓存（简单策略，适合低频变更场景）。</p>
     */
    private static final int MAX_TITLE_CACHE_SIZE = 500;
    
    private NeoTabAPI() {
    }
    
    /**
     * 注册称号提供者。
     * 
     * <p>其他模组可以调用此方法注册自己的称号提供者，
     * NeoTab 会自动调用所有注册的提供者来获取玩家称号。</p>
     * 
     * @param provider 称号提供者
     * @throws IllegalArgumentException 如果提供者为 null 或 ID 重复
     */
    public static void registerTitleProvider(TitleProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Title provider cannot be null");
        }
        
        // 检查是否已有相同ID的提供者
        String providerId = provider.getProviderId();
        for (TitleProvider existing : titleProviders) {
            if (existing.getProviderId().equals(providerId)) {
                throw new IllegalArgumentException("Title provider with ID '" + providerId + "' already registered");
            }
        }
        
        titleProviders.add(provider);
        // 按优先级排序，优先级高的在前
        titleProviders.sort(Comparator.comparingInt(TitleProvider::getPriority).reversed());
        
        // 清空缓存，因为提供者列表变化了
        clearTitleCache();
    }
    
    /**
     * 取消注册称号提供者。
     * 
     * @param providerId 提供者ID
     * @return 如果成功移除则返回 true
     */
    public static boolean unregisterTitleProvider(String providerId) {
        boolean removed = titleProviders.removeIf(provider -> provider.getProviderId().equals(providerId));
        if (removed) {
            // 清空缓存，因为提供者列表变化了
            clearTitleCache();
        }
        return removed;
    }
    
    /**
     * 获取玩家的称号。
     * 
     * <p>此方法会依次调用所有注册的提供者和事件系统来获取称号。</p>
     * 
     * <p>性能优化：使用缓存机制，避免频繁调用提供者。</p>
     * 
     * @param player 玩家对象
     * @return 称号文本，如果没有称号则返回 null
     */
    @Nullable
    public static String getPlayerTitle(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        // 检查缓存是否有效
        Long cacheTime = cacheTimestamps.get(playerId);
        if (cacheTime != null && (currentTime - cacheTime) < CACHE_EXPIRE_TIME) {
            // 缓存有效，直接返回
            return titleCache.get(playerId);
        }
        
        // 缓存过期或不存在，重新获取称号
        String title = fetchPlayerTitle(player);
        
        // P2 优化：超过上限时清空缓存，防止内存泄漏
        if (titleCache.size() >= MAX_TITLE_CACHE_SIZE) {
            titleCache.clear();
            cacheTimestamps.clear();
        }

        // 更新缓存
        if (title != null && !title.isEmpty()) {
            titleCache.put(playerId, title);
        } else {
            titleCache.remove(playerId);
        }
        cacheTimestamps.put(playerId, currentTime);
        
        return title;
    }
    
    /**
     * 从提供者和事件系统获取玩家称号（不使用缓存）。
     */
    @Nullable
    private static String fetchPlayerTitle(ServerPlayer player) {
        // 1. 首先尝试通过注册的提供者获取称号
        for (TitleProvider provider : titleProviders) {
            try {
                String title = provider.getTitle(player);
                if (title != null && !title.isEmpty()) {
                    return title;
                }
            } catch (Exception e) {
                // 使用模组日志系统记录错误，而非 System.err
                NeoTab.LOGGER.error("Error getting title from provider {}: {}", provider.getProviderId(), e.getMessage());
            }
        }
        
        // 2. 如果没有提供者返回称号，尝试通过事件系统获取
        GetPlayerTitleEvent event = new GetPlayerTitleEvent(player);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getTitle();
    }
    
    /**
     * 使指定玩家的称号缓存失效。
     * 
     * <p>当玩家的称号发生变化时，应该调用此方法。</p>
     * 
     * @param playerId 玩家 UUID
     */
    public static void invalidateTitleCache(UUID playerId) {
        titleCache.remove(playerId);
        cacheTimestamps.remove(playerId);
    }
    
    /**
     * 清空所有称号缓存。
     * 
     * <p>当提供者列表变化或需要强制刷新时调用。</p>
     */
    public static void clearTitleCache() {
        titleCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 获取已注册的称号提供者数量。
     * 
     * @return 提供者数量
     */
    public static int getProviderCount() {
        return titleProviders.size();
    }
    
    /**
     * 获取所有已注册的提供者ID列表。
     * 
     * @return 提供者ID列表
     */
    public static List<String> getProviderIds() {
        return titleProviders.stream()
                .map(TitleProvider::getProviderId)
                .toList();
    }
}