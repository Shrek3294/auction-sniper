package com.example.auctionsniper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NtfyService {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendNotification(String message) {
        String topic = SniperConfig.ntfyTopic;
        if (topic == null || topic.isBlank())
            return;
        if (topic.contains(" ")) {
            DebugLog.log("ntfy-skip reason='topic has spaces' topic='" + topic + "'");
            return;
        }

        Thread sender = new Thread(() -> {
            try {
                String url = "https://ntfy.sh/" + topic;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(message))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                DebugLog.log("ntfy-send status=" + response.statusCode() + " topic='" + topic + "'");
            } catch (Exception e) {
                DebugLog.log("ntfy-send error='" + e.getClass().getSimpleName() + " " + e.getMessage() + "'");
                if (isConnectException(e)) {
                    sendWithHttpUrlConnection(topic, message);
                }
            }
        });
        sender.setName("AuctionSniperNtfySender");
        sender.setDaemon(true);
        sender.start();
    }

    private static boolean isConnectException(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof java.net.ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void sendWithHttpUrlConnection(String topic, String message) {
        try {
            URL url = new URL("https://ntfy.sh/" + topic);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            byte[] body = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            conn.getOutputStream().write(body);
            int status = conn.getResponseCode();
            DebugLog.log("ntfy-send-fallback status=" + status + " topic='" + topic + "'");
        } catch (Exception ex) {
            DebugLog.log("ntfy-send-fallback error='" + ex.getClass().getSimpleName() + " " + ex.getMessage() + "'");
        }
    }
}
