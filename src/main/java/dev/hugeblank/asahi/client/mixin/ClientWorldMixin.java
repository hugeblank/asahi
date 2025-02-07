package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.EvictingList;
import dev.hugeblank.asahi.client.TimeSmoother;
import dev.hugeblank.asahi.client.InterpolatedTickProperty;
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
    @Unique private long lastPacketTimeOfDay = 0;
    @Unique private boolean shouldTickDay = true;

    @Unique private InterpolatedTickProperty timeProperty;
    @Unique private InterpolatedTickProperty dayTimeProperty;

    protected ClientWorldMixin(
            LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> biFunction, Profiler profiler, boolean bl
    ) {
        super();
    }

    @Inject(at=@At("TAIL"), method = "<init>")
    private void init(LevelProperties levelProperties, DimensionType dimensionType, BiFunction biFunction, Profiler profiler, boolean bl, CallbackInfo ci) {
        if (this.isClient) {
            this.timeProperty = new InterpolatedTickProperty(this::setTime, this::getTime);
            this.dayTimeProperty = new InterpolatedTickProperty(this::setTimeOfDay, this::getTimeOfDay);
        }
    }

    @Inject(at=@At("HEAD"), method = "tickTime", cancellable = true)
    public void tickTime(CallbackInfo ci) {
        if (this.isClient) {
            timeProperty.increment();
            if (shouldTickDay) dayTimeProperty.increment();
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
        if (lastPacketTimeOfDay == packet.getTimeOfDay()) {
            if (!shouldTickDay) {
                this.setTimeOfDay(packet.getTimeOfDay());
            }
            shouldTickDay = false;
        } else {
            shouldTickDay = true;
        }

        final int TPS = 20;
        int localDiff = (int) (packet.getTime() - this.getTime());
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
            double factor = avg < 0 ? Math.min(avg, -minMoveFactor) : Math.max(avg, minMoveFactor);
            timeProperty.setFactor(factor);
            dayTimeProperty.setFactor(factor);

            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                System.out.format("%s server by %d ticks. Speed: %f\n", (localDiff < 0 ? "ahead of" : "behind"), Math.abs(localDiff), avg);
        }
        lastPacketTimeOfDay = packet.getTimeOfDay();
    }
}
