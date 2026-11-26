package com.omnirig.utils;

import com.omnirig.models.Product;
import com.omnirig.services.IconResolver;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

public class ProductUiFactory {

    public static VBox createProductCard(Product p, Consumer<Product> onAddToCart, Consumer<Product> onOpenDetails) {
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
            onAddToCart.accept(p);
        });

        card.getChildren().addAll(catBadge, imageBox, title, specsLbl, spacer, priceRow, addBtn);
        card.setOnMouseClicked(e -> onOpenDetails.accept(p));

        return card;
    }

    public static ScrollPane createProductDetailsView(Product p, Runnable onBack, Consumer<Product> onAddToCart) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24, 32, 32, 32));
        container.setMaxWidth(960);

        Button backBtn = new Button("←  Πίσω στα Προϊόντα");
        String normalStyle = "-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;";
        String hoverStyle  = "-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-cursor: hand;";
        backBtn.setStyle(normalStyle);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(hoverStyle));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(normalStyle));
        backBtn.setOnAction(e -> onBack.run());

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
        addBtn.setOnAction(e -> onAddToCart.accept(p));

        infoCol.getChildren().addAll(catBadge, title, priceStockRow, new Region(), addBtn);
        heroCard.getChildren().addAll(iconBox, infoCol);

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
            specRow.setStyle("-fx-background-color: " + (row % 2 == 0 ? "#F8FAFC" : "#FFFFFF") + "; -fx-background-radius: 6;");

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

        StackPane centerWrapper = new StackPane(container);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        centerWrapper.setStyle("-fx-background-color: #F8FAFC;");

        ScrollPane detailsScroll = new ScrollPane(centerWrapper);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");
        return detailsScroll;
    }

    public static StackPane createProductImageBox(Product p, double size) {
        StackPane box = new StackPane();
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxHeight(size);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-padding: 12;");

        InputStream is = ProductUiFactory.class.getResourceAsStream(IconResolver.getIconPath(p));
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

    public static void playViewEnterAnimation(Node node) {
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

    private static String formatKey(String key) {
        String spaced = key.replace("_", " ");
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private static String formatValue(Object val) {
        if (val instanceof Boolean b) return b ? "Ναι" : "Όχι";
        String str = String.valueOf(val);
        if (str.startsWith("[") && str.endsWith("]")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}