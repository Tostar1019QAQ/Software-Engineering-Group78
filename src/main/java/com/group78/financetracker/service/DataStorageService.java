package com.group78.financetracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.Budget;
import com.group78.financetracker.model.BillReminder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DataStorageService {
    private static final String DATA_DIR = "data";
    private static final String TRANSACTIONS_FILE = "transactions.json";
    private static final String BUDGETS_FILE = "budgets.json";
    private static final String BILLS_FILE = "bills.json";
    
    private final ObjectMapper objectMapper;
    private final Path dataPath;
    
    public DataStorageService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // 确保数据目录存在
        dataPath = Paths.get(DATA_DIR);
        createDataDirectoryIfNotExists();
    }
    
    private void createDataDirectoryIfNotExists() {
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建数据目录", e);
        }
    }
    
    // 保存交易记录
    public void saveTransactions(List<Transaction> transactions) throws IOException {
        Path filePath = dataPath.resolve(TRANSACTIONS_FILE);
        objectMapper.writeValue(filePath.toFile(), transactions);
    }
    
    // 加载交易记录
    public List<Transaction> loadTransactions() throws IOException {
        Path filePath = dataPath.resolve(TRANSACTIONS_FILE);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(filePath.toFile(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, Transaction.class));
    }
    
    // 保存预算
    public void saveBudgets(List<Budget> budgets) throws IOException {
        Path filePath = dataPath.resolve(BUDGETS_FILE);
        objectMapper.writeValue(filePath.toFile(), budgets);
    }
    
    // 加载预算
    public List<Budget> loadBudgets() throws IOException {
        Path filePath = dataPath.resolve(BUDGETS_FILE);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(filePath.toFile(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, Budget.class));
    }
    
    // 保存账单提醒
    public void saveBillReminders(List<BillReminder> bills) throws IOException {
        Path filePath = dataPath.resolve(BILLS_FILE);
        objectMapper.writeValue(filePath.toFile(), bills);
    }
    
    // 加载账单提醒
    public List<BillReminder> loadBillReminders() throws IOException {
        Path filePath = dataPath.resolve(BILLS_FILE);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(filePath.toFile(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, BillReminder.class));
    }
} 