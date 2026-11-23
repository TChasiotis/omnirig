package com.omnirig.controllers;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.IconResolver;
import com.omnirig.services.MockProductService;
import com.omnirig.services.ProductService;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;

public class CustomerDashboardController {

    @FXML private BorderPane mainPane;
    @FXML private ScrollPane catalogScroll;
    @FXML private HBox topBar;
    @FXML private Button btnMinimize;
    @FXML private Button btnMaximize;
    @FXML private Button btnClose;

    @FXML private TextField searchNameField;
    @FXML private VBox categoryBox;
    @FXML private FlowPane productGrid;
    @FXML private Label sectionTitle;
    @FXML private Button cartButton;

    @FXML private void handleLogoClick() {
        showCatalogView();
    }

    private final ProductService productService = new MockProductService();
    private Category selectedCategory = null;
    private int cartCount = 0;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double prevX, prevY, prevW, prevH;

    private static final String ICON_MINIMIZE = "M 0 5 L 10 5";
    private static final String ICON_MAXIMIZE = "M 1 1 L 9 1 L 9 9 L 1 9 Z";
    private static final String ICON_RESTORE  = "M 3 1 L 9 1 L 9 7 M 1 3 L 7 3 L 7 9 L 1 9 Z";
    private static final String ICON_CLOSE    = "M 1 1 L 9 9 M 9 1 L 1 9";

    @FXML
    public void initialize() {
        setupWindowControls();
        buildCategorySidebar();
        renderProducts(productService.getAllProducts());

        // Επαναφορά του παραθύρου με smooth fade-in όταν το ξανανοίγεις από το taskbar
        Platform.runLater(() -> {
            if (mainPane != null && mainPane.getScene() != null) {
                Stage stage = (Stage) mainPane.getScene().getWindow();
                stage.iconifiedProperty().addListener((obs, wasIconified, isNowIconified) -> {
                    if (!isNowIconified) {
                        mainPane.setTranslateY(0);
                        mainPane.setScaleX(1.0);
                        mainPane.setScaleY(1.0);
                        FadeTransition ft = new FadeTransition(Duration.millis(180), mainPane);
                        ft.setFromValue(0.0);
                        ft.setToValue(1.0);
                        ft.setInterpolator(Interpolator.EASE_OUT);
                        ft.play();
                    }
                });
            }
        });
    }

