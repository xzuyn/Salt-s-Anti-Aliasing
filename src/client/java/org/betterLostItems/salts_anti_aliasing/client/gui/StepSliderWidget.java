package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Slider that steps through the values of an enum (for example Off, 2x, 4x, ...) and saves each
 * change. The caller decides what a change does, and may refuse it by returning the value that is
 * actually in effect.
 */
public final class StepSliderWidget<E extends Enum<E>> extends AbstractSliderButton {
    private static final int ROW_WIDTH = 150;
    private static final int ROW_HEIGHT = 20;

    private final E[] steps;
    private final Supplier<E> current;
    private final UnaryOperator<E> request;
    private final Function<E, Component> messageFor;
    private final Function<E, Component> tooltipFor;

    /**
     * @param steps      the selectable values, in slider order
     * @param current    reads the value currently in effect
     * @param request    applies a requested value and returns the value now in effect
     * @param messageFor slider text for a value
     * @param tooltipFor tooltip for a value
     */
    public StepSliderWidget(
            E[] steps,
            Supplier<E> current,
            UnaryOperator<E> request,
            Function<E, Component> messageFor,
            Function<E, Component> tooltipFor
    ) {
        super(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty(), position(steps, current.get()));
        this.steps = steps;
        this.current = current;
        this.request = request;
        this.messageFor = messageFor;
        this.tooltipFor = tooltipFor;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        E value = current.get();
        this.setMessage(messageFor.apply(value));
        this.setTooltip(Tooltip.create(tooltipFor.apply(value)));
    }

    @Override
    protected void applyValue() {
        this.value = position(steps, request.apply(stepAt(steps, this.value)));
        updateMessage();
    }

    /** Slider position (0..1) of a value. */
    private static <E extends Enum<E>> double position(E[] steps, E value) {
        return (double) value.ordinal() / (steps.length - 1);
    }

    /** The value nearest to a slider position (0..1). */
    private static <E extends Enum<E>> E stepAt(E[] steps, double sliderValue) {
        int maxIndex = steps.length - 1;
        int index = (int) Math.round(Math.max(0.0d, Math.min(1.0d, sliderValue)) * maxIndex);
        return steps[Math.max(0, Math.min(maxIndex, index))];
    }
}
