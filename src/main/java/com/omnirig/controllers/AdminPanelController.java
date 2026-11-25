package com.omnirig.controllers;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.CompatibilityEngine;
import com.omnirig.services.GeminiSpecService;
import com.omnirig.services.ProductService;
import com.omnirig.utils.ModalOverlayHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminPanelController {

    @FXML private Button tabProductsBtn;
    @FXML private Button tabLogsBtn;
    @FXML private Button tabQueriesBtn;
    @FXML private Button tabConcurrencyBtn;
    @FXML private VBox tabContentBox;

    private StackPane rootStack;
    private ProductService productService;
    private Runnable onExitAdmin;

    private final GeminiSpecService geminiSpecService = new GeminiSpecService();
    private ObservableList<Product> productsList;

    public void init(StackPane rootStack, ProductService productService, Runnable onExitAdmin) {
        this.rootStack = rootStack;
        this.productService = productService;
        this.onExitAdmin = onExitAdmin;
        this.productsList = FXCollections.observableArrayList(productService.getAllProducts());
        showProductsTab();
    }

    @FXML
    private void handleExitAdmin() {
        if (onExitAdmin != null) onExitAdmin.run();
    }

    // =========================================================================
    // TAB 1: ΔΙΑΧΕΙΡΙΣΗ ΠΡΟΪΟΝΤΩΝ (CRUD + GEMINI AUTO-FILL SPECS)
    // =========================================================================
    @FXML
    private void showProductsTab() {
        setActiveTab(tabProductsBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Κατάλογος Προϊόντων & Αποθέματος");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("+ Προσθήκη Προϊόντος");
        addBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        topRow.getChildren().addAll(title, sp, addBtn);

        TableView<Product> table = new TableView<>(productsList);
        table.setPrefHeight(360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Product, String> colName = new TableColumn<>("Προϊόν");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Product, String> colCat = new TableColumn<>("Κατηγορία");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().name()));
        colCat.setMaxWidth(130);

        TableColumn<Product, String> colPrice = new TableColumn<>("Τιμή");
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getPrice())));
        colPrice.setMaxWidth(110);

        TableColumn<Product, String> colStock = new TableColumn<>("Απόθεμα");
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));
        colStock.setMaxWidth(95);

        table.getColumns().addAll(colName, colCat, colPrice, colStock);

        Button editBtn = new Button("Επεξεργασία");
        editBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");

        Button delBtn = new Button("Διαγραφή");
        delBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");

        Label statusLbl = new Label("Επίλεξε ένα προϊόν για επεξεργασία ή διαγραφή.");
        statusLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        addBtn.setOnAction(e -> openProductFormModal(null, table, statusLbl));

        editBtn.setOnAction(e -> {
            Product sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                statusLbl.setText("Επίλεξε πρώτα ένα προϊόν από τη λίστα.");
                return;
            }
            openProductFormModal(sel, table, statusLbl);
        });

        delBtn.setOnAction(e -> {
            Product sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                statusLbl.setText("Επίλεξε πρώτα ένα προϊόν από τη λίστα.");
                return;
            }
            // TODO (Backend): Κλήση Hibernate delete() / Stored Procedure για το sel
            productsList.remove(sel);
            statusLbl.setText("Το προϊόν «" + sel.getName() + "» διαγράφηκε.");
        });

        HBox actions = new HBox(10, editBtn, delBtn, statusLbl);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(topRow, new Separator(), table, actions);
        tabContentBox.getChildren().add(card);
    }

    private void openProductFormModal(Product existing, TableView<Product> table, Label statusLbl) {
        boolean isEdit = existing != null;
        Map<String, Object> workingSpecs = new LinkedHashMap<>();
        if (isEdit && existing.getSpecs() != null) {
            workingSpecs.putAll(existing.getSpecs());
        }

        TextField nameField = new TextField(isEdit ? existing.getName() : "");
        nameField.setPromptText("π.χ. AMD Ryzen 7 9800X3D *");
        nameField.setStyle(inputStyle());

        ComboBox<Category> catCombo = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        catCombo.setValue(isEdit ? existing.getCategory() : Category.CPU);
        catCombo.setMaxWidth(Double.MAX_VALUE);

        TextField priceField = new TextField(isEdit ? String.valueOf(existing.getPrice()) : "");
        priceField.setPromptText("Τιμή σε € (π.χ. 299.90) *");
        priceField.setStyle(inputStyle());

        TextField stockField = new TextField(isEdit ? String.valueOf(existing.getStock()) : "");
        stockField.setPromptText("Απόθεμα (π.χ. 10) *");
        stockField.setStyle(inputStyle());

        HBox priceStockRow = new HBox(10,
            new VBox(4, fieldLbl("Τιμή (€) *"), priceField),
            new VBox(4, fieldLbl("Απόθεμα (Τεμ.) *"), stockField)
        );
        HBox.setHgrow(priceStockRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(priceStockRow.getChildren().get(1), Priority.ALWAYS);

        // AI Auto-Fill Specs Section
        Button aiFillBtn = new Button("✨ Auto-Fill Specs (AI)");
        aiFillBtn.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #38BDF8; -fx-font-weight: bold; "
            + "-fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox specsHeader = new HBox(10, fieldLbl("Τεχνικά Χαρακτηριστικά (JSON Specs)"), new Region(), aiFillBtn);
        specsHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(specsHeader.getChildren().get(1), Priority.ALWAYS);

        TextArea specsPreviewArea = new TextArea(formatSpecsPreview(workingSpecs));
        specsPreviewArea.setEditable(false);
        specsPreviewArea.setPrefRowCount(4);
        specsPreviewArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        Label errLbl = new Label();
        errLbl.setWrapText(true);
        errLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-weight: bold;");
        errLbl.setVisible(false);
        errLbl.setManaged(false);

        aiFillBtn.setOnAction(e -> {
            String pName = nameField.getText().trim();
            if (pName.isEmpty()) {
                showErr(errLbl, "Γράψε πρώτα το όνομα του προϊόντος για να βρει τα specs το AI.");
                return;
            }
            errLbl.setVisible(false);
            errLbl.setManaged(false);
            aiFillBtn.setDisable(true);
            aiFillBtn.setText("Αναζήτηση specs...");
            specsPreviewArea.setText("Αναζήτηση τεχνικών χαρακτηριστικών για «" + pName + "»...");

            geminiSpecService.fetchSpecsAsync(pName, catCombo.getValue())
                .thenAccept(specs -> Platform.runLater(() -> {
                    workingSpecs.clear();
                    workingSpecs.putAll(specs);
                    specsPreviewArea.setText(formatSpecsPreview(workingSpecs));
                    aiFillBtn.setDisable(false);
                    aiFillBtn.setText("✨ Auto-Fill Specs (AI)");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        showErr(errLbl, cause.getMessage());
                        specsPreviewArea.setText(formatSpecsPreview(workingSpecs));
                        aiFillBtn.setDisable(false);
                        aiFillBtn.setText("✨ Auto-Fill Specs (AI)");
                    });
                    return null;
                });
        });

        VBox form = new VBox(8,
            fieldLbl("Όνομα Προϊόντος *"), nameField,
            fieldLbl("Κατηγορία *"), catCombo,
            priceStockRow,
            specsHeader,
            specsPreviewArea,
            errLbl
        );

        ModalOverlayHelper.showCustomModal(
            rootStack,
            isEdit ? "Επεξεργασία Προϊόντος" : "Προσθήκη Νέου Προϊόντος",
            isEdit ? "Τροποποίησε τα στοιχεία ή ανανέωσε τα specs με AI." : "Γράψε όνομα & κατηγορία και πάτα Auto-Fill Specs (AI).",
            form,
            isEdit ? "Αποθήκευση" : "Προσθήκη",
            () -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showErr(errLbl, "Το όνομα είναι υποχρεωτικό.");
                    return false;
                }
                double price;
                int stock;
                try {
                    price = Double.parseDouble(priceField.getText().replace(",", ".").trim());
                    stock = Integer.parseInt(stockField.getText().trim());
                    if (price < 0 || stock < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    showErr(errLbl, "Δώσε έγκυρη τιμή και ακέραιο απόθεμα.");
                    return false;
                }

                if (isEdit) {
                    // TODO (Backend): Κλήση Hibernate update() για το existing
                    existing.setName(name);
                    existing.setCategory(catCombo.getValue());
                    existing.setPrice(price);
                    existing.setStock(stock);
                    existing.getSpecs().clear();
                    existing.getSpecs().putAll(workingSpecs);
                    table.refresh();
                    statusLbl.setText("Ενημερώθηκε: " + name);
                } else {
                    // TODO (Backend): Κλήση Hibernate save() / Stored Procedure για νέο προϊόν
                    Product created = new Product();
                    created.setName(name);
                    created.setCategory(catCombo.getValue());
                    created.setPrice(price);
                    created.setStock(stock);
                    created.getSpecs().putAll(workingSpecs);
                    productsList.add(created);
                    statusLbl.setText("Προστέθηκε: " + name + " (" + workingSpecs.size() + " specs)");
                }
                return true;
            }
        );
    }

    private void refreshProductsTable() {
        productsList.setAll(productService.getAllProducts());
    }

    private String formatSpecsPreview(Map<String, Object> specs) {
        if (specs == null || specs.isEmpty()) {
            return "{\n  // Πάτα «Auto-Fill Specs (AI)» για αυτόματη συμπλήρωση\n}";
        }
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, Object> e : specs.entrySet()) {
            sb.append("  \"").append(e.getKey()).append("\": ");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else if (v instanceof List<?> list) {
                sb.append(list.stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(", ", "[", "]")));
            } else {
                sb.append("\"").append(v).append("\"");
            }
            if (++i < specs.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // TAB 2: ΙΣΤΟΡΙΚΟ ΕΝΕΡΓΕΙΩΝ (AUDIT LOG)
    // =========================================================================
    private record AuditLogRow(String timestamp, String action, String entity, String details) {}

    @FXML
    private void showLogsTab() {
        setActiveTab(tabLogsBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Ιστορικό Ενεργειών Συστήματος");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Ανανέωση");
        refreshBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");
        topRow.getChildren().addAll(title, sp, refreshBtn);

        TableView<AuditLogRow> logTable = new TableView<>();
        logTable.setPrefHeight(360);
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<AuditLogRow, String> cTime = new TableColumn<>("Ημερομηνία / Ώρα");
        cTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timestamp()));
        cTime.setMaxWidth(170);

        TableColumn<AuditLogRow, String> cAct = new TableColumn<>("Ενέργεια");
        cAct.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().action()));
        cAct.setMaxWidth(110);

        TableColumn<AuditLogRow, String> cEnt = new TableColumn<>("Ενότητα");
        cEnt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().entity()));
        cEnt.setMaxWidth(130);

        TableColumn<AuditLogRow, String> cDet = new TableColumn<>("Περιγραφή");
        cDet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().details()));

        logTable.getColumns().addAll(cTime, cAct, cEnt, cDet);

        // TODO (Backend): Φόρτωση εγγραφών από τον πίνακα audit_logs (που γεμίζει αυτόματα από DB Trigger)
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logTable.setItems(FXCollections.observableArrayList(
            new AuditLogRow(now, "INSERT", "Παραγγελίες", "Νέα παραγγελία #1042 — Σύνολο: 698,30 €"),
            new AuditLogRow(now, "UPDATE", "Αποθήκη", "Ενημέρωση αποθέματος: AMD Ryzen 5 3600 (15 → 14)"),
            new AuditLogRow(now, "DELETE", "Χρήστες", "Αφαίρεση λογαριασμού χρήστη")
        ));

        refreshBtn.setOnAction(e -> {
            // TODO (Backend): Επαναφόρτωση από audit_logs
        });

        card.getChildren().addAll(topRow, new Separator(), logTable);
        tabContentBox.getChildren().add(card);
    }

    // =========================================================================
    // TAB 3: ΑΝΑΦΟΡΕΣ & ΣΤΑΤΙΣΤΙΚΑ (ΔΥΝΑΜΙΚΑ ΠΕΔΙΑ ΑΝΑ ΑΝΑΦΟΡΑ)
    // =========================================================================
    @FXML
    private void showQueriesTab() {
        setActiveTab(tabQueriesBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();
        Label title = new Label("Αναφορές Καταστήματος & Αποθήκης");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        ComboBox<String> reportCombo = new ComboBox<>();
        reportCombo.getItems().addAll(
            "Προϊόντα ανά Κατηγορία & Εύρος Τιμής",
            "Εξαρτήματα με Χαμηλό Απόθεμα",
            "Ιστορικό Παραγγελιών Πελάτη",
            "Σύνοψη Αξίας & Μέσης Τιμής ανά Κατηγορία",
            "Διαθέσιμοι Επεξεργαστές (CPU) ανά Socket"
        );
        reportCombo.getSelectionModel().selectFirst();
        reportCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Category> catFilter = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        catFilter.setValue(Category.CPU);

        TextField minPriceField = new TextField("0");
        minPriceField.setPromptText("Από €");
        minPriceField.setPrefWidth(95);
        minPriceField.setStyle(inputStyle());

        TextField maxPriceField = new TextField("500");
        maxPriceField.setPromptText("Έως €");
        maxPriceField.setPrefWidth(95);
        maxPriceField.setStyle(inputStyle());

        TextField stockLimitField = new TextField("10");
        stockLimitField.setPromptText("Όριο (π.χ. 10)");
        stockLimitField.setPrefWidth(140);
        stockLimitField.setStyle(inputStyle());

        TextField emailFilterField = new TextField("demo@omnirig.gr");
        emailFilterField.setPromptText("Email πελάτη (ή κενό για όλους)");
        emailFilterField.setPrefWidth(240);
        emailFilterField.setStyle(inputStyle());

        TextField socketField = new TextField("AM4");
        socketField.setPromptText("Socket (π.χ. AM4, AM5, LGA1700)");
        socketField.setPrefWidth(180);
        socketField.setStyle(inputStyle());

        HBox dynamicParamsBox = new HBox(10);
        dynamicParamsBox.setAlignment(Pos.CENTER_LEFT);

        Button runBtn = new Button("Δημιουργία Αναφοράς");
        runBtn.setMinWidth(Region.USE_PREF_SIZE);
        runBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox controlsRow = new HBox(12, dynamicParamsBox, runBtn);
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dynamicParamsBox, Priority.ALWAYS);

        Runnable updateParamsUi = () -> {
            dynamicParamsBox.getChildren().clear();
            int idx = reportCombo.getSelectionModel().getSelectedIndex();
            switch (idx) {
                case 0 -> dynamicParamsBox.getChildren().addAll(fieldLbl("Κατηγορία:"), catFilter, fieldLbl("Από €:"), minPriceField, fieldLbl("Έως €:"), maxPriceField);
                case 1 -> dynamicParamsBox.getChildren().addAll(fieldLbl("Όριο Stock (<):"), stockLimitField);
                case 2 -> dynamicParamsBox.getChildren().addAll(fieldLbl("Email Πελάτη:"), emailFilterField);
                case 3 -> dynamicParamsBox.getChildren().addAll(new Label("Υπολογίζεται αυτόματα για όλες τις κατηγορίες."));
                case 4 -> dynamicParamsBox.getChildren().addAll(fieldLbl("Socket:"), socketField);
            }
        };
        reportCombo.setOnAction(e -> updateParamsUi.run());
        updateParamsUi.run();

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(260);
        resultArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        resultArea.setText("Επίλεξε αναφορά, όρισε τα κριτήρια και πάτα «Δημιουργία Αναφοράς».");

        runBtn.setOnAction(e -> {
            int idx = reportCombo.getSelectionModel().getSelectedIndex();
            // TODO (Backend): Αντικατάσταση με HQL / JPQL queries ή Stored Procedures από PostgreSQL
            StringBuilder sb = new StringBuilder();
            switch (idx) {
                case 0 -> {
                    double min = parseDouble(minPriceField.getText(), 0);
                    double max = parseDouble(maxPriceField.getText(), Double.MAX_VALUE);
                    List<Product> res = productsList.stream()
                        .filter(p -> p.getCategory() == catFilter.getValue() && p.getPrice() >= min && p.getPrice() <= max)
                        .toList();
                    sb.append(String.format("Κατηγορία: %s | Εύρος Τιμής: %.2f € - %.2f €\n", catFilter.getValue(), min, max));
                    sb.append("Βρέθηκαν ").append(res.size()).append(" προϊόντα:\n--------------------------------------------------\n");
                    if (res.isEmpty()) sb.append("Κανένα προϊόν δεν πληροί τα κριτήρια.\n");
                    res.forEach(p -> sb.append(String.format("- %s | %.2f € | Stock: %d\n", p.getName(), p.getPrice(), p.getStock())));
                }
                case 1 -> {
                    int limit = (int) parseDouble(stockLimitField.getText(), 10);
                    List<Product> res = productsList.stream()
                        .filter(p -> p.getStock() < limit)
                        .toList();
                    sb.append("Εξαρτήματα με απόθεμα < ").append(limit).append(" τεμάχια (Βρέθηκαν: ").append(res.size()).append("):\n");
                    sb.append("--------------------------------------------------\n");
                    if (res.isEmpty()) sb.append("Όλα τα προϊόντα έχουν απόθεμα >= ").append(limit).append(".\n");
                    res.forEach(p -> sb.append(String.format("- [%-11s] %-35s | Stock: %d\n", p.getCategory().name(), p.getName(), p.getStock())));
                }
                case 2 -> {
                    String email = emailFilterField.getText().trim().toLowerCase();
                    List<String[]> mockOrders = List.of(
                        new String[]{"#1042", "demo@omnirig.gr", "2026-11-23", "Ολοκληρωμένη", "698,30 €"},
                        new String[]{"#1039", "demo@omnirig.gr", "2026-11-18", "Σε επεξεργασία", "259,00 €"},
                        new String[]{"#1035", "test@omnirig.gr", "2026-11-15", "Απεστάλη", "139,80 €"}
                    );
                    List<String[]> res = mockOrders.stream()
                        .filter(o -> email.isEmpty() || o[1].equalsIgnoreCase(email))
                        .toList();
                    sb.append("Παραγγελίες για: ").append(email.isEmpty() ? "Όλους τους πελάτες" : email).append("\n");
                    sb.append("--------------------------------------------------\n");
                    if (res.isEmpty()) sb.append("Δεν βρέθηκαν παραγγελίες για αυτό το email.\n");
                    res.forEach(o -> sb.append(String.format("- %s | %s | %s | %-15s | %s\n", o[0], o[2], o[1], o[3], o[4])));
                }
                case 3 -> {
                    Map<Category, List<Product>> grouped = productsList.stream().collect(Collectors.groupingBy(Product::getCategory));
                    sb.append("Σύνοψη Αποθήκης ανά Κατηγορία:\n--------------------------------------------------\n");
                    grouped.forEach((cat, list) -> {
                        double avg = list.stream().mapToDouble(Product::getPrice).average().orElse(0);
                        double totalVal = list.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
                        sb.append(String.format("- %-12s | Προϊόντα: %2d | Μέση Τιμή: %7.2f € | Αξία Αποθέματος: %9.2f €\n",
                            cat.name(), list.size(), avg, totalVal));
                    });
                }
                case 4 -> {
                    String sock = socketField.getText().trim();
                    List<Product> res = productsList.stream()
                        .filter(p -> p.getCategory() == Category.CPU)
                        .filter(p -> sock.isEmpty() || CompatibilityEngine.getString(p, "socket").equalsIgnoreCase(sock))
                        .toList();
                    sb.append("Διαθέσιμοι CPU για Socket: ").append(sock.isEmpty() ? "Όλα" : sock).append(" (Βρέθηκαν: ").append(res.size()).append(")\n");
                    sb.append("--------------------------------------------------\n");
                    if (res.isEmpty()) sb.append("Δεν βρέθηκαν επεξεργαστές για αυτό το socket.\n");
                    res.forEach(p -> sb.append(String.format("- %s | Socket: %s | %.2f € | Stock: %d\n",
                        p.getName(), CompatibilityEngine.getString(p, "socket"), p.getPrice(), p.getStock())));
                }
            }
            resultArea.setText(sb.toString());
        });

        card.getChildren().addAll(title, new Separator(), reportCombo, controlsRow, resultArea);
        tabContentBox.getChildren().add(card);
    }

    // =========================================================================
    // TAB 4: ΔΙΑΓΝΩΣΤΙΚΑ ΣΥΣΤΗΜΑΤΟΣ (CHECKOUT STRESS TEST)
    // =========================================================================
    @FXML
    private void showConcurrencyTab() {
        setActiveTab(tabConcurrencyBtn);
        tabContentBox.getChildren().clear();

        VBox card = createCard();
        Label title = new Label("Έλεγχος Αντοχής Αποθέματος (Checkout Stress Test)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label desc = new Label("Δοκιμάζει την αντοχή του συστήματος παραγγελιών όταν δύο πελάτες επιχειρούν να αγοράσουν το τελευταίο διαθέσιμο τεμάχιο την ίδια στιγμή.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(
            "Standard Mode (READ COMMITTED)",
            "Repeatable Read Mode",
            "Strict Lock Mode (SERIALIZABLE / Pessimistic Lock)"
        );
        modeCombo.getSelectionModel().selectLast();

        Button simBtn = new Button("Εκτέλεση Δοκιμής");
        simBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox row = new HBox(12, modeCombo, simBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(260);
        consoleArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        consoleArea.setText("Έτοιμο για εκτέλεση διαγνωστικού ελέγχου.");

        simBtn.setOnAction(e -> {
            String mode = modeCombo.getValue();
            // TODO (Backend): Εκτέλεση 2 concurrent threads με Hibernate Sessions & setTransactionIsolation(...)
            consoleArea.setText(
                "Έναρξη διαγνωστικού ελέγχου (" + mode + ")\n" +
                "--------------------------------------------------\n" +
                "[Session A] Αίτημα αγοράς (διαθέσιμο απόθεμα: 1)...\n" +
                "[Session B] Ταυτόχρονο αίτημα αγοράς για το ίδιο προϊόν...\n" +
                "Αναμονή σύνδεσης με το transactional backend για εκτέλεση ζωντανής δοκιμής."
            );
        });

        card.getChildren().addAll(title, desc, new Separator(), row, consoleArea);
        tabContentBox.getChildren().add(card);
    }

    // --- UI HELPERS ---
    private void setActiveTab(Button active) {
        for (Button btn : new Button[]{tabProductsBtn, tabLogsBtn, tabQueriesBtn, tabConcurrencyBtn}) {
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

    private Label fieldLbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        return l;
    }

    private String inputStyle() {
        return "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10; -fx-font-size: 13px;";
    }

    private void showErr(Label l, String msg) {
        l.setText(msg);
        l.setVisible(true);
        l.setManaged(true);
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.replace(",", ".").trim()); }
        catch (Exception e) { return def; }
    }
}