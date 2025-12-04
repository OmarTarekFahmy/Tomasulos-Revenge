package com.tomasulo.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        ConfigView configView = new ConfigView(primaryStage);
        Scene scene = new Scene(configView, 600, 800);
        
        primaryStage.setTitle("Tomasulo Simulator - Configuration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
