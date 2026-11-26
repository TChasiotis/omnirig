package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.services.GeminiPromptBuilder;
import com.omnirig.utils.MiniJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiSpecService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final Pattern VERSION_PATTERN = Pattern.compile("gemini-(\\d+)(?:\\.(\\d+))?");

    private volatile List<String> flashModels;

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public CompletableFuture<Map<String, Object>> fetchSpecsAsync(String productName, Category category) {
        return CompletableFuture.supplyAsync(() -> fetchSpecs(productName, category));
    }

    private synchronized List<String> resolveFlashModels(String apiKey) {
        if (flashModels != null && !flashModels.isEmpty()) return flashModels;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "?key=" + apiKey))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) {
                throw new RuntimeException("Αποτυχία ανάκτησης μοντέλων (HTTP " + res.statusCode() + "): " + res.body());
            }

            Map<String, Object> root = MiniJson.parseObject(res.body());
            List<?> models = (List<?>) root.get("models");
            if (models == null || models.isEmpty()) {
                throw new RuntimeException("Δεν επιστράφηκαν διαθέσιμα μοντέλα από το Gemini API.");
            }

            List<String> candidates = new ArrayList<>();
            for (Object obj : models) {
                if (!(obj instanceof Map<?, ?> m)) continue;

                Object methodsObj = m.get("supportedGenerationMethods");
                if (!(methodsObj instanceof List<?> methods) || !methods.contains("generateContent")) continue;

                String rawName = String.valueOf(m.get("name"));
                String name = rawName.startsWith("models/") ? rawName.substring("models/".length()) : rawName;
                String lower = name.toLowerCase();

                if (!lower.contains("gemini") || !lower.contains("flash")) continue;
                if (lower.contains("audio") || lower.contains("tts") || lower.contains("image")
                        || lower.contains("live") || lower.contains("embedding") || lower.contains("vision")) {
                    continue;
                }
                candidates.add(name);
            }

            if (candidates.isEmpty()) {
                throw new RuntimeException("Δεν βρέθηκε διαθέσιμο Gemini Flash μοντέλο.");
            }

            candidates.sort(
                Comparator.comparingInt(GeminiSpecService::extractVersionScore).reversed()
                    .thenComparing(n -> n.toLowerCase().contains("lite"))
                    .thenComparing(n -> n.toLowerCase().contains("preview") || n.toLowerCase().contains("exp"))
                    .thenComparingInt(String::length)
            );

            flashModels = List.copyOf(candidates);
            System.out.println("[GeminiSpecService] Διαθέσιμα Flash μοντέλα: " + flashModels);
            return flashModels;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Αποτυχία εύρεσης ενεργών Flash μοντέλων: " + e.getMessage(), e);
        }
    }

    private static int extractVersionScore(String modelName) {
        Matcher m = VERSION_PATTERN.matcher(modelName.toLowerCase());
        if (!m.find()) return 0;
        int major = Integer.parseInt(m.group(1));
        int minor = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        return major * 1000 + minor;
    }

    private Map<String, Object> fetchSpecs(String productName, Category category) {
        String apiKey = loadApiKey();
        List<String> models = resolveFlashModels(apiKey);
        String prompt = GeminiPromptBuilder.build(productName, category);

        String payload = """
            {
              "contents": [{
                "parts": [{"text": "%s"}]
              }],
              "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.2
              }
            }
            """.formatted(escapeJson(prompt));

        String lastError = "Άγνωστο σφάλμα.";

        for (String model : models) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + model + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = res.statusCode();

                if (code == 200) {
                    System.out.println("[GeminiSpecService] Επιτυχής απάντηση από: " + model);
                    Map<String, Object> root = MiniJson.parseObject(res.body());
                    List<?> candidates = (List<?>) root.get("candidates");
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                    String rawJson = String.valueOf(firstPart.get("text"));

                    return MiniJson.parseObject(rawJson);
                }

                if (code == 503 || code == 429 || code == 500 || code == 504 || code == 404) {
                    System.out.println("[GeminiSpecService] Το " + model + " επέστρεψε HTTP " + code + " — fallback στο επόμενο...");
                    lastError = "HTTP " + code + " (" + model + ")";
                    continue;
                }

                throw new RuntimeException("Gemini HTTP " + code + ": " + res.body());

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("[GeminiSpecService] Σφάλμα στο " + model + " (" + e.getMessage() + ") — fallback στο επόμενο...");
                lastError = e.getMessage();
            }
        }

        throw new RuntimeException("Όλα τα διαθέσιμα Gemini Flash μοντέλα είναι προσωρινά υπερφορτωμένα (" + lastError + "). Δοκίμασε ξανά σε λίγα δευτερόλεπτα.");
    }

    private String loadApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();

        for (Path p : List.of(Path.of(".env"), Path.of(System.getProperty("user.dir"), ".env"))) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("GEMINI_API_KEY=")) {
                            String val = trimmed.substring("GEMINI_API_KEY=".length()).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) return val.trim();
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        throw new IllegalStateException("Δεν βρέθηκε το GEMINI_API_KEY στο αρχείο .env.");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}