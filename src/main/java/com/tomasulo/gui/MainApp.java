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
        Scene scene = new Scene(configView, 400, 700);
        
        primaryStage.setTitle("Tomasulo Simulator - Configuration");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(350);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
