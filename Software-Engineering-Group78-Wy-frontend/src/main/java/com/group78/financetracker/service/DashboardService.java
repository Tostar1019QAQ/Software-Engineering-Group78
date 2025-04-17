package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardService {
    private final ImportService importService;

    public DashboardService() {
        this.importService = new ImportService();
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
        List<Transaction> allTransactions = importService.getAllTransactions();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        Map<LocalDate, BigDecimal> dailySpending = new TreeMap<>();
        
        // Initialize all dates with zero
        for (int i = 0; i < days; i++) {
            dailySpending.put(startDate.plusDays(i), BigDecimal.ZERO);
        }

        // Sum up transactions for each day
        for (Transaction transaction : allTransactions) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                LocalDate date = transaction.getDateTime().toLocalDate();
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    dailySpending.merge(date, transaction.getAmount(), BigDecimal::add);
                }
            }
        }

        return dailySpending;
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
} 