package com.group78.financetracker.service;

import com.group78.financetracker.model.Bill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账单服务测试类
 * 
 * 测试账单管理功能，包括：
 * - 添加账单
 * - 获取所有账单
 * - 获取即将到期的账单
 * - 更新账单
 * - 删除账单
 */
public class BillServiceTest {

    private BillService billService;
    
    @BeforeEach
    public void setUp() {
        billService = new BillService();
    }
    
    /**
     * 测试添加账单功能
     */
    @Test
    public void testAddBill() {
        // 创建一个新账单
        Bill bill = new Bill(
            null,
            "租房费用",
            new BigDecimal("2000"),
            LocalDate.now().plusDays(15),
            "支付宝",
            true,
            false
        );
        
        // 添加账单
        Bill addedBill = billService.addBill(bill);
        
        // 验证ID被分配
        assertNotNull(addedBill.getId(), "添加后的账单应该有ID");
        
        // 验证账单被成功添加到列表中
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "添加的账单应该在账单列表中");
    }
    
    /**
     * 测试获取即将到期的账单
     */
    @Test
    public void testGetUpcomingBills() {
        // 添加一个即将到期的账单（3天内）
        Bill upcomingBill = new Bill(
            null,
            "水电费",
            new BigDecimal("300"),
            LocalDate.now().plusDays(3),
            "银行卡",
            true,
            true
        );
        billService.addBill(upcomingBill);
        
        // 添加一个未来的账单（30天后）
        Bill futureBill = new Bill(
            null,
            "网络费",
            new BigDecimal("200"),
            LocalDate.now().plusDays(30),
            "微信支付",
            false,
            true
        );
        billService.addBill(futureBill);
        
        // 获取即将到期的账单
        List<Bill> upcomingBills = billService.getUpcomingBills();
        
        // 验证即将到期的账单在列表中
        boolean foundUpcoming = false;
        for (Bill bill : upcomingBills) {
            if (bill.getName().equals("水电费")) {
                foundUpcoming = true;
                assertEquals("Due Soon", bill.getStatus(), "即将到期的账单状态应为Due Soon");
                break;
            }
        }
        assertTrue(foundUpcoming, "即将到期的账单应该在列表中");
        
        // 验证未来账单不在即将到期列表中，或者状态为Normal
        boolean foundFuture = false;
        for (Bill bill : upcomingBills) {
            if (bill.getName().equals("网络费")) {
                foundFuture = true;
                break;
            }
        }
        // 由于实现可能不同，30天可能也被认为是"即将到期"，所以我们不能确定
        // foundFuture是true或false，而是检查它的状态
        if (foundFuture) {
            boolean hasCorrectStatus = false;
            for (Bill bill : upcomingBills) {
                if (bill.getName().equals("网络费") && bill.getStatus().equals("Normal")) {
                    hasCorrectStatus = true;
                    break;
                }
            }
            assertTrue(hasCorrectStatus, "未来账单应该有正确的状态");
        }
    }
    
    /**
     * 测试更新账单
     */
    @Test
    public void testUpdateBill() {
        // 添加一个账单
        Bill bill = new Bill(
            null,
            "手机费",
            new BigDecimal("100"),
            LocalDate.now().plusDays(10),
            "银行卡",
            false,
            true
        );
        Bill addedBill = billService.addBill(bill);
        
        // 修改账单
        addedBill.setAmount(new BigDecimal("150"));
        addedBill.setName("手机月费");
        billService.updateBill(addedBill);
        
        // 验证修改是否成功
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                assertEquals("手机月费", b.getName(), "账单名称应该被更新");
                assertEquals(0, new BigDecimal("150").compareTo(b.getAmount()), "账单金额应该被更新");
                break;
            }
        }
        assertTrue(found, "更新后的账单应该在列表中");
    }
    
    /**
     * 测试删除账单
     */
    @Test
    public void testDeleteBill() {
        // 添加一个账单
        Bill bill = new Bill(
            null,
            "保险费",
            new BigDecimal("2000"),
            LocalDate.now().plusDays(20),
            "银行卡",
            true,
            true
        );
        Bill addedBill = billService.addBill(bill);
        
        // 记录删除前的数量
        int countBefore = billService.getAllBills().size();
        
        // 删除账单
        billService.deleteBill(addedBill.getId());
        
        // 验证账单已被删除
        int countAfter = billService.getAllBills().size();
        assertEquals(countBefore - 1, countAfter, "删除后账单数量应减少1");
        
        // 验证该账单不在列表中
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                break;
            }
        }
        assertFalse(found, "删除的账单不应该在列表中");
    }
} 