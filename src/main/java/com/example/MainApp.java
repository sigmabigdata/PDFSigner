package com.example;

import com.example.controller.MainController;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

    private Stage primaryStage;
    private Pane loadingRoot;
    private List<Circle> particles = new ArrayList<>();
    private List<Line> matrixLines = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // Настраиваем полностью кастомное окно
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        showCyberpunkLoadingScreen();
        loadMainApplication();
    }

    private void showCyberpunkLoadingScreen() {
        loadingRoot = new Pane();
        loadingRoot.setStyle("-fx-background-color: transparent;");

        // Создаем многослойный анимированный фон
        createAnimatedBackground();

        // Добавляем матричный эффект
        createMatrixRain();

        // Создаем основной контент загрузки
        VBox loadingContent = createCyberpunkLoadingContent();
        loadingRoot.getChildren().add(loadingContent);

        Scene loadingScene = new Scene(loadingRoot, 1200, 800);
        loadingScene.setFill(Color.TRANSPARENT);
        loadingScene.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());

        primaryStage.setScene(loadingScene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void createAnimatedBackground() {
        // Градиентный фон с анимацией
        Rectangle background = new Rectangle(1200, 800);
        background.setFill(createAnimatedGradient());
        loadingRoot.getChildren().add(background);

        // Анимируем градиент
        Timeline gradientAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(background.fillProperty(), createAnimatedGradient())),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(background.fillProperty(), createAnimatedGradient2()))
        );
        gradientAnimation.setCycleCount(Animation.INDEFINITE);
        gradientAnimation.setAutoReverse(true);
        gradientAnimation.play();

        // Добавляем частицы
        createParticleSystem();

        // Добавляем голографические линии
        createHolographicLines();
    }

    private Paint createAnimatedGradient() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.REFLECT,
                new Stop(0, Color.web("#070710")),
                new Stop(0.3, Color.web("#0F0F1E")),
                new Stop(0.6, Color.web("#1A1A2E")),
                new Stop(1, Color.web("#16213E"))
        );
    }

    private Paint createAnimatedGradient2() {
        return new LinearGradient(0, 1, 1, 0, true, CycleMethod.REFLECT,
                new Stop(0, Color.web("#16213E")),
                new Stop(0.3, Color.web("#1A1A2E")),
                new Stop(0.6, Color.web("#0F0F1E")),
                new Stop(1, Color.web("#070710"))
        );
    }

    private void createParticleSystem() {
        for (int i = 0; i < 50; i++) {
            Circle particle = new Circle(1 + Math.random() * 3);

            // Случайный неоновый цвет
            Color[] neonColors = {
                    Color.web("#00D4FF"), Color.web("#A855F7"),
                    Color.web("#FF0080"), Color.web("#00FF88")
            };
            Color particleColor = neonColors[(int)(Math.random() * neonColors.length)];
            particle.setFill(particleColor);

            particle.setEffect(new Glow(0.8 + Math.random() * 0.4));

            particle.setLayoutX(Math.random() * 1200);
            particle.setLayoutY(Math.random() * 800);

            particles.add(particle);
            loadingRoot.getChildren().add(particle);

            // Сложная анимация частиц
            PathTransition path = new PathTransition();
            path.setNode(particle);
            path.setDuration(Duration.seconds(5 + Math.random() * 10));
            path.setCycleCount(Animation.INDEFINITE);
            path.setAutoReverse(true);

            // Создаем случайный путь
            javafx.scene.shape.Path particlePath = new javafx.scene.shape.Path();
            particlePath.getElements().add(new javafx.scene.shape.MoveTo(
                    particle.getLayoutX(), particle.getLayoutY()));
            particlePath.getElements().add(new javafx.scene.shape.CubicCurveTo(
                    particle.getLayoutX() + (Math.random() - 0.5) * 400,
                    particle.getLayoutY() + (Math.random() - 0.5) * 400,
                    particle.getLayoutX() + (Math.random() - 0.5) * 400,
                    particle.getLayoutY() + (Math.random() - 0.5) * 400,
                    particle.getLayoutX() + (Math.random() - 0.5) * 200,
                    particle.getLayoutY() + (Math.random() - 0.5) * 200
            ));

            path.setPath(particlePath);
            path.play();

            // Анимация пульсации
            FadeTransition pulse = new FadeTransition(Duration.seconds(1 + Math.random() * 2), particle);
            pulse.setFromValue(0.2);
            pulse.setToValue(1.0);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    private void createMatrixRain() {
        for (int i = 0; i < 30; i++) {
            Line matrixLine = new Line();
            matrixLine.setStartX(Math.random() * 1200);
            matrixLine.setStartY(0);
            matrixLine.setEndX(matrixLine.getStartX());
            matrixLine.setEndY(20 + Math.random() * 50);
            matrixLine.setStroke(Color.web("#00FF88"));
            matrixLine.setStrokeWidth(1);
            matrixLine.setEffect(new Glow(0.7));

            matrixLines.add(matrixLine);
            loadingRoot.getChildren().add(matrixLine);

            // Анимация падающего кода
            TranslateTransition fall = new TranslateTransition(Duration.seconds(2 + Math.random() * 3), matrixLine);
            fall.setFromY(-100);
            fall.setToY(900);
            fall.setCycleCount(Animation.INDEFINITE);

            FadeTransition fade = new FadeTransition(Duration.seconds(1), matrixLine);
            fade.setFromValue(0);
            fade.setToValue(0.8);
            fade.setAutoReverse(true);
            fade.setCycleCount(Animation.INDEFINITE);

            ParallelTransition parallel = new ParallelTransition(fall, fade);
            parallel.setDelay(Duration.seconds(Math.random() * 5));
            parallel.play();
        }
    }

    private void createHolographicLines() {
        for (int i = 0; i < 10; i++) {
            Line hologramLine = new Line(0, Math.random() * 800, 1200, Math.random() * 800);
            hologramLine.setStroke(Color.web("#00D4FF"));
            hologramLine.setStrokeWidth(1);
            hologramLine.setOpacity(0.3);
            hologramLine.setEffect(new Glow(0.5));

            loadingRoot.getChildren().add(hologramLine);

            // Анимация сканирования
            StrokeTransition scan = new StrokeTransition(Duration.seconds(4), hologramLine);
            scan.setFromValue(Color.web("#00D4FF"));
            scan.setToValue(Color.web("#A855F7"));
            scan.setCycleCount(Animation.INDEFINITE);
            scan.setAutoReverse(true);
            scan.play();
        }
    }

    private VBox createCyberpunkLoadingContent() {
        VBox content = new VBox(40);
        content.setStyle("-fx-alignment: center; -fx-padding: 100;");
        content.setEffect(new Bloom(0.8));

        // Анимированный логотип с неоном
        Pane logoContainer = createAnimatedNeonLogo();

        // Прогресс бар с неоновым свечением
        StackPane progressContainer = createNeonProgressBar();

        // Текст загрузки с эффектом печатания
        Text loadingText = createTypingText("ИНИЦИАЛИЗАЦИЯ СИСТЕМЫ PDF SIGNER PRO");

        content.getChildren().addAll(logoContainer, progressContainer, loadingText);

        return content;
    }

    private Pane createAnimatedNeonLogo() {
        Pane logo = new Pane();
        logo.setPrefSize(400, 200);

        // Внешнее кольцо
        Circle outerRing = new Circle(100);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web("#00D4FF"));
        outerRing.setStrokeWidth(4);
        outerRing.setEffect(new Glow(0.8));

        // Внутреннее кольцо
        Circle innerRing = new Circle(70);
        innerRing.setFill(Color.TRANSPARENT);
        innerRing.setStroke(Color.web("#FF0080"));
        innerRing.setStrokeWidth(3);
        innerRing.setEffect(new Glow(0.7));

        // Центральный круг
        Circle centerCircle = new Circle(40);
        centerCircle.setFill(Color.TRANSPARENT);
        centerCircle.setStroke(Color.web("#00FF88"));
        centerCircle.setStrokeWidth(2);
        centerCircle.setEffect(new Glow(0.6));

        logo.getChildren().addAll(outerRing, innerRing, centerCircle);

        // Сложные анимации вращения
        RotateTransition rotateOuter = new RotateTransition(Duration.seconds(6), outerRing);
        rotateOuter.setByAngle(360);
        rotateOuter.setCycleCount(Animation.INDEFINITE);

        RotateTransition rotateInner = new RotateTransition(Duration.seconds(4), innerRing);
        rotateInner.setByAngle(-360);
        rotateInner.setCycleCount(Animation.INDEFINITE);

        RotateTransition rotateCenter = new RotateTransition(Duration.seconds(8), centerCircle);
        rotateCenter.setByAngle(360);
        rotateCenter.setCycleCount(Animation.INDEFINITE);

        // Анимация свечения
        Timeline glowAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(outerRing.strokeProperty(), Color.web("#00D4FF")),
                        new KeyValue(innerRing.strokeProperty(), Color.web("#FF0080")),
                        new KeyValue(centerCircle.strokeProperty(), Color.web("#00FF88"))
                ),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(outerRing.strokeProperty(), Color.web("#A855F7")),
                        new KeyValue(innerRing.strokeProperty(), Color.web("#00FF88")),
                        new KeyValue(centerCircle.strokeProperty(), Color.web("#00D4FF"))
                ),
                new KeyFrame(Duration.seconds(4),
                        new KeyValue(outerRing.strokeProperty(), Color.web("#00D4FF")),
                        new KeyValue(innerRing.strokeProperty(), Color.web("#FF0080")),
                        new KeyValue(centerCircle.strokeProperty(), Color.web("#00FF88"))
                )
        );
        glowAnimation.setCycleCount(Animation.INDEFINITE);

        ParallelTransition logoAnimations = new ParallelTransition(
                rotateOuter, rotateInner, rotateCenter, glowAnimation
        );
        logoAnimations.play();

        return logo;
    }

    private StackPane createNeonProgressBar() {
        StackPane container = new StackPane();
        container.setStyle("-fx-padding: 30;");

        // Фон прогресс бара
        Rectangle bg = new Rectangle(400, 10);
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setFill(Color.rgb(255, 255, 255, 0.1));
        bg.setEffect(new BoxBlur(5, 5, 2));

        // Сам прогресс бар
        Rectangle progress = new Rectangle(0, 10);
        progress.setArcWidth(10);
        progress.setArcHeight(10);
        progress.setFill(createProgressGradient());
        progress.setEffect(new Glow(0.9));

        container.getChildren().addAll(bg, progress);

        // Анимация прогресс бара
        Timeline progressAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress.widthProperty(), 0)),
                new KeyFrame(Duration.seconds(2.5), new KeyValue(progress.widthProperty(), 400))
        );

        // Анимация цвета прогресс бара
        Timeline colorAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progress.fillProperty(), createProgressGradient())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(progress.fillProperty(), createProgressGradient2())),
                new KeyFrame(Duration.seconds(2.5),
                        new KeyValue(progress.fillProperty(), createProgressGradient3()))
        );

        ParallelTransition parallel = new ParallelTransition(progressAnimation, colorAnimation);
        parallel.play();

        return container;
    }

    private LinearGradient createProgressGradient() {
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00D4FF")),
                new Stop(1, Color.web("#A855F7"))
        );
    }

    private LinearGradient createProgressGradient2() {
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#A855F7")),
                new Stop(1, Color.web("#FF0080"))
        );
    }

    private LinearGradient createProgressGradient3() {
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF0080")),
                new Stop(1, Color.web("#00FF88"))
        );
    }

    private Text createTypingText(String message) {
        Text text = new Text();
        text.setFont(Font.font("Courier New", 20));
        text.setFill(Color.web("#00FF88"));
        text.setEffect(new Glow(0.7));

        // Эффект печатания
        final int[] currentLength = {0};
        Timeline typing = new Timeline();

        for (int i = 0; i <= message.length(); i++) {
            final int length = i;
            KeyFrame keyFrame = new KeyFrame(Duration.millis(100 * i), e -> {
                text.setText(message.substring(0, length));
            });
            typing.getKeyFrames().add(keyFrame);
        }

        typing.setCycleCount(1);
        typing.play();

        // Мигающий курсор
        Rectangle cursor = new Rectangle(2, 25, Color.web("#00FF88"));
        cursor.setLayoutX(text.getLayoutBounds().getWidth());
        cursor.setEffect(new Glow(0.8));

        FadeTransition blink = new FadeTransition(Duration.millis(500), cursor);
        blink.setFromValue(1.0);
        blink.setToValue(0.0);
        blink.setCycleCount(Animation.INDEFINITE);
        blink.setAutoReverse(true);

        // Привязываем курсор к тексту
        text.boundsInLocalProperty().addListener((obs, old, newVal) -> {
            cursor.setLayoutX(newVal.getWidth());
        });

        Pane textContainer = new Pane(text, cursor);
        textContainer.setStyle("-fx-alignment: center;");

        return text;
    }

    private void loadMainApplication() {
        new Thread(() -> {
            try {
                // Имитация загрузки ресурсов
                simulateResourceLoading();

                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/view/main.fxml"));
                        Parent root = loader.load();

                        MainController controller = loader.getController();
                        controller.setPrimaryStage(primaryStage);

                        Scene mainScene = new Scene(root);
                        mainScene.setFill(Color.TRANSPARENT);
                        mainScene.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());

                        // Эпичный переход с эффектом взрыва частиц
                        createEpicParticleTransition(mainScene);

                    } catch (Exception e) {
                        e.printStackTrace();
                        showCyberErrorScreen(e.getMessage());
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void simulateResourceLoading() throws InterruptedException {
        String[] loadingSteps = {
                "Загрузка нейросетевых модулей...",
                "Инициализация квантовых процессоров...",
                "Калибровка голографических интерфейсов...",
                "Активация кибернетических протоколов...",
                "Синхронизация с блокчейн сетью..."
        };

        for (String step : loadingSteps) {
            Thread.sleep(400);
            System.out.println(step);
        }
    }

    private void createEpicParticleTransition(Scene mainScene) {
        // Создаем эффект взрыва частиц
        createParticleExplosion();

        // Анимация исчезновения загрузочного экрана
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), loadingRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.seconds(1), loadingRoot);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(1.5);
        scaleOut.setToY(1.5);

        ParallelTransition exitAnimation = new ParallelTransition(fadeOut, scaleOut);

        exitAnimation.setOnFinished(e -> {
            primaryStage.setScene(mainScene);

            // Эпичное появление основного интерфейса
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), mainScene.getRoot());
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.seconds(1.5), mainScene.getRoot());
            scaleIn.setFromX(0.7);
            scaleIn.setFromY(0.7);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            RotateTransition rotateIn = new RotateTransition(Duration.seconds(1.5), mainScene.getRoot());
            rotateIn.setFromAngle(-10);
            rotateIn.setToAngle(0);

            ParallelTransition entranceAnimation = new ParallelTransition(fadeIn, scaleIn, rotateIn);
            entranceAnimation.play();
        });

        exitAnimation.play();
    }

    private void createParticleExplosion() {
        for (int i = 0; i < 100; i++) {
            Circle explosionParticle = new Circle(1 + Math.random() * 4);
            explosionParticle.setFill(Color.web("#00D4FF"));
            explosionParticle.setEffect(new Glow(0.9));
            explosionParticle.setLayoutX(600); // Центр экрана
            explosionParticle.setLayoutY(400);

            loadingRoot.getChildren().add(explosionParticle);

            // Анимация разлета частиц
            TranslateTransition explode = new TranslateTransition(Duration.seconds(1), explosionParticle);
            explode.setByX((Math.random() - 0.5) * 1000);
            explode.setByY((Math.random() - 0.5) * 800);

            FadeTransition fade = new FadeTransition(Duration.seconds(1), explosionParticle);
            fade.setFromValue(1);
            fade.setToValue(0);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(1), explosionParticle);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(0.1);
            scale.setToY(0.1);

            ParallelTransition particleAnimation = new ParallelTransition(explode, fade, scale);
            particleAnimation.play();
        }
    }

    private void showCyberErrorScreen(String errorMessage) {
        VBox errorRoot = new VBox(30);
        errorRoot.setStyle("-fx-background-color: #0F0F1E; -fx-alignment: center; -fx-padding: 60;");
        errorRoot.setEffect(new Glow(0.3));

        Text errorTitle = new Text("СИСТЕМНЫЙ СБОЙ");
        errorTitle.setFont(Font.font("Orbitron", 36));
        errorTitle.setFill(Color.web("#FF0080"));
        errorTitle.setEffect(new Glow(0.8));

        Text errorDesc = new Text(errorMessage);
        errorDesc.setFont(Font.font("Courier New", 16));
        errorDesc.setFill(Color.web("#00D4FF"));
        errorDesc.setWrappingWidth(500);

        Button retryButton = new Button("РЕБУТ СИСТЕМЫ");
        retryButton.setStyle("-fx-background-color: #FF0080; -fx-text-fill: white; -fx-font-weight: 900; " +
                "-fx-padding: 15 30; -fx-background-radius: 25; -fx-font-size: 14px;");
        retryButton.setOnAction(e -> {
            try {
                start(primaryStage);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        retryButton.setEffect(new Glow(0.5));

        errorRoot.getChildren().addAll(errorTitle, errorDesc, retryButton);

        Scene errorScene = new Scene(errorRoot, 600, 400, Color.TRANSPARENT);
        primaryStage.setScene(errorScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}