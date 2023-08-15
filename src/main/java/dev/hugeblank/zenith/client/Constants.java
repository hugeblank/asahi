package dev.hugeblank.zenith.client;

//TODO: Make configurable
public class Constants {
    public static final int TPS = 20;
    // Duration threshold where smoothing is given up in favor of skipping to sync back up with the server.
    // Default value is 60 seconds, by which point a server is supposed to crash.
    // If the daylight cycle is regularly modified with /time, you may want to reduce this.
    public static final int SKIP_DURATION = 60*TPS; // 1200 ticks
    // Smallest per-second factor the daylight cycle is allowed to progress.
    // Default value is 1 "daylight cycle tick" per second
    // Setting this to 0 will freeze the sun until the server is caught up.
    public static final double MIN_MOVE_FACTOR = 1D/TPS;
}
