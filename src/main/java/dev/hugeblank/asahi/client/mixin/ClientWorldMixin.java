package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.EvictingList;
import dev.hugeblank.asahi.client.TimeSmoother;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.ExtendedBlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(World.class)
public abstract class ClientWorldMixin implements ExtendedBlockView, IWorld, AutoCloseable, TimeSmoother {

    @Unique private final EvictingList<Double> points = new EvictingList<>(10);
    @Unique private double factor = 0D;
    @Unique private double remainder = 0D;
    @Unique private long lastPacketTimeOfDay = 0;
    @Unique private boolean shouldTickDay = true;

    protected ClientWorldMixin(
            LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> biFunction, Profiler profiler, boolean bl
    ) {
        super();
    }

    @Inject(at=@At("HEAD"), method = "tickTime", cancellable = true)
    public void tickTime(CallbackInfo ci) {
        if (this.isClient) {
            remainder += factor; // add remainder to factor
            long increment = (long) remainder; // truncate floating value
            this.setTime(this.getTime() + increment);
            if (shouldTickDay) this.setTimeOfDay(this.getTimeOfDay() + increment);
            // subtract the incremented integer, preserving the floating point remainder for later
            remainder -= increment;
            ci.cancel();
        }
    }

    @Shadow public abstract void setTime(long l);

    @Shadow public abstract void setTimeOfDay(long l);

    @Shadow public abstract long getTime();

    @Shadow public abstract long getTimeOfDay();

    @Shadow @Final public boolean isClient;

    @Override
    public void asahi$updateTimes(WorldTimeUpdateS2CPacket packet) {
        final int TPS = 20;
        long currentPacketTime = packet.getTime();
        if (lastPacketTimeOfDay == packet.getTimeOfDay()) {
            if (!shouldTickDay) {
                this.setTimeOfDay(packet.getTimeOfDay());
            }
            shouldTickDay = false;
        } else {
            shouldTickDay = true;
        }
        int localDiff = (int) (currentPacketTime - this.getTime());
        if (Math.abs(localDiff) >= 60 * TPS) { // SKIP_DURATION
            this.setTime(packet.getTime());
            if (shouldTickDay) this.setTimeOfDay(packet.getTimeOfDay());
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
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                System.out.println((localDiff < 0 ? "ahead of" : "behind") + " server by " + Math.abs(localDiff) + " ticks. Speed: " + avg);
            factor = avg < 0 ? Math.min(avg, -minMoveFactor) : Math.max(avg, minMoveFactor);
            lastPacketTimeOfDay = packet.getTimeOfDay();
        }
    }
}
