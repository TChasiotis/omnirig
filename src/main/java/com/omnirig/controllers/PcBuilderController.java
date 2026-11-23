package com.omnirig.controllers;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.BuildAnalyzer;
import com.omnirig.services.BuildAnalyzer.Resolution;
import com.omnirig.services.CompatibilityEngine;
import com.omnirig.services.ProductService;
import com.omnirig.utils.ModalOverlayHelper;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.omnirig.services.CompatibilityEngine.*;

public class PcBuilderController {

    @FXML private ScrollPane scrollPane;
    @FXML private Label totalPriceLbl;
    @FXML private Label totalWattLbl;
    @FXML private Button saveBuildBtn;
    @FXML private Button analyzeBtn;
    @FXML private Button addAllBtn;
    @FXML private VBox reportContainer;
    @FXML private VBox slotsContainer;

    private StackPane rootStack;
    private ProductService productService;
    private Runnable onBack;
    private Consumer<List<Product>> onAddAllToCart;
    private BiConsumer<String, Map<Category, Product>> onSaveBuild;

    private final Map<Category, Product> currentBuild = new EnumMap<>(Category.class);
    private BuildAnalyzer.Report lastReport;
    private Resolution activeResolution = Resolution.FHD_1080P;

    private static final Category[] BUILD_ORDER = {
        Category.CPU, Category.MOTHERBOARD, Category.RAM, Category.GPU,
        Category.COOLER, Category.STORAGE, Category.CASE, Category.PSU
    };

    public void init(
            StackPane rootStack,
            ProductService productService,
            Runnable onBack,
            Consumer<List<Product>> onAddAllToCart,
            BiConsumer<String, Map<Category, Product>> onSaveBuild) {
        this.rootStack = rootStack;
        this.productService = productService;
        this.onBack = onBack;
        this.onAddAllToCart = onAddAllToCart;
        this.onSaveBuild = onSaveBuild;
        refreshSlots();
    }

    public void loadBuild(Map<Category, Product> parts) {
        currentBuild.clear();
        if (parts != null) currentBuild.putAll(parts);
        hideReport();
        refreshSlots();
    }

    @FXML
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void handleClear() {
        currentBuild.clear();
        lastReport = null;
        hideReport();
        refreshSlots();
    }

    @FXML
    private void handleSaveBuild() {
        if (currentBuild.isEmpty() || onSaveBuild == null) return;

        TextField nameField = new TextField("Το PC Build μου");
        nameField.setPromptText("π.χ. Gaming Rig 1440p");
        nameField.setStyle("-fx-font-size: 14px; -fx-padding: 9 12; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-radius: 8;");

        Label errLbl = new Label("Δώσε ένα όνομα για το build.");
        errLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        errLbl.setVisible(false);
        errLbl.setManaged(false);

        VBox content = new VBox(8, nameField, errLbl);

        ModalOverlayHelper.showCustomModal(
            rootStack,
            "Αποθήκευση PC Build",
            "Δώσε ένα όνομα για να το βρεις αργότερα στον λογαριασμό σου.",
            content,
            "Αποθήκευση",
            () -> {
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                if (name.isEmpty()) {
                    errLbl.setVisible(true);
                    errLbl.setManaged(true);
                    return false;
                }
                onSaveBuild.accept(name, new EnumMap<>(currentBuild));
                return true;
            }
        );
    }

    @FXML
    private void handleAddAllToCart() {
        if (onAddAllToCart != null && !currentBuild.isEmpty()) {
            onAddAllToCart.accept(List.copyOf(currentBuild.values()));
        }
    }

    @FXML
    private void handleAnalyze() {
        lastReport = BuildAnalyzer.analyze(currentBuild);
        if (activeResolution == Resolution.UHD_4K && !lastReport.supports4K()) {
            activeResolution = Resolution.FHD_1080P;
        }
        renderReport();
    }

    private void renderReport() {
        if (lastReport == null) return;
        reportContainer.getChildren().clear();

        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #10B981; -fx-border-width: 1.5; -fx-border-radius: 14;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("Γνώμη Τεχνικού OmniRig");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox resSwitch = new HBox(6);
        resSwitch.setAlignment(Pos.CENTER_RIGHT);
        resSwitch.getChildren().add(createResButton(Resolution.FHD_1080P));
        resSwitch.getChildren().add(createResButton(Resolution.QHD_1440P));
        if (lastReport.supports4K()) {
            resSwitch.getChildren().add(createResButton(Resolution.UHD_4K));
        }

        topRow.getChildren().addAll(header, sp, resSwitch);

        HBox scores = new HBox(18,
            createScorePill("Performance", lastReport.performanceScore() + " / 10", "#2563EB"),
            createScorePill("Value / Money", lastReport.valueScore() + " / 10", "#10B981"),
            createScorePill("Balance", lastReport.balanceScore() + " / 10", "#8B5CF6")
        );

        Label verdict = new Label("Bottleneck Check: " + lastReport.bottleneckVerdict());
        verdict.setWrapText(true);
        verdict.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");

        BuildAnalyzer.FpsSet fps = lastReport.getFpsFor(activeResolution);
        String resTag = activeResolution.getLabel();

        HBox fpsRow = new HBox(18,
            createFpsBox("Fortnite (" + resTag + ")", fps.fortnite() + " FPS"),
            createFpsBox("Cyberpunk 2077 (" + resTag + ")", fps.cyberpunk() + " FPS"),
            createFpsBox("Red Dead Redemption 2 (" + resTag + ")", fps.rdr2() + " FPS")
        );

        VBox notesBox = new VBox(6);
        for (String note : lastReport.technicianNotes()) {
            Label nl = new Label("•  " + note);
            nl.setWrapText(true);
            nl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            notesBox.getChildren().add(nl);
        }

        card.getChildren().addAll(topRow, scores, verdict, new Separator(), fpsRow, new Separator(), notesBox);
        reportContainer.getChildren().add(card);
        reportContainer.setVisible(true);
        reportContainer.setManaged(true);
        scrollPane.setVvalue(0);
    }

