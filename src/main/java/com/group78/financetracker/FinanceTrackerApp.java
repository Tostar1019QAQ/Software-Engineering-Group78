package com.group78.financetracker;

import com.formdev.flatlaf.FlatLightLaf;
import com.group78.financetracker.ui.MainFrame;

import javax.swing.*;

public class FinanceTrackerApp {
    public static void main(String[] args) {
        // Set modern UI look and feel
        FlatLightLaf.setup();
        
        // Run GUI in EDT
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Error starting application: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
} 