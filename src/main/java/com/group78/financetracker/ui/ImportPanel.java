package com.group78.financetracker.ui;

import com.group78.financetracker.service.ImportService;
import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import com.group78.financetracker.ui.DashboardPanel;
import com.group78.financetracker.ui.MainFrame;
import com.group78.financetracker.service.DashboardService;
import com.toedter.calendar.JDateChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.TooManyListenersException;

public class ImportPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ImportPanel.class);
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final ImportService importService;
    private JPanel dropZone;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    
    // Manual entry components
    private JPanel manualEntryPanel;
    private JDateChooser dateChooser;
    private JTextField descriptionField;
    private JTextField amountField;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> paymentMethodComboBox;
    private JComboBox<String> currencyComboBox;
    private JComboBox<TransactionType> typeComboBox;

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

        // Create tabbed pane for different import methods
        JTabbedPane importTabs = new JTabbedPane();
        
        // File upload tab
        JPanel uploadPanel = createUploadPanel();
        importTabs.addTab("File Import", uploadPanel);
        
        // Manual entry tab
        manualEntryPanel = createManualEntryPanel();
        importTabs.addTab("Manual Entry", manualEntryPanel);
        
        // API import tab
        JPanel apiPanel = createApiPanel();
        importTabs.addTab("API Import", apiPanel);
        
        leftPanel.add(importTabs, BorderLayout.CENTER);
        
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

        JLabel dropLabel = new JLabel("Drag & Drop CSV or XLSX File Here", SwingConstants.CENTER);
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
    
    private JPanel createManualEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Manual Transaction Entry",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Date field
        formPanel.add(new JLabel("Date:"));
        dateChooser = new JDateChooser();
        dateChooser.setDate(new Date()); // Set to current date
        formPanel.add(dateChooser);
        
        // Description field
        formPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        formPanel.add(descriptionField);
        
        // Amount field
        formPanel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        formPanel.add(amountField);
        
        // Transaction type
        formPanel.add(new JLabel("Type:"));
        typeComboBox = new JComboBox<>(TransactionType.values());
        formPanel.add(typeComboBox);
        
        // Category field with predefined options
        formPanel.add(new JLabel("Category:"));
        categoryComboBox = new JComboBox<>(new String[]{
            "Food", "Transport", "Shopping", "Entertainment", 
            "Housing", "Healthcare", "Education", "Communication", "Others"
        });
        categoryComboBox.setEditable(true); // Allow custom categories
        formPanel.add(categoryComboBox);
        
        // Payment method
        formPanel.add(new JLabel("Payment Method:"));
        paymentMethodComboBox = new JComboBox<>(new String[]{
            "Cash", "Credit Card", "Debit Card", "Bank Transfer", 
            "Mobile Payment", "Other"
        });
        paymentMethodComboBox.setEditable(true); // Allow custom payment methods
        formPanel.add(paymentMethodComboBox);
        
        // Currency
        formPanel.add(new JLabel("Currency:"));
        currencyComboBox = new JComboBox<>(new String[]{
            "USD", "EUR", "CNY", "GBP", "JPY"
        });
        currencyComboBox.setEditable(true);
        currencyComboBox.setSelectedItem("USD"); // Default to USD
        formPanel.add(currencyComboBox);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        JButton addButton = new JButton("Add Transaction");
        
        clearButton.addActionListener(e -> clearManualEntryForm());
        addButton.addActionListener(e -> addManualTransaction());
        
        buttonPanel.add(clearButton);
        buttonPanel.add(addButton);
        
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void clearManualEntryForm() {
        dateChooser.setDate(new Date());
        descriptionField.setText("");
        amountField.setText("");
        typeComboBox.setSelectedIndex(0);
        categoryComboBox.setSelectedIndex(0);
        paymentMethodComboBox.setSelectedIndex(0);
        currencyComboBox.setSelectedItem("USD");
    }
    
    private void addManualTransaction() {
        try {
            // Validate input fields
            if (dateChooser.getDate() == null) {
                throw new IllegalArgumentException("Please select a date");
            }
            
            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                throw new IllegalArgumentException("Description cannot be empty");
            }
            
            String amountStr = amountField.getText().trim();
            if (amountStr.isEmpty()) {
                throw new IllegalArgumentException("Amount cannot be empty");
            }
            
            // Parse amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr.replace(",", ""));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid amount format");
            }
            
            // If amount is negative and type is EXPENSE, make it positive
            TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
            if (type == TransactionType.EXPENSE && amount.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.negate();
            } else if (type == TransactionType.INCOME && amount.compareTo(BigDecimal.ZERO) < 0) {
                amount = amount.abs();
            }
            
            // Get other fields
            String category = categoryComboBox.getSelectedItem().toString().trim();
            String paymentMethod = paymentMethodComboBox.getSelectedItem().toString().trim();
            String currency = currencyComboBox.getSelectedItem().toString().trim();
            
            // Convert date to LocalDateTime
            LocalDateTime dateTime = dateChooser.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            
            // Create transaction
            Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                amount.abs(), // Store absolute value
                category,
                description,
                dateTime,
                type,
                paymentMethod,
                currency
            );
            
            // Add to service
            importService.addTransaction(transaction);
            
            // Update preview
            updatePreview(List.of(transaction));
            
            // Show success message
            statusLabel.setText("Transaction added successfully");
            
            // Clear form for next entry
            clearManualEntryForm();
            
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, 
                e.getMessage(), 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
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
        
        // Add file filters for CSV and XLSX
        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
            "CSV Files (*.csv)", "csv");
        FileNameExtensionFilter excelFilter = new FileNameExtensionFilter(
            "Excel Files (*.xlsx)", "xlsx");
        
        fileChooser.addChoosableFileFilter(csvFilter);
        fileChooser.addChoosableFileFilter(excelFilter);
        fileChooser.setFileFilter(csvFilter); // Default to CSV
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            processImportedFile(fileChooser.getSelectedFile());
        }
    }

    private void processImportedFile(File file) {
        try {
            List<Transaction> transactions = importService.importFromFile(file);
            updatePreview(transactions);
            statusLabel.setText("Data imported successfully: " + transactions.size() + " transactions");
            
            // Refresh the dashboard 
            updateDashboard();
            
            // Show success message with option to view dashboard
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Data imported successfully! Would you like to view the updated dashboard?",
                "Import Successful",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );
            
            if (choice == JOptionPane.YES_OPTION) {
                // Switch to dashboard and ensure it's refreshed
                JOptionPane.showMessageDialog(
                this,
                "Please click \"refresh\" button to view the updated dashboard",
                "Import Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
                cardLayout.show(contentPanel, "Dashboard");
                forceRefreshDashboard();
            }
            
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
            String amount = transaction.getType() == TransactionType.EXPENSE ? 
                "-" + transaction.getAmount().toString() : 
                transaction.getAmount().toString();
                
            Object[] rowData = {
                transaction.getDateTime().toLocalDate(),
                transaction.getDescription(),
                amount + " " + transaction.getCurrency(),
                transaction.getCategory(),
                transaction.getPaymentMethod(),
                transaction.getCurrency()
            };
            tableModel.addRow(rowData);
        }
    }

    private void updateDashboard() {
        logger.debug("尝试更新仪表板...");
        
        // Try to find MainFrame from parent components
        Container parent = this.getParent();
        while (parent != null && !(parent instanceof MainFrame)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof MainFrame) {
            logger.debug("找到了MainFrame，尝试获取DashboardPanel");
            MainFrame mainFrame = (MainFrame) parent;
            
            try {
                Component[] components = mainFrame.getContentPane().getComponents();
                for (Component component : components) {
                    if (component instanceof DashboardPanel) {
                        logger.debug("找到DashboardPanel，进行更新");
                        ((DashboardPanel) component).updateDashboard();
                        return;
                    }
                }
                logger.debug("dashboardPanel不是DashboardPanel类型");
            } catch (Exception e) {
                logger.error("无法访问DashboardPanel: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("未找到MainFrame，使用备用方法");
            forceRefreshDashboard();
        }
    }
    
    private void forceRefreshDashboard() {
        // Try to get Dashboard directly from MainFrame
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            logger.debug("Found parent JFrame");
            Container contentPane = ((JFrame) window).getContentPane();
            forceRefreshComponents(contentPane);
        } else {
            logger.debug("Could not find parent JFrame");
        }
    }
    
    private void forceRefreshComponents(Container container) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            logger.debug("Searching in component: {}", component.getClass().getName());
            
            if (component instanceof DashboardPanel) {
                logger.debug("Found DashboardPanel through recursive search");
                ((DashboardPanel) component).updateDashboard();
                return;
            } else if (component instanceof Container) {
                forceRefreshComponents((Container) component);
            }
        }
    }
} 