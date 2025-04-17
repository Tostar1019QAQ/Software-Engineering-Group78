package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
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

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    // DeepSeek API配置
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
     * 基于描述自动分类交易
     */
    public String categorizeTransaction(String description) {
        String lowerDesc = description.toLowerCase();
        
        // 先尝试基于关键词匹配
        for (Map.Entry<String, String> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            if (lowerDesc.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // 如果关键词匹配失败，尝试使用DeepSeek API进行分类
        try {
            return categorizeWithDeepSeek(description);
        } catch (Exception e) {
            logger.error("DeepSeek API categorization failed: {}", e.getMessage());
            return "Others"; // 默认分类
        }
    }
    
    /**
     * 使用DeepSeek API分类交易
     */
    private String categorizeWithDeepSeek(String description) throws IOException, InterruptedException {
        // 如果API密钥未设置，直接返回默认分类
        if (DEEPSEEK_API_KEY == null || DEEPSEEK_API_KEY.isEmpty()) {
            logger.warn("DeepSeek API key not configured");
            return "Others";
        }
        
        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", DEEPSEEK_MODEL);
        
        ArrayNode messagesArray = requestBody.putArray("messages");
        ObjectNode systemMessage = messagesArray.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a financial transaction categorizer. " +
                "Categorize the given transaction description into one of these categories: " +
                String.join(", ", PREDEFINED_CATEGORIES) + ". " +
                "Reply with only the category name, nothing else.");
        
        ObjectNode userMessage = messagesArray.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", "Transaction description: " + description);
        
        // 设置温度，使输出更确定性
        requestBody.put("temperature", 0.1);
        
        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEEPSEEK_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        
        // 发送请求并解析响应
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("DeepSeek API error: {} {}", response.statusCode(), response.body());
            return "Others";
        }
        
        // 解析响应获取分类
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");
            
            // 清理并验证分类
            String category = content.trim();
            if (PREDEFINED_CATEGORIES.contains(category)) {
                return category;
            } else {
                logger.warn("DeepSeek returned invalid category: {}", category);
                return "Others";
            }
        } catch (Exception e) {
            logger.error("Failed to parse DeepSeek API response: {}", e.getMessage());
            return "Others";
        }
    }

    /**
     * 分析支出模式
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
     * 使用DeepSeek生成个性化财务分析和建议
     */
    public String generateFinancialInsights(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "Insufficient data for analysis. Please add more transactions.";
        }
        
        // 如果API密钥未设置，返回基本建议
        if (DEEPSEEK_API_KEY == null || DEEPSEEK_API_KEY.isEmpty()) {
            logger.warn("DeepSeek API key not configured, falling back to basic suggestions");
            return String.join("\n\n", generateBasicSavingsSuggestions(analyzeSpendingPattern(transactions)));
        }
        
        try {
            // 准备交易数据
            Map<String, BigDecimal> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
            
            BigDecimal totalSpent = categoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("Here is my transaction data for analysis:\n\n");
            prompt.append("Total Spent: ¥").append(totalSpent).append("\n");
            prompt.append("Spending by Category:\n");
            
            for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ¥")
                      .append(entry.getValue()).append(" (")
                      .append(entry.getValue().multiply(BigDecimal.valueOf(100))
                             .divide(totalSpent, 2, BigDecimal.ROUND_HALF_UP))
                      .append("%)\n");
            }
            
            // 调用DeepSeek API
            return callDeepSeekForInsights(prompt.toString());
        } catch (Exception e) {
            logger.error("Failed to generate insights: {}", e.getMessage());
            return "Failed to generate insights. Please try again later.";
        }
    }
    
    /**
     * 调用DeepSeek API获取财务洞察
     */
    private String callDeepSeekForInsights(String prompt) throws IOException, InterruptedException {
        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", DEEPSEEK_MODEL);
        
        ArrayNode messagesArray = requestBody.putArray("messages");
        ObjectNode systemMessage = messagesArray.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a financial advisor. Analyze the user's spending data and provide " +
                "personalized insights and suggestions to help them improve their financial health. " +
                "Focus on identifying patterns, potential savings opportunities, and actionable advice. " +
                "Keep your response concise, clear, and easy to understand. " +
                "Format your response with clear headings and bullet points where appropriate.");
        
        ObjectNode userMessage = messagesArray.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        // 设置温度，允许一些创造性
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        
        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEEPSEEK_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        
        // 发送请求并解析响应
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("DeepSeek API error: {} {}", response.statusCode(), response.body());
            return "Failed to generate insights due to API error. Please try again later.";
        }
        
        // 解析响应获取内容
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            logger.error("Failed to parse DeepSeek API response: {}", e.getMessage());
            return "Failed to parse AI-generated insights. Please try again.";
        }
    }

    /**
     * 生成基本的节省建议（备用方案）
     */
    public List<String> generateBasicSavingsSuggestions(Map<String, Object> analysis) {
        List<String> suggestions = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> categoryTotals = (Map<String, BigDecimal>) analysis.get("categoryTotals");
        String topCategory = (String) analysis.get("topCategory");
        BigDecimal dailyAverage = (BigDecimal) analysis.get("dailyAverage");
        
        // 基于最高支出类别的建议
        if (topCategory != null && !topCategory.isEmpty()) {
            suggestions.add("Your highest spending is in " + topCategory + " category. Consider reviewing these expenses.");
        }
        
        // 基于日均支出的建议
        if (dailyAverage.compareTo(new BigDecimal("200")) > 0) {
            suggestions.add("Your daily spending is relatively high. Consider creating a detailed budget plan.");
        }
        
        // 基于类别分布的建议
        if (categoryTotals != null) {
            BigDecimal foodExpense = categoryTotals.getOrDefault("Food", BigDecimal.ZERO);
            BigDecimal totalExpense = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (foodExpense.compareTo(totalExpense.multiply(new BigDecimal("0.3"))) > 0) {
                suggestions.add("Food expenses are high relative to total spending. Consider reducing dining out frequency.");
            }
        }
        
        // 通用建议
        suggestions.add("Consider using automatic expense tracking to maintain timely and accurate records.");
        suggestions.add("Regularly review your bills to ensure there are no duplicate or erroneous charges.");
        
        return suggestions;
    }
    
    /**
     * 配置API密钥
     */
    public void setApiKey(String apiKey) {
        // 在实际应用中，应该安全地存储API密钥
        // 这里仅用于演示
        logger.info("DeepSeek API key updated");
    }
    // ====================== 【胡峻玮新增】辅助分析功能 ======================

    /**
     * 【胡峻玮新增】
     * 判断某一支出类别是否超过合理比例（如30%），用于预警分析
     * @param transactions 交易列表
     * @param category 要检测的支出类别（如 "Food"）
     * @param thresholdPercent 超过多少百分比算“过高”支出（如30.0）
     * @return 是否超支
     */
    public boolean isCategoryOverspent(List<Transaction> transactions, String category, double thresholdPercent) {
        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal categoryTotal = transactions.stream()
                .filter(t -> category.equalsIgnoreCase(t.getCategory()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal ratio = categoryTotal.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, BigDecimal.ROUND_HALF_UP);
        return ratio.compareTo(BigDecimal.valueOf(thresholdPercent)) > 0;
    }

    /**
     * 【胡峻玮新增】
     * 检查某个关键词是否能被 KEYWORD_CATEGORY_MAP 中识别（用于分类提示）
     * @param keyword 用户输入的关键词
     * @return 是否为可识别关键词
     */
    public boolean isKeywordRecognized(String keyword) {
        return KEYWORD_CATEGORY_MAP.containsKey(keyword.toLowerCase());
    }

    // ====================== 【胡峻玮新增结束】 ======================

} 