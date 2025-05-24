package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    // DeepSeek API configuration
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";
    
    // 加密密钥和配置文件路径
    private static final String CONFIG_FILE = "config/api_config.properties";
    private static final String ENCRYPTED_API_KEY_PROPERTY = "encrypted_api_key";
    private static SecretKey secretKey;
    
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
        
        // 初始化加密密钥
        try {
            initializeSecretKey();
            createConfigDirectoryIfNeeded();
        } catch (Exception e) {
            logger.error("初始化加密密钥失败: " + e.getMessage(), e);
        }
        
        logger.info("AIService initialized with DeepSeek API");
    }
    
    /**
     * 初始化加密密钥
     */
    private void initializeSecretKey() throws NoSuchAlgorithmException, IOException {
        File keyFile = new File("config/secret.key");
        
        if (keyFile.exists()) {
            // 从文件加载密钥
            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            // 生成新密钥
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // 使用256位AES密钥
            secretKey = keyGen.generateKey();
            
            // 保存密钥到文件
            createConfigDirectoryIfNeeded();
            try (FileOutputStream fos = new FileOutputStream(keyFile)) {
                fos.write(secretKey.getEncoded());
            }
        }
    }
    
    /**
     * 创建配置目录（如果不存在）
     */
    private void createConfigDirectoryIfNeeded() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }
    
    /**
     * 加密API密钥
     */
    private String encryptApiKey(String apiKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("加密API密钥失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解密API密钥
     */
    private String decryptApiKey(String encryptedApiKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedApiKey));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("解密API密钥失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从配置文件加载加密的API密钥
     */
    private String loadApiKey() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                // 默认API密钥
                String defaultApiKey = "sk-5437453fe5544771b8ca5196cfac9bc5";
                saveApiKey(defaultApiKey);
                return defaultApiKey;
            }
            
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            }
            
            String encryptedApiKey = properties.getProperty(ENCRYPTED_API_KEY_PROPERTY);
            if (encryptedApiKey == null || encryptedApiKey.isEmpty()) {
                return "sk-5437453fe5544771b8ca5196cfac9bc5"; // 默认API密钥
            }
            
            return decryptApiKey(encryptedApiKey);
        } catch (Exception e) {
            logger.error("加载API密钥失败: " + e.getMessage(), e);
            return "sk-5437453fe5544771b8ca5196cfac9bc5"; // 出错时返回默认密钥
        }
    }
    
    /**
     * 保存API密钥到配置文件（以加密形式）
     */
    public void saveApiKey(String apiKey) {
        try {
            String encryptedApiKey = encryptApiKey(apiKey);
            if (encryptedApiKey == null) {
                logger.error("API密钥加密失败，无法保存");
                return;
            }
            
            Properties properties = new Properties();
            properties.setProperty(ENCRYPTED_API_KEY_PROPERTY, encryptedApiKey);
            
            createConfigDirectoryIfNeeded();
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.store(fos, "DeepSeek API Configuration");
            }
            
            logger.info("API密钥已加密保存");
        } catch (Exception e) {
            logger.error("保存API密钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用DeepSeek API的核心方法
     */
    private String callDeepSeekAPI(List<Map<String, String>> messages) {
        try {
            // 获取API密钥
            String apiKey = loadApiKey();
            
            // 创建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", DEEPSEEK_MODEL);
            
            // 添加消息
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (Map<String, String> message : messages) {
                ObjectNode messageObj = messagesArray.addObject();
                messageObj.put("role", message.get("role"));
                messageObj.put("content", message.get("content"));
            }
            
            // 设置其他参数
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            
            // 发送请求并获取响应
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 记录响应状态和内容
            logger.info("DeepSeek API response status: " + response.statusCode());
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.body());
            if (responseJson.has("error")) {
                String errorMessage = responseJson.path("error").path("message").asText("Unknown error");
                logger.error("DeepSeek API error: " + errorMessage);
                return "AI服务返回错误: " + errorMessage;
            }
            
            return responseJson.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            logger.error("调用DeepSeek API出错: " + e.getMessage(), e);
            return "调用AI服务时出错: " + e.getMessage();
        }
    }

    /**
     * 使用DeepSeek API自动分类交易
     */
    public String categorizeTransaction(String description) {
        // 首先尝试使用DeepSeek API
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个财务分析助手，专门负责将交易描述分类到以下类别之一：Food, Transport, Shopping, Entertainment, Housing, Healthcare, Education, Communication, Others。只返回一个类别名称，不要有其他解释。Please respond in English only.");
            
            // 用户提示
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "请将以下交易描述分类到最合适的类别：" + description);
            
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // 调用API
            String response = callDeepSeekAPI(messages);
            logger.info("AI categorization result: " + response);
            
            // 处理响应，确保返回有效类别
            for (String category : PREDEFINED_CATEGORIES) {
                if (response.toLowerCase().contains(category.toLowerCase())) {
                    return category;
                }
            }
        } catch (Exception e) {
            logger.error("AI分类交易出错，使用备选方法: " + e.getMessage(), e);
        }
        
        // 备选方案：使用现有的关键词匹配逻辑
        String lowerDesc = description.toLowerCase();
        
        // First try keyword-based matching
        for (Map.Entry<String, String> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            if (lowerDesc.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // If keyword matching fails, use simple heuristic matching
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
     * 使用DeepSeek API生成个性化财务洞察
     */
    public String generateFinancialInsights(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "没有足够的交易数据进行分析。";
        }
        
        try {
            // 先进行基本分析以提供更多上下文
            Map<String, Object> analysis = analyzeSpendingPattern(transactions);
            
            // 构建交易数据文本
            StringBuilder transactionData = new StringBuilder();
            for (int i = 0; i < Math.min(30, transactions.size()); i++) {
                Transaction t = transactions.get(i);
                transactionData.append(t.getDateTime()).append(" | ")
                          .append(t.getType()).append(" | ")
                          .append(t.getAmount()).append(" | ")
                          .append(t.getCategory()).append(" | ")
                          .append(t.getDescription()).append("\n");
            }
            if (transactions.size() > 30) {
                transactionData.append("... (还有更多交易记录，已省略) ...\n");
            }
            
            // 构建分析数据文本
            StringBuilder analysisData = new StringBuilder();
            analysisData.append("总支出: ").append(analysis.get("totalSpent")).append("\n");
            analysisData.append("日均支出: ").append(analysis.get("dailyAverage")).append("\n");
            analysisData.append("最高支出类别: ").append(analysis.get("topCategory")).append("\n");
            
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> categoryTotals = (Map<String, BigDecimal>) analysis.get("categoryTotals");
            if (categoryTotals != null) {
                analysisData.append("各类别支出:\n");
                for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                    analysisData.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            // 创建消息
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的财务顾问，负责分析用户的交易数据并提供有价值的财务洞察和建议。请基于提供的交易数据和分析指标，给出个性化的财务建议。包括支出模式分析、预算建议和储蓄策略。回答应当全面但简洁，重点突出具体可行的改进措施。Please respond in English only.");
            
            // 用户提示
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "请分析以下交易数据和财务指标，并提供个性化财务建议：\n\n" +
                    "===交易数据===\n" + transactionData.toString() + "\n\n" +
                    "===分析指标===\n" + analysisData.toString());
            
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // 调用API
            String response = callDeepSeekAPI(messages);
            logger.info("Financial insights generated using DeepSeek API");
            return response;
        } catch (Exception e) {
            logger.error("生成财务洞察出错，使用备选方法: " + e.getMessage(), e);
            
            // 使用基本分析方法
            Map<String, Object> analysis = analyzeSpendingPattern(transactions);
            List<String> suggestions = generateBasicSavingsSuggestions(analysis);
            
            StringBuilder insights = new StringBuilder();
            insights.append("财务分析报告：\n\n");
            
            BigDecimal totalSpent = (BigDecimal) analysis.get("totalSpent");
            BigDecimal dailyAverage = (BigDecimal) analysis.get("dailyAverage");
            String topCategory = (String) analysis.get("topCategory");
            
            insights.append("您的最高支出类别是：").append(topCategory).append("。\n");
            insights.append("您的平均日支出为：$").append(dailyAverage.setScale(2, RoundingMode.HALF_UP)).append("。\n\n");
            
            insights.append("建议：\n");
            for (String suggestion : suggestions) {
                insights.append("- ").append(suggestion).append("\n");
            }
            
            return insights.toString();
        }
    }
    
    /**
     * 使用DeepSeek API处理自定义财务分析提示
     */
    public String generateFinancialInsights(String customPrompt) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的财务顾问AI，专注于回答财务相关问题，提供个性化的财务建议和洞察。请基于用户的问题，提供清晰、准确、有价值的财务建议。如果问题与财务无关，请礼貌地说明你只能回答财务相关问题。Please respond in English only.");
            
            // 用户提示
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", customPrompt);
            
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // 调用API
            String response = callDeepSeekAPI(messages);
            logger.info("Financial insights for custom prompt generated using DeepSeek API");
            return response;
        } catch (Exception e) {
            logger.error("处理自定义财务分析提示出错，使用备选方法: " + e.getMessage(), e);
            
            // 提供固定财务洞察
            StringBuilder insights = new StringBuilder();
            
            insights.append("财务分析：\n\n");
            insights.append("根据您的财务数据，我们建议考虑以下改进措施：\n");
            insights.append("1. 建立应急基金：积累相当于3-6个月日常开支的储备金。\n");
            insights.append("2. 创建详细预算：跟踪月度支出，识别可以削减的领域。\n");
            insights.append("3. 优化支出结构：尝试将至少20%的收入分配给储蓄和投资。\n");
            insights.append("4. 审查固定支出：定期检查订阅服务和账单，取消不必要的支出。\n");
            insights.append("5. 设置自动储蓄：在每次发薪日自动将一部分资金转入储蓄账户。\n");
            
            return insights.toString();
        }
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
            suggestions.add("您的最高支出在" + topCategory + "类别。考虑审查这些支出。");
        }
        
        // Suggestions based on daily average spending
        if (dailyAverage.compareTo(new BigDecimal("200")) > 0) {
            suggestions.add("您的日均支出相对较高。考虑创建详细的预算计划。");
        }
        
        // Suggestions based on category distribution
        if (categoryTotals != null) {
            BigDecimal foodExpense = categoryTotals.getOrDefault("Food", BigDecimal.ZERO);
            BigDecimal totalExpense = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (foodExpense.compareTo(totalExpense.multiply(new BigDecimal("0.3"))) > 0) {
                suggestions.add("食品支出占总支出的比例较高。考虑减少外出就餐的频率。");
            }
        }
        
        // General suggestions
        suggestions.add("考虑使用自动支出跟踪来保持及时准确的记录。");
        suggestions.add("定期审查您的账单，确保没有重复或错误的收费。");
        
        return suggestions;
    }
    
    /**
     * Configure API key
     */
    public void setApiKey(String apiKey) {
        // 加密并保存API密钥
        saveApiKey(apiKey);
        logger.info("DeepSeek API key updated and encrypted");
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
            recommendations.add("将支出收入比降低到80%以下，以避免财务压力");
        }
        
        if (netSavings.compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add("紧急调整预算，确保收入超过支出，避免负现金流");
        }
        
        // Add general recommendations
        recommendations.add("建立相当于3-6个月生活费用的应急基金");
        recommendations.add("定期审查账单和订阅服务，取消不必要的支出");
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
     * 使用DeepSeek API生成财务健康报告
     */
    public String generateFinancialHealthReport(List<Transaction> transactions) {
        try {
            // 先计算基本指标
            Map<String, Object> healthScore = calculateFinancialHealthScore(transactions);
            
            // 构建财务健康数据文本
            StringBuilder healthData = new StringBuilder();
            healthData.append("财务健康得分: ").append(healthScore.get("score")).append("/100\n");
            healthData.append("类别: ").append(healthScore.get("category")).append("\n");
            healthData.append("储蓄率: ").append(healthScore.get("savingsRate")).append("%\n");
            healthData.append("支出收入比: ").append(healthScore.get("expenseToIncomeRatio")).append("\n");
            healthData.append("总收入: ￥").append(healthScore.get("totalIncome")).append("\n");
            healthData.append("总支出: ￥").append(healthScore.get("totalExpenses")).append("\n");
            healthData.append("净储蓄: ￥").append(healthScore.get("netSavings")).append("\n");
            
            // 创建消息
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的财务健康顾问，负责根据用户的财务指标生成全面的财务健康报告。报告应包括对用户当前财务状况的评估、强项和弱项分析，以及具体的改进建议。回答应当包含标题、分析和建议三个部分，语言应当专业但易于理解。Please respond in English only.");
            
            // 用户提示
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "请根据以下财务健康指标生成一份全面的财务健康报告：\n\n" + healthData.toString());
            
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // 调用API
            String aiReport = callDeepSeekAPI(messages);
            logger.info("Financial health report generated using DeepSeek API");
            
            return aiReport;
        } catch (Exception e) {
            logger.error("生成财务健康报告出错，使用备选方法: " + e.getMessage(), e);
            
            // 使用原有方法生成基本报告
            return generateBasicFinancialHealthReport(transactions);
        }
    }
    
    /**
     * 生成基本财务健康报告（备选方案）
     */
    private String generateBasicFinancialHealthReport(List<Transaction> transactions) {
        Map<String, Object> healthScore = calculateFinancialHealthScore(transactions);
        
        int score = (int) healthScore.get("score");
        String category = (String) healthScore.get("category");
        BigDecimal savingsRate = (BigDecimal) healthScore.get("savingsRate");
        BigDecimal expenseRatio = (BigDecimal) healthScore.get("expenseToIncomeRatio");
        
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) healthScore.get("recommendations");
        
        // 生成报告
        StringBuilder report = new StringBuilder();
        report.append("# 财务健康报告\n\n");
        report.append(String.format("您的财务健康得分：%d/100（%s）\n\n", score, category));
        report.append(String.format("储蓄率：%.1f%%\n", savingsRate.doubleValue()));
        report.append(String.format("支出收入比：%.2f\n\n", expenseRatio.doubleValue()));
        
        report.append("## 分析\n\n");
        if (score >= 80) {
            report.append("您的财务状况非常健康。您有良好的储蓄习惯，并很好地管理支出。继续保持这些良好习惯将帮助您实现长期财务目标。\n\n");
        } else if (score >= 60) {
            report.append("您的财务状况良好，但仍有改进空间。增加储蓄或减少非必要支出可以进一步改善您的财务健康。\n\n");
        } else if (score >= 40) {
            report.append("您的财务状况一般，需要改进。我们建议审查您的支出模式，找出可以削减的领域，并增加储蓄。\n\n");
        } else if (score >= 20) {
            report.append("您的财务状况需要显著改进。优先减少支出，增加储蓄，并可能寻求额外收入来源。\n\n");
        } else {
            report.append("您的财务状况处于临界水平。需要立即采取行动减少支出，重新规划预算，避免更严重的财务问题。\n\n");
        }
        
        report.append("## 建议\n\n");
        for (String recommendation : recommendations) {
            report.append("- ").append(recommendation).append("\n");
        }
        
        return report.toString();
    }

    /**
     * 获取当前API密钥
     */
    public String getApiKey() {
        return loadApiKey();
    }
} 