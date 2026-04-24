package com.poso.neotab.mixin.client;

import com.poso.neotab.client.NeoTabClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 在启用"更好的延迟显示"或"在线时长显示"时，处理右对齐显示和列宽度调整。
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    
    /** 与用户名称之间的固定间距 */
    private static final int NAME_TO_INFO_SPACING = 8;
    
    /** 测试模式：启用后会模拟不同的延迟和在线时长 */
    private static final boolean TEST_MODE = false;  // 性能优化：禁用测试模式
    
    /**
     * 缓存的列宽度计算结果。
     * 
     * <p>性能优化：缓存计算结果，避免每帧重复计算所有玩家的文本宽度。</p>
     */
    private int cachedRequiredSpace = -1;
    
    /**
     * 上一次计算宽度时的玩家数量。
     * 
     * <p>性能优化：用于检测玩家列表是否变化，只在变化时重新计算。</p>
     */
    private int lastPlayerCount = -1;
    
    /**
     * 上一次计算宽度时的配置状态。
     * 
     * <p>性能优化：用于检测配置是否变化，只在变化时重新计算。</p>
     */
    private boolean lastBetterPingEnabled = false;
    private boolean lastOnlineDurationEnabled = false;
    
    /**
     * 测试模式：根据玩家名称返回模拟的延迟值
     */
    private int getTestLatency(PlayerInfo playerInfo) {
        if (!TEST_MODE) {
            return playerInfo.getLatency();
        }
        
        String name = playerInfo.getProfile().getName();
        // 根据名称长度模拟不同的延迟
        int nameLength = name.length();
        if (nameLength <= 5) {
            return 50;  // 短名称：低延迟
        } else if (nameLength <= 10) {
            return 150; // 中等名称：中等延迟
        } else if (nameLength <= 15) {
            return 350; // 较长名称：较高延迟
        } else {
            return 1000; // 很长名称：高延迟
        }
    }
    
    /**
     * 测试模式：根据玩家名称返回模拟的在线时长
     */
    private String getTestOnlineDuration(UUID playerId, PlayerInfo playerInfo) {
        if (!TEST_MODE) {
            return NeoTabClientState.getOnlineDuration(playerId);
        }
        
        String name = playerInfo.getProfile().getName();
        // 根据名称长度模拟不同的在线时长
        int nameLength = name.length();
        if (nameLength <= 5) {
            return "1h";     // 短名称：1小时
        } else if (nameLength <= 10) {
            return "5h";     // 中等名称：5小时
        } else if (nameLength <= 15) {
            return "1d12h";  // 较长名称：1天12小时
        } else {
            return "100d5h"; // 很长名称：100天5小时
        }
    }

    /**
     * 处理延迟图标位置的渲染，实现延迟信息和在线时长的右对齐。
     */
    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void neotab$renderRightAlignedInfo(GuiGraphics guiGraphics, int width, int x, int y, PlayerInfo playerInfo, CallbackInfo ci) {
        var config = NeoTabClientState.getCurrentConfig();
        Font font = this.minecraft.font;
        
        // 构建要显示的文本
        StringBuilder textBuilder = new StringBuilder();
        boolean hasPing = config.betterPingEnabled();
        boolean hasDuration = config.onlineDurationEnabled();
        
        if (hasPing) {
            // 使用测试延迟或真实延迟
            int latency = getTestLatency(playerInfo);
            textBuilder.append(latency).append("ms");
        }
        
        if (hasDuration) {
            if (hasPing) {
                textBuilder.append(" ");
            }
            // 使用测试在线时长或真实在线时长
            String onlineDuration = getTestOnlineDuration(playerInfo.getProfile().getId(), playerInfo);
            textBuilder.append(onlineDuration);
        }
        
        // 如果启用了betterPing，取消原版图标渲染
        if (hasPing) {
            ci.cancel();
        }
        
        // 如果有任何信息需要右对齐显示
        if (hasPing || hasDuration) {
            String fullText = textBuilder.toString();
            int textWidth = font.width(fullText);
            
            // 计算右对齐位置
            int rightAlignedX = x + width - textWidth;

            // 如果只显示在线时长（没有启用更好的延迟），需要为原版延迟图标留出空间
            if (hasDuration && !hasPing) {
                rightAlignedX -= 13; // 为原版延迟图标留出空间（13px图标）
            }
            
            // 分别渲染延迟和在线时长（保持各自的颜色）
            int currentX = rightAlignedX;
            
            if (hasPing) {
                // 使用测试延迟或真实延迟
                int latency = getTestLatency(playerInfo);
                String pingText = latency + "ms";
                int pingColor = getPingColor(latency);
                guiGraphics.drawString(font, pingText, currentX, y, pingColor, false);
                currentX += font.width(pingText);
                
                if (hasDuration) {
                    guiGraphics.drawString(font, " ", currentX, y, 0xFFFFFF, false);
                    currentX += font.width(" ");
                }
            }
            
            if (hasDuration) {
                int durationColor = ChatFormatting.AQUA.getColor() != null ? ChatFormatting.AQUA.getColor() : 0x55FFFF;
                // 使用测试在线时长或真实在线时长
                String onlineDuration = getTestOnlineDuration(playerInfo.getProfile().getId(), playerInfo);
                guiGraphics.drawString(font, onlineDuration, currentX, y, durationColor, false);
            }
        }
    }

    /**
     * 修改列宽度计算，根据实际玩家列表中的延迟和在线时长动态计算所需空间。
     * 
     * <p>性能优化：使用缓存避免每帧重复计算。</p>
     * 
     * 使用@ModifyConstant来修改原版的13像素常量。
     */
    @ModifyConstant(
        method = "render",
        constant = @Constant(intValue = 13)
    )
    private int neotab$adjustPingIconSpace(int original) {
        var config = NeoTabClientState.getCurrentConfig();
        
        if (config.betterPingEnabled() || config.onlineDurationEnabled()) {
            // 检查配置是否变化
            boolean configChanged = (config.betterPingEnabled() != lastBetterPingEnabled) 
                                 || (config.onlineDurationEnabled() != lastOnlineDurationEnabled);
            
            // 检查玩家数量是否变化
            int currentPlayerCount = 0;
            if (this.minecraft.getConnection() != null) {
                currentPlayerCount = this.minecraft.getConnection().getOnlinePlayers().size();
            }
            boolean playerCountChanged = (currentPlayerCount != lastPlayerCount);
            
            // 只在配置变化或玩家数量变化时重新计算
            if (configChanged || playerCountChanged || cachedRequiredSpace == -1) {
                Font font = this.minecraft.font;
                cachedRequiredSpace = calculateActualRequiredSpace(font, config);
                
                // 更新缓存状态
                lastBetterPingEnabled = config.betterPingEnabled();
                lastOnlineDurationEnabled = config.onlineDurationEnabled();
                lastPlayerCount = currentPlayerCount;
            }
            
            return cachedRequiredSpace;
        }

        return original;
    }
    
    /**
     * 根据当前在线玩家的实际延迟和在线时长计算所需的最大宽度。
     * 
     * <p>性能优化：此方法现在只在必要时调用（配置或玩家列表变化时）。</p>
     */
    private int calculateActualRequiredSpace(Font font, com.poso.neotab.config.TabConfig config) {
        int maxWidth = 0;
        
        // 获取当前在线的所有玩家
        if (this.minecraft.getConnection() != null) {
            var playerInfos = this.minecraft.getConnection().getOnlinePlayers();
            
            for (var playerInfo : playerInfos) {
                int currentWidth = 0;
                
                if (config.betterPingEnabled()) {
                    // 使用测试延迟或真实延迟
                    int latency = getTestLatency(playerInfo);
                    String pingText = latency + "ms";
                    currentWidth += font.width(pingText);
                    
                    if (config.onlineDurationEnabled()) {
                        currentWidth += font.width(" ");
                    }
                }
                
                if (config.onlineDurationEnabled()) {
                    // 如果只启用在线时长（没有启用更好的延迟），需要为原版延迟图标留出空间
                    if (!config.betterPingEnabled()) {
                        currentWidth += 13; // 原版延迟图标宽度
                    }
                    
                    // 使用测试在线时长或真实在线时长
                    String onlineDuration = getTestOnlineDuration(playerInfo.getProfile().getId(), playerInfo);
                    currentWidth += font.width(onlineDuration);
                }
                
                // 更新最大宽度
                maxWidth = Math.max(maxWidth, currentWidth);
            }
        }
        
        // 如果没有玩家或计算结果为0，使用默认值
        if (maxWidth == 0) {
            if (config.betterPingEnabled()) {
                maxWidth = font.width("999ms");
                if (config.onlineDurationEnabled()) {
                    maxWidth += font.width(" 99d23h");
                }
            } else if (config.onlineDurationEnabled()) {
                maxWidth = 13 + font.width("99d23h");
            }
        }

        // 保持 10 的间距
        return maxWidth + 10;
    }

    /**
     * 根据延迟返回颜色值。
     */
    private int getPingColor(int latency) {
        if (latency < 100) {
            return ChatFormatting.GREEN.getColor() != null ? ChatFormatting.GREEN.getColor() : 0x55FF55;
        } else if (latency < 200) {
            return ChatFormatting.YELLOW.getColor() != null ? ChatFormatting.YELLOW.getColor() : 0xFFFF55;
        } else if (latency < 350) {
            return ChatFormatting.GOLD.getColor() != null ? ChatFormatting.GOLD.getColor() : 0xFFAA00;
        } else {
            return ChatFormatting.RED.getColor() != null ? ChatFormatting.RED.getColor() : 0xFF5555;
        }
    }
}
