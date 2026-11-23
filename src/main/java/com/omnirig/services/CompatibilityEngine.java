package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.models.Product;

import java.util.*;
import java.util.stream.Collectors;

public class CompatibilityEngine {

    public static List<Product> getCompatibleProducts(
            Category targetCategory,
            List<Product> allCategoryProducts,
            Map<Category, Product> currentBuild) {

        return allCategoryProducts.stream()
            .filter(candidate -> isCompatibleWithBuild(candidate, currentBuild))
            .collect(Collectors.toList());
    }

    public static boolean isCompatibleWithBuild(Product candidate, Map<Category, Product> currentBuild) {
        for (Map.Entry<Category, Product> entry : currentBuild.entrySet()) {
            if (entry.getKey() == candidate.getCategory()) continue;
            if (!areCompatible(candidate, entry.getValue())) {
                return false;
            }
        }
        Product psu = candidate.getCategory() == Category.PSU ? candidate : currentBuild.get(Category.PSU);
        if (psu != null) {
            int requiredW = estimateRequiredPsuWattage(currentBuild, candidate);
            if (getInt(psu, "wattage", 0) < requiredW) {
                return false;
            }
        }
        return true;
    }

    public static boolean areCompatible(Product a, Product b) {
        Category ca = a.getCategory();
        Category cb = b.getCategory();

        if (ca == Category.CPU && cb == Category.MOTHERBOARD) return checkCpuMobo(a, b);
        if (cb == Category.CPU && ca == Category.MOTHERBOARD) return checkCpuMobo(b, a);

        if (ca == Category.CPU && cb == Category.RAM) return checkCpuRam(a, b);
        if (cb == Category.CPU && ca == Category.RAM) return checkCpuRam(b, a);

        if (ca == Category.MOTHERBOARD && cb == Category.RAM) return checkMoboRam(a, b);
        if (cb == Category.MOTHERBOARD && ca == Category.RAM) return checkMoboRam(b, a);

        if (ca == Category.MOTHERBOARD && cb == Category.CASE) return checkMoboCase(a, b);
        if (cb == Category.MOTHERBOARD && ca == Category.CASE) return checkMoboCase(b, a);

        if (ca == Category.GPU && cb == Category.CASE) return checkGpuCase(a, b);
        if (cb == Category.GPU && ca == Category.CASE) return checkGpuCase(b, a);

        if (ca == Category.GPU && cb == Category.PSU) return checkGpuPsu(a, b);
        if (cb == Category.GPU && ca == Category.PSU) return checkGpuPsu(b, a);

        if (ca == Category.COOLER && cb == Category.CPU) return checkCoolerCpu(a, b);
        if (cb == Category.COOLER && ca == Category.CPU) return checkCoolerCpu(b, a);

        if (ca == Category.COOLER && cb == Category.CASE) return checkCoolerCase(a, b);
        if (cb == Category.COOLER && ca == Category.CASE) return checkCoolerCase(b, a);

        if (ca == Category.PSU && cb == Category.CASE) return checkPsuCase(a, b);
        if (cb == Category.PSU && ca == Category.CASE) return checkPsuCase(b, a);

        if (ca == Category.STORAGE && cb == Category.MOTHERBOARD) return checkStorageMobo(a, b);
        if (cb == Category.STORAGE && ca == Category.MOTHERBOARD) return checkStorageMobo(b, a);

        if (ca == Category.STORAGE && cb == Category.CASE) return checkStorageCase(a, b);
        if (cb == Category.STORAGE && ca == Category.CASE) return checkStorageCase(b, a);

        return true;
    }

