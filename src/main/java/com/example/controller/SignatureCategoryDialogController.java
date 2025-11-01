package com.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.example.model.SignatureDetails;

import java.util.Optional;

public class SignatureCategoryDialogController {
    @FXML private Label ownerLabel;
    @FXML private VBox dialogContainer;

    private String docType;
    private String rightTitle;
    private String additionalTitle;

    public void initialize(String ownerLine, String docType, String rightTitle, String additionalTitle) {
        this.docType = docType;
        this.rightTitle = rightTitle;
        this.additionalTitle = additionalTitle;
        ownerLabel.setText(ownerLine);
    }

    public static int showDialog(String ownerLine, String rightTitle, String additionalTitle) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Определение принадлежности подписи");
        dialog.setHeaderText("Выберите категорию для этой подписи:");

        // Находим начало информации о владельце
        String[] lines = ownerLine.split("\n");
        StringBuilder ownerInfo = new StringBuilder();

        // Ищем строку, начинающуюся с "Владелец:"
        for (String line : lines) {
            if (line.startsWith("Владелец:")) {
                ownerInfo.append(line).append("\n");
            } else if (ownerInfo.length() > 0) {
                // Добавляем последующие строки (организация, ФИО)
                ownerInfo.append(line).append("\n");
            }
        }

        // Если не нашли "Владелец:", берем последнюю строку (ФИО)
        if (ownerInfo.length() == 0 && lines.length > 0) {
            ownerInfo.append(lines[lines.length - 1]);
        }

        // Создаем текстовую область для отображения информации
        TextArea textArea = new TextArea(ownerInfo.toString().trim());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setStyle("-fx-font-family: monospace;");

        VBox content = new VBox(10,
                new Label("Информация о подписи:"),
                textArea,
                new Label("Выберите категорию:")
        );
        content.setPadding(new Insets(15));

        dialog.getDialogPane().setContent(content);

        // Кнопки выбора
        ButtonType bankButton = new ButtonType("Подписант со стороны банка", ButtonBar.ButtonData.RIGHT);
        ButtonType rightButton = new ButtonType(rightTitle, ButtonBar.ButtonData.LEFT);
        ButtonType additionalButton = new ButtonType(additionalTitle, ButtonBar.ButtonData.OTHER);

        dialog.getDialogPane().getButtonTypes().addAll(bankButton, rightButton, additionalButton);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == bankButton) return 2;
            if (result.get() == rightButton) return 0;
            if (result.get() == additionalButton) return 1;
        }
        return -1;
    }

    private static SignatureDetails parseSignatureFromText(String ownerLine) {
        SignatureDetails details = new SignatureDetails();
        String[] lines = ownerLine.split("\n");

        // ФИО всегда последняя строка
        details.setFullName(lines[lines.length-1].trim());

        // Анализируем остальные строки
        if (lines.length > 1) {
            String firstLine = lines[0].replace("Владелец: ", "").trim();

            if (lines.length == 2) {
                // Вариант 1: Владелец: должность\nФИО
                // Вариант 2: Владелец: организация\nФИО
                if (isCompanyName(firstLine)) {
                    details.setCompany(firstLine);
                } else {
                    details.setPosition(firstLine);
                }
            } else if (lines.length >= 3) {
                // Формат: Владелец: должность\nорганизация\nФИО
                details.setPosition(firstLine);
                details.setCompany(lines[1].trim());
            }
        }

        return details;
    }

    private static class SignatureInfo {
        String position = "";
        String company = "";
        String fullName;
        boolean isLegalEntity = false;
    }

    public static boolean isCompanyName(String text) {
        // Проверяем признаки названия компании
        return text.matches(".*(ООО|АО|ПАО|ЗАО|ИП|LLC|LTD|INC|CORP).*") ||
                text.contains("\"") ||
                text.matches("[А-Я]{2,}.*");
    }

    private static boolean isLegalEntitySignature(String company, String[] lines) {
        // Если явно найдена организация
        if (!company.isEmpty() && !isPersonName(company)) {
            return true;
        }

        // Дополнительные проверки по структуре данных
        if (lines.length >= 3) {
            // Если есть три строки (Владелец, Организация, ФИО) - это юрлицо
            return true;
        }

        // Проверяем CN на наличие признаков организации
        if (lines.length >= 2) {
            String possibleCompany = lines[1];
            return !isPersonName(possibleCompany) &&
                    (possibleCompany.contains("ООО") ||
                            possibleCompany.contains("АО") ||
                            possibleCompany.contains("\""));
        }

        return false;
    }

    private static boolean isPersonName(String text) {
        // Проверяем, является ли текст ФИО (содержит только буквы, пробелы и точки)
        return text.matches("[А-Яа-яЁё\\s.]+");
    }

    private static String formatOwnerInfoForDialog(String ownerLine) {
        // Разбиваем на строки
        String[] lines = ownerLine.split("\n");
        StringBuilder result = new StringBuilder();

        // Первая строка - должность/организация (если есть)
        if (lines.length > 1) {
            result.append(lines[0]).append("\n");
        }

        // Последняя строка - всегда ФИО
        result.append("ФИО: ").append(lines[lines.length - 1]);

        return result.toString();
    }
}