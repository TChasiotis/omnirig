package com.omnirig.services;

import com.omnirig.models.Category;

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
        String prompt = buildPrompt(productName, category);

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

    private String buildPrompt(String productName, Category category) {
        String schema = switch (category) {
            case CPU -> """
                {
                  "socket": "<string, actual CPU socket e.g. AM5, LGA1851>",
                  "cores": <int>,
                  "threads": <int>,
                  "base_clock_ghz": <double>,
                  "boost_clock_ghz": <double>,
                  "cache_l3_mb": <int>,
                  "tdp_base_w": <int>,
                  "tdp_max_w": <int>,
                  "memory_support": ["<string, e.g. DDR5>"],
                  "pcie_version": "<string, e.g. 5.0>",
                  "has_integrated_graphics": <boolean>
                }""";

            case MOTHERBOARD -> """
                {
                  "socket": "<string>",
                  "chipset": "<string>",
                  "form_factor": "<string, e.g. ATX, Micro-ATX, Mini-ITX, E-ATX>",
                  "length_mm": <int>,
                  "width_mm": <int>,
                  "memory_type": "<string, e.g. DDR5>",
                  "memory_slots": <int>,
                  "max_memory_gb": <int>,
                  "max_memory_speed_mhz": <int>,
                  "pcie_x16_version": "<string, e.g. 5.0>",
                  "pcie_x16_slots": <int>,
                  "pcie_x1_slots": <int>,
                  "m2_slots": <int>,
                  "m2_slots_gen": ["<string, e.g. 5.0, 4.0>"],
                  "sata_ports": <int>,
                  "lan_speed": "<string, e.g. 2.5Gbps>",
                  "usb_ports_rear": <int>,
                  "usb_c_rear": <int>,
                  "usb_c_header": <boolean>,
                  "audio_codec": "<string>",
                  "vrm_phases": "<string, e.g. 14+2+1>",
                  "rgb_headers_12v": <int>,
                  "argb_headers_5v": <int>,
                  "fan_headers": <int>,
                  "bios_flashback": <boolean>,
                  "color": "<string>"
                }""";

            case RAM -> """
                {
                  "type": "<string, e.g. DDR5>",
                  "capacity_gb": <int, total kit capacity>,
                  "modules": <int>,
                  "module_capacity_gb": <int>,
                  "speed_mhz": <int>,
                  "cas_latency": <int>,
                  "timings": "<string, e.g. 30-38-38-96>",
                  "voltage_v": <double>,
                  "height_mm": <int>,
                  "xmp_support": <boolean>,
                  "expo_support": <boolean>,
                  "rgb": <boolean>,
                  "color": "<string>"
                }""";

            case GPU -> """
                {
                  "chipset_brand": "<string, e.g. NVIDIA, AMD, Intel>",
                  "chipset": "<string>",
                  "vram_gb": <int>,
                  "vram_type": "<string, e.g. GDDR7, GDDR6X, GDDR6>",
                  "memory_bus_bit": <int>,
                  "boost_clock_mhz": <int>,
                  "pcie_interface": "<string, e.g. PCIe 5.0 x16>",
                  "pcie_version": "<string, e.g. 5.0>",
                  "length_mm": <int>,
                  "width_mm": <int>,
                  "thickness_mm": <int>,
                  "slots_thickness": <double>,
                  "tdp_w": <int>,
                  "recommended_psu_w": <int>,
                  "power_connectors": "<string, e.g. 1x 16-pin>",
                  "cooling_type": "<string, e.g. Triple Fan>",
                  "hdmi_ports": <int>,
                  "displayport_ports": <int>,
                  "max_displays": <int>,
                  "ray_tracing": <boolean>,
                  "upscaling_tech": "<string, e.g. DLSS 4, FSR 4, XeSS>",
                  "color": "<string>"
                }""";

            case COOLER -> """
                {
                  "type": "<string, e.g. Air or AIO>",
                  "supported_sockets": ["<string, all supported sockets>"],
                  "height_mm": <int>,
                  "max_tdp_w": <int>,
                  "fan_count": <int>,
                  "fan_size_mm": <int>,
                  "max_noise_db": <double>,
                  "rgb": <boolean>,
                  "color": "<string>"
                }""";

            case CASE -> """
                {
                  "case_type": "<string, e.g. Mid-Tower, Full-Tower, Dual-Chamber Mid-Tower>",
                  "supported_mobo_form_factors": ["<string, e.g. ATX, Micro-ATX, Mini-ITX>"],
                  "max_gpu_length_mm": <int>,
                  "max_cpu_cooler_height_mm": <int>,
                  "max_psu_length_mm": <int>,
                  "max_radiator_top_mm": <int>,
                  "max_radiator_front_mm": <int>,
                  "max_radiator_rear_mm": <int>,
                  "included_fans": <int>,
                  "max_fans": <int>,
                  "expansion_slots": <int>,
                  "drive_bays_2_5": <int>,
                  "drive_bays_3_5": <int>,
                  "length_mm": <int>,
                  "width_mm": <int>,
                  "height_mm": <int>,
                  "side_panel": "<string, e.g. Tempered Glass>",
                  "usb_c_front": <boolean>,
                  "color": "<string>"
                }""";

            case PSU -> """
                {
                  "wattage": <int>,
                  "efficiency_rating": "<string, e.g. 80+ Gold>",
                  "modularity": "<string, e.g. Full-Modular, Semi-Modular, Non-Modular>",
                  "form_factor": "<string, e.g. ATX, SFX>",
                  "length_mm": <int>,
                  "atx_version": "<string, e.g. ATX 3.1>",
                  "pcie_5_12vhpwr": <boolean>,
                  "pcie_8pin_connectors": <int>,
                  "sata_connectors": <int>,
                  "fan_size_mm": <int>,
                  "zero_rpm_mode": <boolean>,
                  "warranty_years": <int>,
                  "color": "<string>"
                }""";

            case STORAGE -> """
                {
                  "capacity_gb": <int>,
                  "form_factor": "<string, e.g. M.2 2280, 2.5\\">",
                  "interface": "<string, e.g. NVMe PCIe 4.0 x4, SATA III>",
                  "read_speed_mb_s": <int>,
                  "write_speed_mb_s": <int>,
                  "nand_type": "<string, e.g. TLC, QLC>",
                  "dram_cache": <boolean>,
                  "heatsink": <boolean>,
                  "tbw": <int>,
                  "warranty_years": <int>
                }""";
        };

        return "You are a PC hardware database assistant. "
            + "Determine the exact real-world technical specifications for the product: \"" + productName
            + "\" in category " + category.name() + ". "
            + "Use the true specifications of this exact product (do NOT restrict values to the examples in the schema). "
            + "If the product does not exist in reality, invent realistic, consistent technical specifications based on its name and category. "
            + "Every single field in the schema MUST be populated with a valid non-null value of the exact indicated type. "
            + "Return ONLY a valid JSON object matching these exact keys (no markdown, no explanation, no extra keys): "
            + schema;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class MiniJson {
        private final String src;
        private int pos = 0;

        private MiniJson(String src) {
            this.src = src;
        }

        static Map<String, Object> parseObject(String json) {
            MiniJson p = new MiniJson(json);
            Object res = p.readValue();
            if (res instanceof Map<?, ?> m) {
                Map<String, Object> out = new LinkedHashMap<>();
                m.forEach((k, v) -> out.put(String.valueOf(k), v));
                return out;
            }
            throw new IllegalArgumentException("Μη έγκυρο JSON object.");
        }

        private Object readValue() {
            skipWs();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return readObject();
            if (c == '[') return readArray();
            if (c == '"') return readString();
            if (c == 't' && src.startsWith("true", pos))  { pos += 4; return Boolean.TRUE; }
            if (c == 'f' && src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (c == 'n' && src.startsWith("null", pos))  { pos += 4; return null; }
            return readNumber();
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWs();
            if (pos < src.length() && src.charAt(pos) == '}') { pos++; return map; }
            while (pos < src.length()) {
                skipWs();
                String key = readString();
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                Object val = readValue();
                map.put(key, val);
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
                if (pos < src.length() && src.charAt(pos) == '}') { pos++; break; }
            }
            return map;
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ']') { pos++; return list; }
            while (pos < src.length()) {
                list.add(readValue());
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
                if (pos < src.length() && src.charAt(pos) == ']') { pos++; break; }
            }
            return list;
        }

        private String readString() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 <= src.length()) {
                                sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                                pos += 4;
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number readNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            String num = src.substring(start, pos);
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            long l = Long.parseLong(num);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }
}