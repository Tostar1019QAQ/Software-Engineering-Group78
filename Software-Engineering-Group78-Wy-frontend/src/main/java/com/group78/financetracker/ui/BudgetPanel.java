package com.group78.financetracker.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BudgetPanel extends JPanel {
    private JPanel formPanel;
    private JPanel tablePanel;
    private JPanel summaryPanel;
    private JTable budgetTable;
    private DefaultTableModel tableModel;
    private JTextField categoryField;
    private JTextField amountField;
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal totalSpent = BigDecimal.ZERO;

    public BudgetPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        createFormPanel();
        createTablePanel();
        createSummaryPanel();
        
        // Layout components
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.SOUTH);
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
        
        header.add(backButton, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        
        return header;
    }

    private void createFormPanel() {
        formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Budget Category"));

        // Input fields panel
        JPanel inputsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        categoryField = new JTextField();
        amountField = new JTextField();
        
        inputsPanel.add(new JLabel("Category Name:"));
        inputsPanel.add(categoryField);
        inputsPanel.add(new JLabel("Monthly Budget Amount:"));
        inputsPanel.add(amountField);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Form");
        JButton addButton = new JButton("Add Category");
        
        clearButton.addActionListener(e -> clearForm());
        addButton.addActionListener(e -> addBudgetCategory());
        
        buttonsPanel.add(clearButton);
        buttonsPanel.add(addButton);

        formPanel.add(inputsPanel, BorderLayout.CENTER);
        formPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void createTablePanel() {
        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Budget Categories"));

        // Create table model with columns
        String[] columns = {"Category", "Budget Amount", "Spent Amount", "Remaining", "Status", "Progress", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only actions column is editable
            }
        };

        // Create table
        budgetTable = new JTable(tableModel);
        budgetTable.setRowHeight(30);
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(budgetTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createSummaryPanel() {
        summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Budget Summary"));

        // Create summary cards
        summaryPanel.add(createSummaryCard("Total Budget", "$0.00"));
        summaryPanel.add(createSummaryCard("Total Spent", "$0.00"));
        summaryPanel.add(createSummaryCard("Remaining Budget", "$0.00"));
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

            // Add to table
            Object[] rowData = {
                category,
                "$" + amount.toString(),
                "$0.00",
                "$" + amount.toString(),
                createStatusLabel("Normal"),
                createProgressBar(0),
                createActionButtons()
            };
            tableModel.addRow(rowData);

            // Update summary
            totalBudget = totalBudget.add(amount);
            updateSummary();

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
        JButton deleteButton = new JButton("Delete");
        
        editButton.setFocusPainted(false);
        deleteButton.setFocusPainted(false);
        
        panel.add(editButton);
        panel.add(deleteButton);
        
        return panel;
    }

    private void updateSummary() {
        // Update summary cards
        for (Component component : summaryPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel card = (JPanel) component;
                JLabel valueLabel = (JLabel) card.getComponent(2);
                JLabel titleLabel = (JLabel) card.getComponent(0);
                
                switch (titleLabel.getText()) {
                    case "Total Budget":
                        valueLabel.setText("$" + totalBudget.toString());
                        break;
                    case "Total Spent":
                        valueLabel.setText("$" + totalSpent.toString());
                        break;
                    case "Remaining Budget":
                        valueLabel.setText("$" + totalBudget.subtract(totalSpent).toString());
                        break;
                }
            }
        }
    }
} 