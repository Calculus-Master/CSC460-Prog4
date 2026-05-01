import java.sql.*;
import java.util.Scanner;

public class ConversationMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("""
                --- Conversations & Messages ---
                1. Start Conversation
                2. Add Message
                3. Update Message Feedback
                4. Bookmark Message
                5. View Conversation Messages
                6. Archive Conversation
                7. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1: startConversation(conn, sc);  break;
                case 2: addMessage(conn, sc);         break;
                case 3: updateFeedback(conn, sc);     break;
                case 4: bookmarkMessage(conn, sc);    break;
                case 5: viewMessages(conn, sc);       break;
                case 6: archiveConversation(conn, sc);break;
                case 7: back = true;                  break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void startConversation(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        String title = DBUtil.promptString(sc, "Title: ");
        String personaStr = DBUtil.promptOptional(sc, "Persona ID");
        Integer personaId = null;
        String pNameSnap = null, pInstrSnap = null;

        if (personaStr != null) {
            try {
                personaId = Integer.parseInt(personaStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid persona ID, ignoring.");
            }
        }

        if (personaId != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, instructions FROM Persona WHERE persona_id=?")) {
                ps.setInt(1, personaId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    pNameSnap = rs.getString(1);
                    pInstrSnap = rs.getString(2);
                } else {
                    System.out.println("Persona not found; starting without persona.");
                    personaId = null;
                }
            }
        }

        String wsStr = DBUtil.promptOptional(sc, "Workspace ID");
        Integer wsId = null;
        if (wsStr != null) {
            try { wsId = Integer.parseInt(wsStr); } catch (NumberFormatException ignored) {}
        }

        String sql = "INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status, " +
                     "persona_name_snapshot, persona_instr_snapshot) VALUES (?,?,?,?,SYSTIMESTAMP,'ACTIVE',?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"conversation_id"})) {
            ps.setInt(1, userId);
            if (wsId != null) ps.setInt(2, wsId); else ps.setNull(2, Types.INTEGER);
            if (personaId != null) ps.setInt(3, personaId); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, title);
            ps.setString(5, pNameSnap);
            ps.setString(6, pInstrSnap);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Conversation created with ID " + keys.getInt(1) + ".");
        }
    }

    private static void addMessage(Connection conn, Scanner sc) throws SQLException {
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        System.out.println("Role: 1. USER  2. AI");
        int roleChoice = DBUtil.promptInt(sc, "Choice: ");
        String role = (roleChoice == 1) ? "USER" : "AI";
        String content = DBUtil.promptString(sc, "Content: ");

        if ("USER".equals(role)) {
            // Rate-limit check: count today's USER messages for this user in this tier
            int todayCount = 0;
            int limit = Integer.MAX_VALUE;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Message m " +
                    "JOIN Conversation c ON m.conversation_id = c.conversation_id " +
                    "WHERE c.user_id = (SELECT user_id FROM Conversation WHERE conversation_id=?) " +
                    "  AND m.sender_role = 'USER' " +
                    "  AND TRUNC(m.time_sent) = TRUNC(SYSDATE)")) {
                ps.setInt(1, convId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) todayCount = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.message_limit FROM Tier t " +
                    "JOIN LLMUser u ON u.tier_id = t.tier_id " +
                    "JOIN Conversation c ON c.user_id = u.user_id " +
                    "WHERE c.conversation_id = ?")) {
                ps.setInt(1, convId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) limit = rs.getInt(1);
            }
            if (todayCount >= limit) {
                System.out.println("Rate limit reached (" + limit + " USER messages/day). Cannot add message.");
                return;
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Message (conversation_id, sender_role, time_sent, content) " +
                "VALUES (?, ?, SYSTIMESTAMP, ?)", new String[]{"message_id"})) {
            ps.setInt(1, convId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Message added with ID " + keys.getInt(1) + ".");
        }
    }

    private static void updateFeedback(Connection conn, Scanner sc) throws SQLException {
        int msgId = DBUtil.promptInt(sc, "Message ID: ");
        System.out.println("Rating: 1. Thumbs Up  2. Thumbs Down");
        int r = DBUtil.promptInt(sc, "Choice: ");
        String rating = (r == 1) ? "Thumbs Up" : "Thumbs Down";
        String comment = DBUtil.promptOptional(sc, "Comment");

        // Check if feedback already exists
        int existing = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM MessageFeedback WHERE message_id=?")) {
            ps.setInt(1, msgId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) existing = rs.getInt(1);
        }

        if (existing > 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE MessageFeedback SET rating=?, comment_text=?, submitted_at=SYSTIMESTAMP WHERE message_id=?")) {
                ps.setString(1, rating);
                ps.setString(2, comment);
                ps.setInt(3, msgId);
                ps.executeUpdate();
                System.out.println("Feedback updated.");
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at) VALUES (?,?,?,SYSTIMESTAMP)")) {
                ps.setInt(1, msgId);
                ps.setString(2, rating);
                ps.setString(3, comment);
                ps.executeUpdate();
                System.out.println("Feedback submitted.");
            }
        }
    }

    private static void bookmarkMessage(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        int msgId  = DBUtil.promptInt(sc, "Message ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time) VALUES (?,?,SYSTIMESTAMP)")) {
            ps.setInt(1, userId);
            ps.setInt(2, msgId);
            ps.executeUpdate();
            System.out.println("Bookmarked.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) System.out.println("Already bookmarked.");
            else throw e;
        }
    }

    private static void viewMessages(Connection conn, Scanner sc) throws SQLException {
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT message_id, sender_role, TO_CHAR(time_sent,'YYYY-MM-DD HH24:MI:SS') AS sent, " +
                "SUBSTR(content,1,80) AS content_preview " +
                "FROM Message WHERE conversation_id=? ORDER BY time_sent")) {
            ps.setInt(1, convId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }

    private static void archiveConversation(Connection conn, Scanner sc) throws SQLException {
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Conversation SET status='ARCHIVED' WHERE conversation_id=?")) {
            ps.setInt(1, convId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Conversation archived." : "Conversation not found.");
        }
    }
}
