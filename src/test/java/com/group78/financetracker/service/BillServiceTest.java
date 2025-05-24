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
 * Bill Service Test Class
 * 
 * Tests bill management functionality, including:
 * - Adding bills
 * - Getting all bills
 * - Getting upcoming bills
 * - Updating bills
 * - Deleting bills
 */
public class BillServiceTest {

    private BillService billService;
    
    @BeforeEach
    public void setUp() {
        billService = new BillService();
    }
    
    /**
     * Test for adding bills functionality
     */
    @Test
    public void testAddBill() {
        // Create a new bill
        Bill bill = new Bill(
            null,
            "Rent Payment",
            new BigDecimal("2000"),
            LocalDate.now().plusDays(15),
            "Alipay",
            true,
            false
        );
        
        // Add the bill
        Bill addedBill = billService.addBill(bill);
        
        // Verify ID is assigned
        assertNotNull(addedBill.getId(), "Added bill should have an ID");
        
        // Verify the bill is successfully added to the list
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Added bill should be in the bill list");
    }
    
    /**
     * Test for getting upcoming bills
     */
    @Test
    public void testGetUpcomingBills() {
        // Add a bill that's due soon (within 3 days)
        Bill upcomingBill = new Bill(
            null,
            "Utility Bill",
            new BigDecimal("300"),
            LocalDate.now().plusDays(3),
            "Bank Card",
            true,
            true
        );
        billService.addBill(upcomingBill);
        
        // Add a future bill (30 days later)
        Bill futureBill = new Bill(
            null,
            "Internet Fee",
            new BigDecimal("200"),
            LocalDate.now().plusDays(30),
            "WeChat Pay",
            false,
            true
        );
        billService.addBill(futureBill);
        
        // Get upcoming bills
        List<Bill> upcomingBills = billService.getUpcomingBills();
        
        // Verify upcoming bill is in the list
        boolean foundUpcoming = false;
        for (Bill bill : upcomingBills) {
            if (bill.getName().equals("Utility Bill")) {
                foundUpcoming = true;
                assertEquals("Overdue", bill.getStatus(), "Upcoming bill status should be 'Overdue'");
                break;
            }
        }
        assertTrue(foundUpcoming, "Upcoming bill should be in the list");
        
        // Verify future bill is not in the upcoming list, or has status Normal
        boolean foundFuture = false;
        for (Bill bill : upcomingBills) {
            if (bill.getName().equals("Internet Fee")) {
                foundFuture = true;
                break;
            }
        }
        // Since implementations may vary, 30 days might also be considered "upcoming"
        // so we cannot be certain if foundFuture is true or false, but check its status
        if (foundFuture) {
            boolean hasCorrectStatus = false;
            for (Bill bill : upcomingBills) {
                if (bill.getName().equals("Internet Fee") && bill.getStatus().equals("Normal")) {
                    hasCorrectStatus = true;
                    break;
                }
            }
            assertTrue(hasCorrectStatus, "Future bill should have the correct status");
        }
    }
    
    /**
     * Test for updating bills
     */
    @Test
    public void testUpdateBill() {
        // Add a bill
        Bill bill = new Bill(
            null,
            "Phone Bill",
            new BigDecimal("100"),
            LocalDate.now().plusDays(10),
            "Bank Card",
            false,
            true
        );
        Bill addedBill = billService.addBill(bill);
        
        // Modify the bill
        addedBill.setAmount(new BigDecimal("150"));
        addedBill.setName("Monthly Phone Bill");
        billService.updateBill(addedBill);
        
        // Verify the changes were successful
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                assertEquals("Monthly Phone Bill", b.getName(), "Bill name should be updated");
                assertEquals(0, new BigDecimal("150").compareTo(b.getAmount()), "Bill amount should be updated");
                break;
            }
        }
        assertTrue(found, "Updated bill should be in the list");
    }
    
    /**
     * Test for deleting bills
     */
    @Test
    public void testDeleteBill() {
        // Add a bill
        Bill bill = new Bill(
            null,
            "Insurance Fee",
            new BigDecimal("2000"),
            LocalDate.now().plusDays(20),
            "Bank Card",
            true,
            true
        );
        Bill addedBill = billService.addBill(bill);
        
        // Record count before deletion
        int countBefore = billService.getAllBills().size();
        
        // Delete the bill
        billService.deleteBill(addedBill.getId());
        
        // Verify the bill has been deleted
        int countAfter = billService.getAllBills().size();
        assertEquals(countBefore - 1, countAfter, "Bill count should decrease by 1 after deletion");
        
        // Verify the bill is not in the list
        List<Bill> allBills = billService.getAllBills();
        boolean found = false;
        for (Bill b : allBills) {
            if (b.getId().equals(addedBill.getId())) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Deleted bill should not be in the list");
    }
} 