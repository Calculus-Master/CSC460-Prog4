import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class QueryMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Database Queries submenu of the LLM
 ||                  Platform Management System.  Contains four read-only
 ||                  analytical queries: (1) bookmarked messages for a user,
 ||                  (2) users with unpaid invoices sorted by amount owed,
 ||                  (3) the persona with the highest Thumbs Up percentage,
 ||                  and (4) a custom query listing conversations that used
 ||                  a persona along with the AI message count per
 ||                  conversation.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Database
 ||                      Queries menu and dispatches to query methods.
 ||                  queryBookmarks(Connection, Scanner) -- Q1: retrieves
 ||                      all bookmarked messages for a specified user.
 ||                  queryUnpaidInvoices(Connection) -- Q2: retrieves all
 ||                      users with outstanding unpaid invoices.
 ||                  queryMostHelpfulPersona(Connection) -- Q3: identifies
 ||                      the persona with the highest Thumbs Up percentage.
 ||                  queryPersonaInfoForUser(Connection, Scanner) -- Q4:
 ||                      lists persona-linked conversations and AI message
 ||                      counts for a specified user.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class QueryMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Database Queries submenu in a loop, reading
     |      the user's choice and dispatching to queryBookmarks,
     |      queryUnpaidInvoices, queryMostHelpfulPersona, or
     |      queryPersonaInfoForUser until the user selects "Back".
     |
     |  Pre-condition:  conn is an open, valid JDBC Connection to the Oracle
     |      database.  sc is an open Scanner connected to stdin.
     |
     |  Post-condition: The user has selected "Back"; control returns to
     |      the main menu loop.  No data has been modified.
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
                    --- Database Queries ---
                    1. My Bookmarked Messages
                    2. Users with Unpaid Invoices
                    3. Most Helpful Persona
                    4. Show Conversations that Used a Persona + Number of Messages (custom query)
                    5. Back
                    """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> queryBookmarks(conn, sc);
                case 2 -> queryUnpaidInvoices(conn);
                case 3 -> queryMostHelpfulPersona(conn);
                case 4 -> queryPersonaInfoForUser(conn, sc);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    /*---------------------------------------------------------------------
     |  Method queryBookmarks
     |
     |  Purpose:  (Query 1) Retrieves all messages bookmarked by the specified
     |      user, showing the containing conversation title, a 60-character
     |      message content preview, and the timestamp the bookmark was created.
     |      Results are ordered by bookmark time descending (most recent first).
     |      The query joins MessageBookmark, Message, and Conversation.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: The result set has been printed to stdout via
     |      DBUtil.printResultSet.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    // Q1: Bookmarked messages for a user
    private static void queryBookmarks(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id"))
            return;

        // Execute query
        String sql = """
                SELECT c.title AS conversation,
                       SUBSTR(m.content,1,60) AS message_preview,
                       TO_CHAR(mb.bookmarked_time,'YYYY-MM-DD HH24:MI') AS bookmarked
                FROM MessageBookmark mb
                JOIN Message m ON mb.message_id = m.message_id
                JOIN Conversation c ON m.conversation_id = c.conversation_id
                WHERE mb.user_id = ?
                ORDER BY mb.bookmarked_time DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            System.out.println("Bookmarked messages for user " + userId + ":");
            DBUtil.printResultSet(rs);
        }
    }

    /*---------------------------------------------------------------------
     |  Method queryUnpaidInvoices
     |
     |  Purpose:  (Query 2) Retrieves all users who have at least one unpaid
     |      invoice, showing each user's ID, email, total amount owed
     |      (sum of all Unpaid invoice amounts), and the timestamp of their
     |      most recent conversation.  Results are ordered by total_owed
     |      descending.  The query joins LLMUser, Invoice (filtered to
     |      payment_status = 'Unpaid'), and Conversation (left join so users
     |      without conversations still appear).
     |
     |  Pre-condition:  conn is open.  The LLMUser, Invoice, and Conversation
     |      tables exist.
     |
     |  Post-condition: The result set has been printed to stdout.  The
     |      database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    // Q2: Users with unpaid invoices
    private static void queryUnpaidInvoices(Connection conn) throws SQLException {
        String sql = """
                SELECT u.user_id, u.email,
                       SUM(i.amount) AS total_owed,
                       MAX(c.start_time) AS last_conversation
                FROM LLMUser u
                JOIN Invoice i ON u.user_id = i.user_id AND i.payment_status = 'Unpaid'
                LEFT JOIN Conversation c ON u.user_id = c.user_id
                GROUP BY u.user_id, u.email
                ORDER BY total_owed DESC
                """;
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            System.out.println("Users with unpaid invoices:");
            DBUtil.printResultSet(rs);
        }
    }

    /*---------------------------------------------------------------------
     |  Method queryMostHelpfulPersona
     |
     |  Purpose:  (Query 3) Identifies the single persona with the highest
     |      Thumbs Up percentage among all personas that have received at
     |      least one piece of feedback.  For each persona, the query counts
     |      all MessageFeedback rows on AI messages in conversations using
     |      that persona, computes the percentage that are 'Thumbs Up', and
     |      selects the top result using Oracle's ROWNUM <= 1 after ordering
     |      by thumbs_up_pct descending.
     |
     |  Pre-condition:  conn is open.  The Persona, Conversation, Message,
     |      and MessageFeedback tables exist and contain data.
     |
     |  Post-condition: The single top-rated persona row has been printed to
     |      stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    // Q3: Most helpful persona (highest thumbs-up %)
    private static void queryMostHelpfulPersona(Connection conn) throws SQLException {
        String sql = """
                SELECT * FROM (
                    SELECT p.name AS persona,
                           ROUND(100 * SUM(CASE WHEN f.rating = 'Thumbs Up' THEN 1 ELSE 0 END)
                                      / COUNT(f.feedback_id), 2) AS thumbs_up_pct,
                           COUNT(f.feedback_id) AS total_ratings
                    FROM Persona p
                    JOIN Conversation c ON c.persona_id = p.persona_id
                    JOIN Message m ON m.conversation_id = c.conversation_id AND m.sender_role = 'AI'
                    JOIN MessageFeedback f ON f.message_id = m.message_id
                    GROUP BY p.persona_id, p.name
                    HAVING COUNT(f.feedback_id) > 0
                    ORDER BY thumbs_up_pct DESC
                )
                WHERE ROWNUM <= 1
                """;
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            System.out.println("Most helpful persona:");
            DBUtil.printResultSet(rs);
        }
    }

    /*---------------------------------------------------------------------
     |  Method queryPersonaInfoForUser
     |
     |  Purpose:  (Query 4 -- custom) For a specified user, lists all of that
     |      user's conversations that were started with a persona, along with
     |      the conversation title, start time, the persona name snapshot stored
     |      at the time the conversation was created, and the count of AI-role
     |      messages in each conversation.  Results are ordered by start_time
     |      descending.  This query helps users review how their conversations
     |      have made use of the platform's AI personas over time.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: The result set has been printed to stdout.  The
     |      database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    // Q4 (custom): Conversations that Used a Persona + Number of Messages
    private static void queryPersonaInfoForUser(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id"))
            return;

        String sql = """
                SELECT
                    c.title,
                    c.start_time,
                    c.persona_name_snapshot,
                    COUNT(m.message_id) AS ai_message_count
                FROM
                    Conversation c
                    JOIN Message m ON m.conversation_id = c.conversation_id
                    AND m.sender_role = 'AI'
                WHERE
                    c.user_id = ?
                    AND c.persona_id IS NOT NULL
                GROUP BY
                    c.title,
                    c.start_time,
                    c.persona_name_snapshot
                ORDER BY
                    c.start_time DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            System.out.println("Conversations with personas for user '" + userId + "':");
            DBUtil.printResultSet(rs);
        }
    }
}
