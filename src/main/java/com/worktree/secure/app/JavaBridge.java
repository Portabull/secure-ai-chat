package com.worktree.secure.app;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JavaBridge {

    private final WebView webView;
    private final JSObject jsWindow;

    private final ScreenshotSession screenshotSession = new ScreenshotSession();


    // ---------------- AUDIO ----------------
    private TargetDataLine mic;
    private ByteArrayOutputStream currentBuffer;   // ✅ REQUIRED
    private final AudioSession audioSession = new AudioSession();

    // ---------------- STREAM STATE ----------------
    private volatile boolean resizing = false;
    private final ConcurrentLinkedQueue<String> buffer =
            new ConcurrentLinkedQueue<>();

    public JavaBridge(WebView webView, JSObject jsWindow) {
        this.webView = webView;
        this.jsWindow = jsWindow;
    }

    /* -------------------------
       RESIZE HANDLING
     ------------------------- */
    public void setResizing(boolean value) {
        resizing = value;

        if (!value) {
            Platform.runLater(() -> {
                while (!buffer.isEmpty()) {
                    jsWindow.call("appendToken", buffer.poll());
                }
            });
        }
    }

    public void endResize() {
        setResizing(false);
    }

    /* -------------------------
       NORMAL TEXT MESSAGE
     ------------------------- */
//    public void sendMessage(String text, boolean withScreenshot) {
//
//        Platform.runLater(() ->
//                jsWindow.call("debug", "Java.sendMessage INVOKED")
//        );
//
//        new Thread(() -> {
//
//            String screenshot = withScreenshot
//                    ? ScreenshotUtil.captureBase64()
//                    : null;
//
//            OpenAIStreamer.stream(
//                    text,
//                    screenshot,
//                    token -> {
//                        if (resizing) {
//                            buffer.add(token);
//                        } else {
//                            Platform.runLater(() -> {
//                                try {
//                                    jsWindow.call("appendToken", token);
//                                } catch (Exception ignored) {}
//                            });
//                        }
//                    }
//            );
//
//        }, "openai-stream").start();
//    }

    /* -------------------------
       CLIPBOARD
     ------------------------- */
    public void copyToClipboard(String text) {
        Platform.runLater(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        });
    }

    /* -------------------------
       RECORDING
     ------------------------- */
    public void startRecording() {
        try {
            AudioFormat format = new AudioFormat(
                    16000f,
                    16,
                    1,
                    true,
                    false
            );

            mic = AudioSystem.getTargetDataLine(format);
            mic.open(format);
            mic.start();

            currentBuffer = new ByteArrayOutputStream();

            new Thread(() -> {
                byte[] buf = new byte[4096];
                while (mic.isOpen()) {
                    int n = mic.read(buf, 0, buf.length);
                    if (n > 0) {
                        currentBuffer.write(buf, 0, n);
                    }
                }
            }, "mic-recorder").start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        try {
            mic.stop();
            mic.close();

            byte[] pcm = currentBuffer.toByteArray();
            byte[] wav = pcmToWav(pcm);

//            // ✅ SAVE TO DESKTOP FOR DEBUG
//            Path desktop = Paths.get(
//                    System.getProperty("user.home"),
//                    "Desktop",
//                    "recording_" + System.currentTimeMillis() + ".wav"
//            );
//            Files.write(desktop, wav);
//
//            System.out.println("Saved recording to: " + desktop);

            audioSession.add(wav);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* -------------------------
       PCM → WAV (CRITICAL)
     ------------------------- */
    private byte[] pcmToWav(byte[] pcmData) throws Exception {

        AudioFormat format = new AudioFormat(
                16000f,
                16,
                1,
                true,
                false
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
        AudioInputStream ais =
                new AudioInputStream(
                        bais,
                        format,
                        pcmData.length / format.getFrameSize()
                );

        ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);

        return wavOut.toByteArray();
    }

    /* -------------------------
       SEND AUDIO + TEXT
     ------------------------- */
    public void submitPromptWithRecordings(String userText) {

        verify();

        boolean hasText = userText != null && !userText.trim().isEmpty();
        boolean hasAudio = !audioSession.isEmpty();
        boolean hasScreenshots = !screenshotSession.isEmpty();

        if (!hasText && !hasAudio && !hasScreenshots) {
            sendToken("\n❌ Please provide text, audio, or screenshot.");
            return;
        }

        try {

            // AUDIO (with optional text + screenshots)
            if (hasAudio) {
                byte[] mergedWav = audioSession.mergeWavFiles();

                OpenAIStreamer.streamWithAudioAndScreenshots(
                        hasText ? userText : "",
                        mergedWav,
                        screenshotSession.getAll(),
                        token -> sendToken(token)
                );
            }
            // TEXT / SCREENSHOT only
            else {
                OpenAIStreamer.streamWithScreenshots(
                        hasText ? userText : "",
                        screenshotSession.getAll(),
                        token -> sendToken(token)
                );
            }

            audioSession.clear();
            screenshotSession.clear();

        } catch (Exception e) {
            e.printStackTrace();
            sendToken("\n❌ Failed to process request");
        }
    }

    String licenseKey = null;

    private synchronized void verify() {
        try {
            if (licenseKey == null)
                licenseKey = ConfigManager.load().getString("licenseKey");
            LicenseStatus verify = RSALicenseVerifier.verify(licenseKey);
            if (!verify.valid) {
                AppControls.restartApplication();
            }
        } catch (Exception e) {
            e.printStackTrace();
            AppControls.restartApplication();
        }
    }

//    public void submitPromptWithRecordings(String userText) {
//
//        boolean hasText = userText != null && !userText.trim().isEmpty();
//        boolean hasAudio = !audioSession.isEmpty();
//
//        // ❌ Nothing provided
//        if (!hasText && !hasAudio) {
//            sendToken("\n❌ Please enter text or record audio.");
//            return;
//        }
//
//        try {
//
//            // 🎤 Audio + (optional) text
//            if (hasAudio) {
//                byte[] mergedWav = audioSession.mergeWavFiles();
//
//                OpenAIStreamer.streamWithAudio(
//                        hasText ? userText : "",
//                        mergedWav,
//                        token -> sendToken(token)
//                );
//            }
//            // ✍️ Text only
//            else {
//                OpenAIStreamer.stream(
//                        userText,
//                        null,
//                        token -> sendToken(token)
//                );
//            }
//
//            audioSession.clear();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            sendToken("\n❌ Failed to process request");
//        }
//    }


    /* -------------------------
       STREAM TOKEN TO UI
     ------------------------- */
    public void sendToken(String token) {
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript(
                        "appendToken(" + toJsString(token) + ")"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String toJsString(String s) {
        if (s == null) return "''";
        return "'" + s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "") +
                "'";
    }

    public void removeRecording(int index) {
        audioSession.remove(index);
        System.out.println("Removed recording at index: " + index);
    }

    public void takeScreenshot() {
        String base64 = ScreenshotUtil.captureBase64();
        screenshotSession.add(base64);
        System.out.println("Screenshot added");
    }

    public void removeScreenshot(int index) {
        screenshotSession.remove(index);
        System.out.println("Removed screenshot at index: " + index);
    }

    public void submitSetup(String openAiKey, String licenseKey) {
        Platform.runLater(() -> {
            try {
                if (!validateOpenAIKey(openAiKey)) {
                    jsWindow.call("showError", "Invalid OpenAI key");
                    return;
                }

                LicenseStatus verify = RSALicenseVerifier.verify(licenseKey);
                if (!verify.valid) {
                    jsWindow.call("showError", verify.message);
                    return;
                }

                // Optional: ping OpenAI to verify key
                // Optional: call license server

                ConfigManager.save(openAiKey, licenseKey);

                AppControls.restartApplication();

            } catch (Exception e) {
                jsWindow.call("showError", e.getMessage());
            }
        });
    }



    public static boolean validateOpenAIKey(String apiKey) {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL("https://api.openai.com/v1/models").openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            return code == 200;

        } catch (Exception e) {
            return false;
        }
    }

}
