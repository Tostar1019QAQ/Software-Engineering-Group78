package com.group78.financetracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Budget Service Test Class
 * 
 * Tests budget management functionality, including:
 * - Setting total budget
 * - Getting budget information
 * - Calculating budget usage
 */
public class BudgetServiceTest {

    private DashboardService dashboardService;
    
    @BeforeEach
    public void setUp() {
        dashboardService = new DashboardService();
    }
    
    /**
     * Test for updating total budget functionality
     */
    @Test
    public void testUpdateTotalBudget() {
        // Set a budget value and period
        BigDecimal budgetAmount = new BigDecimal("5000");
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(1);
        
        // Update budget
        dashboardService.updateTotalBudget(budgetAmount, startDate, endDate);
        
        // Verify budget value is correctly set
        assertEquals(0, budgetAmount.compareTo(dashboardService.getTotalBudget()),
            "Total budget amount should be set correctly");
        
        // Verify date range is correctly set
        assertEquals(startDate, dashboardService.getBudgetStartDate(),
            "Budget start date should be set correctly");
        assertEquals(endDate, dashboardService.getBudgetEndDate(),
            "Budget end date should be set correctly");
    }
    
    /**
     * Test for calculating balance functionality
     */
    @Test
    public void testCalculateBalance() {
        // Since we use mock data, the total income and expenses are determined by test data
        // Here we verify that the balance calculation formula is correct
        
        BigDecimal totalIncome = dashboardService.getTotalIncome();
        BigDecimal totalExpenses = dashboardService.getTotalExpenses();
        BigDecimal expectedBalance = totalIncome.subtract(totalExpenses);
        
        BigDecimal actualBalance = dashboardService.getBalance();
        
        assertEquals(0, expectedBalance.compareTo(actualBalance),
            "Balance should equal total income minus total expenses");
    }
    
    /**
     * Test for default budget values
     */
    @Test
    public void testDefaultBudgetValues() {
        // Reinitialize service to ensure default values
        dashboardService = new DashboardService();
        
        // Verify default budget value is zero
        assertEquals(0, BigDecimal.ZERO.compareTo(dashboardService.getTotalBudget()),
            "Default total budget should be zero");
        
        // Verify default date values are null
        assertNull(dashboardService.getBudgetStartDate(),
            "Default budget start date should be null");
        assertNull(dashboardService.getBudgetEndDate(),
            "Default budget end date should be null");
    }
} 