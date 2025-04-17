package com.group78.financetracker.ui;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.service.AIService;
import com.group78.financetracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AIPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(AIPanel.class);
    
    private final AIService aiService;
    private final TransactionService transactionService;
    
    private JTextArea resultTextArea;
    private JButton analyzeButton;
    private JLabel statusLabel;
    private JTextField apiKeyField;
    private JPanel settingsPanel;
    private JComboBox<String> analysisTypeComboBox;
    
    public AIPanel(AIService aiService, TransactionService transactionService) {
        this.aiService = aiService;
        this.transactionService = transactionService;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        initComponents();
        layoutComponents();
    }
    
    private void initComponents() {
        // Results display area
        resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Analysis type selection
        analysisTypeComboBox = new JComboBox<>(new String[]{
            "Smart Financial Analysis & Recommendations",
            "Spending Pattern Analysis",
            "Auto-categorization Examples"
        });
        
        // Analyze button
        analyzeButton = new JButton("Start Analysis");
        analyzeButton.addActionListener(this::handleAnalyzeButtonClick);
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.GRAY);
        
        // API settings area
        apiKeyField = new JTextField(20);
        apiKeyField.setText("sk-5437453fe5544771b8ca5196cfac9bc5");  // Pre-populate with the provided API key
        JButton saveApiKeyButton = new JButton("Save API Key");
        saveApiKeyButton.addActionListener(e -> {
            aiService.setApiKey(apiKeyField.getText().trim());
            JOptionPane.showMessageDialog(this, "API key updated", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Settings panel
        settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        settingsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "DeepSeek API Settings", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        ));
        
        JLabel apiKeyLabel = new JLabel("API Key:");
        settingsPanel.add(apiKeyLabel);
        settingsPanel.add(apiKeyField);
        settingsPanel.add(saveApiKeyButton);
    }
    
    private void layoutComponents() {
        // Top control area
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel analysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        analysisPanel.add(new JLabel("Analysis Type:"));
        analysisPanel.add(analysisTypeComboBox);
        analysisPanel.add(analyzeButton);
        
        controlPanel.add(analysisPanel, BorderLayout.NORTH);
        controlPanel.add(settingsPanel, BorderLayout.CENTER);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // Results area
        JScrollPane scrollPane = new JScrollPane(resultTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Analysis Results", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        ));
        scrollPane.setPreferredSize(new Dimension(800, 400));
        
        // Add to main panel
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "About AI Analysis", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        ));
        
        JTextArea infoTextArea = new JTextArea(
            "This feature uses DeepSeek AI to analyze your transaction data and provide personalized financial insights.\n" + 
            "For advanced features, configure your DeepSeek API key. Basic analysis will be used if no API key is provided.\n" +
            "DeepSeek AI helps you understand spending patterns, discover potential savings, and provides targeted financial advice."
        );
        infoTextArea.setEditable(false);
        infoTextArea.setBackground(UIManager.getColor("Panel.background"));
        infoTextArea.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        
        infoPanel.add(infoTextArea, BorderLayout.CENTER);
        
        add(infoPanel, BorderLayout.SOUTH);
    }
    
    private void handleAnalyzeButtonClick(ActionEvent e) {
        if (transactionService.getAllTransactions().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No transaction data to analyze. Please import or add transactions first.", 
                "No Data", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Update UI status
        analyzeButton.setEnabled(false);
        statusLabel.setText("Analyzing...");
        statusLabel.setForeground(new Color(0, 102, 204));
        resultTextArea.setText("Processing your transaction data, please wait...");
        
        // Get selected analysis type
        int selectedIndex = analysisTypeComboBox.getSelectedIndex();
        List<Transaction> transactions = transactionService.getAllTransactions();
        
        // Execute analysis in background thread
        CompletableFuture.supplyAsync(() -> {
            try {
                if (selectedIndex == 0) {
                    // Smart financial analysis
                    return aiService.generateFinancialInsights(transactions);
                } else if (selectedIndex == 1) {
                    // Spending pattern analysis
                    return formatAnalysisResults(aiService.analyzeSpendingPattern(transactions));
                } else {
                    // Auto-categorization examples
                    return generateCategorizationExamples();
                }
            } catch (Exception ex) {
                logger.error("Analysis error: {}", ex.getMessage(), ex);
                return "Error during analysis: " + ex.getMessage();
            }
        }).thenAccept(result -> {
            // Update UI in EDT
            SwingUtilities.invokeLater(() -> {
                resultTextArea.setText(result);
                analyzeButton.setEnabled(true);
                statusLabel.setText("Analysis complete");
                statusLabel.setForeground(new Color(0, 153, 0));
            });
        });
    }
    
    /**
     * Format analysis results as readable text
     */
    private String formatAnalysisResults(java.util.Map<String, Object> analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== SPENDING PATTERN ANALYSIS ====\n\n");
        
        // Total spent
        sb.append("Total Spent: $").append(analysis.get("totalSpent")).append("\n\n");
        
        // Daily average
        sb.append("Daily Average: $").append(analysis.get("dailyAverage")).append("\n\n");
        
        // Top category
        sb.append("Top Spending Category: ").append(analysis.get("topCategory")).append("\n\n");
        
        // Category spending
        sb.append("=== SPENDING BY CATEGORY ===\n");
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.math.BigDecimal> categoryTotals = 
            (java.util.Map<String, java.math.BigDecimal>) analysis.get("categoryTotals");
        
        if (categoryTotals != null) {
            categoryTotals.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> 
                    sb.append(entry.getKey()).append(": $").append(entry.getValue()).append("\n")
                );
        }
        
        // Statistics
        sb.append("\n=== STATISTICS ===\n");
        sb.append("Mean: $").append(String.format("%.2f", analysis.get("mean"))).append("\n");
        sb.append("Median: $").append(String.format("%.2f", analysis.get("median"))).append("\n");
        sb.append("Standard Deviation: $").append(String.format("%.2f", analysis.get("standardDeviation"))).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Generate categorization examples
     */
    private String generateCategorizationExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append("==== AUTO-CATEGORIZATION EXAMPLES ====\n\n");
        sb.append("Here are examples of transaction descriptions and their automated categorization:\n\n");
        
        String[][] examples = {
            {"Lunch at McDonald's", aiService.categorizeTransaction("Lunch at McDonald's")},
            {"Movie theater tickets", aiService.categorizeTransaction("Movie theater tickets")},
            {"Subway pass recharge", aiService.categorizeTransaction("Subway pass recharge")},
            {"Online shopping - electronics", aiService.categorizeTransaction("Online shopping - electronics")},
            {"Mobile phone bill payment", aiService.categorizeTransaction("Mobile phone bill payment")},
            {"Monthly rent payment", aiService.categorizeTransaction("Monthly rent payment")},
            {"University tuition fee", aiService.categorizeTransaction("University tuition fee")},
            {"Pharmacy purchase", aiService.categorizeTransaction("Pharmacy purchase")},
            {"Utility bills", aiService.categorizeTransaction("Utility bills")},
            {"Gym membership fee", aiService.categorizeTransaction("Gym membership fee")}
        };
        
        for (String[] example : examples) {
            sb.append("Description: \"").append(example[0]).append("\"\n");
            sb.append("Category: ").append(example[1]).append("\n\n");
        }
        
        sb.append("You can use the DeepSeek API for more accurate transaction categorization.");
        
        return sb.toString();
    }
} 