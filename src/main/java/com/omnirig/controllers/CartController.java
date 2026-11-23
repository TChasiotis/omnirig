package com.omnirig.controllers;

import com.omnirig.models.Product;
import com.omnirig.services.IconResolver;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.InputStream;
import java.util.Map;

public class CartController {

    private static final double FREE_SHIPPING_THRESHOLD = 100.0;
    private static final double STANDARD_SHIPPING = 4.90;

    @FXML private VBox itemsContainer;
    @FXML private VBox summaryCard;
    @FXML private Button clearCartBtn;
    @FXML private Button checkoutBtn;
    @FXML private Label subtotalLbl;
    @FXML private Label shippingLbl;
    @FXML private Label freeShippingHintLbl;
    @FXML private Label totalLbl;

    private Map<Product, Integer> cart;
    private Runnable onBack;
    private Runnable onCartChanged;
    private Runnable onProceedToCheckout;

    public void init(Map<Product, Integer> cart, Runnable onBack, Runnable onCartChanged, Runnable onProceedToCheckout) {
        this.cart = cart;
        this.onBack = onBack;
        this.onCartChanged = onCartChanged;
        this.onProceedToCheckout = onProceedToCheckout;
        renderCart();
    }

    @FXML
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void handleClearCart() {
        cart.clear();
        if (onCartChanged != null) onCartChanged.run();
        renderCart();
    }

    @FXML
    private void handleProceedToCheckout() {
        if (!cart.isEmpty() && onProceedToCheckout != null) {
            onProceedToCheckout.run();
        }
    }

    private void renderCart() {
        itemsContainer.getChildren().clear();

        boolean empty = cart.isEmpty();
        clearCartBtn.setDisable(empty);
        checkoutBtn.setDisable(empty);

        if (empty) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60, 20, 60, 20));
            emptyBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #E2E8F0; -fx-border-radius: 14;");

            Label msg = new Label("Το καλάθι σου είναι άδειο.");
            msg.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            Label sub = new Label("Πρόσθεσε εξαρτήματα από τον κατάλογο ή φτιάξε ένα σύστημα στον PC Builder.");
            sub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
            emptyBox.getChildren().addAll(msg, sub);
            itemsContainer.getChildren().add(emptyBox);
        } else {
            for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
                itemsContainer.getChildren().add(createCartRow(entry.getKey(), entry.getValue()));
            }
        }

        updateSummary();
    }

    private HBox createCartRow(Product product, int qty) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        ImageView icon = loadIcon(product);

        VBox info = new VBox(3);
        Label nameLbl = new Label(product.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
        Label unitLbl = new Label(product.getCategory().getDisplayName() + "  •  " + String.format("%.2f € / τεμ.", product.getPrice()));
        unitLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        info.getChildren().addAll(nameLbl, unitLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Controls ποσότητας (- 1 +)
        Button minusBtn = new Button("−");
        minusBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-min-width: 28; -fx-background-radius: 6; -fx-cursor: hand;");
        minusBtn.setOnAction(e -> {
            if (qty > 1) cart.put(product, qty - 1);
            else cart.remove(product);
            if (onCartChanged != null) onCartChanged.run();
            renderCart();
        });

        Label qtyLbl = new Label(String.valueOf(qty));
        qtyLbl.setMinWidth(26);
        qtyLbl.setAlignment(Pos.CENTER);
        qtyLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-min-width: 28; -fx-background-radius: 6; -fx-cursor: hand;");
        plusBtn.setDisable(qty >= product.getStock());
        plusBtn.setOnAction(e -> {
            cart.put(product, qty + 1);
            if (onCartChanged != null) onCartChanged.run();
            renderCart();
        });

        HBox qtyBox = new HBox(6, minusBtn, qtyLbl, plusBtn);
        qtyBox.setAlignment(Pos.CENTER);

        Label lineTotalLbl = new Label(String.format("%.2f €", product.getPrice() * qty));
        lineTotalLbl.setMinWidth(85);
        lineTotalLbl.setAlignment(Pos.CENTER_RIGHT);
        lineTotalLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2563EB;");

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-cursor: hand;");
        removeBtn.setMinWidth(Region.USE_PREF_SIZE);
        removeBtn.setOnAction(e -> {
            cart.remove(product);
            if (onCartChanged != null) onCartChanged.run();
            renderCart();
        });

        row.getChildren().addAll(icon, info, qtyBox, lineTotalLbl, removeBtn);
        return row;
    }

    private void updateSummary() {
        double subtotal = cart.entrySet().stream()
            .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
            .sum();

        double shipping = (subtotal == 0 || subtotal >= FREE_SHIPPING_THRESHOLD) ? 0.0 : STANDARD_SHIPPING;
        double total = subtotal + shipping;

        subtotalLbl.setText(String.format("%.2f €", subtotal));
        if (shipping == 0.0) {
            shippingLbl.setText("Δωρεάν");
            shippingLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981;");
            freeShippingHintLbl.setText(subtotal > 0 ? "Κέρδισες δωρεάν μεταφορικά!" : "");
        } else {
            shippingLbl.setText(String.format("%.2f €", shipping));
            shippingLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
            freeShippingHintLbl.setText(String.format("Πρόσθεσε %.2f € ακόμα για δωρεάν μεταφορικά.", FREE_SHIPPING_THRESHOLD - subtotal));
        }
        totalLbl.setText(String.format("%.2f €", total));
    }

    private ImageView loadIcon(Product p) {
        ImageView iv = new ImageView();
        iv.setFitHeight(44);
        iv.setFitWidth(44);
        iv.setPreserveRatio(true);
        InputStream is = getClass().getResourceAsStream(IconResolver.getIconPath(p));
        if (is != null) iv.setImage(new Image(is));
        return iv;
    }
}