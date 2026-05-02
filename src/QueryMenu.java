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
                4. Top 3 Personas by Tier  (custom query)
                5. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> queryBookmarks(conn, sc);
                case 2 -> queryUnpaidInvoices(conn);
                case 3 -> queryMostHelpfulPersona(conn);
                case 4 -> queryTopPersonasByTier(conn, sc);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // Q1: Bookmarked messages for a user
    private static void queryBookmarks(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

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

    // Q4 (custom): Top 3 personas by tier
    private static void queryTopPersonasByTier(Connection conn, Scanner sc) throws SQLException {
        String tierName = DBUtil.promptString(sc, "Tier name (Free / Plus / Enterprise): ");
        String sql = """
            WITH tier_users AS (
              SELECT u.user_id FROM LLMUser u
              JOIN Tier t ON u.tier_id = t.tier_id WHERE t.tier_name = ?
            ),
            persona_ratings AS (
              SELECT p.persona_id, p.name AS persona_name, o.email AS owner_email,
                     COUNT(DISTINCT c.user_id) AS tier_user_count,
                     SUM(CASE WHEN f.rating = 'Thumbs Up' THEN 1 ELSE 0 END) AS up_count,
                     COUNT(f.feedback_id) AS total_rated
              FROM Persona p
              JOIN LLMUser o ON p.owner_user_id = o.user_id
              JOIN Conversation c ON c.persona_id = p.persona_id
              JOIN tier_users tu ON tu.user_id = c.user_id
              JOIN Message m ON m.conversation_id = c.conversation_id AND m.sender_role = 'AI'
              JOIN MessageFeedback f ON f.message_id = m.message_id
              GROUP BY p.persona_id, p.name, o.email
              HAVING COUNT(f.feedback_id) > 0
            )
            SELECT persona_name, owner_email, tier_user_count,
                   ROUND(100 * up_count / total_rated, 2) AS thumbs_up_pct
                 FROM (
                SELECT persona_name, owner_email, tier_user_count,
                    ROUND(100 * up_count / total_rated, 2) AS thumbs_up_pct
                FROM persona_ratings
                ORDER BY thumbs_up_pct DESC, tier_user_count DESC
                 )
                 WHERE ROWNUM <= 3
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tierName);
            ResultSet rs = ps.executeQuery();
            System.out.println("Top 3 personas for tier '" + tierName + "':");
            DBUtil.printResultSet(rs);
        }
    }
}
