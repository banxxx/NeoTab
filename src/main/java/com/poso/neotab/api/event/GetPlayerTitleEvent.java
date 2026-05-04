package com.poso.neotab.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * 获取玩家称号事件。
 * 
 * <p>当 NeoTab 需要获取玩家称号时会触发此事件。其他模组可以监听此事件
 * 并通过 {@link #setTitle(String)} 方法提供称号数据。</p>
 * 
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onGetPlayerTitle(GetPlayerTitleEvent event) {
 *     ServerPlayer player = event.getPlayer();
 *     String title = MyMod.getTitleForPlayer(player);
 *     if (title != null) {
 *         event.setTitle(title);
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>称号文本格式：</strong></p>
 * <ul>
 *     <li>支持纯文本：{@code "『管理员』"}</li>
 *     <li>支持颜色标签：{@code "<color #FF0000>『红色称号』</color>"}</li>
 *     <li>支持渐变标签：{@code "<gradient #FF0000,#00FF00>『渐变称号』</gradient>"}</li>
 *     <li>支持粗体标签：{@code "<bold>『粗体称号』</bold>"}</li>
 * </ul>
 */
public class GetPlayerTitleEvent extends Event {
    
    private final ServerPlayer player;
    private String title;
    
    public GetPlayerTitleEvent(ServerPlayer player) {
        this.player = player;
    }
    
    /**
     * 获取要查询称号的玩家。
     * 
     * @return 玩家对象
     */
    public ServerPlayer getPlayer() {
        return player;
    }
    
    /**
     * 获取当前设置的称号。
     * 
     * @return 称号文本，如果没有设置则返回 null
     */
    @Nullable
    public String getTitle() {
        return title;
    }
    
    /**
     * 设置玩家的称号。
     * 
     * <p>如果多个模组都调用此方法，后调用的会覆盖先调用的。
     * 建议模组在设置前检查是否已有其他模组设置了称号。</p>
     * 
     * @param title 称号文本，支持富文本标签；传入 null 表示清除称号
     */
    public void setTitle(@Nullable String title) {
        this.title = title;
    }
    
    /**
     * 检查是否已有称号被设置。
     * 
     * @return 如果已有称号则返回 true
     */
    public boolean hasTitle() {
        return title != null && !title.isEmpty();
    }
}