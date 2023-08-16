package dev.hugeblank.asahi.client;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public interface TimeSmoother {
    void zenith$updateTimes(WorldTimeUpdateS2CPacket packet);
}
