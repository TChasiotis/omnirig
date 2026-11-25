package com.omnirig.controllers.admin;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.GeminiSpecService;
import com.omnirig.utils.ModalOverlayHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.omnirig.controllers.admin.AdminUiHelper.*;

public class AdminProductsTab {

    private final StackPane rootStack;
    private final ObservableList<Product> productsList;
    private final GeminiSpecService geminiSpecService;
    private final Runnable onRefreshProducts;

    public AdminProductsTab(
        StackPane rootStack,
        ObservableList<Product> productsList,
        GeminiSpecService geminiSpecService,
        Runnable onRefreshProducts) {
        this.rootStack = rootStack;
        this.productsList = productsList;
        this.geminiSpecService = geminiSpecService;
        this.onRefreshProducts = onRefreshProducts;
    }

    public Node build() {
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
            // TODO (Backend): Κλήση Hibernate delete() / Stored Procedure για το sel και μετά onRefreshProducts.run();
            productsList.remove(sel);
            statusLbl.setText("Το προϊόν «" + sel.getName() + "» διαγράφηκε.");
        });

        HBox actions = new HBox(10, editBtn, delBtn, statusLbl);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(topRow, new Separator(), table, actions);
        return card;
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
                    // TODO (Backend): Κλήση Hibernate update() για το existing και μετά onRefreshProducts.run();
                    existing.setName(name);
                    existing.setCategory(catCombo.getValue());
                    existing.setPrice(price);
                    existing.setStock(stock);
                    existing.getSpecs().clear();
                    existing.getSpecs().putAll(workingSpecs);
                    table.refresh();
                    statusLbl.setText("Ενημερώθηκε: " + name);
                } else {
                    // TODO (Backend): Κλήση Hibernate save() / Stored Procedure για νέο προϊόν και μετά onRefreshProducts.run();
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
}