
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class QRScannerApp extends JFrame {

    private JTextField qrInput;
    private JTextField searchInput;
    private JTable table;
    private DefaultTableModel model;
    private JLabel statusLabel;
    private JLabel totalLabel;
    private JLabel uniqueLabel;
    private JLabel duplicateLabel;

    private final List<ScanRecord> records = new ArrayList<>();
    private final Set<String> uniqueCodes = new HashSet<>();

    private String lastScannedCode = "";

    private final Path dataFile = Paths.get(
            System.getProperty("user.home"),
            ".qr-scanner-gui",
            "scan-history.csv"
    );

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int duplicateCount = 0;

    public QRScannerApp() {

        setTitle("QR Scanner Pro");
        setSize(950, 650);
        setMinimumSize(new Dimension(750, 500));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadHistory();
        updateStatistics();
        configureShortcuts();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHistory();
                dispose();
                System.exit(0);
            }
        });
    }

    private void createInterface() {

        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel title = new JLabel("QR SCANNER PRO");
        title.setFont(new Font("Arial", Font.BOLD, 26));

        JLabel subtitle = new JLabel(
                "USB QR Scanner Input Manager"
        );
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.add(title);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 20, 15, 20)
        );

        // Scanner input
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(
                BorderFactory.createTitledBorder("Scanner Input")
        );

        qrInput = new JTextField();
        qrInput.setFont(new Font("Arial", Font.PLAIN, 18));

        JButton scanButton = new JButton("Submit");
        JButton copyLastButton = new JButton("Copy Last");

        scanButton.addActionListener(e -> processQRCode());
        qrInput.addActionListener(e -> processQRCode());

        copyLastButton.addActionListener(e -> copyLastCode());

        JPanel inputButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inputButtons.add(scanButton);
        inputButtons.add(copyLastButton);

        inputPanel.add(qrInput, BorderLayout.CENTER);
        inputPanel.add(inputButtons, BorderLayout.EAST);

        // Statistics
        JPanel statisticsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statisticsPanel.setBorder(
                BorderFactory.createEmptyBorder(5, 0, 5, 0)
        );

        totalLabel = new JLabel("Total Scans: 0", SwingConstants.CENTER);
        uniqueLabel = new JLabel("Unique Codes: 0", SwingConstants.CENTER);
        duplicateLabel = new JLabel("Duplicates: 0", SwingConstants.CENTER);

        Font statFont = new Font("Arial", Font.BOLD, 14);

        totalLabel.setFont(statFont);
        uniqueLabel.setFont(statFont);
        duplicateLabel.setFont(statFont);

        statisticsPanel.add(totalLabel);
        statisticsPanel.add(uniqueLabel);
        statisticsPanel.add(duplicateLabel);

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setBorder(
                BorderFactory.createTitledBorder("Search History")
        );

        searchInput = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton resetSearchButton = new JButton("Reset");

        searchButton.addActionListener(e -> applySearch());
        searchInput.addActionListener(e -> applySearch());

        resetSearchButton.addActionListener(e -> {
            searchInput.setText("");
            applySearch();
        });

        searchPanel.add(searchInput, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchButtons.add(searchButton);
        searchButtons.add(resetSearchButton);

        searchPanel.add(searchButtons, BorderLayout.EAST);

        // Table
        String[] columns = {
                "No.", "QR Code", "Date & Time", "Scan Type"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder("Scan History")
        );

        // Actions
        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 5)
        );

        JButton copySelectedButton = new JButton("Copy Selected");
        JButton copyAllButton = new JButton("Copy All");
        JButton deleteButton = new JButton("Delete Selected");
        JButton exportButton = new JButton("Export CSV");
        JButton clearButton = new JButton("Clear History");

        copySelectedButton.addActionListener(e -> copySelectedCodes());
        copyAllButton.addActionListener(e -> copyAllCodes());
        deleteButton.addActionListener(e -> deleteSelected());
        exportButton.addActionListener(e -> exportCSV());
        clearButton.addActionListener(e -> clearHistory());

        actionPanel.add(copySelectedButton);
        actionPanel.add(copyAllButton);
        actionPanel.add(deleteButton);
        actionPanel.add(exportButton);
        actionPanel.add(clearButton);

        // Status
        statusLabel = new JLabel("Ready to scan");
        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(actionPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        // Layout
        JPanel topContent = new JPanel(new BorderLayout(5, 5));
        topContent.add(inputPanel, BorderLayout.NORTH);
        topContent.add(statisticsPanel, BorderLayout.SOUTH);

        JPanel centerContent = new JPanel(new BorderLayout(5, 8));
        centerContent.add(searchPanel, BorderLayout.NORTH);
        centerContent.add(tableScroll, BorderLayout.CENTER);

        mainPanel.add(topContent, BorderLayout.NORTH);
        mainPanel.add(centerContent, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Initial focus
        SwingUtilities.invokeLater(() -> qrInput.requestFocusInWindow());
    }

    private void processQRCode() {

        String code = qrInput.getText().trim();

        if (code.isEmpty()) {
            statusLabel.setText("Please scan or enter a QR code.");
            qrInput.requestFocusInWindow();
            return;
        }

        boolean duplicate = uniqueCodes.contains(code);

        if (duplicate) {
            duplicateCount++;
        } else {
            uniqueCodes.add(code);
        }

        lastScannedCode = code;

        String scanType = duplicate ? "DUPLICATE" : "NEW";

        ScanRecord record = new ScanRecord(
                records.size() + 1,
                code,
                LocalDateTime.now().format(formatter),
                scanType
        );

        records.add(record);
        model.addRow(new Object[] {
                record.number,
                record.code,
                record.dateTime,
                record.scanType
        });

        updateStatistics();
        saveHistory();

        if (duplicate) {
            statusLabel.setText("Duplicate scan detected: " + code);
            Toolkit.getDefaultToolkit().beep();
        } else {
            statusLabel.setText("Successfully scanned: " + code);
        }

        qrInput.setText("");
        qrInput.requestFocusInWindow();
    }

    private void copyLastCode() {

        if (lastScannedCode.isEmpty()) {
            statusLabel.setText("No scanned code available.");
            return;
        }

        copyToClipboard(lastScannedCode);
    }

    private void copySelectedCodes() {

        int[] selectedRows = table.getSelectedRows();

        if (selectedRows.length == 0) {
            statusLabel.setText("Select one or more records first.");
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int row : selectedRows) {
            int modelRow = table.convertRowIndexToModel(row);
            text.append(model.getValueAt(modelRow, 1)).append("\n");
        }

        copyToClipboard(text.toString().trim());
    }

    private void copyAllCodes() {

        if (records.isEmpty()) {
            statusLabel.setText("There are no codes to copy.");
            return;
        }

        StringBuilder text = new StringBuilder();

        for (ScanRecord record : records) {
            text.append(record.code).append("\n");
        }

        copyToClipboard(text.toString().trim());
    }

    private void copyToClipboard(String text) {

        try {
            StringSelection selection = new StringSelection(text);

            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, null);

            statusLabel.setText("Copied to clipboard successfully.");

        } catch (Exception e) {
            statusLabel.setText("Unable to access clipboard.");
        }
    }

    private void applySearch() {

        String query = searchInput.getText().trim();

        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter =
                (TableRowSorter<DefaultTableModel>) table.getRowSorter();

        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 1)
            );
        }

        statusLabel.setText(
                "Search completed. Visible records: " + table.getRowCount()
        );
    }

    private void deleteSelected() {

        int[] selectedRows = table.getSelectedRows();

        if (selectedRows.length == 0) {
            statusLabel.setText("Select records to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete " + selectedRows.length + " selected record(s)?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        List<Integer> modelRows = new ArrayList<>();

        for (int row : selectedRows) {
            modelRows.add(table.convertRowIndexToModel(row));
        }

        modelRows.sort(Collections.reverseOrder());

        for (int row : modelRows) {
            model.removeRow(row);
            records.remove(row);
        }

        rebuildRecords();
        updateStatistics();
        saveHistory();

        statusLabel.setText("Selected records deleted.");
        qrInput.requestFocusInWindow();
    }

    private void clearHistory() {

        if (records.isEmpty()) {
            statusLabel.setText("History is already empty.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Permanently clear all scan history?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        records.clear();
        uniqueCodes.clear();
        model.setRowCount(0);

        duplicateCount = 0;
        lastScannedCode = "";

        updateStatistics();
        saveHistory();

        statusLabel.setText("Scan history cleared.");
        qrInput.requestFocusInWindow();
    }

    private void rebuildRecords() {

        model.setRowCount(0);
        uniqueCodes.clear();
        duplicateCount = 0;

        for (int i = 0; i < records.size(); i++) {

            ScanRecord old = records.get(i);

            boolean duplicate = !uniqueCodes.add(old.code);

            if (duplicate) {
                duplicateCount++;
            }

            String type = duplicate ? "DUPLICATE" : "NEW";

            ScanRecord updated = new ScanRecord(
                    i + 1,
                    old.code,
                    old.dateTime,
                    type
            );

            records.set(i, updated);

            model.addRow(new Object[] {
                    updated.number,
                    updated.code,
                    updated.dateTime,
                    updated.scanType
            });
        }

        lastScannedCode = records.isEmpty()
                ? ""
                : records.get(records.size() - 1).code;
    }

    private void updateStatistics() {

        totalLabel.setText("Total Scans: " + records.size());
        uniqueLabel.setText("Unique Codes: " + uniqueCodes.size());
        duplicateLabel.setText("Duplicates: " + duplicateCount);
    }

    private void exportCSV() {

        if (records.isEmpty()) {
            statusLabel.setText("No records available to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Export Scan History");

        chooser.setFileFilter(
                new FileNameExtensionFilter("CSV Files (*.csv)", "csv")
        );

        chooser.setSelectedFile(
                new File("qr-scan-history.csv")
        );

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "The file already exists. Overwrite it?",
                    "Confirm Export",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                file.toPath(), StandardCharsets.UTF_8)) {

            writer.write("No.,QR Code,Date & Time,Scan Type");
            writer.newLine();

            for (ScanRecord record : records) {
                writer.write(
                        record.number + "," +
                        csvEscape(record.code) + "," +
                        csvEscape(record.dateTime) + "," +
                        csvEscape(record.scanType)
                );

                writer.newLine();
            }

            statusLabel.setText("Exported successfully: " + file.getName());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Export failed: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String csvEscape(String value) {

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void saveHistory() {

        try {
            Files.createDirectories(dataFile.getParent());

            Path tempFile = dataFile.resolveSibling(
                    dataFile.getFileName().toString() + ".tmp"
            );

            try (BufferedWriter writer = Files.newBufferedWriter(
                    tempFile, StandardCharsets.UTF_8)) {

                writer.write("QR Code,Date & Time");
                writer.newLine();

                for (ScanRecord record : records) {
                    writer.write(
                            csvEscape(record.code) + "," +
                            csvEscape(record.dateTime)
                    );

                    writer.newLine();
                }
            }

            try {
                Files.move(
                        tempFile,
                        dataFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException e) {
                Files.move(
                        tempFile,
                        dataFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            statusLabel.setText("Warning: Could not save scan history.");
        }
    }

    private void loadHistory() {

        if (!Files.exists(dataFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(
                dataFile, StandardCharsets.UTF_8)) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                List<String> fields = parseCSVLine(line);

                if (fields.size() < 2) {
                    continue;
                }

                String code = fields.get(0);
                String dateTime = fields.get(1);

                boolean duplicate = !uniqueCodes.add(code);

                if (duplicate) {
                    duplicateCount++;
                }

                ScanRecord record = new ScanRecord(
                        records.size() + 1,
                        code,
                        dateTime,
                        duplicate ? "DUPLICATE" : "NEW"
                );

                records.add(record);

                model.addRow(new Object[] {
                        record.number,
                        record.code,
                        record.dateTime,
                        record.scanType
                });
            }

            if (!records.isEmpty()) {
                lastScannedCode = records.get(records.size() - 1).code;
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load saved history:\n" + e.getMessage(),
                    "History Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private List<String> parseCSVLine(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                if (insideQuotes &&
                        i + 1 < line.length() &&
                        line.charAt(i + 1) == '"') {

                    field.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (c == ',' && !insideQuotes) {

                fields.add(field.toString());
                field.setLength(0);

            } else {
                field.append(c);
            }
        }

        fields.add(field.toString());

        return fields;
    }

    private void configureShortcuts() {

        JRootPane root = getRootPane();

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                        InputEvent.CTRL_DOWN_MASK), "focusScanner");

        root.getActionMap().put("focusScanner", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                qrInput.requestFocusInWindow();
            }
        });

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                        InputEvent.CTRL_DOWN_MASK), "focusSearch");

        root.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchInput.requestFocusInWindow();
                searchInput.selectAll();
            }
        });
    }

    private static class ScanRecord {

        int number;
        String code;
        String dateTime;
        String scanType;

        ScanRecord(int number, String code,
                   String dateTime, String scanType) {

            this.number = number;
            this.code = code;
            this.dateTime = dateTime;
            this.scanType = scanType;
        }
    }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            QRScannerApp app = new QRScannerApp();
            app.setVisible(true);
        });
    }
}
