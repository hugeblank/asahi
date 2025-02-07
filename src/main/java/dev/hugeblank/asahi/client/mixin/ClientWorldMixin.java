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
            this.timeProperty = new InterpolatedTickProperty("time", this::setTime, this::getTime);
            this.dayTimeProperty = new InterpolatedTickProperty("timeOfDay", this::setTimeOfDay, this::getTimeOfDay);
        }
    }

    @Inject(at=@At("HEAD"), method = "tickTime", cancellable = true)
    public void tickTime(CallbackInfo ci) {
        if (this.isClient) {
            timeProperty.tick();
            if (shouldTickDay) dayTimeProperty.tick();
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
        shouldTickDay = packet.getTimeOfDay() > 0;

        timeProperty.update(packet.getTime());
        dayTimeProperty.update(shouldTickDay ? packet.getTimeOfDay() : packet.getTimeOfDay()*-1);
    }
}
