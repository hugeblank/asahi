package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.EvictingList;
import dev.hugeblank.asahi.client.TimeSmoother;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements TimeSmoother {

    @Shadow @Final private ClientWorld.Properties clientWorldProperties;

    @Unique private final EvictingList<Double> points = new EvictingList<>(10);
    @Unique private double factor = 0D;
    @Unique private double remainder = 0D;

    protected ClientWorldMixin(
        MutableWorldProperties properties,
        RegistryKey<World> registryManager,
        RegistryEntry<DimensionType> registryEntry,
        Supplier<Profiler> profiler,
        boolean isClient,
        boolean debugWorld,
        long seed
    ) {
        super(properties, registryManager, registryEntry, profiler, isClient, debugWorld, seed);
    }

    /**
     * @author hugeblank
     * @reason Smooth out daylight cycle & remove client de-sync jitter.
     */
    @Overwrite
    private void tickTime() {
        remainder += factor; // add remainder to factor
        long increment = (long) remainder; // truncate floating value
        clientWorldProperties.setTime(properties.getTime() + increment);
        if (properties.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            clientWorldProperties.setTimeOfDay(properties.getTimeOfDay() + increment);
        }
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
    }

    @Override
    public void asahi$updateTimes(WorldTimeUpdateS2CPacket packet) {
        final int TPS = 20;
        long currentPacketTime = packet.getTimeOfDay();
        int localDiff = (int) (currentPacketTime - properties.getTimeOfDay());
        if (Math.abs(localDiff) >= 60* TPS) { // SKIP_DURATION
            clientWorldProperties.setTime(packet.getTime());
            clientWorldProperties.setTimeOfDay(packet.getTimeOfDay());
        } else {
            float minMoveFactor = 1f/ TPS; // MIN_MOVE_FACTOR
            points.add((double) (localDiff + TPS) / TPS);
            double avg = 0, weights = 0; // weighted average
            int size = points.size();
            for (int i = 0; i < size; i++) {
                double weight = size - i + 1;
                weight *= weight;
                weights += weight;
                avg += points.get(i)*weight;
            }
            avg /= weights;
            // TODO: Debug logging that doesn't show up in prod
            // System.out.println((localDiff < 0 ? "ahead of" : "behind") + " server by " + Math.abs(localDiff) + " ticks. Speed: " + avg);
            factor = avg < 0 ? Math.min(avg, -minMoveFactor) : Math.max(avg, minMoveFactor);
        }
    }
}
