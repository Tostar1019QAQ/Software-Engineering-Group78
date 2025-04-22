package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private final ImportService importService;

    public DashboardService() {
        this.importService = new ImportService();
    }

    public ImportService getImportService() {
        return importService;
    }

    public List<Transaction> getRecentTransactions() {
        List<Transaction> allTransactions = importService.getAllTransactions();
        return allTransactions.stream()
                .sorted((t1, t2) -> t2.getDateTime().compareTo(t1.getDateTime()))
                .limit(10)
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getCategoryDistribution() {
        List<Transaction> allTransactions = importService.getAllTransactions();
        Map<String, BigDecimal> distribution = new HashMap<>();

        for (Transaction transaction : allTransactions) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                String category = transaction.getCategory();
                BigDecimal amount = transaction.getAmount();
                distribution.merge(category, amount, BigDecimal::add);
            }
        }

        return distribution;
    }

    public Map<LocalDate, BigDecimal> getDailySpending(int days) {
        logger.debug("Fetching daily spending for last {} days", days);
        List<Transaction> allTransactions = importService.getAllTransactions();
        logger.debug("Total transactions: {}", allTransactions.size());
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        logger.debug("Date range: {} to {}", startDate, endDate);
        
        Map<LocalDate, BigDecimal> dailySpending = new TreeMap<>();
        
        // Initialize all dates with zero
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            dailySpending.put(date, BigDecimal.ZERO);
            logger.trace("Initialized date: {}", date);
        }

        // Sum up transactions for each day
        for (Transaction transaction : allTransactions) {
            LocalDate date = transaction.getDateTime().toLocalDate();
            
            // Only process EXPENSE transactions
            if (transaction.getType() == TransactionType.EXPENSE) {
                logger.trace("Checking expense transaction: {} - {} - {}", 
                           date, transaction.getAmount(), transaction.getDescription());
                
                // Check if the date is within our range
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    BigDecimal currentAmount = dailySpending.get(date);
                    BigDecimal newAmount = currentAmount.add(transaction.getAmount());
                    dailySpending.put(date, newAmount);
                    logger.trace("Added to daily spending: {}, now: {}", date, newAmount);
                }
            }
        }

        return dailySpending;
    }

    /**
     * 获取指定天数内的每日收入
     * @param days 天数
     * @return 日期到收入金额的映射
     */
    public Map<LocalDate, BigDecimal> getDailyIncome(int days) {
        logger.debug("Fetching daily income for last {} days", days);
        List<Transaction> allTransactions = importService.getAllTransactions();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        Map<LocalDate, BigDecimal> dailyIncome = new TreeMap<>();
        
        // Initialize all dates with zero
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            dailyIncome.put(date, BigDecimal.ZERO);
        }

        // Sum up transactions for each day
        for (Transaction transaction : allTransactions) {
            LocalDate date = transaction.getDateTime().toLocalDate();
            
            // Only process INCOME transactions
            if (transaction.getType() == TransactionType.INCOME) {
                // Check if the date is within our range
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    BigDecimal currentAmount = dailyIncome.get(date);
                    BigDecimal newAmount = currentAmount.add(transaction.getAmount());
                    dailyIncome.put(date, newAmount);
                    logger.trace("Added to daily income: {}, now: {}", date, newAmount);
                }
            }
        }

        return dailyIncome;
    }

    public BigDecimal getTotalExpenses() {
        List<Transaction> allTransactions = importService.getAllTransactions();
        return allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIncome() {
        List<Transaction> allTransactions = importService.getAllTransactions();
        return allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpenses());
    }

    // Total budget management
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private LocalDate budgetStartDate;
    private LocalDate budgetEndDate;
    
    /**
     * Updates the total budget and budget period
     * @param totalBudget The total budget amount
     * @param startDate The start date of the budget period
     * @param endDate The end date of the budget period
     */
    public void updateTotalBudget(BigDecimal totalBudget, LocalDate startDate, LocalDate endDate) {
        logger.info("Updating total budget: amount={}, period={} to {}", 
                   totalBudget, startDate, endDate);
        this.totalBudget = totalBudget;
        this.budgetStartDate = startDate;
        this.budgetEndDate = endDate;
    }
    
    /**
     * Gets the total budget for the current period
     * @return The total budget amount
     */
    public BigDecimal getTotalBudget() {
        return totalBudget;
    }
    
    /**
     * Gets the budget start date
     * @return The budget start date
     */
    public LocalDate getBudgetStartDate() {
        return budgetStartDate;
    }
    
    /**
     * Gets the budget end date
     * @return The budget end date
     */
    public LocalDate getBudgetEndDate() {
        return budgetEndDate;
    }
} 