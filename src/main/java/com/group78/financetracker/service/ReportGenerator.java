package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates various reports based on transaction data
 */
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    private final AIService aiService;
    
    public ReportGenerator(AIService aiService) {
        this.aiService = aiService;
    }
    
    /**
     * Test for exception handling during report generation
     * 
     * @param type Report type (income-expense, cash-flow, savings)
     * @param transactions List of transactions to analyze
     * @return HTML formatted report
     */
    public String generateReport(String type, List<Transaction> transactions) {
        try {
            // Create output directory if it doesn't exist
            createOutputDirectory();
            
            // Basic financial calculations
            FinancialMetrics metrics = calculateFinancialMetrics(transactions);
            
            // Generate appropriate report based on type
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportContent;
            
            switch (type) {
                case "income-expense":
                    reportContent = generateIncomeExpenseReport(metrics, timestamp);
                    break;
                case "cash-flow":
                    reportContent = generateCashFlowReport(metrics, timestamp);
                    break;
                case "savings":
                    reportContent = generateSavingsReport(metrics, timestamp);
                    break;
                default:
                    return "<html><body><h1>Error</h1><p>Invalid report type: '" + type + "'. Please select a valid report type.</p></body></html>";
            }
            
            return reportContent;
        } catch (Exception e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            return "<html><body><h1>Error</h1><p>Failed to generate report: " 
                + e.getMessage() + "</p><p>Please try again or contact support if the issue persists.</p></body></html>";
        }
    }
    
    /**
     * Helper class to store financial metrics
     */
    private static class FinancialMetrics {
        public final BigDecimal totalIncome;
        public final BigDecimal totalExpenses;
        public final BigDecimal netSavings;
        public final BigDecimal savingsRate;
        public final Map<String, BigDecimal> categoryTotals;
        public final Map<YearMonth, Map<TransactionType, BigDecimal>> monthlyTotals;
        public final List<YearMonth> sortedMonths;
        
        public FinancialMetrics(
                BigDecimal totalIncome, 
                BigDecimal totalExpenses,
                BigDecimal netSavings,
                BigDecimal savingsRate,
                Map<String, BigDecimal> categoryTotals,
                Map<YearMonth, Map<TransactionType, BigDecimal>> monthlyTotals,
                List<YearMonth> sortedMonths) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netSavings = netSavings;
            this.savingsRate = savingsRate;
            this.categoryTotals = categoryTotals;
            this.monthlyTotals = monthlyTotals;
            this.sortedMonths = sortedMonths;
        }
    }
    
    /**
     * Calculate financial metrics from transaction data
     */
    private FinancialMetrics calculateFinancialMetrics(List<Transaction> transactions) {
        // Calculate total income and expenses
        BigDecimal totalIncome = transactions.stream()
            .filter(t -> t.getType() == TransactionType.INCOME)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpenses = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        
        // Calculate savings rate
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.multiply(new BigDecimal("100"))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }
        
        // Calculate expense categories
        Map<String, BigDecimal> categoryTotals = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        
        // Group by month and calculate monthly totals
        Map<YearMonth, List<Transaction>> transactionsByMonth = transactions.stream()
            .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDateTime())));
        
        Map<YearMonth, Map<TransactionType, BigDecimal>> monthlyTotals = new HashMap<>();
        
        for (Map.Entry<YearMonth, List<Transaction>> entry : transactionsByMonth.entrySet()) {
            YearMonth month = entry.getKey();
            List<Transaction> monthTransactions = entry.getValue();
            
            Map<TransactionType, BigDecimal> typeTotals = monthTransactions.stream()
                .collect(Collectors.groupingBy(
                    Transaction::getType,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
            
            monthlyTotals.put(month, typeTotals);
        }
        
        // Sort months chronologically
        List<YearMonth> sortedMonths = new ArrayList<>(monthlyTotals.keySet());
        sortedMonths.sort(YearMonth::compareTo);
        
        return new FinancialMetrics(
            totalIncome, totalExpenses, netSavings, savingsRate, 
            categoryTotals, monthlyTotals, sortedMonths);
    }
    
    /**
     * Generate income and expense report
     */
    private String generateIncomeExpenseReport(FinancialMetrics metrics, String timestamp) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Income and Expense Report</h1>");
        
        // Financial summary
        sb.append("<div style='background-color:#e8f5e9; padding:15px; border-radius:8px; margin:15px 0;'>");
        sb.append("<h2>Financial Summary</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Total Income</th><td>$").append(formatAmount(metrics.totalIncome)).append("</td></tr>");
        sb.append("<tr><th>Total Expenses</th><td>$").append(formatAmount(metrics.totalExpenses)).append("</td></tr>");
        sb.append("<tr><th>Net Savings</th><td>$").append(formatAmount(metrics.netSavings)).append("</td></tr>");
        sb.append("<tr><th>Savings Rate</th><td>").append(formatAmount(metrics.savingsRate)).append("%</td></tr>");
        sb.append("</table>");
        sb.append("</div>");
        
        // Monthly breakdown
        sb.append("<h2>Monthly Summary</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Month</th><th>Income</th><th>Expenses</th><th>Net</th></tr>");
        
        for (YearMonth month : metrics.sortedMonths) {
            BigDecimal income = metrics.monthlyTotals.get(month).getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
            BigDecimal expenses = metrics.monthlyTotals.get(month).getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expenses);
            
            sb.append("<tr>");
            sb.append("<td>").append(month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append("</td>");
            sb.append("<td style='color:green;'>$").append(formatAmount(income)).append("</td>");
            sb.append("<td style='color:red;'>$").append(formatAmount(expenses)).append("</td>");
            sb.append("<td>$").append(formatAmount(net)).append("</td>");
            sb.append("</tr>");
        }
        
        sb.append("</table>");
        
        // Category breakdown
        sb.append("<h2>Expense Categories</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Category</th><th>Amount</th><th>Percentage</th></tr>");
        
        // Sort categories by amount
        metrics.categoryTotals.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                BigDecimal percentage = entry.getValue().multiply(new BigDecimal("100"))
                    .divide(metrics.totalExpenses, 2, RoundingMode.HALF_UP);
                
                sb.append("<tr>");
                sb.append("<td>").append(entry.getKey()).append("</td>");
                sb.append("<td>$").append(formatAmount(entry.getValue())).append("</td>");
                sb.append("<td>").append(formatAmount(percentage)).append("%</td>");
                sb.append("</tr>");
            });
        
        sb.append("</table>");
        
        // Export notification
        File reportFile = new File("output/income_expense_report_" + timestamp + ".html");
        saveHtmlReport(sb.toString(), reportFile);
        
        sb.append("<h2>Exported Report</h2>");
        sb.append("<p>Report has been saved to: <a href='").append(reportFile.toURI()).append("'>")
          .append(reportFile.getName()).append("</a></p>");
        
        sb.append("</body></html>");
        return sb.toString();
    }
    
    /**
     * Generate cash flow forecast report
     */
    private String generateCashFlowReport(FinancialMetrics metrics, String timestamp) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Cash Flow Forecast (Next 6 Months)</h1>");
        
        // Check if we have enough data
        if (metrics.sortedMonths.size() < 3) {
            sb.append("<p class='highlight'>At least 3 months of transaction history is needed for accurate forecasting.</p>");
            sb.append("</body></html>");
            return sb.toString();
        }
        
        // Get recent months for forecasting
        int monthsToUse = Math.min(metrics.sortedMonths.size(), 6);
        List<YearMonth> recentMonths = metrics.sortedMonths.subList(metrics.sortedMonths.size() - monthsToUse, metrics.sortedMonths.size());
        
        // Calculate trends
        BigDecimal avgMonthlyIncome = calculateAverageMonthly(recentMonths, TransactionType.INCOME, metrics.monthlyTotals);
        BigDecimal avgMonthlyExpenses = calculateAverageMonthly(recentMonths, TransactionType.EXPENSE, metrics.monthlyTotals);
        BigDecimal avgMonthlySavings = avgMonthlyIncome.subtract(avgMonthlyExpenses);
        
        // Add Forecast Summary section
        sb.append("<div style='background-color:#e3f2fd; padding:15px; border-radius:8px; margin:15px 0;'>");
        sb.append("<h2>Forecast Summary</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Average Monthly Income</th><td>$").append(formatAmount(avgMonthlyIncome)).append("</td></tr>");
        sb.append("<tr><th>Average Monthly Expenses</th><td>$").append(formatAmount(avgMonthlyExpenses)).append("</td></tr>");
        sb.append("<tr><th>Average Monthly Savings</th><td>$").append(formatAmount(avgMonthlySavings)).append("</td></tr>");
        sb.append("</table>");
        sb.append("</div>");
        
        // Forecast next 6 months
        YearMonth lastMonth = metrics.sortedMonths.get(metrics.sortedMonths.size() - 1);
        List<YearMonth> forecastMonths = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            forecastMonths.add(lastMonth.plusMonths(i));
        }
        
        // Monthly forecast
        sb.append("<h2>6-Month Projection</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Month</th><th>Projected Income</th><th>Projected Expenses</th><th>Projected Net</th><th>Cumulative</th></tr>");
        
        BigDecimal currentSavings = metrics.netSavings;
        BigDecimal cumulativeSavings = currentSavings;
        
        for (YearMonth month : forecastMonths) {
            // Apply simple trend calculation
            BigDecimal projectedIncome = avgMonthlyIncome;
            BigDecimal projectedExpenses = avgMonthlyExpenses;
            BigDecimal projectedNet = projectedIncome.subtract(projectedExpenses);
            cumulativeSavings = cumulativeSavings.add(projectedNet);
            
            sb.append("<tr>");
            sb.append("<td>").append(month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append("</td>");
            sb.append("<td style='color:green;'>$").append(formatAmount(projectedIncome)).append("</td>");
            sb.append("<td style='color:red;'>$").append(formatAmount(projectedExpenses)).append("</td>");
            sb.append("<td>$").append(formatAmount(projectedNet)).append("</td>");
            sb.append("<td>$").append(formatAmount(cumulativeSavings)).append("</td>");
            sb.append("</tr>");
        }
        
        sb.append("</table>");
        
        // Financial Risk Assessment
        sb.append("<h2>Financial Risk Assessment</h2>");
        sb.append("<div style='padding:10px; border-radius:8px; margin:10px 0;");
        
        // Determine risk level
        String riskLevel;
        String riskColor;
        String riskDescription;
        
        if (avgMonthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            riskLevel = "High Risk";
            riskColor = "#ffebee"; // Light red
            riskDescription = "You are spending more than your income. This is not sustainable long-term.";
        } else if (metrics.savingsRate.compareTo(new BigDecimal("10")) < 0) {
            riskLevel = "Moderate Risk";
            riskColor = "#fff8e1"; // Light amber
            riskDescription = "Your savings rate is low. Consider reducing non-essential expenses.";
        } else if (metrics.savingsRate.compareTo(new BigDecimal("20")) < 0) {
            riskLevel = "Low Risk";
            riskColor = "#e8f5e9"; // Light green
            riskDescription = "Your finances are in good shape, but there's room for improvement.";
        } else {
            riskLevel = "Excellent";
            riskColor = "#e8f5e9"; // Light green
            riskDescription = "You have a healthy savings rate. Keep up the good work!";
        }
        
        sb.append("background-color:").append(riskColor).append(";'>");
        sb.append("<h3>").append(riskLevel).append("</h3>");
        sb.append("<p>").append(riskDescription).append("</p>");
        
        // Get AI insights
        sb.append("<h3>AI Insights</h3>");
        String aiInsight = "Based on your financial data, consider setting aside emergency funds equivalent to 3-6 months of expenses. "
                         + "It's also prudent to plan for future large expenses and create a detailed monthly budget. "
                         + "If your savings rate is below 20%, consider reviewing discretionary spending.";
        
        sb.append("<p>").append(aiInsight).append("</p>");
        sb.append("</div>");
        
        // Export notification
        File reportFile = new File("output/cash_flow_forecast_" + timestamp + ".html");
        saveHtmlReport(sb.toString(), reportFile);
        
        sb.append("<h2>Exported Report</h2>");
        sb.append("<p>Report has been saved to: <a href='").append(reportFile.toURI()).append("'>")
          .append(reportFile.getName()).append("</a></p>");
        
        sb.append("</body></html>");
        return sb.toString();
    }
    
    /**
     * Helper method to calculate average monthly amounts
     */
    private BigDecimal calculateAverageMonthly(List<YearMonth> months, TransactionType type, 
            Map<YearMonth, Map<TransactionType, BigDecimal>> monthlyTotals) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (YearMonth month : months) {
            Map<TransactionType, BigDecimal> typeTotals = monthlyTotals.get(month);
            if (typeTotals != null && typeTotals.containsKey(type)) {
                sum = sum.add(typeTotals.get(type));
                count++;
            }
        }
        
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        
        return sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generate savings recommendations report
     */
    private String generateSavingsReport(FinancialMetrics metrics, String timestamp) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Smart Savings Recommendations</h1>");
        
        // Financial overview
        sb.append("<div style='background-color:#e8f5e9; padding:15px; border-radius:8px; margin:15px 0;'>");
        sb.append("<h2>Financial Overview</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Total Income</th><td>$").append(formatAmount(metrics.totalIncome)).append("</td></tr>");
        sb.append("<tr><th>Total Expenses</th><td>$").append(formatAmount(metrics.totalExpenses)).append("</td></tr>");
        sb.append("<tr><th>Net Savings</th><td>$").append(formatAmount(metrics.netSavings)).append("</td></tr>");
        sb.append("<tr><th>Savings Rate</th><td>").append(formatAmount(metrics.savingsRate)).append("%</td></tr>");
        sb.append("</table>");
        sb.append("</div>");
        
        // Savings rate assessment
        sb.append("<h2>Savings Rate Assessment</h2>");
        if (metrics.savingsRate.compareTo(new BigDecimal("20")) >= 0) {
            sb.append("<div style='background-color:#e8f5e9; padding:15px; border-left:4px solid #4caf50; margin:15px 0;'>");
            sb.append("<h3 style='color:#2e7d32; margin-top:0;'>Excellent Savings Rate</h3>");
            sb.append("<p>Your savings rate of ").append(formatAmount(metrics.savingsRate)).append("% is excellent! ");
            sb.append("This puts you in a strong position for future financial goals.</p>");
            sb.append("</div>");
        } else if (metrics.savingsRate.compareTo(new BigDecimal("10")) >= 0) {
            sb.append("<div style='background-color:#fff8e1; padding:15px; border-left:4px solid #ffc107; margin:15px 0;'>");
            sb.append("<h3 style='color:#ff8f00; margin-top:0;'>Good Savings Rate</h3>");
            sb.append("<p>Your savings rate of ").append(formatAmount(metrics.savingsRate)).append("% is good, ");
            sb.append("but there's room for improvement. Aim for 20% for optimal financial health.</p>");
            sb.append("</div>");
        } else {
            sb.append("<div style='background-color:#ffebee; padding:15px; border-left:4px solid #f44336; margin:15px 0;'>");
            sb.append("<h3 style='color:#c62828; margin-top:0;'>Low Savings Rate</h3>");
            sb.append("<p>Your savings rate of ").append(formatAmount(metrics.savingsRate)).append("% is below ");
            sb.append("the recommended minimum of 10%. This limits your financial security and future options.</p>");
            sb.append("</div>");
        }
        
        // Top expense categories
        sb.append("<h2>Top Expense Categories</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Category</th><th>Amount</th><th>Percentage</th><th>Potential Savings</th></tr>");
        
        // Define category thresholds
        Map<String, BigDecimal> categoryThresholds = new HashMap<>();
        categoryThresholds.put("Housing", new BigDecimal("30"));
        categoryThresholds.put("Food", new BigDecimal("15"));
        categoryThresholds.put("Transportation", new BigDecimal("15"));
        categoryThresholds.put("Entertainment", new BigDecimal("10"));
        categoryThresholds.put("Shopping", new BigDecimal("10"));
        categoryThresholds.put("Utilities", new BigDecimal("10"));
        
        BigDecimal totalPotentialSavings = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : metrics.categoryTotals.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(6)
                .collect(Collectors.toList())) {
            
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal percentage = amount.multiply(new BigDecimal("100"))
                .divide(metrics.totalExpenses, 2, RoundingMode.HALF_UP);
            
            BigDecimal threshold = categoryThresholds.getOrDefault(category, new BigDecimal("10"));
            BigDecimal potentialSavings = BigDecimal.ZERO;
            
            // If over threshold, calculate potential savings
            if (percentage.compareTo(threshold) > 0) {
                BigDecimal overagePercentage = percentage.subtract(threshold);
                potentialSavings = metrics.totalExpenses.multiply(overagePercentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalPotentialSavings = totalPotentialSavings.add(potentialSavings);
            }
            
            sb.append("<tr>");
            sb.append("<td>").append(category).append("</td>");
            sb.append("<td>$").append(formatAmount(amount)).append("</td>");
            sb.append("<td>").append(formatAmount(percentage)).append("%</td>");
            
            if (potentialSavings.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("<td style='color:green;'>$").append(formatAmount(potentialSavings)).append("</td>");
            } else {
                sb.append("<td>$0.00</td>");
            }
            
            sb.append("</tr>");
        }
        
        sb.append("</table>");
        
        // Savings potential
        if (totalPotentialSavings.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<div style='background-color:#e8f5e9; padding:15px; border-radius:8px; margin:15px 0;'>");
            sb.append("<h2>Total Savings Potential</h2>");
            sb.append("<p>By optimizing your spending in the categories above, you could potentially save up to ");
            sb.append("<strong style='color:green;'>$").append(formatAmount(totalPotentialSavings)).append("</strong>.</p>");
            
            // Calculate improved savings rate
            BigDecimal improvedSavings = metrics.netSavings.add(totalPotentialSavings);
            BigDecimal improvedRate = improvedSavings.multiply(new BigDecimal("100"))
                .divide(metrics.totalIncome, 2, RoundingMode.HALF_UP);
            
            sb.append("<p>This would increase your savings rate from ")
              .append(formatAmount(metrics.savingsRate)).append("% to ")
              .append(formatAmount(improvedRate)).append("%.</p>");
            sb.append("</div>");
            
            // Long-term impact
            sb.append("<h2>Long-Term Impact of Improved Savings</h2>");
            sb.append("<table>");
            sb.append("<tr><th>Time Period</th><th>Current Path</th><th>With Improvements</th><th>Difference</th></tr>");
            
            BigDecimal monthlySavings = metrics.netSavings.compareTo(BigDecimal.ZERO) > 0 
                ? metrics.netSavings : BigDecimal.ZERO;
            BigDecimal monthlyImproved = monthlySavings.add(totalPotentialSavings);
            
            for (int years : new int[] {1, 3, 5, 10}) {
                BigDecimal currentTotal = monthlySavings.multiply(BigDecimal.valueOf(12 * years));
                BigDecimal improvedTotal = monthlyImproved.multiply(BigDecimal.valueOf(12 * years));
                BigDecimal difference = improvedTotal.subtract(currentTotal);
                
                sb.append("<tr>");
                sb.append("<td>").append(years).append(" Year").append(years > 1 ? "s" : "").append("</td>");
                sb.append("<td>$").append(formatAmount(currentTotal)).append("</td>");
                sb.append("<td>$").append(formatAmount(improvedTotal)).append("</td>");
                sb.append("<td style='color:green;'>$").append(formatAmount(difference)).append("</td>");
                sb.append("</tr>");
            }
            
            sb.append("</table>");
        }
        
        // Savings tips
        sb.append("<h2>General Savings Tips</h2>");
        sb.append("<ul>");
        sb.append("<li><strong>50/30/20 Rule:</strong> Aim to spend 50% of income on needs, 30% on wants, and save 20%.</li>");
        sb.append("<li><strong>Emergency Fund:</strong> Build an emergency fund covering 3-6 months of expenses.</li>");
        sb.append("<li><strong>Automate Savings:</strong> Set up automatic transfers to savings accounts on payday.</li>");
        sb.append("<li><strong>Review Subscriptions:</strong> Regularly audit and cancel unused subscriptions.</li>");
        sb.append("<li><strong>Meal Planning:</strong> Plan meals ahead to reduce food waste and dining out expenses.</li>");
        sb.append("</ul>");
        
        // Export notification
        File reportFile = new File("output/savings_recommendations_" + timestamp + ".html");
        saveHtmlReport(sb.toString(), reportFile);
        
        sb.append("<h2>Exported Report</h2>");
        sb.append("<p>Report has been saved to: <a href='").append(reportFile.toURI()).append("'>")
          .append(reportFile.getName()).append("</a></p>");
        
        sb.append("</body></html>");
        return sb.toString();
    }
    
    /**
     * Create output directory if it doesn't exist
     */
    private void createOutputDirectory() throws IOException {
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            Files.createDirectories(Paths.get("output"));
        }
    }
    
    /**
     * Save HTML report to file
     */
    private void saveHtmlReport(String html, File file) throws IOException {
        Files.write(file.toPath(), html.getBytes());
    }
    
    /**
     * Format amount for display
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
} 