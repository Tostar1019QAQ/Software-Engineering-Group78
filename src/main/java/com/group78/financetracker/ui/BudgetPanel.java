package com.group78.financetracker.ui;

import com.group78.financetracker.service.ImportService;
import com.group78.financetracker.service.DashboardService;
import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.FileReader;
import org.apache.commons.csv.CSVParser;

public class BudgetPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(BudgetPanel.class);
    private JPanel formPanel;
    private JPanel totalBudgetPanel;
    private JPanel categoryPanel;
    private JPanel tablePanel;
    private JPanel summaryPanel;
    private JTable budgetTable;
    private DefaultTableModel tableModel;
    private JTextField categoryField;
    private JTextField amountField;
    private JTextField totalBudgetField;
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JComboBox<String> existingCategoriesCombo;
    
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final ImportService importService;
    private final DashboardService dashboardService;
    private Map<String, BigDecimal> categoryBudgets = new HashMap<>();
    private Map<String, BigDecimal> categorySpending = new HashMap<>();
    private LocalDate budgetStartDate;
    private LocalDate budgetEndDate;

    public BudgetPanel(CardLayout cardLayout, JPanel contentPanel) {
        this.cardLayout = cardLayout;
        this.contentPanel = contentPanel;
        this.importService = new ImportService();
        this.dashboardService = new DashboardService();
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // First create all panels and components
        createTotalBudgetPanel();
        createCategoryPanel();
        createTablePanel();
        createSummaryPanel();
        
        // Then set layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        
        topPanel.add(totalBudgetPanel, BorderLayout.NORTH);
        topPanel.add(categoryPanel, BorderLayout.CENTER);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        // Make the whole panel scrollable
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.SOUTH);
        
        // Initialize current month as default budget period
        setDefaultBudgetPeriod();
        
        // Load transaction data from ImportService
        loadTransactionData();
        
        // Update category combo box
        updateCategoryComboBox();
        
        // Load budget data if exists
        loadBudgetData();
    }

    private void setDefaultBudgetPeriod() {
        LocalDate now = LocalDate.now();
        budgetStartDate = now.withDayOfMonth(1);
        budgetEndDate = now.withDayOfMonth(now.lengthOfMonth());
        
        // Update date choosers
        Date startDate = Date.from(budgetStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(budgetEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        startDateChooser.setDate(startDate);
        endDateChooser.setDate(endDate);
    }

    private void loadTransactionData() {
        try {
            List<Transaction> transactions = importService.getAllTransactions();
            calculateCategorySpending(transactions);
            updateBudgetTable();
            updateSummary();
        } catch (Exception e) {
            logger.error("Error loading transaction data: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                "Error loading transaction data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateCategorySpending(List<Transaction> transactions) {
        categorySpending.clear();
        totalSpent = BigDecimal.ZERO;
        
        // 根据日期范围筛选交易
        for (Transaction transaction : transactions) {
            LocalDate transactionDate = transaction.getDateTime().toLocalDate();
            
            // 检查交易是否在预算周期内
            if ((transactionDate.isEqual(budgetStartDate) || transactionDate.isAfter(budgetStartDate)) &&
                (transactionDate.isEqual(budgetEndDate) || transactionDate.isBefore(budgetEndDate))) {
                
                // 只处理支出
                if (transaction.getType() == TransactionType.EXPENSE) {
                    String category = transaction.getCategory();
                    BigDecimal amount = transaction.getAmount();
                    
                    // 添加到类别支出
                    categorySpending.merge(category, amount, BigDecimal::add);
                    
                    // 添加到总支出
                    totalSpent = totalSpent.add(amount);
                }
            }
        }
        
        logger.debug("Calculated spending: {} across {} categories", totalSpent, categorySpending.size());
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Title
        JLabel titleLabel = new JLabel("Budget Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        // Back button
        JButton backButton = new JButton("← Back to Dashboard");
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> cardLayout.show(contentPanel, "Dashboard"));
        
        // Refresh button
        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadTransactionData());
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(refreshButton);
        
        header.add(backButton, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }

    private void createTotalBudgetPanel() {
        totalBudgetPanel = new JPanel(new BorderLayout(10, 10));
        totalBudgetPanel.setBorder(BorderFactory.createTitledBorder("Set Total Budget"));

        // Input fields panel with vertical layout
        JPanel inputsPanel = new JPanel();
        inputsPanel.setLayout(new BoxLayout(inputsPanel, BoxLayout.Y_AXIS));
        inputsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        totalBudgetField = new JTextField();
        startDateChooser = new JDateChooser();
        endDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        
        // Total Budget input
        JPanel budgetPanel = new JPanel(new BorderLayout(5, 5));
        budgetPanel.add(new JLabel("Total Budget Amount:"), BorderLayout.NORTH);
        budgetPanel.add(totalBudgetField, BorderLayout.CENTER);
        
        // Date range panel with both date choosers in one row
        JPanel dateRangePanel = new JPanel(new BorderLayout(5, 5));
        JPanel dateLabelsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel dateChoosersPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        dateLabelsPanel.add(new JLabel("Start Date:"));
        dateLabelsPanel.add(new JLabel("End Date:"));
        dateChoosersPanel.add(startDateChooser);
        dateChoosersPanel.add(endDateChooser);
        
        dateRangePanel.add(dateLabelsPanel, BorderLayout.NORTH);
        dateRangePanel.add(dateChoosersPanel, BorderLayout.CENTER);
        
        // Add some spacing between components
        budgetPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        inputsPanel.add(budgetPanel);
        inputsPanel.add(dateRangePanel);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton setTotalBudgetButton = new JButton("Set Total Budget");
        
        setTotalBudgetButton.addActionListener(e -> setTotalBudget());
        
        buttonsPanel.add(setTotalBudgetButton);

        totalBudgetPanel.add(inputsPanel, BorderLayout.CENTER);
        totalBudgetPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void createCategoryPanel() {
        categoryPanel = new JPanel(new BorderLayout(10, 10));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Add Budget Category"));

        // Input fields panel with vertical layout
        JPanel inputsPanel = new JPanel();
        inputsPanel.setLayout(new BoxLayout(inputsPanel, BoxLayout.Y_AXIS));
        inputsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        categoryField = new JTextField();
        amountField = new JTextField();
        existingCategoriesCombo = new JComboBox<>();
        
        // Add a default option
        existingCategoriesCombo.addItem("-- Select Existing Category --");
        existingCategoriesCombo.addActionListener(e -> {
            String selected = (String) existingCategoriesCombo.getSelectedItem();
            if (selected != null && !selected.equals("-- Select Existing Category --")) {
                categoryField.setText(selected);
            }
        });
        
        // Category selection panel
        JPanel categorySelectionPanel = new JPanel(new BorderLayout(5, 5));
        categorySelectionPanel.add(new JLabel("Select Existing Category:"), BorderLayout.NORTH);
        categorySelectionPanel.add(existingCategoriesCombo, BorderLayout.CENTER);
        
        // Category name panel
        JPanel categoryNamePanel = new JPanel(new BorderLayout(5, 5));
        categoryNamePanel.add(new JLabel("Category Name:"), BorderLayout.NORTH);
        categoryNamePanel.add(categoryField, BorderLayout.CENTER);
        
        // Budget amount panel
        JPanel amountPanel = new JPanel(new BorderLayout(5, 5));
        amountPanel.add(new JLabel("Budget Amount:"), BorderLayout.NORTH);
        amountPanel.add(amountField, BorderLayout.CENTER);
        
        // Add some spacing between components
        categorySelectionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        categoryNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        inputsPanel.add(categorySelectionPanel);
        inputsPanel.add(categoryNamePanel);
        inputsPanel.add(amountPanel);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Form");
        JButton addButton = new JButton("Add Category");
        
        clearButton.addActionListener(e -> clearForm());
        addButton.addActionListener(e -> addBudgetCategory());
        
        buttonsPanel.add(clearButton);
        buttonsPanel.add(addButton);

        categoryPanel.add(inputsPanel, BorderLayout.CENTER);
        categoryPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void setTotalBudget() {
        try {
            // Validate inputs
            if (totalBudgetField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a total budget amount.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (startDateChooser.getDate() == null || endDateChooser.getDate() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select start and end dates for the budget period.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse total budget
            BigDecimal newTotalBudget = new BigDecimal(totalBudgetField.getText().trim());
            if (newTotalBudget.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid amount greater than zero.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse dates
            LocalDate newStartDate = startDateChooser.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            
            LocalDate newEndDate = endDateChooser.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            
            // Validate dates
            if (newEndDate.isBefore(newStartDate)) {
                JOptionPane.showMessageDialog(this, 
                    "End date cannot be before start date.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Update budget and date range
            totalBudget = newTotalBudget;
            budgetStartDate = newStartDate;
            budgetEndDate = newEndDate;
            
            // Recalculate category spending
            loadTransactionData();
            
            // Save budget data
            saveBudgetData();
            
            // Show success message
            JOptionPane.showMessageDialog(this, 
                "Total budget set successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid amount.", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createTablePanel() {
        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Budget Categories"));

        // Create table model
        String[] columns = {"Category", "Budget Amount", "Spent Amount", "Remaining", "Progress", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only actions column is editable
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) {
                    return JProgressBar.class; // Column 5 is progress bar
                }
                return super.getColumnClass(columnIndex);
            }
        };

        // Create table
        budgetTable = new JTable(tableModel);
        budgetTable.setRowHeight(30);
        budgetTable.getColumnModel().getColumn(4).setCellRenderer(new ProgressBarRenderer());
        
        // Set button renderer
        budgetTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        budgetTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = budgetTable.getColumnModel().getColumnIndexAtX(e.getX());
                int row = e.getY() / budgetTable.getRowHeight();
                
                if (row < budgetTable.getRowCount() && row >= 0 && column == 5) {
                    JPanel panel = (JPanel)budgetTable.getValueAt(row, 5);
                    
                    // Determine which button was clicked
                    Component[] components = panel.getComponents();
                    for (Component comp : components) {
                        if (comp instanceof JButton) {
                            JButton button = (JButton)comp;
                            if (button.getText().equals("Edit")) {
                                editCategory(row);
                            }
                        }
                    }
                }
            }
        });
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(budgetTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createSummaryPanel() {
        summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Budget Summary"));

        // Create summary cards
        summaryPanel.add(createSummaryCard("Total Budget", "¥0.00"));
        summaryPanel.add(createSummaryCard("Total Spent", "¥0.00"));
        summaryPanel.add(createSummaryCard("Remaining Budget", "¥0.00"));
    }

    private JPanel createSummaryCard(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(102, 102, 102));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(20f));
        valueLabel.setForeground(new Color(74, 107, 255));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);

        return card;
    }

    private void clearForm() {
        categoryField.setText("");
        amountField.setText("");
        existingCategoriesCombo.setSelectedIndex(0);
    }

    private void addBudgetCategory() {
        try {
            String category = categoryField.getText().trim();
            if (category.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a category name.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid amount greater than zero.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if category already exists
            if (categoryBudgets.containsKey(category)) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "This category already exists. Do you want to update the budget amount?",
                    "Category Exists",
                    JOptionPane.YES_NO_OPTION);
                    
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                
                // Update existing category
                updateExistingCategory(category, amount);
            } else {
                // Add new category
                categoryBudgets.put(category, amount);
                
                // Get spending for this category
                BigDecimal spent = categorySpending.getOrDefault(category, BigDecimal.ZERO);
                
                // Calculate remaining amount and progress percentage
                BigDecimal remaining = amount.subtract(spent);
                int percentage = (amount.compareTo(BigDecimal.ZERO) > 0) ? 
                    spent.multiply(new BigDecimal(100)).divide(amount, RoundingMode.HALF_UP).intValue() : 0;
                
                // Add to table
                Object[] rowData = {
                    category,
                    "¥" + amount.toString(),
                    "¥" + spent.toString(),
                    "¥" + remaining.toString(),
                    createProgressBar(percentage),
                    createActionButtons()
                };
                tableModel.addRow(rowData);
                
                // Update category combo box
                updateCategoryComboBox();
            }

            // Update summary
            updateSummary();
            
            // Save budget data
            saveBudgetData();

            // Clear form
            clearForm();

            // Show success message
            JOptionPane.showMessageDialog(this, 
                "Budget category added successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid amount.", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCategoryComboBox() {
        existingCategoriesCombo.removeAllItems();
        existingCategoriesCombo.addItem("-- Select Existing Category --");
        
        // Add all transaction categories
        Set<String> allCategories = new HashSet<>();
        for (Transaction transaction : importService.getAllTransactions()) {
            allCategories.add(transaction.getCategory());
        }
        
        // Add existing budget categories
        allCategories.addAll(categoryBudgets.keySet());
        
        // Add to combo box
        for (String category : allCategories) {
            existingCategoriesCombo.addItem(category);
        }
    }

    private void updateExistingCategory(String category, BigDecimal newAmount) {
        // Update budget mapping
        categoryBudgets.put(category, newAmount);
        
        // Find the row in the table
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(category)) {
                // Get spending for this category
                BigDecimal spent = categorySpending.getOrDefault(category, BigDecimal.ZERO);
                
                // Calculate remaining amount and progress percentage
                BigDecimal remaining = newAmount.subtract(spent);
                int percentage = (newAmount.compareTo(BigDecimal.ZERO) > 0) ? 
                    spent.multiply(new BigDecimal(100)).divide(newAmount, RoundingMode.HALF_UP).intValue() : 0;
                
                // Update table row
                tableModel.setValueAt("¥" + newAmount.toString(), i, 1);
                tableModel.setValueAt("¥" + remaining.toString(), i, 3);
                tableModel.setValueAt(createProgressBar(percentage), i, 4);
                
                break;
            }
        }
    }

    private String getStatus(BigDecimal spent, BigDecimal budget) {
        if (spent.compareTo(budget) >= 0) {
            return "Over Budget";
        } else if (spent.multiply(new BigDecimal(0.8)).compareTo(budget) >= 0) {
            return "Warning";
        } else {
            return "Normal";
        }
    }

    private JLabel createStatusLabel(String status) {
        JLabel label = new JLabel(status);
        switch (status) {
            case "Normal":
                label.setForeground(new Color(40, 167, 69));
                break;
            case "Warning":
                label.setForeground(new Color(255, 193, 7));
                break;
            case "Over Budget":
                label.setForeground(new Color(220, 53, 69));
                break;
        }
        return label;
    }

    private JProgressBar createProgressBar(int percentage) {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(percentage);
        progressBar.setStringPainted(true);
        
        if (percentage < 70) {
            progressBar.setForeground(new Color(40, 167, 69));
        } else if (percentage < 90) {
            progressBar.setForeground(new Color(255, 193, 7));
        } else {
            progressBar.setForeground(new Color(220, 53, 69));
        }
        
        return progressBar;
    }

    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        JButton editButton = new JButton("Edit");
        editButton.setFocusPainted(false);
        
        panel.add(editButton);
        
        return panel;
    }

    private void editCategory(int row) {
        String category = (String) tableModel.getValueAt(row, 0);
        String budgetAmount = ((String) tableModel.getValueAt(row, 1)).substring(1); // Remove ¥ symbol
        
        // Create form panel with vertical layout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField editCategoryField = new JTextField(category);
        JTextField editAmountField = new JTextField(budgetAmount);
        
        // Category name panel
        JPanel categoryNamePanel = new JPanel(new BorderLayout(5, 5));
        categoryNamePanel.add(new JLabel("Category Name:"), BorderLayout.NORTH);
        categoryNamePanel.add(editCategoryField, BorderLayout.CENTER);
        
        // Budget amount panel
        JPanel amountPanel = new JPanel(new BorderLayout(5, 5));
        amountPanel.add(new JLabel("Budget Amount:"), BorderLayout.NORTH);
        amountPanel.add(editAmountField, BorderLayout.CENTER);
        
        // Add spacing between components
        categoryNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        panel.add(categoryNamePanel);
        panel.add(amountPanel);
        
        int result = JOptionPane.showConfirmDialog(this, 
            panel, 
            "Edit Budget Category", 
            JOptionPane.OK_CANCEL_OPTION);
            
        if (result == JOptionPane.OK_OPTION) {
            try {
                String newCategory = editCategoryField.getText().trim();
                BigDecimal newAmount = new BigDecimal(editAmountField.getText().trim());
                
                if (newCategory.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter a category name.", 
                        "Input Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter a valid amount greater than zero.", 
                        "Input Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // If category name has changed
                if (!newCategory.equals(category)) {
                    // Check if new name already exists
                    if (categoryBudgets.containsKey(newCategory) && !newCategory.equals(category)) {
                        JOptionPane.showMessageDialog(this, 
                            "Category name already exists. Please use a different name.", 
                            "Input Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Update mappings
                    categoryBudgets.remove(category);
                    categoryBudgets.put(newCategory, newAmount);
                    
                    // Update table
                    tableModel.setValueAt(newCategory, row, 0);
                } else {
                    // Just update the amount
                    categoryBudgets.put(category, newAmount);
                }
                
                // Get spending for this category
                BigDecimal spent = categorySpending.getOrDefault(newCategory, BigDecimal.ZERO);
                
                // Calculate remaining amount and progress percentage
                BigDecimal remaining = newAmount.subtract(spent);
                int percentage = (newAmount.compareTo(BigDecimal.ZERO) > 0) ? 
                    spent.multiply(new BigDecimal(100)).divide(newAmount, RoundingMode.HALF_UP).intValue() : 0;
                
                // Update table row
                tableModel.setValueAt("¥" + newAmount.toString(), row, 1);
                tableModel.setValueAt("¥" + remaining.toString(), row, 3);
                tableModel.setValueAt(createProgressBar(percentage), row, 4);
                
                // Update summary
                updateSummary();
                
                // Save budget data
                saveBudgetData();
                
                // Update category combo box
                updateCategoryComboBox();
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid amount.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteCategory(int row) {
        String category = (String) tableModel.getValueAt(row, 0);
        
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete the category '" + category + "'?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            // Remove from mappings
            categoryBudgets.remove(category);
            
            // Remove from table
            tableModel.removeRow(row);
            
            // Update summary
            updateSummary();
            
            // Save budget data
            saveBudgetData();
            
            // Update category combo box
            updateCategoryComboBox();
        }
    }

    private void assignTransactions(int row) {
        String category = (String) tableModel.getValueAt(row, 0);
        
        // Get unassigned transactions
        List<Transaction> unassignedTransactions = new ArrayList<>();
        for (Transaction transaction : importService.getAllTransactions()) {
            if (transaction.getType() == TransactionType.EXPENSE && 
                (transaction.getCategory() == null || transaction.getCategory().trim().isEmpty() || 
                 transaction.getCategory().equals("Other"))) {
                unassignedTransactions.add(transaction);
            }
        }
        
        if (unassignedTransactions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No unassigned transactions found.",
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Create transaction selection panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 400));
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Transaction t : unassignedTransactions) {
            listModel.addElement(t.getDateTime().toLocalDate() + " - " + t.getDescription() + " - ¥" + t.getAmount());
        }
        
        JList<String> transactionList = new JList<>(listModel);
        transactionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(transactionList);
        panel.add(new JLabel("Select transactions to assign to category '" + category + "':"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this,
            panel,
            "Assign Transactions to Category",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if (result == JOptionPane.OK_OPTION) {
            int[] selectedIndices = transactionList.getSelectedIndices();
            
            if (selectedIndices.length == 0) {
                JOptionPane.showMessageDialog(this,
                    "Please select at least one transaction.",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Update selected transactions' categories
            for (int index : selectedIndices) {
                Transaction transaction = unassignedTransactions.get(index);
                
                // Since Transaction is immutable, we need to create a new object
                Transaction updatedTransaction = new Transaction(
                    transaction.getId(),
                    transaction.getAmount(),
                    category, // Update category
                    transaction.getDescription(),
                    transaction.getDateTime(),
                    transaction.getType(),
                    transaction.getPaymentMethod(),
                    transaction.getCurrency()
                );
                
                // Add to ImportService
                importService.addTransaction(updatedTransaction);
            }
            
            // Reload data to reflect changes
            loadTransactionData();
            
            JOptionPane.showMessageDialog(this,
                "Successfully assigned " + selectedIndices.length + " transactions to category '" + category + "'.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateBudgetTable() {
        tableModel.setRowCount(0);
        
        for (Map.Entry<String, BigDecimal> entry : categoryBudgets.entrySet()) {
            String category = entry.getKey();
            BigDecimal budget = entry.getValue();
            
            // Get spending for this category
            BigDecimal spent = categorySpending.getOrDefault(category, BigDecimal.ZERO);
            
            // Calculate remaining amount and progress percentage
            BigDecimal remaining = budget.subtract(spent);
            int percentage = (budget.compareTo(BigDecimal.ZERO) > 0) ? 
                spent.multiply(new BigDecimal(100)).divide(budget, RoundingMode.HALF_UP).intValue() : 0;
            
            // Determine status
            String status = getStatus(spent, budget);
            
            // Add to table
            Object[] rowData = {
                category,
                "¥" + budget.toString(),
                "¥" + spent.toString(),
                "¥" + remaining.toString(),
                createProgressBar(percentage),
                createActionButtons()
            };
            tableModel.addRow(rowData);
        }
    }

    private void updateSummary() {
        // Calculate total budget and spent amount
        BigDecimal categoryBudgetTotal = BigDecimal.ZERO;
        for (BigDecimal budget : categoryBudgets.values()) {
            categoryBudgetTotal = categoryBudgetTotal.add(budget);
        }
        
        // If total budget is not set, use category budget total
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            totalBudget = categoryBudgetTotal;
        }
        
        // Calculate remaining budget
        BigDecimal remainingBudget = totalBudget.subtract(totalSpent);
        if (remainingBudget.compareTo(BigDecimal.ZERO) < 0) {
            remainingBudget = BigDecimal.ZERO;
        }
        
        // Update summary cards
        for (Component component : summaryPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel card = (JPanel) component;
                JLabel valueLabel = (JLabel) card.getComponent(2);
                JLabel titleLabel = (JLabel) card.getComponent(0);
                
                switch (titleLabel.getText()) {
                    case "Total Budget":
                        valueLabel.setText("¥" + totalBudget.setScale(2, RoundingMode.HALF_UP).toString());
                        break;
                    case "Total Spent":
                        valueLabel.setText("¥" + totalSpent.setScale(2, RoundingMode.HALF_UP).toString());
                        break;
                    case "Remaining Budget":
                        valueLabel.setText("¥" + remainingBudget.setScale(2, RoundingMode.HALF_UP).toString());
                        break;
                }
            }
        }
    }

    // Custom progress bar renderer
    class ProgressBarRenderer extends JProgressBar implements TableCellRenderer {
        public ProgressBarRenderer() {
            super(0, 100);
            setStringPainted(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (value instanceof JProgressBar) {
                JProgressBar pb = (JProgressBar) value;
                setValue(pb.getValue());
                setForeground(pb.getForeground());
            } else {
                setValue(0);
            }
            return this;
        }
    }
    
    // Custom button renderer
    class ButtonRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return (Component) value;
        }
    }

    // Add methods to save/load budget data
    private void saveBudgetData() {
        try {
            // Create data directory if it doesn't exist
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // Create budget data file
            File budgetFile = new File("data/budget.csv");
            
            try (FileWriter writer = new FileWriter(budgetFile);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("TotalBudget", "StartDate", "EndDate", "Category", "Amount"))) {
                
                // Save total budget info
                csvPrinter.printRecord(
                    totalBudget.toString(),
                    budgetStartDate.toString(),
                    budgetEndDate.toString(),
                    "", // Empty category for total budget row
                    ""  // Empty amount for total budget row
                );
                
                // Save category budgets
                for (Map.Entry<String, BigDecimal> entry : categoryBudgets.entrySet()) {
                    csvPrinter.printRecord(
                        "", // Empty total budget for category rows
                        "", // Empty start date for category rows
                        "", // Empty end date for category rows
                        entry.getKey(),
                        entry.getValue().toString()
                    );
                }
                
                csvPrinter.flush();
                logger.info("Budget data saved successfully");
            }
            
            // Update Dashboard service with the budget
            dashboardService.updateTotalBudget(totalBudget, budgetStartDate, budgetEndDate);
            
        } catch (IOException e) {
            logger.error("Failed to save budget data: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                "Error saving budget data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadBudgetData() {
        try {
            File budgetFile = new File("data/budget.csv");
            if (!budgetFile.exists()) {
                logger.info("No budget data file found. Starting with empty budget.");
                return;
            }
            
            try (FileReader reader = new FileReader(budgetFile);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {
                
                boolean firstRow = true;
                
                for (CSVRecord record : csvParser) {
                    if (firstRow) {
                        // First row contains total budget info
                        String totalBudgetStr = record.get("TotalBudget");
                        String startDateStr = record.get("StartDate");
                        String endDateStr = record.get("EndDate");
                        
                        if (!totalBudgetStr.isEmpty()) {
                            totalBudget = new BigDecimal(totalBudgetStr);
                        }
                        
                        if (!startDateStr.isEmpty() && !endDateStr.isEmpty()) {
                            budgetStartDate = LocalDate.parse(startDateStr);
                            budgetEndDate = LocalDate.parse(endDateStr);
                            
                            // Update date choosers
                            startDateChooser.setDate(Date.from(budgetStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                            endDateChooser.setDate(Date.from(budgetEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        }
                        
                        firstRow = false;
                    } else {
                        // Other rows contain category budgets
                        String category = record.get("Category");
                        String amountStr = record.get("Amount");
                        
                        if (!category.isEmpty() && !amountStr.isEmpty()) {
                            BigDecimal amount = new BigDecimal(amountStr);
                            categoryBudgets.put(category, amount);
                        }
                    }
                }
                
                // Update UI with loaded data
                updateBudgetTable();
                updateSummary();
                
                // Update Dashboard service with the budget
                dashboardService.updateTotalBudget(totalBudget, budgetStartDate, budgetEndDate);
                
                logger.info("Budget data loaded successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to load budget data: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                "Error loading budget data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 