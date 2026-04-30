package com.poso.neotab.client.screen;

import com.poso.neotab.config.HealthDisplayMode;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * NeoTab 配置界面 Widget 工厂。
 *
 * <p>将 {@link NeoTabConfigScreen} 中所有 Widget 创建方法集中到此处，
 * 使主屏幕类专注于状态管理和事件处理。</p>
 *
 * <p>所有方法均为静态，不持有任何状态。</p>
 */
final class NeoTabConfigWidgetFactory {

    // 与 NeoTabConfigScreen 中保持一致的常量（避免跨类访问 private 成员）
    private static final int TOGGLE_WIDTH = 56;
    private static final int INPUT_HEIGHT = 20;

    private NeoTabConfigWidgetFactory() {}

    /**
     * 创建 ON/OFF 切换按钮（仅显示值，无标签）。
     */
    static CycleButton<Boolean> newToggle(int x, boolean initialValue) {
        return CycleButton.onOffBuilder(initialValue)
                .displayOnlyValue()
                .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY);
    }

    /**
     * 创建带标签的 ON/OFF 切换按钮。
     */
    static CycleButton<Boolean> newLabeledToggle(int x, int width, boolean initialValue, Component label) {
        return CycleButton.onOffBuilder(initialValue)
                .create(x, 0, width, INPUT_HEIGHT, label);
    }

    /**
     * 创建血量显示模式选择按钮（FULL / COMPACT 循环切换）。
     *
     * @param onModeChanged 模式变化时的回调（通常用于调整布局配置到新的限制范围）
     */
    static CycleButton<HealthDisplayMode> newHealthModeButton(int x, HealthDisplayMode initialValue,
                                                               Runnable onModeChanged) {
        return CycleButton.<HealthDisplayMode>builder(mode -> Component.translatable(
                        mode == HealthDisplayMode.FULL
                                ? "screen.neotab.theme.health_mode.full"
                                : "screen.neotab.theme.health_mode.compact"))
                .withValues(HealthDisplayMode.FULL, HealthDisplayMode.COMPACT)
                .withInitialValue(initialValue)
                .displayOnlyValue()
                .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                        (button, newMode) -> onModeChanged.run());
    }
}
