package main.java.com.finance.service;

import main.java.com.finance.model.Transaction;
import main.java.com.finance.model.Budget;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FinanceServiceImpl implements FinanceService {
    private List<Transaction> transactions;
    private List<Budget> budgets;
    
    public FinanceServiceImpl() {
        this.transactions = new ArrayList<>();
        this.budgets = new ArrayList<>();
    }
    
    @Override
    public void addTransaction(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID().toString());
        }
        transactions.add(transaction);
    }
    
    @Override
    public void deleteTransaction(String id) {
        transactions.removeIf(t -> t.getId().equals(id));
    }
    
    @Override
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }
    
    @Override
    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(t -> !t.getDate().toLocalDate().isBefore(startDate) && !t.getDate().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());
    }
    
    @Override
    public double getBalance(LocalDate startDate, LocalDate endDate) {
        return getTotalIncome(startDate, endDate) - getTotalExpenses(startDate, endDate);
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
    public void addBudget(Budget budget) {
        if (budget.getId() == null) {
            budget.setId(UUID.randomUUID().toString());
        }
        budgets.add(budget);
    }
    
    @Override
    public void deleteBudget(String id) {
        budgets.removeIf(b -> b.getId().equals(id));
    }
    
    @Override
    public List<Budget> getBudgets() {
        return new ArrayList<>(budgets);
    }
    
    @Override
    public Budget getBudgetByCategory(String category) {
        return budgets.stream()
                .filter(b -> b.getCategory().equals(category))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public void updateBudget(Budget budget) {
        deleteBudget(budget.getId());
        budgets.add(budget);
    }
} 