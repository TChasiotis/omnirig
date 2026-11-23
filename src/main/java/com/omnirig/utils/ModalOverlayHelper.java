package com.omnirig.utils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class ModalOverlayHelper {

    public static void showCheckoutAuthModal(
            StackPane rootStack,
            Runnable onLogin,
            Runnable onRegister,
            Runnable onGuest) {

        if (rootStack == null) return;

        // Σκούρο ημιδιαφανές backdrop
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(15, 23, 42, 0.58);");
        backdrop.setAlignment(Pos.CENTER);

        // Κεντραρισμένη κάρτα
        VBox card = new VBox(16);
        card.setMaxWidth(430);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(26, 28, 24, 28));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.28), 28, 0, 0, 8);");

        // Header + κουμπί κλεισίματος
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Συνέχεια στο Checkout");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        headerRow.getChildren().addAll(title, sp, closeBtn);

        Label subtitle = new Label("Δεν είσαι συνδεδεμένος στον λογαριασμό σου. Συνδέσου για αυτόματη συμπλήρωση των στοιχείων σου ή προχώρησε ως επισκέπτης.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-line-spacing: 3;");

        // Κουμπιά επιλογών
        Button loginBtn = new Button("Σύνδεση στον λογαριασμό μου");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setStyle("-fx-padding: 11 16; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");

        Button registerBtn = new Button("Δημιουργία νέου λογαριασμού (Εγγραφή)");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; "
            + "-fx-padding: 10 16; -fx-background-radius: 8; -fx-border-color: #BFDBFE; -fx-border-radius: 8; -fx-cursor: hand;");

        Button guestBtn = new Button("Συνέχεια ως Επισκέπτης  →");
        guestBtn.setMaxWidth(Double.MAX_VALUE);
        guestBtn.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #334155; -fx-font-weight: bold; "
            + "-fx-padding: 10 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;");

        Runnable dismiss = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(140), backdrop);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> rootStack.getChildren().remove(backdrop));
            fadeOut.play();
        };

        closeBtn.setOnAction(e -> dismiss.run());
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) dismiss.run();
        });

        loginBtn.setOnAction(e -> { dismiss.run(); if (onLogin != null) onLogin.run(); });
        registerBtn.setOnAction(e -> { dismiss.run(); if (onRegister != null) onRegister.run(); });
        guestBtn.setOnAction(e -> { dismiss.run(); if (onGuest != null) onGuest.run(); });

        card.getChildren().addAll(headerRow, subtitle, loginBtn, registerBtn, guestBtn);
        backdrop.getChildren().add(card);
        rootStack.getChildren().add(backdrop);

        // Smooth είσοδος
        backdrop.setOpacity(0);
        card.setScaleX(0.94);
        card.setScaleY(0.94);

        FadeTransition ft = new FadeTransition(Duration.millis(160), backdrop);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
        st.setToX(1.0);
        st.setToY(1.0);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
    }

    public static void showBudgetInputModal(StackPane rootStack, java.util.function.Consumer<Double> onConfirm) {
        if (rootStack == null) return;

        TextField budgetField = new TextField("800");
        budgetField.setPromptText("π.χ. 800");
        budgetField.setStyle("-fx-font-size: 15px; -fx-padding: 10 12; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-radius: 8;");

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        errLbl.setVisible(false);
        errLbl.setManaged(false);

        VBox content = new VBox(8, budgetField, errLbl);

        showCustomModal(rootStack, "Auto-Build βάσει Budget",
            "Δώσε συνολικό budget σε € (δημιουργούνται έως 3 συμβατές προτάσεις).",
            content, "Εύρεση Προτάσεων", () -> {
                try {
                    double val = Double.parseDouble(budgetField.getText().replace(",", ".").trim());
                    if (val <= 50) {
                        errLbl.setText("Δώσε ένα ποσό μεγαλύτερο από 50 €.");
                        errLbl.setVisible(true);
                        errLbl.setManaged(true);
                        return false;
                    }
                    onConfirm.accept(val);
                    return true;
                } catch (NumberFormatException ex) {
                    errLbl.setText("Μη έγκυρος αριθμός.");
                    errLbl.setVisible(true);
                    errLbl.setManaged(true);
                    return false;
                }
            });
    }

    public static void showCustomModal(
            StackPane rootStack,
            String titleText,
            String subtitleText,
            javafx.scene.Node bodyContent,
            String confirmText,
            java.util.function.BooleanSupplier onConfirm) {

        if (rootStack == null) return;

        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(15, 23, 42, 0.58);");
        backdrop.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.setMaxWidth(520);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(24, 26, 22, 26));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.28), 28, 0, 0, 8);");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        headerRow.getChildren().addAll(title, sp, closeBtn);

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        Button cancelBtn = new Button("Ακύρωση");
        cancelBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 9 16; -fx-background-radius: 8; -fx-cursor: hand;");

        Button okBtn = new Button(confirmText);
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setStyle("-fx-padding: 9 18; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox actions = new HBox(10, cancelBtn, okBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Runnable dismiss = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(140), backdrop);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> rootStack.getChildren().remove(backdrop));
            fadeOut.play();
        };

        closeBtn.setOnAction(e -> dismiss.run());
        cancelBtn.setOnAction(e -> dismiss.run());
        backdrop.setOnMouseClicked(e -> { if (e.getTarget() == backdrop) dismiss.run(); });
        okBtn.setOnAction(e -> {
            if (onConfirm == null || onConfirm.getAsBoolean()) dismiss.run();
        });

        card.getChildren().addAll(headerRow, subtitle, bodyContent, actions);
        backdrop.getChildren().add(card);
        rootStack.getChildren().add(backdrop);

        backdrop.setOpacity(0);
        card.setScaleX(0.94);
        card.setScaleY(0.94);
        FadeTransition ft = new FadeTransition(Duration.millis(160), backdrop);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
        st.setToX(1.0);
        st.setToY(1.0);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
    }
}