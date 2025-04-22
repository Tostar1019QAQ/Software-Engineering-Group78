package com.group78.financetracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.group78.financetracker.model.Bill;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BillService {
    private static final Logger logger = LoggerFactory.getLogger(BillService.class);
    private static final String DATA_DIR = "data";
    private static final String BILLS_JSON_FILE = "bills.json";
    private static final String BILLS_CSV_FILE = "bills.csv";
    private final ObjectMapper objectMapper;
    private final Path dataPath;
    private List<Bill> bills;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BillService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        dataPath = Paths.get(DATA_DIR);
        bills = new ArrayList<>();
        createDataDirectoryIfNotExists();
        
        // Try to load from CSV first, then fall back to JSON if CSV doesn't exist
        if (!loadBillsFromCSV()) {
            loadBillsFromJSON();
            // Convert to CSV format for future use
            saveBillsToCSV();
        }
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
    
    /**
     * Loads bills from CSV file
     * @return true if loaded successfully, false otherwise
     */
    private boolean loadBillsFromCSV() {
        Path filePath = dataPath.resolve(BILLS_CSV_FILE);
        if (!Files.exists(filePath)) {
            logger.info("Bills CSV file not found: {}", filePath);
            return false;
        }
        
        try (FileReader reader = new FileReader(filePath.toFile());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim())) {
            
            bills.clear();
            
            for (CSVRecord record : csvParser) {
                try {
                    String id = record.get("ID");
                    String name = record.get("Name");
                    BigDecimal amount = new BigDecimal(record.get("Amount"));
                    LocalDate dueDate = LocalDate.parse(record.get("DueDate"), dateFormatter);
                    String paymentMethod = record.get("PaymentMethod");
                    boolean emailNotification = Boolean.parseBoolean(record.get("EmailNotification"));
                    boolean pushNotification = Boolean.parseBoolean(record.get("PushNotification"));
                    
                    Bill bill = new Bill(id, name, amount, dueDate, paymentMethod, 
                                        emailNotification, pushNotification);
                    bills.add(bill);
                } catch (DateTimeParseException | NumberFormatException e) {
                    logger.error("Error parsing bill record: {}", e.getMessage());
                }
            }
            
            logger.info("Loaded {} bills from CSV file", bills.size());
            return true;
        } catch (IOException e) {
            logger.error("Failed to load bills from CSV: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Saves bills to CSV file
     */
    private void saveBillsToCSV() {
        Path filePath = dataPath.resolve(BILLS_CSV_FILE);
        
        try (FileWriter writer = new FileWriter(filePath.toFile());
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("ID", "Name", "Amount", "DueDate", "PaymentMethod", 
                            "EmailNotification", "PushNotification"))) {
            
            for (Bill bill : bills) {
                csvPrinter.printRecord(
                    bill.getId(),
                    bill.getName(),
                    bill.getAmount().toString(),
                    bill.getDueDate().format(dateFormatter),
                    bill.getPaymentMethod(),
                    bill.hasEmailNotification(),
                    bill.hasPushNotification()
                );
            }
            
            csvPrinter.flush();
            logger.info("Saved {} bills to CSV file", bills.size());
        } catch (IOException e) {
            logger.error("Failed to save bills to CSV: {}", e.getMessage());
        }
    }

    /**
     * Loads bills from JSON file
     */
    private void loadBillsFromJSON() {
        Path filePath = dataPath.resolve(BILLS_JSON_FILE);
        if (Files.exists(filePath)) {
            try {
                bills = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<List<Bill>>() {});
                // Update all bill statuses after loading
                bills.forEach(Bill::updateStatus);
                logger.info("Loaded {} bills from JSON file", bills.size());
            } catch (IOException e) {
                logger.error("Failed to load bills from JSON: {}", e.getMessage());
                bills = new ArrayList<>();
            }
        }
    }

    /**
     * Saves bills to JSON file (legacy support)
     */
    private void saveBillsToJSON() {
        try {
            Path filePath = dataPath.resolve(BILLS_JSON_FILE);
            objectMapper.writeValue(filePath.toFile(), bills);
            logger.info("Saved bills to JSON file");
        } catch (IOException e) {
            logger.error("Failed to save bills to JSON: {}", e.getMessage());
        }
    }
    
    /**
     * Save bills to both CSV and JSON formats
     */
    private void saveBills() {
        saveBillsToCSV();
        saveBillsToJSON(); // Keep JSON for backward compatibility
    }
    
    /**
     * Reload bills data from CSV file
     */
    public void reloadBills() {
        logger.info("正在重新加载账单数据...");
        // 首先尝试从CSV加载
        boolean csvLoaded = loadBillsFromCSV();
        
        // 如果CSV加载失败，尝试从JSON加载
        if (!csvLoaded) {
            logger.info("CSV加载失败，尝试从JSON加载");
            loadBillsFromJSON();
        }
        
        // 更新所有账单的状态
        bills.forEach(Bill::updateStatus);
        
        logger.info("账单数据重新加载完成，共 {} 条账单记录", bills.size());
    }
} 