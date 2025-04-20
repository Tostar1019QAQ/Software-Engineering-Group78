package com.group78.financetracker.ui;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.service.AIService;
import com.group78.financetracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AIPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(AIPanel.class);
    
    private final AIService aiService;
    private final TransactionService transactionService;
    
    private JEditorPane resultPane;
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
        // Results display area with HTML support
        resultPane = new JEditorPane();
        resultPane.setEditable(false);
        resultPane.setContentType("text/html");
        
        // Configure HTML styling
        HTMLEditorKit kit = new HTMLEditorKit();
        resultPane.setEditorKit(kit);
        
        // Create and set up a stylesheet
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {font-family: 'Segoe UI', Arial, sans-serif; font-size: 16px; margin: 10px;}");
        styleSheet.addRule("h1 {color: #2962FF; font-size: 24px; margin-top: 20px; margin-bottom: 10px;}");
        styleSheet.addRule("h2 {color: #0277BD; font-size: 22px; margin-top: 15px; margin-bottom: 8px;}");
        styleSheet.addRule("h3 {color: #0288D1; font-size: 20px; font-weight: bold; margin-top: 12px; margin-bottom: 6px;}");
        styleSheet.addRule("p {margin-top: 8px; margin-bottom: 8px; font-size: 16px;}");
        styleSheet.addRule(".highlight {background-color: #E1F5FE; padding: 8px; border-left: 3px solid #0288D1; font-size: 16px;}");
        styleSheet.addRule(".expense {color: #D32F2F; font-size: 16px;}");
        styleSheet.addRule(".income {color: #388E3C; font-size: 16px;}");
        styleSheet.addRule("table {border-collapse: collapse; width: 100%; font-size: 16px;}");
        styleSheet.addRule("th {background-color: #E3F2FD; padding: 10px; text-align: left; border-bottom: 2px solid #90CAF9; font-size: 18px;}");
        styleSheet.addRule("td {padding: 8px; border-bottom: 1px solid #E0E0E0; font-size: 16px;}");
        styleSheet.addRule("tr:nth-child(even) {background-color: #F5F5F5;}");
        
        // Initialize with welcome message
        resultPane.setText("<html><body><h1>AI Financial Analysis</h1>" +
                "<p>Select an analysis type and click 'Start Analysis' to analyze your transaction data.</p>" +
                "</body></html>");
        
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
        JScrollPane scrollPane = new JScrollPane(resultPane);
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
        resultPane.setText("<html><body><h2>Processing your transaction data, please wait...</h2></body></html>");
        
        // Get selected analysis type
        int selectedIndex = analysisTypeComboBox.getSelectedIndex();
        List<Transaction> transactions = transactionService.getAllTransactions();
        
        // Execute analysis in background thread
        CompletableFuture.supplyAsync(() -> {
            try {
                if (selectedIndex == 0) {
                    // Smart financial analysis
                    String insights = aiService.generateFinancialInsights(transactions);
                    return formatAIResponse(insights);
                } else if (selectedIndex == 1) {
                    // Spending pattern analysis
                    return formatAnalysisResults(aiService.analyzeSpendingPattern(transactions));
                } else {
                    // Auto-categorization examples
                    return generateCategorizationExamples();
                }
            } catch (Exception ex) {
                logger.error("Analysis error: {}", ex.getMessage(), ex);
                return "<html><body><h1>Error</h1><p class='highlight'>Error during analysis: " 
                    + ex.getMessage() + "</p></body></html>";
            }
        }).thenAccept(result -> {
            // Update UI in EDT
            SwingUtilities.invokeLater(() -> {
                resultPane.setText(result);
                resultPane.setCaretPosition(0); // Scroll to top
                analyzeButton.setEnabled(true);
                statusLabel.setText("Analysis complete");
                statusLabel.setForeground(new Color(0, 153, 0));
            });
        });
    }
    
    /**
     * Format the AI response from DeepSeek into HTML
     */
    private String formatAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return "<html><body><p>No insights available</p></body></html>";
        }
        
        // Replace line breaks with HTML breaks
        String htmlText = aiResponse.replace("\n\n", "</p><p>")
                                  .replace("\n", "<br/>")
                                  .replace("**", ""); // Remove markdown bold markers
        
        // Add headers for better formatting (heuristic detection)
        if (!htmlText.contains("<h1>") && !htmlText.contains("<h2>")) {
            // Simple heuristic to identify potential headers
            htmlText = htmlText.replaceAll("(?m)^([A-Z][A-Za-z0-9 ]+:)<br/>", "<h2>$1</h2>");
            htmlText = htmlText.replaceAll("(?m)<p>([A-Z][A-Za-z0-9 ]+:)</p>", "<h2>$1</h2><p>");
        }
        
        // Format bullet points (if they exist)
        htmlText = htmlText.replace("• ", "&#8226; ");
        htmlText = htmlText.replace("- ", "&#8226; ");
        
        // Complete HTML structure
        return "<html><body><h1>Financial Insights</h1>" + 
               "<p>" + htmlText + "</p>" +
               "</body></html>";
    }
    
    /**
     * Format analysis results as HTML for better readability
     */
    private String formatAnalysisResults(java.util.Map<String, Object> analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>SPENDING PATTERN ANALYSIS</h1>");
        
        // Total spent
        sb.append("<p><b>Total Spent:</b> <span class='expense'>$").append(analysis.get("totalSpent")).append("</span></p>");
        
        // Daily average
        sb.append("<p><b>Daily Average:</b> <span>$").append(analysis.get("dailyAverage")).append("</span></p>");
        
        // Top category
        sb.append("<p><b>Top Spending Category:</b> <span class='highlight'>").append(analysis.get("topCategory")).append("</span></p>");
        
        // Category spending - Create a table
        sb.append("<h2>SPENDING BY CATEGORY</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Category</th><th>Amount</th></tr>");
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.math.BigDecimal> categoryTotals = 
            (java.util.Map<String, java.math.BigDecimal>) analysis.get("categoryTotals");
        
        if (categoryTotals != null) {
            categoryTotals.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> 
                    sb.append("<tr><td>").append(entry.getKey()).append("</td><td>$")
                      .append(entry.getValue()).append("</td></tr>")
                );
        }
        sb.append("</table>");
        
        // Statistics
        sb.append("<h2>STATISTICS</h2>");
        sb.append("<table>");
        sb.append("<tr><td>Mean:</td><td>$").append(String.format("%.2f", analysis.get("mean"))).append("</td></tr>");
        sb.append("<tr><td>Median:</td><td>$").append(String.format("%.2f", analysis.get("median"))).append("</td></tr>");
        sb.append("<tr><td>Standard Deviation:</td><td>$").append(String.format("%.2f", analysis.get("standardDeviation"))).append("</td></tr>");
        sb.append("</table>");
        
        sb.append("</body></html>");
        return sb.toString();
    }
    
    /**
     * Generate categorization examples with HTML formatting
     */
    private String generateCategorizationExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>AUTO-CATEGORIZATION EXAMPLES</h1>");
        sb.append("<p>Here are examples of transaction descriptions and their automated categorization:</p>");
        
        sb.append("<table>");
        sb.append("<tr><th>Description</th><th>Category</th></tr>");
        
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
            sb.append("<tr><td>\"").append(example[0]).append("\"</td>");
            sb.append("<td>").append(example[1]).append("</td></tr>");
        }
        
        sb.append("</table>");
        sb.append("<p class='highlight'>You can use the DeepSeek API for more accurate transaction categorization.</p>");
        sb.append("</body></html>");
        
        return sb.toString();
    }
} 