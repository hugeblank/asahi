package dev.hugeblank.asahi.client;

import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;

public interface TimeSmoother {
    void asahi$updateTimes(WorldTimeUpdateS2CPacket packet);
}
