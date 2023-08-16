package dev.hugeblank.zenith.client.mixin;

import dev.hugeblank.zenith.client.Constants;
import dev.hugeblank.zenith.client.TimeSmoother;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements TimeSmoother {

    @Shadow
    @Final
    private ClientWorld.Properties clientWorldProperties;

    @Shadow public abstract void processPendingUpdate(BlockPos pos, BlockState state, Vec3d playerPos);

    @Unique
    private double factor = 0D;
    @Unique
    private double remainder = 0D;

    protected ClientWorldMixin(
            MutableWorldProperties properties,
            RegistryKey<World> registryRef,
            DynamicRegistryManager registryManager,
            RegistryEntry<DimensionType> dimensionEntry,
            Supplier<Profiler> profiler,
            boolean isClient,
            boolean debugWorld,
            long biomeAccess,
            int maxChainedNeighborUpdates
    ) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Unique
    private void zenith$setTime(long time) {
        this.clientWorldProperties.setTime(time);
    }

    @Unique
    private void zenith$setTimeOfDay(long timeOfDay) {
        if (timeOfDay < 0L) {
            timeOfDay = -timeOfDay;
            properties.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, null);
        } else {
            properties.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, null);
        }
        this.clientWorldProperties.setTimeOfDay(timeOfDay);
    }


    /**
     * @author hugeblank
     * @reason Smooth out daylight cycle & remove client de-sync jitter.
     */
    @Overwrite
    private void tickTime() {
        remainder += factor;
        long increment = (long) remainder;
        zenith$setTime(properties.getTime() + increment);
        if (properties.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            zenith$setTimeOfDay(properties.getTimeOfDay() + increment);
        }
        remainder = remainder-increment;
    }

    @Override
    public void zenith$updateTimes(WorldTimeUpdateS2CPacket packet) {
        long diff = packet.getTimeOfDay() - properties.getTimeOfDay();
        System.out.println(diff);
        if (Math.abs(diff) >= Constants.SKIP_DURATION) {
            zenith$setTime(packet.getTime());
            zenith$setTimeOfDay(packet.getTimeOfDay());
        }
        factor = Math.max( (double) (diff + Constants.TPS) / Constants.TPS, Constants.MIN_MOVE_FACTOR);
    }
}
