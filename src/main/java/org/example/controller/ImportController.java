package org.example.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dao.ItemDAO;
import org.example.model.Item;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;

public class ImportController {

    // FXML controls (must match Import.fxml)
    @FXML private ComboBox<String> cmbImportModule;
    @FXML private TextField txtImportNote;
    @FXML private Button btnChooseFile;
    @FXML private Label lblChosenFile;
    @FXML private CheckBox chkDryRun;
    @FXML private Button btnRunImport;
    @FXML private GridPane gridMapping;
    @FXML private TableView<Map<String,String>> tblImportPreview;
    @FXML private ProgressIndicator piImport;

    // Internal state
    private File chosenImportFile;
    private List<String> detectedHeaders = Collections.emptyList();

    private final List<String> domainFields = List.of(
        "item_code","description","category","brand","material","size","unit","hsn","gst",
        "purchase_price","selling_price","opening_stock","minimum_stock","location","remarks"
    );

    private final Map<String,String> currentMapping = new LinkedHashMap<>();
    private final Map<String, ComboBox<String>> mappingControls = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingRefresh;

    private final ItemDAO itemDAO = new ItemDAO();
    private final Preferences prefs = Preferences.userNodeForPackage(ImportController.class);

    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.###");

    @FXML
    public void initialize() {
        if (cmbImportModule != null) {
            cmbImportModule.getItems().setAll("Item Master","Customer","Supplier","Purchase","Sales");
            cmbImportModule.getSelectionModel().selectFirst();
        }

        if (btnRunImport != null) btnRunImport.setDisable(true);

        if (tblImportPreview != null) {
            tblImportPreview.getItems().clear();
            tblImportPreview.getColumns().clear();
            tblImportPreview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        if (piImport != null) piImport.setVisible(false);
    }

    // ---------------- UI actions ----------------

    @FXML
    private void onChooseImportFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select import file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx", "*.xls"));

        Stage owner = null;
        if (gridMapping != null && gridMapping.getScene() != null) owner = (Stage) gridMapping.getScene().getWindow();
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        chosenImportFile = file;
        if (lblChosenFile != null) lblChosenFile.setText(file.getName());
        if (btnRunImport != null) btnRunImport.setDisable(true);

        try {
            detectedHeaders = detectHeaders(file.toPath());
            Map<String,String> auto = autoMapHeaders(detectedHeaders);
            currentMapping.clear();
            currentMapping.putAll(auto);
            renderMappingGrid(detectedHeaders, currentMapping);
            List<Map<String,String>> previewRows = parsePreviewRows(file.toPath(), currentMapping, 50);
            Platform.runLater(() -> {
                showPreview(previewRows);
                if (btnRunImport != null) btnRunImport.setDisable(false);
            });
        } catch (Exception ex) {
            showError("Could not read file: " + ex.getMessage());
            chosenImportFile = null;
            if (lblChosenFile != null) lblChosenFile.setText("No file chosen");
        }
    }



