package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel import/export contract for item masters. The first worksheet uses the columns exported here.
 */
public final class ItemSpreadsheetService {
    private static final String[] HEADERS = {"Item Code", "Description", "Category", "Brand", "Material", "Size", "Unit", "HSN", "GST %", "Purchase Price", "Selling Price", "Opening Stock", "Minimum Stock", "Location", "Remarks"};
    private final ItemService itemService = new ItemService();

    public void exportItems(List<Item> items, Path file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Item Master");
            sheet.createFreezePane(0, 1);
            CellStyle header = headerStyle(workbook);
            CellStyle number = numberStyle(workbook, "#,##0.00");
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(header);
            }
            int rowIndex = 1;
            for (Item item : items) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, item.getItemCode());
                write(row, 1, item.getDescription());
                write(row, 2, item.getCategory());
                write(row, 3, item.getBrand());
                write(row, 4, item.getMaterial());
                write(row, 5, item.getSize());
                write(row, 6, item.getUnit());
                write(row, 7, item.getHsn());
                writeNumber(row, 8, item.getGst(), number);
                writeNumber(row, 9, item.getPurchasePrice(), number);
                writeNumber(row, 10, item.getSellingPrice(), number);
                writeNumber(row, 11, item.getOpeningStock(), number);
                writeNumber(row, 12, item.getMinimumStock(), number);
                write(row, 13, item.getLocation());
                write(row, 14, item.getRemarks());
            }
            for (int c = 0; c < HEADERS.length; c++) sheet.setColumnWidth(c, c == 1 || c == 14 ? 28 * 256 : 16 * 256);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, HEADERS.length - 1));
            workbook.write(output);
        }
    }

    public ImportResult importItems(Path file) throws IOException {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            if (sheet.getPhysicalNumberOfRows() == 0) return new ImportResult(0, List.of("The workbook is empty."));
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || blank(row, formatter)) continue;
                try {
                    Item item = readItem(row, formatter);
                    itemService.saveOrUpdate(item);
                    imported++;
                } catch (IllegalArgumentException ex) {
                    errors.add("Row " + (rowIndex + 1) + ": " + ex.getMessage());
                } catch (RuntimeException ex) {
                    errors.add("Row " + (rowIndex + 1) + ": could not be saved (" + ex.getMessage() + ")");
                }
            }
        }
        return new ImportResult(imported, errors);
    }

    private Item readItem(Row row, DataFormatter f) {
        String code = text(row, 0, f);
        String description = text(row, 1, f);
        if (code.isBlank()) throw new IllegalArgumentException("Item Code is required");
        if (description.isBlank()) throw new IllegalArgumentException("Description is required");
        Item item = new Item();
        item.setItemCode(code);
        item.setDescription(description);
        item.setCategory(text(row, 2, f));
        item.setBrand(text(row, 3, f));
        item.setMaterial(text(row, 4, f));
        item.setSize(text(row, 5, f));
        item.setUnit(text(row, 6, f));
        item.setHsn(text(row, 7, f));
        item.setGst(number(row, 8, f));
        item.setPurchasePrice(number(row, 9, f));
        item.setSellingPrice(number(row, 10, f));
        item.setOpeningStock(number(row, 11, f));
        item.setMinimumStock(number(row, 12, f));
        item.setLocation(text(row, 13, f));
        item.setRemarks(text(row, 14, f));
        return item;
    }

    private static String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static double number(Row row, int column, DataFormatter formatter) {
        String value = text(row, column, formatter);
        if (value.isBlank()) return 0;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(HEADERS[column] + " must be a number");
        }
    }

    private static boolean blank(Row row, DataFormatter formatter) {
        for (int c = 0; c < HEADERS.length; c++) if (!text(row, c, formatter).isBlank()) return false;
        return true;
    }

    private static void write(Row row, int col, String value) {
        row.createCell(col).setCellValue(value == null ? "" : value);
    }

    private static void writeNumber(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle numberStyle(Workbook workbook, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    public record ImportResult(int imported, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
