package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.analyzer.HardwareScorer;
import com.omnirig.services.analyzer.TechnicianNotesGenerator;

import java.util.List;
import java.util.Map;

import static com.omnirig.services.CompatibilityEngine.getInt;
import static com.omnirig.services.analyzer.HardwareScorer.clamp;

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

    public static Report analyze(Map<Category, Product> build) {
        Product cpu = build.get(Category.CPU);
        Product gpu = build.get(Category.GPU);
        Product ramProduct = build.get(Category.RAM);
        Product cooler = build.get(Category.COOLER);
        Product motherboard = build.get(Category.MOTHERBOARD);

        double totalPrice = build.values().stream().mapToDouble(Product::getPrice).sum();

        double gpuScore = HardwareScorer.gpuIndex(gpu);
        double cpuScore = HardwareScorer.cpuIndex(cpu);

        double gpuRating = clamp(5.5 + Math.log(Math.max(0.1, gpuScore)) * 2.5, 1.0, 10.0);
        double performanceScore = clamp(gpuRating * 0.72 + cpuScore * 0.28, 1.0, 10.0);
        double balanceScore = clamp(10 - Math.abs(gpuRating - cpuScore) * 1.35, 2.0, 10.0);

        double memoryFactor = HardwareScorer.ramFactor(ramProduct);
        double board = HardwareScorer.motherboardFactor(gpu, motherboard);
        double cooling = HardwareScorer.coolerFactor(cpu, cooler);

        int vram = gpu != null ? getInt(gpu, "vram_gb", 8) : 0;
        double vram1440 = vram < 6 ? 0.90 : 1.0;
        double vram4k = vram < 6 ? 0.72 : vram < 8 ? 0.85 : vram < 10 ? 0.94 : 1.0;

        FpsSet fps1080 = calculateFps(gpuScore, cpuScore, memoryFactor, board, cooling, 1.00, 1.00, 1.0);
        FpsSet fps1440 = calculateFps(gpuScore, cpuScore, memoryFactor, board, cooling, 0.68, 0.55, vram1440);
        FpsSet fps4k   = calculateFps(gpuScore, cpuScore, memoryFactor, board, cooling, 0.36, 0.25, vram4k);

        boolean supports4K = gpu != null;

        double averageFps = (fps1080.fortnite() + fps1080.cyberpunk() + fps1080.rdr2()) / 3.0;
        double valueScore = totalPrice > 0 ? clamp(averageFps / totalPrice * 35, 1, 10) : 1;

        double difference = cpuScore - gpuRating;
        String bottleneckVerdict;
        if (difference < -1.5) {
            bottleneckVerdict = "CPU Bottleneck — ο επεξεργαστής ενδέχεται να περιορίζει σημαντικά την GPU, κυρίως σε 1080p.";
        } else if (difference > 1.5) {
            bottleneckVerdict = "GPU Limited — η κάρτα γραφικών είναι ο βασικός περιορισμός στις gaming επιδόσεις.";
        } else {
            bottleneckVerdict = "Ισορροπημένο σύστημα — CPU και GPU συνεργάζονται σε καλό επίπεδο.";
        }

        List<String> notes = TechnicianNotesGenerator.generate(build, cpuScore, difference, vram, fps4k);

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

    private static double cpuGamingFactor(double cpuScore, double influence) {
        double difference = cpuScore - 5.5;
        return clamp(1.0 + difference * 0.065 * influence, 0.70, 1.12);
    }

    private static FpsSet calculateFps(
            double gpu, double cpu, double ramProduct, double motherboard, double cooler,
            double resolutionScale, double cpuInfluence, double vramFactor) {

        double gpuScaling = Math.pow(gpu, 0.82);
        double systemFactor = ramProduct * motherboard * cooler * vramFactor;

        double fortnite  = 110 * gpuScaling * resolutionScale * systemFactor * cpuGamingFactor(cpu, cpuInfluence * 1.45);
        double cyberpunk = 55  * gpuScaling * resolutionScale * systemFactor * cpuGamingFactor(cpu, cpuInfluence * 0.85);
        double rdr2      = 70  * gpuScaling * resolutionScale * systemFactor * cpuGamingFactor(cpu, cpuInfluence * 0.75);

        return new FpsSet(
            Math.max(1, (int) Math.round(fortnite)),
            Math.max(1, (int) Math.round(cyberpunk)),
            Math.max(1, (int) Math.round(rdr2))
        );
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}