package com.group78.financetracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预算服务测试类
 * 
 * 测试预算管理功能，包括：
 * - 设置总预算
 * - 获取预算信息
 * - 计算预算使用情况
 */
public class BudgetServiceTest {

    private DashboardService dashboardService;
    
    @BeforeEach
    public void setUp() {
        dashboardService = new DashboardService();
    }
    
    /**
     * 测试更新总预算功能
     */
    @Test
    public void testUpdateTotalBudget() {
        // 设置一个预算值和期限
        BigDecimal budgetAmount = new BigDecimal("5000");
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(1);
        
        // 更新预算
        dashboardService.updateTotalBudget(budgetAmount, startDate, endDate);
        
        // 验证预算值被正确设置
        assertEquals(0, budgetAmount.compareTo(dashboardService.getTotalBudget()),
            "总预算金额应该被正确设置");
        
        // 验证日期范围被正确设置
        assertEquals(startDate, dashboardService.getBudgetStartDate(),
            "预算开始日期应该被正确设置");
        assertEquals(endDate, dashboardService.getBudgetEndDate(),
            "预算结束日期应该被正确设置");
    }
    
    /**
     * 测试计算余额功能
     */
    @Test
    public void testCalculateBalance() {
        // 因为我们使用的是模拟数据，所以总收入和总支出是由测试数据决定的
        // 在这里我们验证余额计算公式是否正确
        
        BigDecimal totalIncome = dashboardService.getTotalIncome();
        BigDecimal totalExpenses = dashboardService.getTotalExpenses();
        BigDecimal expectedBalance = totalIncome.subtract(totalExpenses);
        
        BigDecimal actualBalance = dashboardService.getBalance();
        
        assertEquals(0, expectedBalance.compareTo(actualBalance),
            "余额应该等于总收入减总支出");
    }
    
    /**
     * 测试默认预算值
     */
    @Test
    public void testDefaultBudgetValues() {
        // 重新初始化服务，确保使用默认值
        dashboardService = new DashboardService();
        
        // 验证默认预算值是否为零
        assertEquals(0, BigDecimal.ZERO.compareTo(dashboardService.getTotalBudget()),
            "默认总预算应该为零");
        
        // 验证默认日期值是否为null
        assertNull(dashboardService.getBudgetStartDate(),
            "默认预算开始日期应该为null");
        assertNull(dashboardService.getBudgetEndDate(),
            "默认预算结束日期应该为null");
    }
} 