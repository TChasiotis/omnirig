package com.omnirig.controllers;

import com.omnirig.controllers.builder.PcBuilderBudgetGenerator;
import com.omnirig.controllers.builder.PcBuilderPickerDialog;
import com.omnirig.controllers.builder.PcBuilderReportView;
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
    private PcBuilderBudgetGenerator budgetGenerator;
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
        this.budgetGenerator = new PcBuilderBudgetGenerator(productService, BUILD_ORDER);
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
        reportContainer.getChildren().setAll(
            PcBuilderReportView.build(lastReport, activeResolution, res -> {
                activeResolution = res;
                renderReport();
            })
        );
        reportContainer.setVisible(true);
        reportContainer.setManaged(true);
        scrollPane.setVvalue(0);
    }

    private void hideReport() {
        lastReport = null;
        reportContainer.getChildren().clear();
        reportContainer.setVisible(false);
        reportContainer.setManaged(false);
    }

    @FXML
    private void handleBudgetAutoBuild() {
        budgetGenerator.showBudgetFlow(rootStack, chosenBuild -> {
            currentBuild.clear();
            currentBuild.putAll(chosenBuild);
            hideReport();
            refreshSlots();
        });
    }

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
                pickBtn.setOnAction(e -> PcBuilderPickerDialog.show(cat, compatible).ifPresent(chosen -> {
                    currentBuild.put(cat, chosen);
                    hideReport();
                    refreshSlots();
                }));

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
}