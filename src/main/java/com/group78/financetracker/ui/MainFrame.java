package com.group78.financetracker.ui;

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

    public MainFrame() {
        initializeFrame();
        createPanels();
        createNavigationPanel();
        layoutComponents();
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
        budgetPanel = new BudgetPanel();
        billsPanel = new BillsPanel(cardLayout, contentPanel);
        importPanel = new ImportPanel(cardLayout, contentPanel);  // Use the new ImportPanel
        aiAnalysisPanel = new JPanel(); // TODO: Replace with AIAnalysisPanel
        
        // Add temporary labels to panels (except for Dashboard, Budget, Bills, and Import)
        addTemporaryLabel(aiAnalysisPanel, "AI Analysis");
        
        // Add all panels to card layout
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(budgetPanel, "Budget");
        contentPanel.add(billsPanel, "Bills");
        contentPanel.add(importPanel, "Import");
        contentPanel.add(aiAnalysisPanel, "AI Analysis");
    }

    private void addTemporaryLabel(JPanel panel, String text) {
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
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