package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.EvictingList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin{

    @Shadow @Final private ClientWorld.Properties clientWorldProperties;

    @Shadow @Final private TickManager tickManager;

    @Shadow private boolean shouldTickTimeOfDay;

    @Unique private final EvictingList<Double> points = new EvictingList<>(10);
    @Unique private double factor = 0D;
    @Unique private double remainder = 0D;


    // Poor man's non-nuclear redirect
    @Inject(at=@At("HEAD"), method = "tickTime", cancellable = true, order = 10000)
    private void tickTime(CallbackInfo ci) {
        remainder += factor; // add remainder to factor
        long increment = (long) remainder; // truncate floating value
        clientWorldProperties.setTime(clientWorldProperties.getTime() + increment);
        if (this.shouldTickTimeOfDay)
            clientWorldProperties.setTimeOfDay(clientWorldProperties.getTimeOfDay() + increment);
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
        ci.cancel();
    }

    @Inject(at=@At("HEAD"), method = "setTime", cancellable = true, order = 10000)
    public void setTime(long time, long timeOfDay, boolean tickTimeOfDay, CallbackInfo ci) {
        float tickRate = tickManager.getTickRate(); // Get the TPS
        int localDiff = (int) (time - clientWorldProperties.getTime());
        // If the next position is greater than where the cycle would be 60 seconds from now, just snap to the position.
        // We do this instead of rapidly speeding the cycle up to its true position (i.e. after sleeping).
        if (Math.abs(localDiff) >= 60*tickRate) {
            clientWorldProperties.setTime(time);
            if (tickTimeOfDay || clientWorldProperties.getTimeOfDay() != timeOfDay)
                // Only snap the time of day when necessary.
                // (When doDaylightCycle == true, if the time of day is out of sync from the server)
                clientWorldProperties.setTimeOfDay(timeOfDay);
        } else {
            float minMoveFactor = 1f/tickRate; // Create a minimum move factor, so that the sun never appears frozen.
            points.add((double) (localDiff + tickRate) / tickRate); // Project where the sun will be in the next second
            // Get the weighted average of the last n ticks
            // (see EvictingList instantiation for n)
            double avg = 0, weights = 0;
            int size = points.size();
            for (int i = 0; i < size; i++) {
                double weight = size - i + 1;
                weight *= weight;
                weights += weight;
                avg += points.get(i)*weight;
            }
            avg /= weights;
            factor = avg < 0 ? Math.min(avg, -minMoveFactor) : Math.max(avg, minMoveFactor);
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                System.out.println((localDiff < 0 ? "ahead of" : "behind") + " server by " + Math.abs(localDiff) + " ticks. Speed: " + avg);
        }
        this.shouldTickTimeOfDay = tickTimeOfDay;
        ci.cancel();
    }
}
