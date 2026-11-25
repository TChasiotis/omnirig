package com.omnirig.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.omnirig.controllers.admin.AdminUiHelper.createCard;

public class AdminLogsTab {

    private record AuditLogRow(String timestamp, String action, String entity, String details) {}

    public Node build() {
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
        return card;
    }
}