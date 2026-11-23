
package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.models.Product;

import java.util.*;
import java.util.regex.*;

import static com.omnirig.services.CompatibilityEngine.*;

public class BuildAnalyzer {

    public enum Resolution {
        FHD_1080P("1080p"),
        QHD_1440P("1440p"),
        UHD_4K("4K");

        private final String label;

        Resolution(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record FpsSet(int fortnite, int cyberpunk, int rdr2) {}

    public record Report(
        double performanceScore,
        double valueScore,
        double balanceScore,
        boolean supports4K,
        FpsSet fps1080p,
        FpsSet fps1440p,
        FpsSet fps4k,
        String bottleneckVerdict,
        List<String> technicianNotes
    ) {
        public FpsSet getFpsFor(Resolution res) {
            return switch (res) {
                case FHD_1080P -> fps1080p;
                case QHD_1440P -> fps1440p;
                case UHD_4K -> fps4k != null ? fps4k : fps1440p;
            };
        }
    }

    // =====================================================
    // 1. GPU PERFORMANCE ESTIMATION
    // =====================================================

    /*
     * Relative GPU performance index.
     *
     * RTX 3060 = approximately 1.00
     *
     * The index is inferred from the GPU family, generation
     * and performance tier. It is NOT a product database.
     */

    private static double gpuIndex(Product gpu) {

        if (gpu == null) return 0.40;

        String chipset = getString(gpu, "chipset");

        if (chipset.isBlank()) {
            chipset = gpu.getName();
        }

        String name = chipset.toUpperCase(Locale.ROOT);

        // NVIDIA RTX / GTX
        Matcher nv = Pattern.compile(
            "(?:RTX|GTX)\\s*(\\d{4})"
        ).matcher(name);

        if (nv.find()) {

            int model = Integer.parseInt(nv.group(1));
            int generation = model / 1000;
            int tier = model % 100;

            double base;

            if (tier >= 90) base = 2.65;
            else if (tier >= 80) base = 2.00;
            else if (tier >= 70) base = 1.45;
            else if (tier >= 60) base = 1.00;
            else if (tier >= 50) base = 0.58;
            else if (tier >= 40) base = 0.40;
            else base = 0.30;

            double generationFactor = switch (generation) {
                case 2 -> 0.92;
                case 3 -> 1.00;
                case 4 -> 1.07;
                case 5 -> 1.16;
                default -> 0.90;
            };

            double suffix = 1.0;

            if (name.contains("TI SUPER")) suffix = 1.20;
            else if (name.contains("TI")) suffix = 1.12;
            else if (name.contains("SUPER")) suffix = 1.07;

            // Older GTX models have a different performance class.
            if (name.contains("GTX") && tier < 70) {
                base *= 0.80;
            }

            return base * generationFactor * suffix;
        }

        // AMD Radeon RX
        Matcher amd = Pattern.compile(
            "RX\\s*(\\d{4})"
        ).matcher(name);

        if (amd.find()) {

            int model = Integer.parseInt(amd.group(1));
            int generation = model / 1000;
            int rem = model % 1000;
            int tier = rem < 100 ? rem : rem / 10;

            double base;

            if (tier >= 90) base = 2.00;
            else if (tier >= 80) base = 1.60;
            else if (tier >= 70) base = 1.22;
            else if (tier >= 60) base = 0.78;
            else if (tier >= 50) base = 0.48;
            else base = 0.30;

            double generationFactor = switch (generation) {
                case 5 -> 0.90;
                case 6 -> 0.90;
                case 7 -> 1.00;
                case 9 -> 1.12;
                default -> 0.85;
            };

            double suffix = 1.0;

            if (name.contains("XTX")) suffix = 1.20;
            else if (name.contains("XT")) suffix = 1.10;
            else if (name.contains("GRE")) suffix = 1.05;

            return base * generationFactor * suffix;
        }

        // Intel Arc
        Matcher intel = Pattern.compile(
            "ARC\\s+([AB])(\\d{3})"
        ).matcher(name);

        if (intel.find()) {

            String generation = intel.group(1);
            int tier = Integer.parseInt(intel.group(2));

            double base;

            if (tier >= 770) base = 0.90;
            else if (tier >= 750) base = 0.80;
            else if (tier >= 580) base = 0.85;
            else if (tier >= 570) base = 0.72;
            else if (tier >= 380) base = 0.38;
            else base = 0.30;

            return base * (generation.equals("B") ? 1.10 : 1.0);
        }

        /*
         * Unknown GPU:
         * Use its available hardware specifications.
         *
         * Lower confidence than chipset recognition.
         */

        int vram = getInt(gpu, "vram_gb", 8);
        int bus = getInt(gpu, "memory_bus_bit", 128);
        int tdp = getInt(gpu, "tdp_w", 150);
        int clock = getInt(gpu, "boost_clock_mhz", 1500);

        return clamp(
            0.30
            + Math.min(vram, 24) * 0.018
            + Math.min(bus, 384) / 384.0 * 0.25
            + Math.min(tdp, 450) / 450.0 * 0.30
            + Math.max(0, clock - 1200) / 3000.0,
            0.30,
            1.50
        );
    }

    // =====================================================
    // 2. CPU PERFORMANCE
    // =====================================================

    /*
     * Estimates gaming CPU capability using:
     * - Boost frequency
     * - Core count
     * - Thread count
     * - L3 cache
     * - CPU generation
     *
     * This is a heuristic, not a measured CPU score.
     */

    private static double cpuIndex(Product cpu) {

        if (cpu == null) return 4.0;

        int cores = getInt(cpu, "cores", 4);
        int threads = getInt(cpu, "threads", cores * 2);
        int cache = getInt(cpu, "cache_l3_mb", 8);

        double boost = getDouble(cpu, "boost_clock_ghz", 3.5);

        String name = cpu.getName().toUpperCase(Locale.ROOT);

        double generationBonus = 0;

        // Ryzen generation inference
        Matcher ryzen = Pattern.compile(
            "RYZEN\\s+[3579]\\s+(\\d{4})"
        ).matcher(name);

        if (ryzen.find()) {

            int generation = Integer.parseInt(
                ryzen.group(1).substring(0, 1)
            );

            generationBonus = switch (generation) {
                case 1, 2, 3 -> 0.0;
                case 4 -> 0.15;
                case 5 -> 0.35;
                case 6 -> 0.45;
                case 7 -> 0.60;
                case 9 -> 0.80;
                default -> 0.0;
            };
        }

        // Intel Core generation inference
        Matcher intel = Pattern.compile(
            "I[3579][- ]?(\\d{4,5})"
        ).matcher(name);

        if (intel.find()) {

            String number = intel.group(1);

            int generation = number.length() == 5
                ? Integer.parseInt(number.substring(0, 2))
                : Integer.parseInt(number.substring(0, 1));

            generationBonus = Math.max(
                0,
                Math.min(1.0, (generation - 8) * 0.09)
            );
        }

        // Gaming cache bonus for X3D processors
        double cacheBonus = name.contains("X3D") ? 1.0 : 0.0;

        double score =
            3.0
            + (boost - 4.0) * 0.90
            + Math.min(cores, 16) * 0.14
            + Math.min(threads, 32) * 0.055
            + Math.min(cache, 128) * 0.012
            + generationBonus
            + cacheBonus;

        return clamp(score, 2.0, 10.0);
    }

    // =====================================================
    // 3. RAM PERFORMANCE
    // =====================================================

    private static double ramFactor(Product ram) {

        if (ram == null) return 0.85;

        int capacity = getInt(ram, "capacity_gb", 16);
        int modules = getInt(ram, "modules", 1);
        int speed = getInt(ram, "speed_mhz", 3200);

        String type = getString(ram, "type").toUpperCase();

        double factor = 1.0;

        // Capacity
        if (capacity <= 8) factor *= 0.82;
        else if (capacity < 16) factor *= 0.92;
        else if (capacity >= 32) factor *= 1.01;

        // Dual channel
        if (modules >= 2) factor *= 1.035;
        else factor *= 0.96;

        // Memory frequency relative to its generation
        double referenceSpeed = type.contains("DDR5") ? 5600 : 3200;

        double speedFactor = Math.pow(
            Math.max(0.5, speed / referenceSpeed),
            0.12
        );

        factor *= clamp(speedFactor, 0.94, 1.06);

        return clamp(factor, 0.65, 1.10);
    }

    // =====================================================
    // 4. MOTHERBOARD AND COOLING
    // =====================================================

    private static double motherboardFactor(
        Product gpu,
        Product motherboard
    ) {

        if (gpu == null || motherboard == null) return 1.0;

        String gpuPcie = getString(gpu, "pcie_version");
        String boardPcie = getString(motherboard, "pcie_x16_version");

        if (gpuPcie.isBlank() || boardPcie.isBlank()) return 1.0;

        double gpuGen = parsePcie(gpuPcie);
        double boardGen = parsePcie(boardPcie);

        if (gpuGen <= 0 || boardGen <= 0) return 1.0;

        if (boardGen < gpuGen) {
            return 0.97;
        }

        return 1.0;
    }

    private static double coolerFactor(
        Product cpu,
        Product cooler
    ) {

        if (cpu == null || cooler == null) return 1.0;

        int cpuTdp = getInt(cpu, "tdp_max_w", 65);
        int coolerTdp = getInt(cooler, "max_tdp_w", 150);

        if (cpuTdp <= 0 || coolerTdp <= 0) return 1.0;

        double ratio = coolerTdp / (double) cpuTdp;

        if (ratio < 0.65) return 0.90;
        if (ratio < 0.90) return 0.96;

        return 1.0;
    }

    private static double parsePcie(String version) {

        Matcher matcher = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)"
        ).matcher(version);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return 0;
    }

