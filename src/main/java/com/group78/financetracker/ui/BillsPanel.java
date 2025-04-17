package com.group78.financetracker.ui;

import com.group78.financetracker.model.Bill;
import com.group78.financetracker.service.BillService;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillsPanel extends JPanel {
    private final BillService billService;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private JPanel formPanel;
    private JPanel tablePanel;
    private JPanel alertsPanel;
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
        createTablePanel();
        createAlertsPanel();
        
        // Layout components
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(alertsPanel, BorderLayout.EAST);
        
        // Load initial data
        refreshData();
    }

    private void refreshData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        // Load bills from service
        List<Bill> bills = billService.getAllBills();
        for (Bill bill : bills) {
            addBillToTable(bill);
        }
        
        // Update alerts panel
        updateAlertsPanel();
    }

    private void updateAlertsPanel() {
        // Remove all existing alerts
        alertsPanel.removeAll();
        alertsPanel.setLayout(new BorderLayout());
        alertsPanel.setBorder(BorderFactory.createTitledBorder("Upcoming Bill Alerts"));
        alertsPanel.setPreferredSize(new Dimension(250, 0));

        JPanel alertsList = new JPanel();
        alertsList.setLayout(new BoxLayout(alertsList, BoxLayout.Y_AXIS));
        
        // Add alerts for upcoming and overdue bills
        List<Bill> upcomingBills = billService.getUpcomingBills();
        if (upcomingBills.isEmpty()) {
            JLabel noAlertsLabel = new JLabel("No upcoming bills");
            noAlertsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            alertsList.add(noAlertsLabel);
        } else {
            for (Bill bill : upcomingBills) {
                alertsList.add(createAlertCard(bill));
                alertsList.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(alertsList);
        scrollPane.setBorder(null);
        alertsPanel.add(scrollPane, BorderLayout.CENTER);
        alertsPanel.revalidate();
        alertsPanel.repaint();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Title
        JLabel titleLabel = new JLabel("Bill Reminders");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        // Back button
        JButton backButton = new JButton("← Back to Dashboard");
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> cardLayout.show(contentPanel, "Dashboard"));
        
        header.add(backButton, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        
        return header;
    }

    private void createFormPanel() {
        formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Bill"));

        // Input fields panel
        JPanel inputsPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        
        // Bill Name
        billNameField = new JTextField();
        inputsPanel.add(new JLabel("Bill Name:"));
        inputsPanel.add(billNameField);
        
        // Amount
        amountField = new JTextField();
        inputsPanel.add(new JLabel("Amount:"));
        inputsPanel.add(amountField);
        
        // Due Date
        dueDateChooser = new JDateChooser();
        dueDateChooser.setDate(new Date());
        dueDateChooser.setDateFormatString("yyyy-MM-dd");
        inputsPanel.add(new JLabel("Due Date:"));
        inputsPanel.add(dueDateChooser);
        
        // Payment Method
        String[] paymentMethods = {"Credit Card", "Debit Card", "Bank Transfer", "Cash"};
        paymentMethodCombo = new JComboBox<>(paymentMethods);
        inputsPanel.add(new JLabel("Payment Method:"));
        inputsPanel.add(paymentMethodCombo);
        
        // Notifications
        JPanel notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        emailNotificationCheck = new JCheckBox("Email");
        pushNotificationCheck = new JCheckBox("Push Notification");
        notificationPanel.add(emailNotificationCheck);
        notificationPanel.add(pushNotificationCheck);
        
        inputsPanel.add(new JLabel("Notification Method:"));
        inputsPanel.add(notificationPanel);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Form");
        JButton addButton = new JButton("Add Bill");
        
        clearButton.addActionListener(e -> clearForm());
        addButton.addActionListener(e -> addBill());
        
        buttonsPanel.add(clearButton);
        buttonsPanel.add(addButton);

        formPanel.add(inputsPanel, BorderLayout.CENTER);
        formPanel.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void createTablePanel() {
        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Upcoming Bills"));

        // Create table model with columns
        String[] columns = {"Bill Name", "Amount", "Due Date", "Payment Method", "Edit", "Delete", "ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4 || column == 5; // Only Edit and Delete columns are editable
            }
        };

        // Create table
        billsTable = new JTable(tableModel);
        billsTable.setRowHeight(30);
        
        // 设置按钮渲染器
        billsTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer("Edit"));
        billsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer("Delete"));
        
        // 隐藏ID列
        billsTable.getColumnModel().getColumn(6).setMinWidth(0);
        billsTable.getColumnModel().getColumn(6).setMaxWidth(0);
        billsTable.getColumnModel().getColumn(6).setWidth(0);
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(billsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add mouse listener to handle button clicks
        billsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = billsTable.getColumnModel().getColumnIndexAtX(e.getX());
                int row = e.getY() / billsTable.getRowHeight();
                
                if (row < billsTable.getRowCount() && row >= 0 && (column == 4 || column == 5)) {
                    // 获取该行对应的账单
                    String billId = (String) billsTable.getValueAt(row, 6); // 隐藏列存储ID
                    List<Bill> bills = billService.getAllBills();
                    Bill selectedBill = null;
                    
                    for (Bill bill : bills) {
                        if (bill.getId().equals(billId)) {
                            selectedBill = bill;
                            break;
                        }
                    }
                    
                    if (selectedBill != null) {
                        if (column == 4) { // Edit column
                            editBill(selectedBill);
                        } else if (column == 5) { // Delete column
                            deleteBill(selectedBill);
                        }
                    }
                }
            }
        });
    }

    private void addBillToTable(Bill bill) {
        Object[] rowData = {
            bill.getName(),
            "¥" + bill.getAmount().toString(),
            bill.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            bill.getPaymentMethod(),
            "Edit", // 这里只放字符串，由渲染器显示为按钮
            "Delete", // 这里只放字符串，由渲染器显示为按钮
            bill.getId() // 添加隐藏列存储ID
        };
        tableModel.addRow(rowData);
    }

    private JPanel createAlertCard(Bill bill) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        
        // 根据状态设置边框颜色
        Color borderColor = getColorForStatus(bill.getStatus());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(255, 255, 255));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel nameLabel = new JLabel(bill.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel statusLabel = new JLabel(bill.getStatusDescription());
        statusLabel.setForeground(borderColor);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountLabel = new JLabel("Amount: $" + bill.getAmount().toString());
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(statusLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(amountLabel);

        return card;
    }

    private Color getColorForStatus(String status) {
        switch (status) {
            case "Overdue":
                return new Color(220, 53, 69);  // Red
            case "Due Soon":
                return new Color(255, 193, 7);  // Yellow
            case "Upcoming":
                return new Color(255, 136, 0);  // Orange
            default:
                return new Color(40, 167, 69);  // Green
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

    private void addBill() {
        try {
            // Validate input
            String billName = billNameField.getText().trim();
            if (billName.isEmpty()) {
                showError("Please enter a bill name.");
                return;
            }

            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Please enter a valid amount greater than zero.");
                return;
            }

            Date selectedDate = dueDateChooser.getDate();
            if (selectedDate == null) {
                showError("Please select a due date.");
                return;
            }

            LocalDate dueDate = selectedDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            
            // 验证日期不能是过去的日期
            if (dueDate.isBefore(LocalDate.now())) {
                showError("Due date cannot be in the past.");
                return;
            }

            String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
            
            // Create and save new bill
            Bill bill = new Bill(
                "",  // ID will be generated by service
                billName,
                amount,
                dueDate,
                paymentMethod,
                emailNotificationCheck.isSelected(),
                pushNotificationCheck.isSelected()
            );
            
            billService.addBill(bill);
            
            // Refresh UI
            refreshData();
            
            // Clear form
            clearForm();

            // Show success message
            showSuccess("Bill added successfully!");

        } catch (NumberFormatException e) {
            showError("Please enter a valid amount.");
        }
    }

    private void editBill(Bill bill) {
        // 创建表单面板
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField nameField = new JTextField(bill.getName());
        JTextField amountField = new JTextField(bill.getAmount().toString());
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDate(Date.from(bill.getDueDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        
        String[] paymentMethods = {"Credit Card", "Debit Card", "Bank Transfer", "Cash"};
        JComboBox<String> paymentMethodCombo = new JComboBox<>(paymentMethods);
        paymentMethodCombo.setSelectedItem(bill.getPaymentMethod());
        
        JCheckBox emailCheck = new JCheckBox("Email Notification", bill.hasEmailNotification());
        JCheckBox pushCheck = new JCheckBox("Push Notification", bill.hasPushNotification());
        
        // 创建输入字段面板
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Bill Name:"), BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);
        
        JPanel amountPanel = new JPanel(new BorderLayout(5, 5));
        amountPanel.add(new JLabel("Amount:"), BorderLayout.NORTH);
        amountPanel.add(amountField, BorderLayout.CENTER);
        
        JPanel datePanel = new JPanel(new BorderLayout(5, 5));
        datePanel.add(new JLabel("Due Date:"), BorderLayout.NORTH);
        datePanel.add(dateChooser, BorderLayout.CENTER);
        
        JPanel methodPanel = new JPanel(new BorderLayout(5, 5));
        methodPanel.add(new JLabel("Payment Method:"), BorderLayout.NORTH);
        methodPanel.add(paymentMethodCombo, BorderLayout.CENTER);
        
        JPanel notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notificationPanel.add(emailCheck);
        notificationPanel.add(pushCheck);
        
        // 添加间距
        namePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        amountPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        datePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        methodPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // 添加到主面板
        panel.add(namePanel);
        panel.add(amountPanel);
        panel.add(datePanel);
        panel.add(methodPanel);
        panel.add(new JLabel("Notification:"));
        panel.add(notificationPanel);
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Edit Bill",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                // 验证输入
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showError("Please enter a bill name.");
                    return;
                }
                
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Please enter a valid amount greater than zero.");
                    return;
                }
                
                Date selectedDate = dateChooser.getDate();
                if (selectedDate == null) {
                    showError("Please select a due date.");
                    return;
                }
                
                LocalDate dueDate = selectedDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                
                String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
                
                // 创建更新后的账单
                Bill updatedBill = new Bill(
                    bill.getId(),
                    name,
                    amount,
                    dueDate,
                    paymentMethod,
                    emailCheck.isSelected(),
                    pushCheck.isSelected()
                );
                
                // 保存更新
                billService.updateBill(updatedBill);
                
                // 刷新界面
                refreshData();
                
                // 显示成功信息
                showSuccess("Bill updated successfully!");
                
            } catch (NumberFormatException e) {
                showError("Please enter a valid amount.");
            }
        }
    }

    private void deleteBill(Bill bill) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this bill?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            billService.deleteBill(bill.getId());
            refreshData();
            showSuccess("Bill deleted successfully!");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void createAlertsPanel() {
        alertsPanel = new JPanel();
        updateAlertsPanel();
    }

    // 自定义按钮渲染器
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String text) {
            setText(text);
            setFocusPainted(false);
            setBorderPainted(true);
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
} 