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
import java.util.List;
import java.util.UUID;

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
        return new ArrayList<>(bills);
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

    public List<Bill> getUpcomingBills() {
        List<Bill> upcomingBills = new ArrayList<>();
        for (Bill bill : bills) {
            bill.updateStatus();
            if (bill.getStatus().equals("Due Soon") || bill.getStatus().equals("Overdue")) {
                upcomingBills.add(bill);
            }
        }
        return upcomingBills;
    }
} 