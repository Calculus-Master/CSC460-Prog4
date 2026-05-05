import java.sql.*;
import java.util.Scanner;

public class UserMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("""
                    --- User Account Management ---
                    1. Add User
                    2. Update User
                    3. Delete User
                    4. View All Users
                    5. Back
                    """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> addUser(conn, sc);
                case 2 -> updateUser(conn, sc);
                case 3 -> deleteUser(conn, sc);
                case 4 -> viewUsers(conn);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
            System.out.println();
        }
    }

    private static void addUser(Connection conn, Scanner sc) throws SQLException {
        // Show available tiers
        DBUtil.displayTiers(conn, true);

        // Prompt for user details: all information needed for adding a new user and their billing info
        int tierId  = DBUtil.promptInt(sc, "Tier ID: ");
        String name = DBUtil.promptString(sc, "Name: ");
        String email= DBUtil.promptString(sc, "Email: ");
        String lang = DBUtil.promptString(sc, "Language: ");
        String method  = DBUtil.promptString(sc, "Payment method: ");
        String address = DBUtil.promptString(sc, "Billing address: ");

        String sql = "INSERT INTO LLMUser (tier_id, name, email, creation_date, language) VALUES (?, ?, ?, SYSDATE, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, new String[]{"USER_ID"});
        ps.setInt(1, tierId);
        ps.setString(2, name);
        ps.setString(3, email);
        ps.setString(4, lang);
        ps.executeUpdate();
        

        // Pull the new user ID from the inserted record (made with the trigger)
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            int newId = keys.getInt(1);
            PreparedStatement bp = conn.prepareStatement("INSERT INTO BillingRecord (user_id, payment_method, billing_address) VALUES (?,?,?)");
            bp.setInt(1, newId);
            bp.setString(2, method);
            bp.setString(3, address);
            bp.executeUpdate();
            System.out.println("User created with ID " + newId + ".");
        } else System.out.println("User created, but failed to retrieve new user ID to create a billing record.");
    }

    private static void updateUser(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID to update: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Prompt for which field within the User table to update
        System.out.println("Update: 1. Tier  2. Name  3. Email  4. Language");
        int field = DBUtil.promptInt(sc, "Field: ");

        // Prompt for the new value to update the selected field with and update the record
        switch (field) {
            case 1 -> {
                DBUtil.displayTiers(conn, false);
                int val = DBUtil.promptInt(sc, "New Tier ID: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET tier_id=? WHERE user_id=?", val, userId);
            }
            case 2 -> {
                String val = DBUtil.promptString(sc, "New name: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET name=? WHERE user_id=?", val, userId);
            }
            case 3 -> {
                String val = DBUtil.promptString(sc, "New email: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET email=? WHERE user_id=?", val, userId);
            }
            case 4 -> {
                String val = DBUtil.promptString(sc, "New language: ");
                executeUserUpdate(conn, "UPDATE LLMUser SET language=? WHERE user_id=?", val, userId);
            }
            default -> System.out.println("Invalid field.");
        }
        System.out.println("Updated.");
    }

    private static void deleteUser(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID to delete: ");

        // Pre-check: user actually exists
        if(!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Pre-check: unpaid invoices
        int unpaid = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Invoice WHERE user_id=? AND payment_status='Unpaid'")) {
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
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM SupportTicket WHERE user_id=? AND status IN ('Open','In Progress')")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) openTickets = rs.getInt(1);
        }
        if (openTickets > 0) {
            System.out.println("Cannot delete: user has " + openTickets + " open/in-progress ticket(s). Resolve them first.");
            return;
        }

        String confirm = DBUtil.promptString(sc, "Type YES to confirm deletion: ");
        if (confirm.equals("YES")) {
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
        try (Statement st = conn.createStatement()) {
            String sql = """
                SELECT u.user_id, u.name, u.email, t.tier_name, u.language 
                FROM LLMUser u JOIN Tier t ON u.tier_id=t.tier_id ORDER BY u.user_id
                """;
            ResultSet rs = st.executeQuery(sql);
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
