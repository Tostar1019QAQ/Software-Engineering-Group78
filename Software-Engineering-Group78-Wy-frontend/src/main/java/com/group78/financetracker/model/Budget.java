package com.group78.financetracker.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class Budget {
    private YearMonth period;
    private BigDecimal totalBudget;
    private Map<String, BigDecimal> categoryBudgets;
    private Map<String, BigDecimal> categorySpent;

    public Budget(YearMonth period, BigDecimal totalBudget) {
        this.period = period;
        this.totalBudget = totalBudget;
        this.categoryBudgets = new HashMap<>();
        this.categorySpent = new HashMap<>();
    }

    // 设置某个类别的预算
    public void setCategoryBudget(String category, BigDecimal amount) {
        categoryBudgets.put(category, amount);
    }

    // 记录某个类别的支出
    public void recordExpense(String category, BigDecimal amount) {
        BigDecimal currentSpent = categorySpent.getOrDefault(category, BigDecimal.ZERO);
        categorySpent.put(category, currentSpent.add(amount));
    }

    // 获取某个类别的剩余预算
    public BigDecimal getCategoryRemaining(String category) {
        BigDecimal budget = categoryBudgets.getOrDefault(category, BigDecimal.ZERO);
        BigDecimal spent = categorySpent.getOrDefault(category, BigDecimal.ZERO);
        return budget.subtract(spent);
    }

    // 获取总剩余预算
    public BigDecimal getTotalRemaining() {
        BigDecimal totalSpent = categorySpent.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalBudget.subtract(totalSpent);
    }

    // 检查某个类别是否超支
    public boolean isOverBudget(String category) {
        return getCategoryRemaining(category).compareTo(BigDecimal.ZERO) < 0;
    }

    // 获取某个类别的预算使用百分比
    public double getCategoryUsagePercentage(String category) {
        BigDecimal budget = categoryBudgets.getOrDefault(category, BigDecimal.ZERO);
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        BigDecimal spent = categorySpent.getOrDefault(category, BigDecimal.ZERO);
        return spent.divide(budget, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"))
            .doubleValue();
    }

    // Getters
    public YearMonth getPeriod() { return period; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public Map<String, BigDecimal> getCategoryBudgets() { return new HashMap<>(categoryBudgets); }
    public Map<String, BigDecimal> getCategorySpent() { return new HashMap<>(categorySpent); }

    // Setters
    public void setPeriod(YearMonth period) { this.period = period; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
} 