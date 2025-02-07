package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.InterpolatedTickProperty;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin{

    @Shadow @Final private ClientWorld.Properties clientWorldProperties;

    @Shadow @Final private TickManager tickManager;

    @Unique private InterpolatedTickProperty timeProperty;
    @Unique private InterpolatedTickProperty dayTimeProperty;
    @Unique private boolean tickTimeOfDay = true;

    @Inject(at=@At("TAIL"), method = "<init>")
    private void init(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey registryRef, RegistryEntry dimensionType, int loadDistance, int simulationDistance, WorldRenderer worldRenderer, boolean debugWorld, long seed, int seaLevel, CallbackInfo ci) {
        this.timeProperty = new InterpolatedTickProperty("time", this.clientWorldProperties::setTime, this.clientWorldProperties::getTime, tickManager);
        this.dayTimeProperty = new InterpolatedTickProperty("timeOfDay", this.clientWorldProperties::setTimeOfDay, this.clientWorldProperties::getTimeOfDay, tickManager);
    }

    @Inject(at=@At("HEAD"), method = "tickTime", cancellable = true)
    public void tickTime(CallbackInfo ci) {
        timeProperty.tick();
        if (tickTimeOfDay) dayTimeProperty.tick();
        ci.cancel();
    }

    @Inject(at=@At("HEAD"), method = "setTime", cancellable = true, order = 10000)
    public void setTime(long time, long timeOfDay, boolean tickTimeOfDay, CallbackInfo ci) {
        this.tickTimeOfDay = tickTimeOfDay;
        timeProperty.update(time);
        dayTimeProperty.update(timeOfDay);
        ci.cancel();
    }
}
