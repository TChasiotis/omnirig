package com.omnirig.controllers;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.MockProductService;
import com.omnirig.services.ProductService;
import com.omnirig.services.IconResolver;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.InputStream;

public class CustomerDashboardController {

    @FXML private TextField searchNameField;
    @FXML private VBox categoryBox;
    @FXML private FlowPane productGrid;
    @FXML private Label sectionTitle;
    @FXML private Button cartButton;

    private final ProductService productService = new MockProductService();
    private Category selectedCategory = null;
    private int cartCount = 0;

    @FXML
    public void initialize() {
        buildCategorySidebar();
        renderProducts(productService.getAllProducts());
    }

    private void buildCategorySidebar() {
        Button allBtn = createCategoryButton("Όλα τα Προϊόντα", null);
        allBtn.getStyleClass().add("cat-btn-active");
        categoryBox.getChildren().add(allBtn);

        for (Category cat : Category.values()) {
            categoryBox.getChildren().add(createCategoryButton(cat.getDisplayName(), cat));
        }
    }

    private Button createCategoryButton(String label, Category category) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("cat-btn");
        btn.setOnAction(e -> {
            selectedCategory = category;
            sectionTitle.setText(label);
            categoryBox.getChildren().forEach(node -> node.getStyleClass().remove("cat-btn-active"));
            btn.getStyleClass().add("cat-btn-active");
            handleSearch();
        });
        return btn;
    }

    private void renderProducts(List<Product> products) {
        productGrid.getChildren().clear();
        if (products.isEmpty()) {
            Label empty = new Label("Δεν βρέθηκαν προϊόντα.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 15px;");
            productGrid.getChildren().add(empty);
            return;
        }
        for (Product p : products) {
            productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setMinHeight(210);
        card.getStyleClass().add("product-card");

        // Badge κατηγορίας
        Label catBadge = new Label(p.getCategory().name());
        catBadge.getStyleClass().add("card-category");

        StackPane imageBox = createProductImageBox(p, 140);
        // Τίτλος
        Label title = new Label(p.getName());
        title.setWrapText(true);
        title.setPrefHeight(42);
        title.getStyleClass().add("card-title");

        // 2 βασικά specs για preview
        String previewSpecs = p.getSpecs().entrySet().stream()
            .limit(2)
            .map(entry -> formatKey(entry.getKey()) + ": " + entry.getValue())
            .reduce((a, b) -> a + "  •  " + b)
            .orElse("");
        Label specsLbl = new Label(previewSpecs);
        specsLbl.setWrapText(true);
        specsLbl.getStyleClass().add("card-specs");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Τιμή + Διαθεσιμότητα
        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLbl = new Label(String.format("%.2f €", p.getPrice()));
        priceLbl.getStyleClass().add("card-price");
        Region midSpacer = new Region();
        HBox.setHgrow(midSpacer, Priority.ALWAYS);
        Label stockLbl = new Label(p.getStock() > 0 ? "Διαθέσιμο (" + p.getStock() + ")" : "Εξαντλήθηκε");
        stockLbl.getStyleClass().add(p.getStock() > 0 ? "badge-instock" : "badge-outstock");
        priceRow.getChildren().addAll(priceLbl, midSpacer, stockLbl);

        // Κουμπί προσθήκης στο καλάθι
        Button addBtn = new Button("Προσθήκη στο καλάθι");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setDisable(p.getStock() == 0);
        addBtn.setOnAction(e -> {
            e.consume();
            cartCount++;
            cartButton.setText("Καλάθι (" + cartCount + ")");
        });

        card.getChildren().addAll(catBadge, imageBox, title, specsLbl, spacer, priceRow, addBtn);

        // Κλικ στην κάρτα -> Άνοιγμα αναλυτικών specs
        card.setOnMouseClicked(e -> showProductDetails(p));

        return card;
    }
    private StackPane createProductImageBox(Product p, double size) {
    StackPane box = new StackPane();
    box.setPrefHeight(size);
    box.setMaxHeight(size);
    box.setAlignment(Pos.CENTER);
    box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-padding: 8;");

    String path = IconResolver.getIconPath(p);
    InputStream is = getClass().getResourceAsStream(path);

    if (is != null) {
        ImageView imageView = new ImageView(new Image(is));
        imageView.setFitWidth(size - 16);
        imageView.setFitHeight(size - 16);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        box.getChildren().add(imageView);
    } else {
        Label placeholder = new Label("No Icon");
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        box.getChildren().add(placeholder);
    }

    return box;
}

    private void showProductDetails(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(p.getName());

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label cat = new Label(p.getCategory().getDisplayName());
        cat.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label title = new Label(p.getName());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label price = new Label(String.format("%.2f €   |   Απόθεμα: %d τεμ.", p.getPrice(), p.getStock()));
        price.setStyle("-fx-font-size: 15px; -fx-text-fill: #334155;");

        // Πίνακας όλων των specs
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int row = 0;
        for (Map.Entry<String, Object> entry : p.getSpecs().entrySet()) {
            Label k = new Label(formatKey(entry.getKey()));
            k.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
            Label v = new Label(String.valueOf(entry.getValue()));
            v.setStyle("-fx-text-fill: #0F172A;");
            grid.add(k, 0, row);
            grid.add(v, 1, row);
            row++;
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(340);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #FFFFFF;");

        Button closeBtn = new Button("Κλείσιμο");
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");

        root.getChildren().addAll(cat, title, price, new Separator(), scroll, closeBtn);
        dialog.setScene(new Scene(root, 480, 560));
        dialog.showAndWait();
    }

    private String formatKey(String key) {
        String spaced = key.replace("_", " ");
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    @FXML
    public void handleSearch() {
        String keyword = searchNameField.getText();
        renderProducts(productService.searchProducts(keyword, null, selectedCategory));
    }

    @FXML
    public void handleOpenPcBuilder() {
        System.out.println("Άνοιγμα PC Builder (επόμενο βήμα!)");
    }

    @FXML
    public void handleLogin() {
        switchScene("/com/omnirig/views/Login.fxml", "OmniRig - Σύνδεση");
    }

    @FXML
    public void handleRegister() {
        System.out.println("Πάτησε Εγγραφή");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) searchNameField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1200, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}