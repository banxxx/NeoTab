package com.poso.neotab.client.screen;

import com.poso.neotab.config.HealthDisplayMode;
import com.poso.neotab.config.PlayerTabConfig;
import com.poso.neotab.network.NeoTabNetwork;
import com.poso.neotab.network.packet.SavePlayerConfigPacket;
import com.poso.neotab.permission.PlayerCustomizePolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
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
    
    // 配置控件
    private CycleButton<Boolean> topTitleToggle;
    private EditBox topTitleEdit;
    private CycleButton<Boolean> pingToggle;
    private CycleButton<Boolean> durationToggle;
    private CycleButton<Boolean> titleToggle;
    private CycleButton<Boolean> healthToggle;
    private CycleButton<HealthDisplayMode> healthModeCycle;
    private EditBox footerEdit;
    private CycleButton<Boolean> tpsToggle;
    private CycleButton<Boolean> msptToggle;
    private CycleButton<Boolean> onlineToggle;
    
    public NeoTabCustomizeScreen(PlayerTabConfig config, PlayerCustomizePolicy policy) {
        super(Component.translatable("screen.neotab.customize.title"));
        this.originalConfig = deepCopy(config);
        this.policy = policy;
        this.workingConfig = deepCopy(config);
    }
    
    /**
     * 深拷贝 PlayerTabConfig。
     */
    private PlayerTabConfig deepCopy(PlayerTabConfig config) {
        return new PlayerTabConfig(
            config.playerId(),
            config.topTitleEnabled(),
            config.topTitleText(),
            config.topContentEnabled(),
            config.topContentText(),
            config.betterPingEnabled(),
            config.onlineDurationEnabled(),
            config.titleEnabled(),
            config.healthDisplayEnabled(),
            config.healthDisplayMode(),
            config.footerCustomText(),
            config.footerTpsEnabled(),
            config.footerMsptEnabled(),
            config.footerOnlineEnabled(),
            config.tabTheme()
        );
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
        
        // 添加配置控件
        int y = 60;
        int leftX = this.width / 2 - 150;
        
        // 顶部信息
        if (policy.allowTopTitleToggle()) {
            topTitleToggle = CycleButton.onOffBuilder(
                workingConfig.topTitleEnabled() != null ? workingConfig.topTitleEnabled() : false
            ).create(0, 0, 100, 20, 
                Component.translatable("screen.neotab.customize.top_title_toggle"),
                (button, value) -> {
                    workingConfig = new PlayerTabConfig(
                        workingConfig.playerId(), value, workingConfig.topTitleText(),
                        workingConfig.topContentEnabled(), workingConfig.topContentText(),
                        workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                        workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                        workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                        workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                        workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                    );
                }
            );
            topTitleToggle.setPosition(leftX, y);
            this.addRenderableWidget(topTitleToggle);
            y += 25;
        }
        
        if (policy.allowTopTitleEdit()) {
            topTitleEdit = new EditBox(this.font, leftX, y, 300, 20, 
                Component.translatable("screen.neotab.customize.top_title_text"));
            topTitleEdit.setValue(workingConfig.topTitleText() != null ? workingConfig.topTitleText() : "");
            topTitleEdit.setResponder(value -> {
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), value,
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
            });
            this.addRenderableWidget(topTitleEdit);
            y += 30;
        }
        
        // 玩家列表选项
        if (policy.allowPingDisplayToggle()) {
            pingToggle = CycleButton.onOffBuilder(
                workingConfig.betterPingEnabled() != null ? workingConfig.betterPingEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.ping_toggle"),
                (button, value) -> updateBooleanField("betterPingEnabled", value)
            );
            pingToggle.setPosition(leftX, y);
            this.addRenderableWidget(pingToggle);
            y += 25;
        }
        
        if (policy.allowDurationToggle()) {
            durationToggle = CycleButton.onOffBuilder(
                workingConfig.onlineDurationEnabled() != null ? workingConfig.onlineDurationEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.duration_toggle"),
                (button, value) -> updateBooleanField("onlineDurationEnabled", value)
            );
            durationToggle.setPosition(leftX, y);
            this.addRenderableWidget(durationToggle);
            y += 25;
        }
        
        if (policy.allowTitleToggle()) {
            titleToggle = CycleButton.onOffBuilder(
                workingConfig.titleEnabled() != null ? workingConfig.titleEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.title_toggle"),
                (button, value) -> updateBooleanField("titleEnabled", value)
            );
            titleToggle.setPosition(leftX, y);
            this.addRenderableWidget(titleToggle);
            y += 25;
        }
        
        if (policy.allowHealthDisplayToggle()) {
            healthToggle = CycleButton.onOffBuilder(
                workingConfig.healthDisplayEnabled() != null ? workingConfig.healthDisplayEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.health_toggle"),
                (button, value) -> updateBooleanField("healthDisplayEnabled", value)
            );
            healthToggle.setPosition(leftX, y);
            this.addRenderableWidget(healthToggle);
            y += 25;
        }
        
        if (policy.allowHealthModeChange()) {
            healthModeCycle = CycleButton.builder(HealthDisplayMode::getDisplayName)
                .withValues(HealthDisplayMode.values())
                .withInitialValue(workingConfig.healthDisplayMode() != null ? workingConfig.healthDisplayMode() : HealthDisplayMode.FULL)
                .create(0, 0, 150, 20,
                    Component.translatable("screen.neotab.customize.health_mode"),
                    (button, value) -> {
                        workingConfig = new PlayerTabConfig(
                            workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                            workingConfig.topContentEnabled(), workingConfig.topContentText(),
                            workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                            workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                            value, workingConfig.footerCustomText(),
                            workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                            workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                        );
                    }
                );
            healthModeCycle.setPosition(leftX, y);
            this.addRenderableWidget(healthModeCycle);
            y += 25;
        }
        
        // 底部信息
        if (policy.allowFooterCustomEdit()) {
            footerEdit = new EditBox(this.font, leftX, y, 300, 20,
                Component.translatable("screen.neotab.customize.footer_text"));
            footerEdit.setValue(workingConfig.footerCustomText() != null ? workingConfig.footerCustomText() : "");
            footerEdit.setResponder(value -> {
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), value,
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
            });
            this.addRenderableWidget(footerEdit);
            y += 30;
        }
        
        if (policy.allowFooterTpsToggle()) {
            tpsToggle = CycleButton.onOffBuilder(
                workingConfig.footerTpsEnabled() != null ? workingConfig.footerTpsEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.tps_toggle"),
                (button, value) -> updateBooleanField("footerTpsEnabled", value)
            );
            tpsToggle.setPosition(leftX, y);
            this.addRenderableWidget(tpsToggle);
            y += 25;
        }
        
        if (policy.allowFooterMsptToggle()) {
            msptToggle = CycleButton.onOffBuilder(
                workingConfig.footerMsptEnabled() != null ? workingConfig.footerMsptEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.mspt_toggle"),
                (button, value) -> updateBooleanField("footerMsptEnabled", value)
            );
            msptToggle.setPosition(leftX, y);
            this.addRenderableWidget(msptToggle);
            y += 25;
        }
        
        if (policy.allowFooterOnlineToggle()) {
            onlineToggle = CycleButton.onOffBuilder(
                workingConfig.footerOnlineEnabled() != null ? workingConfig.footerOnlineEnabled() : false
            ).create(0, 0, 100, 20,
                Component.translatable("screen.neotab.customize.online_toggle"),
                (button, value) -> updateBooleanField("footerOnlineEnabled", value)
            );
            onlineToggle.setPosition(leftX, y);
            this.addRenderableWidget(onlineToggle);
        }
    }
    
    /**
     * 更新布尔字段。
     */
    private void updateBooleanField(String fieldName, boolean value) {
        switch (fieldName) {
            case "betterPingEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    value, workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "onlineDurationEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), value,
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "titleEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    value, workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "healthDisplayEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), value,
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "footerTpsEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    value, workingConfig.footerMsptEnabled(),
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "footerMsptEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), value,
                    workingConfig.footerOnlineEnabled(), workingConfig.tabTheme()
                );
                break;
            case "footerOnlineEnabled":
                workingConfig = new PlayerTabConfig(
                    workingConfig.playerId(), workingConfig.topTitleEnabled(), workingConfig.topTitleText(),
                    workingConfig.topContentEnabled(), workingConfig.topContentText(),
                    workingConfig.betterPingEnabled(), workingConfig.onlineDurationEnabled(),
                    workingConfig.titleEnabled(), workingConfig.healthDisplayEnabled(),
                    workingConfig.healthDisplayMode(), workingConfig.footerCustomText(),
                    workingConfig.footerTpsEnabled(), workingConfig.footerMsptEnabled(),
                    value, workingConfig.tabTheme()
                );
                break;
        }
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
        
        // 渲染控件标签
        int y = 60;
        int labelX = this.width / 2 - 150;
        
        if (policy.allowTopTitleToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.top_title_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowTopTitleEdit()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.top_title_text"), 
                labelX, y + 6, 0xFFFFFF);
            y += 30;
        }
        
        if (policy.allowPingDisplayToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.ping_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowDurationToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.duration_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowTitleToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.title_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowHealthDisplayToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.health_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowHealthModeChange()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.health_mode"), 
                labelX + 160, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowFooterCustomEdit()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.footer_text"), 
                labelX, y + 6, 0xFFFFFF);
            y += 30;
        }
        
        if (policy.allowFooterTpsToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.tps_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowFooterMsptToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.mspt_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
            y += 25;
        }
        
        if (policy.allowFooterOnlineToggle()) {
            guiGraphics.drawString(this.font, 
                Component.translatable("screen.neotab.customize.online_toggle"), 
                labelX + 110, y + 6, 0xFFFFFF);
        }
        
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
            // 显示错误消息
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("screen.neotab.customize.save_error"), 
                    false
                );
            }
        }
    }
    
    /**
     * 重置配置到默认值。
     */
    private void resetConfig() {
        // 重置为跟随服务器的默认配置
        this.workingConfig = PlayerTabConfig.defaults(originalConfig.playerId());
        
        // 更新界面控件状态
        if (topTitleToggle != null) {
            topTitleToggle.setValue(false);
        }
        if (topTitleEdit != null) {
            topTitleEdit.setValue("");
        }
        if (pingToggle != null) {
            pingToggle.setValue(false);
        }
        if (durationToggle != null) {
            durationToggle.setValue(false);
        }
        if (titleToggle != null) {
            titleToggle.setValue(false);
        }
        if (healthToggle != null) {
            healthToggle.setValue(false);
        }
        if (healthModeCycle != null) {
            healthModeCycle.setValue(HealthDisplayMode.FULL);
        }
        if (footerEdit != null) {
            footerEdit.setValue("");
        }
        if (tpsToggle != null) {
            tpsToggle.setValue(false);
        }
        if (msptToggle != null) {
            msptToggle.setValue(false);
        }
        if (onlineToggle != null) {
            onlineToggle.setValue(false);
        }
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