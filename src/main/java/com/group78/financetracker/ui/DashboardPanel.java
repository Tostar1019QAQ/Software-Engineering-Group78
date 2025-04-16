package com.group78.financetracker.ui;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private JPanel summaryPanel;
    private JPanel chartPanel;
    private JPanel actionPanel;

    public DashboardPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));

        createComponents();
        layoutComponents();
    }

    private void createComponents() {
        // Create summary panel
        summaryPanel = createSummaryPanel();
        
        // Create chart panel
        chartPanel = createChartPanel();
        
        // Create action button panel
        actionPanel = createActionPanel();
    }

    private void layoutComponents() {
        // Add components to dashboard
        add(summaryPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);

        // Add three summary cards
        panel.add(createSummaryCard("Monthly Expenses", "$4,250"));
        panel.add(createSummaryCard("Remaining Budget", "$8,750"));
        panel.add(createSummaryCard("Pending Bills", "3"));

        return panel;
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
        valueLabel.setFont(valueLabel.getFont().deriveFont(24f));
        valueLabel.setForeground(new Color(74, 107, 255));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);

        return card;
    }

    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);

        // Create two chart containers
        panel.add(createChartContainer("Last 7 Days Spending"));
        panel.add(createChartContainer("Expense Distribution"));

        return panel;
    }

    private JPanel createChartContainer(String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(74, 107, 255));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        // Chart placeholder
        JPanel chartPlaceholder = new JPanel();
        chartPlaceholder.setPreferredSize(new Dimension(0, 300));
        chartPlaceholder.setBackground(new Color(245, 247, 250));

        container.add(titleLabel, BorderLayout.NORTH);
        container.add(chartPlaceholder, BorderLayout.CENTER);

        return container;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);

        // Add AI analysis button
        JButton aiButton = createActionButton("AI Smart Analysis", "🤖");
        aiButton.setBackground(new Color(74, 107, 255));
        aiButton.setForeground(Color.WHITE);
        
        panel.add(aiButton);
        panel.add(Box.createHorizontalStrut(1)); // Placeholder

        return panel;
    }

    private JButton createActionButton(String text, String icon) {
        JButton button = new JButton(icon + " " + text);
        button.setFont(button.getFont().deriveFont(14f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
} 