    @FXML
    private void onRunImport() {
        if (chosenImportFile == null) { showWarning("Choose a file first"); return; }

        Map<String,String> mapping = new LinkedHashMap<>();
        for (String field : domainFields) {
            ComboBox<String> cb = mappingControls.get(field);
            if (cb != null && cb.getValue() != null && !cb.getValue().isBlank()) mapping.put(field, cb.getValue());
            else if (currentMapping.containsKey(field)) mapping.put(field, currentMapping.get(field));
        }

        boolean dryRun = chkDryRun != null && chkDryRun.isSelected();
        Path path = chosenImportFile.toPath();

        if (btnRunImport != null) btnRunImport.setDisable(true);
        if (btnChooseFile != null) btnChooseFile.setDisable(true);
        if (piImport != null) { piImport.setProgress(0); piImport.setVisible(true); }

        Task<ImportSummary> task = new Task<>() {
            @Override
            protected ImportSummary call() throws Exception {
                List<String> errors = new ArrayList<>();
                int processed = 0;
                int imported = 0;
                List<Item> batch = new ArrayList<>();

                try (InputStream in = java.nio.file.Files.newInputStream(path);
                     Workbook wb = new XSSFWorkbook(in)) {

                    Sheet sheet = wb.getSheetAt(0);
                    if (sheet == null) throw new IllegalStateException("No sheet found in workbook");

                    Map<String,Integer> headerIndex = buildHeaderIndex(sheet);
                    int lastRow = sheet.getLastRowNum();
                    for (int r = 1; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;
                        processed++;

                        Map<String,String> rowMap = buildDomainMapFromRow(row, headerIndex, mapping);
                        List<String> rowErrors = validateRowForImport(rowMap);
                        if (!rowErrors.isEmpty()) {
                            errors.add("Row " + (r+1) + ": " + String.join("; ", rowErrors));
                            updateProgress(r, lastRow);
                            continue;
                        }

                        Item item = mapToItem(rowMap);
                        batch.add(item);

                        updateProgress(r, lastRow);
                    }
                }

                if (!dryRun && !batch.isEmpty()) {
                    itemDAO.saveOrUpdateBatch(batch);
                    imported = batch.size();
                }

                return new ImportSummary(processed, imported, errors);
            }
        };

        task.progressProperty().addListener((obs, oldV, newV) -> {
            if (piImport != null) piImport.setProgress(newV.doubleValue());
        });

        task.setOnSucceeded(e -> {
            // existing success UI...
            ImportSummary res = task.getValue();
            StringBuilder msg = new StringBuilder();
            msg.append("Processed: ").append(res.processed).append("\n");
            msg.append("Imported: ").append(res.imported).append("\n");
            if (!res.errors.isEmpty()) {
                msg.append("\nErrors (first 50):\n");
                res.errors.stream().limit(50).forEach(er -> msg.append(er).append("\n"));
            }
            Alert alert = new Alert(res.errors.isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, msg.toString());
            alert.setHeaderText("Import finished");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();

            // optionally refresh master list (if you have reference)
            // itemMasterController.refreshItems();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String detail = ex == null ? "unknown error" : (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            if (detail == null) detail = "unknown error";

            // show a dialog with a "Details" expandable area
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import failed");
            alert.setHeaderText("Import failed: " + detail);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (ex != null) ex.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setExpandableContent(textArea);
            alert.getDialogPane().setExpanded(true);
            alert.showAndWait();

            if (btnRunImport != null) btnRunImport.setDisable(false);
            if (btnChooseFile != null) btnChooseFile.setDisable(false);
            if (piImport != null) piImport.setVisible(false);
        });


        new Thread(task, "import-task").start();
    }

    // ---------------- rendering / preview ----------------

