package com.poso.neotab.client.screen;

import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.network.NeoTabNetwork;
import com.poso.neotab.network.packet.SavePlayerConfigPacket;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

/**
 * NeoTab 玩家个人自定义界面。
 * 
 * <p>从 NeoForge 1.21.1 移植到 Forge 1.20.1 的主要适配：</p>
 * <ul>
 *     <li>检查 Screen 构造函数兼容性</li>
 *     <li>更新 GuiGraphics 渲染方法调用</li>
 *     <li>适配按钮创建和事件处理</li>
 *     <li>更新网络包发送方式</li>
 * </ul>
 */
public class NeoTabCustomizeScreen extends Screen {
    
    private final PlayerTabConfig originalConfig;
    private final PlayerCustomizePolicy policy;
    private PlayerTabConfig workingConfig;
    
    // GUI组件
    private Button saveButton;
    private Button cancelButton;
    private Button resetButton;
    
    public NeoTabCustomizeScreen(PlayerTabConfig config, PlayerCustomizePolicy policy) {
        super(Component.translatable("screen.neotab.customize.title"));
        this.originalConfig = config;
        this.policy = policy;
        this.workingConfig = config; // 后续会实现深拷贝
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 计算按钮位置
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonSpacing = 10;
        int totalButtonWidth = buttonWidth * 3 + buttonSpacing * 2;
        int startX = (this.width - totalButtonWidth) / 2;
        int buttonY = this.height - 40;
        
        // 保存按钮
        this.saveButton = Button.builder(
            Component.translatable("screen.neotab.customize.save"),
            button -> saveConfig()
        ).bounds(startX, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.saveButton);
        
        // 重置按钮
        this.resetButton = Button.builder(
            Component.translatable("screen.neotab.customize.reset"),
            button -> resetConfig()
        ).bounds(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.resetButton);
        
        // 取消按钮
        this.cancelButton = Button.builder(
            Component.translatable("screen.neotab.customize.cancel"),
            button -> onClose()
        ).bounds(startX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.cancelButton);
        
        // TODO: 根据策略添加可用的配置控件
        // 这里将在后续实现具体的自定义选项控件
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics);
        
        // 渲染标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 渲染说明文本
        Component description = Component.translatable("screen.neotab.customize.description");
        guiGraphics.drawCenteredString(this.font, description, this.width / 2, 40, 0xAAAAAA);
        
        // TODO: 渲染自定义选项
        // 这里将在后续实现具体的自定义界面内容
        // 需要根据 policy 来决定哪些选项可用
        
        // 渲染父类组件（按钮等）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 保存个人配置。
     */
    private void saveConfig() {
        try {
            // 发送保存个人配置网络包到服务端
            NeoTabNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), 
                new SavePlayerConfigPacket(workingConfig));
            
            // 关闭界面
            onClose();
        } catch (Exception e) {
            // TODO: 显示错误消息
            onClose();
        }
    }
    
    /**
     * 重置配置到默认值。
     */
    private void resetConfig() {
        // 重置为跟随服务器的默认配置
        this.workingConfig = PlayerTabConfig.defaults(originalConfig.playerId());
        
        // TODO: 更新界面控件状态
        // 这里需要重新初始化所有控件的值
    }
    
    @Override
    public void onClose() {
        // 返回游戏
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        // 自定义界面不暂停游戏
        return false;
    }
}