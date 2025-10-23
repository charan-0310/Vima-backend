package com.vimainsurance.vimaadmin.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageHelper;

public class TesseractTest {
    private static final Pattern PAN_PATTERN = Pattern.compile(
        "\\b[A-Z0O]{5}[0-9OI]{4}[A-Z0O]{1}\\b",
        Pattern.CASE_INSENSITIVE
    );    private static final Pattern AADHAAR_PATTERN = Pattern.compile(
        "(?<!\\d)(\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4})(?!\\d)"
    );
    // public static void main(String[] args) {
    //     ITesseract tesseract = new Tesseract();
    //     String outputPdf = "C:\\Users\\CharansundarMoorthy\\Downloads\\charan pan card masked.pdf";
    //     // Set tessdata folder path
    //     tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
    //     tesseract.setLanguage("eng");
    //     Pattern PAN_PATTERN = Pattern.compile("\\b([A-Z]{5}[0-9]{4}[A-Z])\\b");

    //     try {
    //         PDDocument document = PDDocument.load(new File("C:\\Users\\CharansundarMoorthy\\Downloads\\charan pan card.pdf"));
    //         PDFRenderer pdfRenderer = new PDFRenderer(document);
    //         for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
    //             BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);
    
    //             // Extract words with bounding boxes
    //             List<Word> words = tesseract.getWords(pageImage, ITessAPI.TessPageIteratorLevel.RIL_WORD);
    //             PDDocument newDoc = new PDDocument();
    //             PDPage newPage = new PDPage(PDRectangle.A4);
    //             newDoc.addPage(newPage);
    //             PDImageXObject pdImage = LosslessFactory.createFromImage(newDoc, pageImage);
    //             PDPage page = document.getPage(pageIndex);
    //             try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
    //                     PDPageContentStream.AppendMode.APPEND, true, true)) {

    //                 contentStream.setNonStrokingColor(Color.BLACK);
    
    //                 for (Word word : words) {
    //                     String text = word.getText().trim();
    //                     if (PAN_PATTERN.matcher(text).matches()) {
    //                         System.out.println("Found PAN: " + text);
    //                         String maskedText = maskPAN(text);
    //                         // Get word box coordinates (in pixels)
    //                         int x = word.getBoundingBox().x;
    //                         int y = word.getBoundingBox().y;
    //                         int width = word.getBoundingBox().width;
    //                         int height = word.getBoundingBox().height;
                    
    //                         // Convert Y-coordinate (PDF bottom-left origin)
    //                         float pdfY = pageImage.getHeight() - y - height;
                    
    //                         // Scale 300 DPI → 72 points
    //                         float scale = 72f / 300f;
    //                         float padding = 2f;
                    
    //                         // Draw white rectangle over original text
    //                         contentStream.setNonStrokingColor(Color.WHITE);
    //                         contentStream.addRect(
    //                             (x - padding) * scale,
    //                             (pdfY - padding) * scale,
    //                             (width + 2 * padding) * scale,
    //                             (height + 2 * padding) * scale
    //                         );
    //                         contentStream.fill();

    //                         // Draw masked text
    //                         contentStream.beginText();
    //                         contentStream.setNonStrokingColor(Color.BLACK);

    //                         // Use slightly larger font size for visibility
    //                         float fontSize = (height * scale) * 1.2f;
    //                         contentStream.setFont(PDType1Font.HELVETICA_BOLD, fontSize);

    //                         // Adjust baseline to ensure text is visible
    //                         float textY = (pdfY + (height * 0.25f)) * scale; // move up slightly
    //                         contentStream.newLineAtOffset(x * scale, textY);
    //                         contentStream.showText(maskedText);
    //                         contentStream.endText();
    //                     }
    //                 }
    //             }
    //         }
    
    //         document.save(outputPdf);
    //         document.close();
    //         System.out.println("✅ Masked PDF saved at: " + outputPdf);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
    
