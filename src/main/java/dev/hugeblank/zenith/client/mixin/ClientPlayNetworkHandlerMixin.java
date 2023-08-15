package dev.hugeblank.zenith.client.mixin;

import dev.hugeblank.zenith.client.TimeSmoother;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.telemetry.WorldSession;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Final
    @Shadow
    private WorldSession field_34963;

    @Inject(at=@At(value="INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTime(J)V"), method="onWorldTimeUpdate(Lnet/minecraft/network/packet/s2c/play/WorldTimeUpdateS2CPacket;)V", cancellable = true)
    private void clearTickable(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        ((TimeSmoother) world).zenith$updateTimes(packet);
        field_34963.setTick(packet.getTime());
        ci.cancel();
    }
}
