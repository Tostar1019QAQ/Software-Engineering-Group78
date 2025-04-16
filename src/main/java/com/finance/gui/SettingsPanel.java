package main.java.com.finance.gui;

import main.java.com.finance.service.FinanceService;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsPanel extends JPanel {
    private final JTextField dataDirectoryField;
    private final JCheckBox autoBackupCheckBox;
    private final JSpinner backupIntervalSpinner;
    
    public SettingsPanel() {
        setLayout(new BorderLayout());
        
        // Create settings panel
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Data directory
        gbc.gridx = 0;
        gbc.gridy = 0;
        settingsPanel.add(new JLabel("Data Directory:"), gbc);
        
        gbc.gridx = 1;
        dataDirectoryField = new JTextField(20);
        dataDirectoryField.setEditable(false);
        settingsPanel.add(dataDirectoryField, gbc);
        
        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseDataDirectory());
        settingsPanel.add(browseButton, gbc);
        
        // Auto backup
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        autoBackupCheckBox = new JCheckBox("Enable Auto Backup");
        settingsPanel.add(autoBackupCheckBox, gbc);
        
        // Backup interval
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        settingsPanel.add(new JLabel("Backup Interval (hours):"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(24, 1, 168, 1);
        backupIntervalSpinner = new JSpinner(spinnerModel);
        settingsPanel.add(backupIntervalSpinner, gbc);
        
        // Save button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveSettings());
        settingsPanel.add(saveButton, gbc);
        
        // Add settings panel to main panel
        add(settingsPanel, BorderLayout.CENTER);
        
        // Load current settings
        loadSettings();
    }
    
    private void browseDataDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            dataDirectoryField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void loadSettings() {
        // TODO: Load settings from configuration file
        dataDirectoryField.setText("data");
        autoBackupCheckBox.setSelected(true);
        backupIntervalSpinner.setValue(24);
    }
    
    private void saveSettings() {
        // TODO: Save settings to configuration file
        String dataDirectory = dataDirectoryField.getText();
        boolean autoBackup = autoBackupCheckBox.isSelected();
        int backupInterval = (Integer) backupIntervalSpinner.getValue();
        
        // Show success message
        JOptionPane.showMessageDialog(this,
                "Settings saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }
} 