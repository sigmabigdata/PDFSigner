package com.example.controller;

import com.example.util.PDFAreaSelector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Glow;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
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
        startFastEntranceAnimations();

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

        applyModernStyles();

        // Начальное сообщение
        appendStatus("Система инициализирована", "УСПЕХ");
        appendStatus("Готов к работе", "ИНФО");
    }

    private void setupModernDesign() {
        // Неоновое свечение для основного контейнера
        Glow mainGlow = new Glow();
        mainGlow.setLevel(0.2);
        mainContainer.setEffect(mainGlow);
    }

    private void startFastEntranceAnimations() {
        // Быстрая задержка для запуска анимаций
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            animateFastEntrance();
        }));
        delayTimeline.play();
    }

    private void animateFastEntrance() {
        ParallelTransition parallelTransition = new ParallelTransition();

        // Собираем все элементы для анимации
        List<Node> animatedNodes = new ArrayList<>();
        collectAnimatableNodes(mainContainer, animatedNodes);

        for (int i = 0; i < animatedNodes.size(); i++) {
            Node node = animatedNodes.get(i);
            if (node.isVisible()) {
                // Начальное состояние - невидимы и смещены
                node.setOpacity(0);
                node.setTranslateY(15);

                // БЫСТРАЯ анимация появления - в 4 раза быстрее
                FadeTransition fadeIn = new FadeTransition(Duration.millis(100), node); // было 400
                fadeIn.setToValue(1);
                fadeIn.setDelay(Duration.millis(i * 20)); // было 80

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(100), node); // было 400
                slideIn.setToY(0);
                slideIn.setDelay(Duration.millis(i * 20)); // было 80

                parallelTransition.getChildren().addAll(fadeIn, slideIn);
            }
        }

        parallelTransition.play();
    }

    private void collectAnimatableNodes(Parent parent, List<Node> nodes) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Control && node.isVisible()) {
                nodes.add(node);
            }
            if (node instanceof Parent) {
                collectAnimatableNodes((Parent) node, nodes);
            }
        }
    }

    private void applyModernStyles() {
        statusTextArea.setStyle("-fx-font-family: 'SF Mono', 'Cascadia Code', monospace; -fx-font-size: 13px;");
    }

    @FXML
    private void handleAddEmployeeSignature() {
        playFastButtonAnimation();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение подписи сотрудника");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG изображения", "*.png")
        );

        Window window = mainContainer.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            employeeSignatureFile = selectedFile;
            appendStatus("Добавлено изображение подписи: " + selectedFile.getName(), "УСПЕХ");
            playFastSuccessAnimation();
        }
    }

    @FXML
    private void handleNewDocument() {
        playFastButtonAnimation();

        pdfFiles.clear();
        sigFiles.clear();
        currentPdfFile = null;
        proxyInfo = null;
        employeeSignatureFile = null;
        statusTextArea.clear();
        appendStatus("Готов к работе. Выберите файлы для нового документа.", "ИНФО");

        playFastResetAnimation();
    }

    @FXML
    private void handleSelectFiles() {
        playFastButtonAnimation();

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
            processSelectedFilesWithFastAnimation(selectedFiles);
        }
    }

    private void processSelectedFilesWithFastAnimation(List<File> selectedFiles) {
        pdfFiles.clear();
        sigFiles.clear();
        statusTextArea.clear();

        // БЫСТРАЯ анимация обработки файлов
        Timeline processingAnimation = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    appendStatus("Сканирование файлов...", "ИНФО");
                }),
                new KeyFrame(Duration.millis(150), e -> {
                    for (File file : selectedFiles) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".pdf")) {
                            pdfFiles.add(file);
                            currentPdfFile = file;
                            appendStatus("Выбран PDF файл: " + file.getName(), "УСПЕХ");
                        } else if (name.endsWith(".sig")) {
                            sigFiles.add(file);
                        }
                    }
                }),
                new KeyFrame(Duration.millis(250), e -> {
                    if (!sigFiles.isEmpty()) {
                        appendStatus("Добавлены подписи:", "УСПЕХ");
                        for (File sigFile : sigFiles) {
                            try {
                                String signerInfo = PDFSigner.extractSignerInfo(sigFile);
                                String ownerLine = signerInfo.split("\n")[signerInfo.split("\n").length - 1];
                                appendStatus("  • " + sigFile.getName() + " (" + ownerLine + ")", "ИНФО");
                            } catch (Exception ex) {
                                appendStatus("  • " + sigFile.getName() + " (не удалось прочитать информацию о подписи)", "ПРЕДУПРЕЖДЕНИЕ");
                            }
                        }
                        playFastSuccessAnimation();
                    }
                })
        );

        processingAnimation.play();
    }

    @FXML
    private void handleAddProxy() {
        playFastButtonAnimation();

        if (pdfFiles.isEmpty() || sigFiles.isEmpty()) {
            showAlert("Ошибка", "Сначала выберите PDF файл и файлы подписей!");
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
                appendStatus("Добавлена доверенность:", "УСПЕХ");
                appendStatus("  Номер: " + proxyInfo.getNumber(), "ИНФО");
                appendStatus("  Срок действия: с " + proxyInfo.getIssueDate() + " по " + proxyInfo.getExpiryDate(), "ИНФО");
                playFastSuccessAnimation();
            } catch (Exception e) {
                showAlert("Ошибка", "Ошибка при чтении файла доверенности: " + e.getMessage());
                appendStatus("Ошибка при чтении файла доверенности: " + e.getMessage(), "ОШИБКА");
                playFastErrorAnimation();
            }
        }
    }

    @FXML
    private void handleSign() {
        playFastButtonAnimation();

        if (pdfFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбран PDF файл!");
            playFastErrorAnimation();
            return;
        }

        if (sigFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбраны файлы подписей (.sig)!");
            playFastErrorAnimation();
            return;
        }

        String docType = docTypeComboBox.getValue();
        if (docType == null) {
            showAlert("Ошибка", "Не выбран тип документа!");
            playFastErrorAnimation();
            return;
        }

        try {
            showFastProcessingAnimation();

            String leftTitle = PDFSigner.getLeftColumnTitle(docType);
            String rightTitle = PDFSigner.getRightColumnTitle(docType);
            String additionalTitle = PDFSigner.getAdditionalTitle(docType);

            SignatureDistribution distribution = PDFSigner.distributeSignatures(sigFiles, rightTitle, additionalTitle);
            SignatureInfo signatureInfo = PDFSigner.processSignatures(distribution);

            if (signatureInfo.isEmpty()) {
                showAlert("Ошибка", "Нет информации о подписях");
                playFastErrorAnimation();
                return;
            }

            File pdfFile = pdfFiles.get(0);
            Optional<String> pagesInput = UIUtils.showPagesInputDialog(pdfFile);
            if (!pagesInput.isPresent()) {
                return;
            }

            List<Integer> requestedPages = PDFSigner.parsePageNumbers(pagesInput.get(),
                    PDFSigner.getPageCount(pdfFile));

            PDFSigner.processDocument(pdfFile, requestedPages, signatureInfo,
                    leftTitle, rightTitle, additionalTitle, proxyInfo);

            hideProcessingAnimation();
            showAlert("Успех", "Документ успешно подписан!");
            appendStatus("Обработка завершена успешно", "УСПЕХ");
            appendStatus("Штампы добавлены на страницы: " +
                    requestedPages.stream().map(String::valueOf).collect(Collectors.joining(", ")), "ИНФО");

            playFastEpicSuccessAnimation();

        } catch (Exception e) {
            hideProcessingAnimation();
            showAlert("Ошибка", "Ошибка: " + e.getMessage());
            appendStatus("Ошибка: " + e.getMessage(), "ОШИБКА");
            playFastErrorAnimation();
        }
    }

    @FXML
    private void handleCreateProtocol() {
        playFastButtonAnimation();

        if (sigFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбраны файлы подписей (.sig)!");
            playFastErrorAnimation();
            return;
        }

        String defaultDocType = docTypeComboBox.getValue();
        if (defaultDocType == null) {
            showAlert("Ошибка", "Не выбран тип документа!");
            playFastErrorAnimation();
            return;
        }

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
            showFastProcessingAnimation();

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

            Optional<PDFAreaSelector.SelectedArea> selectedArea =
                    new PDFAreaSelector(protocolPdfFile, settings.get().isAddBlankPage())
                            .selectArea(primaryStage);

            if (!selectedArea.isPresent()) {
                appendStatus("Отменено размещение протокола", "ИНФО");
                hideProcessingAnimation();
                return;
            }

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

                hideProcessingAnimation();
                showAlert("Успех", "Протокол проверки успешно создан!");
                appendStatus("Протокол добавлен в файл: " + protocolPdfFile.getName(), "УСПЕХ");
                employeeSignatureFile = null;

                playFastEpicSuccessAnimation();
            }
        } catch (Exception e) {
            hideProcessingAnimation();
            showAlert("Ошибка", "Ошибка: " + e.getMessage());
            appendStatus("Ошибка при создании протокола: " + e.getMessage(), "ОШИБКА");
            playFastErrorAnimation();
        }
    }

    // БЫСТРЫЕ АНИМАЦИИ (в 4 раза быстрее)
    private void playFastButtonAnimation() {
        // Быстрая анимация нажатия кнопки
        ScaleTransition scale = new ScaleTransition(Duration.millis(75), mainContainer); // было 150
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.998);
        scale.setToY(0.998);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private void playFastSuccessAnimation() {
        // Быстрая анимация успеха
        Glow glow = new Glow();
        glow.setLevel(0.3);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.1)),
                new KeyFrame(Duration.millis(100), new KeyValue(glow.levelProperty(), 0.3)), // было 200
                new KeyFrame(Duration.millis(200), new KeyValue(glow.levelProperty(), 0.1))  // было 400
        );

        mainContainer.setEffect(glow);
        timeline.setOnFinished(e -> mainContainer.setEffect(new Glow(0.2)));
        timeline.play();
    }

    private void playFastEpicSuccessAnimation() {
        // Быстрая эпичная анимация
        ParallelTransition parallel = new ParallelTransition();

        // Свечение
        Glow glow = new Glow();
        glow.setLevel(0.6);
        Timeline glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.2)),
                new KeyFrame(Duration.millis(150), new KeyValue(glow.levelProperty(), 0.6)), // было 300
                new KeyFrame(Duration.millis(300), new KeyValue(glow.levelProperty(), 0.2))  // было 600
        );

        // Быстрая вибрация
        TranslateTransition vibrate = new TranslateTransition(Duration.millis(50), mainContainer); // было 100
        vibrate.setFromX(0);
        vibrate.setToX(2);
        vibrate.setAutoReverse(true);
        vibrate.setCycleCount(3);

        parallel.getChildren().addAll(glowTimeline, vibrate);
        parallel.play();
    }

    private void playFastErrorAnimation() {
        // Быстрая анимация ошибки
        TranslateTransition shake = new TranslateTransition(Duration.millis(25), mainContainer); // было 50
        shake.setFromX(0);
        shake.setToX(6);
        shake.setAutoReverse(true);
        shake.setCycleCount(4);
        shake.play();
    }

    private void playFastResetAnimation() {
        // Быстрая анимация сброса
        FadeTransition fade = new FadeTransition(Duration.millis(150), statusTextArea); // было 300
        fade.setFromValue(0.8);
        fade.setToValue(1);
        fade.play();
    }

    private void showFastProcessingAnimation() {
        appendStatus("Выполняется обработка...", "ИНФО");

        // Быстрая анимация пульсации
        ScaleTransition pulse = new ScaleTransition(Duration.millis(500), statusTextArea); // было 1000
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.001);
        pulse.setToY(1.001);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private void hideProcessingAnimation() {
        statusTextArea.getTransforms().clear();
        statusTextArea.setScaleX(1);
        statusTextArea.setScaleY(1);
    }

    private void appendStatus(String message, String type) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = "";

        switch (type) {
            case "УСПЕХ": prefix = "✓ "; break;
            case "ОШИБКА": prefix = "✗ "; break;
            case "ПРЕДУПРЕЖДЕНИЕ": prefix = "! "; break;
            case "ИНФО": prefix = "• "; break;
        }

        statusTextArea.appendText("[" + timestamp + "] " + prefix + message + "\n");
    }

    private void showAlert(String type, String message) {
        Alert.AlertType alertType;
        switch (type) {
            case "ОШИБКА": alertType = Alert.AlertType.ERROR; break;
            case "ПРЕДУПРЕЖДЕНИЕ": alertType = Alert.AlertType.WARNING; break;
            default: alertType = Alert.AlertType.INFORMATION; break;
        }

        Alert alert = new Alert(alertType);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/css/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }

    private Window getWindow() {
        return (primaryStage != null) ? primaryStage :
                (mainContainer != null && mainContainer.getScene() != null) ?
                        mainContainer.getScene().getWindow() : null;
    }

    private ProxyInfo parseProxyFile(File proxyFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(proxyFile);

        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            String number = xpath.evaluate("//*[local-name()='СвДов']/@НомДовер", document);
            String issueDate = xpath.evaluate("//*[local-name()='СвДов']/@ДатаВыдДовер", document);
            String expiryDate = xpath.evaluate("//*[local-name()='СвДов']/@СрокДейст", document);

            if (number.isEmpty()) {
                throw new IllegalArgumentException("Не найден номер доверенности");
            }
            if (issueDate.isEmpty()) {
                throw new IllegalArgumentException("Не найдена дата выдачи доверенности");
            }
            if (expiryDate.isEmpty()) {
                throw new IllegalArgumentException("Не найден срок действия доверенности");
            }

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

    private boolean checkProxyNameMatch(SignatureInfo signatureInfo, ProxyInfo proxyInfo) throws Exception {
        if (proxyInfo == null) {
            return false;
        }

        String proxyName = proxyInfo.getFullName();
        boolean matchFound = false;
        List<String> allSigners = new ArrayList<>();

        allSigners.addAll(signatureInfo.bankSignerInfos);
        allSigners.addAll(signatureInfo.rightSignerInfos);
        allSigners.addAll(signatureInfo.additionalSignerInfos);

        for (String signerInfo : allSigners) {
            String[] lines = signerInfo.split("\n");
            if (lines.length > 0) {
                String ownerLine = lines[lines.length - 1];
                String signerName = ownerLine.replace("Владелец: ", "").trim();

                if (compareNames(signerName, proxyName)) {
                    matchFound = true;
                    break;
                }
            }
        }

        return matchFound;
    }

    private boolean compareNames(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        String[] parts1 = name1.trim().toLowerCase().split("\\s+");
        String[] parts2 = name2.trim().toLowerCase().split("\\s+");

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
}