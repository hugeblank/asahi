package dev.hugeblank.asahi.client;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InterpolatedTickProperty {
    public static final byte TPS = 20;

    private final EvictingList<Double> points = new EvictingList<>(10);
    private double factor = 1;
    private double remainder = 0;

    private final String prefix;
    private final Consumer<Long> setProperty;
    private final Supplier<Long> getProperty;

    public InterpolatedTickProperty(String prefix, Consumer<Long> setProperty, Supplier<Long> getProperty) {
        this.prefix = prefix;
        this.setProperty = setProperty;
        this.getProperty = getProperty;
    }

    public void tick() {
        remainder += factor; // add remainder to factor
        long increment = (long) remainder; // truncate floating value
        setProperty.accept(getProperty.get() + increment);
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
    }

    public void update(long serverValue) {
        int localDiff = (int) (serverValue - getProperty.get());
        float minMoveFactor = 1f / TPS; // MIN_MOVE_FACTOR
        points.add((double) (localDiff + TPS) / TPS);
        double avg = 0, weights = 0; // weighted average
        int size = points.size();
        for (int i = 0; i < size; i++) {
            double weight = size - i + 1;
            weight *= weight;
            weights += weight;
            avg += points.get(i) * weight;
        }
        avg /= weights;
        double factor = avg < 0 ? Math.min(avg, -minMoveFactor) : Math.max(avg, minMoveFactor);

        // If the next value would take more than 60 seconds at the current TPS to reach, just skip to the position.
        if (Math.abs(serverValue-getProperty.get()) >= 60 * TPS) {
            this.factor = 1;
            setProperty.accept(serverValue);
        } else {
            this.factor = factor;
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                System.out.format("%s: %s server by %d ticks. Speed: %f\n", prefix, (localDiff < 0 ? "ahead of" : "behind"), Math.abs(localDiff), avg);
        }
    }
}
