package com.example.controller;

import com.example.util.PDFAreaSelector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.example.model.*;
import com.example.util.PDFSigner;
import com.example.util.UIUtils;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import javafx.animation.*;
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

    // Для управления анимациями и предотвращения утечек памяти
    private final List<Animation> activeAnimations = new ArrayList<>();
    private Timeline processingAnimation;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        setupWindowDragging();
        setupCloseHandler();
    }

    private void setupWindowDragging() {
        if (primaryStage != null) {
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];

            mainContainer.setOnMousePressed(event -> {
                xOffset[0] = primaryStage.getX() - event.getScreenX();
                yOffset[0] = primaryStage.getY() - event.getScreenY();
            });

            mainContainer.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() + xOffset[0]);
                primaryStage.setY(event.getScreenY() + yOffset[0]);
            });
        }
    }

    private void setupCloseHandler() {
        if (primaryStage != null) {
            primaryStage.setOnCloseRequest(event -> {
                shutdown();
            });
        }
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

        appendStatus("Система инициализирована", "УСПЕХ");
        appendStatus("Готов к работе", "ИНФО");
    }

    private void setupModernDesign() {
        // Упрощенный дизайн без лишних эффектов
    }

    private void startFastEntranceAnimations() {
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            animateFastEntrance();
        }));
        delayTimeline.play();
        trackAnimation(delayTimeline);
    }

    private void animateFastEntrance() {
        ParallelTransition parallelTransition = new ParallelTransition();

        List<Node> animatedNodes = new ArrayList<>();
        collectAnimatableNodes(mainContainer, animatedNodes);

        for (int i = 0; i < animatedNodes.size(); i++) {
            Node node = animatedNodes.get(i);
            if (node.isVisible()) {
                node.setOpacity(0);
                node.setTranslateY(15);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(100), node);
                fadeIn.setToValue(1);
                fadeIn.setDelay(Duration.millis(i * 20));

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(100), node);
                slideIn.setToY(0);
                slideIn.setDelay(Duration.millis(i * 20));

                parallelTransition.getChildren().addAll(fadeIn, slideIn);
            }
        }

        parallelTransition.play();
        trackAnimation(parallelTransition);
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

        File selectedFile = fileChooser.showOpenDialog(getWindow());
        if (selectedFile != null) {
            employeeSignatureFile = selectedFile;
            appendStatus("Добавлено изображение подписи: " + selectedFile.getName(), "УСПЕХ");
        }
    }

    @FXML
    private void handleNewDocument() {
        playFastButtonAnimation();

        // Очистка ресурсов
        cleanupResources();

        pdfFiles.clear();
        sigFiles.clear();
        proxyInfo = null;
        employeeSignatureFile = null;
        statusTextArea.clear();
        appendStatus("Готов к работе. Выберите файлы для нового документа.", "ИНФО");
    }

    @FXML
    private void handleSelectFiles() {
        playFastButtonAnimation();

        // Создаем FileChooser который позволяет выбирать все файлы
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите PDF файлы и файлы подписи (.sig)");

        // Устанавливаем фильтр который показывает все поддерживаемые файлы
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Все поддерживаемые файлы", "*.pdf", "*.sig"),
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"),
                new FileChooser.ExtensionFilter("SIG файлы", "*.sig"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            processSelectedFiles(selectedFiles);
        }
    }

    private void processSelectedFiles(List<File> selectedFiles) {
        // Отмена предыдущей анимации обработки если есть
        if (processingAnimation != null) {
            processingAnimation.stop();
        }

        pdfFiles.clear();
        sigFiles.clear();
        statusTextArea.clear();

        processingAnimation = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    appendStatus("Сканирование файлов...", "ИНФО");
                }),
                new KeyFrame(Duration.millis(150), e -> {
                    for (File file : selectedFiles) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".pdf")) {
                            pdfFiles.add(file);
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
                                String[] lines = signerInfo.split("\n");
                                String ownerLine = lines.length > 0 ? lines[lines.length - 1] : "Неизвестно";
                                appendStatus("  • " + sigFile.getName() + " (" + ownerLine + ")", "ИНФО");
                            } catch (Exception ex) {
                                appendStatus("  • " + sigFile.getName() + " (не удалось прочитать информацию о подписи)", "ПРЕДУПРЕЖДЕНИЕ");
                            }
                        }
                    }
                })
        );

        processingAnimation.play();
        trackAnimation(processingAnimation);
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

        File proxyFile = fileChooser.showOpenDialog(getWindow());
        if (proxyFile != null) {
            try {
                proxyInfo = parseProxyFile(proxyFile);
                appendStatus("Добавлена доверенность:", "УСПЕХ");
                appendStatus("  Номер: " + proxyInfo.getNumber(), "ИНФО");
                appendStatus("  Срок действия: с " + proxyInfo.getIssueDate() + " по " + proxyInfo.getExpiryDate(), "ИНФО");
            } catch (Exception e) {
                showAlert("Ошибка", "Ошибка при чтении файла доверенности: " + e.getMessage());
                appendStatus("Ошибка при чтении файла доверенности: " + e.getMessage(), "ОШИБКА");
            }
        }
    }

    @FXML
    private void handleSign() {
        playFastButtonAnimation();

        if (pdfFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбран PDF файл!");
            return;
        }

        if (sigFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбраны файлы подписей (.sig)!");
            return;
        }

        String docType = docTypeComboBox.getValue();
        if (docType == null) {
            showAlert("Ошибка", "Не выбран тип документа!");
            return;
        }

        try {
            showProcessingAnimation();

            String leftTitle = PDFSigner.getLeftColumnTitle(docType);
            String rightTitle = PDFSigner.getRightColumnTitle(docType);
            String additionalTitle = PDFSigner.getAdditionalTitle(docType);

            SignatureDistribution distribution = PDFSigner.distributeSignatures(sigFiles, rightTitle, additionalTitle);
            SignatureInfo signatureInfo = PDFSigner.processSignatures(distribution);

            if (signatureInfo.isEmpty()) {
                showAlert("Ошибка", "Нет информации о подписях");
                hideProcessingAnimation();
                return;
            }

            File pdfFile = pdfFiles.get(0);
            Optional<String> pagesInput = UIUtils.showPagesInputDialog(pdfFile);
            if (!pagesInput.isPresent()) {
                hideProcessingAnimation();
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

        } catch (Exception e) {
            hideProcessingAnimation();
            showAlert("Ошибка", "Ошибка: " + e.getMessage());
            appendStatus("Ошибка: " + e.getMessage(), "ОШИБКА");
        }
    }

    @FXML
    private void handleCreateProtocol() {
        playFastButtonAnimation();

        if (sigFiles.isEmpty()) {
            showAlert("Ошибка", "Не выбраны файлы подписей (.sig)!");
            return;
        }

        String defaultDocType = docTypeComboBox.getValue();
        if (defaultDocType == null) {
            showAlert("Ошибка", "Не выбран тип документа!");
            return;
        }

        Optional<ProtocolSettings> settings = UIUtils.showProtocolSettingsDialog();
        if (!settings.isPresent()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите PDF файл для размещения протокола");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"));

        File protocolPdfFile = fileChooser.showOpenDialog(getWindow());
        if (protocolPdfFile == null) {
            return;
        }

        try {
            showProcessingAnimation();

            List<String> signers = sigFiles.stream()
                    .map(sigFile -> {
                        try {
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
                            return sb.toString();
                        } catch (Exception e) {
                            return sigFile.getName() + " (ошибка чтения)";
                        }
                    })
                    .collect(Collectors.toList());

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
                employeeSignatureFile = null; // Очищаем ссылку на файл
            }
        } catch (Exception e) {
            hideProcessingAnimation();
            showAlert("Ошибка", "Ошибка: " + e.getMessage());
            appendStatus("Ошибка при создании протокола: " + e.getMessage(), "ОШИБКА");
        }
    }

    // Упрощенные анимации
    private void playFastButtonAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(75), mainContainer);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.998);
        scale.setToY(0.998);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
        trackAnimation(scale);
    }

    private void showProcessingAnimation() {
        appendStatus("Выполняется обработка...", "ИНФО");
    }

    private void hideProcessingAnimation() {
        statusTextArea.getTransforms().clear();
        statusTextArea.setScaleX(1);
        statusTextArea.setScaleY(1);
    }

    private void appendStatus(String message, String type) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String prefix = switch (type) {
            case "УСПЕХ" -> "✓ ";
            case "ОШИБКА" -> "✗ ";
            case "ПРЕДУПРЕЖДЕНИЕ" -> "! ";
            case "ИНФО" -> "• ";
            default -> "";
        };

        statusTextArea.appendText("[" + timestamp + "] " + prefix + message + "\n");
    }

    private void showAlert(String type, String message) {
        Alert.AlertType alertType = switch (type) {
            case "ОШИБКА" -> Alert.AlertType.ERROR;
            case "ПРЕДУПРЕЖДЕНИЕ" -> Alert.AlertType.WARNING;
            default -> Alert.AlertType.INFORMATION;
        };

        Alert alert = new Alert(alertType);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Устанавливаем владельца для правильного позиционирования
        Window window = getWindow();
        if (window != null) {
            alert.initOwner(window);
        }

        alert.showAndWait();
    }

    private Window getWindow() {
        return (primaryStage != null) ? primaryStage :
                (mainContainer != null && mainContainer.getScene() != null) ?
                        mainContainer.getScene().getWindow() : null;
    }

    // Упрощенный парсинг XML файла доверенности
    private ProxyInfo parseProxyFile(File proxyFile) throws Exception {
        // Простая реализация парсинга XML без внешних зависимостей
        String content = new String(Files.readAllBytes(proxyFile.toPath()));

        // Извлекаем данные с помощью простых строковых операций
        String number = extractXmlValue(content, "НомДовер");
        String issueDate = extractXmlValue(content, "ДатаВыдДовер");
        String expiryDate = extractXmlValue(content, "СрокДейст");

        if (number.isEmpty()) {
            throw new IllegalArgumentException("Не найден номер доверенности");
        }
        if (issueDate.isEmpty()) {
            throw new IllegalArgumentException("Не найдена дата выдачи доверенности");
        }
        if (expiryDate.isEmpty()) {
            throw new IllegalArgumentException("Не найден срок действия доверенности");
        }

        // Простая попытка извлечь ФИО
        String fullName = extractFullNameFromXml(content);

        return new ProxyInfo(number, issueDate, expiryDate, fullName);
    }

    private String extractXmlValue(String xmlContent, String attributeName) {
        // Простой поиск значения атрибута в XML
        String pattern = attributeName + "=\"([^\"]*)\"";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(xmlContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractFullNameFromXml(String xmlContent) {
        // Простая попытка извлечь ФИО из XML
        // Ищем блок с ФИО
        if (xmlContent.contains("ФИО")) {
            // Ищем фамилию, имя, отчество
            String lastName = extractXmlValue(xmlContent, "Фамилия");
            String firstName = extractXmlValue(xmlContent, "Имя");
            String middleName = extractXmlValue(xmlContent, "Отчество");

            if (!lastName.isEmpty() || !firstName.isEmpty() || !middleName.isEmpty()) {
                return String.format("%s %s %s", lastName, firstName, middleName).trim();
            }
        }
        return "";
    }

    // Методы для управления анимациями и предотвращения утечек памяти
    private void trackAnimation(Animation animation) {
        activeAnimations.add(animation);
        animation.setOnFinished(e -> activeAnimations.remove(animation));
    }

    private void cleanupResources() {
        // Останавливаем все активные анимации
        for (Animation animation : activeAnimations) {
            if (animation != null) {
                animation.stop();
            }
        }
        activeAnimations.clear();

        // Останавливаем анимацию обработки
        if (processingAnimation != null) {
            processingAnimation.stop();
            processingAnimation = null;
        }

        // Очищаем коллекции
        pdfFiles.clear();
        sigFiles.clear();

        // Помогаем сборщику мусора
        System.gc();
    }

    // Метод для закрытия приложения и освобождения ресурсов
    public void shutdown() {
        cleanupResources();

        // Дополнительная очистка если нужно
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    // Метод для закрытия окна
    @FXML
    private void handleClose() {
        shutdown();
    }
}