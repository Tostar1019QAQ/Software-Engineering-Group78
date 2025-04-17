package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 服务类，用于管理交易数据，作为ImportService的封装器
 */
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final ImportService importService;
    
    public TransactionService(ImportService importService) {
        this.importService = importService;
        logger.info("TransactionService initialized");
    }
    
    /**
     * 获取所有交易数据
     * @return 交易列表
     */
    public List<Transaction> getAllTransactions() {
        return importService.getAllTransactions();
    }
    
    /**
     * 添加新交易
     * @param transaction 要添加的交易
     */
    public void addTransaction(Transaction transaction) {
        importService.addTransaction(transaction);
        logger.info("Added transaction: {}", transaction.getDescription());
    }
    
    /**
     * 获取指定种类的交易数据
     * @param category 交易种类
     * @return 指定种类的交易列表
     */
    public List<Transaction> getTransactionsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getAllTransactions();
        }
        
        return getAllTransactions().stream()
                .filter(t -> category.equalsIgnoreCase(t.getCategory()))
                .toList();
    }
    
    /**
     * 清除所有交易数据并重新加载默认数据
     */
    public void reloadDefaultData() {
        importService.reloadDefaultData();
        logger.info("Reloaded default transaction data");
    }
} 