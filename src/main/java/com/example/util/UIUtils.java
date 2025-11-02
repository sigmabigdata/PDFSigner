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
        showStyledAlert(Alert.AlertType.ERROR, "Ошибка", null, message);
    }

    public static void showSuccessAlert(String message) {
        showStyledAlert(Alert.AlertType.INFORMATION, "Успех", null, message);
    }

    public static void showWarningAlert(String message) {
        showStyledAlert(Alert.AlertType.WARNING, "Предупреждение", null, message);
    }

    private static void showStyledAlert(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(UIUtils.class.getResource("/com/example/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }

    public static Optional<ProtocolData> showProtocolInputDialog(String defaultDocType, List<String> signers) {
        Dialog<ProtocolData> dialog = new Dialog<>();
        dialog.setTitle("Данные для протокола проверки");
        dialog.setHeaderText("Введите данные протокола");

        // Стилизация диалога
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(UIUtils.class.getResource("/com/example/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        // Создаем элементы формы
        TextField docTypeField = new TextField(defaultDocType);
        TextField docNumberField = new TextField();
        TextField docDateField = new TextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        TextField verificationDateField = new TextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        TextField employeeNameField = new TextField();

        // Применяем стили к полям ввода
        docTypeField.getStyleClass().add("modern-text-field");
        docNumberField.getStyleClass().add("modern-text-field");
        docDateField.getStyleClass().add("modern-text-field");
        verificationDateField.getStyleClass().add("modern-text-field");
        employeeNameField.getStyleClass().add("modern-text-field");

        // Список для отображения подписантов
        ListView<String> signersList = new ListView<>();
        signersList.getItems().addAll(signers);
        signersList.setPrefHeight(120);
        signersList.getStyleClass().add("list-view");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("modern-grid");

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

        // Стилизация кнопок
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("modern-button");
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        // Валидация
        okButton.setDisable(true);
        docNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateOkButton(okButton, docNumberField, employeeNameField, docTypeField);
        });
        employeeNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateOkButton(okButton, docNumberField, employeeNameField, docTypeField);
        });
        docTypeField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateOkButton(okButton, docNumberField, employeeNameField, docTypeField);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new ProtocolData(
                        docTypeField.getText(),
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

    private static void updateOkButton(Node okButton, TextField docNumberField,
                                       TextField employeeNameField, TextField docTypeField) {
        boolean isValid = !docNumberField.getText().trim().isEmpty() &&
                !employeeNameField.getText().trim().isEmpty() &&
                !docTypeField.getText().trim().isEmpty();
        okButton.setDisable(!isValid);
    }

    public static Optional<ProtocolSettings> showProtocolSettingsDialog() {
        Dialog<ProtocolSettings> dialog = new Dialog<>();
        dialog.setTitle("Настройки протокола");
        dialog.setHeaderText("Настройки создания протокола");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(UIUtils.class.getResource("/com/example/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        CheckBox blankPageCheckBox = new CheckBox("Добавить пустой лист для дополнительной информации");
        blankPageCheckBox.getStyleClass().add("check-box");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getChildren().add(blankPageCheckBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Стилизация кнопок
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("modern-button");
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new ProtocolSettings(blankPageCheckBox.isSelected());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static Optional<String> showPagesInputDialog(File pdfFile) throws IOException {
        TextInputDialog dialog = new TextInputDialog("-1");
        dialog.setTitle("Ввод страниц");
        dialog.setHeaderText(String.format("Введите номера страниц для штампов (1-%d, -1 для последней):\n"
                        + "Примеры: '1,3,5' или '1-3,5'",
                PDFSigner.getPageCount(pdfFile)));
        dialog.setContentText("Номера страниц:");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(UIUtils.class.getResource("/com/example/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        return dialog.showAndWait();
    }
}