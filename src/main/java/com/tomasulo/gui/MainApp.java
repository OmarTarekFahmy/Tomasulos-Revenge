package com.tomasulo.gui;

import com.tomasulo.gui.controller.ConfigController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        ConfigController controller = new ConfigController(primaryStage);
        ConfigView configView = new ConfigView(controller);
        Scene scene = new Scene(configView, 600, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setTitle("Tomasulo Simulator - Configuration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
