package com.group78.financetracker.ui;

import com.group78.financetracker.service.ImportService;
import com.group78.financetracker.model.Transaction;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.TooManyListenersException;

public class ImportPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final ImportService importService;
    private JPanel dropZone;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    public ImportPanel(CardLayout cardLayout, JPanel contentPanel) {
        this.cardLayout = cardLayout;
        this.contentPanel = contentPanel;
        this.importService = new ImportService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createComponents();
    }

    private void createComponents() {
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Main content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));

        // Left panel for import options
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 0));

        // File upload section
        JPanel uploadPanel = createUploadPanel();
        leftPanel.add(uploadPanel, BorderLayout.NORTH);

        // API section
        JPanel apiPanel = createApiPanel();
        leftPanel.add(apiPanel, BorderLayout.CENTER);

        mainContent.add(leftPanel, BorderLayout.WEST);

        // Preview section
        JPanel previewPanel = createPreviewPanel();
        mainContent.add(previewPanel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Import Transactions");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton backButton = new JButton("← Back to Dashboard");
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> cardLayout.show(contentPanel, "Dashboard"));

        header.add(backButton, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "File Upload",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));

        // Drop zone
        dropZone = new JPanel(new BorderLayout());
        dropZone.setPreferredSize(new Dimension(0, 200));
        dropZone.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        dropZone.setBackground(new Color(245, 247, 250));

        JLabel dropLabel = new JLabel("Drag & Drop CSV File Here", SwingConstants.CENTER);
        dropLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dropZone.add(dropLabel, BorderLayout.CENTER);

        // Setup drag and drop
        setupDragAndDrop();

        // Browse button
        JButton browseButton = new JButton("Browse Files");
        browseButton.addActionListener(e -> browseFiles());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(browseButton);

        panel.add(dropZone, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createApiPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "API Import",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // API URL
        JTextField apiUrlField = new JTextField();
        formPanel.add(new JLabel("API URL:"));
        formPanel.add(apiUrlField);

        // API Key
        JTextField apiKeyField = new JTextField();
        formPanel.add(new JLabel("API Key:"));
        formPanel.add(apiKeyField);

        // Account Number
        JTextField accountField = new JTextField();
        formPanel.add(new JLabel("Account Number:"));
        formPanel.add(accountField);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetButton = new JButton("Reset");
        JButton connectButton = new JButton("Connect API");

        resetButton.addActionListener(e -> {
            apiUrlField.setText("");
            apiKeyField.setText("");
            accountField.setText("");
        });

        connectButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "API connection feature is not implemented yet.",
                "Not Implemented",
                JOptionPane.INFORMATION_MESSAGE);
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(connectButton);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Preview",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));

        // Status label
        statusLabel = new JLabel("No data imported yet", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        String[] columns = {"Date", "Description", "Amount", "Category", "Payment Method", "Currency"};
        tableModel = new DefaultTableModel(columns, 0);
        previewTable = new JTable(tableModel);
        previewTable.setFillsViewportHeight(true);

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(previewTable);
        
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupDragAndDrop() {
        DropTarget dropTarget = new DropTarget();
        try {
            dropTarget.addDropTargetListener(new DropTargetListener() {
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (isDragAcceptable(dtde)) {
                        dropZone.setBorder(BorderFactory.createLineBorder(new Color(74, 107, 255), 2, true));
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {
                        dtde.rejectDrag();
                    }
                }

                public void dragExit(DropTargetEvent dte) {
                    dropZone.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
                }

                public void dragOver(DropTargetDragEvent dtde) {
                    // Not needed
                }

                public void dropActionChanged(DropTargetDragEvent dtde) {
                    if (isDragAcceptable(dtde)) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {
                        dtde.rejectDrag();
                    }
                }

                public void drop(DropTargetDropEvent dtde) {
                    try {
                        Transferable tr = dtde.getTransferable();
                        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY);
                            @SuppressWarnings("unchecked")
                            List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                            if (!files.isEmpty()) {
                                processImportedFile(files.get(0));
                            }
                            dtde.dropComplete(true);
                        } else {
                            dtde.rejectDrop();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        dtde.rejectDrop();
                    }
                }
            });
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
        dropZone.setDropTarget(dropTarget);
    }

    private boolean isDragAcceptable(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    private void browseFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            processImportedFile(fileChooser.getSelectedFile());
        }
    }

    private void processImportedFile(File file) {
        try {
            List<Transaction> transactions = importService.importFromCSV(file.getAbsolutePath());
            updatePreview(transactions);
            statusLabel.setText("Data imported successfully: " + transactions.size() + " transactions");
            
            // Refresh the dashboard
            updateDashboard();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error importing file: " + e.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePreview(List<Transaction> transactions) {
        tableModel.setRowCount(0);
        for (Transaction transaction : transactions) {
            Object[] rowData = {
                transaction.getDateTime().toLocalDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getPaymentMethod(),
                transaction.getCurrency()
            };
            tableModel.addRow(rowData);
        }
    }

    private void updateDashboard() {
        Component[] components = contentPanel.getComponents();
        for (Component component : components) {
            if (component instanceof DashboardPanel) {
                ((DashboardPanel) component).updateDashboard();
                break;
            }
        }
    }
} 