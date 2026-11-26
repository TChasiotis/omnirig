package com.omnirig.controllers.builder;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.BuildAnalyzer;
import com.omnirig.services.CompatibilityEngine;
import com.omnirig.services.ProductService;
import com.omnirig.utils.ModalOverlayHelper;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.Consumer;

public class PcBuilderBudgetGenerator {

    private record BudgetOption(String title, Map<Category, Product> build) {}

    private final ProductService productService;
    private final Category[] buildOrder;

    public PcBuilderBudgetGenerator(ProductService productService, Category[] buildOrder) {
        this.productService = productService;
        this.buildOrder = buildOrder;
    }

    public void showBudgetFlow(StackPane rootStack, Consumer<Map<Category, Product>> onBuildChosen) {
        ModalOverlayHelper.showBudgetInputModal(rootStack, budget -> {
            List<BudgetOption> options = generateBudgetOptions(budget);
            if (options.isEmpty()) return;

            VBox listBox = new VBox(10);
            ToggleGroup tg = new ToggleGroup();
            List<RadioButton> radios = new ArrayList<>();

            for (int i = 0; i < options.size(); i++) {
                BudgetOption opt = options.get(i);
                double sum = opt.build().values().stream().mapToDouble(Product::getPrice).sum();
                BuildAnalyzer.Report rep = BuildAnalyzer.analyze(opt.build());

                RadioButton rb = new RadioButton(opt.title() + "  —  " + String.format("%.2f €", sum)
                    + "  (Perf: " + rep.performanceScore() + "/10 | Balance: " + rep.balanceScore() + "/10)");
                rb.setToggleGroup(tg);
                rb.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
                if (i == 0) rb.setSelected(true);

                Label summary = new Label(buildShortSummary(opt.build()));
                summary.setWrapText(true);
                summary.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

                VBox box = new VBox(4, rb, summary);
                box.setPadding(new Insets(10));
                box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");
                listBox.getChildren().add(box);
                radios.add(rb);
            }

            ModalOverlayHelper.showCustomModal(
                rootStack,
                "Επιλογή Πρότασης Build",
                "Βρέθηκαν " + options.size() + " προτάσεις για budget ~" + String.format("%.0f €", budget),
                listBox,
                "Φόρτωση Build",
                () -> {
                    for (int i = 0; i < radios.size(); i++) {
                        if (radios.get(i).isSelected()) {
                            onBuildChosen.accept(options.get(i).build());
                            break;
                        }
                    }
                    return true;
                }
            );
        });
    }

    private List<BudgetOption> generateBudgetOptions(double budget) {
        List<BudgetOption> result = new ArrayList<>();

        Map<Category, Product> balanced = pickBuildWithWeights(budget, 0.22, 0.34, 0.12);
        if (!balanced.isEmpty()) result.add(new BudgetOption("1. Balanced All-Round Build", balanced));

        Map<Category, Product> gaming = pickBuildWithWeights(budget, 0.17, 0.43, 0.11);
        if (!gaming.isEmpty() && !gaming.equals(balanced)) {
            result.add(new BudgetOption("2. Max Gaming FPS Build", gaming));
        }

        Map<Category, Product> value = pickBuildWithWeights(budget * 0.92, 0.26, 0.28, 0.13);
        if (!value.isEmpty() && !value.equals(balanced) && !value.equals(gaming)) {
            result.add(new BudgetOption("3. Best Value / Smart Budget", value));
        }

        return result;
    }

    private Map<Category, Product> pickBuildWithWeights(double budget, double cpuShare, double gpuShare, double moboShare) {
        Map<Category, Product> build = new EnumMap<>(Category.class);
        double remaining = budget;

        Product cpu = bestWithinTarget(Category.CPU, build, budget * cpuShare, remaining);
        if (cpu == null) return Map.of();
        build.put(Category.CPU, cpu);
        remaining -= cpu.getPrice();

        Product mobo = bestWithinTarget(Category.MOTHERBOARD, build, budget * moboShare, remaining);
        if (mobo == null) return Map.of();
        build.put(Category.MOTHERBOARD, mobo);
        remaining -= mobo.getPrice();

        Product gpu = bestWithinTarget(Category.GPU, build, budget * gpuShare, remaining);
        if (gpu != null) {
            build.put(Category.GPU, gpu);
            remaining -= gpu.getPrice();
        }

        Category[] tail = {Category.RAM, Category.STORAGE, Category.COOLER, Category.CASE, Category.PSU};
        for (int i = 0; i < tail.length; i++) {
            Category c = tail[i];
            int slotsLeft = tail.length - i;
            double target = remaining / slotsLeft;
            Product p = bestWithinTarget(c, build, target, remaining);
            if (p != null) {
                build.put(c, p);
                remaining -= p.getPrice();
            }
        }
        return build;
    }

    private Product bestWithinTarget(Category cat, Map<Category, Product> build, double targetPrice, double maxAllowed) {
        List<Product> compat = CompatibilityEngine.getCompatibleProducts(
            cat, productService.getProductsByCategory(cat), build
        );
        if (compat.isEmpty()) return null;

        List<Product> affordable = compat.stream()
            .filter(p -> p.getPrice() <= maxAllowed)
            .sorted(Comparator.comparingDouble(p -> Math.abs(p.getPrice() - targetPrice)))
            .toList();

        if (!affordable.isEmpty()) return affordable.get(0);
        return compat.stream().min(Comparator.comparingDouble(Product::getPrice)).orElse(null);
    }

    private String buildShortSummary(Map<Category, Product> b) {
        List<String> parts = new ArrayList<>();
        for (Category c : buildOrder) {
            if (b.containsKey(c)) parts.add(c.name() + ": " + b.get(c).getName());
        }
        return String.join("  |  ", parts);
    }
}