    /**
     * Σημειώσεις συμβατότητας που δεν μπλοκάρουν τη συναρμολόγηση,
     * αλλά ο Τεχνικός τις αναφέρει στην αξιολόγηση.
     */
    public static List<String> getCompatibilityNotes(Map<Category, Product> build) {
        List<String> list = new ArrayList<>();
        Product mobo = build.get(Category.MOTHERBOARD);
        Product pcCase = build.get(Category.CASE);
        Product gpu = build.get(Category.GPU);

        if (mobo != null && pcCase != null) {
            boolean caseUsbC = getBool(pcCase, "usb_c_front", false);
            boolean moboHeader = getBool(mobo, "usb_c_header", false);
            if (caseUsbC && !moboHeader) {
                list.add("Το κουτί (" + pcCase.getName() + ") έχει μπροστινή USB-C, αλλά η μητρική δεν έχει εσωτερικό USB-C header.");
            }
        }

        if (mobo != null && gpu != null) {
            String gpuPcie = getString(gpu, "pcie_version");
            String moboPcie = getString(mobo, "pcie_x16_version");
            if (!gpuPcie.isEmpty() && !moboPcie.isEmpty() && !gpuPcie.equals(moboPcie)) {
                list.add("Η κάρτα γραφικών υποστηρίζει PCIe " + gpuPcie + " και η μητρική PCIe " + moboPcie + " (λειτουργεί κανονικά με ελάχιστη/μηδενική διαφορά).");
            }
        }
        return list;
    }

    private static boolean checkCpuMobo(Product cpu, Product mobo) {
        if (!getString(cpu, "socket").equalsIgnoreCase(getString(mobo, "socket"))) return false;
        List<String> cpuMem = getStringList(cpu, "memory_support");
        String moboMem = getString(mobo, "memory_type");
        return cpuMem.isEmpty() || cpuMem.stream().anyMatch(m -> m.equalsIgnoreCase(moboMem));
    }

    private static boolean checkCpuRam(Product cpu, Product ram) {
        List<String> cpuMem = getStringList(cpu, "memory_support");
        String ramType = getString(ram, "type");
        return cpuMem.isEmpty() || cpuMem.stream().anyMatch(m -> m.equalsIgnoreCase(ramType));
    }

    private static boolean checkMoboRam(Product mobo, Product ram) {
        if (!getString(mobo, "memory_type").equalsIgnoreCase(getString(ram, "type"))) return false;
        if (getInt(ram, "modules", 1) > getInt(mobo, "memory_slots", 4)) return false;
        if (getInt(ram, "capacity_gb", 0) > getInt(mobo, "max_memory_gb", 256)) return false;
        return true;
    }

    private static boolean checkMoboCase(Product mobo, Product pcCase) {
        String ff = getString(mobo, "form_factor");
        return getStringList(pcCase, "supported_mobo_form_factors")
            .stream().anyMatch(s -> s.equalsIgnoreCase(ff));
    }

    private static boolean checkGpuCase(Product gpu, Product pcCase) {
        return getInt(gpu, "length_mm", 0) <= getInt(pcCase, "max_gpu_length_mm", 999);
    }

    private static boolean checkGpuPsu(Product gpu, Product psu) {
        if (getInt(psu, "wattage", 0) < getInt(gpu, "recommended_psu_w", 0)) return false;
        String conn = getString(gpu, "power_connectors").toLowerCase();
        if (conn.contains("12vhpwr") || conn.contains("16-pin")) {
            return getBool(psu, "pcie_5_12vhpwr", false);
        } else if (conn.contains("8-pin")) {
            int needed = conn.contains("3x") ? 3 : (conn.contains("2x") ? 2 : 1);
            return getInt(psu, "pcie_8pin_connectors", 0) >= needed;
        }
        return true;
    }

    private static boolean checkCoolerCpu(Product cooler, Product cpu) {
        String socket = getString(cpu, "socket");
        List<String> sockets = getStringList(cooler, "supported_sockets");
        if (!sockets.isEmpty() && sockets.stream().noneMatch(s -> s.equalsIgnoreCase(socket))) return false;
        int cpuMaxTdp = getInt(cpu, "tdp_max_w", getInt(cpu, "tdp_base_w", 65));
        return getInt(cooler, "max_tdp_w", 999) >= cpuMaxTdp;
    }

