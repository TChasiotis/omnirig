package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MockProductService implements ProductService {
    private final List<Product> database = new ArrayList<>();

    public MockProductService() {
        // 1. RAM
        database.add(new Product(1, Category.RAM, "Corsair", "Vengeance LPX 16GB (2x8GB) DDR4-3200 CL16", 39.9, 20)
            .addSpec("type", "DDR4").addSpec("capacity_gb", 16).addSpec("modules", 2)
            .addSpec("module_capacity_gb", 8).addSpec("speed_mhz", 3200).addSpec("cas_latency", 16)
            .addSpec("timings", "16-18-18-36").addSpec("voltage_v", 1.35).addSpec("height_mm", 31)
            .addSpec("xmp_support", true).addSpec("expo_support", false).addSpec("rgb", false).addSpec("color", "Black"));

        // 2. CPU
        database.add(new Product(2, Category.CPU, "AMD", "Ryzen 5 3600", 79.9, 15)
            .addSpec("socket", "AM4").addSpec("cores", 6).addSpec("threads", 12)
            .addSpec("base_clock_ghz", 3.6).addSpec("boost_clock_ghz", 4.2).addSpec("cache_l3_mb", 32)
            .addSpec("tdp_base_w", 65).addSpec("tdp_max_w", 65).addSpec("memory_support", List.of("DDR4"))
            .addSpec("pcie_version", "4.0").addSpec("has_integrated_graphics", false));

        // 3. GPU
        database.add(new Product(3, Category.GPU, "MSI", "GeForce RTX 3060 VENTUS 2X 12G OC", 259.0, 8)
            .addSpec("chipset_brand", "NVIDIA").addSpec("chipset", "GeForce RTX 3060").addSpec("vram_gb", 12)
            .addSpec("vram_type", "GDDR6").addSpec("memory_bus_bit", 192).addSpec("boost_clock_mhz", 1807)
            .addSpec("pcie_interface", "PCIe 4.0 x16").addSpec("pcie_version", "4.0")
            .addSpec("length_mm", 235).addSpec("width_mm", 124).addSpec("thickness_mm", 42).addSpec("slots_thickness", 2.0)
            .addSpec("tdp_w", 170).addSpec("recommended_psu_w", 550).addSpec("power_connectors", "1x 8-pin")
            .addSpec("cooling_type", "Dual Fan").addSpec("hdmi_ports", 1).addSpec("displayport_ports", 3)
            .addSpec("max_displays", 4).addSpec("ray_tracing", true).addSpec("upscaling_tech", "DLSS 2").addSpec("color", "Black"));

        // 4. Motherboard
        database.add(new Product(4, Category.MOTHERBOARD, "MSI", "B450 TOMAHAWK MAX II", 99.9, 12)
            .addSpec("socket", "AM4").addSpec("chipset", "B450").addSpec("form_factor", "ATX")
            .addSpec("length_mm", 305).addSpec("width_mm", 244).addSpec("memory_type", "DDR4")
            .addSpec("memory_slots", 4).addSpec("max_memory_gb", 128).addSpec("max_memory_speed_mhz", 4133)
            .addSpec("pcie_x16_version", "3.0").addSpec("pcie_x16_slots", 2).addSpec("pcie_x1_slots", 3)
            .addSpec("m2_slots", 1).addSpec("m2_slots_gen", List.of("3.0")).addSpec("sata_ports", 6)
            .addSpec("lan_speed", "1Gbps").addSpec("usb_ports_rear", 6).addSpec("usb_c_rear", 1)
            .addSpec("usb_c_header", false).addSpec("audio_codec", "Realtek ALC892").addSpec("vrm_phases", "4+2")
            .addSpec("rgb_headers_12v", 2).addSpec("argb_headers_5v", 0).addSpec("fan_headers", 6)
            .addSpec("bios_flashback", true).addSpec("color", "Black"));

        // 5. Cooler
        database.add(new Product(5, Category.COOLER, "DeepCool", "AG400", 24.9, 25)
            .addSpec("type", "Air").addSpec("supported_sockets", List.of("AM4", "AM5", "LGA 1151", "LGA 1200", "LGA 1700", "LGA 1851"))
            .addSpec("height_mm", 150).addSpec("max_tdp_w", 220).addSpec("fan_count", 1)
            .addSpec("fan_size_mm", 120).addSpec("max_noise_db", 31.6).addSpec("rgb", false).addSpec("color", "Black/Silver"));

        // 6. Case
        database.add(new Product(6, Category.CASE, "NZXT", "H6 Flow", 109.9, 10)
            .addSpec("case_type", "Dual-Chamber Mid-Tower")
            .addSpec("supported_mobo_form_factors", List.of("ATX", "Micro-ATX", "Mini-ITX"))
            .addSpec("max_gpu_length_mm", 365).addSpec("max_cpu_cooler_height_mm", 163).addSpec("max_psu_length_mm", 200)
            .addSpec("max_radiator_top_mm", 360).addSpec("max_radiator_front_mm", 0).addSpec("max_radiator_rear_mm", 120)
            .addSpec("included_fans", 3).addSpec("max_fans", 9).addSpec("expansion_slots", 7)
            .addSpec("drive_bays_2_5", 2).addSpec("drive_bays_3_5", 1)
            .addSpec("length_mm", 415).addSpec("width_mm", 287).addSpec("height_mm", 435)
            .addSpec("side_panel", "Tempered Glass").addSpec("usb_c_front", true).addSpec("color", "White"));

        // 7. PSU
        database.add(new Product(7, Category.PSU, "MSI", "MAG A550BN 550W", 49.9, 18)
            .addSpec("wattage", 550).addSpec("efficiency_rating", "80+ Bronze").addSpec("modularity", "Non-Modular")
            .addSpec("form_factor", "ATX").addSpec("length_mm", 140).addSpec("atx_version", "ATX 2.4")
            .addSpec("pcie_5_12vhpwr", false).addSpec("pcie_8pin_connectors", 2).addSpec("sata_connectors", 5)
            .addSpec("fan_size_mm", 120).addSpec("zero_rpm_mode", false).addSpec("warranty_years", 5).addSpec("color", "Black"));

        // 8. SSD
        database.add(new Product(8, Category.STORAGE, "Kingston", "A400 480GB SATA 2.5\"", 34.9, 30)
            .addSpec("capacity_gb", 480).addSpec("form_factor", "2.5\"").addSpec("interface", "SATA III")
            .addSpec("read_speed_mb_s", 500).addSpec("write_speed_mb_s", 450).addSpec("nand_type", "TLC")
            .addSpec("dram_cache", false).addSpec("heatsink", false).addSpec("tbw", 160).addSpec("warranty_years", 3));
    }

    @Override
    public List<Product> getAllProducts() {
        return database;
    }

    @Override
    public List<Product> getProductsByCategory(Category category) {
        return database.stream()
            .filter(p -> p.getCategory() == category)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> searchProducts(String keyword, Double maxPrice, Category category) {
        return database.stream()
            .filter(p -> category == null || p.getCategory() == category)
            .filter(p -> keyword == null || keyword.isEmpty() || p.getName().toLowerCase().contains(keyword.toLowerCase()))
            .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
            .collect(Collectors.toList());
    }
}