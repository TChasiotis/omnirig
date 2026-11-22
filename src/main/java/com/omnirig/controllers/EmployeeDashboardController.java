package com.omnirig.controllers;

import com.omnirig.models.Product;
import com.omnirig.services.MockProductService;
import com.omnirig.services.ProductService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class EmployeeDashboardController {
    
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;

    // Τα νέα πεδία για τα φίλτρα
    @FXML private TextField searchNameField;
    @FXML private TextField maxPriceField;

    private ProductService productService = new MockProductService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        loadAllProducts();
    }

    // Μέθοδος που φορτώνει όλα τα δεδομένα (τη βάλαμε ξεχωριστά για να την καλούμε εύκολα)
    private void loadAllProducts() {
        ObservableList<Product> products = FXCollections.observableArrayList(productService.getAllProducts());
        productTable.setItems(products);
    }

    // Όταν πατάει "Φιλτράρισμα"
    @FXML
    public void handleSearch() {
        String keyword = searchNameField.getText();
        Double maxPrice = null;

        if (!maxPriceField.getText().isEmpty()) {
            try {
                maxPrice = Double.parseDouble(maxPriceField.getText());
            } catch (NumberFormatException e) {
                System.out.println("Βάλε σωστό αριθμό στην τιμή!");
            }
        }

        // Εδώ καλεί το δυναμικό ερώτημα από το Service (που το είχαμε γράψει στο Mock)
        ObservableList<Product> filteredProducts = FXCollections.observableArrayList(
            productService.searchProducts(keyword, maxPrice, null)
        );
        productTable.setItems(filteredProducts);
    }

    // Όταν πατάει "Καθαρισμός"
    @FXML
    public void handleClear() {
        searchNameField.clear();
        maxPriceField.clear();
        loadAllProducts();
    }
}