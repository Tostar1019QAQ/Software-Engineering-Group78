package com.finance.service.impl;

import com.finance.model.Transaction;
import com.finance.model.Budget;
import com.finance.service.DataStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileDataStorageService implements DataStorageService {
    private static final String DATA_DIR = "data";
    private static final String TRANSACTIONS_FILE = "transactions.json";
    private static final String BUDGETS_FILE = "budgets.json";
    
    private final ObjectMapper objectMapper;
    private final File transactionsFile;
    private final File budgetsFile;
    
    public FileDataStorageService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        transactionsFile = new File(dataDir, TRANSACTIONS_FILE);
        budgetsFile = new File(dataDir, BUDGETS_FILE);
    }
    
    @Override
    public void initializeStorage() {
        try {
            if (!transactionsFile.exists()) {
                transactionsFile.createNewFile();
            }
            if (!budgetsFile.exists()) {
                budgetsFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }
    
    @Override
    public void saveTransaction(Transaction transaction) {
        try {
            List<Transaction> transactions = getAllTransactions();
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID().toString());
            }
            transactions.add(transaction);
            objectMapper.writeValue(transactionsFile, transactions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save transaction", e);
        }
    }
    
    @Override
    public List<Transaction> getAllTransactions() {
        try {
            if (!transactionsFile.exists() || transactionsFile.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(transactionsFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Transaction.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read transactions", e);
        }
    }
    
    @Override
    public Transaction getTransactionById(String id) {
        return getAllTransactions().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public void updateTransaction(Transaction transaction) {
        List<Transaction> transactions = getAllTransactions();
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(transaction.getId())) {
                transactions.set(i, transaction);
                try {
                    objectMapper.writeValue(transactionsFile, transactions);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to update transaction", e);
                }
                return;
            }
        }
    }
    
    @Override
    public void deleteTransaction(String id) {
        List<Transaction> transactions = getAllTransactions();
        transactions.removeIf(t -> t.getId().equals(id));
        try {
            objectMapper.writeValue(transactionsFile, transactions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete transaction", e);
        }
    }
    
    @Override
    public void saveBudget(Budget budget) {
        try {
            List<Budget> budgets = getAllBudgets();
            if (budget.getId() == null) {
                budget.setId(UUID.randomUUID().toString());
            }
            budgets.add(budget);
            objectMapper.writeValue(budgetsFile, budgets);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save budget", e);
        }
    }
    
    @Override
    public List<Budget> getAllBudgets() {
        try {
            if (!budgetsFile.exists() || budgetsFile.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(budgetsFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Budget.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read budgets", e);
        }
    }
    
    @Override
    public Budget getBudgetById(String id) {
        return getAllBudgets().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public void updateBudget(Budget budget) {
        List<Budget> budgets = getAllBudgets();
        for (int i = 0; i < budgets.size(); i++) {
            if (budgets.get(i).getId().equals(budget.getId())) {
                budgets.set(i, budget);
                try {
                    objectMapper.writeValue(budgetsFile, budgets);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to update budget", e);
                }
                return;
            }
        }
    }
    
    @Override
    public void deleteBudget(String id) {
        List<Budget> budgets = getAllBudgets();
        budgets.removeIf(b -> b.getId().equals(id));
        try {
            objectMapper.writeValue(budgetsFile, budgets);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete budget", e);
        }
    }
    
    @Override
    public void backupData() {
        // TODO: Implement backup functionality
    }
    
    @Override
    public void restoreData() {
        // TODO: Implement restore functionality
    }
} 