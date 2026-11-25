package com.omnirig.controllers.admin;

import com.omnirig.models.Product;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static com.omnirig.controllers.admin.AdminUiHelper.createCard;
import static com.omnirig.controllers.admin.AdminUiHelper.fieldLbl;

public class AdminConcurrencyTab {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final ObservableList<Product> productsList;

    public AdminConcurrencyTab(ObservableList<Product> productsList) {
        this.productsList = productsList;
    }

    public Node build() {
        VBox card = createCard();
        Label title = new Label("Έλεγχος Αντοχής Αποθέματος (Checkout Stress Test)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label desc = new Label("Εκκινεί 2 ταυτόχρονα νήματα (Session A & Session B) που προσπαθούν να αγοράσουν το τελευταίο διαθέσιμο τεμάχιο (Stock = 1) την ίδια στιγμή.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        ComboBox<Product> productCombo = new ComboBox<>(productsList);
        productCombo.setPrefWidth(260);
        productCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Product p) { return p != null ? p.getName() : ""; }
            @Override public Product fromString(String s) { return null; }
        });
        if (!productsList.isEmpty()) productCombo.getSelectionModel().selectFirst();

        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(
            "1. Standard (READ COMMITTED - χωρίς lock)",
            "2. Repeatable Read (Rollback σε σύγκρουση)",
            "3. Strict Lock (SELECT ... FOR UPDATE / Pessimistic)"
        );
        modeCombo.getSelectionModel().selectLast();

        Button simBtn = new Button("Εκτέλεση Δοκιμής");
        simBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox row = new HBox(10, fieldLbl("Προϊόν:"), productCombo, fieldLbl("Επίπεδο:"), modeCombo, simBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(270);
        consoleArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        consoleArea.setText("Επίλεξε προϊόν και επίπεδο απομόνωσης και πάτα «Εκτέλεση Δοκιμής».");

        simBtn.setOnAction(e -> {
            Product selected = productCombo.getValue();
            if (selected == null) {
                consoleArea.setText("Δεν υπάρχει διαθέσιμο προϊόν για δοκιμή.");
                return;
            }
            int modeIdx = modeCombo.getSelectionModel().getSelectedIndex();
            runConcurrentSimulation(selected.getName(), modeIdx, modeCombo.getValue(), simBtn, consoleArea);
        });

        card.getChildren().addAll(title, desc, new Separator(), row, consoleArea);
        return card;
    }

    private void runConcurrentSimulation(String productName, int modeIdx, String modeLabel, Button simBtn, TextArea console) {
        simBtn.setDisable(true);
        console.clear();
        log(console, "=== ΕΝΑΡΞΗ STRESS TEST: " + productName + " (Αρχικό Stock = 1) ===");
        log(console, "Λειτουργία: " + modeLabel);
        log(console, "------------------------------------------------------------------");

        // TODO (Backend): Αντικατάσταση με 2 Hibernate Sessions + setTransactionIsolation / LockModeType.PESSIMISTIC_WRITE
        AtomicInteger simulatedStock = new AtomicInteger(1);
        AtomicInteger version = new AtomicInteger(1);
        ReentrantLock rowLock = new ReentrantLock(true);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Runnable taskA = () -> executeCheckout("Session A", modeIdx, simulatedStock, version, rowLock, startGun, doneLatch, console, 180);
        Runnable taskB = () -> executeCheckout("Session B", modeIdx, simulatedStock, version, rowLock, startGun, doneLatch, console, 220);

        Thread tA = new Thread(taskA, "Checkout-Session-A");
        Thread tB = new Thread(taskB, "Checkout-Session-B");
        tA.setDaemon(true);
        tB.setDaemon(true);
        tA.start();
        tB.start();

        // Εκκίνηση και των 2 threads ταυτόχρονα
        startGun.countDown();

        Thread watcher = new Thread(() -> {
            try {
                doneLatch.await();
            } catch (InterruptedException ignored) {}
            log(console, "------------------------------------------------------------------");
            int finalStock = simulatedStock.get();
            if (finalStock < 0) {
                log(console, "ΑΠΟΤΕΛΕΣΜΑ: [ΣΦΑΛΜΑ OVERSELLING] Τελικό Stock = " + finalStock + " (Πουλήθηκαν 2 τεμάχια ενώ υπήρχε 1!)");
            } else {
                log(console, "ΑΠΟΤΕΛΕΣΜΑ: [ΑΣΦΑΛΕΣ] Τελικό Stock = " + finalStock + " (Δεν επιτράπηκε διπλή αγορά.)");
            }
            Platform.runLater(() -> simBtn.setDisable(false));
        });
        watcher.setDaemon(true);
        watcher.start();
    }

    private void executeCheckout(
        String session, int modeIdx, AtomicInteger stock, AtomicInteger version,
        ReentrantLock rowLock, CountDownLatch startGun, CountDownLatch doneLatch,
        TextArea console, long workMs) {
        try {
            startGun.await();

            if (modeIdx == 2) {
                // Strict Lock (Pessimistic Lock / SELECT ... FOR UPDATE)
                log(console, "[" + session + "] Αίτημα για Pessimistic Lock (SELECT ... FOR UPDATE)...");
                rowLock.lock();
                try {
                    int current = stock.get();
                    log(console, "[" + session + "] Απέκτησε το lock — Διάβασε Stock = " + current);
                    if (current <= 0) {
                        log(console, "[" + session + "] ΑΠΟΡΡΙΨΗ: Το προϊόν εξαντλήθηκε. ROLLBACK.");
                        return;
                    }
                    Thread.sleep(workMs);
                    int updated = stock.decrementAndGet();
                    log(console, "[" + session + "] ΕΠΙΤΥΧΙΑ: Ολοκλήρωση αγοράς -> Νέο Stock = " + updated + " (COMMIT).");
                } finally {
                    rowLock.unlock();
                }
            } else if (modeIdx == 1) {
                // Repeatable Read (Optimistic / MVCC Conflict Detection)
                int readStock = stock.get();
                int readVer = version.get();
                log(console, "[" + session + "] Διάβασε snapshot: Stock = " + readStock + " (version=" + readVer + ")");
                Thread.sleep(workMs);
                synchronized (version) {
                    if (version.get() != readVer) {
                        log(console, "[" + session + "] ΣΥΓΚΡΟΥΣΗ (SQLState 40001): Η εγγραφή άλλαξε από άλλο transaction -> ROLLBACK.");
                        return;
                    }
                    version.incrementAndGet();
                    int updated = stock.decrementAndGet();
                    log(console, "[" + session + "] ΕΠΙΤΥΧΙΑ: COMMIT -> Νέο Stock = " + updated);
                }
            } else {
                // Standard (READ COMMITTED χωρίς lock -> Race Condition)
                int readStock = stock.get();
                log(console, "[" + session + "] Διάβασε Stock = " + readStock + " (χωρίς lock)");
                if (readStock <= 0) {
                    log(console, "[" + session + "] ΑΠΟΡΡΙΨΗ: Stock = 0.");
                    return;
                }
                Thread.sleep(workMs); // Επεξεργασία πληρωμής όσο το άλλο thread διαβάζει το ίδιο stock
                int updated = stock.decrementAndGet();
                log(console, "[" + session + "] ΕΠΙΤΥΧΙΑ: Αφαίρεσε 1 τεμάχιο -> Νέο Stock = " + updated + " (COMMIT).");
            }
        } catch (InterruptedException ignored) {
        } finally {
            doneLatch.countDown();
        }
    }

    private void log(TextArea console, String msg) {
        String line = "[" + LocalTime.now().format(TIME_FMT) + "] " + msg + "\n";
        Platform.runLater(() -> console.appendText(line));
    }
}