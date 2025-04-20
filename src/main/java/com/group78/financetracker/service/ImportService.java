package com.group78.financetracker.service;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.model.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
            logger.info("Loading default transactions from: {}", DEFAULT_DATA_FILE);
            File defaultFile = new File(DEFAULT_DATA_FILE);
            if (defaultFile.exists()) {
                logger.info("Default transactions file exists, loading...");
                // Clear existing transactions before loading
                transactions.clear();
                
                List<Transaction> defaultTransactions = importFromFile(defaultFile);
                logger.info("Loaded {} default transactions", defaultTransactions.size());
                
                // Print first few transactions for debugging
                int count = 0;
                for (Transaction t : transactions) {
                    if (count++ < 5) {
                        logger.debug("Sample transaction: {} - {} - {}", 
                                   t.getDescription(), t.getAmount(), t.getDateTime().toLocalDate());
                    }
                }
            } else {
                logger.warn("Default transactions file not found: {}", DEFAULT_DATA_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load default transactions: {}", e.getMessage(), e);
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

    /**
     * Determines the file type and imports accordingly
     * @param file File to import
     * @return List of imported transactions
     * @throws IOException If an error occurs during import
     */
    public List<Transaction> importFromFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".csv")) {
            return importFromCSV(file.getAbsolutePath());
        } else if (fileName.endsWith(".xlsx")) {
            return importFromXLSX(file.getAbsolutePath());
        } else {
            throw new IOException("Unsupported file format. Please use CSV or XLSX files.");
        }
    }

    /**
     * Import transactions from XLSX file
     * @param filePath Path to the XLSX file
     * @return List of imported transactions
     * @throws IOException If an error occurs during import
     */
    public List<Transaction> importFromXLSX(String filePath) throws IOException {
        List<Transaction> importedTransactions = new ArrayList<>();
        importErrors.clear();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row to determine columns
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel file does not contain a header row");
            }
            
            // Map column indices
            int dateColIdx = -1;
            int descColIdx = -1;
            int amountColIdx = -1;
            int categoryColIdx = -1;
            int paymentMethodColIdx = -1;
            int currencyColIdx = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                
                String headerName = cell.getStringCellValue().trim().toLowerCase();
                switch (headerName) {
                    case "date": dateColIdx = i; break;
                    case "description": descColIdx = i; break;
                    case "amount": amountColIdx = i; break;
                    case "category": categoryColIdx = i; break;
                    case "payment method": paymentMethodColIdx = i; break;
                    case "currency": currencyColIdx = i; break;
                }
            }
            
            // Validate required columns exist
            if (dateColIdx == -1 || descColIdx == -1 || amountColIdx == -1 || 
                categoryColIdx == -1 || paymentMethodColIdx == -1) {
                throw new IOException("Excel file missing required columns. Required columns: Date, Description, Amount, Category, Payment Method");
            }
            
            // Process data rows
            List<Transaction> batch = new ArrayList<>();
            int rowNum = 1; // Start from second row (after header)
            
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
                
                try {
                    // Extract cell values (handle nulls)
                    String date = getCellValueAsString(row.getCell(dateColIdx));
                    String description = getCellValueAsString(row.getCell(descColIdx));
                    String amount = getCellValueAsString(row.getCell(amountColIdx));
                    String category = getCellValueAsString(row.getCell(categoryColIdx));
                    String paymentMethod = getCellValueAsString(row.getCell(paymentMethodColIdx));
                    String currency = currencyColIdx != -1 ? 
                        getCellValueAsString(row.getCell(currencyColIdx)) : "CNY";
                    
                    // Validate values
                    if (date.isEmpty() || description.isEmpty() || amount.isEmpty() || 
                        category.isEmpty() || paymentMethod.isEmpty()) {
                        throw new IllegalArgumentException("Required fields cannot be empty");
                    }
                    
                    // Parse date
                    LocalDateTime dateTime;
                    try {
                        LocalDate localDate = LocalDate.parse(date, dateFormatter);
                        dateTime = localDate.atStartOfDay();
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException("Invalid date format, should be: " + dateFormatter.toString());
                    }
                    
                    // Parse amount and determine transaction type
                    BigDecimal amountValue;
                    try {
                        amountValue = new BigDecimal(amount.replace(",", ""));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid amount format");
                    }
                    
                    TransactionType type = amountValue.compareTo(BigDecimal.ZERO) < 0 ?
                        TransactionType.EXPENSE : TransactionType.INCOME;
                    
                    // Create transaction
                    Transaction transaction = new Transaction(
                        UUID.randomUUID().toString(),
                        amountValue.abs(),
                        category.trim(),
                        description.trim(),
                        dateTime,
                        type,
                        paymentMethod.trim(),
                        currency.trim()
                    );
                    
                    batch.add(transaction);
                    
                    if (batch.size() >= BATCH_SIZE) {
                        importedTransactions.addAll(batch);
                        transactions.addAll(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    String errorMessage = String.format("Row %d: %s", rowNum, e.getMessage());
                    importErrors.add(new ImportError(rowNum, errorMessage));
                    logger.error(errorMessage, e);
                }
                
                rowNum++;
            }
            
            // Process the last batch
            if (!batch.isEmpty()) {
                importedTransactions.addAll(batch);
                transactions.addAll(batch);
            }
            
            // Save the combined data
            saveTransactionsToFile();
        }
        
        return importedTransactions;
    }
    
    /**
     * Helper method to extract cell value as string, handling different cell types
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return dateFormatter.format(cell.getLocalDateTimeCellValue().toLocalDate());
                } else {
                    // Avoid scientific notation
                    return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
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