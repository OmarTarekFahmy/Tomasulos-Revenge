module com.tomasulo {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Export packages for JavaFX to access
    exports com.tomasulo.gui;
    exports com.tomasulo.core;
    exports com.tomasulo.parser;

    // Open packages for reflection (needed by JavaFX)
    opens com.tomasulo.gui to javafx.fxml, javafx.graphics;
    opens com.tomasulo.core to javafx.base;
}
