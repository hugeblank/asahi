package dev.hugeblank.asahi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.hugeblank.asahi.client.InterpolatedTickProperty;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin{

    @Shadow @Final private ClientLevel.ClientLevelData clientLevelData;

    @Shadow @Final private TickRateManager tickRateManager;

    @Unique private InterpolatedTickProperty timeProperty;

    @Inject(at=@At("TAIL"), method = "<init>")
    private void init(
            ClientPacketListener connection,
            ClientLevel.ClientLevelData levelData,
            ResourceKey<Level> dimension,
            Holder<DimensionType> dimensionType,
            int serverChunkRadius,
            int serverSimulationDistance,
            LevelRenderer levelRenderer,
            boolean isDebug,
            long biomeZoomSeed,
            int seaLevel,
            CallbackInfo ci
    ) {
        this.timeProperty = new InterpolatedTickProperty(
                "gameTime",
                this.clientLevelData::setGameTime,
                this.clientLevelData::getGameTime,
                tickRateManager
        );
    }

    @WrapMethod(method = "tickTime")
    public void tickTime(Operation<Void> original) {
        timeProperty.tick();
    }

    @WrapMethod(method = "setTimeFromServer")
    public void setTimeFromServer(long gameTime, Operation<Void> original) {
        timeProperty.update(gameTime);
    }
}
