package com.omnirig.controllers;

import com.omnirig.models.Product;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

public class CheckoutController {

    public record CustomerProfile(
        String firstName, String lastName, String email, String phone,
        String address, String city, String zip
    ) {}

    @FXML private Label modeBadgeLbl;
    @FXML private TextField firstNameField, lastNameField, emailField, phoneField;
    @FXML private TextField shipAddressField, shipCityField, shipZipField;
    @FXML private CheckBox sameAsShippingCheck;
    @FXML private VBox billingBox;
    @FXML private TextField billAddressField, billCityField, billZipField;

    @FXML private RadioButton shipCourierRadio, shipPickupRadio;
    @FXML private RadioButton payCardRadio, payCodRadio, payBankRadio;
    @FXML private HBox stripeInfoBox;

    @FXML private RadioButton receiptRadio, invoiceRadio;
    @FXML private HBox invoiceBox;
    @FXML private TextField afmField, doyField, companyField;
    @FXML private TextArea notesArea;
    @FXML private Label errorLbl;

    @FXML private VBox orderItemsMiniList;
    @FXML private Label subLbl, extraLbl, grandTotalLbl;

    private Map<Product, Integer> cart;
    private Runnable onBackToCart;
    private Runnable onOrderSuccess;
    private double subtotal = 0.0;

    @FXML
    public void initialize() {
        ToggleGroup shipGroup = new ToggleGroup();
        shipCourierRadio.setToggleGroup(shipGroup);
        shipPickupRadio.setToggleGroup(shipGroup);

        ToggleGroup payGroup = new ToggleGroup();
        payCardRadio.setToggleGroup(payGroup);
        payCodRadio.setToggleGroup(payGroup);
        payBankRadio.setToggleGroup(payGroup);

        ToggleGroup docGroup = new ToggleGroup();
        receiptRadio.setToggleGroup(docGroup);
        invoiceRadio.setToggleGroup(docGroup);
    }

    public void init(Map<Product, Integer> cart, CustomerProfile loggedInUser, Runnable onBackToCart, Runnable onOrderSuccess) {
        this.cart = cart;
        this.onBackToCart = onBackToCart;
        this.onOrderSuccess = onOrderSuccess;

        if (loggedInUser != null) {
            modeBadgeLbl.setText("Συνδεδεμένος: " + loggedInUser.firstName() + " " + loggedInUser.lastName());
            modeBadgeLbl.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 12px;");
            firstNameField.setText(loggedInUser.firstName());
            lastNameField.setText(loggedInUser.lastName());
            emailField.setText(loggedInUser.email());
            phoneField.setText(loggedInUser.phone());
            shipAddressField.setText(loggedInUser.address());
            shipCityField.setText(loggedInUser.city());
            shipZipField.setText(loggedInUser.zip());
        } else {
            modeBadgeLbl.setText("Αγορά ως Επισκέπτης");
        }

        populateMiniList();
        recalculateTotals();
    }

    private void populateMiniList() {
        orderItemsMiniList.getChildren().clear();
        subtotal = 0.0;
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            double line = e.getKey().getPrice() * e.getValue();
            subtotal += line;
            HBox row = new HBox(8);
            Label name = new Label(e.getValue() + "x " + e.getKey().getName());
            name.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
            name.setMaxWidth(190);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label price = new Label(String.format("%.2f €", line));
            price.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            row.getChildren().addAll(name, sp, price);
            orderItemsMiniList.getChildren().add(row);
        }
    }

    @FXML
    private void handleToggleBilling() {
        boolean showBilling = !sameAsShippingCheck.isSelected();
        billingBox.setVisible(showBilling);
        billingBox.setManaged(showBilling);
    }

    @FXML
    private void handlePaymentChanged() {
        boolean isCard = payCardRadio.isSelected();
        stripeInfoBox.setVisible(isCard);
        stripeInfoBox.setManaged(isCard);
        recalculateTotals();
    }

    @FXML
    private void handleInvoiceToggle() {
        boolean isInvoice = invoiceRadio.isSelected();
        invoiceBox.setVisible(isInvoice);
        invoiceBox.setManaged(isInvoice);
    }

    @FXML
    private void recalculateTotals() {
        shipCourierRadio.setText(subtotal >= 100.0 ? "Courier (Δωρεάν)" : "Courier (4,90 €)");

        double shipping = (shipPickupRadio.isSelected() || subtotal >= 100.0) ? 0.0 : 4.90;
        double codFee = payCodRadio.isSelected() ? 2.00 : 0.0;
        double extra = shipping + codFee;

        subLbl.setText(String.format("%.2f €", subtotal));
        extraLbl.setText(extra == 0.0 ? "Δωρεάν" : String.format("%.2f €", extra));
        grandTotalLbl.setText(String.format("%.2f €", subtotal + extra));
    }

    @FXML
    private void handleBackToCart() {
        if (onBackToCart != null) onBackToCart.run();
    }

    @FXML
    private void handleSubmitOrder() {
        if (isBlank(firstNameField) || isBlank(lastNameField) || isBlank(emailField) || isBlank(phoneField)
                || isBlank(shipAddressField) || isBlank(shipCityField) || isBlank(shipZipField)) {
            showError("Συμπλήρωσε όλα τα υποχρεωτικά πεδία (*) επικοινωνίας και αποστολής.");
            return;
        }
        if (!sameAsShippingCheck.isSelected() && (isBlank(billAddressField) || isBlank(billCityField) || isBlank(billZipField))) {
            showError("Συμπλήρωσε τα πεδία της διεύθυνσης χρέωσης ή επίλεξε να είναι ίδια με την αποστολή.");
            return;
        }
        if (invoiceRadio.isSelected() && (isBlank(afmField) || isBlank(doyField) || isBlank(companyField))) {
            showError("Συμπλήρωσε ΑΦΜ, ΔΟΥ και Επωνυμία για έκδοση τιμολογίου.");
            return;
        }

        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // TODO: Όταν συνδεθεί το Stripe, αν payCardRadio.isSelected() -> άνοιγμα Stripe Checkout Session

        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Επιβεβαίωση Παραγγελίας");
        ok.setHeaderText("Η παραγγελία σου καταχωρήθηκε επιτυχώς!");
        ok.setContentText("Ευχαριστούμε " + firstNameField.getText() + "! Θα λάβεις επιβεβαίωση στο " + emailField.getText() + ".");
        ok.showAndWait();

        cart.clear();
        if (onOrderSuccess != null) onOrderSuccess.run();
    }

    private boolean isBlank(TextField tf) {
        return tf.getText() == null || tf.getText().trim().isEmpty();
    }

    private void showError(String msg) {
        errorLbl.setText(msg);
        errorLbl.setVisible(true);
        errorLbl.setManaged(true);
    }
}