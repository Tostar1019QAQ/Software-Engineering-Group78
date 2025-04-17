package com.group78.financetracker.model;

public enum TransactionType {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
} 