    private Button createResButton(Resolution res) {
        boolean selected = activeResolution == res;
        Button btn = new Button(res.getLabel());
        btn.setStyle(selected
            ? "-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 7; -fx-cursor: hand;"
            : "-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 7; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            activeResolution = res;
            renderReport();
        });
        return btn;
    }

    private void hideReport() {
        lastReport = null;
        reportContainer.getChildren().clear();
        reportContainer.setVisible(false);
        reportContainer.setManaged(false);
    }

    // --- AUTO-BUILD BY BUDGET (IN-WINDOW MODALS) ---
    @FXML
    private void handleBudgetAutoBuild() {
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
                            currentBuild.clear();
                            currentBuild.putAll(options.get(i).build());
                            hideReport();
                            refreshSlots();
                            break;
                        }
                    }
                    return true;
                }
            );
        });
    }

    private record BudgetOption(String title, Map<Category, Product> build) {}

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
        for (Category c : BUILD_ORDER) {
            if (b.containsKey(c)) parts.add(c.name() + ": " + b.get(c).getName());
        }
        return String.join("  |  ", parts);
    }

    // --- SLOTS & PICKER ---
    private void refreshSlots() {
        slotsContainer.getChildren().clear();

        for (Category cat : BUILD_ORDER) {
            Product selected = currentBuild.get(cat);
            List<Product> compatible = CompatibilityEngine.getCompatibleProducts(
                cat, productService.getProductsByCategory(cat), currentBuild
            );

            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 18, 14, 18));
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-border-color: "
                + (selected != null ? "#2563EB" : "#E2E8F0") + "; -fx-border-radius: 12;");

            Label catLbl = new Label(cat.getDisplayName());
            catLbl.setPrefWidth(195);
            catLbl.setMinWidth(195);
            catLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 14px;");

            if (selected != null) {
                Label nameLbl = new Label(selected.getName());
                nameLbl.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 14px; -fx-font-weight: bold;");
                HBox.setHgrow(nameLbl, Priority.ALWAYS);

                Label priceLbl = new Label(String.format("%.2f €", selected.getPrice()));
                priceLbl.setMinWidth(Region.USE_PREF_SIZE);
                priceLbl.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 15px; -fx-font-weight: bold;");

                Button removeBtn = new Button("✕");
                removeBtn.setMinWidth(Region.USE_PREF_SIZE);
                removeBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    currentBuild.remove(cat);
                    hideReport();
                    refreshSlots();
                });

                row.getChildren().addAll(catLbl, nameLbl, priceLbl, removeBtn);
            } else {
                Label statusLbl = new Label(compatible.isEmpty()
                    ? "Κανένα συμβατό εξάρτημα με τις τρέχουσες επιλογές"
                    : "Διαθέσιμα συμβατά: " + compatible.size());
                statusLbl.setStyle("-fx-text-fill: " + (compatible.isEmpty() ? "#DC2626" : "#64748B") + "; -fx-font-size: 13px;");
                HBox.setHgrow(statusLbl, Priority.ALWAYS);

                Button pickBtn = new Button("+ Επιλογή");
                pickBtn.setMinWidth(Region.USE_PREF_SIZE);
                pickBtn.getStyleClass().add("btn-primary");
                pickBtn.setDisable(compatible.isEmpty());
                pickBtn.setOnAction(e -> openPickerDialog(cat, compatible));

                row.getChildren().addAll(catLbl, statusLbl, pickBtn);
            }

            slotsContainer.getChildren().add(row);
        }

        double total = currentBuild.values().stream().mapToDouble(Product::getPrice).sum();
        int watts = CompatibilityEngine.estimateSystemWattage(currentBuild);
        totalPriceLbl.setText(String.format("%.2f €", total));
        totalWattLbl.setText("~" + watts + " W");

        boolean empty = currentBuild.isEmpty();
        if (saveBuildBtn != null) saveBuildBtn.setDisable(empty);
        analyzeBtn.setDisable(currentBuild.size() < BUILD_ORDER.length);
        addAllBtn.setDisable(empty);
    }

    private void openPickerDialog(Category cat, List<Product> compatible) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Επιλογή: " + cat.getDisplayName());
        dialog.setHeaderText("Συμβατά εξαρτήματα (" + compatible.size() + ") — Ταξινόμηση & Specs");

        List<String> sortOptions = getSortOptionsForCategory(cat);
        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll(sortOptions);
        sortBox.getSelectionModel().selectFirst();

        ListView<Product> listView = new ListView<>();
        listView.setPrefSize(560, 340);

        Runnable applySort = () -> {
            List<Product> sorted = new ArrayList<>(compatible);
            sorted.sort(comparatorFor(cat, sortBox.getValue()));
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

        dialog.showAndWait().ifPresent(chosen -> {
            currentBuild.put(cat, chosen);
            hideReport();
            refreshSlots();
        });
    }

    private List<String> getSortOptionsForCategory(Category cat) {
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

    private Comparator<Product> comparatorFor(Category cat, String criterion) {
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

    private String shortSpecsLine(Product p) {
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

    private VBox createScorePill(String label, String value, String color) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10, 18, 10, 18));
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        box.getChildren().addAll(l, v);
        return box;
    }

    private VBox createFpsBox(String game, String fps) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 10;");
        Label g = new Label(game);
        g.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        Label f = new Label(fps);
        f.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().addAll(g, f);
        return box;
    }
}