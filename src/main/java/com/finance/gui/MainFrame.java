package main.java.com.finance.gui;

import main.java.com.finance.model.Transaction;
import main.java.com.finance.model.Budget;
import main.java.com.finance.service.FinanceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MainFrame extends JFrame {
    private final JTabbedPane tabbedPane;
    private final DashboardPanel dashboardPanel;
    private final TransactionPanel transactionPanel;
    private final BudgetPanel budgetPanel;
    private final SettingsPanel settingsPanel;
    
    public MainFrame() {
        setTitle("Personal Finance Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create main tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels
        dashboardPanel = new DashboardPanel();
        transactionPanel = new TransactionPanel();
        budgetPanel = new BudgetPanel();
        settingsPanel = new SettingsPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Transactions", transactionPanel);
        tabbedPane.addTab("Budget", budgetPanel);
        tabbedPane.addTab("Settings", settingsPanel);
        
        // Add tabbed pane to frame
        add(tabbedPane);
        
        // Add menu bar
        setJMenuBar(createMenuBar());
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import");
        JMenuItem exportItem = new JMenuItem("Export");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        importItem.addActionListener(e -> showImportDialog());
        exportItem.addActionListener(e -> showExportDialog());
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        
        aboutItem.addActionListener(e -> showAboutDialog());
        
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    private void showImportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            // TODO: Implement import functionality
        }
    }
    
    private void showExportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            // TODO: Implement export functionality
        }
    }
    
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Personal Finance Manager\nVersion 1.0\n\nA simple application to manage your personal finances.",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
} 