package com.finance.service.impl;

import com.finance.model.Transaction;
import com.finance.model.Budget;
import com.finance.service.FinanceService;
import com.finance.service.DataStorageService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class FinanceServiceImpl implements FinanceService {
    private final DataStorageService dataStorageService;
    
    public FinanceServiceImpl(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }
    
    @Override
    public void addTransaction(Transaction transaction) {
        dataStorageService.saveTransaction(transaction);
    }
    
    @Override
    public List<Transaction> getTransactions() {
        return dataStorageService.getAllTransactions();
    }
    
    @Override
    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return getTransactions().stream()
                .filter(t -> !t.getDate().toLocalDate().isBefore(startDate) && 
                           !t.getDate().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Transaction> getTransactionsByCategory(String category) {
        return getTransactions().stream()
                .filter(t -> t.getCategory().equals(category))
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateTransaction(Transaction transaction) {
        dataStorageService.updateTransaction(transaction);
    }
    
    @Override
    public void deleteTransaction(String id) {
        dataStorageService.deleteTransaction(id);
    }
    
    @Override
    public void createBudget(Budget budget) {
        dataStorageService.saveBudget(budget);
    }
    
    @Override
    public List<Budget> getBudgets() {
        return dataStorageService.getAllBudgets();
    }
    
    @Override
    public Budget getBudgetByCategory(String category) {
        return getBudgets().stream()
                .filter(b -> b.getCategory().equals(category))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public void updateBudget(Budget budget) {
        dataStorageService.updateBudget(budget);
    }
    
    @Override
    public void deleteBudget(String id) {
        dataStorageService.deleteBudget(id);
    }
    
    @Override
    public double getTotalIncome(LocalDate startDate, LocalDate endDate) {
        return getTransactionsByDateRange(startDate, endDate).stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
    
    @Override
    public double getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        return getTransactionsByDateRange(startDate, endDate).stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
    
    @Override
    public double getBalance(LocalDate startDate, LocalDate endDate) {
        return getTotalIncome(startDate, endDate) - getTotalExpenses(startDate, endDate);
    }
    
    @Override
    public List<String> getCategories() {
        return getTransactions().stream()
                .map(Transaction::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
    
    @Override
    public double getBudgetProgress(String category) {
        Budget budget = getBudgetByCategory(category);
        if (budget == null) {
            return 0.0;
        }
        return (budget.getSpent() / budget.getAmount()) * 100;
    }
    
    @Override
    public void importTransactions(String filePath) {
        // TODO: Implement import functionality
    }
    
    @Override
    public void exportTransactions(String filePath) {
        // TODO: Implement export functionality
    }
    
    @Override
    public void importBudgets(String filePath) {
        // TODO: Implement import functionality
    }
    
    @Override
    public void exportBudgets(String filePath) {
        // TODO: Implement export functionality
    }
} 