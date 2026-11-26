package com.omnirig.services.analyzer;

import com.omnirig.models.Product;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.omnirig.services.CompatibilityEngine.*;

public final class HardwareScorer {

    private static final Pattern NVIDIA_PATTERN = Pattern.compile("(?:RTX|GTX)\\s*(\\d{4})");
    private static final Pattern AMD_PATTERN    = Pattern.compile("RX\\s*(\\d{4})");
    private static final Pattern INTEL_GPU_PAT  = Pattern.compile("ARC\\s+([AB])(\\d{3})");
    private static final Pattern RYZEN_PATTERN  = Pattern.compile("RYZEN\\s+[3579]\\s+(\\d{4})");
    private static final Pattern INTEL_CPU_PAT  = Pattern.compile("I[3579][- ]?(\\d{4,5})");
    private static final Pattern PCIE_PATTERN   = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private HardwareScorer() {}

    /**
     * Relative GPU performance index (RTX 3060 ≈ 1.00).
     */
    public static double gpuIndex(Product gpu) {
        if (gpu == null) return 0.40;

        String chipset = getString(gpu, "chipset");
        if (chipset.isBlank()) chipset = gpu.getName();
        String name = chipset.toUpperCase(Locale.ROOT);

        // NVIDIA RTX / GTX
        Matcher nv = NVIDIA_PATTERN.matcher(name);
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

            if (name.contains("GTX") && tier < 70) base *= 0.80;

            return base * generationFactor * suffix;
        }

        // AMD Radeon RX
        Matcher amd = AMD_PATTERN.matcher(name);
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
                case 5, 6 -> 0.90;
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
        Matcher intel = INTEL_GPU_PAT.matcher(name);
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

        // Fallback: unknown GPU estimated by specs
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

    public static double cpuIndex(Product cpu) {
        if (cpu == null) return 4.0;

        int cores = getInt(cpu, "cores", 4);
        int threads = getInt(cpu, "threads", cores * 2);
        int cache = getInt(cpu, "cache_l3_mb", 8);
        double boost = getDouble(cpu, "boost_clock_ghz", 3.5);
        String name = cpu.getName().toUpperCase(Locale.ROOT);

        double generationBonus = 0;

        Matcher ryzen = RYZEN_PATTERN.matcher(name);
        if (ryzen.find()) {
            int generation = Integer.parseInt(ryzen.group(1).substring(0, 1));
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

        Matcher intel = INTEL_CPU_PAT.matcher(name);
        if (intel.find()) {
            String number = intel.group(1);
            int generation = number.length() == 5
                ? Integer.parseInt(number.substring(0, 2))
                : Integer.parseInt(number.substring(0, 1));
            generationBonus = Math.max(0, Math.min(1.0, (generation - 8) * 0.09));
        }

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

    public static double ramFactor(Product ram) {
        if (ram == null) return 0.85;

        int capacity = getInt(ram, "capacity_gb", 16);
        int modules = getInt(ram, "modules", 1);
        int speed = getInt(ram, "speed_mhz", 3200);
        String type = getString(ram, "type").toUpperCase();

        double factor = 1.0;

        if (capacity <= 8) factor *= 0.82;
        else if (capacity < 16) factor *= 0.92;
        else if (capacity >= 32) factor *= 1.01;

        factor *= (modules >= 2) ? 1.035 : 0.96;

        double referenceSpeed = type.contains("DDR5") ? 5600 : 3200;
        double speedFactor = Math.pow(Math.max(0.5, speed / referenceSpeed), 0.12);
        factor *= clamp(speedFactor, 0.94, 1.06);

        return clamp(factor, 0.65, 1.10);
    }

    public static double motherboardFactor(Product gpu, Product motherboard) {
        if (gpu == null || motherboard == null) return 1.0;

        String gpuPcie = getString(gpu, "pcie_version");
        String boardPcie = getString(motherboard, "pcie_x16_version");
        if (gpuPcie.isBlank() || boardPcie.isBlank()) return 1.0;

        double gpuGen = parsePcie(gpuPcie);
        double boardGen = parsePcie(boardPcie);
        if (gpuGen <= 0 || boardGen <= 0) return 1.0;

        return boardGen < gpuGen ? 0.97 : 1.0;
    }

    public static double coolerFactor(Product cpu, Product cooler) {
        if (cpu == null || cooler == null) return 1.0;

        int cpuTdp = getInt(cpu, "tdp_max_w", 65);
        int coolerTdp = getInt(cooler, "max_tdp_w", 150);
        if (cpuTdp <= 0 || coolerTdp <= 0) return 1.0;

        double ratio = coolerTdp / (double) cpuTdp;
        if (ratio < 0.65) return 0.90;
        if (ratio < 0.90) return 0.96;
        return 1.0;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double parsePcie(String version) {
        Matcher matcher = PCIE_PATTERN.matcher(version);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0;
    }
}