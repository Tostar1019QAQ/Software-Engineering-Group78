package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
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
        
        // If keyword matching fails, try using DeepSeek API for categorization
        try {
            return categorizeWithDeepSeek(description);
        } catch (Exception e) {
            logger.error("DeepSeek API categorization failed: {}", e.getMessage());
            return "Others"; // Default category
        }
    }
    
    /**
     * Uses DeepSeek API to categorize a transaction
     */
    private String categorizeWithDeepSeek(String description) throws IOException, InterruptedException {
        // If API key is not set, return default category
        if (DEEPSEEK_API_KEY == null || DEEPSEEK_API_KEY.isEmpty()) {
            logger.warn("DeepSeek API key not configured");
            return "Others";
        }
        
        // Build request body
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
        
        // Set temperature for more deterministic output
        requestBody.put("temperature", 0.1);
        
        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEEPSEEK_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        
        // Send request and parse response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("DeepSeek API error: {} {}", response.statusCode(), response.body());
            return "Others";
        }
        
        // Parse response to get category
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");
            
            // Clean and validate category
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
     * Generates personalized financial analysis and advice using DeepSeek
     */
    public String generateFinancialInsights(List<Transaction> transactions) {
        // Create generic prompt
        String prompt = "Please analyze the user's transaction data and provide personalized financial advice. " +
                       "Offer insights regarding spending patterns, budget management, and savings opportunities.";
        
        return generateFinancialInsights(transactions, prompt);
    }
    
    /**
     * Generates personalized financial analysis and advice using DeepSeek (with custom prompt)
     * @param transactions list of transactions
     * @param customPrompt custom prompt/question
     * @return generated financial analysis and advice
     */
    public String generateFinancialInsights(List<Transaction> transactions, String customPrompt) {
        try {
            if (transactions.isEmpty()) {
                return "Not enough transaction data for analysis.";
            }
            
            // Prepare transaction data summary
            Map<String, Object> analysis = analyzeSpendingPattern(transactions);
            
            String transactionSummary = String.format(
                "User has %d transactions, with total spending of %s. The highest spending category is %s, with a daily average of %s.",
                transactions.size(),
                analysis.get("totalSpent"),
                analysis.get("topCategory"),
                analysis.get("dailyAverage")
            );
            
            // Build final prompt
            String finalPrompt = customPrompt + "\n\n" + transactionSummary + 
                               "\n\nPlease provide detailed analysis and practical recommendations. Answer in English with clear organization.";
            
            // Try using DeepSeek API
            try {
                return callDeepSeekForInsights(finalPrompt);
            } catch (Exception e) {
                logger.warn("DeepSeek API call failed, falling back to basic analysis: {}", e.getMessage());
                // If DeepSeek API call fails, fall back to basic analysis
                List<String> suggestions = generateBasicSavingsSuggestions(analysis);
                return String.join("\n\n", suggestions);
            }
        } catch (Exception e) {
            logger.error("Error generating financial insights: {}", e.getMessage());
            return "Error generating financial analysis: " + e.getMessage();
        }
    }
    
    /**
     * Calls DeepSeek API for financial insights based on a user message
     * 
     * @param userMessage The user's message
     * @return AI-generated response
     * @throws IOException If an error occurs during API call
     * @throws InterruptedException If the API call is interrupted
     */
    private String callDeepSeekForInsights(String userMessage) throws IOException, InterruptedException {
        String systemPrompt = "You are a professional financial analyst and advisor. Only answer questions related to finance, investments, budgeting, savings, and other financial topics. " +
                "If a user asks about non-financial topics, politely remind them that you can only answer finance-related questions. " +
                "Provide useful and accurate financial advice using clear and understandable language. Always base your analysis on the data provided by the user. " +
                "If questions involve budget data, refer to the information in budget.csv file. If questions involve bill data, refer to the information in bills.csv file.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3); // Lower temperature for more deterministic answers
        requestBody.put("max_tokens", 1000); // Increase max tokens for more complete answers
        
        String requestBodyJson = new ObjectMapper().writeValueAsString(requestBody);
        
        // Implement retry logic
        int maxRetries = 2;
        int retryCount = 0;
        HttpResponse<String> response = null;
        
        while (retryCount <= maxRetries) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(DEEPSEEK_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                        .timeout(Duration.ofSeconds(30)) // Set 30 second timeout
                        .build();
        
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    break; // If successful, exit retry loop
                } else if (response.statusCode() == 429 || response.statusCode() == 500) {
                    // If rate limited or server error, retry
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        logger.warn("API request failed (status code: {}), retrying attempt {}", response.statusCode(), retryCount);
                        Thread.sleep(1000 * retryCount); // Exponential backoff retry
                    }
                } else {
                    // For other errors, throw exception directly
                    logger.error("API returned error: {} - {}", response.statusCode(), response.body());
                    throw new IOException("API error: " + response.statusCode() + " - " + response.body());
                }
            } catch (IOException | InterruptedException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    logger.warn("API request failed, retrying attempt {}: {}", retryCount, e.getMessage());
                    Thread.sleep(1000 * retryCount); // Exponential backoff retry
                } else {
                    throw e; // Retry count exhausted, throw exception
                }
            }
        }
        
        if (response == null || response.statusCode() != 200) {
            throw new IOException("API request failed after " + maxRetries + " retries");
        }

        try {
            JsonNode rootNode = new ObjectMapper().readTree(response.body());
            if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                JsonNode firstChoice = rootNode.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            logger.error("Unexpected API response format: {}", response.body());
            throw new IOException("Unexpected API response format");
        } catch (Exception e) {
            logger.error("Error parsing API response: {}", e.getMessage());
            throw new IOException("Error parsing API response: " + e.getMessage());
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
     * Generates financial insights based on a user message.
     * This method is used by the AIPanel for direct chatting functionality.
     *
     * @param userMessage The message from the user
     * @return AI-generated financial insights or a fallback message if the API key is not configured
     */
    public String generateFinancialInsights(String userMessage) {
        if (DEEPSEEK_API_KEY == null || DEEPSEEK_API_KEY.isEmpty() || DEEPSEEK_API_KEY.equals("your_api_key_here")) {
            logger.warn("DeepSeek API key not configured. Using fallback message.");
            return "Unable to process your request as the AI service is not configured. Please add your DeepSeek API key in the settings.";
        }

        try {
            return callDeepSeekForInsights(userMessage);
        } catch (Exception e) {
            logger.error("Error generating financial insights: {}", e.getMessage());
            String errorMessage = "Sorry, I encountered an error while processing your request. Please try again later.";
            
            // If it's a specific API error, provide more detailed feedback
            if (e.getMessage().contains("API error")) {
                errorMessage += "\n\nTechnical error: " + e.getMessage();
            }
            
            return errorMessage;
        }
    }

    /**
     * Calculates a financial health score based on income, expenses, and savings.
     * The score is on a scale of 0 to 100, where:
     * - 90-100: Excellent financial health
     * - 75-89: Good financial health
     * - 60-74: Average financial health
     * - 40-59: Below average financial health
     * - 0-39: Poor financial health
     * 
     * @param transactions List of transactions to analyze
     * @return A map containing the score and recommendations
     */
    public Map<String, Object> calculateFinancialHealthScore(List<Transaction> transactions) {
        Map<String, Object> result = new HashMap<>();
        
        if (transactions == null || transactions.isEmpty()) {
            result.put("score", 0);
            result.put("category", "Insufficient Data");
            result.put("recommendations", Collections.singletonList("Not enough transaction data to calculate a financial health score."));
            return result;
        }
        
        // Calculate total income and expenses
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(transaction.getAmount());
            }
        }
        
        // Calculate metrics
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.divide(totalIncome, 4, BigDecimal.ROUND_HALF_UP)
                               .multiply(new BigDecimal("100"));
        }
        
        // Calculate expense to income ratio
        BigDecimal expenseToIncomeRatio = BigDecimal.ONE;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            expenseToIncomeRatio = totalExpenses.divide(totalIncome, 4, BigDecimal.ROUND_HALF_UP);
        }
        
        // Calculate score components
        int savingsComponent = calculateSavingsComponent(savingsRate);
        int expenseRatioComponent = calculateExpenseRatioComponent(expenseToIncomeRatio);
        int diversificationComponent = calculateDiversificationComponent(transactions);
        
        // Calculate final score (weighted average)
        int finalScore = (int) (savingsComponent * 0.5 + expenseRatioComponent * 0.3 + diversificationComponent * 0.2);
        finalScore = Math.max(0, Math.min(100, finalScore)); // Ensure score is between 0 and 100
        
        // Determine score category
        String category;
        if (finalScore >= 90) {
            category = "Excellent";
        } else if (finalScore >= 75) {
            category = "Good";
        } else if (finalScore >= 60) {
            category = "Average";
        } else if (finalScore >= 40) {
            category = "Below Average";
        } else {
            category = "Poor";
        }
        
        // Generate recommendations
        List<String> recommendations = generateFinancialHealthRecommendations(
                savingsRate.doubleValue(), 
                expenseToIncomeRatio.doubleValue(),
                diversificationComponent,
                transactions);
        
        // Prepare result
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
     * Calculates the savings component of the financial health score.
     * @param savingsRate The savings rate as a percentage
     * @return A score between 0 and 100
     */
    private int calculateSavingsComponent(BigDecimal savingsRate) {
        double rate = savingsRate.doubleValue();
        
        // Ideal savings rate is 20% or more
        if (rate >= 20) {
            return 100;
        } else if (rate >= 15) {
            return 90;
        } else if (rate >= 10) {
            return 80;
        } else if (rate >= 5) {
            return 60;
        } else if (rate >= 0) {
            return 40;
        } else {
            // Negative savings rate (spending more than earning)
            return (int) Math.max(0, 40 + rate); // Decrease score for negative rates
        }
    }
    
    /**
     * Calculates the expense ratio component of the financial health score.
     * @param expenseToIncomeRatio The ratio of expenses to income
     * @return A score between 0 and 100
     */
    private int calculateExpenseRatioComponent(BigDecimal expenseToIncomeRatio) {
        double ratio = expenseToIncomeRatio.doubleValue();
        
        // Ideal ratio is 0.8 or less (spending 80% or less of income)
        if (ratio <= 0.6) {
            return 100;
        } else if (ratio <= 0.7) {
            return 90;
        } else if (ratio <= 0.8) {
            return 80;
        } else if (ratio <= 0.9) {
            return 60;
        } else if (ratio <= 1.0) {
            return 40;
        } else {
            // Spending more than earning
            return (int) Math.max(0, 40 - (ratio - 1.0) * 40);
        }
    }
    
    /**
     * Calculates the diversification component of the financial health score.
     * @param transactions List of transactions to analyze
     * @return A score between 0 and 100
     */
    private int calculateDiversificationComponent(List<Transaction> transactions) {
        // Calculate category distribution
        Map<String, BigDecimal> categoryTotals = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        
        if (categoryTotals.isEmpty()) {
            return 50; // Neutral score if no expense categories
        }
        
        // Calculate total expenses
        BigDecimal totalExpenses = categoryTotals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return 50; // Neutral score if no expenses
        }
        
        // Calculate percentage for each category
        List<Double> categoryPercentages = categoryTotals.values().stream()
            .map(amount -> amount.divide(totalExpenses, 4, BigDecimal.ROUND_HALF_UP).doubleValue() * 100)
            .collect(Collectors.toList());
        
        // Calculate standard deviation of percentages (lower is better - more evenly distributed)
        double mean = categoryPercentages.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSquaredDiff = categoryPercentages.stream()
            .mapToDouble(p -> Math.pow(p - mean, 2))
            .sum();
        double stdDev = Math.sqrt(sumSquaredDiff / categoryPercentages.size());
        
        // Convert standard deviation to a score (inverse relationship)
        // A perfectly balanced budget would have stdDev = 0
        // Typical range might be 0-50 for stdDev in realistic scenarios
        int score = (int) Math.max(0, 100 - stdDev * 2);
        
        // Adjust for number of categories (more categories is better)
        int categoryCount = categoryTotals.size();
        int categoryBonus = Math.min(20, categoryCount * 4); // Up to 20 point bonus for 5+ categories
        
        return Math.min(100, score + categoryBonus);
    }
    
    /**
     * Generates personalized recommendations based on financial health analysis.
     * 
     * @param savingsRate The savings rate percentage
     * @param expenseRatio The expense to income ratio
     * @param diversificationScore The diversification component score
     * @param transactions List of transactions for detailed analysis
     * @return A list of specific recommendations
     */
    private List<String> generateFinancialHealthRecommendations(
            double savingsRate, 
            double expenseRatio, 
            int diversificationScore,
            List<Transaction> transactions) {
        
        List<String> recommendations = new ArrayList<>();
        
        // Savings recommendations
        if (savingsRate < 0) {
            recommendations.add("CRITICAL: You're spending more than you earn. Reduce expenses immediately to avoid debt accumulation.");
        } else if (savingsRate < 5) {
            recommendations.add("Your savings rate is very low. Aim to save at least 5-10% of your income.");
        } else if (savingsRate < 10) {
            recommendations.add("Consider increasing your savings rate to at least 10-15% for better financial security.");
        } else if (savingsRate < 20) {
            recommendations.add("Good savings rate. For long-term financial independence, try to increase savings to 20% or more.");
        } else {
            recommendations.add("Excellent savings rate! Continue maintaining this level for strong financial health.");
        }
        
        // Expense ratio recommendations
        if (expenseRatio > 1.0) {
            recommendations.add("Your expenses exceed your income. Identify non-essential spending to reduce immediately.");
        } else if (expenseRatio > 0.9) {
            recommendations.add("Your expense ratio is very high. Look for ways to reduce expenses or increase income.");
        } else if (expenseRatio > 0.8) {
            recommendations.add("Try to reduce your expense-to-income ratio to below 80% for better financial stability.");
        } else {
            recommendations.add("Your expense-to-income ratio is well-managed. Continue monitoring to maintain this level.");
        }
        
        // Diversification recommendations
        if (diversificationScore < 50) {
            recommendations.add("Your spending is concentrated in too few categories. A more balanced budget across different categories reduces financial risk.");
        }
        
        // Category-specific recommendations
        Map<String, BigDecimal> categoryTotals = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        
        BigDecimal totalExpenses = categoryTotals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
            // Check for categories with excessive spending
            for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                String category = entry.getKey();
                BigDecimal amount = entry.getValue();
                BigDecimal percentage = amount.divide(totalExpenses, 4, BigDecimal.ROUND_HALF_UP)
                                           .multiply(new BigDecimal("100"));
                
                // Category-specific thresholds for recommendations
                if (category.equals("Food") && percentage.compareTo(new BigDecimal("30")) > 0) {
                    recommendations.add("Your food expenses are high at " + percentage.setScale(1, BigDecimal.ROUND_HALF_UP) + 
                                     "% of total spending. Consider meal planning or cooking at home more often.");
                } else if (category.equals("Entertainment") && percentage.compareTo(new BigDecimal("15")) > 0) {
                    recommendations.add("Entertainment expenses at " + percentage.setScale(1, BigDecimal.ROUND_HALF_UP) + 
                                     "% may be reduced by finding lower-cost alternatives.");
                } else if (category.equals("Shopping") && percentage.compareTo(new BigDecimal("20")) > 0) {
                    recommendations.add("Shopping expenses are " + percentage.setScale(1, BigDecimal.ROUND_HALF_UP) + 
                                     "% of your budget. Consider implementing a waiting period before non-essential purchases.");
                }
            }
        }
        
        // Add some general recommendations if we don't have many specific ones
        if (recommendations.size() < 3) {
            recommendations.add("Establish an emergency fund with 3-6 months of essential expenses.");
            recommendations.add("Review and optimize your recurring subscriptions and bills regularly.");
        }
        
        return recommendations;
    }
    
    /**
     * Analyzes financial health and generates a comprehensive report with DeepSeek API.
     * 
     * @param transactions List of transactions to analyze
     * @return A detailed financial health report
     */
    public String generateFinancialHealthReport(List<Transaction> transactions) {
        try {
            Map<String, Object> healthScore = calculateFinancialHealthScore(transactions);
            
            // Format the score data for the prompt
            int score = (int) healthScore.get("score");
            String category = (String) healthScore.get("category");
            BigDecimal savingsRate = (BigDecimal) healthScore.get("savingsRate");
            BigDecimal expenseRatio = (BigDecimal) healthScore.get("expenseToIncomeRatio");
            @SuppressWarnings("unchecked")
            List<String> recommendations = (List<String>) healthScore.get("recommendations");
            
            // Create detailed prompt
            String prompt = String.format(
                "Generate a comprehensive financial health report for a user with the following metrics:\n" +
                "- Financial Health Score: %d/100 (%s)\n" +
                "- Savings Rate: %.1f%%\n" +
                "- Expense-to-Income Ratio: %.2f\n\n" +
                "Initial recommendations based on analysis:\n%s\n\n" +
                "Please provide a detailed assessment of their financial health, explaining the score and offering practical, " +
                "actionable advice for improvement. Include specific strategies for building emergency savings, reducing debt, " +
                "optimizing spending patterns, and improving long-term financial stability.",
                score, category, savingsRate.doubleValue(), expenseRatio.doubleValue(),
                String.join("\n", recommendations)
            );
            
            // Call the AI service
            try {
                return callDeepSeekForInsights(prompt);
            } catch (Exception e) {
                logger.warn("DeepSeek API call failed for financial health report: {}", e.getMessage());
                
                // If API call fails, return a basic report
                StringBuilder report = new StringBuilder();
                report.append("# Financial Health Report\n\n");
                report.append(String.format("Your Financial Health Score: %d/100 (%s)\n\n", score, category));
                report.append(String.format("Savings Rate: %.1f%%\n", savingsRate.doubleValue()));
                report.append(String.format("Expense-to-Income Ratio: %.2f\n\n", expenseRatio.doubleValue()));
                report.append("## Recommendations\n\n");
                
                for (String recommendation : recommendations) {
                    report.append("- ").append(recommendation).append("\n");
                }
                
                return report.toString();
            }
        } catch (Exception e) {
            logger.error("Error generating financial health report: {}", e.getMessage());
            return "Unable to generate financial health report due to an error: " + e.getMessage();
        }
    }
} 