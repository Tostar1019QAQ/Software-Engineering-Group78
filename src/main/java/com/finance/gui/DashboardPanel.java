package main.java.com.finance.gui;

import main.java.com.finance.model.Transaction;
import main.java.com.finance.service.FinanceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final JLabel balanceLabel;
    private final JLabel incomeLabel;
    private final JLabel expenseLabel;
    private final JPanel chartPanel;
    private final JTable recentTransactionsTable;
    
    public DashboardPanel() {
        setLayout(new BorderLayout());
        
        // Create top panel for summary
        JPanel summaryPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        balanceLabel = new JLabel("Balance: £0.00");
        incomeLabel = new JLabel("Income: £0.00");
        expenseLabel = new JLabel("Expenses: £0.00");
        
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 20));
        incomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        expenseLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        summaryPanel.add(balanceLabel);
        summaryPanel.add(incomeLabel);
        summaryPanel.add(expenseLabel);
        
        // Create chart panel
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // TODO: Implement chart drawing
            }
        };
        chartPanel.setPreferredSize(new Dimension(400, 200));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Monthly Overview"));
        
        // Create recent transactions table
        String[] columnNames = {"Date", "Category", "Description", "Amount"};
        Object[][] data = {};
        recentTransactionsTable = new JTable(data, columnNames);
        recentTransactionsTable.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(recentTransactionsTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Recent Transactions"));
        
        // Add components to panel
        add(summaryPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(tableScrollPane, BorderLayout.SOUTH);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshDashboard());
        add(refreshButton, BorderLayout.EAST);
        
        // Initial refresh
        refreshDashboard();
    }
    
    private void refreshDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        double balance = FinanceService.getFinanceService().getBalance(startOfMonth, today);
        double income = FinanceService.getFinanceService().getTotalIncome(startOfMonth, today);
        double expenses = FinanceService.getFinanceService().getTotalExpenses(startOfMonth, today);
        
        balanceLabel.setText(String.format("Balance: £%.2f", balance));
        incomeLabel.setText(String.format("Income: £%.2f", income));
        expenseLabel.setText(String.format("Expenses: £%.2f", expenses));
        
        // Update recent transactions
        List<Transaction> recentTransactions = FinanceService.getFinanceService()
                .getTransactionsByDateRange(startOfMonth, today);
        
        // TODO: Update table model with recent transactions
        
        // Repaint chart
        chartPanel.repaint();
    }
} 