    // =====================================================
    // 5. CPU BOTTLENECK
    // =====================================================

    /*
     * CPU influence changes according to resolution.
     * At 1080p CPU limitations matter more.
     */

    private static double cpuGamingFactor(
        double cpuScore,
        double influence
    ) {

        double difference = cpuScore - 5.5;

        return clamp(
            1.0 + difference * 0.065 * influence,
            0.70,
            1.12
        );
    }

    // =====================================================
    // 6. FPS ESTIMATION
    // =====================================================

    /*
     * Reference model:
     *
     * RTX 3060 class GPU
     * Mid-range CPU
     * 16GB dual-channel RAM
     *
     * High preset, native resolution,
     * Ray Tracing OFF, upscaling OFF.
     *
     * These are approximate model anchors.
     */

    private static FpsSet calculateFps(
        double gpu,
        double cpu,
        double ramProduct,
        double motherboard,
        double cooler,
        double resolutionScale,
        double cpuInfluence,
        double vramFactor
    ) {

        double gpuScaling = Math.pow(gpu, 0.82);

        double systemFactor =
            ramProduct * motherboard * cooler * vramFactor;

        double fortnite = 110
            * gpuScaling
            * resolutionScale
            * systemFactor
            * cpuGamingFactor(cpu, cpuInfluence * 1.45);

        double cyberpunk = 55
            * gpuScaling
            * resolutionScale
            * systemFactor
            * cpuGamingFactor(cpu, cpuInfluence * 0.85);

        double rdr2 = 70
            * gpuScaling
            * resolutionScale
            * systemFactor
            * cpuGamingFactor(cpu, cpuInfluence * 0.75);

        return new FpsSet(
            Math.max(1, (int) Math.round(fortnite)),
            Math.max(1, (int) Math.round(cyberpunk)),
            Math.max(1, (int) Math.round(rdr2))
        );
    }

