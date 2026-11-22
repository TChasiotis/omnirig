package com.omnirig;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Φορτώνει το FXML από τον φάκελο resources
        Parent root = FXMLLoader.load(getClass().getResource("/com/omnirig/views/CustomerDashboard.fxml"));
        
        primaryStage.setTitle("OmniRig - Hardware Management");
        primaryStage.setScene(new Scene(root, 1200, 750));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}