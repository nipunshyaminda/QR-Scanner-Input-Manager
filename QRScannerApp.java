
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class QRScannerApp extends JFrame {

    private JTextField qrInput;
    private JTextArea outputArea;
    private JLabel statusLabel;

    public QRScannerApp() {

        setTitle("QR Scanner Input App");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel(
                "QR Scanner Input App",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        add(titleLabel, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // QR input field
        qrInput = new JTextField();
        qrInput.setFont(new Font("Arial", Font.PLAIN, 18));

        qrInput.addActionListener(e -> processQRCode());

        mainPanel.add(new JLabel("Scan QR Code:"), BorderLayout.NORTH);
        mainPanel.add(qrInput, BorderLayout.CENTER);

        // Buttons
        JButton scanButton = new JButton("Submit");
        JButton clearButton = new JButton("Clear");

        scanButton.addActionListener(e -> processQRCode());

        clearButton.addActionListener(e -> {
            qrInput.setText("");
            qrInput.requestFocusInWindow();
            statusLabel.setText("Ready to scan");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(scanButton);
        buttonPanel.add(clearButton);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Scanned QR Codes")
        );

        statusLabel = new JLabel("Ready to scan");

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Automatically focus the scanner input
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                qrInput.requestFocusInWindow();
            }
        });
    }

    private void processQRCode() {

        String qrData = qrInput.getText().trim();

        if (qrData.isEmpty()) {
            statusLabel.setText("Please scan a QR code first.");
            qrInput.requestFocusInWindow();
            return;
        }

        outputArea.append(qrData + "\n");

        statusLabel.setText("Successfully scanned: " + qrData);

        qrInput.setText("");
        qrInput.requestFocusInWindow();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            QRScannerApp app = new QRScannerApp();
            app.setVisible(true);
        });
    }
}
