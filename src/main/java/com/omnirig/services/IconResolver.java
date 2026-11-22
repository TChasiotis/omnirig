package com.omnirig.services;

import com.omnirig.models.Product;

public class IconResolver {

    private static final String BASE_PATH = "/com/omnirig/images/icons/";

    public static String getIconPath(Product p) {
        String filename = switch (p.getCategory()) {
            case CPU -> resolveCpu(p);
            case MOTHERBOARD -> resolveMotherboard(p);
            case RAM -> resolveRam(p);
            case GPU -> resolveGpu(p);
            case COOLER -> resolveCooler(p);
            case CASE -> resolveCase(p);
            case PSU -> resolvePsu(p);
            case STORAGE -> resolveStorage(p);
        };
        return BASE_PATH + filename;
    }

    private static String resolveCpu(Product p) {
        String socket = p.getStringSpec("socket").toUpperCase();
        if (socket.contains("AM5")) return "cpu_am5.png";
        if (socket.contains("AM4")) return "cpu_am4.png";
        return "cpu_intel.png";
    }

    private static String resolveMotherboard(Product p) {
    String socket = p.getStringSpec("socket").toLowerCase().replace(" ", "");
    return switch (socket) {
        case "am5" -> "mb_am5.png";
        case "lga1151" -> "mb_lga1151.png";
        case "lga1200" -> "mb_lga1200.png";
        case "lga1700" -> "mb_lga1700.png";
        case "lga1851" -> "mb_lga1851.png";
        default -> "mb_am4.png";
    };
}

    private static String resolveRam(Product p) {
        String type = p.getStringSpec("type").toUpperCase();
        return type.contains("DDR5") ? "ram_ddr5.png" : "ram_ddr4.png";
    }

    private static String resolveGpu(Product p) {
        String cooling = p.getStringSpec("cooling_type").toLowerCase();
        String fans = (cooling.contains("triple") || cooling.contains("3")) ? "triple" : "dual";

        String chipset = (p.getStringSpec("chipset_brand") + " " + p.getStringSpec("chipset") + " " + p.getModel()).toLowerCase();
        String brand;
        if (chipset.contains("amd") || chipset.contains("radeon")) {
            brand = "radeon";
        } else if (chipset.contains("intel") || chipset.contains("arc")) {
            brand = "arc";
        } else {
            brand = "rtx";
        }

        return "gpu_" + fans + "_" + brand + ".png";
    }

    private static String resolveCooler(Product p) {
    String type = p.getStringSpec("type").toLowerCase();
    int radSize = p.getIntSpec("radiator_size_mm");

    // 1. AIO / Liquid
    if (type.contains("aio") || type.contains("liquid") || radSize > 0) {
        return radSize >= 360 ? "aio_360mm.png" : "aio_240mm.png";
    }

    // 2. Low Profile
    int height = p.getIntSpec("height_mm");
    if (height > 0 && height <= 95) {
        return "cooler_low_profile.png";
    }

    // 3. Dual Tower vs Single Tower
    int fans = p.getIntSpec("fan_count");
    return fans >= 2 ? "cooler_dual_tower.png" : "cooler_tower.png";
}

    private static String resolveCase(Product p) {
        String caseType = p.getStringSpec("case_type").toLowerCase();
        if (caseType.contains("dual")) return "case_dual_chamber.png";
        if (caseType.contains("full")) return "case_full_tower.png";
        if (caseType.contains("micro") || caseType.contains("matx")) return "case_micro_atx.png";
        if (caseType.contains("mini") || caseType.contains("itx") || caseType.contains("sff")) return "case_mini_itx.png";
        return "case_mid_tower.png";
    }

    private static String resolvePsu(Product p) {
        String ff = p.getStringSpec("form_factor").toLowerCase();
        if (ff.contains("sfx")) return "psu_sfx.png";

        String mod = p.getStringSpec("modularity").toLowerCase();
        if (mod.contains("non")) return "psu_non_modular.png";
        return "psu_fully_modular.png";
    }

    private static String resolveStorage(Product p) {
        String info = (p.getStringSpec("form_factor") + " " + p.getStringSpec("interface")).toLowerCase();
        if (info.contains("m.2") || info.contains("nvme") || info.contains("pcie")) {
            return "ssd_m2.png";
        }
        return "ssd_sata.png";
    }
}