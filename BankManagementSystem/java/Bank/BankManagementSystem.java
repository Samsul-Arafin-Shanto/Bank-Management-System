package Bank;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public BankManagementSystem() {
        // Set up the main window
        setTitle("Bank Management System");
        setSize(1000, 700); // Larger window for better UI
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
        accountNumberLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        loginPanel.add(accountNumberLabel, gbc);

        accountNumberField = new JTextField(20);
        accountNumberField.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        loginPanel.add(accountNumberField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        loginPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        loginPanel.add(passwordField, gbc);

        // Password error label
        passwordErrorLabel = new JLabel();
        passwordErrorLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
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
        nameLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        registerPanel.add(nameLabel, gbc);

        nameField = new JTextField(20);
        nameField.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        registerPanel.add(nameField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        registerPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        registerPanel.add(passwordField, gbc);

        // Register button
        JButton registerButton = createStyledButton("Register", new Color(0, 120, 215));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(registerButton, gbc);

        // Back button
        JButton backButton = createStyledButton("Back to Login", new Color(220, 20, 60));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(backButton, gbc);

        // Add register panel to the window
        add(registerPanel, BorderLayout.CENTER);

        // Register button action
        registerButton.addActionListener(e -> {
            String name = nameField.getText();
            String password = new String(passwordField.getPassword());

            if (name.isEmpty() || password.isEmpty()) {
                showMessage("Name and password cannot be empty.");
                return;
            }

            String accountNumber = generateAccountNumber();
            if (createAccount(accountNumber, name, password)) {
                // Display account number and password
                String message = "Account created successfully!\n" +
                        "Account Number: " + accountNumber + "\n" +
                        "Password: " + password + "\n\n" +
                        "Please note these credentials and click 'Back to Login' to proceed.";
                JOptionPane.showMessageDialog(this, message);
                showLoginScreen(); // Go back to login screen after registration
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

    private boolean createAccount(String accountNumber, String name, String password) {
        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO accounts (account_number, name, password) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, name);
            pstmt.setString(3, hashedPassword);
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
        JButton loanButton = createStyledButton("Apply for Loan", new Color(0, 128, 128)); // New Loan button
        JButton loanRepaymentButton = createStyledButton("Repay Loan", new Color(0, 128, 128)); // Loan Repayment button
        JButton logoutButton = createStyledButton("Logout", new Color(220, 20, 60));

        topPanel.add(depositButton);
        topPanel.add(withdrawButton);
        topPanel.add(checkBalanceButton);
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
        amountLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        centerPanel.add(amountLabel, gbc);

        // Amount field
        amountField = new JTextField(20);
        amountField.setFont(new Font("Consolas", Font.PLAIN, 16));
        amountField.setToolTipText("Enter the amount to deposit or withdraw");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        centerPanel.add(amountField, gbc);

        // Bottom panel for output
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.SOUTH);

        // Button actions
        depositButton.addActionListener(e -> depositMoney());
        withdrawButton.addActionListener(e -> withdrawMoney());
        checkBalanceButton.addActionListener(e -> checkBalance());
        viewHistoryButton.addActionListener(e -> viewTransactionHistory());
        transferButton.addActionListener(e -> transferFunds());
        editProfileButton.addActionListener(e -> editProfile());
        loanButton.addActionListener(e -> applyForLoan()); // Loan button action
        loanRepaymentButton.addActionListener(e -> repayLoan()); // Loan Repayment button action
        logoutButton.addActionListener(e -> {
            loggedInAccountNumber = null;
            showLoginScreen();
        });

        // Refresh the window
        revalidate();
        repaint();
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
        String newName = JOptionPane.showInputDialog(this, "Enter New Name:");
        String newPassword = JOptionPane.showInputDialog(this, "Enter New Password:");

        if (newName == null || newPassword == null || newName.isEmpty() || newPassword.isEmpty()) {
            showMessage("Name and password cannot be empty.");
            return;
        }

        String hashedPassword = hashPassword(newPassword);
        String sql = "UPDATE accounts SET name = ?, password = ? WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, loggedInAccountNumber);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                showMessage("Profile updated successfully!");
            } else {
                showMessage("Error updating profile.");
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

    private void showAdminPanel() {
        // Clear the window
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Top panel for buttons
        JPanel topPanel = new JPanel(new GridLayout(1, 8, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        add(topPanel, BorderLayout.NORTH);

        // Buttons
        JButton viewAllAccountsButton = createStyledButton("View All Accounts", new Color(0, 120, 215));
        JButton blockUnblockButton = createStyledButton("Block/Unblock Account", new Color(50, 205, 50));
        JButton viewAllTransactionsButton = createStyledButton("View All Transactions", new Color(255, 140, 0));
        JButton setInterestRateButton = createStyledButton("Set Interest Rate", new Color(147, 112, 219));
        JButton deleteAccountButton = createStyledButton("Delete Account", new Color(255, 0, 0));
        JButton logoutButton = createStyledButton("Logout", new Color(220, 20, 60));

        topPanel.add(viewAllAccountsButton);
        topPanel.add(blockUnblockButton);
        topPanel.add(viewAllTransactionsButton);
        topPanel.add(setInterestRateButton);
        topPanel.add(deleteAccountButton);
        topPanel.add(logoutButton);

        // Bottom panel for output
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Button actions
        viewAllAccountsButton.addActionListener(e -> viewAllAccounts());
        blockUnblockButton.addActionListener(e -> blockUnblockAccount());
        viewAllTransactionsButton.addActionListener(e -> viewAllTransactions());
        setInterestRateButton.addActionListener(e -> setInterestRate());
        deleteAccountButton.addActionListener(e -> deleteAccount());
        logoutButton.addActionListener(e -> {
            isAdmin = false;
            showLoginScreen();
        });

        // Refresh the window
        revalidate();
        repaint();
    }

    private void viewAllAccounts() {
        String sql = "SELECT account_number, name, balance FROM accounts";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            StringBuilder accounts = new StringBuilder("All Accounts:\n");
            while (rs.next()) {
                String accountNumber = rs.getString("account_number");
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                accounts.append("Account Number: ").append(accountNumber)
                        .append(", Name: ").append(name)
                        .append(", Balance: $").append(balance).append("\n");
            }
            showMessage(accounts.toString());
        } catch (SQLException e) {
            showMessage("Error fetching accounts: " + e.getMessage());
        }
    }

    private void blockUnblockAccount() {
        String accountNumber = JOptionPane.showInputDialog(this, "Enter Account Number to Block/Unblock:");
        if (accountNumber == null || accountNumber.isEmpty()) {
            showMessage("Account number cannot be empty.");
            return;
        }

        // Fetch current block status
        String checkStatusSql = "SELECT is_blocked FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkStatusSql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean isBlocked = rs.getBoolean("is_blocked");
                String newStatus = isBlocked ? "unblocked" : "blocked";

                // Update block status
                String updateSql = "UPDATE accounts SET is_blocked = ? WHERE account_number = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBoolean(1, !isBlocked); // Toggle block status
                    updateStmt.setString(2, accountNumber);
                    int rowsUpdated = updateStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        showMessage("Account " + accountNumber + " has been " + newStatus + ".");
                    } else {
                        showMessage("Error updating account status.");
                    }
                }
            } else {
                showMessage("Account not found.");
            }
        } catch (SQLException e) {
            showMessage("Error updating account status: " + e.getMessage());
        }
    }

    private void viewAllTransactions() {
        String sql = "SELECT account_number, type, amount, timestamp FROM transactions ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            StringBuilder transactions = new StringBuilder("All Transactions:\n");
            while (rs.next()) {
                String accountNumber = rs.getString("account_number");
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                String timestamp = rs.getString("timestamp");
                transactions.append("Account Number: ").append(accountNumber)
                        .append(", Type: ").append(type)
                        .append(", Amount: $").append(amount)
                        .append(", Timestamp: ").append(timestamp).append("\n");
            }
            showMessage(transactions.toString());
        } catch (SQLException e) {
            showMessage("Error fetching transactions: " + e.getMessage());
        }
    }

    private void setInterestRate() {
        String interestRateText = JOptionPane.showInputDialog(this, "Enter New Interest Rate (%):");
        if (interestRateText == null || interestRateText.isEmpty()) {
            showMessage("Interest rate cannot be empty.");
            return;
        }

        double interestRate = Double.parseDouble(interestRateText);
        String sql = "UPDATE accounts SET balance = balance * (1 + ? / 100)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, interestRate);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                showMessage("Interest rate applied successfully!");
            } else {
                showMessage("Error applying interest rate.");
            }
        } catch (SQLException e) {
            showMessage("Error applying interest rate: " + e.getMessage());
        }
    }

    private void deleteAccount() {
        String accountNumber = JOptionPane.showInputDialog(this, "Enter Account Number to Delete:");
        if (accountNumber == null || accountNumber.isEmpty()) {
            showMessage("Account number cannot be empty.");
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this account?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM accounts WHERE account_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, accountNumber);
                int rowsDeleted = pstmt.executeUpdate();
                if (rowsDeleted > 0) {
                    showMessage("Account " + accountNumber + " deleted successfully.");
                } else {
                    showMessage("Account not found.");
                }
            } catch (SQLException e) {
                showMessage("Error deleting account: " + e.getMessage());
            }
        }
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
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
                "is_blocked BOOLEAN DEFAULT FALSE);";

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

        String createActivityLogsTable = "CREATE TABLE IF NOT EXISTS activity_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "account_number TEXT, " +
                "activity_type TEXT, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);";

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
        try (PreparedStatement pstmt = conn.prepareStatement(createActivityLogsTable)) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankManagementSystem system = new BankManagementSystem();
            system.setVisible(true);
        });
    }
}