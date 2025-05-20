package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    // DeepSeek API configuration
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static String DEEPSEEK_API_KEY = "sk-5437453fe5544771b8ca5196cfac9bc5"; // Using the provided API key
    private static final String DEEPSEEK_MODEL = "deepseek-chat";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Predefined expense categories
    private static final Set<String> PREDEFINED_CATEGORIES = new HashSet<>(Arrays.asList(
        "Food", "Transport", "Shopping", "Entertainment", "Housing", "Healthcare", "Education", "Communication", "Others"
    ));

    // Keyword-based category mapping
    private static final Map<String, String> KEYWORD_CATEGORY_MAP = new HashMap<String, String>() {{
        put("supermarket", "Shopping");
        put("restaurant", "Food");
        put("subway", "Transport");
        put("bus", "Transport");
        put("taxi", "Transport");
        put("movie", "Entertainment");
        put("rent", "Housing");
        put("utility", "Housing");
        put("hospital", "Healthcare");
        put("pharmacy", "Healthcare");
        put("tuition", "Education");
        put("bookstore", "Education");
        put("phone", "Communication");
        put("internet", "Communication");
    }};

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        logger.info("AIService initialized with DeepSeek API");
    }

    /**
     * Automatically categorizes a transaction based on its description
     */
    public String categorizeTransaction(String description) {
        String lowerDesc = description.toLowerCase();
        
        // First try keyword-based matching
        for (Map.Entry<String, String> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            if (lowerDesc.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // If keyword matching fails, use simple heuristic matching instead of calling API
        if (lowerDesc.contains("gas") || lowerDesc.contains("uber") || lowerDesc.contains("bus") || lowerDesc.contains("train")) {
            return "Transport";
        } else if (lowerDesc.contains("grocery") || lowerDesc.contains("restaurant") || lowerDesc.contains("cafe")) {
            return "Food";
        } else if (lowerDesc.contains("rent") || lowerDesc.contains("mortgage")) {
            return "Housing";
        } else if (lowerDesc.contains("movie") || lowerDesc.contains("game") || lowerDesc.contains("netflix")) {
            return "Entertainment";
        } else if (lowerDesc.contains("doctor") || lowerDesc.contains("hospital") || lowerDesc.contains("medicine")) {
            return "Healthcare";
        } else if (lowerDesc.contains("school") || lowerDesc.contains("book") || lowerDesc.contains("course")) {
            return "Education";
        } else {
            return "Others"; // Default category
        }
    }
    
    /**
     * Analyzes spending patterns
     */
    public Map<String, Object> analyzeSpendingPattern(List<Transaction> transactions) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (transactions.isEmpty()) {
            return analysis;
        }

        // Calculate total spending by category
        Map<String, BigDecimal> categoryTotals = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        
        // Find category with highest spending
        String topCategory = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
        
        // Calculate daily average spending
        BigDecimal totalSpent = categoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long daysBetween = ChronoUnit.DAYS.between(
            transactions.get(0).getDateTime(),
            transactions.get(transactions.size() - 1).getDateTime()
        ) + 1;
        BigDecimal dailyAverage = totalSpent.divide(BigDecimal.valueOf(daysBetween), 2, BigDecimal.ROUND_HALF_UP);

        // Calculate spending trends
        double[] amounts = transactions.stream()
            .mapToDouble(t -> t.getAmount().doubleValue())
            .toArray();
        DescriptiveStatistics stats = new DescriptiveStatistics(amounts);
        
        analysis.put("categoryTotals", categoryTotals);
        analysis.put("topCategory", topCategory);
        analysis.put("dailyAverage", dailyAverage);
        analysis.put("totalSpent", totalSpent);
        analysis.put("mean", stats.getMean());
        analysis.put("median", stats.getPercentile(50));
        analysis.put("standardDeviation", stats.getStandardDeviation());

        return analysis;
    }

    /**
     * Generates personalized financial insights
     */
    public String generateFinancialInsights(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "Not enough transaction data for analysis.";
        }
        
        // Use basic analysis instead of DeepSeek API call
        Map<String, Object> analysis = analyzeSpendingPattern(transactions);
        List<String> suggestions = generateBasicSavingsSuggestions(analysis);
        
        StringBuilder insights = new StringBuilder();
        insights.append("Financial Analysis Report:\n\n");
        
        // Add basic financial metrics
        BigDecimal totalSpent = (BigDecimal) analysis.get("totalSpent");
        BigDecimal dailyAverage = (BigDecimal) analysis.get("dailyAverage");
        String topCategory = (String) analysis.get("topCategory");
        
        insights.append("Your highest spending is in the ").append(topCategory).append(" category.\n");
        insights.append("Your average daily spending is $").append(dailyAverage.setScale(2, RoundingMode.HALF_UP)).append(".\n\n");
        
        // Add recommendations
        insights.append("Recommendations:\n");
        for (String suggestion : suggestions) {
            insights.append("- ").append(suggestion).append("\n");
        }
        
        return insights.toString();
    }
    
    /**
     * Generate financial insights from a custom prompt
     */
    public String generateFinancialInsights(String customPrompt) {
        // Provide fixed financial insights instead of calling API
        StringBuilder insights = new StringBuilder();
        
        insights.append("Financial Analysis:\n\n");
        insights.append("Based on your financial data, we recommend considering the following improvements:\n");
        insights.append("1. Create an emergency fund: Build a reserve equivalent to 3-6 months of daily expenses.\n");
        insights.append("2. Create a detailed budget: Track monthly spending to identify areas for reduction.\n");
        insights.append("3. Optimize spending structure: Try to allocate at least 20% of income to savings and investments.\n");
        insights.append("4. Review fixed expenses: Regularly check subscription services and bills, cancel unnecessary expenses.\n");
        insights.append("5. Set up automatic savings: Automatically transfer a portion of funds to a savings account on each payday.\n");
        
        return insights.toString();
    }

    /**
     * Generates basic savings suggestions (fallback option)
     */
    public List<String> generateBasicSavingsSuggestions(Map<String, Object> analysis) {
        List<String> suggestions = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> categoryTotals = (Map<String, BigDecimal>) analysis.get("categoryTotals");
        String topCategory = (String) analysis.get("topCategory");
        BigDecimal dailyAverage = (BigDecimal) analysis.get("dailyAverage");
        
        // Suggestions based on highest spending category
        if (topCategory != null && !topCategory.isEmpty()) {
            suggestions.add("Your highest spending is in " + topCategory + " category. Consider reviewing these expenses.");
        }
        
        // Suggestions based on daily average spending
        if (dailyAverage.compareTo(new BigDecimal("200")) > 0) {
            suggestions.add("Your daily spending is relatively high. Consider creating a detailed budget plan.");
        }
        
        // Suggestions based on category distribution
        if (categoryTotals != null) {
            BigDecimal foodExpense = categoryTotals.getOrDefault("Food", BigDecimal.ZERO);
            BigDecimal totalExpense = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (foodExpense.compareTo(totalExpense.multiply(new BigDecimal("0.3"))) > 0) {
                suggestions.add("Food expenses are high relative to total spending. Consider reducing dining out frequency.");
            }
        }
        
        // General suggestions
        suggestions.add("Consider using automatic expense tracking to maintain timely and accurate records.");
        suggestions.add("Regularly review your bills to ensure there are no duplicate or erroneous charges.");
        
        return suggestions;
    }
    
    /**
     * Configure API key
     */
    public void setApiKey(String apiKey) {
        // In a real application, the API key should be stored securely
        // This is just for demonstration
        DEEPSEEK_API_KEY = apiKey;
        logger.info("DeepSeek API key updated");
    }

    /**
     * Calculates financial health score based on transaction data
     */
    public Map<String, Object> calculateFinancialHealthScore(List<Transaction> transactions) {
        Map<String, Object> result = new HashMap<>();
        
        // Calculate income and expenses
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
        
        // Calculate expense-to-income ratio
        BigDecimal expenseToIncomeRatio = BigDecimal.ONE; // Default to 1 (100%)
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            expenseToIncomeRatio = totalExpenses.divide(totalIncome, 2, RoundingMode.HALF_UP);
        }
        
        // Basic score calculation based on savings rate
        int savingsComponent = 0;
        if (savingsRate.compareTo(BigDecimal.ZERO) > 0) {
            savingsComponent = Math.min(50, savingsRate.intValue());
        }
        
        // Score based on expense ratio (lower is better)
        int expenseComponent = 0;
        if (expenseToIncomeRatio.compareTo(BigDecimal.ONE) <= 0) {
            expenseComponent = (int)(50 * (1 - expenseToIncomeRatio.doubleValue()));
        }
        
        // Final score combines both components
        int finalScore = savingsComponent + expenseComponent;
        
        // Determine category based on score
        String category;
        if (finalScore >= 80) {
            category = "Excellent";
        } else if (finalScore >= 60) {
            category = "Good";
        } else if (finalScore >= 40) {
            category = "Fair";
        } else if (finalScore >= 20) {
            category = "Needs Improvement";
        } else {
            category = "Critical";
        }
        
        // Generate recommendations based on financial metrics
        List<String> recommendations = new ArrayList<>();
        
        if (savingsRate.compareTo(new BigDecimal("20")) < 0) {
            recommendations.add("Increase savings rate to at least 20% to ensure long-term financial security");
        }
        
        if (expenseToIncomeRatio.compareTo(new BigDecimal("0.8")) > 0) {
            recommendations.add("Reduce expense-to-income ratio to below 80% to avoid financial stress");
        }
        
        if (netSavings.compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add("Urgently adjust budget to ensure income exceeds expenses and avoid negative cash flow");
        }
        
        // Add general recommendations
        recommendations.add("Establish an emergency fund equivalent to 3-6 months of living expenses");
        recommendations.add("Regularly review bills and subscription services, cancel unnecessary expenses");
        recommendations.add("Consider adding additional income sources to improve overall financial stability");
        
        // Populate result map
        result.put("score", finalScore);
        result.put("category", category);
        result.put("savingsRate", savingsRate);
        result.put("expenseToIncomeRatio", expenseToIncomeRatio);
        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("netSavings", netSavings);
        result.put("recommendations", recommendations);
        
        return result;
    }
    
    /**
     * Generates financial health report based on transaction data
     */
    public String generateFinancialHealthReport(List<Transaction> transactions) {
        Map<String, Object> healthScore = calculateFinancialHealthScore(transactions);
        
        // Create basic financial health report
        int score = (int) healthScore.get("score");
        String category = (String) healthScore.get("category");
        BigDecimal savingsRate = (BigDecimal) healthScore.get("savingsRate");
        BigDecimal expenseRatio = (BigDecimal) healthScore.get("expenseToIncomeRatio");
        
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) healthScore.get("recommendations");
        
        // Generate report
        StringBuilder report = new StringBuilder();
        report.append("# Financial Health Report\n\n");
        report.append(String.format("Your Financial Health Score: %d/100 (%s)\n\n", score, category));
        report.append(String.format("Savings Rate: %.1f%%\n", savingsRate.doubleValue()));
        report.append(String.format("Expense-to-Income Ratio: %.2f\n\n", expenseRatio.doubleValue()));
        
        report.append("## Analysis\n\n");
        if (score >= 80) {
            report.append("Your financial condition is very healthy. You have good saving habits and manage expenses well. Continuing these good habits will help you achieve long-term financial goals.\n\n");
        } else if (score >= 60) {
            report.append("Your financial condition is good, but there is still room for improvement. Increasing savings or reducing non-essential expenses can further improve your financial health.\n\n");
        } else if (score >= 40) {
            report.append("Your financial condition is average and needs improvement. We recommend reviewing your spending patterns, finding areas for reduction, and increasing savings.\n\n");
        } else if (score >= 20) {
            report.append("Your financial condition needs significant improvement. Prioritize reducing expenses, increasing savings, and possibly seeking additional income sources.\n\n");
        } else {
            report.append("Your financial condition is at a critical level. Immediate action is needed to reduce expenses, re-plan your budget, and avoid more serious financial problems.\n\n");
        }
        
        report.append("## Recommendations\n\n");
        for (String recommendation : recommendations) {
            report.append("- ").append(recommendation).append("\n");
        }
        
        return report.toString();
    }
} 