    public static void maskPANImage(String inputImg, String outputImg) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
        try {
            BufferedImage image = ImageIO.read(new File(inputImg));
            BufferedImage bright = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            float scaleFactor = 1.0f;  // 1.0 = no change, >1 = brighter
            float offset = 15f; 
            RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
            rescaleOp.filter(image, bright);
            bright = ImageHelper.getScaledInstance(bright, image.getWidth(), image.getHeight());
            List<Word> words = tesseract.getWords(bright, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            Graphics2D g = bright.createGraphics();
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(Color.BLACK);
            
        for (Word word : words) {
            String text = word.getText().replaceAll("[^A-Za-z0-9 ]", "").trim();
            if (text.isEmpty()) continue;

            if(PAN_PATTERN.matcher(text).matches()){
            Rectangle box = word.getBoundingBox();
            g.setColor(Color.WHITE);
            g.fillRect(box.x, box.y, box.width, box.height);

            g.setColor(Color.BLACK);
            String masked = maskPAN(text);
            g.drawString(masked, box.x, box.y + box.height - 3);

            System.out.println("Masked: " + text + " → " + masked);
            }
            }
        

        g.dispose();
        ImageIO.write(bright, "png", new File(outputImg));
        System.out.println("✅ Masked image saved: " + outputImg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void maskAadharImage(String inputImg, String outputImg) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        String foundAadhar = "";
        try {
            BufferedImage image = ImageIO.read(new File(inputImg));
            BufferedImage bright = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            float scaleFactor = 1.5f;  // 1.0 = no change, >1 = brighter
            float offset = 15f; 
            RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
            rescaleOp.filter(image, bright);
            bright = ImageHelper.getScaledInstance(bright, image.getWidth() * 3, image.getHeight() * 3);

            List<Word> words = tesseract.getWords(bright, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
            Graphics2D g = bright.createGraphics();
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(Color.BLACK);
            
        for (Word word : words) {
            String text = word.getText().replaceAll("[^A-Za-z0-9 ]", "").trim();
            if (text.isEmpty()) continue;
            Matcher m = AADHAAR_PATTERN.matcher(text);
            if(m.find()){
            foundAadhar = text;
            String masked = maskAADHAR(text);

            Rectangle box = word.getBoundingBox();
            int padding = 2; // reduce box padding (experiment with 1–4)
            int maskX = box.x + padding;
            int maskY = box.y + padding;
            int maskW = box.width - 2 * padding;
            int maskH = box.height - 2 * padding;
            
            g.setColor(Color.WHITE);
            g.fillRect(maskX, maskY, maskW, maskH);
            
            // draw masked Aadhaar text smaller, centered
            g.setColor(Color.BLACK);
            int fontSize = Math.max(10, (int)(maskH * 0.6)); // scale text smaller than box
            g.setFont(new Font("Helvetica", Font.BOLD, fontSize));
            
            // adjust Y-position for better centering
            FontMetrics fm = g.getFontMetrics();
            int textY = maskY + (maskH + fm.getAscent() - fm.getDescent()) / 2;
            
            g.drawString(masked, maskX + 3, textY);

            System.out.println("Masked: " + text + " → " + masked);
            }
            }
        

        g.dispose();
        ImageIO.write(bright, "png", new File(outputImg));
        System.out.println("✅ Masked image saved: " + outputImg);
        } catch (Exception e) {
            e.printStackTrace();
        }
     }

    public static void maskPAN(String inputPdf, String outputPdf) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
        PDDocument originalDoc = null;
        PDDocument newDoc = new PDDocument();

        try {
            originalDoc = PDDocument.load(new File(inputPdf));
            PDFRenderer pdfRenderer = new PDFRenderer(originalDoc);

            for (int pageIndex = 0; pageIndex < originalDoc.getNumberOfPages(); pageIndex++) {
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 400);
                BufferedImage brightened = new BufferedImage(
                pageImage.getWidth(), 
                pageImage.getHeight(), 
                pageImage.getType()
                );

                // scaleFactor >1.0 brightens the image, offset can adjust brightness
                 float scaleFactor = 1.0f;  // 1.0 = no change, >1 = brighter
                float offset = 15f;        // adds constant brightness

                RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
                rescaleOp.filter(pageImage, brightened);
                // for (int y = 0; y < pageImage.getHeight(); y++) {
                //     for (int x = 0; x < pageImage.getWidth(); x++) {
                //         int rgb = pageImage.getRGB(x, y);
                //         int pageImageValue = (rgb >> 16) & 0xff;
                //         if (pageImageValue > 128) pageImage.setRGB(x, y, 0xFFFFFFFF);
                //         else pageImage.setRGB(x, y, 0xFF000000);
                //     }
                // }
                Graphics g = brightened.getGraphics();
                g.drawImage(brightened, 0, 0, null);
                g.dispose();
                List<Word> words = tesseract.getWords(brightened, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                PDRectangle originalSize = originalDoc.getPage(pageIndex).getMediaBox();
                PDPage newPage = new PDPage(originalSize);
                newDoc.addPage(newPage);

                PDImageXObject pdImage = LosslessFactory.createFromImage(newDoc, brightened);

                try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Draw the full page image to maintain exact layout
                    cs.drawImage(pdImage, 0, 0, originalSize.getWidth(), originalSize.getHeight());

                    // Scale coordinates from image pixels to PDF points
                    float scaleX = originalSize.getWidth() / brightened.getWidth();
                    float scaleY = originalSize.getHeight() / brightened.getHeight();

                    for (Word word : words) {
                        System.out.println("Word: " + word.getText());
                        String text = word.getText().trim();
                        Matcher m = PAN_PATTERN.matcher(text);
                        if (m.find()) {
                            System.out.println("Found PAN: " + text);
                            String maskedText = maskPAN(text);

                            int x = word.getBoundingBox().x;
                            int y = word.getBoundingBox().y;
                            int width = word.getBoundingBox().width;
                            int height = word.getBoundingBox().height;

                            float pdfX = x * scaleX;
                            float pdfY = originalSize.getHeight() - ((y + height) * scaleY); // invert Y-axis
                            float pdfWidth = width * scaleX;
                            float pdfHeight = height * scaleY;

                            // Mask original text
                            cs.setNonStrokingColor(Color.WHITE);
                            cs.addRect(pdfX, pdfY, pdfWidth, pdfHeight);
                            cs.fill();

                            // Draw masked text
                            cs.beginText();
                            cs.setNonStrokingColor(Color.BLACK);
                            cs.setFont(PDType1Font.HELVETICA_BOLD, pdfHeight * 0.8f);
                            cs.newLineAtOffset(pdfX, pdfY + pdfHeight * 0.2f);
                            cs.showText(maskedText);
                            cs.endText();
                        }
                    }
                }
            }

            newDoc.save(outputPdf);
            System.out.println("✅ Masked + Flattened PDF saved at: " + outputPdf);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (originalDoc != null) originalDoc.close();
                if (newDoc != null) newDoc.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
   
    public static void main(String[] args) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);

        // maskAadharImage("C:\\Users\\CharansundarMoorthy\\Downloads\\ranjith_aadhar.png", "C:\\Users\\CharansundarMoorthy\\Downloads\\ranjith_aadhar_masked.png");
        // maskPANImage("C:\\Users\\CharansundarMoorthy\\Downloads\\ranjith_pan.png", "C:\\Users\\CharansundarMoorthy\\Downloads\\ranjith_pan_card_masked_image.png");
        // maskPAN("C:\\Users\\CharansundarMoorthy\\Downloads\\charan pan card.pdf", "C:\\Users\\CharansundarMoorthy\\Downloads\\charan_pan_card_masked_flattened.pdf");
        maskAADHAR("C:\\Users\\CharansundarMoorthy\\Downloads\\AADHAR.pdf", "C:\\Users\\CharansundarMoorthy\\Downloads\\charan_aadhar_card_masked_flattened.pdf");
        // String inputPdf = "C:\\Users\\CharansundarMoorthy\\Downloads\\aadhar card.pdf";
        // String outputPdf = "C:\\Users\\CharansundarMoorthy\\Downloads\\charan_aadhar_card_masked_flattened.pdf";

        // PDDocument originalDoc = null;
        // PDDocument newDoc = new PDDocument();

        // try {
        //     originalDoc = PDDocument.load(new File(inputPdf));
        //     PDFRenderer pdfRenderer = new PDFRenderer(originalDoc);

        //     for (int pageIndex = 0; pageIndex < originalDoc.getNumberOfPages(); pageIndex++) {

        //         BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 600);
        //         BufferedImage brightened = new BufferedImage(
        //         pageImage.getWidth(), 
        //         pageImage.getHeight(), 
        //         pageImage.getType()
        //         );

        //         // scaleFactor >1.0 brightens the image, offset can adjust brightness
        //          float scaleFactor = 1.5f;  // 1.0 = no change, >1 = brighter
        //         float offset = 15f;        // adds constant brightness

        //         RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
        //         rescaleOp.filter(pageImage, brightened);
        //         // for (int y = 0; y < pageImage.getHeight(); y++) {
        //         //     for (int x = 0; x < pageImage.getWidth(); x++) {
        //         //         int rgb = pageImage.getRGB(x, y);
        //         //         int pageImageValue = (rgb >> 16) & 0xff;
        //         //         if (pageImageValue > 128) pageImage.setRGB(x, y, 0xFFFFFFFF);
        //         //         else pageImage.setRGB(x, y, 0xFF000000);
        //         //     }
        //         // }
        //         Graphics g = brightened.getGraphics();
        //         g.drawImage(brightened, 0, 0, null);
        //         g.dispose();
        //         List<Word> words = tesseract.getWords(brightened, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

        //         PDRectangle originalSize = originalDoc.getPage(pageIndex).getMediaBox();
        //         PDPage newPage = new PDPage(originalSize);
        //         newDoc.addPage(newPage);

        //         PDImageXObject pdImage = LosslessFactory.createFromImage(newDoc, brightened);

        //         try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage,
        //                 PDPageContentStream.AppendMode.APPEND, true, true)) {

        //             // Draw the full page image to maintain exact layout
        //             cs.drawImage(pdImage, 0, 0, originalSize.getWidth(), originalSize.getHeight());

        //             // Scale coordinates from image pixels to PDF points
        //             float scaleX = originalSize.getWidth() / brightened.getWidth();
        //             float scaleY = originalSize.getHeight() / brightened.getHeight();

        //             for (Word word : words) {
        //                 String text = word.getText().trim();
        //                 System.out.println("Text: " + text);
        //                 Matcher m = AADHAAR_PATTERN.matcher(text);
        //                 if (m.find()) {
        //                     System.out.println("Found AADHAAR: " + text);
        //                     String maskedText = maskAADHAR(text.replaceAll("\\D", ""));

        //                     int x = word.getBoundingBox().x;
        //                     int y = word.getBoundingBox().y;
        //                     int width = word.getBoundingBox().width;
        //                     int height = word.getBoundingBox().height;

        //                     float pdfX = x * scaleX;
        //                     float pdfY = originalSize.getHeight() - ((y + height) * scaleY); // invert Y-axis
        //                     float pdfWidth = width * scaleX;
        //                     float pdfHeight = height * scaleY;

        //                     // Mask original text
        //                     cs.setNonStrokingColor(Color.RED);
        //                     cs.addRect(pdfX, pdfY, pdfWidth, pdfHeight);
        //                     cs.fill();

        //                     // Draw masked text
        //                     cs.beginText();
        //                     cs.setNonStrokingColor(Color.BLACK);

        //                     // Reduce font size: smaller than rectangle height
        //                     float fontSize = Math.min(pdfHeight * 0.6f, pdfWidth / maskedText.length() * 1.5f);
        //                     cs.setFont(PDType1Font.HELVETICA_BOLD, fontSize);

        //                     // Center the masked text within rectangle
        //                     float textX = pdfX + (pdfWidth - (maskedText.length() * fontSize * 0.5f)) / 2;
        //                     float textY = pdfY + (pdfHeight - fontSize) / 2;
        //                     cs.newLineAtOffset(textX, textY);

        //                     cs.showText(maskedText);
        //                     cs.endText();
        //                 }
        //             }
        //         }
        //     }

        //     newDoc.save(outputPdf);
        //     System.out.println("✅ Masked + Flattened PDF saved at: " + outputPdf);

        // } catch (Exception e) {
        //     e.printStackTrace();
        // } finally {
        //     try {
        //         if (originalDoc != null) originalDoc.close();
        //         if (newDoc != null) newDoc.close();
        //     } catch (Exception ex) {
        //         ex.printStackTrace();
        //     }
        // }
    }

    public static void maskAADHAR(String inputPdf, String outputPdf) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);

