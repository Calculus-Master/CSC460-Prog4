import java.sql.*;
import java.util.Scanner;

public class QueryMenu {

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
