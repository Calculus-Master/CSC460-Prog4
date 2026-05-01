import java.sql.*;
import java.util.Scanner;

public class UserMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("--- User Account Management ---");
            System.out.println("1. Add User");
            System.out.println("2. Update User");
            System.out.println("3. Delete User");
            System.out.println("4. View All Users");
            System.out.println("5. Back");
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1: addUser(conn, sc);    break;
                case 2: updateUser(conn, sc); break;
                case 3: deleteUser(conn, sc); break;
                case 4: viewUsers(conn);      break;
                case 5: back = true;          break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void addUser(Connection conn, Scanner sc) throws SQLException {
        // Show available tiers
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT tier_id, tier_name, cost FROM Tier ORDER BY tier_id")) {
            System.out.println("Available tiers:");
            while (rs.next()) {
                System.out.printf("  %d. %s ($%.2f/mo)%n",
                    rs.getInt(1), rs.getString(2), rs.getDouble(3));
            }
        }
        int tierId  = DBUtil.promptInt(sc, "Tier ID: ");
        String name = DBUtil.promptString(sc, "Name: ");
        String email= DBUtil.promptString(sc, "Email: ");
        String lang = DBUtil.promptString(sc, "Language: ");
        String method  = DBUtil.promptString(sc, "Payment method: ");
        String address = DBUtil.promptString(sc, "Billing address: ");

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO LLMUser (tier_id, name, email, creation_date, language) " +
                "VALUES (?, ?, ?, SYSDATE, ?)",
                new String[]{"USER_ID"})) {
            ps.setInt(1, tierId);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, lang);
            ps.executeUpdate();
            System.out.println("Inserted new record into User table.");

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                try (PreparedStatement bp = conn.prepareStatement(
                        "INSERT INTO BillingRecord (user_id, payment_method, billing_address) VALUES (?,?,?)")) {
                    bp.setInt(1, newId);
                    bp.setString(2, method);
                    bp.setString(3, address);
                    bp.executeUpdate();
                    System.out.println("Inserted new record into BillingRecord table.");
                }
                System.out.println("User created with ID " + newId + ".");
            }
        }
    }

    private static void updateUser(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID to update: ");
        System.out.println("Update: 1. Tier  2. Name  3. Email  4. Language");
        int field = DBUtil.promptInt(sc, "Field: ");
        switch (field) {
            case 1: {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT tier_id, tier_name FROM Tier ORDER BY tier_id")) {
                    System.out.println("Tiers:");
                    while (rs.next()) System.out.printf("  %d. %s%n", rs.getInt(1), rs.getString(2));
                }
                int newTier = DBUtil.promptInt(sc, "New Tier ID: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET tier_id=? WHERE user_id=?", newTier, userId);
                break;
            }
            case 2: {
                String val = DBUtil.promptString(sc, "New name: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET name=? WHERE user_id=?", val, userId);
                break;
            }
            case 3: {
                String val = DBUtil.promptString(sc, "New email: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET email=? WHERE user_id=?", val, userId);
                break;
            }
            case 4: {
                String val = DBUtil.promptString(sc, "New language: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET language=? WHERE user_id=?", val, userId);
                break;
            }
            default: System.out.println("Invalid field.");
        }
        System.out.println("Updated.");
    }

    private static void deleteUser(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID to delete: ");

        // Pre-check: unpaid invoices
        int unpaid = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM Invoice WHERE user_id=? AND payment_status='Unpaid'")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) unpaid = rs.getInt(1);
        }
        if (unpaid > 0) {
            System.out.println("Cannot delete: user has " + unpaid + " unpaid invoice(s). Resolve them first.");
            return;
        }

        // Pre-check: open/in-progress tickets
        int openTickets = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM SupportTicket WHERE user_id=? AND status IN ('Open','In Progress')")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) openTickets = rs.getInt(1);
        }
        if (openTickets > 0) {
            System.out.println("Cannot delete: user has " + openTickets + " open/in-progress ticket(s). Resolve them first.");
            return;
        }

        String confirm = DBUtil.promptString(sc, "Type YES to confirm deletion: ");
        if ("YES".equals(confirm)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM LLMUser WHERE user_id=?")) {
                ps.setInt(1, userId);
                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? "User deleted." : "User not found.");
            }
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void viewUsers(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT u.user_id, u.name, u.email, t.tier_name, u.language " +
                "FROM LLMUser u JOIN Tier t ON u.tier_id=t.tier_id ORDER BY u.user_id")) {
            DBUtil.printResultSet(rs);
        }
    }

    // Helper for updating various fields in the User table
    private static void executeUserUpdate(Connection conn, String sql, Object value, int userID) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (value instanceof Integer i) ps.setInt(1, i);
            else ps.setString(1, (String)value);
            ps.setInt(2, userID);
            ps.executeUpdate();
        }
    }
}
