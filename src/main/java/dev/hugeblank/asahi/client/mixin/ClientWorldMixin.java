package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.EvictingList;
import dev.hugeblank.asahi.client.TimeSmoother;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.ExtendedBlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.*;

import java.util.function.BiFunction;

@Mixin(World.class)
public abstract class ClientWorldMixin implements ExtendedBlockView, IWorld, AutoCloseable, TimeSmoother {

    @Unique private final EvictingList<Double> points = new EvictingList<>(10);
    @Unique private double factor = 0D;
    @Unique private double remainder = 0D;

    protected ClientWorldMixin(
            LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> biFunction, Profiler profiler, boolean bl
    ) {
        super();
    }

    /**
     * @author hugeblank
     * @reason Smooth out daylight cycle & remove client de-sync jitter.
     */
    @Shadow
    @Overwrite
    protected void tickTime() {
        remainder += factor; // add remainder to factor
        long increment = (long) remainder; // truncate floating value
        this.setTime(this.getTime() + increment);
        if (this.getGameRules().getBoolean(GameRules.getKeys().get("doDaylightCycle").toString())) {
            this.setTimeOfDay(this.getTimeOfDay() + increment);
        }
        // subtract the incremented integer, preserving the floating point remainder for later
        remainder -= increment;
    }

    @Shadow public abstract void setTime(long l);

    @Shadow public abstract void setTimeOfDay(long l);

    @Shadow public abstract long getTime();

    @Shadow public abstract GameRules getGameRules();

    @Shadow public abstract long getTimeOfDay();

    @Override
    public void asahi$updateTimes(WorldTimeUpdateS2CPacket packet) {
        final int TPS = 20;
        long currentPacketTime = packet.getTimeOfDay();
        int localDiff = (int) (currentPacketTime - this.getTimeOfDay());
        if (Math.abs(localDiff) >= 60* TPS) { // SKIP_DURATION
            this.setTime(packet.getTime());
            this.setTimeOfDay(packet.getTimeOfDay());
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
