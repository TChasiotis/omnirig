package com.omnirig.utils;

import com.omnirig.models.Category;
import com.omnirig.services.ProductService;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class CollapsibleSidebarHelper {

    private static final double COLLAPSED_WIDTH = 68.0;
    private static final double EXPANDED_WIDTH = 245.0;

    // Minimalist 24x24 vector icons
    // Minimalist 24x24 vector icons
    private static final String SVG_ALL  = "M 3 3 H 10 V 10 H 3 Z M 14 3 H 21 V 10 H 14 Z M 3 14 H 10 V 21 H 3 Z M 14 14 H 21 V 21 H 14 Z";
    private static final String SVG_CPU  = "M 6 6 H 18 V 18 H 6 Z M 9 9 H 15 V 15 H 9 Z M 8 2 V 6 M 12 2 V 6 M 16 2 V 6 M 8 18 V 22 M 12 18 V 22 M 16 18 V 22 M 2 8 H 6 M 2 12 H 6 M 2 16 H 6 M 18 8 H 22 M 18 12 H 22 M 18 16 H 22";
    private static final String SVG_MOBO = "M 3 3 H 21 V 21 H 3 Z M 6 6 H 12 V 12 H 6 Z M 15 6 V 13 M 18 6 V 13 M 6 16 H 15 M 6 19 H 12";
    private static final String SVG_RAM  = "M 2 6 H 22 V 16 H 2 Z M 5 9 H 8 V 13 H 5 Z M 10.5 9 H 13.5 V 13 H 10.5 Z M 16 9 H 19 V 13 H 16 Z M 4 16 V 19 M 6.5 16 V 19 M 9 16 V 19 M 13 16 V 19 M 15.5 16 V 19 M 18 16 V 19 M 20.5 16 V 19";
    private static final String SVG_GPU  = "M 2 6 H 22 V 17 H 2 Z M 8 11.5 m -3 0 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 M 16 11.5 m -3 0 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 M 5 17 V 19 H 13 V 17";
    private static final String SVG_COOL = "M 12 2 V 22 M 9.5 4.5 L 12 7 L 14.5 4.5 M 9.5 19.5 L 12 17 L 14.5 19.5 M 3.3 7 L 20.7 17 M 4.5 10.5 L 7 9.1 L 6.5 6.2 M 19.5 13.5 L 17 14.9 L 17.5 17.8 M 20.7 7 L 3.3 17 M 19.5 10.5 L 17 9.1 L 17.5 6.2 M 4.5 13.5 L 7 14.9 L 6.5 17.8";
    private static final String SVG_SSD  = "M 4 7 H 22 V 17 H 4 Z M 2 9 H 4 M 2 12 H 4 M 2 15 H 4 M 7 10 H 10 V 14 H 7 Z M 12.5 9.5 H 18.5 V 14.5 H 12.5 Z";
    private static final String SVG_CASE = "M 5 2 H 19 V 20 H 5 Z M 7 20 V 22 M 17 20 V 22 M 8 5 H 16 V 13 H 8 Z M 5 16 H 19 M 8 18 H 12 M 15.5 18 H 16";
    private static final String SVG_PSU  = "M 2 5 H 18 V 19 H 2 Z M 11 8 L 7.5 12.5 H 10.5 L 9.5 16 L 13 11.5 H 10 Z M 18 8.5 H 22 M 18 12 H 22 M 18 15.5 H 22";

    private final VBox categoryBox;
    private final BiConsumer<Category, String> onCategorySelected;

    private boolean expanded = false;
    private Label headerLabel;
    private Button toggleBtn;
    private final List<Button> categoryButtons = new ArrayList<>();

    public CollapsibleSidebarHelper(
            VBox categoryBox,
            ProductService productService,
            BiConsumer<Category, String> onCategorySelected) {
        this.categoryBox = categoryBox;
        this.onCategorySelected = onCategorySelected;
    }

    public void build() {
        categoryBox.getChildren().clear();
        categoryButtons.clear();

        categoryBox.setPrefWidth(COLLAPSED_WIDTH);
        categoryBox.setMinWidth(COLLAPSED_WIDTH);
        categoryBox.setMaxWidth(COLLAPSED_WIDTH);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        headerLabel = new Label("ΚΑΤΗΓΟΡΙΕΣ");
        headerLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        headerLabel.setVisible(false);
        headerLabel.setManaged(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleBtn = new Button("❯");
        toggleBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; "
            + "-fx-min-width: 34; -fx-min-height: 30; -fx-background-radius: 8; -fx-cursor: hand;");
        toggleBtn.setTooltip(createTooltip("Άνοιγμα κατηγοριών"));
        toggleBtn.setOnAction(e -> toggle());

        topRow.getChildren().addAll(headerLabel, spacer, toggleBtn);
        categoryBox.getChildren().add(topRow);

        Button allBtn = createCategoryButton("Όλα τα Προϊόντα", null, createVectorIcon(SVG_ALL));
        allBtn.getStyleClass().add("cat-btn-active");
        categoryBox.getChildren().add(allBtn);

        for (Category cat : Category.values()) {
            categoryBox.getChildren().add(createCategoryButton(cat.getDisplayName(), cat, createCategoryIcon(cat)));
        }

        applyCollapsedState(false);
    }

    private void toggle() {
        expanded = !expanded;
        double targetW = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;

        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(180),
                new KeyValue(categoryBox.prefWidthProperty(), targetW, Interpolator.EASE_BOTH),
                new KeyValue(categoryBox.minWidthProperty(), targetW, Interpolator.EASE_BOTH),
                new KeyValue(categoryBox.maxWidthProperty(), targetW, Interpolator.EASE_BOTH)
            )
        );
        tl.play();

        applyCollapsedState(expanded);
    }

    private void applyCollapsedState(boolean isExpanded) {
        headerLabel.setVisible(isExpanded);
        headerLabel.setManaged(isExpanded);
        toggleBtn.setText(isExpanded ? "❮" : "❯");
        toggleBtn.getTooltip().setText(isExpanded ? "Σύμπτυξη κατηγοριών" : "Άνοιγμα κατηγοριών");

        for (Button btn : categoryButtons) {
            btn.setContentDisplay(isExpanded ? ContentDisplay.LEFT : ContentDisplay.GRAPHIC_ONLY);
            btn.setAlignment(isExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
        }
    }

    private Button createCategoryButton(String label, Category category, Node icon) {
        Button btn = new Button(label);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(12);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("cat-btn");
        btn.setTooltip(createTooltip(label));

        btn.setOnAction(e -> {
            categoryButtons.forEach(b -> b.getStyleClass().remove("cat-btn-active"));
            btn.getStyleClass().add("cat-btn-active");
            onCategorySelected.accept(category, label);
        });

        categoryButtons.add(btn);
        return btn;
    }

    private Node createCategoryIcon(Category cat) {
        String path = switch (cat) {
            case CPU         -> SVG_CPU;
            case MOTHERBOARD -> SVG_MOBO;
            case RAM         -> SVG_RAM;
            case GPU         -> SVG_GPU;
            case COOLER      -> SVG_COOL;
            case STORAGE     -> SVG_SSD;
            case CASE        -> SVG_CASE;
            case PSU         -> SVG_PSU;
        };
        return createVectorIcon(path);
    }

    private Node createVectorIcon(String svgPathData) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgPathData);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#475569"));
        svg.setStrokeWidth(1.8);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.setScaleX(0.82);
        svg.setScaleY(0.82);

        StackPane box = new StackPane(new Group(svg));
        box.setPrefSize(22, 22);
        box.setMinSize(22, 22);
        box.setMaxSize(22, 22);
        return box;
    }

    private Tooltip createTooltip(String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(150));
        t.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-padding: 5 9; -fx-background-radius: 6;");
        return t;
    }
}