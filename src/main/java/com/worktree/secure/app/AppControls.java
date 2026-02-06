package com.worktree.secure.app;

import javafx.application.Platform;

import java.io.File;

public class AppControls {

    public static void restartApplication() {
        try {
            // Path to the BAT that launched the app
            String batPath = new File("start.bat").getAbsolutePath();

            new ProcessBuilder(
                    "cmd",
                    "/c",
                    "start",
                    "\"\"",
                    batPath
            ).start();

            // Shutdown current JVM
            Platform.exit();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