    private void setupWindowControls() {
        if (topBar == null || btnMinimize == null || btnMaximize == null || btnClose == null) return;

        btnMinimize.setGraphic(createWinIcon(ICON_MINIMIZE));
        btnMaximize.setGraphic(createWinIcon(ICON_MAXIMIZE));
        btnClose.setGraphic(createWinIcon(ICON_CLOSE));

        attachTooltip(btnMinimize, "Ελαχιστοποίηση");
        attachTooltip(btnMaximize, "Μεγιστοποίηση");
        attachTooltip(btnClose, "Κλείσιμο εφαρμογής");

        for (Button btn : new Button[]{btnMinimize, btnMaximize, btnClose}) {
            btn.setOnMouseEntered(e -> animateScale(btn, 1.08));
            btn.setOnMouseExited(e -> animateScale(btn, 1.0));
        }

        topBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        topBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) topBar.getScene().getWindow();
            if (isMaximized) {
                handleMaximize();
                xOffset = stage.getWidth() / 2;
            }
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        topBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) handleMaximize();
        });
    }

    private SVGPath createWinIcon(String svgContent) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.getStyleClass().add("win-icon");
        return icon;
    }

    private void attachTooltip(Button btn, String text) {
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(200));
        tip.setHideDelay(Duration.millis(100));
        tip.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-padding: 5 9; -fx-background-radius: 6; -fx-border-color: #334155; -fx-border-radius: 6;");
        btn.setTooltip(tip);
    }

    private void animateScale(Node node, double target) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setToX(target);
        st.setToY(target);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) topBar.getScene().getWindow();

        FadeTransition ft = new FadeTransition(Duration.millis(160), mainPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(160), mainPane);
        tt.setToY(24);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setInterpolator(Interpolator.EASE_IN);
        pt.setOnFinished(e -> stage.setIconified(true));
        pt.play();
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) topBar.getScene().getWindow();

        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(60), new KeyValue(stage.opacityProperty(), 0.0, Interpolator.EASE_IN))
        );

        fadeOut.setOnFinished(e -> {
            if (!isMaximized) {
                prevX = stage.getX();
                prevY = stage.getY();
                prevW = stage.getWidth();
                prevH = stage.getHeight();

                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());

                isMaximized = true;
                btnMaximize.setGraphic(createWinIcon(ICON_RESTORE));
                btnMaximize.getTooltip().setText("Επαναφορά");
            } else {
                stage.setX(prevX);
                stage.setY(prevY);
                stage.setWidth(prevW);
                stage.setHeight(prevH);

                isMaximized = false;
                btnMaximize.setGraphic(createWinIcon(ICON_MAXIMIZE));
                btnMaximize.getTooltip().setText("Μεγιστοποίηση");
            }

            Platform.runLater(() -> {
                Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.millis(140), new KeyValue(stage.opacityProperty(), 1.0, Interpolator.EASE_OUT))
                );
                fadeIn.play();
            });
        });

        fadeOut.play();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) topBar.getScene().getWindow();

        FadeTransition ft = new FadeTransition(Duration.millis(160), mainPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(160), mainPane);
        st.setToX(0.96);
        st.setToY(0.96);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setInterpolator(Interpolator.EASE_IN);
        pt.setOnFinished(e -> stage.close());
        pt.play();
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
            showCatalogView();
            handleSearch();
        });
        return btn;
    }

    private void showCatalogView() {
        if (mainPane.getCenter() != catalogScroll) {
            mainPane.setCenter(catalogScroll);
            playViewEnterAnimation(catalogScroll);
        }
    }

    private void playViewEnterAnimation(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
        tt.setFromY(12);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
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

        Label catBadge = new Label(p.getCategory().name());
        catBadge.getStyleClass().add("card-category");

        StackPane imageBox = createProductImageBox(p, 140);

        Label title = new Label(p.getName());
        title.setWrapText(true);
        title.setPrefHeight(42);
        title.getStyleClass().add("card-title");

        String previewSpecs = p.getSpecs().entrySet().stream()
            .limit(2)
            .map(entry -> formatKey(entry.getKey()) + ": " + formatValue(entry.getValue()))
            .reduce((a, b) -> a + "  •  " + b)
            .orElse("");
        Label specsLbl = new Label(previewSpecs);
        specsLbl.setWrapText(true);
        specsLbl.getStyleClass().add("card-specs");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLbl = new Label(String.format("%.2f €", p.getPrice()));
        priceLbl.getStyleClass().add("card-price");
        Region midSpacer = new Region();
        HBox.setHgrow(midSpacer, Priority.ALWAYS);
        Label stockLbl = new Label(p.getStock() > 0 ? "Διαθέσιμο (" + p.getStock() + ")" : "Εξαντλήθηκε");
        stockLbl.getStyleClass().add(p.getStock() > 0 ? "badge-instock" : "badge-outstock");
        priceRow.getChildren().addAll(priceLbl, midSpacer, stockLbl);

        Button addBtn = new Button("Προσθήκη στο καλάθι");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setDisable(p.getStock() == 0);
        addBtn.setOnAction(e -> {
            e.consume();
            addToCart(p);
        });

        card.getChildren().addAll(catBadge, imageBox, title, specsLbl, spacer, priceRow, addBtn);
        card.setOnMouseClicked(e -> showProductDetails(p));

        return card;
    }

    private void addToCart(Product p) {
        cartCount++;
        cartButton.setText("Καλάθι (" + cartCount + ")");
    }

    private StackPane createProductImageBox(Product p, double size) {
        StackPane box = new StackPane();
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxHeight(size);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-padding: 12;");

        String path = IconResolver.getIconPath(p);
        InputStream is = getClass().getResourceAsStream(path);

        if (is != null) {
            ImageView imageView = new ImageView(new Image(is));
            imageView.setFitWidth(size - 24);
            imageView.setFitHeight(size - 24);
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
        VBox container = new VBox(20);
        container.setPadding(new Insets(24, 32, 32, 32));
        container.setMaxWidth(960);

        // Κουμπί επιστροφής
        Button backBtn = new Button("←  Πίσω στα Προϊόντα");
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnAction(e -> showCatalogView());

        // Hero Card (Icon αριστερά + Στοιχεία & Αγορά δεξιά)
        HBox heroCard = new HBox(28);
        heroCard.setAlignment(Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(24));
        heroCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #E2E8F0; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.05), 10, 0, 0, 2);");

        StackPane iconBox = createProductImageBox(p, 210);

        VBox infoCol = new VBox(12);
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        Label catBadge = new Label(p.getCategory().getDisplayName());
        catBadge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 6;");

        Label title = new Label(p.getName());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        HBox priceStockRow = new HBox(16);
        priceStockRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLbl = new Label(String.format("%.2f €", p.getPrice()));
        priceLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label stockPill = new Label(p.getStock() > 0 ? "●  Διαθέσιμο (" + p.getStock() + " τεμ.)" : "●  Εξαντλήθηκε");
        stockPill.setStyle(p.getStock() > 0
            ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 20;"
            : "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 20;");
        priceStockRow.getChildren().addAll(priceLbl, stockPill);

        Button addBtn = new Button("Προσθήκη στο καλάθι");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setStyle("-fx-font-size: 14px; -fx-padding: 11 26;");
        addBtn.setDisable(p.getStock() == 0);
        addBtn.setOnAction(e -> addToCart(p));

        infoCol.getChildren().addAll(catBadge, title, priceStockRow, new Region(), addBtn);
        heroCard.getChildren().addAll(iconBox, infoCol);

        // Specs Card (Τεχνικά Χαρακτηριστικά)
        VBox specsCard = new VBox(12);
        specsCard.setPadding(new Insets(24));
        specsCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #E2E8F0; -fx-border-radius: 14;");

        Label specsHeader = new Label("Τεχνικά Χαρακτηριστικά");
        specsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        VBox specsList = new VBox(0);
        int row = 0;
        for (Map.Entry<String, Object> entry : p.getSpecs().entrySet()) {
            HBox specRow = new HBox(16);
            specRow.setPadding(new Insets(10, 14, 10, 14));
            specRow.setAlignment(Pos.CENTER_LEFT);
            String bg = (row % 2 == 0) ? "#F8FAFC" : "#FFFFFF";
            specRow.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6;");

            Label k = new Label(formatKey(entry.getKey()));
            k.setPrefWidth(240);
            k.setMinWidth(240);
            k.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 13px;");

            Label v = new Label(formatValue(entry.getValue()));
            v.setWrapText(true);
            v.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13px;");

            specRow.getChildren().addAll(k, v);
            specsList.getChildren().add(specRow);
            row++;
        }

        specsCard.getChildren().addAll(specsHeader, new Separator(), specsList);
        container.getChildren().addAll(backBtn, heroCard, specsCard);

        // Wrapper για κεντράρισμα μέσα σε ScrollPane
        StackPane centerWrapper = new StackPane(container);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        centerWrapper.setStyle("-fx-background-color: #F8FAFC;");

        ScrollPane detailsScroll = new ScrollPane(centerWrapper);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");

        mainPane.setCenter(detailsScroll);
        playViewEnterAnimation(detailsScroll);
    }

    private String formatKey(String key) {
        String spaced = key.replace("_", " ");
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private String formatValue(Object val) {
        if (val instanceof Boolean b) return b ? "Ναι" : "Όχι";
        String str = String.valueOf(val);
        if (str.startsWith("[") && str.endsWith("]")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    @FXML
    public void handleSearch() {
        showCatalogView();
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