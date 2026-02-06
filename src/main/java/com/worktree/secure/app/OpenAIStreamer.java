


package com.worktree.secure.app;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class OpenAIStreamer {

    // ================= CONFIG =================
    private static String API_KEY;


    private static final String CHAT_URL =
            "https://api.openai.com/v1/chat/completions";

    private static final String TRANSCRIBE_URL =
            "https://api.openai.com/v1/audio/transcriptions";

    // ================= PUBLIC APIS =================

    /* -------- TEXT ONLY -------- */
//    public static void streamText(
//            String text,
//            Consumer<String> tokenConsumer
//    ) throws Exception {
//
//        String prompt = text == null ? "" : text;
//        streamChat(prompt, null);
//
//        streamChat(prompt, tokenConsumer);
//    }

    /* -------- TEXT + SCREENSHOTS -------- */
    public static void streamWithScreenshots(
            String text,
            List<String> screenshots,
            Consumer<String> tokenConsumer
    ) throws Exception {

        String prompt = text == null ? "" : text;
        streamChat(prompt, screenshots, tokenConsumer);
    }

    /* -------- AUDIO + (TEXT) + SCREENSHOTS -------- */
    public static void streamWithAudioAndScreenshots(
            String userText,
            byte[] wavAudio,
            List<String> screenshots,
            Consumer<String> tokenConsumer
    ) throws Exception {

        loadAPIKey();

        new Thread(() -> {
            try {
                String transcription = transcribeAudio(API_KEY, wavAudio);

                StringBuilder prompt = new StringBuilder();
                prompt.append("User spoke:\n").append(transcription);

                if (userText != null && !userText.trim().isEmpty()) {
                    prompt.append("\n\nAdditional text:\n").append(userText);
                }

                streamChat(prompt.toString(), screenshots, tokenConsumer);

            } catch (Exception e) {
                tokenConsumer.accept("\n❌ Error: " + e.getMessage());
            }
        }).start();
    }

    private synchronized static void loadAPIKey() {
        try {
            if (API_KEY == null) {
                API_KEY = ConfigManager.load().getString("openaiKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CHAT STREAMING =================

    private static void streamChat(
            String prompt,
            List<String> screenshots,
            Consumer<String> tokenConsumer
    ) throws Exception {

        loadAPIKey();

        HttpURLConnection conn =
                (HttpURLConnection) new URL(CHAT_URL).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");

        String body = buildChatRequest(prompt, screenshots);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        InputStream is = conn.getResponseCode() >= 400
                ? conn.getErrorStream()
                : conn.getInputStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("data: ")) continue;
            if (line.contains("[DONE]")) break;

            String token = extractToken(line);
            if (token != null) {
                tokenConsumer.accept(token);
            }
        }
    }

    // ================= REQUEST BUILDERS =================

    private static String buildChatRequest(
            String prompt,
            List<String> screenshots
    ) {

        StringBuilder content = new StringBuilder();
        content.append("{\"type\":\"text\",\"text\":\"")
                .append(jsonEscape(prompt))
                .append("\"}");

        if (screenshots != null) {
            for (String img : screenshots) {
                content.append(",{\"type\":\"image_url\",\"image_url\":{")
                        .append("\"url\":\"data:image/png;base64,")
                        .append(img)
                        .append("\"}}");
            }
        }

        return "{"
                + "\"model\":\"gpt-4o-mini\","
                + "\"stream\":true,"
                + "\"messages\":["
                + "{\"role\":\"user\",\"content\":["
                + content
                + "]}"
                + "]"
                + "}";
    }

    // ================= TRANSCRIPTION =================

    private static String transcribeAudio(
            String apiKey,
            byte[] wavAudio
    ) throws Exception {

        String boundary = "----Boundary" + System.currentTimeMillis();

        HttpURLConnection conn =
                (HttpURLConnection) new URL(TRANSCRIBE_URL).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=" + boundary
        );

        try (OutputStream os = conn.getOutputStream()) {

            writeFormField(os, boundary, "model", "whisper-1");

            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n"
                            .getBytes()
            );
            os.write("Content-Type: audio/wav\r\n\r\n".getBytes());
            os.write(wavAudio);
            os.write("\r\n".getBytes());
            os.write(("--" + boundary + "--\r\n").getBytes());
        }

        InputStream is = conn.getResponseCode() >= 400
                ? conn.getErrorStream()
                : conn.getInputStream();

        String response = readAll(is);
        System.out.println("Whisper response:\n" + response);

        int idx = response.indexOf("\"text\":\"");
        if (idx == -1) return "";

        String text = response.substring(idx + 8);
        return text.substring(0, text.indexOf("\""))
                .replace("\\n", " ")
                .replace("\\\"", "\"");
    }

    // ================= HELPERS =================

    private static void writeFormField(
            OutputStream os,
            String boundary,
            String name,
            String value
    ) throws IOException {

        os.write(("--" + boundary + "\r\n").getBytes());
        os.write(
                ("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                        .getBytes()
        );
        os.write((value + "\r\n").getBytes());
    }

    private static String extractToken(String line) {
        int idx = line.indexOf("\"content\":\"");
        if (idx == -1) return null;

        String part = line.substring(idx + 11);
        int end = part.indexOf("\"");
        if (end == -1) return null;

        return part.substring(0, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toString();
    }
}
