package com.omnirig.controllers;

import com.omnirig.controllers.admin.*;
import com.omnirig.models.Product;
import com.omnirig.services.GeminiSpecService;
import com.omnirig.services.ProductService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AdminPanelController {

    @FXML private Button tabProductsBtn;
    @FXML private Button tabLogsBtn;
    @FXML private Button tabQueriesBtn;
    @FXML private Button tabConcurrencyBtn;
    @FXML private VBox tabContentBox;

    private ProductService productService;
    private Runnable onExitAdmin;
    private ObservableList<Product> productsList;

    private final GeminiSpecService geminiSpecService = new GeminiSpecService();

    private AdminProductsTab productsTab;
    private AdminLogsTab logsTab;
    private AdminReportsTab reportsTab;
    private AdminConcurrencyTab concurrencyTab;

    public void init(StackPane rootStack, ProductService productService, Runnable onExitAdmin) {
        this.productService = productService;
        this.onExitAdmin = onExitAdmin;
        this.productsList = FXCollections.observableArrayList(productService.getAllProducts());

        this.productsTab = new AdminProductsTab(rootStack, productsList, geminiSpecService, this::refreshProductsTable);
        this.logsTab = new AdminLogsTab();
        this.reportsTab = new AdminReportsTab(productsList);
        this.concurrencyTab = new AdminConcurrencyTab(productsList);

        showProductsTab();
    }

    public void refreshProductsTable() {
        productsList.setAll(productService.getAllProducts());
    }

    @FXML
    private void handleExitAdmin() {
        if (onExitAdmin != null) onExitAdmin.run();
    }

    @FXML
    private void showProductsTab() {
        showTab(tabProductsBtn, productsTab.build());
    }

    @FXML
    private void showLogsTab() {
        showTab(tabLogsBtn, logsTab.build());
    }

    @FXML
    private void showQueriesTab() {
        showTab(tabQueriesBtn, reportsTab.build());
    }

    @FXML
    private void showConcurrencyTab() {
        showTab(tabConcurrencyBtn, concurrencyTab.build());
    }

    private void showTab(Button activeBtn, Node content) {
        for (Button btn : new Button[]{tabProductsBtn, tabLogsBtn, tabQueriesBtn, tabConcurrencyBtn}) {
            boolean isSel = (btn == activeBtn);
            btn.setStyle(isSel
                ? "-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 20; -fx-cursor: hand;"
                : "-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-radius: 20; -fx-cursor: hand;");
        }
        tabContentBox.getChildren().setAll(content);
    }
}