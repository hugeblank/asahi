package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.Constants;
import dev.hugeblank.asahi.client.TimeSmoother;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
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

    @Unique
    private double factor = 0D;
    @Unique
    private double remainder = 0D;
    @Unique
    private long lastPacketTime = 0L;

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
        remainder = remainder - increment;
    }

    @Override
    public void zenith$updateTimes(WorldTimeUpdateS2CPacket packet) {
        long currentPacketTime = packet.getTimeOfDay();
        long packetDiff = currentPacketTime - lastPacketTime;
        long localDiff = currentPacketTime - properties.getTimeOfDay();
        // System.out.println("packetDiff: " + packetDiff);
        // System.out.println("localDiff: " + localDiff); // TODO: Debug logging that doesn't show up in prod
        boolean skip = Math.abs(localDiff) >= Constants.SKIP_DURATION;
        if (packetDiff < 0) {
            if (skip) {
                zenith$setTime(packet.getTime());
                zenith$setTimeOfDay(packet.getTimeOfDay());
                factor = 0;
            } else {
                factor = Math.min( (double) (localDiff + Constants.TPS) / Constants.TPS, -Constants.MIN_MOVE_FACTOR);
            }
        } else {
            if (skip) {
                zenith$setTime(packet.getTime());
                zenith$setTimeOfDay(packet.getTimeOfDay());
                factor = 0;
            } else {
                factor = Math.max( (double) (localDiff + Constants.TPS) / Constants.TPS, Constants.MIN_MOVE_FACTOR);
            }
        }
        lastPacketTime = currentPacketTime;
    }
}