    // =====================================================
    // 7. MAIN ANALYZER
    // =====================================================

    public static Report analyze(Map<Category, Product> build) {

        Product cpu = build.get(Category.CPU);
        Product gpu = build.get(Category.GPU);
        Product ramProduct = build.get(Category.RAM);
        Product psu = build.get(Category.PSU);
        Product storage = build.get(Category.STORAGE);
        Product cooler = build.get(Category.COOLER);
        Product motherboard = build.get(Category.MOTHERBOARD);
        Product pcCase = build.get(Category.CASE);

        double totalPrice = build.values()
            .stream()
            .mapToDouble(Product::getPrice)
            .sum();

        double gpuScore = gpuIndex(gpu);
        double cpuScore = cpuIndex(cpu);

        double gpuRating = clamp(
            5.5 + Math.log(Math.max(0.1, gpuScore)) * 2.5,
            1.0,
            10.0
        );

        double performanceScore = clamp(
            gpuRating * 0.72 + cpuScore * 0.28,
            1.0,
            10.0
        );

        double balanceScore = clamp(
            10 - Math.abs(gpuRating - cpuScore) * 1.35,
            2.0,
            10.0
        );

        double memoryFactor = ramFactor(ramProduct);
        double board = motherboardFactor(gpu, motherboard);
        double cooling = coolerFactor(cpu, cooler);

        // VRAM affects high resolution performance
        int vram = gpu != null
            ? getInt(gpu, "vram_gb", 8)
            : 0;

        double vram1440 = vram < 6 ? 0.90 : 1.0;
        double vram4k = vram < 6 ? 0.72
            : vram < 8 ? 0.85
            : vram < 10 ? 0.94
            : 1.0;

        // Resolution scaling
        FpsSet fps1080 = calculateFps(
            gpuScore, cpuScore, memoryFactor, board, cooling,
            1.00, 1.00, 1.0
        );

        FpsSet fps1440 = calculateFps(
            gpuScore, cpuScore, memoryFactor, board, cooling,
            0.68, 0.55, vram1440
        );

        FpsSet fps4k = calculateFps(
            gpuScore, cpuScore, memoryFactor, board, cooling,
            0.36, 0.25, vram4k
        );

        // 4K is a resolution, not a performance guarantee.
        boolean supports4K = gpu != null;

        // Value score
        double averageFps = (
            fps1080.fortnite()
            + fps1080.cyberpunk()
            + fps1080.rdr2()
        ) / 3.0;

        double valueScore = totalPrice > 0
            ? clamp(averageFps / totalPrice * 35, 1, 10)
            : 1;

        // Bottleneck verdict
        double difference = cpuScore - gpuRating;

        String bottleneckVerdict;

        if (difference < -1.5) {
            bottleneckVerdict =
                "CPU Bottleneck — ο επεξεργαστής ενδέχεται να περιορίζει σημαντικά την GPU, κυρίως σε 1080p.";
        } else if (difference > 1.5) {
            bottleneckVerdict =
                "GPU Limited — η κάρτα γραφικών είναι ο βασικός περιορισμός στις gaming επιδόσεις.";
        } else {
            bottleneckVerdict =
                "Ισορροπημένο σύστημα — CPU και GPU συνεργάζονται σε καλό επίπεδο.";
        }

        // =================================================
        // 8. TECHNICIAN NOTES
        // =================================================

        List<String> notes = new ArrayList<>(
            CompatibilityEngine.getCompatibilityNotes(build)
        );

        if (gpu != null) {

            if (fps4k.cyberpunk() < 25) {
                notes.add(
                    "Το Cyberpunk 2077 σε native 4K αναμένεται να έχει χαμηλή απόδοση. " +
                    "Συνιστώνται χαμηλότερη ανάλυση ή upscaling."
                );
            } else if (fps4k.cyberpunk() < 40) {
                notes.add(
                    "Το Cyberpunk 2077 σε native 4K θα χρειαστεί συμβιβασμούς στις ρυθμίσεις για ομαλότερη εμπειρία."
                );
            } else if (fps4k.cyberpunk() < 60) {
                notes.add(
                    "Το σύστημα μπορεί να τρέξει Cyberpunk σε 4K, αλλά δεν αναμένεται σταθερή εμπειρία 60 FPS."
                );
            } else {
                notes.add(
                    "Η εκτιμώμενη απόδοση στο Cyberpunk 4K είναι ικανοποιητική σε σχέση με τον στόχο των 60 FPS."
                );
            }

            if (vram < 8) {
                notes.add(
                    "Η διαθέσιμη VRAM μπορεί να περιορίσει τις επιλογές texture quality σε υψηλές αναλύσεις."
                );
            }
        }

        if (cpu != null && cpuScore < 4.5) {
            notes.add(
                "Ο επεξεργαστής είναι σχετικά αδύναμος για σύγχρονο gaming και μπορεί να περιορίσει τα FPS."
            );
        }

        if (cpu != null && gpu != null && difference < -1.5) {
            notes.add(
                "Ένας ισχυρότερος επεξεργαστής θα μπορούσε να αξιοποιήσει καλύτερα την κάρτα γραφικών."
            );
        }

        if (ramProduct != null) {

            int capacity = getInt(ramProduct, "capacity_gb", 16);
            int modules = getInt(ramProduct, "modules", 1);

            if (capacity < 16) {
                notes.add(
                    "Τα λιγότερα από 16GB RAM ενδέχεται να προκαλέσουν stuttering σε σύγχρονα παιχνίδια."
                );
            }

            if (modules == 1) {
                notes.add(
                    "Η RAM λειτουργεί πιθανότατα σε single-channel διάταξη. " +
                    "Ένα δεύτερο συμβατό module μπορεί να βελτιώσει τις επιδόσεις."
                );
            }

            if (capacity >= 32) {
                notes.add(
                    "Τα " + capacity + "GB RAM προσφέρουν άνεση για σύγχρονο gaming και multitasking."
                );
            }
        }

        if (storage != null) {

            String interfaceType = getString(storage, "interface").toUpperCase();

            if (interfaceType.contains("SATA")) {
                notes.add(
                    "Ο SATA SSD είναι επαρκής για gaming. " +
                    "Ένας NVMe SSD θα βελτίωνε κυρίως τους χρόνους φόρτωσης, όχι ουσιαστικά τα FPS."
                );
            }
        }

        if (psu != null && gpu != null) {

            int wattage = getInt(psu, "wattage", 550);
            int recommended = getInt(gpu, "recommended_psu_w", 0);
            int estimated = estimateSystemWattage(build);

            if (recommended > 0 && wattage < recommended) {
                notes.add(
                    "Προσοχή: Το PSU είναι χαμηλότερο από τη συνιστώμενη ισχύ τροφοδοτικού της GPU."
                );
            } else if (wattage < estimated * 1.15) {
                notes.add(
                    "Το PSU έχει περιορισμένο περιθώριο ισχύος για το συγκεκριμένο σύστημα."
                );
            } else {
                notes.add(
                    "Το τροφοδοτικό καλύπτει τις εκτιμώμενες ανάγκες του συστήματος."
                );
            }
        }

        if (cooler != null && cpu != null) {

            int cpuTdp = getInt(cpu, "tdp_max_w", 65);
            int coolerTdp = getInt(cooler, "max_tdp_w", 150);

            if (coolerTdp < cpuTdp) {
                notes.add(
                    "Η ψύκτρα έχει χαμηλότερη ονομαστική ικανότητα ψύξης από το TDP του επεξεργαστή."
                );
            } else {
                notes.add(
                    "Η ψύκτρα διαθέτει επαρκή ονομαστική ικανότητα για το TDP του CPU."
                );
            }
        }

        if (pcCase != null) {

            int includedFans = getInt(pcCase, "included_fans", 0);

            if (includedFans == 0) {
                notes.add(
                    "Το κουτί δεν περιλαμβάνει εργοστασιακούς ανεμιστήρες. " +
                    "Συνιστάται η προσθήκη ανεμιστήρων για σωστό airflow."
                );
            } else if (includedFans >= 3) {
                notes.add(
                    "Το κουτί διαθέτει πολλούς εργοστασιακούς ανεμιστήρες, κάτι που μπορεί να βοηθήσει στη θερμική διαχείριση."
                );
            }
        }

        notes.add(
            "Οι εκτιμήσεις FPS αφορούν High preset, native resolution, " +
            "Ray Tracing OFF και χωρίς DLSS/FSR. " +
            "Αποτελούν προσεγγιστικές εκτιμήσεις και όχι εγγυημένες μετρήσεις."
        );

        return new Report(
            round1(performanceScore),
            round1(valueScore),
            round1(balanceScore),
            supports4K,
            fps1080,
            fps1440,
            fps4k,
            bottleneckVerdict,
            notes
        );
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
