package com.aiusage;

import com.aiusage.ui.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();
        Scene scene = new Scene(controller.buildUI(), 680, 560);
        scene.getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm()
        );
        primaryStage.setTitle("AI Usage Dashboard");
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/app-icon.png"));
            if (!icon.isError()) {
                primaryStage.getIcons().add(icon);
            }
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }
        primaryStage.setScene(scene);
        primaryStage.show();
        controller.loadData();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
