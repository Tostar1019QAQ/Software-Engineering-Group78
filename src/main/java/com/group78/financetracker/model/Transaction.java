package com.group78.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String id;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDateTime dateTime;
    private TransactionType type;
    private String paymentMethod;
    private String currency;

    public Transaction(String id, BigDecimal amount, String category, String description,
                      LocalDateTime dateTime, TransactionType type, String paymentMethod) {
        this(id, amount, category, description, dateTime, type, paymentMethod, "CNY");
    }

    public Transaction(String id, BigDecimal amount, String category, String description,
                      LocalDateTime dateTime, TransactionType type, String paymentMethod, String currency) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.dateTime = dateTime;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.currency = currency;
    }

    // Getters
    public String getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDateTime getDateTime() { return dateTime; }
    public TransactionType getType() { return type; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getCurrency() { return currency; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public void setType(TransactionType type) { this.type = type; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setCurrency(String currency) { this.currency = currency; }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', amount=%s %s, category='%s', description='%s', " +
                           "dateTime=%s, type=%s, paymentMethod='%s'}", 
                           id, amount, currency, category, description, dateTime, type, paymentMethod);
    }
} 