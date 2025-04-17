package com.group78.financetracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.group78.financetracker.model.Bill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BillService {
    private static final String DATA_DIR = "data";
    private static final String BILLS_FILE = "bills.json";
    private final ObjectMapper objectMapper;
    private final Path dataPath;
    private List<Bill> bills;

    public BillService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        dataPath = Paths.get(DATA_DIR);
        bills = new ArrayList<>();
        createDataDirectoryIfNotExists();
        loadBills();
    }

    private void createDataDirectoryIfNotExists() {
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory", e);
        }
    }

    public List<Bill> getAllBills() {
        // Update status for all bills
        bills.forEach(Bill::updateStatus);
        
        // Sort bills by due date
        return bills.stream()
            .sorted(Comparator.comparing(Bill::getDueDate))
            .collect(Collectors.toList());
    }

    public List<Bill> getUpcomingBills() {
        // Update status for all bills and filter
        return bills.stream()
            .peek(Bill::updateStatus)
            .filter(bill -> !bill.getStatus().equals("Normal"))
            .sorted(Comparator.comparing(Bill::getDueDate))
            .collect(Collectors.toList());
    }

    public Bill addBill(Bill bill) {
        if (bill.getId() == null || bill.getId().isEmpty()) {
            bill.setId(UUID.randomUUID().toString());
        }
        bills.add(bill);
        saveBills();
        return bill;
    }

    public void updateBill(Bill updatedBill) {
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(updatedBill.getId())) {
                bills.set(i, updatedBill);
                break;
            }
        }
        saveBills();
    }

    public void deleteBill(String billId) {
        bills.removeIf(bill -> bill.getId().equals(billId));
        saveBills();
    }

    private void loadBills() {
        Path filePath = dataPath.resolve(BILLS_FILE);
        if (Files.exists(filePath)) {
            try {
                bills = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<List<Bill>>() {});
                // 加载后立即更新所有账单状态
                bills.forEach(Bill::updateStatus);
            } catch (IOException e) {
                e.printStackTrace();
                bills = new ArrayList<>();
            }
        }
    }

    private void saveBills() {
        try {
            Path filePath = dataPath.resolve(BILLS_FILE);
            objectMapper.writeValue(filePath.toFile(), bills);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 