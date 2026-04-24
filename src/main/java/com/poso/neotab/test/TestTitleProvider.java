package com.poso.neotab.test;

import com.poso.neotab.api.TitleProvider;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 测试用的称号提供者。
 * 
 * <p>此类用于演示如何实现 TitleProvider 接口。
 * 在实际部署时可以移除此类。</p>
 */
public class TestTitleProvider implements TitleProvider {
    
    @Override
    @Nullable
    public String getTitle(ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        
        // 为特定玩家名称提供特殊称号
        return switch (playerName.toLowerCase()) {
            case "admin", "administrator" -> "<color #FF0000><bold>『管理员』</bold></color>";
            case "dev", "developer" -> "<gradient #00FF00,#0080FF><bold>『开发者』</bold></gradient>";
            case "test", "tester" -> "<color #FFD700>『测试员』</color>";
            default -> null; // 其他玩家使用模拟数据
        };
    }
    
    @Override
    public int getPriority() {
        return 1000; // 高优先级，优先于模拟数据
    }
    
    @Override
    public String getProviderId() {
        return "neotab:test_provider";
    }
}