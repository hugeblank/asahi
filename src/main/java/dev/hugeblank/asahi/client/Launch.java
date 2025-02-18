package dev.hugeblank.asahi.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Launch implements ClientModInitializer {

    public static Config CONFIG = new Config(60, 10, 1.0, 20);
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("asahi.json");

    @Override
    public void onInitializeClient() {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            CONFIG = gson.fromJson(Files.readString(PATH), Config.class);
        } catch (IOException e) {
            String json = gson.toJson(CONFIG);
            try {
                Files.writeString(PATH, json);
            } catch (IOException ex) {
                throw new CrashException(CrashReport.create(ex, "Failed to initialize asahi config."));
            }
        }
    }
}
