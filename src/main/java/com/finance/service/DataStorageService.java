package com.finance.service;

import com.finance.model.Transaction;
import com.finance.model.Budget;
import java.util.List;

public interface DataStorageService {
    // Transaction operations
    void saveTransaction(Transaction transaction);
    List<Transaction> getAllTransactions();
    Transaction getTransactionById(String id);
    void updateTransaction(Transaction transaction);
    void deleteTransaction(String id);
    
    // Budget operations
    void saveBudget(Budget budget);
    List<Budget> getAllBudgets();
    Budget getBudgetById(String id);
    void updateBudget(Budget budget);
    void deleteBudget(String id);
    
    // File operations
    void initializeStorage();
    void backupData();
    void restoreData();
} 