import java.sql.*;
import java.util.Scanner;

public class Main {

    static Connection conn;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== LLM Platform Management System ===");
        System.out.println("Connecting to Oracle at aloe.cs.arizona.edu...");

        // Read in user input for Oracle credentials
        String user = DBUtil.promptString(scanner, "Oracle username: ");
        String pass = DBUtil.promptString(scanner, "Oracle password: ");

        // If empty, use defaults
        if(user.trim().isEmpty()) user = "mallicksap";
        if(pass.trim().isEmpty()) pass = "a6515";

        // Load Oracle JDBC driver and establish connection
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle", user, pass);
            conn.setAutoCommit(true);
            System.out.println("Connected.\n");
        } catch (ClassNotFoundException e) {
            System.out.println("Oracle JDBC driver not found: " + e.getMessage());
            return;
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return;
        }

        // Continuously show the Main Menu, and based on user input, call the appropriate submenu method
        boolean running = true;
        while (running) {
            printMainMenu();

            int choice = DBUtil.promptInt(scanner, "Choice: ");
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> UserMenu.show(conn, scanner);
                    case 2 -> ConversationMenu.show(conn, scanner);
                    case 3 -> WorkspaceMenu.show(conn, scanner);
                    case 4 -> PersonaMenu.show(conn, scanner);
                    case 5 -> PromptMenu.show(conn, scanner);
                    case 6 -> BillingMenu.show(conn, scanner);
                    case 7 -> SupportMenu.show(conn, scanner);
                    case 8 -> QueryMenu.show(conn, scanner);
                    case 9 -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            System.out.println();
        }

        try { conn.close(); } catch (SQLException ignored) {}
        System.out.println("Exiting...");
    }

    private static void printMainMenu() {
        System.out.println("""
                --- Main Menu ---
                1. User Account Management
                2. Conversations & Messages
                3. Workspace Organization
                4. Persona Management
                5. Prompt Library
                6. Billing & Subscriptions
                7. Support Tickets
                8. Database Queries
                9. Exit
                """);
    }
}
