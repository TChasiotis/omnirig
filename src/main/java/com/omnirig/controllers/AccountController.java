package com.omnirig.controllers;

import com.omnirig.controllers.CheckoutController.CustomerProfile;
import com.omnirig.models.SavedBuild;
import com.omnirig.services.AuthService;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AccountController {

    @FXML private Label avatarInitialsLbl;
    @FXML private Label fullNameLbl;
    @FXML private Label emailHeaderLbl;

    @FXML private Button tabProfileBtn;
    @FXML private Button tabOrdersBtn;
    @FXML private Button tabBuildsBtn;
    @FXML private VBox tabContentBox;

    private AuthService authService;
    private CustomerProfile currentUser;
    private List<SavedBuild> savedBuilds;
    private Runnable onBack;
    private Consumer<CustomerProfile> onProfileUpdated;
    private Consumer<SavedBuild> onLoadBuild;
    private Consumer<SavedBuild> onDeleteBuild;
    private Runnable onLogout;
    private Runnable onAccountDeleted;

    private boolean isEditing = false;

    public void init(
            AuthService authService,
            CustomerProfile user,
            List<SavedBuild> savedBuilds,
            Runnable onBack,
            Consumer<CustomerProfile> onProfileUpdated,
            Consumer<SavedBuild> onLoadBuild,
            Consumer<SavedBuild> onDeleteBuild,
            Runnable onLogout,
            Runnable onAccountDeleted) {
        this.authService = authService;
        this.currentUser = user;
        this.savedBuilds = savedBuilds;
        this.onBack = onBack;
        this.onProfileUpdated = onProfileUpdated;
        this.onLoadBuild = onLoadBuild;
        this.onDeleteBuild = onDeleteBuild;
        this.onLogout = onLogout;
        this.onAccountDeleted = onAccountDeleted;

        updateHeader();
        showProfileTab();
    }

    private void updateHeader() {
        String f = currentUser.firstName().isBlank() ? "" : currentUser.firstName().substring(0, 1).toUpperCase();
        String l = currentUser.lastName().isBlank() ? "" : currentUser.lastName().substring(0, 1).toUpperCase();
        avatarInitialsLbl.setText(f + l);
        fullNameLbl.setText(currentUser.firstName() + " " + currentUser.lastName());
        emailHeaderLbl.setText(currentUser.email());
    }

    @FXML
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void handleLogout() {
        if (onLogout != null) onLogout.run();
    }

    // --- TAB 1: ΣΤΟΙΧΕΙΑ ΠΡΟΦΙΛ ---
    @FXML
    private void showProfileTab() {
        setActiveTab(tabProfileBtn);
        isEditing = false;
        renderProfileContent();
    }

    private void renderProfileContent() {
        tabContentBox.getChildren().clear();

        VBox card = createCard();

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(isEditing ? "Επεξεργασία Στοιχείων" : "Προσωπικά Στοιχεία & Διεύθυνση");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        cardHeader.getChildren().addAll(title, sp);

        if (!isEditing) {
            Button editBtn = new Button("Επεξεργασία");
            editBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; "
                + "-fx-padding: 7 16; -fx-background-radius: 8; -fx-border-color: #BFDBFE; -fx-border-radius: 8; -fx-cursor: hand;");
            editBtn.setOnAction(e -> {
                isEditing = true;
                renderProfileContent();
            });
            cardHeader.getChildren().add(editBtn);

            GridPane readGrid = new GridPane();
            readGrid.setHgap(24);
            readGrid.setVgap(16);
            ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
            ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
            readGrid.getColumnConstraints().addAll(c1, c2);

            readGrid.add(readOnlyField("Όνομα", currentUser.firstName()), 0, 0);
            readGrid.add(readOnlyField("Επώνυμο", currentUser.lastName()), 1, 0);
            readGrid.add(readOnlyField("Email", currentUser.email()), 0, 1);
            readGrid.add(readOnlyField("Τηλέφωνο", currentUser.phone()), 1, 1);
            readGrid.add(readOnlyField("Διεύθυνση Αποστολής", currentUser.address()), 0, 2);
            readGrid.add(readOnlyField("Πόλη / Τ.Κ.", currentUser.city() + ", " + currentUser.zip()), 1, 2);

            card.getChildren().addAll(cardHeader, new Separator(), readGrid);
        } else {
            TextField firstField = createTextField(currentUser.firstName());
            TextField lastField  = createTextField(currentUser.lastName());
            TextField emailField = createTextField(currentUser.email());
            emailField.setDisable(true);
            TextField phoneField = createTextField(currentUser.phone());
            TextField addrField  = createTextField(currentUser.address());
            TextField cityField  = createTextField(currentUser.city());
            TextField zipField   = createTextField(currentUser.zip());

            GridPane editGrid = new GridPane();
            editGrid.setHgap(16);
            editGrid.setVgap(12);
            ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
            ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
            editGrid.getColumnConstraints().addAll(c1, c2);

            editGrid.add(labeledInput("Όνομα *", firstField), 0, 0);
            editGrid.add(labeledInput("Επώνυμο *", lastField), 1, 0);
            editGrid.add(labeledInput("Email (Κλειδωμένο)", emailField), 0, 1);
            editGrid.add(labeledInput("Τηλέφωνο *", phoneField), 1, 1);
            editGrid.add(labeledInput("Οδός & Αριθμός *", addrField), 0, 2, 2, 1);
            editGrid.add(labeledInput("Πόλη *", cityField), 0, 3);
            editGrid.add(labeledInput("Τ.Κ. *", zipField), 1, 3);

            Label errorLbl = new Label();
            errorLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 12px;");
            errorLbl.setVisible(false);
            errorLbl.setManaged(false);

            Button cancelBtn = new Button("Ακύρωση");
            cancelBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; "
                + "-fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                isEditing = false;
                renderProfileContent();
            });

            Button saveBtn = new Button("Αποθήκευση Αλλαγών");
            saveBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                + "-fx-padding: 9 20; -fx-background-radius: 8; -fx-cursor: hand;");
            saveBtn.setOnAction(e -> {
                if (firstField.getText().isBlank() || lastField.getText().isBlank() || phoneField.getText().isBlank()
                        || addrField.getText().isBlank() || cityField.getText().isBlank() || zipField.getText().isBlank()) {
                    errorLbl.setText("Συμπλήρωσε όλα τα υποχρεωτικά πεδία (*).");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                    return;
                }
                CustomerProfile updated = new CustomerProfile(
                    firstField.getText().trim(), lastField.getText().trim(), currentUser.email(),
                    phoneField.getText().trim(), addrField.getText().trim(), cityField.getText().trim(), zipField.getText().trim()
                );
                authService.updateProfile(updated);
                currentUser = updated;
                updateHeader();
                if (onProfileUpdated != null) onProfileUpdated.accept(updated);
                isEditing = false;
                renderProfileContent();
            });

            HBox actions = new HBox(10, cancelBtn, saveBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            card.getChildren().addAll(cardHeader, new Separator(), editGrid, errorLbl, actions);
        }

        // Danger Zone
        HBox dangerCard = new HBox(16);
        dangerCard.setAlignment(Pos.CENTER_LEFT);
        dangerCard.setPadding(new Insets(18, 22, 18, 22));
        dangerCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #FECACA; -fx-border-radius: 14;");

        VBox dangerInfo = new VBox(3);
        Label dTitle = new Label("Διαγραφή Λογαριασμού");
        dTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #991B1B;");
        Label dSub = new Label("Η ενέργεια αυτή διαγράφει μόνιμα τον λογαριασμό και τα στοιχεία σου από τη βάση.");
        dSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        dangerInfo.getChildren().addAll(dTitle, dSub);
        HBox.setHgrow(dangerInfo, Priority.ALWAYS);

        Button deleteBtn = new Button("Διαγραφή");
        deleteBtn.setMinWidth(Region.USE_PREF_SIZE);
        deleteBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; "
            + "-fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");

        final boolean[] armed = {false};
        deleteBtn.setOnAction(e -> {
            if (!armed[0]) {
                armed[0] = true;
                deleteBtn.setText("Επιβεβαίωση Διαγραφής;");
                deleteBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                    + "-fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                authService.deleteAccount(currentUser.email());
                if (onAccountDeleted != null) onAccountDeleted.run();
            }
        });

        dangerCard.getChildren().addAll(dangerInfo, deleteBtn);
        tabContentBox.getChildren().addAll(card, dangerCard);
    }

    // --- TAB 2: ΟΙ ΠΑΡΑΓΓΕΛΙΕΣ ΜΟΥ ---
    @FXML
    private void showOrdersTab() {
        setActiveTab(tabOrdersBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();
        Label title = new Label("Ιστορικό Παραγγελιών");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label empty = new Label("Δεν υπάρχουν ακόμα καταχωρημένες παραγγελίες. Όταν συνδεθεί η βάση δεδομένων, εδώ θα εμφανίζονται οι παραγγελίες σου με κατάσταση και αναλυτικά προϊόντα.");
        empty.setWrapText(true);
        empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-padding: 12 0;");

        card.getChildren().addAll(title, new Separator(), empty);
        tabContentBox.getChildren().add(card);
    }

    // --- TAB 3: ΑΠΟΘΗΚΕΥΜΕΝΑ BUILDS ---
    @FXML
    private void showBuildsTab() {
        setActiveTab(tabBuildsBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();
        Label title = new Label("Αποθηκευμένα PC Builds (" + (savedBuilds == null ? 0 : savedBuilds.size()) + ")");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        card.getChildren().addAll(title, new Separator());

        if (savedBuilds == null || savedBuilds.isEmpty()) {
            Label empty = new Label("Δεν έχεις αποθηκεύσει ακόμα κάποιο PC Build. Φτιάξε ένα σύστημα στον PC Builder και πάτα «Αποθήκευση».");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-padding: 12 0;");
            card.getChildren().add(empty);
        } else {
            for (SavedBuild sb : List.copyOf(savedBuilds)) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14, 16, 14, 16));
                row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

                VBox info = new VBox(4);
                Label nameLbl = new Label(sb.name() + "  (" + sb.parts().size() + " εξαρτήματα)");
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

                String summary = sb.parts().entrySet().stream()
                    .map(e -> e.getKey().name() + ": " + e.getValue().getName())
                    .collect(Collectors.joining("  •  "));
                Label partsLbl = new Label(summary);
                partsLbl.setWrapText(true);
                partsLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
                info.getChildren().addAll(nameLbl, partsLbl);
                HBox.setHgrow(info, Priority.ALWAYS);

                Label priceLbl = new Label(String.format("%.2f €", sb.totalPrice()));
                priceLbl.setMinWidth(Region.USE_PREF_SIZE);
                priceLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2563EB;");

                Button loadBtn = new Button("Φόρτωση");
                loadBtn.setMinWidth(Region.USE_PREF_SIZE);
                loadBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");
                loadBtn.setOnAction(e -> {
                    if (onLoadBuild != null) onLoadBuild.accept(sb);
                });

                Button delBtn = new Button("✕");
                delBtn.setMinWidth(Region.USE_PREF_SIZE);
                delBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-padding: 7 11; -fx-background-radius: 8; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    if (onDeleteBuild != null) onDeleteBuild.accept(sb);
                    showBuildsTab();
                });

                row.getChildren().addAll(info, priceLbl, loadBtn, delBtn);
                card.getChildren().add(row);
            }
        }

        tabContentBox.getChildren().add(card);
    }

    // --- UI HELPERS ---
    private void setActiveTab(Button active) {
        for (Button btn : new Button[]{tabProfileBtn, tabOrdersBtn, tabBuildsBtn}) {
            boolean isSel = (btn == active);
            btn.setStyle(isSel
                ? "-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 20; -fx-cursor: hand;"
                : "-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-radius: 20; -fx-cursor: hand;");
        }
    }

    private VBox createCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #E2E8F0; -fx-border-radius: 14;");
        return card;
    }

    private VBox readOnlyField(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        return new VBox(3, l, v);
    }

    private VBox labeledInput(String label, TextField field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        return new VBox(4, l, field);
    }

    private TextField createTextField(String value) {
        TextField tf = new TextField(value);
        tf.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 11; -fx-font-size: 13px;");
        return tf;
    }
}