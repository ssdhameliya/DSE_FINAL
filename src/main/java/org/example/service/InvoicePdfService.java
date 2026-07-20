package org.example.service;

import org.example.model.Purchase;
import org.example.model.Sales;
import org.example.util.PdfTemplateUtil;
import org.example.util.SalesPdfTemplateUtil;


import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service class for generating Purchase PDFs using PdfTemplateUtil.
 */
public class InvoicePdfService {

    /**
     * Generate a professional Purchase Invoice PDF.
     *
     * @param invoice Purchase object containing invoice details
     * @return Path to the generated PDF file
     * @throws Exception if PDF generation fails
     */
    public static Path purchase(Purchase invoice) throws Exception {
        // Path to logo in resources
        String logoPath = "D:\\JavaProject\\DSE_Final\\src\\main\\resources\\logo.png";

        // Ensure output directory exists
        Path outputDir = Path.of("D:\\JavaProject\\DSE_Final\\output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Output file path
        String outputPath = outputDir.resolve("purchase_" + invoice.getInvoiceNo() + ".pdf").toString();

        // Generate PDF using improved template
        PdfTemplateUtil.createDocument(outputPath, logoPath, invoice.getInvoiceNo(), "Purchase");

        return Path.of(outputPath);

    }

    public static Path sales(Sales invoice) throws Exception {
        // Path to logo in resources
        String logoPath = "D:\\JavaProject\\DSE_Final\\src\\main\\resources\\logo.png";

        // Ensure output directory exists
        Path outputDir = Path.of("D:\\JavaProject\\DSE_Final\\output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Output file path
        String outputPath = outputDir.resolve("Sales_" + invoice.getInvoiceNo() + ".pdf").toString();

        // Generate PDF using improved template
        SalesPdfTemplateUtil.createSalesPdf(outputPath, logoPath, invoice.getInvoiceNo(), "Sales");

        return Path.of(outputPath);
    }
}
