import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class SupportMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Support Tickets submenu of the LLM Platform
 ||                  Management System.  Allows operators to open new support
 ||                  tickets (defaulted to 'Open' status with no assigned
 ||                  agent), assign an available support agent to a ticket
 ||                  (setting status to 'In Progress'), resolve or escalate a
 ||                  ticket (recording a closed_time), and view all currently
 ||                  open or in-progress tickets.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Support Tickets
 ||                      menu and dispatches to sub-operations.
 ||                  createTicket(Connection, Scanner) -- inserts a new
 ||                      SupportTicket row with status 'Open'.
 ||                  assignAgent(Connection, Scanner) -- updates a ticket with
 ||                      an agent_id and sets status to 'In Progress'.
 ||                  resolveTicket(Connection, Scanner) -- sets a ticket's
 ||                      status to 'Resolved' or 'Escalated' and records
 ||                      the closed_time.
 ||                  viewOpenTickets(Connection) -- displays all tickets with
 ||                      status 'Open' or 'In Progress'.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class SupportMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Support Tickets submenu in a loop, reading
     |      the user's choice and dispatching to createTicket, assignAgent,
     |      resolveTicket, or viewOpenTickets until the user selects "Back".
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
                --- Support Tickets ---
                1. Create Ticket
                2. Assign to Agent
                3. Resolve / Escalate Ticket
                4. View Open Tickets
                5. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> createTicket(conn, sc);
                case 2 -> assignAgent(conn, sc);
                case 3 -> resolveTicket(conn, sc);
                case 4 -> viewOpenTickets(conn);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    /*---------------------------------------------------------------------
     |  Method createTicket
     |
     |  Purpose:  Creates a new SupportTicket row for the specified user with
     |      status 'Open' and no assigned agent.  The operator selects a topic
     |      from a fixed list: Billing, Model Error, Account, Feature Request,
     |      or Other.  The auto-generated ticket_id is printed on success.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: A new SupportTicket row with status 'Open' and
     |      opened_time = SYSTIMESTAMP has been inserted.  The new ticket_id
     |      is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void createTicket(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Prompt for ticket topic
        System.out.println("Topic: 1. Billing  2. Model Error  3. Account  4. Feature Request  5. Other");
        int t = DBUtil.promptInt(sc, "Choice: ");
        String[] topics = {"Billing", "Model Error", "Account", "Feature Request", "Other"};
        String topic = (t >= 1 && t <= 5) ? topics[t - 1] : "Other";

        // Create new ticket with status defaulted to Open and no assigned agent
        String sql = "INSERT INTO SupportTicket (user_id, topic_name, opened_time, status) VALUES (?,?,SYSTIMESTAMP,'Open')";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ticket_id"})) {
            ps.setInt(1, userId);
            ps.setString(2, topic);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Ticket created with ID " + keys.getInt(1) + " (status: Open).");
        }
    }

    /*---------------------------------------------------------------------
     |  Method assignAgent
     |
     |  Purpose:  Assigns a support agent to an existing ticket and sets the
     |      ticket's status to 'In Progress'.  Displays all available agents
     |      (from the SupportAgent table) before prompting, so the operator
     |      can choose an appropriate agent.  Both the ticket_id and agent_id
     |      are validated for existence before the UPDATE is issued.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      SupportTicket and SupportAgent tables exist with at least one agent.
     |
     |  Post-condition: The target SupportTicket row's agent_id has been set
     |      and its status set to 'In Progress'.  A confirmation message is
     |      printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void assignAgent(Connection conn, Scanner sc) throws SQLException {
        // Show available agents
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT agent_id, name, specialty FROM SupportAgent ORDER BY agent_id")) {
            System.out.println("Available agents:");
            while (rs.next()) System.out.printf("  %d. %s (%s)%n",
                rs.getInt(1), rs.getString(2), rs.getString(3));
        }

        // Prompt for ticket ID and validate that it exists
        int ticketId = DBUtil.promptInt(sc, "Ticket ID: ");
        if (!DBUtil.checkExists(conn, ticketId, "SupportTicket", "ticket_id")) return;

        // Prompt for agent ID, validate it exists
        int agentId  = DBUtil.promptInt(sc, "Agent ID: ");
        if (!DBUtil.checkExists(conn, agentId, "SupportAgent", "agent_id")) return;

        // Assign the agent to the ticket and update status to In Progress
        try (PreparedStatement ps = conn.prepareStatement("UPDATE SupportTicket SET agent_id=?, status='In Progress' WHERE ticket_id=?")) {
            ps.setInt(1, agentId); ps.setInt(2, ticketId);
            ps.executeUpdate();
            System.out.println("Agent assigned; status set to In Progress.");
        }
    }

    /*---------------------------------------------------------------------
     |  Method resolveTicket
     |
     |  Purpose:  Closes an existing support ticket by setting its status
     |      to either 'Resolved' (issue addressed) or 'Escalated' (requires
     |      higher-level attention) and recording the current timestamp as
     |      closed_time.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified ticket_id exists in the SupportTicket table.
     |
     |  Post-condition: The target SupportTicket row's status has been set
     |      to 'Resolved' or 'Escalated', and closed_time has been set to
     |      SYSTIMESTAMP.  The new status is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void resolveTicket(Connection conn, Scanner sc) throws SQLException {
        // Prompt for ticket ID and validate that it exists
        int ticketId = DBUtil.promptInt(sc, "Ticket ID: ");
        if (!DBUtil.checkExists(conn, ticketId, "SupportTicket", "ticket_id")) return;

        // Prompt for final status (Resolved vs Escalated) and update the ticket record accordingly
        System.out.println("Final status: 1. Resolved  2. Escalated");
        int s = DBUtil.promptInt(sc, "Choice: ");
        String status = (s == 1) ? "Resolved" : "Escalated";

        // Update the ticket record with the final status and closed_time
        try (PreparedStatement ps = conn.prepareStatement("UPDATE SupportTicket SET status=?, closed_time=SYSTIMESTAMP WHERE ticket_id=?")) {
            ps.setString(1, status); ps.setInt(2, ticketId);
            ps.executeUpdate();
            System.out.println("Ticket " + status + ".");
        }
    }

    /*---------------------------------------------------------------------
     |  Method viewOpenTickets
     |
     |  Purpose:  Retrieves and displays all support tickets whose status is
     |      'Open' or 'In Progress', showing ticket ID, the submitting user's
     |      email, the assigned agent's name (NULL if unassigned), the topic,
     |      the opened timestamp, and the current status.  Results are ordered
     |      by opened_time ascending (oldest first).
     |
     |  Pre-condition:  conn is open.  The SupportTicket, LLMUser, and
     |      SupportAgent tables exist.
     |
     |  Post-condition: All open and in-progress tickets have been printed to
     |      stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void viewOpenTickets(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                SELECT st.ticket_id, u.email AS user_email, sa.name AS agent,
                st.topic_name, TO_CHAR(st.opened_time,'YYYY-MM-DD HH24:MI') AS opened, st.status
                FROM SupportTicket st
                JOIN LLMUser u ON st.user_id=u.user_id
                LEFT JOIN SupportAgent sa ON st.agent_id=sa.agent_id
                WHERE st.status IN ('Open','In Progress')
                ORDER BY st.opened_time
                """)) {
            DBUtil.printResultSet(rs);
        }
    }
}
