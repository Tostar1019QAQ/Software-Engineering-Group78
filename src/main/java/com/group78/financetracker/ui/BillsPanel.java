package com.group78.financetracker.ui;

import com.group78.financetracker.model.Bill;
import com.group78.financetracker.service.BillService;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class BillsPanel extends JPanel {
    private final BillService billService;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private JPanel formPanel;
    private JTable billsTable;
    private DefaultTableModel tableModel;

    // Form fields
    private JTextField billNameField;
    private JTextField amountField;
    private JDateChooser dueDateChooser;
    private JComboBox<String> paymentMethodCombo;
    private JCheckBox emailNotificationCheck;
    private JCheckBox pushNotificationCheck;

    public BillsPanel(CardLayout cardLayout, JPanel contentPanel) {
        this.cardLayout = cardLayout;
        this.contentPanel = contentPanel;
        this.billService = new BillService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createFormPanel();
        add(formPanel, BorderLayout.NORTH);
    }

    private void createFormPanel() {
        formPanel = new JPanel(new BorderLayout(10, 10));
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
        clearButton.setBackground(new Color(240, 128, 128));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> clearForm());

        JButton addButton = new JButton("Add Bill");
        addButton.setBackground(new Color(72, 209, 204));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        // 示例：这里未定义 addBill 方法，你可以根据你的业务逻辑添加
        // addButton.addActionListener(e -> addBill());

        buttonsPanel.add(clearButton);
        buttonsPanel.add(addButton);

        formPanel.add(inputsPanel, BorderLayout.CENTER);
        formPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void clearForm() {
        billNameField.setText("");
        amountField.setText("");
        dueDateChooser.setDate(new Date());
        paymentMethodCombo.setSelectedIndex(0);
        emailNotificationCheck.setSelected(false);
        pushNotificationCheck.setSelected(false);
    }

    // 其余方法根据实际项目内容保留或扩展
}
