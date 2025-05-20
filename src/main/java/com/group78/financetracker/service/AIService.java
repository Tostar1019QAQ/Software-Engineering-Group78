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
        
        // 如果关键词匹配失败，使用简单的启发式匹配而不是调用API
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
            return "Others"; // 默认分类
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
            return "没有足够的交易数据进行分析。";
        }
        
        // 使用基本分析替代DeepSeek API调用
        Map<String, Object> analysis = analyzeSpendingPattern(transactions);
        List<String> suggestions = generateBasicSavingsSuggestions(analysis);
        
        StringBuilder insights = new StringBuilder();
        insights.append("财务分析报告:\n\n");
        
        // 添加基本财务指标
        BigDecimal totalSpent = (BigDecimal) analysis.get("totalSpent");
        BigDecimal dailyAverage = (BigDecimal) analysis.get("dailyAverage");
        String topCategory = (String) analysis.get("topCategory");
        
        insights.append("您在 ").append(topCategory).append(" 类别上的支出最高。\n");
        insights.append("平均每日支出为 ¥").append(dailyAverage.setScale(2, RoundingMode.HALF_UP)).append("。\n\n");
        
        // 添加建议
        insights.append("建议:\n");
        for (String suggestion : suggestions) {
            insights.append("- ").append(suggestion).append("\n");
        }
        
        return insights.toString();
    }
    
    /**
     * Generate financial insights from a custom prompt
     */
    public String generateFinancialInsights(String customPrompt) {
        // 提供固定的财务洞察而不是调用API
        StringBuilder insights = new StringBuilder();
        
        insights.append("财务分析:\n\n");
        insights.append("根据您的财务数据，我们建议考虑以下方面的改进:\n");
        insights.append("1. 创建应急基金：建立相当于3-6个月日常开支的储备金。\n");
        insights.append("2. 制定详细预算：跟踪每月支出，找出可以削减的领域。\n");
        insights.append("3. 优化开支结构：尽量将至少20%的收入用于储蓄和投资。\n");
        insights.append("4. 审查固定支出：定期检查各种订阅服务和账单，取消不必要的支出。\n");
        insights.append("5. 设立自动储蓄：每次发薪日自动转移一部分资金到储蓄账户。\n");
        
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
            recommendations.add("增加储蓄率至少达到20%，以确保长期财务安全");
        }
        
        if (expenseToIncomeRatio.compareTo(new BigDecimal("0.8")) > 0) {
            recommendations.add("降低支出与收入的比例到80%以下，避免财务紧张");
        }
        
        if (netSavings.compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add("紧急调整预算，确保收入大于支出，避免负现金流");
        }
        
        // Add general recommendations
        recommendations.add("建立相当于3-6个月生活费用的应急基金");
        recommendations.add("定期检查账单和订阅服务，取消不必要的开支");
        recommendations.add("考虑增加额外收入来源，提高整体财务稳定性");
        
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
        
        // 创建基本的财务健康报告
        int score = (int) healthScore.get("score");
        String category = (String) healthScore.get("category");
        BigDecimal savingsRate = (BigDecimal) healthScore.get("savingsRate");
        BigDecimal expenseRatio = (BigDecimal) healthScore.get("expenseToIncomeRatio");
        
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) healthScore.get("recommendations");
        
        // 生成报告
        StringBuilder report = new StringBuilder();
        report.append("# 财务健康报告\n\n");
        report.append(String.format("您的财务健康评分: %d/100 (%s)\n\n", score, category));
        report.append(String.format("储蓄率: %.1f%%\n", savingsRate.doubleValue()));
        report.append(String.format("支出占收入比例: %.2f\n\n", expenseRatio.doubleValue()));
        
        report.append("## 分析\n\n");
        if (score >= 80) {
            report.append("您的财务状况非常健康。您有良好的储蓄习惯，支出管理得当。继续保持这些良好习惯，将帮助您实现长期财务目标。\n\n");
        } else if (score >= 60) {
            report.append("您的财务状况良好，但仍有改进空间。增加储蓄或减少非必要支出可以进一步提高您的财务健康。\n\n");
        } else if (score >= 40) {
            report.append("您的财务状况一般，需要注意改进。建议审视您的支出模式，寻找可以削减的领域，并增加储蓄。\n\n");
        } else if (score >= 20) {
            report.append("您的财务状况需要显著改善。优先考虑减少支出，增加储蓄，并可能需要寻求额外收入来源。\n\n");
        } else {
            report.append("您的财务状况处于危急水平。需要立即采取行动减少支出，重新规划预算，以避免更严重的财务问题。\n\n");
        }
        
        report.append("## 建议\n\n");
        for (String recommendation : recommendations) {
            report.append("- ").append(recommendation).append("\n");
        }
        
        return report.toString();
    }
} 