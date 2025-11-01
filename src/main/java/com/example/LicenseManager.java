// LicenseManager.java
package com.example.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.prefs.Preferences;

public class LicenseManager {
    private static final String LICENSE_ACCEPTED_KEY = "license_accepted";
    private static final String LICENSE_VERSION_KEY = "license_version";
    private static final String CURRENT_LICENSE_VERSION = "1.0";

    private static Preferences prefs = Preferences.userNodeForPackage(LicenseManager.class);

    public static boolean isLicenseAccepted() {
        return prefs.getBoolean(LICENSE_ACCEPTED_KEY, false) &&
                CURRENT_LICENSE_VERSION.equals(prefs.get(LICENSE_VERSION_KEY, ""));
    }

    public static void setLicenseAccepted(boolean accepted) {
        prefs.putBoolean(LICENSE_ACCEPTED_KEY, accepted);
        prefs.put(LICENSE_VERSION_KEY, CURRENT_LICENSE_VERSION);
    }

    public static boolean showLicenseAgreement(Stage parentStage) {
        // Если лицензия уже принята, не показываем диалог
        if (isLicenseAccepted()) {
            return true;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Лицензионное соглашение");
        dialog.setHeaderText("Пожалуйста, ознакомьтесь с лицензионным соглашением");
        dialog.initOwner(parentStage);

        // Создаем текстовую область с лицензией
        TextArea textArea = new TextArea(getLicenseText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(60);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        // Чекбокс принятия соглашения
        CheckBox acceptCheckBox = new CheckBox("Я принимаю условия лицензионного соглашения");

        VBox content = new VBox(10, textArea, acceptCheckBox);
        content.setPadding(new Insets(15));
        content.setPrefSize(600, 400);

        dialog.getDialogPane().setContent(content);

        // Настраиваем кнопки
        ButtonType acceptButton = new ButtonType("Принять", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButton = new ButtonType("Отклонить", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButton, rejectButton);

        // Делаем кнопку "Принять" недоступной пока не отмечен чекбокс
        Button acceptButtonNode = (Button) dialog.getDialogPane().lookupButton(acceptButton);
        acceptButtonNode.setDisable(true);

        acceptCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            acceptButtonNode.setDisable(!newVal);
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == acceptButton) {
            setLicenseAccepted(true);
            return true;
        } else {
            return false;
        }
    }

    public static void showLicenseText() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Лицензионное соглашение");
        alert.setHeaderText("Полный текст лицензионного соглашения");

        TextArea textArea = new TextArea(getLicenseText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(60);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefSize(600, 400);
        alert.showAndWait();
    }

    private static String getLicenseText() {
        return """
                ЛИЦЕНЗИОННОЕ СОГЛАШЕНИЕ С КОНЕЧНЫМ ПОЛЬЗОВАТЕЛЕМ
                для программы "Визуализатор штампов электронной подписи" версии 1.0

                НАСТОЯЩЕЕ ЛИЦЕНЗИОННОЕ СОГЛАШЕНИЕ (ДАЛЕЕ — «СОГЛАШЕНИЕ») ЯВЛЯЕТСЯ ЮРИДИЧЕСКИМ
                СОГЛАШЕНИЕМ МЕЖДУ ВАМИ (ФИЗИЧЕСКИМ ЛИЦОМ ИЛИ ЮРИДИЧЕСКИМ ЛИЦОМ) И
                ДЫРУЛ АНАТОЛИЕМ В ОТНОШЕНИИ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ, УКАЗАННОГО ВЫШЕ.

                УСТАНАВЛИВАЯ, КОПИРУЯ ИЛИ ИНЫМ СПОСОБОМ ИСПОЛЬЗУЯ ПРОГРАММУ, ВЫ ПОДТВЕРЖДАЕТЕ
                СВОЕ СОГЛАСИЕ С УСЛОВИЯМИ НАСТОЯЩЕГО СОГЛАШЕНИЯ. ЕСЛИ ВЫ НЕ СОГЛАСНЫ С УСЛОВИЯМИ
                СОГЛАШЕНИЯ, НЕ УСТАНАВЛИВАЙТЕ И НЕ ИСПОЛЬЗУЙТЕ ПРОГРАММУ.

                1. ПРЕДОСТАВЛЕНИЕ ЛИЦЕНЗИИ
                Дырул Анатолий предоставляет вам ограниченную, неисключительную,
                неподлежащую передаче лицензию на использование Программы исключительно для
                внутренних бизнес-процессов [Название Вашей Компании].

                2. ОГРАНИЧЕНИЯ
                - Запрещается реинжиниринг, декомпиляция и дизассемблирование Программы
                - Запрещается передача, распространение Программы третьим лицам
                - Запрещается использование Программы вне служебной деятельности

                3. КОНФИДЕНЦИАЛЬНОСТЬ
                Исходный код и алгоритмы Программы являются коммерческой тайной
                Дырул Анатолия.

                            """;
    }
}