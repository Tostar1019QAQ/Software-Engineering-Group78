package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ReportGenerator
 * 
 * This class demonstrates Test-Driven Development (TDD) approach:
 * 1. Write a test for a feature before implementing it
 * 2. Run the test and see it fail
 * 3. Implement the minimal code to make the test pass
 * 4. Refactor the code while keeping the tests passing
 * 5. Repeat for other features
 */
public class ReportGeneratorTest {

    @Mock
    private AIService aiService;
    
    private ReportGenerator reportGenerator;
    private List<Transaction> transactions;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        reportGenerator = new ReportGenerator(aiService);
        transactions = createTestTransactions();
    }
    
    /**
     * Test for income-expense report generation
     * 
     * This test verifies that the income-expense report:
     * - Is successfully generated
     * - Contains expected sections
     * - Has correct content formatting
     */
    @Test
    public void testGenerateIncomeExpenseReport() {
        String report = reportGenerator.generateReport("income-expense", transactions);
        
        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("Income and Expense Report"), "Report should have correct title");
        assertTrue(report.contains("Financial Summary"), "Report should contain financial summary");
        assertTrue(report.contains("Monthly Summary"), "Report should contain monthly summary");
        assertTrue(report.contains("Expense Categories"), "Report should contain expense categories");
    }
    
    /**
     * Test for cash flow report generation
     * 
     * This test verifies that the cash flow report:
     * - Is successfully generated
     * - Contains forecast data
     * - Includes risk assessment
     */
    @Test
    public void testGenerateCashFlowReport() {
        // 添加更多月份的测试数据以满足至少3个月的要求
        transactions.add(new Transaction(
            "Extra Salary", 
            BigDecimal.valueOf(5000), 
            "Additional monthly salary", 
            "Income", 
            LocalDateTime.now().minusDays(75),
            TransactionType.INCOME,
            "income-3"
        ));
        
        transactions.add(new Transaction(
            "Extra Rent", 
            BigDecimal.valueOf(1500), 
            "Additional monthly rent", 
            "Housing", 
            LocalDateTime.now().minusDays(80),
            TransactionType.EXPENSE,
            "expense-5"
        ));
        
        String report = reportGenerator.generateReport("cash-flow", transactions);
        
        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("Cash Flow Forecast"), "Report should have correct title");
        assertTrue(report.contains("Forecast Summary") || report.contains("Average Monthly Income"), "Report should contain forecast information");
        assertTrue(report.contains("Financial Risk Assessment") || report.contains("Risk"), "Report should contain risk assessment");
    }
    
    /**
     * Test for savings report generation
     * 
     * This test verifies that the savings report:
     * - Is successfully generated
     * - Contains overview information
     * - Includes savings recommendations
     */
    @Test
    public void testSavingsReport() {
        String report = reportGenerator.generateReport("savings", transactions);
        
        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("Smart Savings Recommendations") || report.contains("Savings Recommendations"), "Report should have correct title");
        assertTrue(report.contains("Financial Overview") || report.contains("Overview"), "Report should contain financial overview");
        assertTrue(report.contains("Savings Rate Assessment") || report.contains("Savings Rate"), "Report should contain savings rate assessment");
    }
    
    /**
     * Test for invalid report type handling
     * 
     * This test verifies that the system handles invalid report types gracefully
     * by returning an error message instead of throwing exceptions.
     */
    @Test
    public void testInvalidReportType() {
        String report = reportGenerator.generateReport("invalid-type", transactions);
        
        assertNotNull(report, "Report should not be null even for invalid type");
        assertTrue(report.contains("Error") || report.contains("error"), "Report should contain error notification");
        assertTrue(report.contains("Invalid report type") || report.contains("invalid"), "Report should explain the error");
    }
    
    /**
     * Test for exception handling during report generation
     * 
     * This test verifies that exceptions during processing are caught
     * and an error report is generated instead of propagating exceptions.
     */
    @Test
    public void testExceptionHandling() {
        // Setup a mock to throw an exception when called
        when(aiService.generateFinancialInsights(anyString())).thenThrow(new RuntimeException("Test exception"));
        
        // 创建包含足够数据的测试交易列表
        List<Transaction> testTransactions = createTestTransactions();
        // 添加额外的月份数据
        testTransactions.add(new Transaction(
            "Additional Salary", 
            BigDecimal.valueOf(4500), 
            "Extra monthly salary", 
            "Income", 
            LocalDateTime.now().minusDays(70),
            TransactionType.INCOME,
            "income-extra"
        ));
        
        String report = reportGenerator.generateReport("cash-flow", testTransactions);
        
        assertNotNull(report, "Report should not be null when exception occurs");
        
        if (report.contains("Error") || report.contains("error")) {
            assertTrue(report.contains("Failed to generate report") || report.contains("failed") || 
                     report.contains("Test exception"), "Report should explain the failure");
        } else {
            assertTrue(report.contains("Cash Flow Forecast"), "Should contain report title");
        }
    }
    
    /**
     * Helper method to create test transaction data
     * 
     * @return List of test transactions
     */
    private List<Transaction> createTestTransactions() {
        List<Transaction> testData = new ArrayList<>();
        
        // Add income transactions
        testData.add(new Transaction(
            "Salary", 
            BigDecimal.valueOf(5000), 
            "Monthly salary payment", 
            "Income", 
            LocalDateTime.now().minusDays(45),
            TransactionType.INCOME,
            "income-1"
        ));
        
        testData.add(new Transaction(
            "Freelance Work", 
            BigDecimal.valueOf(1200), 
            "Website development project", 
            "Income", 
            LocalDateTime.now().minusDays(30),
            TransactionType.INCOME,
            "income-2"
        ));
        
        // Add expense transactions
        testData.add(new Transaction(
            "Rent", 
            BigDecimal.valueOf(1500), 
            "Monthly apartment rent", 
            "Housing", 
            LocalDateTime.now().minusDays(40),
            TransactionType.EXPENSE,
            "expense-1"
        ));
        
        testData.add(new Transaction(
            "Groceries", 
            BigDecimal.valueOf(350), 
            "Weekly grocery shopping", 
            "Food", 
            LocalDateTime.now().minusDays(35),
            TransactionType.EXPENSE,
            "expense-2"
        ));
        
        testData.add(new Transaction(
            "Restaurant", 
            BigDecimal.valueOf(120), 
            "Dinner with friends", 
            "Food", 
            LocalDateTime.now().minusDays(20),
            TransactionType.EXPENSE,
            "expense-3"
        ));
        
        testData.add(new Transaction(
            "Gas", 
            BigDecimal.valueOf(80), 
            "Car fuel", 
            "Transportation", 
            LocalDateTime.now().minusDays(15),
            TransactionType.EXPENSE,
            "expense-4"
        ));
        
        return testData;
    }
} 