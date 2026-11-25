package com.omnirig.controllers.admin;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.CompatibilityEngine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.omnirig.controllers.admin.AdminUiHelper.*;

public class AdminReportsTab {

    private final ObservableList<Product> productsList;

    public AdminReportsTab(ObservableList<Product> productsList) {
        this.productsList = productsList;
    }

    public Node build() {
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
        return card;
    }
}