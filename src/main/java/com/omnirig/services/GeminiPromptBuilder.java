package com.omnirig.services;

import com.omnirig.models.Category;

public final class GeminiPromptBuilder {

    private GeminiPromptBuilder() {}

    public static String build(String productName, Category category) {
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
}