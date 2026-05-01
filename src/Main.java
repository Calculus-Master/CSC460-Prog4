import java.sql.*;
import java.util.Scanner;

public class Main {

    static Connection conn;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== LLM Platform Management System ===");
        System.out.println("Connecting to Oracle at aloe.cs.arizona.edu...");

        String user = DBUtil.promptString(scanner, "Oracle username: ");
        String pass = DBUtil.promptString(scanner, "Oracle password: ");

        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle", user, pass);
            conn.setAutoCommit(true);
            System.out.println("Connected.\n");
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC driver not found: " + e.getMessage());
            return;
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();

            int choice = DBUtil.promptInt(scanner, "Choice: ");
            System.out.println();

            try {
                switch (choice) {
                    case 1: UserMenu.show(conn, scanner);         break;
                    case 2: ConversationMenu.show(conn, scanner); break;
                    case 3: WorkspaceMenu.show(conn, scanner);    break;
                    case 4: PersonaMenu.show(conn, scanner);      break;
                    case 5: PromptMenu.show(conn, scanner);       break;
                    case 6: BillingMenu.show(conn, scanner);      break;
                    case 7: SupportMenu.show(conn, scanner);      break;
                    case 8: QueryMenu.show(conn, scanner);        break;
                    case 9: running = false;                      break;
                    default: System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
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