    private void renderMappingGrid(List<String> headers, Map<String,String> mapping) {
        Platform.runLater(() -> {
            if (gridMapping == null) return;
            gridMapping.getChildren().clear();
            mappingControls.clear();

            Label l1 = new Label("Field");
            Label l2 = new Label("Excel Column");
            gridMapping.add(l1, 0, 0);
            gridMapping.add(l2, 1, 0);

            for (int i = 0; i < domainFields.size(); i++) {
                String field = domainFields.get(i);
                Label lbl = new Label(field);
                ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(headers));
                cb.setPrefWidth(300);
                String selected = mapping.get(field);
                if (selected != null) cb.getSelectionModel().select(selected);
                cb.valueProperty().addListener((obs, oldV, newV) -> schedulePreviewRefresh());
                gridMapping.add(lbl, 0, i + 1);
                gridMapping.add(cb, 1, i + 1);
                mappingControls.put(field, cb);
            }
        });
    }

    private void schedulePreviewRefresh() {
        if (pendingRefresh != null) pendingRefresh.cancel(false);
        pendingRefresh = scheduler.schedule(() -> Platform.runLater(this::refreshPreviewFromUI), 300, TimeUnit.MILLISECONDS);
    }

    private void refreshPreviewFromUI() {
        if (chosenImportFile == null) return;
        Map<String,String> mapping = new LinkedHashMap<>();
        for (String field : domainFields) {
            ComboBox<String> cb = mappingControls.get(field);
            if (cb != null && cb.getValue() != null && !cb.getValue().isBlank()) mapping.put(field, cb.getValue());
            else if (currentMapping.containsKey(field)) mapping.put(field, currentMapping.get(field));
        }
        try {
            List<Map<String,String>> previewRows = parsePreviewRows(chosenImportFile.toPath(), mapping, 50);
            showPreview(previewRows);
        } catch (Exception ignored) {}
    }

    private void showPreview(List<Map<String,String>> rows) {
        if (tblImportPreview == null) return;

        tblImportPreview.getItems().clear();
        tblImportPreview.getColumns().clear();

        if (rows == null || rows.isEmpty()) return;

        for (String field : domainFields) {
            TableColumn<Map<String,String>, String> col = new TableColumn<>(niceHeader(field));
            col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrDefault(field, "")));

            if (List.of("gst","purchase_price","selling_price","opening_stock","minimum_stock").contains(field)) {
                col.setCellFactory(tc -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.isBlank()) {
                            setText("");
                        } else {
                            try {
                                double d = Double.parseDouble(item.replaceAll("[,₹ ]",""));
                                setText(numberFormat.format(d));
                            } catch (Exception ex) {
                                setText(item);
                            }
                        }
                        setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                        setTooltip((item == null || item.isBlank()) ? null : new Tooltip(getText()));
                    }
                });
            } else {
                col.setCellFactory(tc -> new TableCell<>() {
                    private final Label lbl = new Label();
                    {
                        lbl.setWrapText(true);
                        lbl.setMaxWidth(220);
                    }
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            lbl.setText(item);
                            setGraphic(lbl);
                            setTooltip(new Tooltip(item));
                        }
                    }
                });
            }

            col.setPrefWidth(140);
            tblImportPreview.getColumns().add(col);
        }

        TableColumn<Map<String,String>, String> valCol = new TableColumn<>("Validation");
        valCol.setCellValueFactory(cell -> new SimpleStringProperty(String.join("; ", validateRowForImport(cell.getValue()))));
        valCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText("");
                } else {
                    Label l = new Label(item.length() > 60 ? item.substring(0, 60) + "…" : item);
                    l.setTooltip(new Tooltip(item));
                    l.setStyle("-fx-text-fill:#8b0000; -fx-font-weight:600;");
                    setGraphic(l);
                }
            }
        });
        valCol.setPrefWidth(220);
        tblImportPreview.getColumns().add(valCol);

        tblImportPreview.setItems(FXCollections.observableArrayList(rows));
        tblImportPreview.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Map<String,String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    String v = String.join("", validateRowForImport(item));
                    if (!v.isBlank()) setStyle("-fx-background-color: rgba(255,200,200,0.6);");
                    else if (getIndex() % 2 == 0) setStyle("-fx-background-color: rgba(245,245,245,0.6);");
                    else setStyle("");
                }
            }
        });
    }

    private String niceHeader(String field) {
        String[] parts = field.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    // ---------------- parsing helpers ----------------

    private List<Map<String,String>> parsePreviewRows(Path file, Map<String,String> mapping, int maxRows) throws Exception {
        List<Map<String,String>> preview = new ArrayList<>();
        try (InputStream in = java.nio.file.Files.newInputStream(file);
             Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String,Integer> headerIndex = buildHeaderIndex(sheet);
            for (int r = 1; r <= sheet.getLastRowNum() && preview.size() < maxRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String,String> parsed = buildDomainMapFromRow(row, headerIndex, mapping);
                preview.add(parsed);
            }
        }
        return preview;
    }

    private List<String> detectHeaders(Path file) throws Exception {
        try (InputStream in = java.nio.file.Files.newInputStream(file);
             Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return Collections.emptyList();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                headers.add(cellToString(cell).trim());
            }
            return headers;
        }
    }

    private Map<String, Integer> buildHeaderIndex(Sheet sheet) {
        Map<String, Integer> headerIndex = new HashMap<>();
        Row header = sheet.getRow(0);
        if (header == null) return headerIndex;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String name = cellToString(cell).trim();
            headerIndex.put(name, c);
        }
        return headerIndex;
    }

    private Map<String, String> buildDomainMapFromRow(Row row, Map<String, Integer> headerIndex, Map<String, String> mapping) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String field : domainFields) {
            String headerName = mapping.get(field);
            String value = "";
            if (headerName != null) {
                Integer idx = headerIndex.get(headerName);
                if (idx != null) {
                    Cell c = row.getCell(idx);
                    value = cellToString(c);
                }
            }
            out.put(field, value == null ? "" : value);
        }
        return out;
    }

    private String cellToString(Cell c) {
        if (c == null) return "";
        try {
            switch (c.getCellType()) {
                case STRING: return c.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(c)) {
                        return c.getLocalDateTimeCellValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else {
                        double v = c.getNumericCellValue();
                        if (v == Math.floor(v)) return String.valueOf((long)v);
                        return String.valueOf(v);
                    }
                case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
                case FORMULA:
                    switch (c.getCachedFormulaResultType()) {
                        case STRING: return c.getStringCellValue().trim();
                        case NUMERIC:
                            double v = c.getNumericCellValue();
                            if (v == Math.floor(v)) return String.valueOf((long)v);
                            return String.valueOf(v);
                        case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
                        default: return "";
                    }
                case BLANK: return "";
                default: return c.toString().trim();
            }
        } catch (Exception ex) {
            return c.toString().trim();
        }
    }

    private Map<String, String> autoMapHeaders(List<String> detectedHeaders) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String field : domainFields) {
            String normalized = field.replaceAll("[_\\- ]", "").toLowerCase();
            Optional<String> match = detectedHeaders.stream()
                .filter(Objects::nonNull)
                .filter(h -> h.toLowerCase().replaceAll("[_\\- ]", "").contains(normalized))
                .findFirst();
            match.ifPresent(h -> map.put(field, h));
        }
        return map;
    }

    private List<String> validateRowForImport(Map<String,String> row) {
        List<String> errs = new ArrayList<>();
        String desc = row.get("description");
        String code = row.get("item_code");
        if ((desc == null || desc.isBlank()) && (code == null || code.isBlank())) errs.add("missing description or item_code");

        String gst = row.get("gst");
        if (gst != null && !gst.isBlank()) {
            try { Double.parseDouble(gst.replaceAll("[, ]","")); }
            catch (Exception e) { errs.add("invalid gst"); }
        }

        String sp = row.get("selling_price"), pp = row.get("purchase_price");
        if (sp != null && !sp.isBlank() && pp != null && !pp.isBlank()) {
            try {
                double s = Double.parseDouble(sp.replaceAll("[,₹ ]",""));
                double p = Double.parseDouble(pp.replaceAll("[,₹ ]",""));
                if (s < p) errs.add("selling_price < purchase_price");
            } catch (Exception ignored) {}
        }
        return errs;
    }

    private Item mapToItem(Map<String,String> row) {
        Item item = new Item();

        String code = row.get("item_code");
        if (code == null || code.isBlank()) {
            item.setItemCode(generateItemCode());
        } else {
            item.setItemCode(code + "-" + UUID.randomUUID().toString().substring(0,4).toUpperCase());
        }

        item.setDescription(nullIfEmpty(row.get("description")));
        item.setCategory(nullIfEmpty(row.get("category")));
        item.setBrand(nullIfEmpty(row.get("brand")));
        item.setMaterial(nullIfEmpty(row.get("material")));
        item.setSize(nullIfEmpty(row.get("size")));
        item.setUnit(nullIfEmpty(row.get("unit")));
        item.setHsn(nullIfEmpty(row.get("hsn")));
        item.setGst(parseDoubleOrZero(row.get("gst")));
        item.setPurchasePrice(parseDoubleOrZero(row.get("purchase_price")));
        item.setSellingPrice(parseDoubleOrZero(row.get("selling_price")));
        item.setOpeningStock(parseDoubleOrZero(row.get("opening_stock")));
        item.setMinimumStock(parseDoubleOrZero(row.get("minimum_stock")));
        item.setLocation(nullIfEmpty(row.get("location")));
        item.setRemarks(nullIfEmpty(row.get("remarks")));
        return item;
    }

    private String generateItemCode() {
        return "ITEM-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0,6).toUpperCase();
    }

    private static String nullIfEmpty(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static double parseDoubleOrZero(String s) {
        if (s == null) return 0.0;
        String t = s.trim().replaceAll("[,₹ ]", "");
        if (t.isEmpty()) return 0.0;
        try { return Double.parseDouble(t); }
        catch (Exception e) { return 0.0; }
    }

    private void persistColumnPrefs() {
        if (tblImportPreview == null) return;
        StringBuilder sb = new StringBuilder();
        for (TableColumn<Map<String,String>, ?> col : tblImportPreview.getColumns()) {
            sb.append(col.getText()).append(":").append(col.isVisible()).append(",");
        }
        prefs.put("import_columns", sb.toString());
    }

    private void restoreColumnPrefs() {
        if (tblImportPreview == null) return;
        String saved = prefs.get("import_columns", "");
        if (!saved.isBlank()) {
            Map<String, Boolean> map = new HashMap<>();
            for (String part : saved.split(",")) {
                if (part.contains(":")) {
                    String[] kv = part.split(":");
                    map.put(kv[0], Boolean.parseBoolean(kv[1]));
                }
            }
            for (TableColumn<Map<String,String>, ?> col : tblImportPreview.getColumns()) {
                if (map.containsKey(col.getText())) col.setVisible(map.get(col.getText()));
            }
        }
    }

    private void showWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, message);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    // returns null when the input is empty or invalid
    private static Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("[,₹ ]", "");
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return null;
        }
    }



    private static class ImportSummary {
        final int processed;
        final int imported;
        final List<String> errors;
        ImportSummary(int processed, int imported, List<String> errors) {
            this.processed = processed;
            this.imported = imported;
            this.errors = errors == null ? Collections.emptyList() : errors;
        }
    }
}
