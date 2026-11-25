package com.omnirig.utils;

import com.omnirig.controllers.CheckoutController.CustomerProfile;
import com.omnirig.services.AuthService;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Optional;
import java.util.function.Consumer;

public class AuthModalHelper {

    // --- LOGIN MODAL ---
    public static void showLoginModal(
        StackPane rootStack,
        AuthService authService,
        Consumer<CustomerProfile> onLoginSuccess,
        Runnable onSwitchToRegister,
        Runnable onAdminLogin) {

        if (rootStack == null) return;

        StackPane backdrop = createBackdrop();
        VBox card = createCard(410);

        HBox header = createHeader("Σύνδεση στο OmniRig", () -> dismissBackdrop(rootStack, backdrop));

        Label hint = new Label("Demo: demo@omnirig.gr  /  1234");
        Label hint2 = new Label("Admin: admin@omnirig.gr  /  admin123");
        hint.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hint2.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 6;");

        TextField emailField = createField("name@example.com", "");
        PasswordField passField = new PasswordField();
        passField.setPromptText("••••••••");
        passField.setStyle(fieldStyle());

        Label errorLbl = createErrorLabel();

        Button loginBtn = new Button("Σύνδεση");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setStyle("-fx-padding: 11 16; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox footerRow = new HBox(6);
        footerRow.setAlignment(Pos.CENTER);
        Label noAccLbl = new Label("Δεν έχεις λογαριασμό;");
        noAccLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        Hyperlink regLink = new Hyperlink("Εγγραφή");
        regLink.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 12px; -fx-border-color: transparent;");
        footerRow.getChildren().addAll(noAccLbl, regLink);

        Runnable dismiss = () -> dismissBackdrop(rootStack, backdrop);

        Runnable attemptLogin = () -> {
            String email = emailField.getText().trim();
            String pass = passField.getText().trim();

            if (email.isBlank() || pass.isBlank()) {
                showError(errorLbl, "Συμπλήρωσε email και κωδικό.");
                return;
            }
            if (AdminConfig.isAdminCredentials(email, pass)) {
                dismiss.run();
                if (onAdminLogin != null) onAdminLogin.run();
                return;
            }
            Optional<CustomerProfile> user = authService.login(email, pass);
            if (user.isPresent()) {
                dismiss.run();
                if (onLoginSuccess != null) onLoginSuccess.accept(user.get());
            } else {
                showError(errorLbl, "Λάθος email ή κωδικός πρόσβασης.");
            }
        };

        loginBtn.setOnAction(e -> attemptLogin.run());
        passField.setOnAction(e -> attemptLogin.run());
        backdrop.setOnMouseClicked(e -> { if (e.getTarget() == backdrop) dismiss.run(); });
        regLink.setOnAction(e -> {
            dismiss.run();
            if (onSwitchToRegister != null) onSwitchToRegister.run();
        });

        VBox fields = new VBox(6, fieldLabel("Email"), emailField, fieldLabel("Κωδικός πρόσβασης"), passField);
        card.getChildren().addAll(header, hint, hint2, fields, errorLbl, loginBtn, footerRow);
        showBackdrop(rootStack, backdrop, card);
    }

    // --- REGISTER MODAL (CREATE) ---
    public static void showRegisterModal(
            StackPane rootStack,
            AuthService authService,
            Consumer<CustomerProfile> onRegisterSuccess,
            Runnable onSwitchToLogin) {

        if (rootStack == null) return;

        StackPane backdrop = createBackdrop();
        VBox card = createCard(480);

        HBox header = createHeader("Δημιουργία Λογαριασμού", () -> dismissBackdrop(rootStack, backdrop));

        TextField firstField = createField("Όνομα *", "");
        TextField lastField  = createField("Επώνυμο *", "");
        TextField emailField = createField("Email *", "");
        TextField phoneField = createField("Τηλέφωνο *", "");
        TextField addrField  = createField("Οδός & Αριθμός *", "");
        TextField cityField  = createField("Πόλη *", "");
        TextField zipField   = createField("Τ.Κ. *", "");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Κωδικός πρόσβασης (τουλ. 4 χαρακτήρες) *");
        passField.setStyle(fieldStyle());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(firstField, 0, 0); grid.add(lastField, 1, 0);
        grid.add(emailField, 0, 1); grid.add(phoneField, 1, 1);
        grid.add(addrField,  0, 2, 2, 1);
        grid.add(cityField,  0, 3); grid.add(zipField, 1, 3);
        grid.add(passField,  0, 4, 2, 1);

        Label errorLbl = createErrorLabel();

        Button regBtn = new Button("Εγγραφή & Σύνδεση");
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.getStyleClass().add("btn-primary");
        regBtn.setStyle("-fx-padding: 11 16; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox footerRow = new HBox(6);
        footerRow.setAlignment(Pos.CENTER);
        Label hasAcc = new Label("Έχεις ήδη λογαριασμό;");
        hasAcc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        Hyperlink loginLink = new Hyperlink("Σύνδεση");
        loginLink.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 12px; -fx-border-color: transparent;");
        footerRow.getChildren().addAll(hasAcc, loginLink);

        Runnable dismiss = () -> dismissBackdrop(rootStack, backdrop);
        backdrop.setOnMouseClicked(e -> { if (e.getTarget() == backdrop) dismiss.run(); });

        regBtn.setOnAction(e -> {
            if (firstField.getText().isBlank() || lastField.getText().isBlank() || emailField.getText().isBlank()
                    || phoneField.getText().isBlank() || addrField.getText().isBlank()
                    || cityField.getText().isBlank() || zipField.getText().isBlank() || passField.getText().length() < 4) {
                showError(errorLbl, "Συμπλήρωσε όλα τα πεδία (*) και κωδικό τουλάχιστον 4 χαρακτήρων.");
                return;
            }
            CustomerProfile profile = new CustomerProfile(
                firstField.getText().trim(), lastField.getText().trim(), emailField.getText().trim(),
                phoneField.getText().trim(), addrField.getText().trim(), cityField.getText().trim(), zipField.getText().trim()
            );
            if (!authService.register(passField.getText(), profile)) {
                showError(errorLbl, "Υπάρχει ήδη λογαριασμός με αυτό το email.");
                return;
            }
            dismiss.run();
            if (onRegisterSuccess != null) onRegisterSuccess.accept(profile);
        });

        loginLink.setOnAction(e -> {
            dismiss.run();
            if (onSwitchToLogin != null) onSwitchToLogin.run();
        });

        card.getChildren().addAll(header, grid, errorLbl, regBtn, footerRow);
        showBackdrop(rootStack, backdrop, card);
    }

    // --- EDITABLE PROFILE MODAL (READ / UPDATE / DELETE) ---
    public static void showProfileModal(
            StackPane rootStack,
            AuthService authService,
            CustomerProfile user,
            Consumer<CustomerProfile> onUpdated,
            Runnable onDeleted,
            Runnable onLogout) {

        if (rootStack == null || user == null) return;

        StackPane backdrop = createBackdrop();
        VBox card = createCard(480);

        HBox header = createHeader("Ο Λογαριασμός μου (Επεξεργασία)", () -> dismissBackdrop(rootStack, backdrop));

        TextField firstField = createField("Όνομα", user.firstName());
        TextField lastField  = createField("Επώνυμο", user.lastName());
        TextField emailField = createField("Email", user.email());
        emailField.setDisable(true);
        TextField phoneField = createField("Τηλέφωνο", user.phone());
        TextField addrField  = createField("Οδός & Αριθμός", user.address());
        TextField cityField  = createField("Πόλη", user.city());
        TextField zipField   = createField("Τ.Κ.", user.zip());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(new VBox(3, fieldLabel("Όνομα"), firstField), 0, 0);
        grid.add(new VBox(3, fieldLabel("Επώνυμο"), lastField), 1, 0);
        grid.add(new VBox(3, fieldLabel("Email (Κλειδωμένο)"), emailField), 0, 1);
        grid.add(new VBox(3, fieldLabel("Τηλέφωνο"), phoneField), 1, 1);
        grid.add(new VBox(3, fieldLabel("Διεύθυνση"), addrField), 0, 2, 2, 1);
        grid.add(new VBox(3, fieldLabel("Πόλη"), cityField), 0, 3);
        grid.add(new VBox(3, fieldLabel("Τ.Κ."), zipField), 1, 3);

        Label statusLbl = createErrorLabel();

        Button saveBtn = new Button("Αποθήκευση Αλλαγών");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setStyle("-fx-padding: 10 16; -fx-font-weight: bold; -fx-cursor: hand;");

        Button logoutBtn = new Button("Αποσύνδεση");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; "
            + "-fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand;");

        Button deleteBtn = new Button("Διαγραφή Λογαριασμού");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; "
            + "-fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox bottomRow = new HBox(10, logoutBtn, deleteBtn);
        HBox.setHgrow(logoutBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);

        Runnable dismiss = () -> dismissBackdrop(rootStack, backdrop);
        backdrop.setOnMouseClicked(e -> { if (e.getTarget() == backdrop) dismiss.run(); });

        saveBtn.setOnAction(e -> {
            if (firstField.getText().isBlank() || lastField.getText().isBlank() || phoneField.getText().isBlank()
                    || addrField.getText().isBlank() || cityField.getText().isBlank() || zipField.getText().isBlank()) {
                showError(statusLbl, "Όλα τα πεδία είναι υποχρεωτικά.");
                return;
            }
            CustomerProfile updated = new CustomerProfile(
                firstField.getText().trim(), lastField.getText().trim(), user.email(),
                phoneField.getText().trim(), addrField.getText().trim(), cityField.getText().trim(), zipField.getText().trim()
            );
            authService.updateProfile(updated);
            dismiss.run();
            if (onUpdated != null) onUpdated.accept(updated);
        });

        logoutBtn.setOnAction(e -> {
            dismiss.run();
            if (onLogout != null) onLogout.run();
        });

        final boolean[] confirmDelete = {false};
        deleteBtn.setOnAction(e -> {
            if (!confirmDelete[0]) {
                confirmDelete[0] = true;
                deleteBtn.setText("Επιβεβαίωση Διαγραφής;");
                deleteBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                    + "-fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                authService.deleteAccount(user.email());
                dismiss.run();
                if (onDeleted != null) onDeleted.run();
            }
        });

        card.getChildren().addAll(header, grid, statusLbl, saveBtn, new Separator(), bottomRow);
        showBackdrop(rootStack, backdrop, card);
    }

    // --- HELPERS ---
    private static HBox createHeader(String titleText, Runnable onClose) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> onClose.run());
        header.getChildren().addAll(title, sp, closeBtn);
        return header;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 11px;");
        return l;
    }

    private static TextField createField(String prompt, String value) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private static Label createErrorLabel() {
        Label l = new Label();
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-weight: bold;");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private static void showError(Label l, String msg) {
        l.setText(msg);
        l.setVisible(true);
        l.setManaged(true);
    }

    private static StackPane createBackdrop() {
        StackPane b = new StackPane();
        b.setStyle("-fx-background-color: rgba(15, 23, 42, 0.58);");
        b.setAlignment(Pos.CENTER);
        return b;
    }

    private static VBox createCard(double maxW) {
        VBox c = new VBox(14);
        c.setMaxWidth(maxW);
        c.setMaxHeight(Region.USE_PREF_SIZE);
        c.setPadding(new Insets(24, 26, 22, 26));
        c.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.28), 28, 0, 0, 8);");
        return c;
    }

    private static String fieldStyle() {
        return "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; "
            + "-fx-background-radius: 8; -fx-padding: 8 11; -fx-font-size: 13px;";
    }

    private static void showBackdrop(StackPane rootStack, StackPane backdrop, VBox card) {
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

    private static void dismissBackdrop(StackPane rootStack, StackPane backdrop) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), backdrop);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(backdrop));
        fadeOut.play();
    }
}