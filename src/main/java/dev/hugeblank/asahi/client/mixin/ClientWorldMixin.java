package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.TimeSmoother;
import dev.hugeblank.asahi.client.InterpolatedTickProperty;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements TimeSmoother {

    @Shadow @Final private ClientWorld.Properties clientWorldProperties;

    @Unique private boolean shouldTickDay = true;

    @Unique private InterpolatedTickProperty timeProperty;
    @Unique private InterpolatedTickProperty dayTimeProperty;

    protected ClientWorldMixin(
            MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager,
            RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient,
            boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates
    ) {
        super(
                properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess,
                maxChainedNeighborUpdates
        );
    }

    @Inject(at=@At("TAIL"), method = "<init>")
    private void init(ClientPlayNetworkHandler netHandler, ClientWorld.Properties properties, RegistryKey registryRef, RegistryEntry registryEntry, int loadDistance, int simulationDistance, Supplier profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        if (this.isClient) {
            this.timeProperty = new InterpolatedTickProperty("time", this.clientWorldProperties::setTime, this.clientWorldProperties::getTime);
            this.dayTimeProperty = new InterpolatedTickProperty("timeOfDay", this.clientWorldProperties::setTimeOfDay, this.clientWorldProperties::getTimeOfDay);
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

    @Override
    public void asahi$updateTimes(WorldTimeUpdateS2CPacket packet) {
        shouldTickDay = packet.getTimeOfDay() > 0;

        timeProperty.update(packet.getTime());
        dayTimeProperty.update(shouldTickDay ? packet.getTimeOfDay() : packet.getTimeOfDay()*-1);
    }
}
