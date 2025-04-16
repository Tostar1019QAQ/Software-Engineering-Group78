package main.java.com.finance.service;

import main.java.com.finance.model.Transaction;
import main.java.com.finance.model.Budget;
import java.time.LocalDate;
import java.util.List;

public interface FinanceService {
    // Singleton instance
    static FinanceService instance = new FinanceServiceImpl();
    
    static FinanceService getFinanceService() {
        return instance;
    }
    
    // Transaction methods
    void addTransaction(Transaction transaction);
    void deleteTransaction(String id);
    List<Transaction> getTransactions();
    List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate);
    double getBalance(LocalDate startDate, LocalDate endDate);
    double getTotalIncome(LocalDate startDate, LocalDate endDate);
    double getTotalExpenses(LocalDate startDate, LocalDate endDate);
    
    // Budget methods
    void addBudget(Budget budget);
    void deleteBudget(String id);
    List<Budget> getBudgets();
    Budget getBudgetByCategory(String category);
    void updateBudget(Budget budget);
    
    // Analysis operations
    List<String> getCategories();
    double getBudgetProgress(String category);
    
    // Import/Export operations
    void importTransactions(String filePath);
    void exportTransactions(String filePath);
    void importBudgets(String filePath);
    void exportBudgets(String filePath);
} 