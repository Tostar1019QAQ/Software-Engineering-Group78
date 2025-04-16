package main.java.com.finance.gui;

import main.java.com.finance.model.Transaction;
import main.java.com.finance.service.FinanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionPanel extends JPanel {
    private final JTable transactionTable;
    private final DefaultTableModel tableModel;
    private final JTextField amountField;
    private final JTextField categoryField;
    private final JTextField descriptionField;
    private final JComboBox<String> typeComboBox;
    
    public TransactionPanel() {
        setLayout(new BorderLayout());
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Amount field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Amount:"), gbc);
        
        gbc.gridx = 1;
        amountField = new JTextField(10);
        formPanel.add(amountField, gbc);
        
        // Category field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        
        gbc.gridx = 1;
        categoryField = new JTextField(10);
        formPanel.add(categoryField, gbc);
        
        // Description field
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        descriptionField = new JTextField(10);
        formPanel.add(descriptionField, gbc);
        
        // Type combo box
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Type:"), gbc);
        
        gbc.gridx = 1;
        typeComboBox = new JComboBox<>(new String[]{"INCOME", "EXPENSE"});
        formPanel.add(typeComboBox, gbc);
        
        // Add button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton addButton = new JButton("Add Transaction");
        addButton.addActionListener(e -> addTransaction());
        formPanel.add(addButton, gbc);
        
        // Create table
        String[] columnNames = {"Date", "Category", "Description", "Amount", "Type"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionTable = new JTable(tableModel);
        transactionTable.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(transactionTable);
        
        // Add components to panel
        add(formPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        
        // Add delete button
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedTransaction());
        add(deleteButton, BorderLayout.SOUTH);
        
        // Initial load
        refreshTransactions();
    }
    
    private void addTransaction() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            String category = categoryField.getText();
            String description = descriptionField.getText();
            String type = (String) typeComboBox.getSelectedItem();
            
            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setDescription(description);
            transaction.setType(type);
            transaction.setDate(LocalDateTime.now());
            
            FinanceService.getFinanceService().addTransaction(transaction);
            
            // Clear fields
            amountField.setText("");
            categoryField.setText("");
            descriptionField.setText("");
            
            // Refresh table
            refreshTransactions();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid amount",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow >= 0) {
            String date = (String) tableModel.getValueAt(selectedRow, 0);
            String category = (String) tableModel.getValueAt(selectedRow, 1);
            String description = (String) tableModel.getValueAt(selectedRow, 2);
            double amount = Double.parseDouble((String) tableModel.getValueAt(selectedRow, 3));
            String type = (String) tableModel.getValueAt(selectedRow, 4);
            
            // Find and delete the transaction
            List<Transaction> transactions = FinanceService.getFinanceService().getTransactions();
            for (Transaction transaction : transactions) {
                if (transaction.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).equals(date) &&
                    transaction.getCategory().equals(category) &&
                    transaction.getDescription().equals(description) &&
                    transaction.getAmount() == amount &&
                    transaction.getType().equals(type)) {
                    FinanceService.getFinanceService().deleteTransaction(transaction.getId());
                    break;
                }
            }
            
            refreshTransactions();
        }
    }
    
    private void refreshTransactions() {
        tableModel.setRowCount(0);
        List<Transaction> transactions = FinanceService.getFinanceService().getTransactions();
        
        for (Transaction transaction : transactions) {
            tableModel.addRow(new Object[]{
                transaction.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                transaction.getCategory(),
                transaction.getDescription(),
                String.format("%.2f", transaction.getAmount()),
                transaction.getType()
            });
        }
    }
} 