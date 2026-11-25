package com.omnirig.controllers.builder;

import com.omnirig.models.Category;
import com.omnirig.models.Product;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

import static com.omnirig.services.CompatibilityEngine.getInt;
import static com.omnirig.services.CompatibilityEngine.getString;

public final class PcBuilderPickerDialog {

    private PcBuilderPickerDialog() {}

    public static Optional<Product> show(Category cat, List<Product> compatible) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Επιλογή: " + cat.getDisplayName());
        dialog.setHeaderText("Συμβατά εξαρτήματα (" + compatible.size() + ") — Ταξινόμηση & Specs");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll(getSortOptionsForCategory(cat));
        sortBox.getSelectionModel().selectFirst();

        ListView<Product> listView = new ListView<>();
        listView.setPrefSize(560, 340);

        Runnable applySort = () -> {
            List<Product> sorted = new ArrayList<>(compatible);
            sorted.sort(comparatorFor(sortBox.getValue()));
            listView.getItems().setAll(sorted);
        };

        sortBox.setOnAction(e -> applySort.run());
        applySort.run();

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox h = new HBox(12);
                    h.setAlignment(Pos.CENTER_LEFT);
                    VBox info = new VBox(2);
                    Label n = new Label(item.getName());
                    n.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
                    Label s = new Label(shortSpecsLine(item));
                    s.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    info.getChildren().addAll(n, s);

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    Label pr = new Label(String.format("%.2f €", item.getPrice()));
                    pr.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB;");

                    h.getChildren().addAll(info, sp, pr);
                    setGraphic(h);
                }
            }
        });

        HBox topBar = new HBox(10, new Label("Ταξινόμηση κατά:"), sortBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 8, 0));

        VBox content = new VBox(6, topBar, listView);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);

        return dialog.showAndWait();
    }

    private static List<String> getSortOptionsForCategory(Category cat) {
        List<String> base = new ArrayList<>(List.of("Τιμή (Χαμηλή → Υψηλή)", "Τιμή (Υψηλή → Χαμηλή)"));
        switch (cat) {
            case CPU -> {
                base.add("Socket (AM4 / AM5 / LGA...)");
                base.add("Πυρήνες (Περισσότεροι → Λιγότεροι)");
            }
            case GPU -> {
                base.add("VRAM (Περισσότερη → Λιγότερη)");
                base.add("Κατανάλωση TDP (Χαμηλή → Υψηλή)");
            }
            case RAM -> {
                base.add("Χωρητικότητα GB (Μεγάλη → Μικρή)");
                base.add("Ταχύτητα MHz (Υψηλή → Χαμηλή)");
            }
            case MOTHERBOARD -> {
                base.add("Form Factor (ATX / Micro-ATX / Mini-ITX)");
                base.add("Socket");
            }
            case COOLER -> {
                base.add("Max TDP (Υψηλότερο → Χαμηλότερο)");
                base.add("Ύψος mm (Μικρότερο → Μεγαλύτερο)");
            }
            case CASE -> {
                base.add("Max GPU Length (Μεγαλύτερο → Μικρότερο)");
                base.add("Τύπος Κουτιού");
            }
            case PSU -> base.add("Wattage (Περισσότερα → Λιγότερα)");
            case STORAGE -> {
                base.add("Χωρητικότητα GB (Μεγάλη → Μικρή)");
                base.add("Ταχύτητα Read MB/s (Υψηλή → Χαμηλή)");
            }
        }
        return base;
    }

    private static Comparator<Product> comparatorFor(String criterion) {
        if (criterion == null) return Comparator.comparingDouble(Product::getPrice);
        return switch (criterion) {
            case "Τιμή (Υψηλή → Χαμηλή)" -> Comparator.comparingDouble(Product::getPrice).reversed();
            case "Socket (AM4 / AM5 / LGA...)", "Socket" ->
                Comparator.comparing((Product p) -> getString(p, "socket")).thenComparingDouble(Product::getPrice);
            case "Πυρήνες (Περισσότεροι → Λιγότεροι)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "cores", 0)).reversed();
            case "VRAM (Περισσότερη → Λιγότερη)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "vram_gb", 0)).reversed();
            case "Κατανάλωση TDP (Χαμηλή → Υψηλή)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "tdp_w", 0));
            case "Χωρητικότητα GB (Μεγάλη → Μικρή)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "capacity_gb", 0)).reversed();
            case "Ταχύτητα MHz (Υψηλή → Χαμηλή)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "speed_mhz", 0)).reversed();
            case "Form Factor (ATX / Micro-ATX / Mini-ITX)" ->
                Comparator.comparing((Product p) -> getString(p, "form_factor")).thenComparingDouble(Product::getPrice);
            case "Max TDP (Υψηλότερο → Χαμηλότερο)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "max_tdp_w", 0)).reversed();
            case "Ύψος mm (Μικρότερο → Μεγαλύτερο)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "height_mm", 0));
            case "Max GPU Length (Μεγαλύτερο → Μικρότερο)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "max_gpu_length_mm", 0)).reversed();
            case "Τύπος Κουτιού" ->
                Comparator.comparing((Product p) -> getString(p, "case_type"));
            case "Wattage (Περισσότερα → Λιγότερα)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "wattage", 0)).reversed();
            case "Ταχύτητα Read MB/s (Υψηλή → Χαμηλή)" ->
                Comparator.comparingInt((Product p) -> getInt(p, "read_speed_mb_s", 0)).reversed();
            default -> Comparator.comparingDouble(Product::getPrice);
        };
    }

    private static String shortSpecsLine(Product p) {
        return switch (p.getCategory()) {
            case CPU -> "Socket: " + getString(p, "socket") + " • " + getInt(p, "cores", 0) + "C/" + getInt(p, "threads", 0) + "T • " + getInt(p, "tdp_max_w", 65) + "W";
            case GPU -> getInt(p, "vram_gb", 0) + "GB " + getString(p, "vram_type") + " • " + getInt(p, "length_mm", 0) + "mm • " + getInt(p, "tdp_w", 0) + "W";
            case RAM -> getString(p, "type") + " • " + getInt(p, "capacity_gb", 0) + "GB • " + getInt(p, "speed_mhz", 0) + "MHz";
            case MOTHERBOARD -> getString(p, "socket") + " • " + getString(p, "form_factor") + " • " + getString(p, "memory_type");
            case COOLER -> getString(p, "type") + " • Max " + getInt(p, "max_tdp_w", 0) + "W • " + getInt(p, "height_mm", 0) + "mm";
            case CASE -> getString(p, "case_type") + " • Max GPU " + getInt(p, "max_gpu_length_mm", 0) + "mm";
            case PSU -> getInt(p, "wattage", 0) + "W • " + getString(p, "efficiency_rating") + " • " + getString(p, "form_factor");
            case STORAGE -> getInt(p, "capacity_gb", 0) + "GB • " + getString(p, "interface") + " • " + getInt(p, "read_speed_mb_s", 0) + " MB/s";
        };
    }
}