    private static boolean checkCoolerCase(Product cooler, Product pcCase) {
        String type = getString(cooler, "type").toLowerCase();
        if (type.contains("aio") || type.contains("liquid")) {
            int rad = getInt(cooler, "radiator_size_mm", 240);
            return rad <= getInt(pcCase, "max_radiator_top_mm", 0)
                || rad <= getInt(pcCase, "max_radiator_front_mm", 0)
                || rad <= getInt(pcCase, "max_radiator_rear_mm", 0);
        }
        return getInt(cooler, "height_mm", 0) <= getInt(pcCase, "max_cpu_cooler_height_mm", 999);
    }

    private static boolean checkPsuCase(Product psu, Product pcCase) {
        String psuFF = getString(psu, "form_factor").toUpperCase();
        String caseType = getString(pcCase, "case_type").toLowerCase();
        if ((caseType.contains("mini-itx") || caseType.contains("sff")) && !psuFF.contains("SFX")) {
            return false;
        }
        return getInt(psu, "length_mm", 0) <= getInt(pcCase, "max_psu_length_mm", 999);
    }

    private static boolean checkStorageMobo(Product storage, Product mobo) {
        String iface = getString(storage, "interface").toLowerCase();
        String ff = getString(storage, "form_factor").toLowerCase();
        if (ff.contains("m.2") || iface.contains("nvme")) return getInt(mobo, "m2_slots", 0) > 0;
        if (iface.contains("sata")) return getInt(mobo, "sata_ports", 0) > 0;
        return true;
    }

    private static boolean checkStorageCase(Product storage, Product pcCase) {
        if (getString(storage, "form_factor").contains("2.5")) {
            return getInt(pcCase, "drive_bays_2_5", 1) > 0;
        }
        return true;
    }

    public static int estimateSystemWattage(Map<Category, Product> build) {
        int cpuW = build.containsKey(Category.CPU) ? getInt(build.get(Category.CPU), "tdp_max_w", 65) : 0;
        int gpuW = build.containsKey(Category.GPU) ? getInt(build.get(Category.GPU), "tdp_w", 0) : 0;
        int baseSystemW = build.isEmpty() ? 0 : 75;
        return cpuW + gpuW + baseSystemW;
    }

    private static int estimateRequiredPsuWattage(Map<Category, Product> currentBuild, Product candidate) {
        Product cpu = candidate.getCategory() == Category.CPU ? candidate : currentBuild.get(Category.CPU);
        Product gpu = candidate.getCategory() == Category.GPU ? candidate : currentBuild.get(Category.GPU);
        int cpuW = cpu != null ? getInt(cpu, "tdp_max_w", 65) : 0;
        int gpuW = gpu != null ? getInt(gpu, "tdp_w", 0) : 0;
        int recGpuPsu = gpu != null ? getInt(gpu, "recommended_psu_w", 0) : 0;
        int calcWithHeadroom = (int) Math.ceil((cpuW + gpuW + 75) * 1.25);
        return Math.max(recGpuPsu, calcWithHeadroom);
    }

    public static String getString(Product p, String key) {
        Object v = p.getSpecs().get(key);
        return v == null ? "" : String.valueOf(v);
    }

    public static int getInt(Product p, String key, int def) {
        Object v = p.getSpecs().get(key);
        if (v instanceof Number n) return n.intValue();
        try { return v != null ? Integer.parseInt(String.valueOf(v)) : def; }
        catch (Exception e) { return def; }
    }

    public static double getDouble(Product p, String key, double def) {
        Object v = p.getSpecs().get(key);
        if (v instanceof Number n) return n.doubleValue();
        try { return v != null ? Double.parseDouble(String.valueOf(v)) : def; }
        catch (Exception e) { return def; }
    }

    public static boolean getBool(Product p, String key, boolean def) {
        Object v = p.getSpecs().get(key);
        if (v instanceof Boolean b) return b;
        return v != null ? Boolean.parseBoolean(String.valueOf(v)) : def;
    }

    public static List<String> getStringList(Product p, String key) {
        Object v = p.getSpecs().get(key);
        List<String> result = new ArrayList<>();
        if (v instanceof Collection<?> col) {
            for (Object item : col) result.add(String.valueOf(item));
        }
        return result;
    }
}