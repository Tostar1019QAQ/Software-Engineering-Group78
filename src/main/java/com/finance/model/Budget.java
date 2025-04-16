package main.java.com.finance.model;

import java.time.LocalDate;

public class Budget {
    private String id;
    private String category;
    private double amount;
    private double spent;
    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // "MONTHLY", "YEARLY", "CUSTOM"

    public Budget() {
    }

    public Budget(String id, String category, double amount, double spent, LocalDate startDate, LocalDate endDate, String period) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.spent = spent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public double getRemaining() {
        return amount - spent;
    }
} 