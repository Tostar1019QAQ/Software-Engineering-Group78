package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImportService {
    private final AIService aiService;
    
    public ImportService(AIService aiService) {
        this.aiService = aiService;
    }
    
    public List<Transaction> importFromCSV(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        
        try (FileReader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim())) {
            
            for (CSVRecord record : csvParser) {
                try {
                    Transaction transaction = parseTransaction(record);
                    transactions.add(transaction);
                } catch (Exception e) {
                    System.err.println("解析记录时出错: " + e.getMessage());
                }
            }
        }
        
        return transactions;
    }
    
    private Transaction parseTransaction(CSVRecord record) {
        // 解析日期时间
        String dateTimeStr = record.get("交易时间");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // 解析金额
        String amountStr = record.get("金额");
        BigDecimal amount = new BigDecimal(amountStr);
        
        // 获取描述
        String description = record.get("描述");
        
        // 使用AI服务进行分类
        String category = aiService.categorizeTransaction(description);
        
        // 解析交易类型
        String typeStr = record.get("类型");
        TransactionType type = parseTransactionType(typeStr);
        
        // 获取支付方式
        String paymentMethod = record.get("支付方式");
        
        return new Transaction(
            UUID.randomUUID().toString(),
            amount,
            category,
            description,
            dateTime,
            type,
            paymentMethod
        );
    }
    
    private TransactionType parseTransactionType(String typeStr) {
        switch (typeStr.trim().toLowerCase()) {
            case "支出":
                return TransactionType.EXPENSE;
            case "收入":
                return TransactionType.INCOME;
            case "转账":
                return TransactionType.TRANSFER;
            default:
                return TransactionType.EXPENSE; // 默认为支出
        }
    }
}