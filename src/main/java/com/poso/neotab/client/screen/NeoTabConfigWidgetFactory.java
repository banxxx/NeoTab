package com.poso.neotab.client.screen;

import com.poso.neotab.config.HealthDisplayMode;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Factory class for creating widgets used in NeoTabConfigScreen.
 */
public class NeoTabConfigWidgetFactory {
    private static final int INPUT_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 56;

    private NeoTabConfigWidgetFactory() {}

    /**
     * Create a simple ON/OFF toggle button.
     */
    public static CycleButton<Boolean> newToggle(int x, boolean initialValue) {
        return CycleButton.onOffBuilder(initialValue)
            .displayOnlyValue()
            .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                (button, value) -> { /* value read on save */ });
    }

    /**
     * Create a labeled ON/OFF toggle button.
     */
    public static CycleButton<Boolean> newLabeledToggle(int x, int width, boolean initialValue, Component label) {
        return CycleButton.onOffBuilder(initialValue)
            .displayOnlyValue()
            .create(x, 0, width, INPUT_HEIGHT, label,
                (button, value) -> { /* value read on save */ });
    }

    /**
     * Create a health display mode cycle button.
     */
    public static CycleButton<HealthDisplayMode> newHealthModeButton(int x, HealthDisplayMode initialValue, Runnable onValueChange) {
        return CycleButton.builder((HealthDisplayMode mode) ->
                Component.translatable("screen.neotab.health_mode." + mode.toId()))
            .withValues(HealthDisplayMode.values())
            .withInitialValue(initialValue)
            .displayOnlyValue()
            .create(x, 0, TOGGLE_WIDTH, INPUT_HEIGHT, CommonComponents.EMPTY,
                (button, value) -> {
                    if (onValueChange != null) {
                        onValueChange.run();
                    }
                });
    }
}
