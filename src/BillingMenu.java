import java.sql.*;
import java.util.Scanner;

public class BillingMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("--- Billing & Subscriptions ---");
            System.out.println("1. Generate Invoice");
            System.out.println("2. Mark Invoice Paid");
            System.out.println("3. Update Subscription Tier");
            System.out.println("4. View User Invoices");
            System.out.println("5. Back");
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1: generateInvoice(conn, sc);      break;
                case 2: markPaid(conn, sc);             break;
                case 3: updateTier(conn, sc);           break;
                case 4: viewInvoices(conn, sc);         break;
                case 5: back = true;                    break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void generateInvoice(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        // Snapshot current tier + cost
        int tierId = 0;
        double cost = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT u.tier_id, t.cost FROM \"User\" u JOIN Tier t ON u.tier_id=t.tier_id WHERE u.user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { System.out.println("User not found."); return; }
            tierId = rs.getInt(1);
            cost   = rs.getDouble(2);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status) " +
                "VALUES (?,?,SYSDATE,?,'Unpaid')", new String[]{"invoice_id"})) {
            ps.setInt(1, userId);
            ps.setInt(2, tierId);
            ps.setDouble(3, cost);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.printf("Invoice created (ID %d) for $%.2f, status Unpaid.%n",
                keys.getInt(1), cost);
        }
    }

    private static void markPaid(Connection conn, Scanner sc) throws SQLException {
        int invoiceId = DBUtil.promptInt(sc, "Invoice ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Invoice SET payment_status='Paid' WHERE invoice_id=?")) {
            ps.setInt(1, invoiceId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Invoice marked as Paid." : "Invoice not found.");
        }
    }

    private static void updateTier(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT tier_id, tier_name, cost FROM Tier ORDER BY tier_id")) {
            System.out.println("Available tiers:");
            while (rs.next()) System.out.printf("  %d. %s ($%.2f/mo)%n",
                rs.getInt(1), rs.getString(2), rs.getDouble(3));
        }
        int newTier = DBUtil.promptInt(sc, "New Tier ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE \"User\" SET tier_id=? WHERE user_id=?")) {
            ps.setInt(1, newTier); ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Tier updated." : "User not found.");
        }
    }

    private static void viewInvoices(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.invoice_id, t.tier_name, TO_CHAR(i.invoice_date,'YYYY-MM-DD') AS date_, " +
                "i.amount, i.payment_status " +
                "FROM Invoice i JOIN Tier t ON i.tier_id=t.tier_id " +
                "WHERE i.user_id=? ORDER BY i.invoice_date DESC")) {
            ps.setInt(1, userId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }
}
