package com.omnirig.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void handleEmployeeLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Hardcoded credentials για την ώρα
        if ("admin".equals(username) && "1234".equals(password)) {
            loadScene("/com/omnirig/views/EmployeeDashboard.fxml", "OmniRig - Πίνακας Υπαλλήλων");
        } else {
            errorLabel.setText("Λάθος στοιχεία. Δοκιμάστε ξανά.");
        }
    }

    @FXML
    public void handleCustomerContinue() {
        // Θα το φτιάξουμε στο επόμενο βήμα
        loadScene("/com/omnirig/views/CustomerDashboard.fxml", "OmniRig - Κατάστημα");
    }

    // Μια γενική μέθοδος που την καλείς για να αλλάξεις οθόνη χωρίς να ανοίγεις νέο παράθυρο
    private void loadScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            // Παίρνουμε το τρέχον παράθυρο (Stage) μέσω του κουμπιού που πατήθηκε
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 800, 600));
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Σφάλμα φόρτωσης οθόνης.");
        }
    }
}