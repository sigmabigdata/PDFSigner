package com.example.util;

import com.example.controller.SignatureCategoryDialogController;
import com.example.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class PDFSigner {
    private static final float STAMP_PADDING = 8;
    private static final float BASE_FONT_SIZE = 10;
    private static final float LARGE_FONT_SIZE = 15;
    private static final float LINE_SPACING = 12;
    private static final float MIN_TEXT_MARGIN = 20;

    // Уменьшенные размеры шрифтов на 1/3
    private static final float STAMP_MAIN_FONT_SIZE = 6.7f; // 10 * 2/3
    private static final float STAMP_REGULAR_FONT_SIZE = 5.3f; // 8 * 2/3
    private static final float PROTOCOL_FONT_SIZE = 6.7f; // 10 * 2/3
    // Увеличенный размер шрифта для заголовков столбцов на 1/3
    private static final float COLUMN_TITLE_FONT_SIZE = 8.9f; // 6.7 * 4/3

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    public static class PDFContentAnalyzer extends PDFTextStripper {
        List<TextBlock> textBlocks = new ArrayList<>();

        public PDFContentAnalyzer() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty()) {
                return;
            }

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;

            for (TextPosition pos : textPositions) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                maxX = Math.max(maxX, pos.getEndX());
                maxY = Math.max(maxY, pos.getY() + pos.getHeight());
            }

            TextBlock block = new TextBlock();
            block.text = text;
            block.x = minX;
            block.y = minY;
            block.width = maxX - minX;
            block.height = maxY - minY;
            textBlocks.add(block);
        }

        public class TextBlock {
            public String text;
            public float x;
            public float y;
            public float width;
            public float height;
        }

        public static List<TextBlock> analyzePage(PDDocument doc, int pageNum) throws IOException {
            PDFContentAnalyzer analyzer = new PDFContentAnalyzer();
            analyzer.setStartPage(pageNum);
            analyzer.setEndPage(pageNum);
            analyzer.getText(doc);
            return analyzer.textBlocks;
        }
    }


    protected static class Stamp {
        private final String text;
        private final String signerName;
        private final boolean bankEmployee;
        private final float width;
        private final float height;
        private final String[] lines;
        private final PDType0Font regularFont;
        private final PDType0Font boldFont;
        private final boolean hasProxyInfo;

        Stamp(String text, boolean bankEmployee, PDType0Font regularFont, PDType0Font boldFont, boolean hasProxyInfo) throws IOException {
            this.text = text;
            this.bankEmployee = bankEmployee;
            this.regularFont = regularFont;
            this.boldFont = boldFont;
            this.hasProxyInfo = hasProxyInfo;
            this.lines = text.split("\n");
            this.signerName = extractSignerName();

            // Рассчитываем размеры на основе фактического текста
            float maxWidth = 0;
            float totalHeight = 0;

            // Основной текст
            for (int i = 0; i < lines.length; i++) {
                PDFont font = (i < 1) ? boldFont : regularFont;
                float fontSize = (i == 0) ? STAMP_MAIN_FONT_SIZE : STAMP_REGULAR_FONT_SIZE;
                float lineWidth = getLineWidth(lines[i], font, fontSize);
                maxWidth = Math.max(maxWidth, lineWidth);
                totalHeight += getLineHeight(fontSize);
            }

            // Информация о доверенности
            if (hasProxyInfo) {
                totalHeight += getLineHeight(STAMP_REGULAR_FONT_SIZE) * 2; // 2 строки
            } else {
                totalHeight += getLineHeight(STAMP_REGULAR_FONT_SIZE); // 1 строка
            }

            // Добавляем отступы (увеличенный верхний отступ)
            float verticalPadding = 8; // Увеличенный верхний отступ
            float bottomPadding = 4;   // Стандартный нижний отступ
            this.width = maxWidth + 2 * 6; // Горизонтальные отступы
            this.height = totalHeight + verticalPadding + bottomPadding;
        }

        private String extractSignerName() {
            String ownerLine = lines[lines.length - 1];
            return ownerLine.replace("Владелец: ", "").trim();
        }

        public String getSignerName() {
            return signerName;
        }

        private float getLineWidth(String line, PDFont font, float fontSize) throws IOException {
            return font.getStringWidth(line) / 1000 * fontSize;
        }

        private float getLineHeight(float fontSize) {
            return fontSize + 2; // Небольшой межстрочный интервал
        }


        public String getText() {
            return text;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public String[] getLines() {
            return lines;
        }

        public boolean isBankEmployee() {
            return bankEmployee;
        }

        public PDType0Font getRegularFont() {
            return regularFont;
        }

        public PDType0Font getBoldFont() {
            return boldFont;
        }

        public boolean hasProxyInfo() {
            return hasProxyInfo;
        }
    }

    public static String getLeftColumnTitle(String docType) {
        switch (docType) {
            case "Банковская гарантия":
            case "Расписка":
                return "Гарант";
            case "Договор поручительства":
            case "Договор банковской гарантии":
                return "Банк";
            case "Договор залога":
                return "Залогодержатель";
            case "Договор Лизинга":
                return "Лизингодатель";
            case "Договор купли-продажи":
                return "Покупатель";
            case "Кредитное соглашение":
            default:
                return "Банк";
        }
    }

    public static String getRightColumnTitle(String docType) {
        switch (docType) {
            case "Банковская гарантия":
            case "Договор банковской гарантии":
            case "Расписка":
                return "Принципал";
            case "Договор поручительства":
                return "Поручитель";
            case "Договор залога":
                return "Залогодатель";
            case "Договор Лизинга":
                return "Лизингополучатель";
            case "Договор купли-продажи":
                return "Продавец";
            case "Кредитное соглашение":
            default:
                return "Заемщик";
        }
    }

    public static String getAdditionalTitle(String docType) {
        // Убираем переносы строк - используем одну строку
        return "Поручители и/или залогодатели";
    }

    public static int getPageCount(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            return doc.getNumberOfPages();
        }
    }

    public static List<Integer> parsePageNumbers(String input, int totalPages) {
        List<Integer> numbers = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            numbers.add(totalPages);
            return numbers;
        }

        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                String trimmed = part.trim();
                if (trimmed.equals("-1")) {
                    numbers.add(totalPages);
                } else if (trimmed.matches("\\d+")) {
                    int num = Integer.parseInt(trimmed);
                    if (num > 0) {
                        numbers.add(num);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (numbers.isEmpty()) {
            numbers.add(totalPages);
        }
        return numbers;
    }

    public static SignatureDistribution distributeSignatures(List<File> files, String rightTitle, String additionalTitle) {
        SignatureDistribution distribution = new SignatureDistribution();
        for (File file : files) {
            try {
                String signerInfo = extractSignerInfo(file);
                // Передаем только необходимые параметры
                int choice = SignatureCategoryDialogController.showDialog(signerInfo, rightTitle, additionalTitle);
                if (choice == 0) {
                    distribution.rightSigFiles.add(file);
                } else if (choice == 1) {
                    distribution.additionalSigFiles.add(file);
                } else if (choice == 2) {
                    distribution.bankSigFiles.add(file);
                }
            } catch (Exception e) {
                UIUtils.showErrorAlert("Ошибка обработки подписи: " + file.getName() + "\n" + e.getMessage());
            }
        }
        return distribution;
    }

    protected static String parseRussianCertificateOwner(String dn) {
        try {
            X500Name x500name = new X500Name(dn);

            String surname = getRDNAttribute(x500name, BCStyle.SURNAME);
            String givenName = getRDNAttribute(x500name, BCStyle.GIVENNAME);
            String initials = getRDNAttribute(x500name, BCStyle.INITIALS);
            String title = getRDNAttribute(x500name, BCStyle.T);
            String organization = getRDNAttribute(x500name, BCStyle.O);
            String commonName = getRDNAttribute(x500name, BCStyle.CN);

            String fullName = buildFullName(surname, givenName, initials);
            String company = extractCompanyInfo(organization, commonName);

            // Формируем результат
            StringBuilder result = new StringBuilder("Владелец: ");

            if (!title.equals("Не указано")) {
                result.append(title);
            }

            if (!company.isEmpty()) {
                if (!title.equals("Не указано")) {
                    result.append("\n");
                }
                result.append(company);
            }

            result.append("\n").append(fullName);

            return result.toString();

        } catch (Exception e) {
            return "Владелец: данные не распознаны\nФИО не доступно";
        }
    }

    private static boolean isCompanyName(String text) {
        // Проверяем признаки названия компании
        return text.matches(".*(ООО|АО|ПАО|ЗАО|ИП|ОАО|НАО|LLC|LTD|INC|CORP|JSC).*") ||
                text.contains("\"") ||
                text.matches("[А-Я]{2,}.*");
    }

    public static SignatureInfo processSignatures(SignatureDistribution distribution) throws Exception {
        SignatureInfo info = new SignatureInfo();
        for (File file : distribution.bankSigFiles) info.bankSignerInfos.add(extractSignerInfo(file));
        for (File file : distribution.rightSigFiles) info.rightSignerInfos.add(extractSignerInfo(file));
        for (File file : distribution.additionalSigFiles) info.additionalSignerInfos.add(extractSignerInfo(file));
        return info;
    }

    public static void processDocument(File pdfFile, List<Integer> pageNumbers,
                                       SignatureInfo signatureInfo, String leftTitle,
                                       String rightTitle, String additionalTitle,
                                       ProxyInfo proxyInfo) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDType0Font font = PDType0Font.load(doc, PDFSigner.class.getResourceAsStream("/com/example/fonts/times.ttf"));
            PDType0Font boldFont = PDType0Font.load(doc, PDFSigner.class.getResourceAsStream("/com/example/fonts/timesbd.ttf"));

            SignatureInfo templateSignatureInfo = copySignatureInfo(signatureInfo);

            for (int pageNumber : pageNumbers) {
                int adjustedPageNumber = (pageNumber == -1) ? doc.getNumberOfPages() : pageNumber;
                while (adjustedPageNumber > doc.getNumberOfPages()) {
                    doc.addPage(new PDPage(PDRectangle.A4));
                }

                PDPage page = doc.getPage(adjustedPageNumber - 1);
                SignatureInfo currentSignatureInfo = copySignatureInfo(templateSignatureInfo);

                addStampsToPage(doc, page, currentSignatureInfo, leftTitle,
                        rightTitle, additionalTitle, font, boldFont, proxyInfo);
            }
            saveResult(doc, pdfFile);
        }
    }

    public static void createProtocol(File pdfFile, ProtocolData protocolData,
                                      PDFAreaSelector.SelectedArea selectedArea,
                                      File employeeSignatureFile,
                                      ProtocolSettings settings) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            // Если нужно добавить пустую страницу и она еще не добавлена
            if (settings.isAddBlankPage() && selectedArea.pageIndex >= doc.getNumberOfPages() - 1) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }

            PDType0Font regularFont = PDType0Font.load(doc,
                    PDFSigner.class.getResourceAsStream("/com/example/fonts/times.ttf"));
            PDType0Font boldFont = PDType0Font.load(doc,
                    PDFSigner.class.getResourceAsStream("/com/example/fonts/timesbd.ttf"));

            PDImageXObject signatureImage = null;
            float signatureHeight = 30;
            if (employeeSignatureFile != null) {
                try {
                    signatureImage = PDImageXObject.createFromFileByContent(employeeSignatureFile, doc);
                } catch (IOException e) {
                    System.err.println("Ошибка загрузки изображения подписи: " + e.getMessage());
                }
            }

            // Убедимся, что выбранная страница существует
            int targetPageIndex = Math.min(selectedArea.pageIndex, doc.getNumberOfPages() - 1);
            PDPage page = doc.getPage(targetPageIndex);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {

                float startX = (float) selectedArea.clickX;
                float startY = (float) selectedArea.clickY;

                // Увеличиваем шрифт на 1/3 (было 6.7f, теперь 8.9f)
                float fontSize = 8.9f; // PROTOCOL_FONT_SIZE * 4/3
                float lineHeight = 14; // Увеличиваем межстрочный интервал пропорционально
                float extraLineHeight = 18; // Дополнительный интервал для даты проверки
                PDColor blackColor = new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE);

                // 1. Тип договора и реквизиты
                String docInfo = String.format("%s №%s от %s",
                        protocolData.getDocType(),
                        protocolData.getDocNumber(),
                        protocolData.getDocDate());
                addProtocolTextWithWrap(cs, boldFont, fontSize, startX, startY, docInfo, 400);
                startY -= lineHeight * 1.5f;

                // 2. Подписи
                addProtocolText(cs, boldFont, fontSize, startX, startY, "Подписи:");
                startY -= lineHeight;

                // 3. Список подписантов с переносом строк
                for (String signer : protocolData.getSigners()) {
                    // Получаем фактическую высоту текста с учетом переносов
                    float textHeight = addProtocolTextWithWrap(cs, regularFont, fontSize, startX, startY, "• " + signer, 400);
                    startY -= textHeight;
                }

                // 4. Дата проверки - добавляем дополнительный интервал перед ней
                startY -= 5; // Дополнительный отступ перед датой проверки
                addProtocolText(cs, boldFont, fontSize, startX, startY,
                        "Дата проверки: " + protocolData.getVerificationDate());
                startY -= extraLineHeight; // Увеличиваем интервал после даты проверки

                // 5. Заключительная строка с подписью
                String verificationText = "Проверка действительности УЭК проведена: ";
                float textWidth = boldFont.getStringWidth(verificationText) / 1000 * fontSize;

                // Первая часть жирным
                cs.beginText();
                cs.setFont(boldFont, fontSize);
                cs.setNonStrokingColor(blackColor);
                cs.newLineAtOffset(startX, startY);
                cs.showText(verificationText);
                cs.endText();

                // ФИО обычным
                String employeeName = protocolData.getEmployeeName();
                float nameWidth = regularFont.getStringWidth(employeeName) / 1000 * fontSize;

                cs.beginText();
                cs.setFont(regularFont, fontSize);
                cs.setNonStrokingColor(blackColor);
                cs.newLineAtOffset(startX + textWidth, startY);
                cs.showText(employeeName);
                cs.endText();

                // Подпись
                if (signatureImage != null) {
                    float signatureWidth = signatureImage.getWidth() * signatureHeight / signatureImage.getHeight();
                    float signatureY = startY - 5;
                    cs.drawImage(signatureImage, startX + textWidth + nameWidth + 10,
                            signatureY, signatureWidth, signatureHeight);
                }
            }

            File output = new File(pdfFile.getParent(), "ПОДПИСАННЫЙ_" + pdfFile.getName());
            doc.save(output);
        }
    }

    private static float addProtocolTextWithWrap(PDPageContentStream cs, PDFont font, float fontSize,
                                                 float x, float y, String text, float maxWidth) throws IOException {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float currentY = y;
        int lineCount = 0;

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testWidth <= maxWidth) {
                currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
            } else {
                // Выводим текущую строку
                if (currentLine.length() > 0) {
                    addProtocolText(cs, font, fontSize, x, currentY, currentLine.toString());
                    currentY -= fontSize + 3; // Увеличиваем межстрочный интервал пропорционально
                    lineCount++;
                }

                // Если слово само по себе длиннее maxWidth, разбиваем его
                if (font.getStringWidth(word) / 1000 * fontSize > maxWidth) {
                    StringBuilder currentWord = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        String testChar = currentWord.toString() + c;
                        if (font.getStringWidth(testChar) / 1000 * fontSize > maxWidth) {
                            addProtocolText(cs, font, fontSize, x, currentY, currentWord.toString());
                            currentY -= fontSize + 3;
                            lineCount++;
                            currentWord = new StringBuilder(String.valueOf(c));
                        } else {
                            currentWord.append(c);
                        }
                    }
                    currentLine = currentWord;
                } else {
                    currentLine = new StringBuilder(word);
                }
            }
        }

        // Выводим последнюю строку
        if (currentLine.length() > 0) {
            addProtocolText(cs, font, fontSize, x, currentY, currentLine.toString());
            lineCount++;
        }

        // Возвращаем фактическую высоту текста (количество строк * высота строки)
        return lineCount * (fontSize + 3);
    }

    private static void addProtocolText(PDPageContentStream cs, PDFont font, float fontSize,
                                        float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static float calculateFontSize(PDFont font, String text, float maxWidth, float initialSize)
            throws IOException {
        float size = initialSize;
        float textWidth = font.getStringWidth(text) / 1000 * size;

        while (textWidth > maxWidth && size > 6) {
            size -= 0.5f;
            textWidth = font.getStringWidth(text) / 1000 * size;
        }

        return size;
    }

    private static void addTextWithAlignment(PDPageContentStream cs, PDFont font, float fontSize,
                                             float x, float y, String text, float maxWidth) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float startX = x;

        // Если текст шире доступной области - переносим по словам
        if (textWidth > maxWidth) {
            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;
                float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

                if (testWidth <= maxWidth) {
                    currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
                } else {
                    if (currentLine.length() > 0) {
                        // Выводим текущую строку
                        cs.beginText();
                        cs.setFont(font, fontSize);
                        cs.newLineAtOffset(startX, y);
                        cs.showText(currentLine.toString());
                        cs.endText();
                        y -= fontSize + 2;
                        currentLine = new StringBuilder(word);
                    } else {
                        // Слово слишком длинное - разбиваем принудительно
                        for (char c : word.toCharArray()) {
                            float charWidth = font.getStringWidth(String.valueOf(c)) / 1000 * fontSize;
                            if (font.getStringWidth(currentLine.toString() + c) / 1000 * fontSize > maxWidth) {
                                cs.beginText();
                                cs.setFont(font, fontSize);
                                cs.newLineAtOffset(startX, y);
                                cs.showText(currentLine.toString());
                                cs.endText();
                                y -= fontSize + 2;
                                currentLine = new StringBuilder();
                            }
                            currentLine.append(c);
                        }
                    }
                }
            }

            // Выводим последнюю строку
            if (currentLine.length() > 0) {
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.newLineAtOffset(startX, y);
                cs.showText(currentLine.toString());
                cs.endText();
            }
        } else {
            // Текст помещается в одну строку
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(startX, y);
            cs.showText(text);
            cs.endText();
        }
    }

    private static void addDocumentInfo(PDDocument doc, PDPage page, ProtocolData protocolData) throws IOException {
        List<PDFContentAnalyzer.TextBlock> existingText = PDFContentAnalyzer.analyzePage(doc, 1);
        float startY = findPositionForDocumentInfo(page, existingText, 100);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDType0Font font = loadFont(doc);

            addText(cs, font, BASE_FONT_SIZE + 2, 50, startY,
                    "Результат проверки пакета документов", existingText);

            String docInfo = String.format("%s №%s от %s",
                    protocolData.getDocType(),
                    protocolData.getDocNumber(),
                    protocolData.getDocDate());
            addText(cs, font, BASE_FONT_SIZE, 50, startY - LINE_SPACING, docInfo, existingText);

            float currentY = startY - 2 * LINE_SPACING;
            for (String signer : protocolData.getSigners()) {
                addText(cs, font, BASE_FONT_SIZE, 50, currentY, "Подпись: " + signer, existingText);
                currentY -= LINE_SPACING;
            }
        }
    }

    private static void addVerificationInfo(PDDocument doc, PDPage page, ProtocolData protocolData) throws IOException {
        List<PDFContentAnalyzer.TextBlock> existingText = PDFContentAnalyzer.analyzePage(doc, doc.getNumberOfPages());
        float startY = findPositionForVerificationInfo(page, existingText, 50);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDType0Font font = loadFont(doc);
            String verificationInfo = String.format(
                    "Проверка действительности УЭК проведена %s %s ",
                    protocolData.getVerificationDate(),
                    protocolData.getEmployeeName());

            addText(cs, font, LARGE_FONT_SIZE, 50, startY, verificationInfo, existingText);
        }
    }

    private static float findPositionForDocumentInfo(PDPage page, List<PDFContentAnalyzer.TextBlock> existingText, float requiredHeight) {
        PDRectangle mediaBox = page.getMediaBox();
        float startY = mediaBox.getHeight() - 50;

        for (PDFContentAnalyzer.TextBlock block : existingText) {
            if (block.y > startY - requiredHeight && block.y < startY) {
                startY = block.y - requiredHeight - MIN_TEXT_MARGIN;
            }
        }
        return startY;
    }

    private static float findPositionForVerificationInfo(PDPage page, List<PDFContentAnalyzer.TextBlock> existingText, float requiredHeight) {
        float startY = 50;
        for (PDFContentAnalyzer.TextBlock block : existingText) {
            if (block.y < startY + requiredHeight && block.y > startY) {
                startY = block.y + block.height + MIN_TEXT_MARGIN;
            }
        }
        return startY;
    }

    private static void addText(PDPageContentStream cs, PDFont font, float size,
                                float x, float y, String text, List<PDFContentAnalyzer.TextBlock> existingText) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        if (!isSpaceFree(x, y, textWidth, size, existingText)) {
            y = findFreeSpaceAbove(y, textWidth, size, existingText);
        }

        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static boolean isSpaceFree(float x, float y, float width, float height, List<PDFContentAnalyzer.TextBlock> existingText) {
        if (existingText == null || existingText.isEmpty()) {
            return true;
        }
        for (PDFContentAnalyzer.TextBlock block : existingText) {
            if (y < block.y + block.height &&
                    y + height > block.y &&
                    x < block.x + block.width &&
                    x + width > block.x) {
                return false;
            }
        }
        return true;
    }

    private static float findFreeSpaceAbove(float y, float width, float height, List<PDFContentAnalyzer.TextBlock> existingText) {
        float newY = y;
        for (PDFContentAnalyzer.TextBlock block : existingText) {
            if (newY < block.y + block.height && newY + height > block.y) {
                newY = block.y + block.height + MIN_TEXT_MARGIN;
            }
        }
        return newY;
    }

    private static PDType0Font loadFont(PDDocument doc) throws IOException {
        return PDType0Font.load(doc, PDFSigner.class.getResourceAsStream("/com/example/fonts/times.ttf"));
    }

    private static SignatureInfo copySignatureInfo(SignatureInfo original) {
        SignatureInfo copy = new SignatureInfo();
        copy.bankSignerInfos.addAll(new ArrayList<>(original.bankSignerInfos));
        copy.rightSignerInfos.addAll(new ArrayList<>(original.rightSignerInfos));
        copy.additionalSignerInfos.addAll(new ArrayList<>(original.additionalSignerInfos));
        return copy;
    }

    private static StampPosition addStampsToPage(PDDocument doc, PDPage page,
                                                 SignatureInfo signatureInfo, String leftTitle,
                                                 String rightTitle, String additionalTitle,
                                                 PDType0Font font, PDType0Font boldFont,
                                                 ProxyInfo proxyInfo) throws IOException {
        PDRectangle pageSize = page.getMediaBox();

        float marginHorizontal = 30;
        float marginVertical = 50;
        float columnSpacing = 20;
        float titleToStampSpacing = 15; // Отступ между заголовком и штампами
        float stampSpacing = 5; // Интервал между штампами

        // Создаем штампы с информацией о доверенности
        List<Stamp> leftStamps = createStamps(signatureInfo.bankSignerInfos, true, font, boldFont, proxyInfo);
        List<Stamp> rightStamps = createStamps(signatureInfo.rightSignerInfos, false, font, boldFont, proxyInfo);
        List<Stamp> additionalStamps = createStamps(signatureInfo.additionalSignerInfos, false, font, boldFont, proxyInfo);

        // Рассчитываем ширину колонок на основе фактического содержания
        float leftColumnWidth = calculateDynamicColumnWidth(leftStamps);
        float rightColumnWidth = calculateDynamicColumnWidth(rightStamps);
        float additionalColumnWidth = calculateDynamicColumnWidth(additionalStamps);

        // Выравниваем ширину всех трех колонок по максимальной
        float maxColumnWidth = Math.max(leftColumnWidth, Math.max(rightColumnWidth, additionalColumnWidth));
        leftColumnWidth = maxColumnWidth;
        rightColumnWidth = maxColumnWidth;
        additionalColumnWidth = maxColumnWidth;

        float maxAvailableWidth = pageSize.getWidth() - 2 * marginHorizontal;

        // Определяем, есть ли подписи в дополнительной категории
        boolean hasAdditionalSignatures = !additionalStamps.isEmpty();

        float leftX, rightX, additionalX;

        if (hasAdditionalSignatures) {
            // Есть подписи в категории Поручители и/или залогодатели - размещаем все три колонки
            float totalColumnsWidth = leftColumnWidth + columnSpacing + rightColumnWidth + columnSpacing + additionalColumnWidth;

            // Если не помещается, масштабируем
            if (totalColumnsWidth > maxAvailableWidth) {
                float scaleFactor = maxAvailableWidth / totalColumnsWidth;
                leftColumnWidth *= scaleFactor;
                rightColumnWidth *= scaleFactor;
                additionalColumnWidth *= scaleFactor;
            }

            // Выравниваем все три колонки рядом
            float groupWidth = leftColumnWidth + columnSpacing + rightColumnWidth + columnSpacing + additionalColumnWidth;
            float groupStartX = (pageSize.getWidth() - groupWidth) / 2;

            leftX = groupStartX;
            rightX = groupStartX + leftColumnWidth + columnSpacing;
            additionalX = rightX + rightColumnWidth + columnSpacing;
        } else {
            // Нет подписей в категории Поручители и/или залогодатели - размещаем только две колонки по краям
            float totalColumnsWidth = leftColumnWidth + columnSpacing + rightColumnWidth;

            // Если не помещается, масштабируем
            if (totalColumnsWidth > maxAvailableWidth) {
                float scaleFactor = maxAvailableWidth / totalColumnsWidth;
                leftColumnWidth *= scaleFactor;
                rightColumnWidth *= scaleFactor;
            }

            // Размещаем колонки по краям листа
            leftX = marginHorizontal;
            rightX = pageSize.getWidth() - marginHorizontal - rightColumnWidth;
            additionalX = 0; // Не используется
        }

        // Вычисляем высоту заголовков
        float titleHeight = getColumnTitleHeight();

        // Вычисляем высоту штампов для каждой колонки
        float leftStampsHeight = calculateDynamicColumnHeight(leftStamps, stampSpacing);
        float rightStampsHeight = calculateDynamicColumnHeight(rightStamps, stampSpacing);
        float additionalStampsHeight = calculateDynamicColumnHeight(additionalStamps, stampSpacing);

        // Находим максимальную высоту штампов
        float maxStampsHeight;
        if (hasAdditionalSignatures) {
            maxStampsHeight = Math.max(leftStampsHeight, Math.max(rightStampsHeight, additionalStampsHeight));
        } else {
            maxStampsHeight = Math.max(leftStampsHeight, rightStampsHeight);
        }

        // Общая высота блока (заголовки + отступ + штампы)
        float totalBlockHeight = titleHeight + titleToStampSpacing + maxStampsHeight;

        // Начинаем размещение ОТ САМОГО НИЗА листа
        float blockBottomY = marginVertical; // Отступ от нижнего края
        float blockTopY = blockBottomY + totalBlockHeight;

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            List<String> placedBank = new ArrayList<>();
            List<String> placedRight = new ArrayList<>();
            List<String> placedAdditional = new ArrayList<>();

            // 1. Левая колонка (Банк)
            if (!leftStamps.isEmpty()) {
                // Позиция заголовка (САМЫЙ ВЕРХ блока - над штампами)
                float titleY = blockTopY - titleHeight;

                // Рисуем заголовок
                drawColumnTitle(cs, boldFont, leftTitle, leftX, titleY, leftColumnWidth);

                // Позиция для штампов (ПОД заголовком)
                float stampsBottomY = blockBottomY;
                float stampsTopY = blockTopY - titleHeight - titleToStampSpacing;
                float currentY = stampsTopY;

                // Рисуем штампы сверху вниз
                for (Stamp stamp : leftStamps) {
                    float stampHeight = stamp.getHeight();
                    if (currentY - stampHeight >= stampsBottomY) {
                        drawStamp(cs, stamp, leftX, currentY - stampHeight, leftColumnWidth, proxyInfo);
                        placedBank.add(stamp.getText());
                        currentY -= stampHeight + stampSpacing;
                    }
                }
            }

            // 2. Центральная колонка (Заемщик)
            if (!rightStamps.isEmpty()) {
                // Позиция заголовка (САМЫЙ ВЕРХ блока - над штампами)
                float titleY = blockTopY - titleHeight;

                // Рисуем заголовок
                drawColumnTitle(cs, boldFont, rightTitle, rightX, titleY, rightColumnWidth);

                // Позиция для штампов (ПОД заголовком)
                float stampsBottomY = blockBottomY;
                float stampsTopY = blockTopY - titleHeight - titleToStampSpacing;
                float currentY = stampsTopY;

                // Рисуем штампы сверху вниз
                for (Stamp stamp : rightStamps) {
                    float stampHeight = stamp.getHeight();
                    if (currentY - stampHeight >= stampsBottomY) {
                        drawStamp(cs, stamp, rightX, currentY - stampHeight, rightColumnWidth, proxyInfo);
                        placedRight.add(stamp.getText());
                        currentY -= stampHeight + stampSpacing;
                    }
                }
            }

            // 3. Правая колонка (Поручители и/или залогодатели и/или иные лица)
            if (hasAdditionalSignatures && !additionalStamps.isEmpty()) {
                // Позиция заголовка (САМЫЙ ВЕРХ блока - над штампами)
                float titleY = blockTopY - titleHeight;

                // Рисуем заголовок
                drawColumnTitle(cs, boldFont, additionalTitle, additionalX, titleY, additionalColumnWidth);

                // Позиция для штампов (ПОД заголовком)
                float stampsBottomY = blockBottomY;
                float stampsTopY = blockTopY - titleHeight - titleToStampSpacing;
                float currentY = stampsTopY;

                // Рисуем штампы сверху вниз
                for (Stamp stamp : additionalStamps) {
                    float stampHeight = stamp.getHeight();
                    if (currentY - stampHeight >= stampsBottomY) {
                        drawStamp(cs, stamp, additionalX, currentY - stampHeight, additionalColumnWidth, proxyInfo);
                        placedAdditional.add(stamp.getText());
                        currentY -= stampHeight + stampSpacing;
                    }
                }
            }

            return new StampPosition(
                    signatureInfo.bankSignerInfos.stream()
                            .filter(info -> !placedBank.contains(info))
                            .collect(Collectors.toList()),
                    signatureInfo.rightSignerInfos.stream()
                            .filter(info -> !placedRight.contains(info))
                            .collect(Collectors.toList()),
                    signatureInfo.additionalSignerInfos.stream()
                            .filter(info -> !placedAdditional.contains(info))
                            .collect(Collectors.toList())
            );
        }
    }

    // Метод для расчета высоты заголовка колонки
    private static float getColumnTitleHeight() {
        return COLUMN_TITLE_FONT_SIZE + 8; // Высота текста + отступы
    }

    // Метод для отрисовки заголовков колонок
    private static void drawColumnTitle(PDPageContentStream cs, PDFont font,
                                        String title, float x, float y, float width) throws IOException {
        // Убираем символы переноса строки из заголовка
        String cleanTitle = title.replace("\n", " ");

        cs.beginText();
        cs.setFont(font, COLUMN_TITLE_FONT_SIZE);
        cs.setNonStrokingColor(java.awt.Color.BLACK);
        float titleWidth = font.getStringWidth(cleanTitle) / 1000 * COLUMN_TITLE_FONT_SIZE;
        float titleX = x + (width - titleWidth) / 2;
        // Позиция Y для заголовка (нижний край текста)
        float lineY = y - 4; // Небольшой отступ от нижнего края блока заголовка
        cs.newLineAtOffset(titleX, lineY);
        cs.showText(cleanTitle);
        cs.endText();
    }

    // Метод расчета высоты колонки штампов
    private static float calculateDynamicColumnHeight(List<Stamp> stamps, float stampSpacing) {
        if (stamps.isEmpty()) {
            return 0f;
        }

        float totalHeight = stamps.stream()
                .map(Stamp::getHeight)
                .reduce(0f, (a, b) -> a + b + stampSpacing) - stampSpacing;

        return totalHeight;
    }

    // Метод расчета ширины колонки
    private static float calculateDynamicColumnWidth(List<Stamp> stamps) {
        if (stamps.isEmpty()) {
            return 120f; // минимальная ширина по умолчанию
        }

        float maxWidth = stamps.stream()
                .map(Stamp::getWidth)
                .max(Float::compare)
                .orElse(120f);

        return Math.max(maxWidth, 120f);
    }


    // Вспомогательный метод для подсчета строк в заголовке
    private static int countLines(String title) {
        if (title == null || title.isEmpty()) {
            return 1;
        }
        // Убираем символы переноса строки и считаем как одну строку
        return 1;
    }

    // Метод для отрисовки заголовков колонок (исправленный - без обработки переносов)
    private static void drawColumnTitle(PDPageContentStream cs, PDFont font,
                                        String title, float x, float y, float width, float titleHeight) throws IOException {
        // Убираем символы переноса строки из заголовка
        String cleanTitle = title.replace("\n", " ");

        cs.beginText();
        cs.setFont(font, COLUMN_TITLE_FONT_SIZE);
        cs.setNonStrokingColor(java.awt.Color.BLACK);
        float titleWidth = font.getStringWidth(cleanTitle) / 1000 * COLUMN_TITLE_FONT_SIZE;
        float titleX = x + (width - titleWidth) / 2;
        // Позиция Y для заголовка (центрирование по высоте)
        float lineY = y + (titleHeight - COLUMN_TITLE_FONT_SIZE) / 2;
        cs.newLineAtOffset(titleX, lineY);
        cs.showText(cleanTitle);
        cs.endText();
    }

    // Метод для расчета высоты заголовка колонки
    private static float getColumnTitleHeight(String title) {
        String[] titleLines = title.split("\n");
        // Высота = (количество строк * высота строки) + межстрочный интервал
        return titleLines.length * COLUMN_TITLE_FONT_SIZE + (titleLines.length - 1) * 2;
    }

    private static List<Stamp> createStamps(List<String> signerInfos, boolean isBank,
                                            PDType0Font font, PDType0Font boldFont,
                                            ProxyInfo proxyInfo) throws IOException {
        List<Stamp> stamps = new ArrayList<>();
        for (String info : signerInfos) {
            // Проверяем, есть ли доверенность для этого подписанта
            boolean hasProxyInfo = false;
            if (proxyInfo != null) {
                String signerName = info.split("\n")[info.split("\n").length - 1].replace("Владелец: ", "").trim();
                if (compareSurnames(signerName, proxyInfo.getFullName())) {
                    hasProxyInfo = true;
                }
            }
            stamps.add(new Stamp(info, isBank, font, boldFont, hasProxyInfo));
        }
        return stamps;
    }

    private static float calculateDynamicColumnWidth(List<Stamp> stamps, float padding) {
        if (stamps.isEmpty()) {
            return 120f; // уменьшенная минимальная ширина по умолчанию
        }

        float maxWidth = stamps.stream()
                .map(Stamp::getWidth)
                .max(Float::compare)
                .orElse(120f);

        // Добавляем небольшой запас для красоты
        return Math.max(maxWidth + 2 * padding, 120f);
    }

    private static void drawStamp(PDPageContentStream cs, Stamp stamp,
                                  float x, float y, float width,
                                  ProxyInfo proxyInfo) throws IOException {
        // Используем реальные размеры штампа
        float stampWidth = Math.max(width, stamp.getWidth());
        float stampHeight = stamp.getHeight();

        // Рисуем рамку
        cs.setStrokingColor(new PDColor(new float[]{0, 0, 1}, PDDeviceRGB.INSTANCE));
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, stampWidth, stampHeight);
        cs.stroke();

        float horizontalPadding = 6;
        float topPadding = 6; // Увеличенный верхний отступ
        float currentY = y + stampHeight - topPadding;

        float mainFontSize = STAMP_MAIN_FONT_SIZE;
        float regularFontSize = STAMP_REGULAR_FONT_SIZE;
        float lineSpacing = 2; // Небольшой межстрочный интервал
        PDColor blueColor = new PDColor(new float[]{0, 0, 1}, PDDeviceRGB.INSTANCE);

        String[] lines = stamp.getLines();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            PDFont font = (i < 1) ? stamp.getBoldFont() : stamp.getRegularFont();
            float fontSize = (i == 0) ? mainFontSize : regularFontSize;

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(blueColor);

            if (i == 0) {
                // Первая строка - центрируем
                float textWidth = font.getStringWidth(line) / 1000 * fontSize;
                float textX = x + (stampWidth - textWidth) / 2;
                cs.newLineAtOffset(textX, currentY);
            } else {
                // Остальные строки - с обычным выравниванием
                cs.newLineAtOffset(x + horizontalPadding, currentY);
            }

            cs.showText(line);
            cs.endText();
            currentY -= fontSize + lineSpacing;
        }

        // Добавляем информацию о доверенности
        currentY -= 2;
        if (stamp.hasProxyInfo()) {
            String proxyLine1 = "Доверенность №" + proxyInfo.getNumber();
            cs.beginText();
            cs.setFont(stamp.getRegularFont(), regularFontSize);
            cs.setNonStrokingColor(blueColor);
            cs.newLineAtOffset(x + horizontalPadding, currentY);
            cs.showText(proxyLine1);
            cs.endText();
            currentY -= regularFontSize + lineSpacing;

            String proxyLine2 = "Срок действия с " + proxyInfo.getIssueDate() +
                    " по " + proxyInfo.getExpiryDate();
            cs.beginText();
            cs.setFont(stamp.getRegularFont(), regularFontSize);
            cs.setNonStrokingColor(blueColor);
            cs.newLineAtOffset(x + horizontalPadding, currentY);
            cs.showText(proxyLine2);
            cs.endText();
        } else {
            String noProxyText = "Доверенность не требуется";
            cs.beginText();
            cs.setFont(stamp.getRegularFont(), regularFontSize);
            cs.setNonStrokingColor(blueColor);
            cs.newLineAtOffset(x + horizontalPadding, currentY);
            cs.showText(noProxyText);
            cs.endText();
        }
    }

    private static boolean compareSurnames(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        String surname1 = name1.trim().split("\\s+")[0];
        String surname2 = name2.trim().split("\\s+")[0];
        return surname1.equalsIgnoreCase(surname2);
    }

    public static String extractSignerInfo(File sigFile) throws Exception {
        CMSSignedData signedData = new CMSSignedData(Files.newInputStream(sigFile.toPath()));
        SignerInformation signer = signedData.getSignerInfos().getSigners().iterator().next();
        X509CertificateHolder certHolder = getCertificateHolder(signedData, signer);
        X509Certificate cert = convertCertificate(certHolder);

        SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        Date signingTime = getSigningTime(signer);
        if (signingTime == null) {
            signingTime = new Date();
        }

        String ownerInfo = formatOwnerInfo(
                cert.getSubjectX500Principal().getName(),
                cert.getNotBefore(),
                cert.getNotAfter()
        );

        return String.format(
                "Документ подписан электронной подписью\n" +
                        "Дата подписания: %s\n" +
                        "Сведения о сертификате электронной подписи\n" +
                        "Серийный номер: %s\n" +
                        "Срок действия: с %s по %s\n" +
                        "%s",
                timeFormat.format(signingTime),
                formatSerialNumber(cert.getSerialNumber()),
                dateFormat.format(cert.getNotBefore()),
                dateFormat.format(cert.getNotAfter()),
                ownerInfo
        );
    }

    private static String formatOwnerInfo(String dn, Date notBefore, Date notAfter) {
        try {
            X500Name x500name = new X500Name(dn);

            String surname = getRDNAttribute(x500name, BCStyle.SURNAME);
            String givenName = getRDNAttribute(x500name, BCStyle.GIVENNAME);
            String initials = getRDNAttribute(x500name, BCStyle.INITIALS);
            String title = getRDNAttribute(x500name, BCStyle.T);
            String organization = getRDNAttribute(x500name, BCStyle.O);
            String commonName = getRDNAttribute(x500name, BCStyle.CN);

            String fullName = buildFullName(surname, givenName, initials);
            String company = extractCompanyInfo(organization, commonName);

            StringBuilder result = new StringBuilder("Владелец: ");

            if (!title.equals("Не указано")) {
                result.append(title);
            }

            if (!company.isEmpty()) {
                if (!title.equals("Не указано")) {
                    result.append("\n");
                }
                result.append(company);
            }

            result.append("\n").append(fullName);

            return result.toString();

        } catch (Exception e) {
            return "Владелец: данные не распознаны\nФИО не доступно";
        }
    }


    private static X509CertificateHolder getCertificateHolder(CMSSignedData signedData, SignerInformation signer) {
        return (X509CertificateHolder) signedData.getCertificates()
                .getMatches(signer.getSID())
                .iterator().next();
    }

    private static X509Certificate convertCertificate(X509CertificateHolder certHolder) throws CertificateException {
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private static Date getSigningTime(SignerInformation signer) {
        try {
            AttributeTable attributes = signer.getSignedAttributes();
            if (attributes != null) {
                Attribute signingTimeAttr = attributes.get(CMSAttributes.signingTime);
                if (signingTimeAttr != null) {
                    ASN1Set attrValues = signingTimeAttr.getAttrValues();
                    if (attrValues.size() > 0) {
                        return ((ASN1UTCTime) attrValues.getObjectAt(0)).getDate();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String formatSerialNumber(BigInteger serial) {
        byte[] bytes = serial.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02X", b));
        }
        return hexBuilder.toString();
    }

    private static String getRDNAttribute(X500Name x500name, ASN1ObjectIdentifier attribute) {
        RDN[] rdns = x500name.getRDNs(attribute);
        return (rdns != null && rdns.length > 0) ?
                rdns[0].getFirst().getValue().toString() : "Не указано";
    }

    private static String buildFullName(String surname, String givenName, String initials) {
        StringBuilder sb = new StringBuilder();

        if (!surname.equals("Не указано")) {
            sb.append(surname.trim());
        }

        if (!givenName.equals("Не указано")) {
            sb.append(" ").append(givenName.trim());
        } else if (!initials.equals("Не указано")) {
            sb.append(" ").append(initials.charAt(0)).append(".").append(initials.charAt(1)).append(".");
        }

        return sb.toString().trim();
    }

    public static SignatureDetails parseSignatureFromText(String ownerLine) {
        SignatureDetails details = new SignatureDetails();
        String[] lines = ownerLine.split("\n");

        // Ищем строку с "Владелец:"
        int ownerIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("Владелец:")) {
                ownerIndex = i;
                break;
            }
        }

        if (ownerIndex != -1) {
            // Обрабатываем строку с "Владелец:"
            String ownerData = lines[ownerIndex].replace("Владелец:", "").trim();

            // Проверяем следующие строки (если есть)
            if (ownerIndex + 1 < lines.length) {
                String nextLine = lines[ownerIndex + 1].trim();

                // Если следующая строка - это компания
                if (isCompanyName(nextLine)) {
                    details.setCompany(nextLine);
                    details.setPosition(ownerData);

                    // ФИО может быть на следующей строке
                    if (ownerIndex + 2 < lines.length) {
                        details.setFullName(lines[ownerIndex + 2].trim());
                    }
                } else {
                    // Иначе ownerData - это должность, следующая строка - ФИО
                    details.setPosition(ownerData);
                    details.setFullName(nextLine);
                }
            } else {
                // Только одна строка после "Владелец:"
                if (isCompanyName(ownerData)) {
                    details.setCompany(ownerData);
                } else {
                    details.setPosition(ownerData);
                }
            }
        }

        return details;
    }

    protected static SignatureDetails parseSignatureDetails(String dn) {
        SignatureDetails details = new SignatureDetails();

        try {
            X500Name x500name = new X500Name(dn);

            // Извлекаем основные данные
            String surname = getRDNAttribute(x500name, BCStyle.SURNAME);
            String givenName = getRDNAttribute(x500name, BCStyle.GIVENNAME);
            String initials = getRDNAttribute(x500name, BCStyle.INITIALS);

            details.setPosition(getRDNAttribute(x500name, BCStyle.T));
            details.setCompany(extractCompanyInfo(
                    getRDNAttribute(x500name, BCStyle.O),
                    getRDNAttribute(x500name, BCStyle.CN)
            ));
            details.setFullName(buildFullName(surname, givenName, initials));

        } catch (Exception e) {
            details.setFullName("Данные не распознаны");
        }

        return details;
    }

    private static String extractCompanyInfo(String organization, String commonName) {
        // 1. Проверяем явно указанную организацию
        if (!organization.equals("Не указано")) {
            return formatCompanyName(organization);
        }

        // 2. Пытаемся извлечь из CN
        String fromCN = extractCompanyFromCN(commonName);
        if (!fromCN.isEmpty()) {
            return fromCN;
        }

        return "";
    }

    private static String determineSignatureType(String ownerLine) {
        String[] lines = ownerLine.split("\n");

        // Если есть вторая строка и она не содержит ФИО - это организация
        if (lines.length > 1) {
            String possibleCompany = lines[1];
            if (!possibleCompany.matches("[А-Яа-яЁё\\s-]+")) { // Проверяем, что это не ФИО
                return "Подпись юридического лица";
            }
        }

        return "Подпись физического лица";
    }


    private static String formatPositionAndCompany(String title, String commonName) {
        StringBuilder result = new StringBuilder();

        // Добавляем должность, если есть
        if (!"Не указано".equals(title)) {
            result.append(capitalizeFirstLetter(title));
        }

        // Добавляем компанию, если есть
        String company = extractCompanyFromCN(commonName);
        if (!company.isEmpty()) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(formatCompanyName(company));
        }

        return result.toString();
    }


    private static String formatCompanyName(String company) {
        // Удаляем лишние пробелы
        company = company.trim();

        // Приводим юридическую форму к верхнему регистру
        String[] legalForms = {"ООО", "АО", "ПАО", "ЗАО", "ИП", "ОАО", "НАО"};
        for (String form : legalForms) {
            if (company.startsWith(form + " ") || company.startsWith(form + "\"")) {
                company = form.toUpperCase() + company.substring(form.length());
                break;
            }
        }

        return company;
    }

    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static String extractCompanyFromCN(String cn) {
        if (cn.equals("Не указано")) {
            return "";
        }

        // Ищем юридические формы
        Pattern pattern = Pattern.compile(
                "(?:O=|OU=)?(ООО|АО|ПАО|ЗАО|ИП|ОАО|НАО|LLC|JSC|LTD|INC|CORP)\\s*[\"]?([^\",]+)[\"]?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(cn);
        if (matcher.find()) {
            String legalForm = matcher.group(1).toUpperCase();
            String name = matcher.group(2).trim();

            // Очищаем название от лишних символов
            name = name.replaceAll("[\"<>]", "").trim();
            return legalForm + " " + name;
        }

        return "";
    }

    private static String normalizeName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    protected static String extractNameFromCN(String cn) {
        if (cn == null || "Не указано".equals(cn)) {
            return "ФИО не указано";
        }

        String[] parts = cn.trim().split("\\s+");
        List<String> nameParts = new ArrayList<>();

        for (String part : parts) {
            if (part.matches("[А-Яа-яЁё]{2,}")) {
                nameParts.add(part);
            }
        }

        if (nameParts.size() >= 3) {
            return String.join(" ", nameParts.get(0), nameParts.get(1), nameParts.get(2));
        } else if (nameParts.size() == 2) {
            return String.join(" ", nameParts.get(0), nameParts.get(1));
        } else if (nameParts.size() == 1) {
            return nameParts.get(0);
        }

        return "ФИО не указано";
    }

    private static void saveResult(PDDocument doc, File originalFile) throws IOException {
        File output = new File(originalFile.getParent(), "ВИЗУАЛИЗАЦИЯ_" + originalFile.getName());
        doc.save(output);
    }
}

