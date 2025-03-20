package Bank;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BankManagementSystem extends JFrame {
    private static final String DB_URL = "jdbc:sqlite:bank.db";
    private Connection conn;
    private String loggedInAccountNumber; // Track the logged-in user
    private boolean isAdmin = false; // Track if the user is an admin

    // GUI Components
    private JTextField accountNumberField, nameField, amountField;
    private JPasswordField passwordField;
    private JTextArea outputArea;
    private JLabel passwordErrorLabel; // For displaying password errors

    // Dashboard Components
    private JPanel dashboardPanel;
    private JLabel balanceLabel, loanLabel, activityLabel;
    private JTextArea activityArea;

    public BankManagementSystem() {
        // Set up the main window
        setTitle("Bank Management System");
        setSize(1200, 700); // Larger window to accommodate the dashboard
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize database connection
        try {
            conn = DriverManager.getConnection(DB_URL);
            createTables();
            setupAdminAccount(); // Set up the admin account
        } catch (SQLException e) {
            showMessage("Database error: " + e.getMessage());
        }

        // Show login screen first
        showLoginScreen();
    }

    private void setupAdminAccount() {
        String adminAccountNumber = "admin123";
        String adminPassword = "Groza345";
        String hashedPassword = hashPassword(adminPassword);

        // Check if the admin account already exists
        String checkAdminSql = "SELECT * FROM admins WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAdminSql)) {
            pstmt.setString(1, adminAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                // Admin account does not exist, so create it
                String insertAdminSql = "INSERT INTO admins (account_number, password) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertAdminSql)) {
                    insertStmt.setString(1, adminAccountNumber);
                    insertStmt.setString(2, hashedPassword);
                    insertStmt.executeUpdate();
                    System.out.println("Admin account created successfully!");
                }
            }
        } catch (SQLException e) {
            showMessage("Error setting up admin account: " + e.getMessage());
        }
    }

    private void showLoginScreen() {
        // Clear the window
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Create a panel for the login form
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Add spacing

        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome to Bank Management System");
        welcomeLabel.setFont(new Font("Consolas", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(welcomeLabel, gbc);

        // Account number field
        JLabel accountNumberLabel = new JLabel("Account Number:");
        accountNumberLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        loginPanel.add(accountNumberLabel, gbc);

        accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        loginPanel.add(accountNumberField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        loginPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        loginPanel.add(passwordField, gbc);

        // Password error label
        passwordErrorLabel = new JLabel();
        passwordErrorLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        passwordErrorLabel.setForeground(Color.RED);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_START;
        loginPanel.add(passwordErrorLabel, gbc);

        // Login button
        JButton loginButton = createStyledButton("Login", new Color(0, 120, 215));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginButton, gbc);

        // Register button
        JButton registerButton = createStyledButton("Register", new Color(50, 205, 50));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(registerButton, gbc);

        // Admin login button
        JButton adminLoginButton = createStyledButton("Admin Login", new Color(255, 140, 0));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(adminLoginButton, gbc);

        // Add login panel to the window
        add(loginPanel, BorderLayout.CENTER);

        // Login button action
        loginButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            String password = new String(passwordField.getPassword());

            if (login(accountNumber, password)) {
                loggedInAccountNumber = accountNumber;
                showMainScreen();
            } else {
                passwordErrorLabel.setText("Invalid account number or password.");
            }
        });

        // Register button action
        registerButton.addActionListener(e -> showRegistrationScreen());

        // Admin login button action
        adminLoginButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            String password = new String(passwordField.getPassword());

            if (adminLogin(accountNumber, password)) {
                isAdmin = true;
                showAdminPanel();
            } else {
                passwordErrorLabel.setText("Invalid admin credentials.");
            }
        });

        // Refresh the window
        revalidate();
        repaint();
    }

    private boolean login(String accountNumber, String password) {
        String hashedPassword = hashPassword(password);
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Check if the account is blocked
                boolean isBlocked = rs.getBoolean("is_blocked");
                if (isBlocked) {
                    passwordErrorLabel.setText("Your account is blocked. Please contact the admin.");
                    return false;
                }

                // Check password
                if (rs.getString("password").equals(hashedPassword)) {
                    return true;
                } else {
                    passwordErrorLabel.setText("Invalid password.");
                    return false;
                }
            } else {
                passwordErrorLabel.setText("Account not found.");
                return false;
            }
        } catch (SQLException e) {
            showMessage("Error logging in: " + e.getMessage());
            return false;
        }
    }

    private boolean adminLogin(String accountNumber, String password) {
        String hashedPassword = hashPassword(password);
        String sql = "SELECT * FROM admins WHERE account_number = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, hashedPassword);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            showMessage("Error logging in as admin: " + e.getMessage());
            return false;
        }
    }

    private void showRegistrationScreen() {
        // Clear the window
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Create a panel for the registration form
        JPanel registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Add spacing

        // Registration title
        JLabel titleLabel = new JLabel("Create a New Account");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(titleLabel, gbc);

        // Name field
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        registerPanel.add(nameLabel, gbc);

        nameField = new JTextField(20);
        nameField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        registerPanel.add(nameField, gbc);

        // Date of Birth field
        JLabel dobLabel = new JLabel("Date of Birth:");
        dobLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        registerPanel.add(dobLabel, gbc);

        // Create date panel with spinners
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(2000, 1900, 2024, 1));
        
        // Set font for spinners
        daySpinner.setFont(new Font("Consolas", Font.BOLD, 16));
        monthSpinner.setFont(new Font("Consolas", Font.BOLD, 16));
        yearSpinner.setFont(new Font("Consolas", Font.BOLD, 16));
        
        // Set preferred size for spinners
        Dimension spinnerSize = new Dimension(80, 30);
        daySpinner.setPreferredSize(spinnerSize);
        monthSpinner.setPreferredSize(spinnerSize);
        yearSpinner.setPreferredSize(new Dimension(100, 30));
        
        datePanel.add(daySpinner);
        datePanel.add(new JLabel("/"));
        datePanel.add(monthSpinner);
        datePanel.add(new JLabel("/"));
        datePanel.add(yearSpinner);

        gbc.gridx = 1;
        gbc.gridy = 2;
        registerPanel.add(datePanel, gbc);

        // Address field
        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        registerPanel.add(addressLabel, gbc);

        JTextField addressField = new JTextField(20);
        addressField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 3;
        registerPanel.add(addressField, gbc);

        // Nationality field
        JLabel nationalityLabel = new JLabel("Nationality:");
        nationalityLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 4;
        registerPanel.add(nationalityLabel, gbc);

        JTextField nationalityField = new JTextField(20);
        nationalityField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 4;
        registerPanel.add(nationalityField, gbc);

        // NID Number field
        JLabel nidLabel = new JLabel("NID Number:");
        nidLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 5;
        registerPanel.add(nidLabel, gbc);

        JTextField nidField = new JTextField(20);
        nidField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 5;
        registerPanel.add(nidField, gbc);

        // Phone Number field
        JLabel phoneLabel = new JLabel("Phone Number:");
        phoneLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 6;
        registerPanel.add(phoneLabel, gbc);

        JTextField phoneField = new JTextField(20);
        phoneField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 6;
        registerPanel.add(phoneField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 7;
        registerPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 1;
        gbc.gridy = 7;
        registerPanel.add(passwordField, gbc);

        // Register button
        JButton registerButton = createStyledButton("Register", new Color(0, 120, 215));
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(registerButton, gbc);

        // Back button
        JButton backButton = createStyledButton("Back", new Color(220, 20, 60));
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(backButton, gbc);

        // Add register panel to the window
        add(registerPanel, BorderLayout.CENTER);

        // Register button action
        registerButton.addActionListener(e -> {
            String name = nameField.getText();
            // Format date from spinners
            String dob = String.format("%04d-%02d-%02d", 
                (Integer)yearSpinner.getValue(),
                (Integer)monthSpinner.getValue(),
                (Integer)daySpinner.getValue());
            String address = addressField.getText();
            String nationality = nationalityField.getText();
            String nidNumber = nidField.getText();
            String phoneNumber = phoneField.getText();
            String password = new String(passwordField.getPassword());

            if (name.isEmpty() || address.isEmpty() || nationality.isEmpty() || nidNumber.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
                showMessage("All fields are required.");
                return;
            }

            // Generate account number
            String accountNumber = generateAccountNumber();
            System.out.println("Generated Account Number: " + accountNumber);

            // Create the account
            boolean isCreated = createAccount(accountNumber, name, password, dob, address, nationality, nidNumber, phoneNumber);
            System.out.println("Account creation status: " + isCreated);

            if (isCreated) {
                // Display account number and password to the user
                String message = "Account created successfully!\n" +
                        "Account Number: " + accountNumber + "\n" +
                        "Password: " + password + "\n\n" +
                        "Please note these credentials.";
                JOptionPane.showMessageDialog(this, message, "Registration Successful", JOptionPane.INFORMATION_MESSAGE);

                // Go back to the login screen
                showLoginScreen();
            } else {
                showMessage("Error creating account.");
            }
        });

        // Back button action
        backButton.addActionListener(e -> showLoginScreen());

        // Refresh the window
        revalidate();
        repaint();
    }

    private boolean createAccount(String accountNumber, String name, String password, String dob, String address, String nationality, String nidNumber, String phoneNumber) {
        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO accounts (account_number, name, password, date_of_birth, address, nationality, nid_number, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, name);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, dob);
            pstmt.setString(5, address);
            pstmt.setString(6, nationality);
            pstmt.setString(7, nidNumber);
            pstmt.setString(8, phoneNumber);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            showMessage("Error creating account: " + e.getMessage());
            return false;
        }
    }

    private void showMainScreen() {
        // Clear the window
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Top panel for buttons
        JPanel topPanel = new JPanel(new GridLayout(1, 8, 10, 10)); // Added Loan button
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        add(topPanel, BorderLayout.NORTH);

        // Buttons
        JButton depositButton = createStyledButton("Deposit Money", new Color(0, 120, 215));
        JButton withdrawButton = createStyledButton("Withdraw Money", new Color(50, 205, 50));
        JButton checkBalanceButton = createStyledButton("Check Balance", new Color(255, 140, 0));
        JButton viewHistoryButton = createStyledButton("View History", new Color(147, 112, 219));
        JButton transferButton = createStyledButton("Transfer Funds", new Color(75, 0, 130));
        JButton editProfileButton = createStyledButton("Edit Profile", new Color(255, 69, 0));
        JButton loanButton = createStyledButton("Apply Loan", new Color(0, 128, 128)); // New Loan button
        JButton loanRepaymentButton = createStyledButton("Repay Loan", new Color(0, 128, 128)); // Loan Repayment button
        JButton logoutButton = createStyledButton("Logout", new Color(220, 20, 60));

        topPanel.add(depositButton);
        topPanel.add(withdrawButton);
        topPanel.add(viewHistoryButton);
        topPanel.add(transferButton);
        topPanel.add(editProfileButton);
        topPanel.add(loanButton); // Add Loan button
        topPanel.add(loanRepaymentButton); // Add Loan Repayment button
        topPanel.add(logoutButton);

        // Center panel for input fields
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Add padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Add spacing
        add(centerPanel, BorderLayout.CENTER);

        // Amount label
        JLabel amountLabel = new JLabel("Enter Amount:");
        amountLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        centerPanel.add(amountLabel, gbc);

        // Amount field
        amountField = new JTextField(20);
        amountField.setFont(new Font("Consolas", Font.BOLD, 16));
        amountField.setToolTipText("Enter the amount to deposit or withdraw");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        centerPanel.add(amountField, gbc);

        // Bottom panel for output
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.BOLD, 16));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.SOUTH);

        // Dashboard Panel (Side Window)
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dashboardPanel.setPreferredSize(new Dimension(300, getHeight())); // Fixed width for dashboard
        add(dashboardPanel, BorderLayout.EAST);

        // Initialize Dashboard
        initUserDashboard();

        // Button actions
        depositButton.addActionListener(e -> {
            depositMoney();
            updateDashboard(); // Update dashboard after deposit
        });
        withdrawButton.addActionListener(e -> {
            withdrawMoney();
            updateDashboard(); // Update dashboard after withdrawal
        });
        checkBalanceButton.addActionListener(e -> checkBalance());
        viewHistoryButton.addActionListener(e -> viewTransactionHistory());
        transferButton.addActionListener(e -> {
            transferFunds();
            updateDashboard(); // Update dashboard after transfer
        });
        editProfileButton.addActionListener(e -> editProfile());
        loanButton.addActionListener(e -> {
            applyForLoan();
            updateDashboard(); // Update dashboard after loan
        });
        loanRepaymentButton.addActionListener(e -> {
            repayLoan();
            updateDashboard(); // Update dashboard after loan repayment
        });
        logoutButton.addActionListener(e -> {
            loggedInAccountNumber = null;
            showLoginScreen();
        });

        // Refresh the window
        revalidate();
        repaint();
    }

    private void initUserDashboard() {
        dashboardPanel.removeAll(); // Clear existing components

        // Account Overview
        JLabel accountOverviewLabel = new JLabel("Account Overview");
        accountOverviewLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        dashboardPanel.add(accountOverviewLabel);

        balanceLabel = new JLabel("Balance: $0.00");
        balanceLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        dashboardPanel.add(balanceLabel);

        loanLabel = new JLabel("Loan: $0.00");
        loanLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        dashboardPanel.add(loanLabel);

        // User Activity
        JLabel userActivityLabel = new JLabel("User Activity");
        userActivityLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        dashboardPanel.add(userActivityLabel);

        activityArea = new JTextArea(10, 20);
        activityArea.setFont(new Font("Consolas", Font.BOLD, 14));
        activityArea.setEditable(false);
        JScrollPane activityScrollPane = new JScrollPane(activityArea);
        dashboardPanel.add(activityScrollPane);

        // Update Dashboard
        updateDashboard();
    }

    private void updateDashboard() {
        // Update Balance
        String balanceSql = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(balanceSql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceLabel.setText("Balance: $" + String.format("%.2f", balance));
            }
        } catch (SQLException e) {
            showMessage("Error fetching balance: " + e.getMessage());
        }

        // Update Loan
        String loanSql = "SELECT SUM(remaining_amount) AS total_loans FROM loans WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(loanSql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double totalLoans = rs.getDouble("total_loans");
                loanLabel.setText("Loan: $" + String.format("%.2f", totalLoans));
            }
        } catch (SQLException e) {
            showMessage("Error fetching loan data: " + e.getMessage());
        }

        // Update User Activity
        String activitySql = "SELECT type, amount, timestamp FROM transactions WHERE account_number = ? ORDER BY timestamp DESC LIMIT 5";
        try (PreparedStatement pstmt = conn.prepareStatement(activitySql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder activity = new StringBuilder();
            while (rs.next()) {
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                String timestamp = rs.getString("timestamp");
                activity.append(type).append(": $").append(amount).append(" on ").append(timestamp).append("\n");
            }
            activityArea.setText(activity.toString());
        } catch (SQLException e) {
            showMessage("Error fetching activity: " + e.getMessage());
        }
    }

    private void showAdminPanel() {
        // Clear the window
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Top panel for buttons
        JPanel topPanel = new JPanel(new GridLayout(1, 8, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        add(topPanel, BorderLayout.NORTH);

        // Buttons
        JButton viewAllAccountsButton = createStyledButton("View Accounts", new Color(0, 120, 215));
        JButton blockUnblockButton = createStyledButton("Block/Unblock", new Color(50, 205, 50));
        JButton viewAllTransactionsButton = createStyledButton("View Transactions", new Color(255, 140, 0));
        JButton setInterestRateButton = createStyledButton("Interest Rate", new Color(147, 112, 219));
        JButton deleteAccountButton = createStyledButton("Delete Account", new Color(255, 0, 0));
        JButton changeAdminCredentialsButton = createStyledButton("Reset Admin", new Color(75, 0, 130));
        JButton logoutButton = createStyledButton("Logout", new Color(220, 20, 60));

        topPanel.add(viewAllAccountsButton);
        topPanel.add(blockUnblockButton);
        topPanel.add(viewAllTransactionsButton);
        topPanel.add(setInterestRateButton);
        topPanel.add(deleteAccountButton);
        topPanel.add(changeAdminCredentialsButton);
        topPanel.add(logoutButton);

        // Bottom panel for output
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.BOLD, 16));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Dashboard Panel (Side Window)
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dashboardPanel.setPreferredSize(new Dimension(300, getHeight())); // Fixed width for dashboard
        add(dashboardPanel, BorderLayout.EAST);

        // Initialize Admin Dashboard
        initAdminDashboard();

        // Button actions
        viewAllAccountsButton.addActionListener(e -> viewAllAccounts());
        blockUnblockButton.addActionListener(e -> blockUnblockAccount());
        viewAllTransactionsButton.addActionListener(e -> viewAllTransactions());
        setInterestRateButton.addActionListener(e -> setInterestRate());
        deleteAccountButton.addActionListener(e -> deleteAccount());
        changeAdminCredentialsButton.addActionListener(e -> changeAdminCredentials());
        logoutButton.addActionListener(e -> {
            isAdmin = false;
            showLoginScreen();
        });

        // Refresh the window
        revalidate();
        repaint();
    }

    private void initAdminDashboard() {
        dashboardPanel.removeAll(); // Clear existing components
    
        // Total Money in Bank
        JLabel totalMoneyLabel = new JLabel("Total Money in Bank");
        totalMoneyLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        dashboardPanel.add(totalMoneyLabel);
    
        JLabel totalMoneyValue = new JLabel("$0.00");
        totalMoneyValue.setFont(new Font("Consolas", Font.BOLD, 16));
        totalMoneyValue.setName("totalMoneyValue"); // Set a unique name for reference
        dashboardPanel.add(totalMoneyValue);
    
        // Total Loan Amount
        JLabel totalLoanLabel = new JLabel("Total Loan Amount");
        totalLoanLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        dashboardPanel.add(totalLoanLabel);
    
        JLabel totalLoanValue = new JLabel("$0.00");
        totalLoanValue.setFont(new Font("Consolas", Font.BOLD, 16));
        totalLoanValue.setName("totalLoanValue"); // Set a unique name for reference
        dashboardPanel.add(totalLoanValue);
    
        // Update Admin Dashboard
        updateAdminDashboard();
    }
    
    private void updateAdminDashboard() {
        // Update Total Money in Bank
        String totalMoneySql = "SELECT SUM(balance) AS total_money FROM accounts";
        try (PreparedStatement pstmt = conn.prepareStatement(totalMoneySql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double totalMoney = rs.getDouble("total_money");
                // Find the label by name and update its text
                for (Component comp : dashboardPanel.getComponents()) {
                    if (comp instanceof JLabel && "totalMoneyValue".equals(comp.getName())) {
                        ((JLabel) comp).setText("$" + String.format("%.2f", totalMoney));
                    }
                }
            }
        } catch (SQLException e) {
            showMessage("Error fetching total money: " + e.getMessage());
        }
    
        // Update Total Loan Amount
        String totalLoanSql = "SELECT SUM(remaining_amount) AS total_loans FROM loans";
        try (PreparedStatement pstmt = conn.prepareStatement(totalLoanSql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double totalLoans = rs.getDouble("total_loans");
                // Find the label by name and update its text
                for (Component comp : dashboardPanel.getComponents()) {
                    if (comp instanceof JLabel && "totalLoanValue".equals(comp.getName())) {
                        ((JLabel) comp).setText("$" + String.format("%.2f", totalLoans));
                    }
                }
            }
        } catch (SQLException e) {
            showMessage("Error fetching total loans: " + e.getMessage());
        }
    }

    private void depositMoney() {
        String amountText = amountField.getText();

        if (amountText.isEmpty()) {
            showMessage("Amount cannot be empty.");
            return;
        }

        double amount = Double.parseDouble(amountText);
        if (amount <= 0) {
            showMessage("Invalid amount. Please enter a positive value.");
            return;
        }

        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, loggedInAccountNumber);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                logTransaction(loggedInAccountNumber, "DEPOSIT", amount);
                showMessage("Deposit successful!");
            } else {
                showMessage("Account not found.");
            }
        } catch (SQLException e) {
            showMessage("Error depositing money: " + e.getMessage());
        }
    }

    private void withdrawMoney() {
        String amountText = amountField.getText();

        if (amountText.isEmpty()) {
            showMessage("Amount cannot be empty.");
            return;
        }

        double amount = Double.parseDouble(amountText);
        if (amount <= 0) {
            showMessage("Invalid amount. Please enter a positive value.");
            return;
        }

        String sql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, loggedInAccountNumber);
            pstmt.setDouble(3, amount);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                logTransaction(loggedInAccountNumber, "WITHDRAWAL", amount);
                showMessage("Withdrawal successful!");
            } else {
                showMessage("Insufficient balance or account not found.");
            }
        } catch (SQLException e) {
            showMessage("Error withdrawing money: " + e.getMessage());
        }
    }

    private void checkBalance() {
        String sql = "SELECT name, balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                showMessage("Account Holder: " + name + "\nBalance: $" + balance);
            } else {
                showMessage("Account not found.");
            }
        } catch (SQLException e) {
            showMessage("Error checking balance: " + e.getMessage());
        }
    }

    private void viewTransactionHistory() {
        String sql = "SELECT type, amount, timestamp FROM transactions WHERE account_number = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder history = new StringBuilder("Transaction History:\n");
            while (rs.next()) {
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                String timestamp = rs.getString("timestamp");
                history.append(type).append(" of $").append(amount).append(" on ").append(timestamp).append("\n");
            }
            showMessage(history.toString());
        } catch (SQLException e) {
            showMessage("Error fetching transaction history: " + e.getMessage());
        }
    }

    private void transferFunds() {
        String amountText = amountField.getText();
        String targetAccountNumber = JOptionPane.showInputDialog(this, "Enter Target Account Number:");

        if (amountText.isEmpty() || targetAccountNumber.isEmpty()) {
            showMessage("Amount and target account number cannot be empty.");
            return;
        }

        double amount = Double.parseDouble(amountText);
        if (amount <= 0) {
            showMessage("Invalid amount. Please enter a positive value.");
            return;
        }

        // Check if the target account exists
        String checkAccountSql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAccountSql)) {
            pstmt.setString(1, targetAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                showMessage("Target account not found.");
                return;
            }
        } catch (SQLException e) {
            showMessage("Error checking target account: " + e.getMessage());
            return;
        }

        // Withdraw from the logged-in account
        String withdrawSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(withdrawSql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, loggedInAccountNumber);
            pstmt.setDouble(3, amount);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                showMessage("Insufficient balance.");
                return;
            }
        } catch (SQLException e) {
            showMessage("Error withdrawing money: " + e.getMessage());
            return;
        }

        // Deposit into the target account
        String depositSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(depositSql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, targetAccountNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showMessage("Error depositing money: " + e.getMessage());
            return;
        }

        // Log the transaction
        try {
            logTransaction(loggedInAccountNumber, "TRANSFER_OUT", amount);
            logTransaction(targetAccountNumber, "TRANSFER_IN", amount);
            showMessage("Transfer successful!");
        } catch (SQLException e) {
            showMessage("Error logging transaction: " + e.getMessage());
        }
    }

    private void editProfile() {
        // Fetch current user details
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loggedInAccountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String currentName = rs.getString("name");
                String currentDob = rs.getString("date_of_birth");
                String currentAddress = rs.getString("address");
                String currentNationality = rs.getString("nationality");
                String currentNid = rs.getString("nid_number");
                String currentPhone = rs.getString("phone_number");

                // Create a dialog box for editing profile
                JPanel editPanel = new JPanel(new GridLayout(7, 2, 10, 10));
                JTextField nameField = new JTextField(currentName);
                JTextField dobField = new JTextField(currentDob);
                JTextField addressField = new JTextField(currentAddress);
                JTextField nationalityField = new JTextField(currentNationality);
                JTextField nidField = new JTextField(currentNid);
                JTextField phoneField = new JTextField(currentPhone);
                JPasswordField passwordField = new JPasswordField();

                editPanel.add(new JLabel("Name:"));
                editPanel.add(nameField);
                editPanel.add(new JLabel("Date of Birth (YYYY-MM-DD):"));
                editPanel.add(dobField);
                editPanel.add(new JLabel("Address:"));
                editPanel.add(addressField);
                editPanel.add(new JLabel("Nationality:"));
                editPanel.add(nationalityField);
                editPanel.add(new JLabel("NID Number:"));
                editPanel.add(nidField);
                editPanel.add(new JLabel("Phone Number:"));
                editPanel.add(phoneField);
                editPanel.add(new JLabel("New Password:"));
                editPanel.add(passwordField);

                int result = JOptionPane.showConfirmDialog(this, editPanel, "Edit Profile", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String newName = nameField.getText();
                    String newDob = dobField.getText();
                    String newAddress = addressField.getText();
                    String newNationality = nationalityField.getText();
                    String newNid = nidField.getText();
                    String newPhone = phoneField.getText();
                    String newPassword = new String(passwordField.getPassword());

                    // Update the database
                    String updateSql = "UPDATE accounts SET name = ?, date_of_birth = ?, address = ?, nationality = ?, nid_number = ?, phone_number = ?, password = ? WHERE account_number = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, newName);
                        updateStmt.setString(2, newDob);
                        updateStmt.setString(3, newAddress);
                        updateStmt.setString(4, newNationality);
                        updateStmt.setString(5, newNid);
                        updateStmt.setString(6, newPhone);
                        updateStmt.setString(7, hashPassword(newPassword));
                        updateStmt.setString(8, loggedInAccountNumber);
                        updateStmt.executeUpdate();
                        showMessage("Profile updated successfully!");
                    }
                }
            }
        } catch (SQLException e) {
            showMessage("Error updating profile: " + e.getMessage());
        }
    }

    private void applyForLoan() {
        String amountText = JOptionPane.showInputDialog(this, "Enter Loan Amount:");
        if (amountText == null || amountText.isEmpty()) {
            showMessage("Loan amount cannot be empty.");
            return;
        }

        double loanAmount = Double.parseDouble(amountText);
        if (loanAmount <= 0) {
            showMessage("Invalid loan amount. Please enter a positive value.");
            return;
        }

        // Check if the loan amount is within the limit (e.g., $10,000)
        double loanLimit = 10000.0;
        if (loanAmount > loanLimit) {
            showMessage("Loan amount exceeds the maximum limit of $" + loanLimit);
            return;
        }

        // Add the loan amount to the user's balance
        String updateBalanceSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        String insertLoanSql = "INSERT INTO loans (account_number, loan_amount, remaining_amount) VALUES (?, ?, ?)";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertLoanSql)) {
            // Update balance
            updateStmt.setDouble(1, loanAmount);
            updateStmt.setString(2, loggedInAccountNumber);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated > 0) {
                // Insert loan record
                insertStmt.setString(1, loggedInAccountNumber);
                insertStmt.setDouble(2, loanAmount);
                insertStmt.setDouble(3, loanAmount); // Initially, remaining amount = loan amount
                insertStmt.executeUpdate();

                logTransaction(loggedInAccountNumber, "LOAN", loanAmount);
                showMessage("Loan of $" + loanAmount + " approved and added to your account.");
            } else {
                showMessage("Error processing loan.");
            }
        } catch (SQLException e) {
            showMessage("Error processing loan: " + e.getMessage());
        }
    }

    private void repayLoan() {
        String amountText = JOptionPane.showInputDialog(this, "Enter Loan Repayment Amount:");
        if (amountText == null || amountText.isEmpty()) {
            showMessage("Repayment amount cannot be empty.");
            return;
        }

        double repaymentAmount = Double.parseDouble(amountText);
        if (repaymentAmount <= 0) {
            showMessage("Invalid repayment amount. Please enter a positive value.");
            return;
        }

        // Check if the user has an active loan
        String checkLoanSql = "SELECT id, remaining_amount FROM loans WHERE account_number = ? AND remaining_amount > 0";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkLoanSql)) {
            checkStmt.setString(1, loggedInAccountNumber);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int loanId = rs.getInt("id");
                double remainingAmount = rs.getDouble("remaining_amount");

                if (repaymentAmount > remainingAmount) {
                    showMessage("Repayment amount exceeds the remaining loan amount.");
                    return;
                }

                // Deduct the repayment amount from the user's balance
                String updateBalanceSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceSql)) {
                    updateStmt.setDouble(1, repaymentAmount);
                    updateStmt.setString(2, loggedInAccountNumber);
                    updateStmt.setDouble(3, repaymentAmount);
                    int rowsUpdated = updateStmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        // Update the remaining loan amount
                        String updateLoanSql = "UPDATE loans SET remaining_amount = remaining_amount - ? WHERE id = ?";
                        try (PreparedStatement updateLoanStmt = conn.prepareStatement(updateLoanSql)) {
                            updateLoanStmt.setDouble(1, repaymentAmount);
                            updateLoanStmt.setInt(2, loanId);
                            updateLoanStmt.executeUpdate();
                        }

                        logTransaction(loggedInAccountNumber, "LOAN_REPAYMENT", repaymentAmount);
                        showMessage("Loan repayment of $" + repaymentAmount + " successful!");

                        // Check if the loan is fully repaid
                        if (remainingAmount - repaymentAmount <= 0) {
                            showMessage("Congratulations! Your loan has been fully repaid.");
                        }
                    } else {
                        showMessage("Insufficient balance for repayment.");
                    }
                }
            } else {
                showMessage("No active loan found.");
            }
        } catch (SQLException e) {
            showMessage("Error processing loan repayment: " + e.getMessage());
        }
    }

    private void viewAllAccounts() {
        // Create a dialog for displaying accounts
        JDialog accountsDialog = new JDialog(this, "View All Accounts", true);
        accountsDialog.setLayout(new BorderLayout());
        accountsDialog.setSize(500, 650);

        // Create search panel with padding
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Search fields
        JTextField accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Add components to search panel with Consolas font
        JLabel accountNumberLabel = new JLabel("Account Number:");
        accountNumberLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(accountNumberLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        searchPanel.add(accountNumberField, gbc);

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        searchPanel.add(nameField, gbc);

        // Buttons panel with padding
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton searchButton = createStyledButton("Search", new Color(0, 120, 215));
        JButton resetButton = createStyledButton("Reset", new Color(220, 20, 60));
        JButton closeButton = createStyledButton("Close", new Color(128, 128, 128));

        buttonPanel.add(searchButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);

        // Create text area for displaying results with padding
        JTextArea resultArea = new JTextArea();
        resultArea.setFont(new Font("Consolas", Font.BOLD, 14));
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Add panels to dialog
        accountsDialog.add(searchPanel, BorderLayout.NORTH);
        accountsDialog.add(scrollPane, BorderLayout.CENTER);
        accountsDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Function to display all accounts
        Runnable displayAllAccounts = () -> {
            try {
                String sql = "SELECT account_number, name, date_of_birth, address, nationality, nid_number, phone_number, balance, is_blocked FROM accounts";
                PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
                StringBuilder accounts = new StringBuilder("All Accounts:\n\n");
            while (rs.next()) {
                accounts.append("Account Number: ").append(rs.getString("account_number")).append("\n")
                        .append("Name: ").append(rs.getString("name")).append("\n")
                        .append("Date of Birth: ").append(rs.getString("date_of_birth")).append("\n")
                        .append("Address: ").append(rs.getString("address")).append("\n")
                        .append("Nationality: ").append(rs.getString("nationality")).append("\n")
                        .append("NID Number: ").append(rs.getString("nid_number")).append("\n")
                        .append("Phone Number: ").append(rs.getString("phone_number")).append("\n")
                            .append("Balance: $").append(String.format("%.2f", rs.getDouble("balance"))).append("\n")
                            .append("Status: ").append(rs.getBoolean("is_blocked") ? "BLOCKED" : "ACTIVE").append("\n\n");
            }
                resultArea.setText(accounts.toString());
        } catch (SQLException e) {
                resultArea.setText("Error fetching accounts: " + e.getMessage());
            }
        };

        // Search button action
        searchButton.addActionListener(e -> {
            StringBuilder sql = new StringBuilder("SELECT account_number, name, date_of_birth, address, nationality, nid_number, phone_number, balance, is_blocked FROM accounts WHERE 1=1");
            java.util.List<Object> params = new java.util.ArrayList<>();

            if (!accountNumberField.getText().isEmpty()) {
                sql.append(" AND account_number LIKE ?");
                params.add("%" + accountNumberField.getText() + "%");
            }
            if (!nameField.getText().isEmpty()) {
                sql.append(" AND name LIKE ?");
                params.add("%" + nameField.getText() + "%");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                ResultSet rs = pstmt.executeQuery();
                StringBuilder accounts = new StringBuilder("Search Results:\n\n");
                while (rs.next()) {
                    accounts.append("Account Number: ").append(rs.getString("account_number")).append("\n")
                            .append("Name: ").append(rs.getString("name")).append("\n")
                            .append("Date of Birth: ").append(rs.getString("date_of_birth")).append("\n")
                            .append("Address: ").append(rs.getString("address")).append("\n")
                            .append("Nationality: ").append(rs.getString("nationality")).append("\n")
                            .append("NID Number: ").append(rs.getString("nid_number")).append("\n")
                            .append("Phone Number: ").append(rs.getString("phone_number")).append("\n")
                            .append("Balance: $").append(String.format("%.2f", rs.getDouble("balance"))).append("\n")
                            .append("Status: ").append(rs.getBoolean("is_blocked") ? "BLOCKED" : "ACTIVE").append("\n\n");
                }
                resultArea.setText(accounts.toString());
            } catch (SQLException ex) {
                resultArea.setText("Error searching accounts: " + ex.getMessage());
            }
        });

        // Reset button action
        resetButton.addActionListener(e -> {
            accountNumberField.setText("");
            nameField.setText("");
            displayAllAccounts.run();
        });

        // Close button action
        closeButton.addActionListener(e -> accountsDialog.dispose());

        // Display all accounts initially
        displayAllAccounts.run();

        // Show the dialog
        accountsDialog.setLocationRelativeTo(this);
        accountsDialog.setVisible(true);
    }

    private void blockUnblockAccount() {
        // Create a dialog for blocking/unblocking accounts
        JDialog blockDialog = new JDialog(this, "Block/Unblock Account", true);
        blockDialog.setLayout(new BorderLayout());
        blockDialog.setSize(675, 420); // Adjusted to match the picture size

        // Create main panel with padding
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Reduced padding to match picture
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Account number field
        JLabel accountNumberLabel = new JLabel("Account Number:");
        accountNumberLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Account details panel
        JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Account Details"));
        JTextArea detailsArea = new JTextArea(8, 30);
        detailsArea.setFont(new Font("Consolas", Font.BOLD, 14));
        detailsArea.setEditable(false);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);

        // Reason field for blocking
        JLabel reasonLabel = new JLabel("Reason (for blocking):");
        reasonLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextArea reasonField = new JTextArea(3, 20);
        reasonField.setFont(new Font("Consolas", Font.BOLD, 14));
        JScrollPane reasonScroll = new JScrollPane(reasonField);

        // Add components to main panel
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(accountNumberLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        mainPanel.add(accountNumberField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        mainPanel.add(detailsScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(reasonLabel, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(reasonScroll, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton searchButton = createStyledButton("Search", new Color(0, 120, 215));
        JButton blockButton = createStyledButton("Block", new Color(220, 20, 60));
        JButton unblockButton = createStyledButton("Unblock", new Color(50, 205, 50));
        JButton closeButton = createStyledButton("Close", new Color(128, 128, 128));

        buttonPanel.add(searchButton);
        buttonPanel.add(blockButton);
        buttonPanel.add(unblockButton);
        buttonPanel.add(closeButton);

        // Add panels to dialog
        blockDialog.add(mainPanel, BorderLayout.CENTER);
        blockDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Search button action
        searchButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            if (accountNumber.isEmpty()) {
                showMessage("Please enter an account number.");
            return;
        }

            try {
                String sql = "SELECT * FROM accounts WHERE account_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                        StringBuilder details = new StringBuilder();
                        details.append("Name: ").append(rs.getString("name")).append("\n");
                        details.append("Balance: $").append(String.format("%.2f", rs.getDouble("balance"))).append("\n");
                        details.append("Status: ").append(rs.getBoolean("is_blocked") ? "BLOCKED" : "ACTIVE").append("\n");
                        details.append("Date of Birth: ").append(rs.getString("date_of_birth")).append("\n");
                        details.append("Address: ").append(rs.getString("address")).append("\n");
                        details.append("Phone: ").append(rs.getString("phone_number")).append("\n");
                        detailsArea.setText(details.toString());
                    } else {
                        showMessage("Account not found.");
                    }
                }
            } catch (SQLException ex) {
                showMessage("Error fetching account details: " + ex.getMessage());
            }
        });

        // Block button action
        blockButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            String reason = reasonField.getText().trim();
            
            if (accountNumber.isEmpty()) {
                showMessage("Please enter an account number.");
                return;
            }

            if (reason.isEmpty()) {
                showMessage("Please provide a reason for blocking the account.");
                return;
            }

            try {
                // Check if account exists and is not already blocked
                String checkSql = "SELECT is_blocked FROM accounts WHERE account_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, accountNumber);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        if (rs.getBoolean("is_blocked")) {
                            showMessage("Account is already blocked.");
                            return;
                        }

                        // Confirm blocking
                        int confirm = JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to block this account?\nReason: " + reason,
                            "Confirm Block Account",
                            JOptionPane.YES_NO_OPTION
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            // Update account status
                            String updateSql = "UPDATE accounts SET is_blocked = TRUE WHERE account_number = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, accountNumber);
                                updateStmt.executeUpdate();
                                showMessage("Account blocked successfully!");
                                blockDialog.dispose();
                    }
                }
            } else {
                showMessage("Account not found.");
            }
                }
            } catch (SQLException ex) {
                showMessage("Error blocking account: " + ex.getMessage());
            }
        });

        // Unblock button action
        unblockButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            
            if (accountNumber.isEmpty()) {
                showMessage("Please enter an account number.");
                return;
            }

            try {
                // Check if account exists and is blocked
                String checkSql = "SELECT is_blocked FROM accounts WHERE account_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        if (!rs.getBoolean("is_blocked")) {
                            showMessage("Account is not blocked.");
                            return;
                        }

                        // Confirm unblocking
                        int confirm = JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to unblock this account?",
                            "Confirm Unblock Account",
                            JOptionPane.YES_NO_OPTION
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            // Update account status
                            String updateSql = "UPDATE accounts SET is_blocked = FALSE WHERE account_number = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, accountNumber);
                                updateStmt.executeUpdate();
                                showMessage("Account unblocked successfully!");
                                blockDialog.dispose();
                            }
                        }
                    } else {
                        showMessage("Account not found.");
                    }
                }
            } catch (SQLException ex) {
                showMessage("Error unblocking account: " + ex.getMessage());
            }
        });

        // Close button action
        closeButton.addActionListener(e -> blockDialog.dispose());

        // Show the dialog
        blockDialog.setLocationRelativeTo(this);
        blockDialog.setVisible(true);
    }

    private void deleteAccount() {
        // Create a dialog for deleting accounts
        JDialog deleteDialog = new JDialog(this, "Delete Account", true);
        deleteDialog.setLayout(new BorderLayout());
        deleteDialog.setSize(525, 420);

        // Create main panel with padding
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Account number field
        JLabel accountNumberLabel = new JLabel("Account Number:");
        accountNumberLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Account details panel
        JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Account Details"));
        JTextArea detailsArea = new JTextArea(8, 30);
        detailsArea.setFont(new Font("Consolas", Font.BOLD, 14));
        detailsArea.setEditable(false);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);

        // Reason field for deletion
        JLabel reasonLabel = new JLabel("Reason for Deletion:");
        reasonLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextArea reasonField = new JTextArea(3, 20);
        reasonField.setFont(new Font("Consolas", Font.BOLD, 14));
        JScrollPane reasonScroll = new JScrollPane(reasonField);

        // Add components to main panel
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(accountNumberLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        mainPanel.add(accountNumberField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        mainPanel.add(detailsScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(reasonLabel, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(reasonScroll, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton searchButton = createStyledButton("Search", new Color(0, 120, 215));
        JButton deleteButton = createStyledButton("Delete", new Color(220, 20, 60));
        JButton closeButton = createStyledButton("Close", new Color(128, 128, 128));

        buttonPanel.add(searchButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        // Add panels to dialog
        deleteDialog.add(mainPanel, BorderLayout.CENTER);
        deleteDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Search button action
        searchButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            if (accountNumber.isEmpty()) {
                showMessage("Please enter an account number.");
            return;
        }

            try {
                String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, accountNumber);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        StringBuilder details = new StringBuilder();
                        details.append("Name: ").append(rs.getString("name")).append("\n");
                        details.append("Balance: $").append(String.format("%.2f", rs.getDouble("balance"))).append("\n");
                        details.append("Status: ").append(rs.getBoolean("is_blocked") ? "BLOCKED" : "ACTIVE").append("\n");
                        details.append("Date of Birth: ").append(rs.getString("date_of_birth")).append("\n");
                        details.append("Address: ").append(rs.getString("address")).append("\n");
                        details.append("Phone: ").append(rs.getString("phone_number")).append("\n");
                        detailsArea.setText(details.toString());
            } else {
                        showMessage("Account not found.");
                    }
                }
            } catch (SQLException ex) {
                showMessage("Error fetching account details: " + ex.getMessage());
            }
        });

        // Delete button action
        deleteButton.addActionListener(e -> {
            String accountNumber = accountNumberField.getText();
            String reason = reasonField.getText().trim();
            
            if (accountNumber.isEmpty()) {
                showMessage("Please enter an account number.");
            return;
        }

            if (reason.isEmpty()) {
                showMessage("Please provide a reason for deleting the account.");
                return;
            }

            try {
                // Start transaction
                conn.setAutoCommit(false);

                // Check if account exists and get its balance
                String checkSql = "SELECT * FROM accounts WHERE account_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, accountNumber);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        // Get account details for confirmation
                        String name = rs.getString("name");
                        double balance = rs.getDouble("balance");

        // Confirm deletion
                        int confirm = JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to delete this account?\n\n" +
                            "Account Details:\n" +
                            "Name: " + name + "\n" +
                            "Balance: $" + String.format("%.2f", balance) + "\n" +
                            "Reason: " + reason + "\n\n" +
                            "This action cannot be undone!",
                            "Confirm Delete Account",
                            JOptionPane.YES_NO_OPTION
                        );

        if (confirm == JOptionPane.YES_OPTION) {
                            // Delete related records first
                            String deleteTransactionsSql = "DELETE FROM transactions WHERE account_number = ?";
                            String deleteLoansSql = "DELETE FROM loans WHERE account_number = ?";
                            String deleteAccountSql = "DELETE FROM accounts WHERE account_number = ?";

                            try (PreparedStatement deleteTransStmt = conn.prepareStatement(deleteTransactionsSql);
                                 PreparedStatement deleteLoansStmt = conn.prepareStatement(deleteLoansSql);
                                 PreparedStatement deleteAccountStmt = conn.prepareStatement(deleteAccountSql)) {
                                
                                deleteTransStmt.setString(1, accountNumber);
                                deleteTransStmt.executeUpdate();

                                deleteLoansStmt.setString(1, accountNumber);
                                deleteLoansStmt.executeUpdate();

                                deleteAccountStmt.setString(1, accountNumber);
                                deleteAccountStmt.executeUpdate();

                                conn.commit();
                                showMessage("Account deleted successfully!");
                                updateAdminDashboard(); // Update the dashboard to reflect the deleted account's balance
                                deleteDialog.dispose();
                            }
                        }
                } else {
                    showMessage("Account not found.");
                }
                }
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                    showMessage("Error deleting account: " + ex.getMessage());
                } catch (SQLException rollbackEx) {
                    showMessage("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException autoCommitEx) {
                    showMessage("Error resetting auto-commit: " + autoCommitEx.getMessage());
                }
            }
        });

        // Close button action
        closeButton.addActionListener(e -> deleteDialog.dispose());

        // Show the dialog
        deleteDialog.setLocationRelativeTo(this);
        deleteDialog.setVisible(true);
    }

    private void logTransaction(String accountNumber, String type, double amount) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        }
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10); // Generates a 10-character unique ID
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private void showMessage(String message) {
        outputArea.setText(message);
    }

    private void createTables() throws SQLException {
        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                "account_number TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "balance REAL DEFAULT 0.0, " +
                "is_blocked BOOLEAN DEFAULT FALSE, " +
                "date_of_birth TEXT, " +
                "address TEXT, " +
                "nationality TEXT, " +
                "nid_number TEXT, " +
                "phone_number TEXT);";

        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "account_number TEXT, " +
                "type TEXT, " +
                "amount REAL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (account_number) REFERENCES accounts(account_number));";

        String createAdminsTable = "CREATE TABLE IF NOT EXISTS admins (" +
                "account_number TEXT PRIMARY KEY, " +
                "password TEXT NOT NULL);";

        String createLoansTable = "CREATE TABLE IF NOT EXISTS loans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "account_number TEXT, " +
                "loan_amount REAL, " +
                "remaining_amount REAL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (account_number) REFERENCES accounts(account_number));";

        try (PreparedStatement pstmt = conn.prepareStatement(createAccountsTable)) {
            pstmt.execute();
        }
        try (PreparedStatement pstmt = conn.prepareStatement(createTransactionsTable)) {
            pstmt.execute();
        }
        try (PreparedStatement pstmt = conn.prepareStatement(createAdminsTable)) {
            pstmt.execute();
        }
        try (PreparedStatement pstmt = conn.prepareStatement(createLoansTable)) {
            pstmt.execute();
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Consolas", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Add padding
        button.setPreferredSize(new Dimension(150, 40)); // Fixed button size
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker()); // Darken color on hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color); // Restore original color
            }
        });
        return button;
    }

    private void changeAdminCredentials() {
        // Create a dialog for changing admin credentials
        JDialog credentialsDialog = new JDialog(this, "Reset Admin", true);
        credentialsDialog.setLayout(new BorderLayout());
        credentialsDialog.setSize(500, 300);

        // Create input panel with padding
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Current credentials fields
        JLabel currentAccountLabel = new JLabel("Current Account Number:");
        currentAccountLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField currentAccountField = new JTextField(20);
        currentAccountField.setFont(new Font("Consolas", Font.BOLD, 14));

        JLabel currentPasswordLabel = new JLabel("Current Password:");
        currentPasswordLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JPasswordField currentPasswordField = new JPasswordField(20);
        currentPasswordField.setFont(new Font("Consolas", Font.BOLD, 14));

        // New credentials fields
        JLabel newAccountLabel = new JLabel("New Account Number:");
        newAccountLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField newAccountField = new JTextField(20);
        newAccountField.setFont(new Font("Consolas", Font.BOLD, 14));

        JLabel newPasswordLabel = new JLabel("New Password:");
        newPasswordLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JPasswordField newPasswordField = new JPasswordField(20);
        newPasswordField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Add components to input panel
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(currentAccountLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        inputPanel.add(currentAccountField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(currentPasswordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        inputPanel.add(currentPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(newAccountLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        inputPanel.add(newAccountField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(newPasswordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        inputPanel.add(newPasswordField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton updateButton = createStyledButton("Update", new Color(0, 120, 215));
        JButton cancelButton = createStyledButton("Cancel", new Color(220, 20, 60));

        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog
        credentialsDialog.add(inputPanel, BorderLayout.CENTER);
        credentialsDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Update button action
        updateButton.addActionListener(e -> {
            String currentAccount = currentAccountField.getText();
            String currentPassword = new String(currentPasswordField.getPassword());
            String newAccount = newAccountField.getText();
            String newPassword = new String(newPasswordField.getPassword());

            if (currentAccount.isEmpty() || currentPassword.isEmpty() || newAccount.isEmpty() || newPassword.isEmpty()) {
                showMessage("All fields are required.");
                return;
            }

            try {
                // First, verify current credentials
                String hashedCurrentPassword = hashPassword(currentPassword);
                String checkSql = "SELECT * FROM admins WHERE account_number = ? AND password = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, currentAccount);
                    pstmt.setString(2, hashedCurrentPassword);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        // Check if new account number already exists
                        String checkNewAccountSql = "SELECT * FROM admins WHERE account_number = ?";
                        try (PreparedStatement checkNewStmt = conn.prepareStatement(checkNewAccountSql)) {
                            checkNewStmt.setString(1, newAccount);
                            ResultSet newRs = checkNewStmt.executeQuery();
                            
                            if (newRs.next()) {
                                showMessage("New account number already exists. Please choose a different one.");
                                return;
                            }
                        }

                        // Update admin credentials
                        String updateSql = "UPDATE admins SET account_number = ?, password = ? WHERE account_number = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, newAccount);
                            updateStmt.setString(2, hashPassword(newPassword));
                            updateStmt.setString(3, currentAccount);
                            int rowsUpdated = updateStmt.executeUpdate();

                            if (rowsUpdated > 0) {
                                showMessage("Admin credentials updated successfully!\nPlease use the new credentials for next login.");
                                credentialsDialog.dispose();
                            } else {
                                showMessage("Error updating admin credentials.");
                            }
                        }
                    } else {
                        showMessage("Invalid current credentials.");
                    }
                }
            } catch (SQLException ex) {
                showMessage("Error updating admin credentials: " + ex.getMessage());
            }
        });

        // Cancel button action
        cancelButton.addActionListener(e -> credentialsDialog.dispose());

        // Show the dialog
        credentialsDialog.setLocationRelativeTo(this);
        credentialsDialog.setVisible(true);
    }

    private void viewAllTransactions() {
        // Create a dialog for displaying transactions
        JDialog transactionsDialog = new JDialog(this, "View All Transactions", true);
        transactionsDialog.setLayout(new BorderLayout());
        transactionsDialog.setSize(500, 650);

        // Create search panel with padding
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Search fields
        JTextField accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField typeField = new JTextField(20);
        typeField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Date components for Date Search
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(2024, 2000, 2100, 1));
        
        // Set font for spinners
        daySpinner.setFont(new Font("Consolas", Font.BOLD, 14));
        monthSpinner.setFont(new Font("Consolas", Font.BOLD, 14));
        yearSpinner.setFont(new Font("Consolas", Font.BOLD, 14));
        
        datePanel.add(daySpinner);
        datePanel.add(new JLabel("/"));
        datePanel.add(monthSpinner);
        datePanel.add(new JLabel("/"));
        datePanel.add(yearSpinner);

        // Add components to search panel with Consolas font
        JLabel accountNumberLabel = new JLabel("Account Number:");
        accountNumberLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(accountNumberLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        searchPanel.add(accountNumberField, gbc);

        JLabel typeLabel = new JLabel("Transaction Type:");
        typeLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(typeLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        searchPanel.add(typeField, gbc);

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2;
        searchPanel.add(dateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        searchPanel.add(datePanel, gbc);

        // Buttons panel with padding
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton searchButton = createStyledButton("Search", new Color(0, 120, 215));
        JButton resetButton = createStyledButton("Reset", new Color(220, 20, 60));
        JButton closeButton = createStyledButton("Close", new Color(128, 128, 128));

        buttonPanel.add(searchButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);

        // Create text area for displaying results with padding
        JTextArea resultArea = new JTextArea();
        resultArea.setFont(new Font("Consolas", Font.BOLD, 14));
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Add panels to dialog
        transactionsDialog.add(searchPanel, BorderLayout.NORTH);
        transactionsDialog.add(scrollPane, BorderLayout.CENTER);
        transactionsDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Function to display all transactions
        Runnable displayAllTransactions = () -> {
            try {
                String sql = "SELECT account_number, type, amount, timestamp FROM transactions ORDER BY timestamp DESC";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();
                StringBuilder transactions = new StringBuilder("All Transactions:\n\n");
                while (rs.next()) {
                    String accountNumber = rs.getString("account_number");
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    String timestamp = rs.getString("timestamp");
                    transactions.append("Account Number: ").append(accountNumber)
                            .append("\nType: ").append(type)
                            .append("\nAmount: $").append(String.format("%.2f", amount))
                            .append("\nTimestamp: ").append(timestamp)
                            .append("\n\n");
                }
                resultArea.setText(transactions.toString());
            } catch (SQLException e) {
                resultArea.setText("Error fetching transactions: " + e.getMessage());
            }
        };

        // Search button action
        searchButton.addActionListener(e -> {
            StringBuilder sql = new StringBuilder("SELECT account_number, type, amount, timestamp FROM transactions WHERE 1=1");
            java.util.List<Object> params = new java.util.ArrayList<>();

            if (!accountNumberField.getText().isEmpty()) {
                sql.append(" AND account_number LIKE ?");
                params.add("%" + accountNumberField.getText() + "%");
            }
            if (!typeField.getText().isEmpty()) {
                sql.append(" AND type LIKE ?");
                params.add("%" + typeField.getText() + "%");
            }

            // Add date condition
            String searchDate = String.format("%04d-%02d-%02d", 
                (Integer)yearSpinner.getValue(),
                (Integer)monthSpinner.getValue(),
                (Integer)daySpinner.getValue());

            sql.append(" AND date(timestamp) = date(?)");
            params.add(searchDate);

            sql.append(" ORDER BY timestamp DESC");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                ResultSet rs = pstmt.executeQuery();
                StringBuilder transactions = new StringBuilder("Search Results:\n\n");
                while (rs.next()) {
                    String accountNumber = rs.getString("account_number");
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    String timestamp = rs.getString("timestamp");
                    transactions.append("Account Number: ").append(accountNumber)
                            .append("\nType: ").append(type)
                            .append("\nAmount: $").append(String.format("%.2f", amount))
                            .append("\nTimestamp: ").append(timestamp)
                            .append("\n\n");
                }
                resultArea.setText(transactions.toString());
            } catch (SQLException ex) {
                resultArea.setText("Error searching transactions: " + ex.getMessage());
            }
        });

        // Reset button action
        resetButton.addActionListener(e -> {
            accountNumberField.setText("");
            typeField.setText("");
            daySpinner.setValue(1);
            monthSpinner.setValue(1);
            yearSpinner.setValue(2024);
            displayAllTransactions.run();
        });

        // Close button action
        closeButton.addActionListener(e -> transactionsDialog.dispose());

        // Display all transactions initially
        displayAllTransactions.run();

        // Show the dialog
        transactionsDialog.setLocationRelativeTo(this);
        transactionsDialog.setVisible(true);
    }

    private void setInterestRate() {
        // Create a dialog for setting interest rate
        JDialog interestDialog = new JDialog(this, "Set Interest Rate", true);
        interestDialog.setLayout(new BorderLayout());
        interestDialog.setSize(400, 200);

        // Create input panel with padding
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Interest rate field
        JLabel rateLabel = new JLabel("Enter Interest Rate (%):");
        rateLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        JTextField rateField = new JTextField(10);
        rateField.setFont(new Font("Consolas", Font.BOLD, 14));

        // Add components to input panel
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(rateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        inputPanel.add(rateField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton applyButton = createStyledButton("Apply", new Color(0, 120, 215));
        JButton cancelButton = createStyledButton("Cancel", new Color(220, 20, 60));

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog
        interestDialog.add(inputPanel, BorderLayout.CENTER);
        interestDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Apply button action
        applyButton.addActionListener(e -> {
            String interestRateText = rateField.getText();
            if (interestRateText.isEmpty()) {
                showMessage("Interest rate cannot be empty.");
                return;
            }

            try {
                double interestRate = Double.parseDouble(interestRateText);
                
                // Validate interest rate (between 0 and 100)
                if (interestRate < 0 || interestRate > 100) {
                    showMessage("Interest rate must be between 0 and 100.");
                    return;
                }

                // Confirm with admin
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to apply " + interestRate + "% interest to all accounts?",
                    "Confirm Interest Application",
                    JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        // Start transaction
                        conn.setAutoCommit(false);
                        
                        // Get all accounts
                        String selectSql = "SELECT account_number, balance FROM accounts";
                        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                            ResultSet rs = selectStmt.executeQuery();

                            // Update each account and log the interest
                            String updateSql = "UPDATE accounts SET balance = balance * (1 + ? / 100) WHERE account_number = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                while (rs.next()) {
                                    String accountNumber = rs.getString("account_number");
                                    double oldBalance = rs.getDouble("balance");
                                    double interestAmount = oldBalance * (interestRate / 100);

                                    // Update balance
                                    updateStmt.setDouble(1, interestRate);
                                    updateStmt.setString(2, accountNumber);
                                    updateStmt.executeUpdate();

                                    // Log the interest transaction
                                    logTransaction(accountNumber, "INTEREST", interestAmount);
                                }
                            }
                        }

                        // Commit transaction
                        conn.commit();
                        showMessage("Interest rate of " + interestRate + "% applied successfully to all accounts!");
                        updateAdminDashboard(); // Update the dashboard after applying interest
                        interestDialog.dispose();
                    } catch (SQLException sqlEx) {
                        try {
                            conn.rollback(); // Rollback on error
                            showMessage("Error applying interest rate: " + sqlEx.getMessage());
                        } catch (SQLException rollbackEx) {
                            showMessage("Error rolling back transaction: " + rollbackEx.getMessage());
                        }
                    } finally {
                        try {
                            conn.setAutoCommit(true); // Reset auto-commit
                        } catch (SQLException autoCommitEx) {
                            showMessage("Error resetting auto-commit: " + autoCommitEx.getMessage());
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                showMessage("Please enter a valid number for the interest rate.");
            }
        });

        // Cancel button action
        cancelButton.addActionListener(e -> interestDialog.dispose());

        // Show the dialog
        interestDialog.setLocationRelativeTo(this);
        interestDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankManagementSystem system = new BankManagementSystem();
            system.setVisible(true);
        });
    }
}
