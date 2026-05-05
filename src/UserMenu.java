import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class UserMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the User Account Management submenu of the
 ||                  LLM Platform Management System.  Allows the operator
 ||                  to add new users (with an associated billing record),
 ||                  update individual fields of an existing user, delete
 ||                  a user (subject to unpaid-invoice and open-ticket
 ||                  guards), and view all users in a formatted table.
 ||
 ||  Inherits From:  None.
 ||
 ||     Interfaces:  None.
 ||
 |+-----------------------------------------------------------------------
 ||
 ||      Constants:  None.
 ||
 |+-----------------------------------------------------------------------
 ||
 ||   Constructors:  None defined (static-only class).
 ||
 ||  Class Methods:  show(Connection, Scanner) -- displays the User Account
 ||                      Management menu and dispatches to sub-operations.
 ||                  addUser(Connection, Scanner) -- inserts a new LLMUser
 ||                      row and a corresponding BillingRecord row.
 ||                  updateUser(Connection, Scanner) -- updates one field of
 ||                      an existing LLMUser row.
 ||                  deleteUser(Connection, Scanner) -- deletes an LLMUser
 ||                      row after verifying no blocking constraints exist.
 ||                  viewUsers(Connection) -- displays all LLMUser rows
 ||                      joined with their tier name.
 ||                  executeUserUpdate(Connection, String, Object, int) --
 ||                      helper that executes a parameterized UPDATE against
 ||                      LLMUser for either an integer or string value.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class UserMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the User Account Management submenu in a loop,
     |      reading the user's menu choice and dispatching to addUser,
     |      updateUser, deleteUser, or viewUsers until the user selects
     |      "Back".
     |
     |  Pre-condition:  conn is an open, valid JDBC Connection to the Oracle
     |      database.  sc is an open Scanner connected to stdin.
     |
     |  Post-condition: The user has selected "Back"; control returns to
     |      the main menu loop.  Any database changes made during this
     |      session have been committed (autoCommit is on).
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method addUser
     |
     |  Purpose:  Collects all information needed to create a new platform
     |      user and inserts a new row into the LLMUser table followed by
     |      a corresponding row in the BillingRecord table.  The new user's
     |      auto-generated user_id (produced by a database sequence trigger)
     |      is retrieved via getGeneratedKeys() and used as the foreign key
     |      for the BillingRecord insert.
     |
     |  Pre-condition:  conn is open with autoCommit enabled.  The Tier
     |      table contains at least one row.  sc is ready to read input.
     |
     |  Post-condition: A new LLMUser row and a new BillingRecord row have
     |      been inserted into the database.  The new user's ID is printed
     |      to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method updateUser
     |
     |  Purpose:  Allows the operator to update one field (tier, name, email,
     |      or language) of an existing LLMUser row.  The method first
     |      validates that the target user exists, then presents a field
     |      selection menu, prompts for the new value, and delegates to
     |      executeUserUpdate to perform the parameterized SQL UPDATE.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.
     |
     |  Post-condition: The selected field of the specified LLMUser row
     |      has been updated in the database.  "Updated." is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method deleteUser
     |
     |  Purpose:  Deletes an existing LLMUser row after three sequential
     |      guard checks: (1) the user exists; (2) the user has no unpaid
     |      invoices; (3) the user has no open or in-progress support
     |      tickets.  If any guard fails, an explanatory message is printed
     |      and the deletion is aborted.  Otherwise, the operator must type
     |      "YES" to confirm before the DELETE is executed.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.
     |
     |  Post-condition: If all guards pass and the operator confirmed, the
     |      LLMUser row (and all cascade-delete dependent rows) has been
     |      removed from the database.  Otherwise, the database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method viewUsers
     |
     |  Purpose:  Retrieves and displays all rows from the LLMUser table,
     |      joining with Tier to show the human-readable tier name.
     |      Results are ordered by user_id and formatted by
     |      DBUtil.printResultSet.
     |
     |  Pre-condition:  conn is open and the LLMUser and Tier tables exist.
     |
     |  Post-condition: All user rows have been printed to stdout.
     |      The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method executeUserUpdate
     |
     |  Purpose:  Executes a parameterized SQL UPDATE statement against the
     |      LLMUser table, binding either an integer or a string as the
     |      first parameter and the target user_id as the second.  This
     |      helper centralizes the PreparedStatement lifecycle for the four
     |      field-update cases in updateUser.
     |
     |  Pre-condition:  conn is open.  sql is a two-parameter UPDATE of the
     |      form "UPDATE LLMUser SET <col>=? WHERE user_id=?".  value is
     |      either an Integer or a String.  userID identifies an existing row.
     |
     |  Post-condition: The specified LLMUser field has been updated.
     |      The PreparedStatement has been closed.
     |
     |  Parameters:
     |      conn   -- open JDBC Connection to the Oracle database.
     |      sql    -- the parameterized UPDATE statement to execute.
     |      value  -- new field value; treated as int if instanceof Integer,
     |                otherwise cast to String.
     |      userID -- primary key of the LLMUser row to update.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void executeUserUpdate(Connection conn, String sql, Object value, int userID) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (value instanceof Integer i) ps.setInt(1, i);
            else ps.setString(1, (String)value);
            ps.setInt(2, userID);
            ps.executeUpdate();
        }
    }
}
