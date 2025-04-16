package com.finance.gui;

import com.finance.FinanceApplication;
import com.finance.model.Budget;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class BudgetPanel extends JPanel {
    private final JTable budgetTable;
    private final DefaultTableModel tableModel;
    private final JTextField categoryField;
    private final JTextField amountField;
    private final JComboBox<String> periodComboBox;
    
    public BudgetPanel() {
        setLayout(new BorderLayout());
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Category field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Category:"), gbc);
        
        gbc.gridx = 1;
        categoryField = new JTextField(10);
        formPanel.add(categoryField, gbc);
        
        // Amount field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Amount:"), gbc);
        
        gbc.gridx = 1;
        amountField = new JTextField(10);
        formPanel.add(amountField, gbc);
        
        // Period combo box
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Period:"), gbc);
        
        gbc.gridx = 1;
        periodComboBox = new JComboBox<>(new String[]{"MONTHLY", "YEARLY", "CUSTOM"});
        formPanel.add(periodComboBox, gbc);
        
        // Add button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton addButton = new JButton("Add Budget");
        addButton.addActionListener(e -> addBudget());
        formPanel.add(addButton, gbc);
        
        // Create table
        String[] columnNames = {"Category", "Amount", "Spent", "Remaining", "Period"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        budgetTable = new JTable(tableModel);
        budgetTable.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(budgetTable);
        
        // Add components to panel
        add(formPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        
        // Add delete button
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedBudget());
        add(deleteButton, BorderLayout.SOUTH);
        
        // Initial load
        refreshBudgets();
    }
    
    private void addBudget() {
        try {
            String category = categoryField.getText();
            double amount = Double.parseDouble(amountField.getText());
            String period = (String) periodComboBox.getSelectedItem();
            
            Budget budget = new Budget();
            budget.setCategory(category);
            budget.setAmount(amount);
            budget.setSpent(0.0);
            budget.setPeriod(period);
            budget.setStartDate(LocalDate.now());
            
            if ("MONTHLY".equals(period)) {
                budget.setEndDate(LocalDate.now().plusMonths(1));
            } else if ("YEARLY".equals(period)) {
                budget.setEndDate(LocalDate.now().plusYears(1));
            } else {
                // For custom period, set end date to one month from now
                budget.setEndDate(LocalDate.now().plusMonths(1));
            }
            
            FinanceApplication.getFinanceService().createBudget(budget);
            
            // Clear fields
            categoryField.setText("");
            amountField.setText("");
            
            // Refresh table
            refreshBudgets();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid amount",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteSelectedBudget() {
        int selectedRow = budgetTable.getSelectedRow();
        if (selectedRow >= 0) {
            String category = (String) tableModel.getValueAt(selectedRow, 0);
            double amount = Double.parseDouble((String) tableModel.getValueAt(selectedRow, 1));
            double spent = Double.parseDouble((String) tableModel.getValueAt(selectedRow, 2));
            String period = (String) tableModel.getValueAt(selectedRow, 4);
            
            // Find and delete the budget
            List<Budget> budgets = FinanceApplication.getFinanceService().getBudgets();
            for (Budget budget : budgets) {
                if (budget.getCategory().equals(category) &&
                    budget.getAmount() == amount &&
                    budget.getSpent() == spent &&
                    budget.getPeriod().equals(period)) {
                    FinanceApplication.getFinanceService().deleteBudget(budget.getId());
                    break;
                }
            }
            
            refreshBudgets();
        }
    }
    
    private void refreshBudgets() {
        tableModel.setRowCount(0);
        List<Budget> budgets = FinanceApplication.getFinanceService().getBudgets();
        
        for (Budget budget : budgets) {
            tableModel.addRow(new Object[]{
                budget.getCategory(),
                String.format("%.2f", budget.getAmount()),
                String.format("%.2f", budget.getSpent()),
                String.format("%.2f", budget.getRemaining()),
                budget.getPeriod()
            });
        }
    }
} 