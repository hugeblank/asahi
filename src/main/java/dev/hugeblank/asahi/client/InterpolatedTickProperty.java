package dev.hugeblank.asahi.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.TickRateManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InterpolatedTickProperty {

    private final EvictingList<Double> points = new EvictingList<>(Launch.CONFIG.interpolateSamples());
    private double factor = Launch.CONFIG.initialFactor();
    private double remainder = 0;

    private final String prefix;
    private final Consumer<Long> setProperty;
    private final Supplier<Long> getProperty;
    private final TickRateManager tickRateManager;

    public InterpolatedTickProperty(String prefix, Consumer<Long> setProperty, Supplier<Long> getProperty, TickRateManager tickRateManager) {
        this.prefix = prefix;
        this.setProperty = setProperty;
        this.getProperty = getProperty;
        this.tickRateManager = tickRateManager;
    }

    public void tick() {
        remainder += factor; // add factor to remainder
        long increment = (long) remainder; // truncate floating value
        setProperty.accept(getProperty.get() + increment);
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
    }

    public void update(long serverValue) {

        // If the next value would take more than `skipDuration` seconds at the current TPS to reach, just skip to the position.
        float tickRate = tickRateManager.tickrate();
        if (Math.abs(serverValue-getProperty.get()) >= Launch.CONFIG.skipDuration() * tickRate) {
            factor = Launch.CONFIG.initialFactor();
            setProperty.accept(serverValue);
        } else if (tickRateManager.runsNormally()){
            int localDiff = (int) (serverValue - getProperty.get());
            points.add((double) (localDiff + tickRate) / Launch.CONFIG.standardTickRate());
            double avg = 0, weights = 0; // weighted average
            int size = points.size();
            for (int i = 0; i < size; i++) {
                double weight = size - i + 1;
                weight *= weight;
                weights += weight;
                avg += points.get(i) * weight;
            }
            factor = avg / weights;
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                System.out.format("%s: %s server by %d ticks. Speed: %f. Tick Rate: %f.\n", prefix, (localDiff < 0 ? "ahead of" : "behind"), Math.abs(localDiff), factor, tickRate);
        }
    }
}
