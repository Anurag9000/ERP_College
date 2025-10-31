package main.java;

import com.formdev.flatlaf.FlatLightLaf;
import main.java.data.DatabaseBootstrap;
import main.java.gui.LoginFrame;
import main.java.utils.DatabaseUtil;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Main entry point for the College ERP System
 * Initializes the application and sets up the look and feel
 */
public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (ClassNotFoundException | InstantiationException | 
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Run migrations and initialize application data
        DatabaseBootstrap.migrate();
        DatabaseUtil.initializeDatabase();
        
        // Start the application
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
