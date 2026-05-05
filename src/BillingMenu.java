import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class BillingMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Billing & Subscriptions submenu of the
 ||                  LLM Platform Management System.  Allows operators to
 ||                  generate invoices for a user (snapshotting the current
 ||                  tier and cost at the time of generation), mark an
 ||                  existing invoice as paid, and view all invoices for
 ||                  a given user.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Billing &
 ||                      Subscriptions menu and dispatches to sub-operations.
 ||                  generateInvoice(Connection, Scanner) -- inserts a new
 ||                      Invoice row with the user's current tier cost.
 ||                  markPaid(Connection, Scanner) -- updates an Invoice row's
 ||                      payment_status to 'Paid'.
 ||                  viewInvoices(Connection, Scanner) -- displays all Invoice
 ||                      rows for a given user, joined with the tier name.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class BillingMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Billing & Subscriptions submenu in a loop,
     |      reading the user's choice and dispatching to generateInvoice,
     |      markPaid, or viewInvoices until the user selects "Back".
     |
     |  Pre-condition:  conn is an open, valid JDBC Connection to the Oracle
     |      database.  sc is an open Scanner connected to stdin.
     |
     |  Post-condition: The user has selected "Back"; control returns to
     |      the main menu loop.
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

    /*---------------------------------------------------------------------
     |  Method generateInvoice
     |
     |  Purpose:  Creates a new Invoice row for the given user, snapshotting
     |      the user's current tier_id and the tier's monthly cost at the
     |      moment of generation.  If the tier cost is $0.00 (free tier),
     |      the invoice is immediately marked 'Paid'; otherwise it starts
     |      as 'Unpaid'.  The auto-generated invoice_id and the amount are
     |      printed to stdout.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser and has a valid tier_id
     |      referencing a row in Tier.
     |
     |  Post-condition: A new Invoice row has been inserted with
     |      invoice_date = SYSDATE.  The new invoice_id and amount are
     |      printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method markPaid
     |
     |  Purpose:  Updates the payment_status of an existing Invoice row to
     |      'Paid'.  Used when a user's payment has been confirmed outside
     |      the system and the record needs to be reconciled.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified invoice_id exists in the Invoice table.
     |
     |  Post-condition: The target Invoice row's payment_status has been
     |      set to 'Paid'.  "Invoice marked as Paid." is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method viewInvoices
     |
     |  Purpose:  Retrieves and displays all Invoice rows for the specified
     |      user, showing each invoice's ID, tier name (joined from Tier),
     |      date, amount, and payment status.  Results are ordered by
     |      invoice_date descending (most recent first).
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: All Invoice rows for the user have been printed to
     |      stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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
