package dev.hugeblank.asahi.client.mixin;

import dev.hugeblank.asahi.client.TimeSmoother;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {


    @Shadow public abstract ClientWorld getWorld();

    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(at=@At(value="INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTime(J)V"), method="onWorldTimeUpdate(Lnet/minecraft/network/packet/s2c/play/WorldTimeUpdateS2CPacket;)V", cancellable = true)
    private void clearTickable(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        ((TimeSmoother) getWorld()).asahi$updateTimes(packet);
        worldSession.setTick(packet.getTime());
        ci.cancel();
    }
}
