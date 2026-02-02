package main.java.gui;

import main.java.models.User;
import main.java.utils.DatabaseUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Login frame for user authentication
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    public LoginFrame() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        setTitle("College ERP System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initializeComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        statusLabel = new JLabel(" ");

        // Styling
        loginButton.setBackground(new Color(37, 99, 235));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);

        statusLabel.setForeground(Color.RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(main.java.gui.style.PastelTheme.PASTEL_BLUE_DARK);
        headerPanel.setPreferredSize(new Dimension(0, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel titleLabel = new JLabel("College ERP System");
        titleLabel.setFont(main.java.gui.style.PastelTheme.HEADER_FONT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel);

        // Login panel
        // Use a wrapper to center the card
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(main.java.gui.style.PastelTheme.PASTEL_BG);

        main.java.gui.components.JCard loginCard = new main.java.gui.components.JCard(new GridBagLayout());
        // loginCard styling is auto-applied by JCard constructor

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(main.java.gui.style.PastelTheme.CARD_TITLE_FONT);
        userLbl.setForeground(main.java.gui.style.PastelTheme.TEXT_SECONDARY);
        loginCard.add(userLbl, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        usernameField.setFont(main.java.gui.style.PastelTheme.BODY_FONT);
        // Add a bit of padding to text field
        loginCard.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(main.java.gui.style.PastelTheme.CARD_TITLE_FONT);
        passLbl.setForeground(main.java.gui.style.PastelTheme.TEXT_SECONDARY);
        loginCard.add(passLbl, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        passwordField.setFont(main.java.gui.style.PastelTheme.BODY_FONT);
        loginCard.add(passwordField, gbc);

        // Login button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(24, 12, 12, 12);
        main.java.gui.style.PastelTheme.styleButtonPrimary(loginButton);
        loginCard.add(loginButton, gbc);

        // Status label
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.insets = new Insets(8, 8, 8, 8);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loginCard.add(statusLabel, gbc);

        wrapper.add(loginCard);

        add(headerPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(main.java.gui.style.PastelTheme.PASTEL_BG);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel infoLabel = new JLabel("Default: admin / admin123");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(main.java.gui.style.PastelTheme.TEXT_SECONDARY);
        footerPanel.add(infoLabel);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        ActionListener loginAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        };

        loginButton.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);

        // Set default focus
        SwingUtilities.invokeLater(() -> usernameField.requestFocus());
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Input validation
        if (username.isEmpty()) {
            statusLabel.setText("Username cannot be empty");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            statusLabel.setText("Password cannot be empty");
            passwordField.requestFocus();
            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password");
            return;
        }

        loginButton.setEnabled(false);
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        statusLabel.setText("Authenticating...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return DatabaseUtil.authenticateUser(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        // Login successful
                        dispose();
                        SwingUtilities.invokeLater(() -> {
                            new MainFrame(user).setVisible(true);
                        });
                    } else {
                        // Check lock status in background or just report failure
                        // For simplicity, we'll do lightweight checks or just generic error here,
                        // but strictly speaking subsequent DB calls should also be async.
                        // However, let's keep it simple: if auth fails, we can do a quick check safely
                        // enough
                        // or better yet, spawn another worker if we really want to be purist.
                        // Given the context, let's use a nested worker for the failure case checks to
                        // be truly exhaustive.
                        handleLoginFailure(username);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Login error: " + e.getMessage());
                    resetControls();
                }
            }
        }.execute();
    }

    private void handleLoginFailure(String username) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                if (DatabaseUtil.isUserLocked(username)) {
                    User existing = DatabaseUtil.getUser(username);
                    String until = existing != null && existing.getLockedUntil() != null
                            ? existing.getLockedUntil().toString()
                            : "later";
                    return "Account locked until " + until;
                } else {
                    int remaining = DatabaseUtil.remainingAttempts(username);
                    return "Invalid credentials. Attempts left: " + remaining;
                }
            }

            @Override
            protected void done() {
                try {
                    statusLabel.setText(get());
                } catch (Exception e) {
                    statusLabel.setText("Authentication failed.");
                } finally {
                    resetControls();
                    passwordField.setText("");
                    passwordField.requestFocus();
                }
            }
        }.execute();
    }

    private void resetControls() {
        loginButton.setEnabled(true);
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        setCursor(Cursor.getDefaultCursor());
    }
}
