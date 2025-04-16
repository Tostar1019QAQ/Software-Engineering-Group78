package main.java.com.finance;

import main.java.com.finance.gui.MainFrame;
import javax.swing.SwingUtilities;

public class FinanceApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
} 