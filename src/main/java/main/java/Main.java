package main.java;

import com.formdev.flatlaf.FlatLightLaf;
import main.java.data.DatabaseBootstrap;
import main.java.gui.LoginFrame;
import main.java.utils.DatabaseUtil;
import main.java.utils.FinanceReminderScheduler;
import javax.swing.SwingUtilities;

/**
 * Main entry point for the College ERP System
 * Initializes the application and sets up the look and feel
 */
public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        // Run migrations and initialize application data
        DatabaseBootstrap.migrate();
        DatabaseUtil.initializeDatabase();
        FinanceReminderScheduler.start();
        Runtime.getRuntime().addShutdownHook(new Thread(FinanceReminderScheduler::stop));
        
        // Start the application
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
