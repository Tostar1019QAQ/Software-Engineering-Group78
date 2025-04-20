package com.group78.financetracker.ui;

import com.group78.financetracker.model.Bill;
import com.group78.financetracker.service.BillService;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class BillsPanel extends JPanel {
    private final BillService billService;
    private JTable billsTable;
    private DefaultTableModel tableModel;

    private JTextField billNameField;
    private JTextField amountField;
    private JDateChooser dueDateChooser;
    private JComboBox<String> paymentMethodCombo;
    private JCheckBox emailNotificationCheck;
    private JCheckBox pushNotificationCheck;

    public BillsPanel(CardLayout cardLayout, JPanel contentPanel) {
        this.billService = new BillService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createFormPanel();
        add(createEnhancementPanel(), BorderLayout.CENTER); // Insert enhanced feature panel
    }

    private void createFormPanel() {
        JPanel formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Bill"));

        JPanel inputsPanel = new JPanel(new GridLayout(6, 2, 10, 10));

        billNameField = new JTextField();
        inputsPanel.add(new JLabel("Bill Name:"));
        inputsPanel.add(billNameField);

        amountField = new JTextField();
        inputsPanel.add(new JLabel("Amount:"));
        inputsPanel.add(amountField);

        dueDateChooser = new JDateChooser();
        dueDateChooser.setDate(new Date());
        dueDateChooser.setDateFormatString("yyyy-MM-dd");
        inputsPanel.add(new JLabel("Due Date:"));
        inputsPanel.add(dueDateChooser);

        String[] paymentMethods = {"Credit Card", "Debit Card", "Bank Transfer", "Cash"};
        paymentMethodCombo = new JComboBox<>(paymentMethods);
        inputsPanel.add(new JLabel("Payment Method:"));
        inputsPanel.add(paymentMethodCombo);

        JPanel notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        emailNotificationCheck = new JCheckBox("Email");
        pushNotificationCheck = new JCheckBox("Push Notification");
        notificationPanel.add(emailNotificationCheck);
        notificationPanel.add(pushNotificationCheck);

        inputsPanel.add(new JLabel("Notification Method:"));
        inputsPanel.add(notificationPanel);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clearButton = new JButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());

        JButton addButton = new JButton("Add Bill");
        // addButton.addActionListener(e -> addBill());

        buttonsPanel.add(clearButton);
        buttonsPanel.add(addButton);

        formPanel.add(inputsPanel, BorderLayout.CENTER);
        formPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(formPanel, BorderLayout.NORTH);
    }

    private JPanel createEnhancementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Search & Statistics"));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(12);
        JButton searchButton = new JButton("Search");
        searchPanel.add(new JLabel("Keyword:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JTextField categoryField = new JTextField(10);
        JButton filterButton = new JButton("Filter by Category");
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(categoryField);
        searchPanel.add(filterButton);

        JLabel totalLabel = new JLabel();
        JLabel upcomingLabel = new JLabel();
        JPanel statsPanel = new JPanel(new GridLayout(2, 1));
        statsPanel.add(totalLabel);
        statsPanel.add(upcomingLabel);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(searchPanel, BorderLayout.CENTER);
        topSection.add(statsPanel, BorderLayout.EAST);

        panel.add(topSection, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Name", "Amount", "Due Date", "Payment Method"}, 0);
        billsTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(billsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (!keyword.isEmpty()) {
                updateBillTable(billService.searchBills(keyword));
            }
        });

        filterButton.addActionListener(e -> {
            String category = categoryField.getText().trim();
            if (!category.isEmpty()) {
                updateBillTable(billService.getBillsByCategory(category));
            }
        });

        updateBillTable(billService.getAllBills());
        totalLabel.setText("Total Amount: ¥" + billService.getTotalBillAmount());
        upcomingLabel.setText("Upcoming Bills (within 7 days): " + billService.countUpcomingBillsWithinDays(7));

        return panel;
    }

    private void updateBillTable(List<Bill> billsToShow) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        for (Bill bill : billsToShow) {
            tableModel.addRow(new Object[]{
                bill.getName(),
                bill.getAmount(),
                bill.getDueDate(),
                bill.getPaymentMethod()
            });
        }
    }

    private void clearForm() {
        billNameField.setText("");
        amountField.setText("");
        dueDateChooser.setDate(new Date());
        paymentMethodCombo.setSelectedIndex(0);
        emailNotificationCheck.setSelected(false);
        pushNotificationCheck.setSelected(false);
    }
}
