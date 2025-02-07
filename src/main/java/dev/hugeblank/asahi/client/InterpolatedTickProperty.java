package dev.hugeblank.asahi.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InterpolatedTickProperty {
    private double factor = 0;
    private double remainder = 0;

    private final Consumer<Long> setProperty;
    private final Supplier<Long> getProperty;

    public InterpolatedTickProperty(Consumer<Long> setProperty, Supplier<Long> getProperty) {
        this.setProperty = setProperty;
        this.getProperty = getProperty;
    }

    public void increment() {
        remainder += factor; // add remainder to factor
        long increment = (long) remainder; // truncate floating value
        this.setProperty.accept(this.getProperty.get() + increment);
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
    }

    public void setFactor(double factor) {
        this.factor = factor;
    }
}
