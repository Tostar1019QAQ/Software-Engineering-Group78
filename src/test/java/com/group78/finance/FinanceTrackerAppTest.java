package com.group78.finance;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.group78.financetracker.model.Bill;
import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.Budget;
import com.group78.financetracker.service.BillService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public class FinanceTrackerAppTest {

    // 测试 Bill 类
    @Test
    void testBill() {
        Bill bill = new Bill(
            "1",                 // id
            "Water Bill",        // name
            new BigDecimal("100.0"),
            LocalDate.now().plusDays(5),
            "Credit Card",       // payment method
            true,                // email notification
            false                // push notification
        );

        // 基本属性测试 
        assertEquals("1", bill.getId());
        assertEquals("Water Bill", bill.getName());
        assertEquals(new BigDecimal("100.0"), bill.getAmount());
        assertEquals("Credit Card", bill.getPaymentMethod());
        assertTrue(bill.hasEmailNotification());
        assertFalse(bill.hasPushNotification());

        // 状态更新测试
        bill.updateStatus();
        assertEquals("Upcoming", bill.getStatus());

        // 状态描述测试
        String statusDescription = bill.getStatusDescription();
        assertTrue(statusDescription.contains("Due in"));
    }

    // 测试 Transaction 类
    @Test
    void testTransaction() {
        Transaction transaction = new Transaction(
            "T1",                   // id
            new BigDecimal("50.0"),  // amount
            "Food",                 // category
            "Grocery Shopping",     // description
            LocalDateTime.now(),
            null,                   // type (assuming null for simplicity)
            "Cash",                 // payment method
            "CNY"                   // currency
        );

        // 基本属性测试
        assertEquals("T1", transaction.getId());
        assertEquals(new BigDecimal("50.0"), transaction.getAmount());
        assertEquals("Food", transaction.getCategory());
        assertEquals("Grocery Shopping", transaction.getDescription());
        assertEquals("Cash", transaction.getPaymentMethod());
        assertEquals("CNY", transaction.getCurrency());

        // toString 方法测试
        String expected = "Transaction{id='T1', amount=50.0 CNY, category='Food', description='Grocery Shopping'";
        assertTrue(transaction.toString().contains(expected));
    }

    // 测试 Budget 类
    @Test
    void testBudget() {
        Budget budget = new Budget(
            YearMonth.now(),
            new BigDecimal("1000.0")
        );

        // 验证总预算
        assertEquals(new BigDecimal("1000.0"), budget.getTotalBudget());

        // 设置类别预算
        budget.setCategoryBudget("Food", new BigDecimal("300.0"));
        budget.setCategoryBudget("Transport", new BigDecimal("200.0"));

        // 记录支出
        budget.recordExpense("Food", new BigDecimal("100.0"));

        // 验证类别剩余预算
        assertEquals(new BigDecimal("200.0"), budget.getCategoryRemaining("Food"));
        assertEquals(new BigDecimal("200.0"), budget.getCategoryRemaining("Transport"));

        // 验证总剩余预算
        assertEquals(new BigDecimal("900.0"), budget.getTotalRemaining());

        // 验证是否超支
        budget.recordExpense("Food", new BigDecimal("250.0"));
        assertTrue(budget.isOverBudget("Food"));

        // 使用百分比计算
        double percentage = budget.getCategoryUsagePercentage("Food");
        assertEquals(116.67, percentage, 0.01);
    }

    // 测试 BillService 类
    @Test
    void testBillService() {
        BillService billService = new BillService();

        Bill bill = new Bill(
            "2",                 // id
            "Electricity Bill",  // name
            new BigDecimal("75.0"),
            LocalDate.now().plusDays(2),
            "Debit Card",        // payment method
            false,               // email notification
            true                 // push notification
        );

        // 添加账单
        billService.addBill(bill);
        List<Bill> allBills = billService.getAllBills();
        assertTrue(allBills.contains(bill));

        // 更新账单金额并重新检索
        bill.setAmount(new BigDecimal("80.0"));
        billService.updateBill(bill);
        Bill updatedBill = allBills.stream()
            .filter(b -> b.getId().equals("2"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedBill);
        assertEquals(new BigDecimal("80.0"), updatedBill.getAmount());

        // 删除账单
        billService.deleteBill("2");
        allBills = billService.getAllBills();
        assertFalse(allBills.contains(bill));
    }
}
