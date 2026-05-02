import java.sql.*;
import java.util.Scanner;

public class BillingMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("""
                --- Billing & Subscriptions ---
                1. Generate Invoice
                2. Mark Invoice Paid
                3. View User Invoices
                4. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> generateInvoice(conn, sc);
                case 2 -> markPaid(conn, sc);
                case 3 -> viewInvoices(conn, sc);
                case 4 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void generateInvoice(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Snapshot current tier + cost
        int tierId = 0;
        double cost = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT u.tier_id, t.cost FROM LLMUser u JOIN Tier t ON u.tier_id=t.tier_id WHERE u.user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()) return; // should not happen since we validated user exists, but just in case
            tierId = rs.getInt(1);
            cost = rs.getDouble(2);
        }

        // Add the new invoice record with status Unpaid (or auto-mark as Paid if cost is 0 for free tier)
        String sql = "INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status) VALUES (?,?,SYSDATE,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"invoice_id"})) {
            ps.setInt(1, userId);
            ps.setInt(2, tierId);
            ps.setDouble(3, cost);
            ps.setString(4, cost == 0 ? "Paid" : "Unpaid");
            ps.executeUpdate();

            // Pull the new invoice ID from the inserted record (made with the trigger)
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.printf("Invoice created (ID %d) for $%.2f.%n", keys.getInt(1), cost);
        }
    }

    private static void markPaid(Connection conn, Scanner sc) throws SQLException {
        // Prompt for invoice ID and validate that it exists
        int invoiceId = DBUtil.promptInt(sc, "Invoice ID: ");
        if (!DBUtil.checkExists(conn, invoiceId, "Invoice", "invoice_id")) return;

        // Update the invoice record to mark it as Paid
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Invoice SET payment_status='Paid' WHERE invoice_id=?")) {
            ps.setInt(1, invoiceId);
            ps.executeUpdate();
            System.out.println("Invoice marked as Paid.");
        }
    }

    private static void viewInvoices(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Display all invoices of the user with their fields
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT i.invoice_id, t.tier_name, TO_CHAR(i.invoice_date,'YYYY-MM-DD') AS date_,
                i.amount, i.payment_status
                FROM Invoice i JOIN Tier t ON i.tier_id=t.tier_id
                WHERE i.user_id=? ORDER BY i.invoice_date DESC
                """)) {
            ps.setInt(1, userId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }
}
