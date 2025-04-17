package com.group78.financetracker.ui;

import com.group78.financetracker.model.Transaction;
import com.group78.financetracker.service.BillService;
import com.group78.financetracker.service.DashboardService;
import com.group78.financetracker.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class DashboardPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DashboardPanel.class);
    private final DashboardService dashboardService;
    private final BillService billService;
    private JPanel summaryPanel;
    private JPanel chartPanel;
    private JPanel actionPanel;
    private JPanel headerPanel;

    // Summary labels
    private JLabel expensesValueLabel;
    private JLabel budgetValueLabel;
    private JLabel billsValueLabel;

    // Charts
    private ChartPanel spendingChartPanel;
    private ChartPanel distributionChartPanel;

    // Constants
    private static final Color PRIMARY_COLOR = new Color(74, 107, 255);
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final BigDecimal MONTHLY_BUDGET = new BigDecimal("13000.00");  // Fixed budget amount

    public DashboardPanel() {
        this.dashboardService = new DashboardService();
        this.billService = new BillService();
        
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(BACKGROUND_COLOR);

        createComponents();
        layoutComponents();
        updateDashboard();
    }

    private void createComponents() {
        // Create header panel
        headerPanel = createHeaderPanel();
        
        // Create summary panel
        summaryPanel = createSummaryPanel();
        
        // Create chart panel
        chartPanel = createChartPanel();
        
        // Create action button panel
        actionPanel = createActionPanel();
    }

    private void layoutComponents() {
        // Add components to dashboard
        add(headerPanel, BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.add(summaryPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> {
            reloadData();
        });
        
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);
        
        return header;
    }

    private void reloadData() {
        // Get ImportService instance and reload data
        ImportService importService = dashboardService.getImportService();
        importService.reloadDefaultData();
        
        // Load budget data from file
        loadBudgetDataFromFile();
        
        // Reload bills data
        billService.reloadBills();
        
        // Update all panels
        updateDashboard();
    }
    
    /**
     * Loads budget data from the budget.csv file
     */
    private void loadBudgetDataFromFile() {
        try {
            File budgetFile = new File("data/budget.csv");
            if (!budgetFile.exists()) {
                logger.info("No budget data file found. Using default budget.");
                return;
            }
            
            try (FileReader reader = new FileReader(budgetFile);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {
                
                boolean firstRow = true;
                
                for (CSVRecord record : csvParser) {
                    if (firstRow) {
                        // First row contains total budget info
                        String totalBudgetStr = record.get("TotalBudget");
                        String startDateStr = record.get("StartDate");
                        String endDateStr = record.get("EndDate");
                        
                        if (!totalBudgetStr.isEmpty()) {
                            BigDecimal totalBudget = new BigDecimal(totalBudgetStr);
                            
                            if (!startDateStr.isEmpty() && !endDateStr.isEmpty()) {
                                LocalDate startDate = LocalDate.parse(startDateStr);
                                LocalDate endDate = LocalDate.parse(endDateStr);
                                
                                // Update budget in DashboardService
                                dashboardService.updateTotalBudget(totalBudget, startDate, endDate);
                            }
                        }
                        
                        firstRow = false;
                    }
                }
                
                logger.info("Budget data loaded successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to load budget data: {}", e.getMessage(), e);
        }
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);

        // Create summary cards with placeholder values
        JPanel expensesCard = createSummaryCard("Monthly Expenses", "¥0");
        JPanel budgetCard = createSummaryCard("Remaining Budget", "¥0");
        JPanel billsCard = createSummaryCard("Pending Bills", "0");
        
        // Get the value labels for later updates
        expensesValueLabel = (JLabel) expensesCard.getComponent(2);
        budgetValueLabel = (JLabel) budgetCard.getComponent(2);
        billsValueLabel = (JLabel) billsCard.getComponent(2);

        // Add cards to panel
        panel.add(expensesCard);
        panel.add(budgetCard);
        panel.add(billsCard);

        return panel;
    }

    private JPanel createSummaryCard(String title, String initialValue) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(102, 102, 102));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setFont(valueLabel.getFont().deriveFont(24f));
        valueLabel.setForeground(PRIMARY_COLOR);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);

        return card;
    }

    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);

        // Create spending chart
        JFreeChart spendingChart = createSpendingChart();
        spendingChartPanel = new ChartPanel(spendingChart);
        spendingChartPanel.setPreferredSize(new Dimension(400, 300));
        
        // Create distribution chart
        JFreeChart distributionChart = createDistributionChart();
        distributionChartPanel = new ChartPanel(distributionChart);
        distributionChartPanel.setPreferredSize(new Dimension(400, 300));
        
        // Create containers for charts
        JPanel spendingContainer = createChartContainer("Last 7 Days Spending", spendingChartPanel);
        JPanel distributionContainer = createChartContainer("Expense Distribution", distributionChartPanel);

        panel.add(spendingContainer);
        panel.add(distributionContainer);

        return panel;
    }

    private JPanel createChartContainer(String title, JComponent chartComponent) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        container.add(titleLabel, BorderLayout.NORTH);
        container.add(chartComponent, BorderLayout.CENTER);

        return container;
    }

    private JFreeChart createSpendingChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Add placeholder data (will be updated later)
        for (int i = 6; i >= 0; i--) {
            dataset.addValue(0, "Expenses", LocalDate.now().minusDays(i).format(DATE_FORMATTER));
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            null,                      // Chart title
            "Date",                    // X-Axis label
            "Amount (¥)",              // Y-Axis label
            dataset,                   // Dataset
            PlotOrientation.VERTICAL,  // Orientation
            false,                     // Include legend
            true,                      // Include tooltips
            false                      // Include URLs
        );
        
        // Customize chart
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(75, 192, 192));
        
        return chart;
    }

    private JFreeChart createDistributionChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        // Add placeholder data (will be updated later)
        dataset.setValue("No Data", 1);
        
        JFreeChart chart = ChartFactory.createPieChart(
            null,       // Chart title
            dataset,    // Dataset
            true,       // Include legend
            true,       // Include tooltips
            false       // Include URLs
        );
        
        // Customize chart
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("No Data", Color.LIGHT_GRAY);
        
        return chart;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);

        // Add AI analysis button
        JButton aiButton = createActionButton("AI Smart Analysis", "🤖");
        aiButton.setBackground(PRIMARY_COLOR);
        aiButton.setForeground(Color.WHITE);
        
        panel.add(aiButton);
        panel.add(Box.createHorizontalStrut(1)); // Placeholder

        return panel;
    }

    private JButton createActionButton(String text, String icon) {
        JButton button = new JButton(icon + " " + text);
        button.setFont(button.getFont().deriveFont(14f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    public void updateDashboard() {
        updateSummaryInfo();
        updateSpendingChart();
        updateDistributionChart();
    }
    
    private void updateSummaryInfo() {
        // Get real data
        BigDecimal totalExpenses = dashboardService.getTotalExpenses();
        int pendingBillsCount = billService.getUpcomingBills().size();
        
        // Get budget from DashboardService
        BigDecimal monthlyBudget = dashboardService.getTotalBudget();
        // If no budget is set, use the default value
        if (monthlyBudget.compareTo(BigDecimal.ZERO) == 0) {
            monthlyBudget = MONTHLY_BUDGET;
        }
        
        BigDecimal remainingBudget = monthlyBudget.subtract(totalExpenses);
        
        if (remainingBudget.compareTo(BigDecimal.ZERO) < 0) {
            remainingBudget = BigDecimal.ZERO;
        }
        
        // Update labels
        expensesValueLabel.setText(formatCurrency(totalExpenses));
        budgetValueLabel.setText(formatCurrency(remainingBudget));
        billsValueLabel.setText(String.valueOf(pendingBillsCount));
    }
    
    private void updateSpendingChart() {
        // Get spending data for last 7 days
        Map<LocalDate, BigDecimal> dailySpending = dashboardService.getDailySpending(7);
        
        // Log the data for debugging
        logger.debug("Spending data for chart:");
        for (Map.Entry<LocalDate, BigDecimal> entry : dailySpending.entrySet()) {
            logger.debug("{}: {}", entry.getKey(), entry.getValue());
        }
        
        // Update dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<LocalDate, BigDecimal> entry : dailySpending.entrySet()) {
            dataset.addValue(entry.getValue(), "Expenses", entry.getKey().format(DATE_FORMATTER));
        }
        
        // Update chart
        JFreeChart chart = ChartFactory.createBarChart(
            null,                      // Chart title
            "Date",                    // X-Axis label
            "Amount (¥)",              // Y-Axis label
            dataset,                   // Dataset
            PlotOrientation.VERTICAL,  // Orientation
            false,                     // Include legend
            true,                      // Include tooltips
            false                      // Include URLs
        );
        
        // Customize chart
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(75, 192, 192));
        
        // Set a more reasonable range for the Y-axis
        BigDecimal maxValue = getMaxDailySpending(dailySpending);
        double upperBound = maxValue.doubleValue() * 1.2; // Add 20% margin
        if (upperBound < 100) {
            upperBound = 100; // Minimum upper bound
        }
        plot.getRangeAxis().setRange(0, upperBound);
        
        // Update chart panel
        spendingChartPanel.setChart(chart);
        
        // Force repaint
        spendingChartPanel.repaint();
    }
    
    private BigDecimal getMaxDailySpending(Map<LocalDate, BigDecimal> dailySpending) {
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal value : dailySpending.values()) {
            if (value.compareTo(max) > 0) {
                max = value;
            }
        }
        return max;
    }
    
    private void updateDistributionChart() {
        // Get category distribution
        Map<String, BigDecimal> distribution = dashboardService.getCategoryDistribution();
        
        // Create dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        if (distribution.isEmpty()) {
            dataset.setValue("No Data", 1);
        } else {
            for (Map.Entry<String, BigDecimal> entry : distribution.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue().doubleValue());
            }
        }
        
        // Update chart
        JFreeChart chart = ChartFactory.createPieChart(
            null,       // Chart title
            dataset,    // Dataset
            true,       // Include legend
            true,       // Include tooltips
            false       // Include URLs
        );
        
        // Set colors for categories
        PiePlot plot = (PiePlot) chart.getPlot();
        
        // Assign colors to categories if data is present
        if (!distribution.isEmpty()) {
            int colorIndex = 0;
            Color[] colors = {
                new Color(255, 99, 132),
                new Color(54, 162, 235),
                new Color(255, 206, 86),
                new Color(75, 192, 192),
                new Color(153, 102, 255),
                new Color(255, 159, 64),
                new Color(0, 204, 102)
            };
            
            for (String category : distribution.keySet()) {
                plot.setSectionPaint(category, colors[colorIndex % colors.length]);
                colorIndex++;
            }
        } else {
            plot.setSectionPaint("No Data", Color.LIGHT_GRAY);
        }
        
        // Update chart panel
        distributionChartPanel.setChart(chart);
    }
    
    private String formatCurrency(BigDecimal amount) {
        return "¥" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
} 