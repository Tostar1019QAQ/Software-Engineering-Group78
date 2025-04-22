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
} 