        PDDocument originalDoc = null;
        PDDocument newDoc = new PDDocument();

        try {
            originalDoc = PDDocument.load(new File(inputPdf));
            PDFRenderer pdfRenderer = new PDFRenderer(originalDoc);

            for (int pageIndex = 0; pageIndex < originalDoc.getNumberOfPages(); pageIndex++) {

                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 600);
                BufferedImage brightened = new BufferedImage(
                pageImage.getWidth(), 
                pageImage.getHeight(), 
                pageImage.getType()
                );

                // scaleFactor >1.0 brightens the image, offset can adjust brightness
                 float scaleFactor = 1.5f;  // 1.0 = no change, >1 = brighter
                float offset = 15f;        // adds constant brightness

                RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
                rescaleOp.filter(pageImage, brightened);
                // for (int y = 0; y < pageImage.getHeight(); y++) {
                //     for (int x = 0; x < pageImage.getWidth(); x++) {
                //         int rgb = pageImage.getRGB(x, y);
                //         int pageImageValue = (rgb >> 16) & 0xff;
                //         if (pageImageValue > 128) pageImage.setRGB(x, y, 0xFFFFFFFF);
                //         else pageImage.setRGB(x, y, 0xFF000000);
                //     }
                // }
                Graphics g = brightened.getGraphics();
                g.drawImage(brightened, 0, 0, null);
                g.dispose();
                List<Word> words = tesseract.getWords(brightened, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

                PDRectangle originalSize = originalDoc.getPage(pageIndex).getMediaBox();
                PDPage newPage = new PDPage(originalSize);
                newDoc.addPage(newPage);

                PDImageXObject pdImage = LosslessFactory.createFromImage(newDoc, brightened);

                try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Draw the full page image to maintain exact layout
                    cs.drawImage(pdImage, 0, 0, originalSize.getWidth(), originalSize.getHeight());

                    // Scale coordinates from image pixels to PDF points
                    float scaleX = originalSize.getWidth() / brightened.getWidth();
                    float scaleY = originalSize.getHeight() / brightened.getHeight();

                    for (Word word : words) {
                        String text = word.getText().trim();
                        System.out.println("Text: " + text);
                        Matcher m = AADHAAR_PATTERN.matcher(text);
                        if (m.find()) {
                            System.out.println("Found AADHAAR: " + text);
                            String maskedText = maskAADHAR(text.replaceAll("\\D", ""));

                            int x = word.getBoundingBox().x;
                            int y = word.getBoundingBox().y;
                            int width = word.getBoundingBox().width;
                            int height = word.getBoundingBox().height;

                            float pdfX = x * scaleX;
                            float pdfY = originalSize.getHeight() - ((y + height) * scaleY); // invert Y-axis
                            float pdfWidth = width * scaleX;
                            float pdfHeight = height * scaleY;

                            // Mask original text
                            cs.setNonStrokingColor(Color.WHITE);
                            cs.addRect(pdfX, pdfY, pdfWidth, pdfHeight);
                            cs.fill();

                            // Draw masked text
                            cs.beginText();
                            cs.setNonStrokingColor(Color.BLACK);

                            // Reduce font size: smaller than rectangle height
                            float fontSize = Math.min(pdfHeight * 0.6f, pdfWidth / maskedText.length() * 1.5f);
                            cs.setFont(PDType1Font.HELVETICA_BOLD, fontSize);

                            // Center the masked text within rectangle
                            float textX = pdfX + (pdfWidth - (maskedText.length() * fontSize * 0.5f)) / 2;
                            float textY = pdfY + (pdfHeight - fontSize) / 2;
                            cs.newLineAtOffset(textX, textY);

                            cs.showText(maskedText);
                            cs.endText();
                        }
                    }
                }
            }

            newDoc.save(outputPdf);
            System.out.println("✅ Masked + Flattened PDF saved at: " + outputPdf);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (originalDoc != null) originalDoc.close();
                if (newDoc != null) newDoc.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String maskPAN(String pan) {
        if (pan == null || pan.length() < 5) return "XXXXX";
        String first3 = pan.substring(0, 3);
        String last2 = pan.substring(pan.length() - 2);
        StringBuilder masked = new StringBuilder(first3);
        for (int i = 0; i < pan.length() - 5; i++) masked.append("X");
        masked.append(last2);
        return masked.toString();
    }

    private static String maskAADHAR(String aadhar) {
        if (aadhar == null) return "XXXX XXXX XXXXX";
    
        // Remove spaces or non-digit characters
        String digitsOnly = aadhar.replaceAll("\\D", "");
        if (digitsOnly.length() != 12) return "XXXX XXXX XXXXX";
    
        String last4 = digitsOnly.substring(8);
        return "XXXX XXXX " + last4;
    }
}
