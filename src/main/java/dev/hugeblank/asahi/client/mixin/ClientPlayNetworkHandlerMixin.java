package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.TimeSmoother;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    
    @Shadow private ClientWorld world;

    @Inject(at=@At(value="INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTime(J)V"), method="onWorldTimeUpdate(Lnet/minecraft/client/network/packet/WorldTimeUpdateS2CPacket;)V", cancellable = true)
    private void clearTickable(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        ((TimeSmoother) world).asahi$updateTimes(packet);
        this.world.setTime(packet.getTime());
        ci.cancel();
    }
}
