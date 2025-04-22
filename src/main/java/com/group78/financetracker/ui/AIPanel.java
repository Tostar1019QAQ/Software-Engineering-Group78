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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Panel for AI-related features, including transaction analysis, spending pattern analysis,
 * and a chat interface for financial questions.
 */
public class AIPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(AIPanel.class);
    
    private final AIService aiService;
    private final TransactionService transactionService;
    
    private JEditorPane resultPane;
    private JButton analyzeButton;
    private JButton sendButton;
    private JLabel statusLabel;
    private JTextField apiKeyField;
    private JPanel settingsPanel;
    private JComboBox<String> analysisTypeComboBox;
    private JTextArea userInputArea;
    private List<String> chatHistory;
    
    // Financial and bill-related keywords used to filter questions
    private static final List<String> FINANCE_KEYWORDS = Arrays.asList(
        "bill", "account", "payment", "expense", "income", 
        "budget", "finance", "financial", "money", "savings", 
        "debt", "invest", "spend", "transaction", "bank", 
        "credit", "loan", "pay", "balance", "asset", "liability",
        "interest", "mortgage", "investment", "salary", "earnings",
        "fees", "tax", "insurance", "retirement", "saving"
    );
    
    // Regular expression pattern to check if a question contains financial keywords
    private static final Pattern FINANCE_PATTERN = Pattern.compile(
        "(" + String.join("|", FINANCE_KEYWORDS) + ")", 
        Pattern.CASE_INSENSITIVE
    );
    
    public AIPanel(AIService aiService, TransactionService transactionService) {
        this.aiService = aiService;
        this.transactionService = transactionService;
        this.chatHistory = new ArrayList<>();
        
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
        styleSheet.addRule(".user-message {color: #2962FF; font-weight: bold; margin-top: 15px;}");
        styleSheet.addRule(".ai-message {color: #333; margin-bottom: 15px;}");
        styleSheet.addRule(".error-message {color: #D32F2F; font-weight: bold;}");
        styleSheet.addRule(".loading {font-style: italic; color: #0288D1;}");
        styleSheet.addRule(".dot-one, .dot-two, .dot-three {font-weight: bold;}");
        styleSheet.addRule("table {border-collapse: collapse; width: 100%; font-size: 16px;}");
        styleSheet.addRule("th {background-color: #E3F2FD; padding: 10px; text-align: left; border-bottom: 2px solid #90CAF9; font-size: 18px;}");
        styleSheet.addRule("td {padding: 8px; border-bottom: 1px solid #E0E0E0; font-size: 16px;}");
        styleSheet.addRule("tr:nth-child(even) {background-color: #F5F5F5;}");
        
        // Initialize with welcome message
        resultPane.setText("<html><body><h1>AI Financial Assistant</h1>" +
                "<p>Hello! I'm your AI financial assistant. You can ask me about:</p>" +
                "<ul>" +
                "<li>Bill analysis and recommendations</li>" +
                "<li>Spending patterns and habits</li>" +
                "<li>Budget management and savings plans</li>" +
                "<li>Personal finance strategies</li>" +
                "</ul>" +
                "<p class='highlight'>Note: I can only answer questions related to your finances.</p>" +
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
        
        // User input components
        userInputArea = new JTextArea(3, 20);
        userInputArea.setLineWrap(true);
        userInputArea.setWrapStyleWord(true);
        userInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Send button
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::handleSendButtonClick);
        
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
        JScrollPane resultScrollPane = new JScrollPane(resultPane);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "AI Assistant", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        ));
        resultScrollPane.setPreferredSize(new Dimension(800, 350));
        
        // Chat input area
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Ask a Question", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        ));
        
        JScrollPane inputScrollPane = new JScrollPane(userInputArea);
        chatInputPanel.add(inputScrollPane, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        
        // Create a panel to organize the chat area
        JPanel chatPanel = new JPanel(new BorderLayout(0, 10));
        chatPanel.add(resultScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        
        // Add to main panel
        add(controlPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        
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
    
    /**
     * Handle send button click event
     */
    private void handleSendButtonClick(ActionEvent e) {
        String userQuestion = userInputArea.getText().trim();
        if (userQuestion.isEmpty()) {
            return;
        }

        // Add user question to result pane
        appendToResultPane("<p class='user-message'>You: " + userQuestion + "</p>");
        userInputArea.setText("");
        
        // Check if we need special handling for bill or budget related questions
        boolean needsVisualization = isBillOrBudgetQuestion(userQuestion);
        
        // Create context builder
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("You are a financial assistant AI. You can only answer questions related to finance, budgeting, investments, or user's transaction data. ");
        contextBuilder.append("If a question is not related to finance or user's financial data, please politely explain that you can only assist with finance-related matters and decline to answer.\n\n");
        
        // Create mapping between file types and keywords
        Map<String, List<String>> fileTypeKeywords = new HashMap<>();
        fileTypeKeywords.put("budget.csv", Arrays.asList("budget", "remaining budget", "total budget"));
        fileTypeKeywords.put("bills.csv", Arrays.asList("bill", "bills", "payment", "pay", "due date"));
        fileTypeKeywords.put("default_transactions.csv", Arrays.asList("transaction", "spending", "history"));
        
        // Determine which files to include
        Set<String> filesToInclude = new HashSet<>();
        String lowerCaseQuestion = userQuestion.toLowerCase();
        
        // Always include budget and bills files by default
        filesToInclude.add("budget.csv");
        filesToInclude.add("bills.csv");
        
        // Include transaction data file based on question keywords
        for (Map.Entry<String, List<String>> entry : fileTypeKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerCaseQuestion.contains(keyword.toLowerCase())) {
                    filesToInclude.add(entry.getKey());
                    break;
                }
            }
        }
        
        // Add data files as context, now only including files relevant to the question
        try {
            File dataDir = new File("data");
            if (dataDir.exists() && dataDir.isDirectory()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (filesToInclude.contains(file.getName()) || 
                                             filesToInclude.isEmpty())) { // If no matching keywords, include all files
                            contextBuilder.append("Contents of file ").append(file.getName()).append(":\n");
                            
                            // Limit the number of lines read for large files
                            List<String> lines = Files.readAllLines(file.toPath());
                            int maxLines = file.getName().equals("default_transactions.csv") ? 30 : lines.size(); // Limit large files
                            
                            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                                contextBuilder.append(lines.get(i)).append("\n");
                            }
                            
                            if (lines.size() > maxLines) {
                                contextBuilder.append("... (file contains more data, omitted) ...\n");
                            }
                            
                            contextBuilder.append("\n");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Error reading data files: " + ex.getMessage());
        }
        
        // Add user question to context
        contextBuilder.append("User question: ").append(userQuestion);
        
        // If special handling for bill and budget data is needed, add relevant hints
        if (needsVisualization) {
            contextBuilder.append("\n\nPlease provide a concise analysis of bills and budget, as we've already generated visualization tables for the user.");
        }
        
        String context = contextBuilder.toString();
        
        // Show loading indicator
        appendToResultPane("<p class='loading'>AI thinking<span class='dot-one'>.</span><span class='dot-two'>.</span><span class='dot-three'>.</span></p>");
        
        // Create new thread to handle API call
        new Thread(() -> {
            String response;
            try {
                // Use AIService to generate response
                response = aiService.generateFinancialInsights(context);
                
                // Update UI in EDT
                SwingUtilities.invokeLater(() -> {
                    // Remove loading message
                    String currentContent = resultPane.getText();
                    String newContent = currentContent.replaceAll(
                        "<p class=['\"]loading['\"]>AI thinking<span class=['\"]dot-one['\"]>\\.</span><span class=['\"]dot-two['\"]>\\.</span><span class=['\"]dot-three['\"]>\\.</span></p>", 
                        ""
                    );
                    resultPane.setText(newContent);
                    
                    // If this is a bill or budget related question, add visualization content
                    if (needsVisualization) {
                        String visualization = createBillBudgetVisualization();
                        appendToResultPane("<div class='visualization'>" + visualization + "</div>");
                    }
                    
                    // Add AI response to result pane
                    appendToResultPane("<p class='ai-message'>AI: " + response + "</p>");
                });
            } catch (Exception ex) {
                logger.error("Error generating AI response: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    // Remove loading message
                    String currentContent = resultPane.getText();
                    String newContent = currentContent.replaceAll(
                        "<p class=['\"]loading['\"]>AI thinking<span class=['\"]dot-one['\"]>\\.</span><span class=['\"]dot-two['\"]>\\.</span><span class=['\"]dot-three['\"]>\\.</span></p>", 
                        ""
                    );
                    resultPane.setText(newContent);
                    
                    // Add error message
                    appendToResultPane("<p class='error-message'>AI: Error processing your request. Please try again later.</p>");
                });
            }
        }).start();
    }
    
    /**
     * Check if the question is related to finance
     */
    private boolean isFinanceRelatedQuestion(String question) {
        return FINANCE_PATTERN.matcher(question).find();
    }
    
    /**
     * Detect if the question is related to bills or budget
     */
    private boolean isBillOrBudgetQuestion(String question) {
        String lowerCase = question.toLowerCase();
        return lowerCase.contains("bill") || lowerCase.contains("budget") || 
               lowerCase.contains("payment") || lowerCase.contains("due") || 
               lowerCase.contains("remaining");
    }
    
    /**
     * Read contents of a specific data file from the data directory, returns empty list if file doesn't exist
     */
    private List<String> readDataFile(String fileName) {
        try {
            File file = new File("data/" + fileName);
            if (file.exists()) {
                return Files.readAllLines(file.toPath());
            }
        } catch (IOException e) {
            logger.error("Error reading file {}: {}", fileName, e.getMessage());
        }
        return new ArrayList<>();
    }
    
    /**
     * Create visualization for bill and budget related questions
     */
    private String createBillBudgetVisualization() {
        StringBuilder analysisResult = new StringBuilder();
        analysisResult.append("<h2>Bills and Budget Analysis</h2>");
        
        // Read bill data
        List<String> billLines = readDataFile("bills.csv");
        if (!billLines.isEmpty()) {
            analysisResult.append("<h3>Bill Status</h3>");
            analysisResult.append("<table border='1' style='width:100%; border-collapse:collapse;'>");
            analysisResult.append("<tr style='background-color:#f2f2f2;'><th>Name</th><th>Amount</th><th>Due Date</th><th>Status</th></tr>");
            
            int overdue = 0;
            int dueSoon = 0;
            int upcoming = 0;
            int total = 0;
            
            // Skip header line
            for (int i = 1; i < billLines.size(); i++) {
                String line = billLines.get(i);
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String name = parts[0];
                    String amount = parts[1];
                    String dueDate = parts[2];
                    String status = parts[3];
                    
                    String rowStyle = "";
                    if (status.equalsIgnoreCase("Overdue")) {
                        rowStyle = " style='background-color:#ffdddd;'";
                        overdue++;
                    } else if (status.equalsIgnoreCase("Due Soon")) {
                        rowStyle = " style='background-color:#ffffcc;'";
                        dueSoon++;
                    } else if (status.equalsIgnoreCase("Upcoming")) {
                        upcoming++;
                    }
                    
                    analysisResult.append("<tr").append(rowStyle).append(">");
                    analysisResult.append("<td>").append(name).append("</td>");
                    analysisResult.append("<td>").append(amount).append("</td>");
                    analysisResult.append("<td>").append(dueDate).append("</td>");
                    analysisResult.append("<td>").append(status).append("</td>");
                    analysisResult.append("</tr>");
                    
                    total++;
                }
            }
            analysisResult.append("</table>");
            
            // Add bill summary
            if (total > 0) {
                analysisResult.append("<p><b>Bill Summary:</b> Total of ").append(total).append(" bills, ");
                analysisResult.append("<span style='color:red;'>").append(overdue).append(" overdue</span>, ");
                analysisResult.append("<span style='color:orange;'>").append(dueSoon).append(" due soon</span>, ");
                analysisResult.append("<span style='color:green;'>").append(upcoming).append(" upcoming</span></p>");
            }
        }
        
        // Read budget data
        List<String> budgetLines = readDataFile("budget.csv");
        if (!budgetLines.isEmpty()) {
            analysisResult.append("<h3>Budget Information</h3>");
            analysisResult.append("<table border='1' style='width:100%; border-collapse:collapse;'>");
            analysisResult.append("<tr style='background-color:#f2f2f2;'><th>Total Budget</th><th>Start Date</th><th>End Date</th><th>Remaining Budget</th></tr>");
            
            String totalBudget = "";
            String startDate = "";
            String endDate = "";
            String remainingBudget = "";
            
            // Parse budget data
            for (String line : budgetLines) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    if (parts[0].equals("totalBudget")) {
                        totalBudget = parts[1];
                    } else if (parts[0].equals("startDate")) {
                        startDate = parts[1];
                    } else if (parts[0].equals("endDate")) {
                        endDate = parts[1];
                    } else if (parts[0].equals("remainingBudget")) {
                        remainingBudget = parts[1];
                    }
                }
            }
            
            analysisResult.append("<tr>");
            analysisResult.append("<td>").append(totalBudget).append("</td>");
            analysisResult.append("<td>").append(startDate).append("</td>");
            analysisResult.append("<td>").append(endDate).append("</td>");
            
            // Set color based on remaining budget percentage
            String remainingStyle = "";
            try {
                double total = Double.parseDouble(totalBudget);
                double remaining = Double.parseDouble(remainingBudget);
                double percentage = (remaining / total) * 100;
                
                if (percentage < 20) {
                    remainingStyle = " style='color:red;font-weight:bold;'";
                } else if (percentage < 50) {
                    remainingStyle = " style='color:orange;font-weight:bold;'";
                } else {
                    remainingStyle = " style='color:green;font-weight:bold;'";
                }
            } catch (NumberFormatException e) {
                // If parsing fails, don't set special style
            }
            
            analysisResult.append("<td").append(remainingStyle).append(">").append(remainingBudget).append("</td>");
            analysisResult.append("</tr>");
            analysisResult.append("</table>");
        }
        
        return analysisResult.toString();
    }
    
    /**
     * Append text to the result pane
     */
    private void appendToResultPane(String htmlContent) {
        // Get current content
        String currentContent = resultPane.getText();
        
        // Extract content from HTML tags
        String bodyContent = currentContent.substring(
            currentContent.indexOf("<body>") + 6, 
            currentContent.indexOf("</body>")
        );
        
        // Append new content
        String newContent = "<html><body>" + bodyContent + htmlContent + "</body></html>";
        resultPane.setText(newContent);
        
        // Scroll to bottom
        resultPane.setCaretPosition(resultPane.getDocument().getLength());
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