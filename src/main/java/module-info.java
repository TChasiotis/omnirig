module com.omnirig {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.omnirig.controllers to javafx.fxml;
    
    exports com.omnirig;
    exports com.omnirig.models;
    exports com.omnirig.services;
}