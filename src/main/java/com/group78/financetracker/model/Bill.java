package com.group78.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Bill {
    private String id;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String paymentMethod;
    private boolean emailNotification;
    private boolean pushNotification;
    private String status;

    public Bill(String id, String name, BigDecimal amount, LocalDate dueDate, 
                String paymentMethod, boolean emailNotification, boolean pushNotification) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paymentMethod = paymentMethod;
        this.emailNotification = emailNotification;
        this.pushNotification = pushNotification;
        updateStatus();
    }

    public void updateStatus() {
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        
        if (dueDate.isBefore(today)) {
            this.status = "Overdue";
        } else if (daysUntilDue <= 3) {
            this.status = "Due Soon";
        } else if (daysUntilDue <= 7) {
            this.status = "Upcoming";
        } else {
            this.status = "Normal";
        }
    }

    public String getStatusDescription() {
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        
        if (dueDate.isBefore(today)) {
            return "Overdue since " + dueDate.toString();
        } else if (daysUntilDue == 0) {
            return "Due today";
        } else if (daysUntilDue == 1) {
            return "Due tomorrow";
        } else if (daysUntilDue <= 7) {
            return "Due in " + daysUntilDue + " days";
        } else {
            return "Due on " + dueDate.toString();
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public boolean hasEmailNotification() { return emailNotification; }
    public boolean hasPushNotification() { return pushNotification; }
    public String getStatus() { return status; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDueDate(LocalDate dueDate) { 
        this.dueDate = dueDate;
        updateStatus();
    }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setEmailNotification(boolean emailNotification) { this.emailNotification = emailNotification; }
    public void setPushNotification(boolean pushNotification) { this.pushNotification = pushNotification; }
} 