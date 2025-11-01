package com.example.controller;

import com.example.util.PDFAreaSelector;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.example.model.*;
import com.example.util.PDFSigner;
import com.example.util.UIUtils;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import javafx.animation.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class MainController {
    @FXML private VBox mainContainer;
    @FXML private ComboBox<String> docTypeComboBox;
    @FXML private TextArea statusTextArea;

    private Stage primaryStage;
    private ProxyInfo proxyInfo;
    private File employeeSignatureFile;
    private List<File> pdfFiles = new ArrayList<>();
    private List<File> sigFiles = new ArrayList<>();
    private File currentPdfFile;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        setupModernDesign();
        setupAnimations();

        docTypeComboBox.getItems().addAll(
                "Кредитный договор",
                "Банковская гарантия",
                "Договор поручительства",
                "Договор залога",
                "Договор банковской гарантии",
                "Расписка",
                "Кредитное соглашение",
                "Договор Лизинга",
                "Договор купли-продажи"
        );
        docTypeComboBox.getSelectionModel().selectFirst();

        // Добавляем стили для элементов
        applyModernStyles();
    }

    private void setupModernDesign() {
        // Устанавливаем современные эффекты
        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.rgb(0, 0, 0, 0.15));
        cardShadow.setRadius(20);
        cardShadow.setSpread(0.1);
        mainContainer.setEffect(cardShadow);
    }

    private void setupAnimations() {
        // Анимация появления элементов интерфейса
        SequentialTransition sequentialTransition = new SequentialTransition();

        int delay = 100;
        for (javafx.scene.Node node : mainContainer.getChildren()) {
            if (node instanceof ComboBox || node instanceof Button || node instanceof TextArea) {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), node);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setDelay(Duration.millis(delay));

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), node);
                slideIn.setFromY(20);
                slideIn.setToY(0);
                slideIn.setDelay(Duration.millis(delay));

                ParallelTransition parallelTransition = new ParallelTransition(fadeIn, slideIn);
                sequentialTransition.getChildren().add(parallelTransition);

                delay += 50;
            }
        }

        sequentialTransition.play();
    }

    private void applyModernStyles() {
        // Стилизация комбобокса
        docTypeComboBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-font-size: 14px;");

        // Стилизация текстовой области
        statusTextArea.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif;");
    }

    @FXML
    private void handleAddEmployeeSignature() {
        playButtonClickAnimation();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение подписи сотрудника");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG изображения", "*.png")
        );

        Window window = mainContainer.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            employeeSignatureFile = selectedFile;
            statusTextArea.appendText("✓ Добавлено изображение подписи: " +
                    selectedFile.getName() + "\n");
            playSuccessAnimation();
        }
    }

    @FXML
    private void handleNewDocument() {
        playButtonClickAnimation();

        pdfFiles.clear();
        sigFiles.clear();
        currentPdfFile = null;
        proxyInfo = null;
        employeeSignatureFile = null;
        statusTextArea.clear();
        statusTextArea.appendText("🔄 Готов к работе. Выберите файлы для нового документа.\n");

        playResetAnimation();
    }

    @FXML
    private void handleSelectFiles() {
        playButtonClickAnimation();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите PDF файл и файлы подписи (.sig)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Все файлы", "*.*"),
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"),
                new FileChooser.ExtensionFilter("SIG файлы", "*.sig")
        );

        Window window = mainContainer.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(window);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            pdfFiles.clear();
            sigFiles.clear();
            statusTextArea.clear();

            for (File file : selectedFiles) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".pdf")) {
                    pdfFiles.add(file);
                    currentPdfFile = file;
                    statusTextArea.appendText("📄 Выбран PDF файл: " + file.getName() + "\n");
                } else if (name.endsWith(".sig")) {
                    sigFiles.add(file);
                }
            }

            if (!sigFiles.isEmpty()) {
                statusTextArea.appendText("\n✅ Добавлены подписи:\n");
                for (File sigFile : sigFiles) {
                    try {
                        String signerInfo = PDFSigner.extractSignerInfo(sigFile);
                        String ownerLine = signerInfo.split("\n")[signerInfo.split("\n").length - 1];
                        statusTextArea.appendText("• " + sigFile.getName() + " (" + ownerLine + ")\n");
                    } catch (Exception e) {
                        statusTextArea.appendText("• " + sigFile.getName() + " (не удалось прочитать информацию о подписи)\n");
                    }
                }
                playSuccessAnimation();
            }
        }
    }

    @FXML
    private void handleAddProxy() {
        playButtonClickAnimation();

        // Проверяем, что уже загружен PDF и подписи
        if (pdfFiles == null || sigFiles == null || sigFiles.isEmpty()) {
            UIUtils.showErrorAlert("Сначала выберите PDF файл и файлы подписей!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите XML файл доверенности");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML файлы", "*.xml")
        );

        Window window = getWindow();
        if (window == null) return;

        File proxyFile = fileChooser.showOpenDialog(window);
        if (proxyFile != null) {
            try {
                proxyInfo = parseProxyFile(proxyFile);
                statusTextArea.appendText("\n📋 Добавлена доверенность:\n");
                statusTextArea.appendText("🔢 Номер: " + proxyInfo.getNumber() + "\n");
                statusTextArea.appendText("📅 Срок действия: с " + proxyInfo.getIssueDate() +
                        " по " + proxyInfo.getExpiryDate() + "\n");
                playSuccessAnimation();
            } catch (Exception e) {
                UIUtils.showErrorAlert("Ошибка при чтении файла доверенности: " + e.getMessage());
                statusTextArea.appendText("\n❌ Ошибка при чтении файла доверенности: " + e.getMessage() + "\n");
                playErrorAnimation();
            }
        }
    }

    private ProxyInfo parseProxyFile(File proxyFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(proxyFile);

        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            // Получаем номер доверенности
            String number = xpath.evaluate("//*[local-name()='СвДов']/@НомДовер", document);
            if (number.isEmpty()) {
                throw new IllegalArgumentException("Не найден номер доверенности (НомДовер)");
            }

            // Получаем дату выдачи
            String issueDate = xpath.evaluate("//*[local-name()='СвДов']/@ДатаВыдДовер", document);
            if (issueDate.isEmpty()) {
                throw new IllegalArgumentException("Не найдена дата выдачи доверенности (ДатаВыдДовер)");
            }

            // Получаем срок действия
            String expiryDate = xpath.evaluate("//*[local-name()='СвДов']/@СрокДейст", document);
            if (expiryDate.isEmpty()) {
                throw new IllegalArgumentException("Не найден срок действия доверенности (СрокДейст)");
            }

            // Получаем ФИО доверенного лица
            String fullName = "";
            NodeList nameNodes = (NodeList) xpath.evaluate("//*[local-name()='СвУпПред']//*[local-name()='ФИО']",
                    document, XPathConstants.NODESET);
            if (nameNodes.getLength() > 0) {
                Element nameElement = (Element) nameNodes.item(0);
                String lastName = nameElement.getAttribute("Фамилия");
                String firstName = nameElement.getAttribute("Имя");
                String middleName = nameElement.getAttribute("Отчество");
                fullName = String.format("%s %s %s", lastName, firstName, middleName).trim();
            }

            return new ProxyInfo(number, issueDate, expiryDate, fullName);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Ошибка при разборе XML файла доверенности", e);
        }
    }

    @FXML
    private void handleSign() {
        playButtonClickAnimation();

        if (pdfFiles.isEmpty()) {
            UIUtils.showErrorAlert("Не выбран PDF файл!");
            playErrorAnimation();
            return;
        }

        if (sigFiles.isEmpty()) {
            UIUtils.showErrorAlert("Не выбраны файлы подписей (.sig)!");
            playErrorAnimation();
            return;
        }

        String docType = docTypeComboBox.getValue();
        if (docType == null) {
            UIUtils.showErrorAlert("Не выбран тип документа!");
            playErrorAnimation();
            return;
        }

        try {
            // Показываем анимацию загрузки
            showLoadingAnimation();

            String leftTitle = PDFSigner.getLeftColumnTitle(docType);
            String rightTitle = PDFSigner.getRightColumnTitle(docType);
            String additionalTitle = PDFSigner.getAdditionalTitle(docType);

            // Передаем только rightTitle и additionalTitle
            SignatureDistribution distribution = PDFSigner.distributeSignatures(sigFiles, rightTitle, additionalTitle);
            SignatureInfo signatureInfo = PDFSigner.processSignatures(distribution);

            if (signatureInfo.isEmpty()) {
                UIUtils.showErrorAlert("Нет информации о подписях");
                playErrorAnimation();
                return;
            }

            // Используем первый выбранный PDF файл
            File pdfFile = pdfFiles.get(0);
            Optional<String> pagesInput = UIUtils.showPagesInputDialog(pdfFile);
            if (!pagesInput.isPresent()) {
                return;
            }

            List<Integer> requestedPages = PDFSigner.parsePageNumbers(pagesInput.get(),
                    PDFSigner.getPageCount(pdfFile));

            PDFSigner.processDocument(pdfFile, requestedPages, signatureInfo,
                    leftTitle, rightTitle, additionalTitle, proxyInfo);

            hideLoadingAnimation();
            UIUtils.showSuccessAlert("Документ успешно подписан!");
            statusTextArea.appendText("✅ Обработка завершена успешно.\n");
            statusTextArea.appendText("📑 Штампы добавлены на страницы: " +
                    requestedPages.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "\n");

            playSuccessAnimation();

        } catch (Exception e) {
            hideLoadingAnimation();
            UIUtils.showErrorAlert("Ошибка: " + e.getMessage());
            statusTextArea.appendText("❌ Ошибка: " + e.getMessage() + "\n");
            playErrorAnimation();
        }
    }

    @FXML
    private void handleCreateProtocol() {
        playButtonClickAnimation();

        if (sigFiles.isEmpty()) {
            UIUtils.showErrorAlert("Не выбраны файлы подписей (.sig)!");
            playErrorAnimation();
            return;
        }

        String defaultDocType = docTypeComboBox.getValue();
        if (defaultDocType == null) {
            UIUtils.showErrorAlert("Не выбран тип документа!");
            playErrorAnimation();
            return;
        }

        // Запрашиваем настройки протокола
        Optional<ProtocolSettings> settings = UIUtils.showProtocolSettingsDialog();
        if (!settings.isPresent()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите PDF файл для размещения протокола");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"));

        Window window = mainContainer.getScene().getWindow();
        File protocolPdfFile = fileChooser.showOpenDialog(window);

        if (protocolPdfFile == null) {
            return;
        }

        try {
            showLoadingAnimation();

            // Получаем информацию о подписантах
            List<String> signers = new ArrayList<>();
            for (File sigFile : sigFiles) {
                String signerInfo = PDFSigner.extractSignerInfo(sigFile);
                SignatureDetails details = PDFSigner.parseSignatureFromText(signerInfo);
                StringBuilder sb = new StringBuilder();
                if (!details.getPosition().isEmpty()) {
                    sb.append(details.getPosition());
                }
                if (!details.getCompany().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(details.getCompany());
                }
                if (!details.getFullName().isEmpty()) {
                    if (sb.length() > 0) sb.append(" - ");
                    sb.append(details.getFullName());
                }
                signers.add(sb.toString());
            }

            // Выбираем место для протокола
            Optional<PDFAreaSelector.SelectedArea> selectedArea =
                    new PDFAreaSelector(protocolPdfFile, settings.get().isAddBlankPage())
                            .selectArea(primaryStage);

            if (!selectedArea.isPresent()) {
                statusTextArea.appendText("⏹️ Отменено размещение протокола\n");
                hideLoadingAnimation();
                return;
            }

            // Запрашиваем данные протокола с возможностью редактирования типа документа
            Optional<ProtocolData> protocolData = UIUtils.showProtocolInputDialog(
                    defaultDocType, signers);

            if (protocolData.isPresent()) {
                PDFSigner.createProtocol(
                        protocolPdfFile,
                        protocolData.get(),
                        selectedArea.get(),
                        employeeSignatureFile,
                        settings.get()
                );

                hideLoadingAnimation();
                UIUtils.showSuccessAlert("Протокол проверки успешно создан!");
                statusTextArea.appendText("✅ Протокол добавлен в файл: " + protocolPdfFile.getName() + "\n");
                employeeSignatureFile = null;

                playSuccessAnimation();
            }
        } catch (Exception e) {
            hideLoadingAnimation();
            UIUtils.showErrorAlert("Ошибка: " + e.getMessage());
            statusTextArea.appendText("❌ Ошибка при создании протокола: " + e.getMessage() + "\n");
            playErrorAnimation();
            e.printStackTrace();
        }
    }

    private Window getWindow() {
        return (primaryStage != null) ? primaryStage :
                (mainContainer != null && mainContainer.getScene() != null) ?
                        mainContainer.getScene().getWindow() : null;
    }

    private void processSelectedFiles(List<File> selectedFiles) {
        pdfFiles.clear();
        sigFiles.clear();
        statusTextArea.clear();

        for (File file : selectedFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".pdf")) {
                pdfFiles.add(file);
                statusTextArea.appendText("📄 Выбран PDF файл: " + file.getName() + "\n");
            } else if (name.endsWith(".sig")) {
                sigFiles.add(file);
            }
        }

        if (pdfFiles.isEmpty()) {
            UIUtils.showErrorAlert("Не выбран PDF файл!");
            statusTextArea.appendText("❌ Ошибка: не выбран PDF файл!\n");
            playErrorAnimation();
            return;
        }

        if (!sigFiles.isEmpty()) {
            statusTextArea.appendText("\n✅ Добавлены подписи:\n");
            for (File sigFile : sigFiles) {
                try {
                    String signerInfo = PDFSigner.extractSignerInfo(sigFile);
                    String ownerLine = signerInfo.split("\n")[signerInfo.split("\n").length - 1];
                    statusTextArea.appendText("• " + sigFile.getName() + " (" + ownerLine + ")\n");
                } catch (Exception e) {
                    statusTextArea.appendText("• " + sigFile.getName() + " (не удалось прочитать информацию о подписи)\n");
                }
            }
            playSuccessAnimation();
        } else {
            statusTextArea.appendText("⚠️ Предупреждение: не выбраны файлы подписей (.sig)\n");
        }
    }

    private boolean checkProxyNameMatch(SignatureInfo signatureInfo, ProxyInfo proxyInfo) throws Exception {
        if (proxyInfo == null) {
            return false;
        }

        String proxyName = proxyInfo.getFullName();
        boolean matchFound = false;
        List<String> allSigners = new ArrayList<>();

        // Собираем все подписи в один список для проверки
        allSigners.addAll(signatureInfo.bankSignerInfos);
        allSigners.addAll(signatureInfo.rightSignerInfos);
        allSigners.addAll(signatureInfo.additionalSignerInfos);

        for (String signerInfo : allSigners) {
            String ownerLine = signerInfo.split("\n")[signerInfo.split("\n").length - 1];
            String signerName = ownerLine.replace("Владелец: ", "").trim();

            if (compareNames(signerName, proxyName)) {
                matchFound = true;
                break;
            }
        }

        if (!matchFound) {
            // Логируем информацию для отладки
            System.out.println("Не найдено соответствие для доверенности:");
            System.out.println("ФИО в доверенности: " + proxyName);
            System.out.println("Доступные подписи:");
            for (String signerInfo : allSigners) {
                String ownerLine = signerInfo.split("\n")[signerInfo.split("\n").length - 1];
                System.out.println("- " + ownerLine);
            }
        }

        return matchFound;
    }

    private boolean compareNames(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        // Нормализуем строки: приводим к нижнему регистру и разбиваем на части
        String[] parts1 = name1.trim().toLowerCase().split("\\s+");
        String[] parts2 = name2.trim().toLowerCase().split("\\s+");

        // Проверяем, что все части имени присутствуют в обоих строках
        for (String part : parts1) {
            if (!Arrays.asList(parts2).contains(part)) {
                return false;
            }
        }
        for (String part : parts2) {
            if (!Arrays.asList(parts1).contains(part)) {
                return false;
            }
        }

        return true;
    }

    // Анимации
    private void playButtonClickAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), mainContainer);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.99);
        scale.setToY(0.99);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private void playSuccessAnimation() {
        Glow glow = new Glow();
        glow.setLevel(0.3);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(glow.levelProperty(), 0.3)),
                new KeyFrame(Duration.millis(400), new KeyValue(glow.levelProperty(), 0))
        );

        mainContainer.setEffect(glow);
        timeline.setOnFinished(e -> mainContainer.setEffect(new DropShadow()));
        timeline.play();
    }

    private void playErrorAnimation() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), mainContainer);
        shake.setFromX(0);
        shake.setToX(10);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.play();
    }

    private void playResetAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), statusTextArea);
        fade.setFromValue(0.5);
        fade.setToValue(1);
        fade.play();
    }

    private void showLoadingAnimation() {
        // Можно добавить индикатор загрузки в будущем
        statusTextArea.appendText("⏳ Выполняется обработка...\n");
    }

    private void hideLoadingAnimation() {
        // Скрыть индикатор загрузки
    }
}