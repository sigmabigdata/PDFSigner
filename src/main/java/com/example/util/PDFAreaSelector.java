package com.example.util;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PDFAreaSelector {
    private final File pdfFile;
    private final boolean addBlankPage;
    private int currentPage = 0;
    private List<Image> pageImages = new ArrayList<>();
    private ImageView imageView;
    private Label pageLabel;
    private Circle markerDot;
    private Button confirmButton;

    public PDFAreaSelector(File pdfFile, boolean addBlankPage) {
        this.pdfFile = pdfFile;
        this.addBlankPage = addBlankPage;
    }

    public static class SelectedArea {
        public final double clickX;
        public final double clickY;
        public final int pageIndex;

        public SelectedArea(double clickX, double clickY, int pageIndex) {
            this.clickX = clickX;
            this.clickY = clickY;
            this.pageIndex = pageIndex;
        }

        public Point2D getCoordinates() {
            return new Point2D(clickX, clickY);
        }
    }

    public Optional<SelectedArea> selectArea(Stage ownerStage) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            // Добавляем пустую страницу если нужно
            if (addBlankPage) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            pageImages.clear();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = renderer.renderImage(i, 1.0f);
                pageImages.add(SwingFXUtils.toFXImage(bufferedImage, null));
            }

            Stage selectionStage = new Stage();
            selectionStage.initModality(Modality.APPLICATION_MODAL);
            selectionStage.initOwner(ownerStage);
            selectionStage.setTitle("Выберите место для размещения протокола" +
                    (addBlankPage ? " (последняя страница - пустая)" : ""));

            // Создаем контейнер для изображения и маркера
            Pane imageContainer = new Pane();
            imageView = new ImageView(pageImages.get(0));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(pageImages.get(0).getWidth());
            imageView.setFitHeight(pageImages.get(0).getHeight());

            // Создаем красную точку для маркировки выбранного места
            markerDot = new Circle(5, Color.RED);
            markerDot.setVisible(false);

            // Кнопки навигации по страницам
            Button prevButton = new Button("< Предыдущая");
            Button nextButton = new Button("Следующая >");

            // Метка с номером страницы
            pageLabel = new Label();
            updatePageLabel();

            // Кнопка подтверждения выбора
            confirmButton = new Button("Подтвердить выбор");
            confirmButton.setDisable(true);
            confirmButton.setOnAction(e -> {
                if (markerDot.isVisible()) {
                    Bounds bounds = imageView.getBoundsInParent();
                    double clickX = markerDot.getCenterX() / bounds.getWidth() * imageView.getImage().getWidth();
                    double clickY = (bounds.getHeight() - markerDot.getCenterY()) / bounds.getHeight() * imageView.getImage().getHeight();

                    selectionStage.close();
                    selectionStage.setUserData(new SelectedArea(clickX, clickY, currentPage));
                }
            });

            // Панель навигации
            HBox navPanel = new HBox(10, prevButton, pageLabel, nextButton);
            navPanel.setAlignment(Pos.CENTER);

            // Инструкция для пользователя
            Label instructionLabel = new Label("Кликните на изображение, чтобы выбрать место для протокола");
            instructionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a5885;");

            // Основной контейнер
            VBox root = new VBox(10, instructionLabel, imageContainer, navPanel, confirmButton);
            root.setPadding(new Insets(15));

            // Добавляем изображение и маркер в контейнер
            imageContainer.getChildren().addAll(imageView, markerDot);

            // Обработчики кнопок навигации
            prevButton.setOnAction(e -> {
                if (currentPage > 0) {
                    currentPage--;
                    updateImageView();
                    updatePageLabel();
                    markerDot.setVisible(false);
                    confirmButton.setDisable(true);
                }
            });

            nextButton.setOnAction(e -> {
                if (currentPage < pageImages.size() - 1) {
                    currentPage++;
                    updateImageView();
                    updatePageLabel();
                    markerDot.setVisible(false);
                    confirmButton.setDisable(true);
                }
            });

            // Обработчик клика по изображению
            imageView.setOnMouseClicked(event -> {
                Bounds bounds = imageView.getBoundsInParent();
                double clickX = event.getX();
                double clickY = event.getY();

                // Показываем красную точку в месте клика
                markerDot.setCenterX(clickX);
                markerDot.setCenterY(clickY);
                markerDot.setVisible(true);
                confirmButton.setDisable(false);
            });

            Scene scene = new Scene(root);
            selectionStage.setScene(scene);
            selectionStage.showAndWait();

            return Optional.ofNullable((SelectedArea) selectionStage.getUserData());
        }
    }

    private void updateImageView() {
        imageView.setImage(pageImages.get(currentPage));
        imageView.setFitWidth(pageImages.get(currentPage).getWidth());
        imageView.setFitHeight(pageImages.get(currentPage).getHeight());
    }

    private void updatePageLabel() {
        pageLabel.setText(String.format("Страница %d из %d%s",
                currentPage + 1,
                pageImages.size(),
                (addBlankPage && currentPage == pageImages.size() - 1) ? " (пустая)" : ""));
    }
}