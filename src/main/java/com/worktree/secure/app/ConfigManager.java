package com.worktree.secure.app;

import java.io.*;
import java.nio.file.*;
import org.json.JSONObject;

public class ConfigManager {

    private static final Path CONFIG_DIR =
            Paths.get(System.getenv("APPDATA"), "SecureChat");

    private static final Path CONFIG_FILE =
            CONFIG_DIR.resolve("config.json");

    public static boolean exists() {
        return Files.exists(CONFIG_FILE);
    }

    public static void save(String openAiKey, String licenseKey) throws Exception {
        Files.createDirectories(CONFIG_DIR);

        JSONObject obj = new JSONObject();
        obj.put("openaiKey", openAiKey);
        obj.put("licenseKey", licenseKey);
        obj.put("activatedAt", java.time.LocalDate.now().toString());

        Files.writeString(CONFIG_FILE, obj.toString(2));
    }

    public static JSONObject load() throws Exception {
        return new JSONObject(Files.readString(CONFIG_FILE));
    }
}
