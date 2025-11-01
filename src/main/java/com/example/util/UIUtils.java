package com.example.util;

import com.example.model.ProtocolData;
import com.example.model.ProtocolSettings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UIUtils {
    public static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static Optional<ProtocolData> showProtocolInputDialog(String defaultDocType, List<String> signers) {
        Dialog<ProtocolData> dialog = new Dialog<>();
        dialog.setTitle("Данные для протокола проверки");

        // Создаем элементы формы
        TextField docTypeField = new TextField(defaultDocType);
        TextField docNumberField = new TextField();
        TextField docDateField = new TextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        TextField verificationDateField = new TextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        TextField employeeNameField = new TextField();

        // Список для отображения подписантов
        ListView<String> signersList = new ListView<>();
        signersList.getItems().addAll(signers);
        signersList.setPrefHeight(150);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Тип документа:"), 0, 0);
        grid.add(docTypeField, 1, 0);
        grid.add(new Label("Номер документа:"), 0, 1);
        grid.add(docNumberField, 1, 1);
        grid.add(new Label("Дата документа:"), 0, 2);
        grid.add(docDateField, 1, 2);
        grid.add(new Label("Дата проверки:"), 0, 3);
        grid.add(verificationDateField, 1, 3);
        grid.add(new Label("ФИО сотрудника:"), 0, 4);
        grid.add(employeeNameField, 1, 4);
        grid.add(new Label("Список подписантов:"), 0, 5);
        grid.add(signersList, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Валидация
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        docNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(newVal.trim().isEmpty() ||
                    employeeNameField.getText().trim().isEmpty() ||
                    docTypeField.getText().trim().isEmpty());
        });
        employeeNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(newVal.trim().isEmpty() ||
                    docNumberField.getText().trim().isEmpty() ||
                    docTypeField.getText().trim().isEmpty());
        });
        docTypeField.textProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(newVal.trim().isEmpty() ||
                    docNumberField.getText().trim().isEmpty() ||
                    employeeNameField.getText().trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new ProtocolData(
                        docTypeField.getText(), // Используем введенное пользователем значение
                        docNumberField.getText(),
                        docDateField.getText(),
                        verificationDateField.getText(),
                        employeeNameField.getText(),
                        signers
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static Optional<ProtocolSettings> showProtocolSettingsDialog() {
        Dialog<ProtocolSettings> dialog = new Dialog<>();
        dialog.setTitle("Настройки протокола");

        CheckBox blankPageCheckBox = new CheckBox("Добавить пустой лист для дополнительной информации");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(blankPageCheckBox, 0, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new ProtocolSettings(blankPageCheckBox.isSelected());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static Optional<Boolean> showBlankPageConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Добавление страницы");
        alert.setHeaderText("Добавить пустую страницу?");
        alert.setContentText("Хотите добавить пустую страницу для размещения дополнительной информации?");

        ButtonType yesButton = new ButtonType("Да", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == yesButton) {
            return Optional.of(true);
        } else {
            return Optional.of(false);
        }
    }

    public static Optional<String> showPagesInputDialog(File pdfFile) throws IOException {
        TextInputDialog dialog = new TextInputDialog("-1");
        dialog.setTitle("Ввод страниц");
        dialog.setHeaderText(String.format("Введите номера страниц для штампов (1-%d, -1 для последней):\n"
                        + "Примеры: '1,3,5' или '1-3,5'",
                PDFSigner.getPageCount(pdfFile)));
        dialog.setContentText("Номера страниц:");
        return dialog.showAndWait();
    }

    public static void showInformationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}