package com.example;

import com.example.controller.MainController;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Показываем загрузочный экран
        showLoadingScreen(primaryStage);

        // Загружаем основное приложение в фоне
        loadMainApplication(primaryStage);
    }

    private void showLoadingScreen(Stage primaryStage) {
        Parent loadingRoot = createLoadingScreen();
        Scene loadingScene = new Scene(loadingRoot, 400, 300);
        loadingScene.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());

        primaryStage.setScene(loadingScene);
        primaryStage.setTitle("Загрузка...");
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private Parent createLoadingScreen() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%); " +
                "-fx-alignment: center; -fx-spacing: 20; -fx-padding: 40;");

        // Создаем спиннер
        javafx.scene.shape.Circle spinner = new javafx.scene.shape.Circle(40);
        spinner.setStroke(Color.WHITE);
        spinner.setStrokeWidth(4);
        spinner.setFill(Color.TRANSPARENT);

        // Создаем метку загрузки (используем javafx.scene.control.Label)
        Label loadingLabel = new Label("PDF Signer Pro\nЗагрузка...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-alignment: center;");
        loadingLabel.setWrapText(true);

        root.getChildren().addAll(spinner, loadingLabel);

        // Анимация спиннера
        RotateTransition rotate = new RotateTransition(Duration.seconds(2), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();

        // Анимация пульсации текста
        FadeTransition pulse = new FadeTransition(Duration.seconds(1.5), loadingLabel);
        pulse.setFromValue(0.7);
        pulse.setToValue(1.0);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        return root;
    }

    private void loadMainApplication(Stage primaryStage) {
        new Thread(() -> {
            try {
                // Имитация загрузки
                Thread.sleep(2000);

                // Загрузка основного интерфейса
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/view/main.fxml"));
                        Parent root = loader.load();

                        MainController controller = loader.getController();
                        controller.setPrimaryStage(primaryStage);

                        Scene scene = new Scene(root);

                        // Применяем кастомные стили
                        scene.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());

                        // Анимация перехода
                        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.8), root);
                        fadeTransition.setFromValue(0);
                        fadeTransition.setToValue(1);

                        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.8), root);
                        translateTransition.setFromY(30);
                        translateTransition.setToY(0);

                        ParallelTransition parallelTransition = new ParallelTransition(fadeTransition, translateTransition);

                        primaryStage.setScene(scene);
                        primaryStage.setTitle("PDF Signer Pro - Профессиональная система подписи документов");
                        primaryStage.setMinWidth(1000);
                        primaryStage.setMinHeight(750);
                        primaryStage.centerOnScreen();

                        parallelTransition.play();

                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorScreen(primaryStage, e.getMessage());
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showErrorScreen(Stage primaryStage, String errorMessage) {
        VBox errorRoot = new VBox();
        errorRoot.setStyle("-fx-background-color: #f8f9fa; -fx-alignment: center; -fx-spacing: 20; -fx-padding: 40;");

        Label errorLabel = new Label("Ошибка загрузки приложения\n" + errorMessage);
        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;");
        errorLabel.setWrapText(true);

        javafx.scene.control.Button retryButton = new javafx.scene.control.Button("Повторить");
        retryButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        retryButton.setOnAction(e -> {
            try {
                start(primaryStage);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        errorRoot.getChildren().addAll(errorLabel, retryButton);

        Scene errorScene = new Scene(errorRoot, 500, 300);
        primaryStage.setScene(errorScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}