package com.group78.financetracker.ui;

import com.group78.financetracker.service.AIService;
import com.group78.financetracker.service.ImportService;
import com.group78.financetracker.service.TransactionService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {
    private JPanel contentPanel;
    private JPanel dashboardPanel;
    private JPanel budgetPanel;
    private JPanel billsPanel;
    private JPanel importPanel;
    private JPanel aiAnalysisPanel;
    private JPanel navigationPanel;
    private CardLayout cardLayout;
    
    // 服务实例
    private AIService aiService;
    private ImportService importService;
    private TransactionService transactionService;

    public MainFrame() {
        // 初始化服务
        initializeServices();
        initializeFrame();
        createPanels();
        createNavigationPanel();
        layoutComponents();
    }
    
    private void initializeServices() {
        aiService = new AIService();
        importService = new ImportService();
        transactionService = new TransactionService(importService);
    }

    private void initializeFrame() {
        setTitle("Personal Finance Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void createPanels() {
        // Create card layout for switching panels
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        // Create all panels
        dashboardPanel = new DashboardPanel();
        budgetPanel = new BudgetPanel(cardLayout, contentPanel);
        billsPanel = new BillsPanel(cardLayout, contentPanel);
        importPanel = new ImportPanel(cardLayout, contentPanel);  // Use the new ImportPanel
        aiAnalysisPanel = new AIPanel(aiService, transactionService); // Use the new AIPanel
        
        // Add all panels to card layout
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(budgetPanel, "Budget");
        contentPanel.add(billsPanel, "Bills");
        contentPanel.add(importPanel, "Import");
        contentPanel.add(aiAnalysisPanel, "AI Analysis");
    }

    private void createNavigationPanel() {
        navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(74, 107, 255));
        navigationPanel.setPreferredSize(new Dimension(200, 0));
        
        // Add navigation buttons
        addNavigationButton("Dashboard", "📊", "Dashboard");
        addNavigationButton("Budget", "💰", "Budget");
        addNavigationButton("Bills", "⏰", "Bills");
        addNavigationButton("Import", "📥", "Import");
        addNavigationButton("AI Analysis", "🤖", "AI Analysis");
    }

    private void addNavigationButton(String text, String icon, String cardName) {
        JButton button = new JButton(icon + " " + text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setMargin(new Insets(10, 15, 10, 15));
        
        // Set button style
        button.setBackground(new Color(74, 107, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(90, 120, 255));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(74, 107, 255));
            }
        });
        
        // Add click event
        button.addActionListener(e -> cardLayout.show(contentPanel, cardName));
        
        navigationPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        navigationPanel.add(button);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        add(navigationPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }
} 