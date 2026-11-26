package com.omnirig.utils;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.CompatibilityEngine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CatalogFilterBarHelper {

    private static final String SVG_ALL  = "M 3 3 H 10 V 10 H 3 Z M 14 3 H 21 V 10 H 14 Z M 3 14 H 10 V 21 H 3 Z M 14 14 H 21 V 21 H 14 Z";
    private static final String SVG_CPU  = "M 6 6 H 18 V 18 H 6 Z M 9 9 H 15 V 15 H 9 Z M 8 2 V 6 M 12 2 V 6 M 16 2 V 6 M 8 18 V 22 M 12 18 V 22 M 16 18 V 22 M 2 8 H 6 M 2 12 H 6 M 2 16 H 6 M 18 8 H 22 M 18 12 H 22 M 18 16 H 22";
    private static final String SVG_MOBO = "M 3 3 H 21 V 21 H 3 Z M 6 6 H 12 V 12 H 6 Z M 15 6 V 13 M 18 6 V 13 M 6 16 H 15 M 6 19 H 12";
    private static final String SVG_RAM  = "M 2 6 H 22 V 16 H 2 Z M 5 9 H 8 V 13 H 5 Z M 10.5 9 H 13.5 V 13 H 10.5 Z M 16 9 H 19 V 13 H 16 Z M 4 16 V 19 M 6.5 16 V 19 M 9 16 V 19 M 13 16 V 19 M 15.5 16 V 19 M 18 16 V 19 M 20.5 16 V 19";
    private static final String SVG_GPU  = "M 2 6 H 22 V 17 H 2 Z M 8 11.5 m -3 0 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 M 16 11.5 m -3 0 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 M 5 17 V 19 H 13 V 17";
    private static final String SVG_COOL = "M 12 2 V 22 M 9.5 4.5 L 12 7 L 14.5 4.5 M 9.5 19.5 L 12 17 L 14.5 19.5 M 3.3 7 L 20.7 17 M 4.5 10.5 L 7 9.1 L 6.5 6.2 M 19.5 13.5 L 17 14.9 L 17.5 17.8 M 20.7 7 L 3.3 17 M 19.5 10.5 L 17 9.1 L 17.5 6.2 M 4.5 13.5 L 7 14.9 L 6.5 17.8";
    private static final String SVG_SSD  = "M 4 7 H 22 V 17 H 4 Z M 2 9 H 4 M 2 12 H 4 M 2 15 H 4 M 7 10 H 10 V 14 H 7 Z M 12.5 9.5 H 18.5 V 14.5 H 12.5 Z";
    private static final String SVG_CASE = "M 5 2 H 19 V 20 H 5 Z M 7 20 V 22 M 17 20 V 22 M 8 5 H 16 V 13 H 8 Z M 5 16 H 19 M 8 18 H 12 M 15.5 18 H 16";
    private static final String SVG_PSU  = "M 2 5 H 18 V 19 H 2 Z M 11 8 L 7.5 12.5 H 10.5 L 9.5 16 L 13 11.5 H 10 Z M 18 8.5 H 22 M 18 12 H 22 M 18 15.5 H 22";

    private record SpecFilterDef(String specKey, String label, String unit) {}

    private final FlowPane categoryPillsBox;
    private final FlowPane filterBarBox;
    private final Consumer<Category> onCategoryChanged;
    private final Runnable onFiltersChanged;

    private final List<Button> pillButtons = new ArrayList<>();
    private final Map<String, Set<String>> selectedSpecValues = new LinkedHashMap<>();
    private final Map<String, MenuButton> specMenuButtons = new LinkedHashMap<>();

    private Button toggleFiltersBtn;
    private boolean filtersOpen = false;

    private TextField minPriceField;
    private TextField maxPriceField;
    private CheckBox inStockOnlyCheck;
    private ComboBox<String> sortCombo;

    public CatalogFilterBarHelper(
            FlowPane categoryPillsBox,
            FlowPane filterBarBox,
            Consumer<Category> onCategoryChanged,
            Runnable onFiltersChanged) {
        this.categoryPillsBox = categoryPillsBox;
        this.filterBarBox = filterBarBox;
        this.onCategoryChanged = onCategoryChanged;
        this.onFiltersChanged = onFiltersChanged;

        // Τα φίλτρα ξεκινούν κλειστά
        this.filterBarBox.setVisible(false);
        this.filterBarBox.setManaged(false);
    }

    public void buildCategoryPills() {
        categoryPillsBox.getChildren().clear();
        pillButtons.clear();

        Button allPill = createPill("Όλα τα Προϊόντα", null, SVG_ALL);
        setPillActive(allPill, true);
        categoryPillsBox.getChildren().add(allPill);

        for (Category cat : Category.values()) {
            categoryPillsBox.getChildren().add(createPill(shortCategoryName(cat), cat, svgFor(cat)));
        }

        // Κουμπί που ανοίγει/κλείνει το panel των φίλτρων
        toggleFiltersBtn = new Button("⚙  Φίλτρα");
        toggleFiltersBtn.setPadding(new Insets(7, 14, 7, 14));
        styleToggleFiltersBtn();
        toggleFiltersBtn.setOnAction(e -> {
            filtersOpen = !filtersOpen;
            filterBarBox.setVisible(filtersOpen);
            filterBarBox.setManaged(filtersOpen);
            styleToggleFiltersBtn();
        });
        categoryPillsBox.getChildren().add(toggleFiltersBtn);
    }

    public void rebuildFiltersFor(Category category, List<Product> categoryProducts) {
        filterBarBox.getChildren().clear();
        selectedSpecValues.clear();
        specMenuButtons.clear();

        // 1. Ταξινόμηση
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll(
            "Ταξινόμηση: Προτεινόμενα",
            "Τιμή: Χαμηλή → Υψηλή",
            "Τιμή: Υψηλή → Χαμηλή",
            "Όνομα: A → Z"
        );
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8;");
        sortCombo.setOnAction(e -> onFiltersChanged.run());
        filterBarBox.getChildren().add(sortCombo);

        // 2. Εύρος Τιμής (Min - Max)
        minPriceField = new TextField();
        minPriceField.setPromptText("Από €");
        minPriceField.setPrefWidth(78);
        minPriceField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8;");
        minPriceField.textProperty().addListener((o, ov, nv) -> { updateFilterBadge(); onFiltersChanged.run(); });

        maxPriceField = new TextField();
        maxPriceField.setPromptText("Έως €");
        maxPriceField.setPrefWidth(78);
        maxPriceField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8;");
        maxPriceField.textProperty().addListener((o, ov, nv) -> { updateFilterBadge(); onFiltersChanged.run(); });

        filterBarBox.getChildren().addAll(minPriceField, maxPriceField);

        // 3. Multi-select φίλτρα specs με τίτλο & μονάδες μέτρησης
        if (category != null) {
            for (SpecFilterDef def : getSpecFiltersFor(category)) {
                List<String> distinctValues = categoryProducts.stream()
                    .map(p -> CompatibilityEngine.getString(p, def.specKey()))
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted(Comparator.comparing(this::numericSortKey).thenComparing(String::compareToIgnoreCase))
                    .toList();

                if (distinctValues.isEmpty()) continue;

                Set<String> chosenSet = new LinkedHashSet<>();
                selectedSpecValues.put(def.specKey(), chosenSet);

                MenuButton mb = new MenuButton(def.label());
                mb.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-text-fill: #334155;");

                for (String rawVal : distinctValues) {
                    String displayVal = def.unit().isEmpty() ? rawVal : rawVal + " " + def.unit();
                    CheckBox cb = new CheckBox(displayVal);
                    cb.setStyle("-fx-text-fill: #0F172A; -fx-padding: 3 8;");
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) chosenSet.add(rawVal);
                        else chosenSet.remove(rawVal);

                        if (chosenSet.isEmpty()) {
                            mb.setText(def.label());
                        } else if (chosenSet.size() == 1) {
                            String only = chosenSet.iterator().next();
                            mb.setText(def.label() + ": " + (def.unit().isEmpty() ? only : only + " " + def.unit()));
                        } else {
                            mb.setText(def.label() + " (" + chosenSet.size() + ")");
                        }
                        updateFilterBadge();
                        onFiltersChanged.run();
                    });

                    CustomMenuItem item = new CustomMenuItem(cb);
                    item.setHideOnClick(false); // Επιτρέπει πολλαπλές επιλογές χωρίς να κλείνει
                    mb.getItems().add(item);
                }

                specMenuButtons.put(def.specKey(), mb);
                filterBarBox.getChildren().add(mb);
            }
        }

        // 4. Μόνο Διαθέσιμα
        inStockOnlyCheck = new CheckBox("Μόνο διαθέσιμα");
        inStockOnlyCheck.setStyle("-fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 12px;");
        inStockOnlyCheck.setOnAction(e -> { updateFilterBadge(); onFiltersChanged.run(); });
        filterBarBox.getChildren().add(inStockOnlyCheck);

        // 5. Καθαρισμός
        Button resetBtn = new Button("Καθαρισμός");
        resetBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> {
            rebuildFiltersFor(category, categoryProducts);
            updateFilterBadge();
            onFiltersChanged.run();
        });
        filterBarBox.getChildren().add(resetBtn);

        updateFilterBadge();
    }

    public List<Product> applyFiltersAndSort(List<Product> baseProducts) {
        double minP = parseDoubleOr(minPriceField != null ? minPriceField.getText() : "", 0.0);
        double maxP = parseDoubleOr(maxPriceField != null ? maxPriceField.getText() : "", Double.MAX_VALUE);
        boolean onlyStock = inStockOnlyCheck != null && inStockOnlyCheck.isSelected();

        List<Product> filtered = baseProducts.stream()
            .filter(p -> p.getPrice() >= minP && p.getPrice() <= maxP)
            .filter(p -> !onlyStock || p.getStock() > 0)
            .filter(p -> {
                for (Map.Entry<String, Set<String>> entry : selectedSpecValues.entrySet()) {
                    Set<String> allowed = entry.getValue();
                    if (!allowed.isEmpty()) {
                        String actual = CompatibilityEngine.getString(p, entry.getKey());
                        boolean match = allowed.stream().anyMatch(v -> v.equalsIgnoreCase(actual));
                        if (!match) return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        if (sortCombo != null && sortCombo.getValue() != null) {
            switch (sortCombo.getValue()) {
                case "Τιμή: Χαμηλή → Υψηλή" -> filtered.sort(Comparator.comparingDouble(Product::getPrice));
                case "Τιμή: Υψηλή → Χαμηλή" -> filtered.sort(Comparator.comparingDouble(Product::getPrice).reversed());
                case "Όνομα: A → Z"         -> filtered.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
            }
        }
        return filtered;
    }

    private void updateFilterBadge() {
        if (toggleFiltersBtn == null) return;
        int count = 0;
        if (minPriceField != null && !minPriceField.getText().isBlank()) count++;
        if (maxPriceField != null && !maxPriceField.getText().isBlank()) count++;
        if (inStockOnlyCheck != null && inStockOnlyCheck.isSelected()) count++;
        for (Set<String> s : selectedSpecValues.values()) {
            if (!s.isEmpty()) count++;
        }
        toggleFiltersBtn.setText(count > 0 ? "⚙  Φίλτρα (" + count + ")" : "⚙  Φίλτρα");
        styleToggleFiltersBtn();
    }

    private void styleToggleFiltersBtn() {
        if (toggleFiltersBtn == null) return;
        if (filtersOpen) {
            toggleFiltersBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-border-color: #2563EB; -fx-border-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        } else {
            toggleFiltersBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-border-color: #BFDBFE; -fx-border-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        }
    }

    private List<SpecFilterDef> getSpecFiltersFor(Category cat) {
        return switch (cat) {
            case CPU         -> List.of(
                new SpecFilterDef("socket", "Socket", ""),
                new SpecFilterDef("cores", "Πυρήνες", "Cores"),
                new SpecFilterDef("cache_l3_mb", "L3 Cache", "MB")
            );
            case MOTHERBOARD -> List.of(
                new SpecFilterDef("socket", "Socket", ""),
                new SpecFilterDef("form_factor", "Form Factor", ""),
                new SpecFilterDef("memory_type", "RAM", "")
            );
            case RAM         -> List.of(
                new SpecFilterDef("type", "Τύπος", ""),
                new SpecFilterDef("capacity_gb", "Χωρητικότητα", "GB"),
                new SpecFilterDef("speed_mhz", "Ταχύτητα", "MHz")
            );
            case GPU         -> List.of(
                new SpecFilterDef("chipset_brand", "Brand", ""),
                new SpecFilterDef("vram_gb", "VRAM", "GB")
            );
            case COOLER      -> List.of(
                new SpecFilterDef("type", "Τύπος", ""),
                new SpecFilterDef("max_tdp_w", "Max TDP", "W")
            );
            case STORAGE     -> List.of(
                new SpecFilterDef("interface", "Interface", ""),
                new SpecFilterDef("capacity_gb", "Χωρητικότητα", "GB")
            );
            case CASE        -> List.of(
                new SpecFilterDef("case_type", "Τύπος", "")
            );
            case PSU         -> List.of(
                new SpecFilterDef("wattage", "Ισχύς", "W"),
                new SpecFilterDef("efficiency_rating", "80+ Rating", "")
            );
        };
    }

    private Button createPill(String label, Category cat, String svgData) {
        Button btn = new Button(label);
        btn.setGraphic(createVectorIcon(svgData));
        btn.setGraphicTextGap(8);
        btn.setPadding(new Insets(7, 14, 7, 12));
        setPillActive(btn, false);

        btn.setOnAction(e -> {
            pillButtons.forEach(b -> setPillActive(b, false));
            setPillActive(btn, true);
            onCategoryChanged.accept(cat);
        });

        pillButtons.add(btn);
        return btn;
    }

    private void setPillActive(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-border-color: #0F172A; -fx-border-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        } else {
            btn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        }
    }

    private String shortCategoryName(Category cat) {
        return switch (cat) {
            case CPU         -> "CPU";
            case MOTHERBOARD -> "Motherboard";
            case RAM         -> "RAM";
            case GPU         -> "GPU";
            case COOLER      -> "Ψύξη";
            case STORAGE     -> "SSD";
            case CASE        -> "Κουτιά";
            case PSU         -> "Τροφοδοτικά";
        };
    }

    private String svgFor(Category cat) {
        return switch (cat) {
            case CPU         -> SVG_CPU;
            case MOTHERBOARD -> SVG_MOBO;
            case RAM         -> SVG_RAM;
            case GPU         -> SVG_GPU;
            case COOLER      -> SVG_COOL;
            case STORAGE     -> SVG_SSD;
            case CASE        -> SVG_CASE;
            case PSU         -> SVG_PSU;
        };
    }

    private Node createVectorIcon(String svgPathData) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgPathData);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#64748B"));
        svg.setStrokeWidth(1.8);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.setScaleX(0.75);
        svg.setScaleY(0.75);

        StackPane box = new StackPane(new Group(svg));
        box.setPrefSize(18, 18);
        box.setMinSize(18, 18);
        box.setMaxSize(18, 18);
        return box;
    }

    private double numericSortKey(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return Double.MAX_VALUE; }
    }

    private double parseDoubleOr(String s, double def) {
        if (s == null || s.isBlank()) return def;
        try { return Double.parseDouble(s.replace(",", ".").trim()); }
        catch (Exception e) { return def; }
    }
}