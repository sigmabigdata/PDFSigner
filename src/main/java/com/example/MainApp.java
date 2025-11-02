package com.example;

import com.example.controller.MainController;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class MainApp extends Application {

    private Stage primaryStage;
    private StackPane loadingRoot;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // Показываем неоновый загрузочный экран
        showNeonLoadingScreen();

        // Загружаем основное приложение в фоне
        loadMainApplication();
    }

    private void showNeonLoadingScreen() {
        loadingRoot = new StackPane();
        loadingRoot.setStyle("-fx-background-color: #0F172A;");

        // Создаем неоновые элементы
        createNeonLoadingElements();

        Scene loadingScene = new Scene(loadingRoot, 800, 600);
        loadingScene.setFill(Color.TRANSPARENT);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(loadingScene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void createNeonLoadingElements() {
        // Неоновый спиннер
        Circle spinner = new Circle(50);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#00D4FF"));
        spinner.setStrokeWidth(3);
        spinner.setEffect(new Glow(0.8));

        // Текст загрузки
        Text loadingText = new Text("PDF SIGNER PRO");
        loadingText.setFont(Font.font("Orbitron", 36));
        loadingText.setFill(Color.web("#00D4FF"));
        loadingText.setEffect(new Glow(0.7));

        loadingRoot.getChildren().addAll(spinner, loadingText);

        // Анимация вращения спиннера
        RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();

        // Анимация пульсации текста
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.8), loadingText);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void loadMainApplication() {
        new Thread(() -> {
            try {
                // Имитация загрузки
                Thread.sleep(1500);

                javafx.application.Platform.runLater(() -> {
                    try {
                        // Переход к основному приложению
                        switchToMainApp();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void switchToMainApp() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/view/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        Scene mainScene = new Scene(root);
        mainScene.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());

        // Эффект Bloom для всего приложения
        root.setEffect(new Bloom(0.8));

        // Анимация перехода
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), loadingRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> {
            primaryStage.setScene(mainScene);
            primaryStage.setTitle("PDF Signer Pro - Система электронной подписи");
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();

            // Анимация появления основного интерфейса
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        fadeOut.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}