package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
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
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ImportService {
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int BATCH_SIZE = 1000;
    private static final String DEFAULT_DATA_FILE = "data/default_transactions.csv";
    private List<Transaction> transactions;
    private List<ImportError> importErrors;

    public static class ImportError {
        private final int lineNumber;
        private final String message;

        public ImportError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }
    }

    public ImportService() {
        this.transactions = new ArrayList<>();
        this.importErrors = new ArrayList<>();
        loadDefaultTransactions();
    }

    private void loadDefaultTransactions() {
        try {
            File defaultFile = new File(DEFAULT_DATA_FILE);
            if (defaultFile.exists()) {
                List<Transaction> defaultTransactions = importFromCSV(DEFAULT_DATA_FILE);
                logger.info("Loaded {} default transactions", defaultTransactions.size());
            } else {
                logger.warn("Default transactions file not found: {}", DEFAULT_DATA_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load default transactions", e);
        }
    }

    public void setDateFormat(String format) {
        this.dateFormatter = DateTimeFormatter.ofPattern(format);
    }

    public List<ImportError> getImportErrors() {
        return new ArrayList<>(importErrors);
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        saveTransactionsToFile();
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public void clearTransactions() {
        transactions.clear();
        importErrors.clear();
        loadDefaultTransactions();
    }

    public void reloadDefaultData() {
        clearTransactions();
    }

    public List<Transaction> importFromCSV(String filePath) throws IOException {
        List<Transaction> importedTransactions = new ArrayList<>();
        importErrors.clear();
        
        try (FileReader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim())) {
            
            int recordNumber = 1;
            List<Transaction> batch = new ArrayList<>();
            
            for (CSVRecord record : csvParser) {
                try {
                    validateRecord(record);
                    Transaction transaction = parseTransaction(record);
                    batch.add(transaction);
                    
                    if (batch.size() >= BATCH_SIZE) {
                        importedTransactions.addAll(batch);
                        transactions.addAll(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    String errorMessage = String.format("Row %d: %s", recordNumber, e.getMessage());
                    importErrors.add(new ImportError(recordNumber, errorMessage));
                    logger.error(errorMessage, e);
                }
                recordNumber++;
            }
            
            // Process the last batch
            if (!batch.isEmpty()) {
                importedTransactions.addAll(batch);
                transactions.addAll(batch);
            }
        }
        
        // If this is not the default file, save the combined data
        if (!filePath.equals(DEFAULT_DATA_FILE)) {
            saveTransactionsToFile();
        }
        
        return importedTransactions;
    }
    
    private void saveTransactionsToFile() {
        try {
            File defaultFile = new File(DEFAULT_DATA_FILE);
            File directory = defaultFile.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Sort transactions by date for better readability
            List<Transaction> sortedTransactions = new ArrayList<>(transactions);
            sortedTransactions.sort(Comparator.comparing(Transaction::getDateTime));
            
            try (FileWriter writer = new FileWriter(DEFAULT_DATA_FILE);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Date", "Description", "Amount", "Category", "Payment Method", "Currency"))) {
                
                for (Transaction transaction : sortedTransactions) {
                    String date = transaction.getDateTime().toLocalDate().format(dateFormatter);
                    String description = transaction.getDescription();
                    BigDecimal amount = transaction.getAmount();
                    if (transaction.getType() == TransactionType.EXPENSE) {
                        amount = amount.negate();
                    }
                    
                    csvPrinter.printRecord(
                        date,
                        description,
                        amount.toString(),
                        transaction.getCategory(),
                        transaction.getPaymentMethod(),
                        transaction.getCurrency()
                    );
                }
                
                csvPrinter.flush();
                logger.info("Saved {} transactions to default file", transactions.size());
            }
        } catch (IOException e) {
            logger.error("Failed to save transactions to file", e);
        }
    }
    
    private void validateRecord(CSVRecord record) throws IllegalArgumentException {
        validateField(record, "Date", "Date");
        validateField(record, "Amount", "Amount");
        validateField(record, "Category", "Category");
        validateField(record, "Description", "Description");
        validateField(record, "Payment Method", "Payment Method");
        validateField(record, "Currency", "Currency");
        
        // Validate amount format
        try {
            new BigDecimal(record.get("Amount"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format");
        }
    }
    
    private void validateField(CSVRecord record, String fieldName, String displayName) {
        if (!record.isMapped(fieldName)) {
            throw new IllegalArgumentException(String.format("Missing required field: %s", displayName));
        }
        String value = record.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("Field %s cannot be empty", displayName));
        }
    }
    
    private Transaction parseTransaction(CSVRecord record) {
        // Parse date
        LocalDateTime dateTime;
        try {
            LocalDate date = LocalDate.parse(record.get("Date"), dateFormatter);
            dateTime = date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format, should be: " + dateFormatter.toString());
        }
        
        // Parse amount and determine transaction type
        BigDecimal amount;
        try {
            amount = new BigDecimal(record.get("Amount"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format");
        }
        
        TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0 ? 
            TransactionType.EXPENSE : TransactionType.INCOME;
        
        // Get other fields
        String description = record.get("Description").trim();
        String category = record.get("Category").trim();
        String paymentMethod = record.get("Payment Method").trim();
        String currency = record.get("Currency").trim();
        
        // Create transaction
        return new Transaction(
            UUID.randomUUID().toString(),
            amount.abs(),
            category,
            description,
            dateTime,
            type,
            paymentMethod,
            currency
        );
    }
}