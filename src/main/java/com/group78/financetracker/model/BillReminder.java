package com.group78.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillReminder {
    private String id;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;
    private BillStatus status;
    private String description;
    private boolean isRecurring;
    private RecurrencePattern recurrencePattern;

    public BillReminder(String id, String name, BigDecimal amount, LocalDate dueDate,
                       String category, String description, boolean isRecurring,
                       RecurrencePattern recurrencePattern) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.category = category;
        this.status = BillStatus.PENDING;
        this.description = description;
        this.isRecurring = isRecurring;
        this.recurrencePattern = recurrencePattern;
    }

    // 检查账单是否即将到期（3天内）
    public boolean isDueSoon() {
        LocalDate now = LocalDate.now();
        LocalDate threeDaysFromNow = now.plusDays(3);
        return !dueDate.isBefore(now) && !dueDate.isAfter(threeDaysFromNow);
    }

    // 检查账单是否已过期
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) && status == BillStatus.PENDING;
    }

    // 标记账单为已支付
    public void markAsPaid() {
        this.status = BillStatus.PAID;
        if (isRecurring) {
            generateNextBill();
        }
    }

    // 生成下一个周期的账单
    private BillReminder generateNextBill() {
        LocalDate nextDueDate = recurrencePattern.getNextDate(dueDate);
        return new BillReminder(
            generateNextId(),
            this.name,
            this.amount,
            nextDueDate,
            this.category,
            this.description,
            this.isRecurring,
            this.recurrencePattern
        );
    }

    private String generateNextId() {
        // 简单的ID生成逻辑，实际应用中可能需要更复杂的实现
        return id + "_next";
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCategory() { return category; }
    public BillStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public boolean isRecurring() { return isRecurring; }
    public RecurrencePattern getRecurrencePattern() { return recurrencePattern; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(BillStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public void setRecurrencePattern(RecurrencePattern recurrencePattern) { 
        this.recurrencePattern = recurrencePattern; 
    }
} 