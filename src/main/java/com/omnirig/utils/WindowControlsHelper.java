package com.omnirig.utils;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WindowControlsHelper {

    private static final String ICON_MINIMIZE = "M 0 5 L 10 5";
    private static final String ICON_MAXIMIZE = "M 1 1 L 9 1 L 9 9 L 1 9 Z";
    private static final String ICON_RESTORE  = "M 3 1 L 9 1 L 9 7 M 1 3 L 7 3 L 7 9 L 1 9 Z";
    private static final String ICON_CLOSE    = "M 1 1 L 9 9 M 9 1 L 1 9";

    private final BorderPane mainPane;
    private final HBox topBar;
    private final Button btnMinimize;
    private final Button btnMaximize;
    private final Button btnClose;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double prevX, prevY, prevW, prevH;

    public WindowControlsHelper(BorderPane mainPane, HBox topBar, Button btnMinimize, Button btnMaximize, Button btnClose) {
        this.mainPane = mainPane;
        this.topBar = topBar;
        this.btnMinimize = btnMinimize;
        this.btnMaximize = btnMaximize;
        this.btnClose = btnClose;
    }

    public void attach() {
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
                toggleMaximize();
                xOffset = stage.getWidth() / 2;
            }
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        topBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) toggleMaximize();
        });

        // Smooth fade-in κατά την επαναφορά από το taskbar
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

    public void minimize() {
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

    public void toggleMaximize() {
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

    public void close() {
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
}