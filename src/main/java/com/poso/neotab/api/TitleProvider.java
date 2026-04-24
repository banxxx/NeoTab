package com.poso.neotab.api;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 称号提供者接口。
 * 
 * <p>其他模组可以实现此接口来为 NeoTab 提供玩家称号数据。</p>
 * 
 * <p><strong>使用方法：</strong></p>
 * <ol>
 *     <li>实现此接口</li>
 *     <li>在模组初始化时调用 {@code NeoTabAPI.registerTitleProvider(yourProvider)}</li>
 *     <li>NeoTab 会自动调用所有注册的提供者来获取称号</li>
 * </ol>
 * 
 * <p><strong>称号文本格式：</strong></p>
 * <ul>
 *     <li>支持纯文本：{@code "『管理员』"}</li>
 *     <li>支持颜色标签：{@code "<color #FF0000>『红色称号』</color>"}</li>
 *     <li>支持渐变标签：{@code "<gradient #FF0000,#00FF00>『渐变称号』</gradient>"}</li>
 *     <li>支持粗体标签：{@code "<bold>『粗体称号』</bold>"}</li>
 *     <li>可以组合使用：{@code "<bold><color #FFD700>『黄金管理员』</color></bold>"}</li>
 * </ul>
 */
public interface TitleProvider {
    
    /**
     * 获取玩家的称号文本。
     * 
     * @param player 玩家对象
     * @return 称号文本，支持富文本标签；如果该提供者不为此玩家提供称号，返回 null
     */
    @Nullable
    String getTitle(ServerPlayer player);
    
    /**
     * 获取此提供者的优先级。
     * 
     * <p>当多个提供者都为同一玩家提供称号时，优先级高的提供者会被优先使用。</p>
     * 
     * @return 优先级，数值越大优先级越高，默认为 0
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * 获取此提供者的标识符。
     * 
     * <p>用于调试和日志记录，建议使用模组ID作为前缀。</p>
     * 
     * @return 提供者标识符，例如 "mymod:rank_system"
     */
    String getProviderId();
}