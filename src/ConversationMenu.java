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
                case 1 -> startConversation(conn, sc);
                case 2 -> addMessage(conn, sc);
                case 3 -> updateFeedback(conn, sc);
                case 4 -> bookmarkMessage(conn, sc);
                case 5 -> viewMessages(conn, sc);
                case 6 -> archiveConversation(conn, sc);
                case 7 -> back = true;
                default -> System.out.println("Invalid option.");
            }
            System.out.println();
        }
    }

    private static void startConversation(Connection conn, Scanner sc) throws SQLException {
        // Prompt for the user ID to start the conversation under, and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Prompt for conversation title
        String title = DBUtil.promptString(sc, "Title: ");

        // Prompt for an optional persona to use, and validate it if provided
        int personaId = DBUtil.promptOptionalInt(sc, "Persona ID");
        String pNameSnap = null, pInstrSnap = null;

        // If a persona ID was provided, validate that it exists and pull the name/instructions snapshot for storing with the conversation
        if (personaId != -1) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, instructions FROM Persona WHERE persona_id=?")) {
                ps.setInt(1, personaId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    pNameSnap = rs.getString(1);
                    pInstrSnap = rs.getString(2);
                } else {
                    System.out.println("Persona not found; starting without persona.");
                    personaId = -1;
                }
            }
        }

        // Prompt for an optional workspace to start the conversation within, and validate it if provided
        int wsId = DBUtil.promptOptionalInt(sc, "Workspace ID: ");
        if (wsId != -1 && !DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Lastly, create the conversation record
        // Default status: active
        String sql = "INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status, " +
                     "persona_name_snapshot, persona_instr_snapshot) VALUES (?,?,?,?,SYSTIMESTAMP,'ACTIVE',?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"conversation_id"})) {
            ps.setInt(1, userId);

            if (wsId != -1) ps.setInt(2, wsId); 
            else ps.setNull(2, Types.INTEGER);

            if (personaId != -1) ps.setInt(3, personaId); 
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, title);
            ps.setString(5, pNameSnap);
            ps.setString(6, pInstrSnap);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Conversation created with ID " + keys.getInt(1) + ".");
        }
    }

    private static void addMessage(Connection conn, Scanner sc) throws SQLException {
        // Prompt for conversation ID and validate that it exists
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        try(PreparedStatement ps = conn.prepareStatement("SELECT conversation_id FROM Conversation WHERE conversation_id=?")) {
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("Conversation with ID " + convId + " not found.");
                return;
            }
        }

        // Prompt for the message role
        System.out.println("Role: 1. USER  2. AI");
        int roleChoice = DBUtil.promptInt(sc, "Choice: ");
        String role = (roleChoice == 1) ? "USER" : "AI";

        // Prompt for the message content
        String content = DBUtil.promptString(sc, "Content: ");

        // If it's a USER message, check and enforce rate limit based on the tier and today's date
        if (role.equals("USER")) {
            int todayCount = 0;
            int limit = Integer.MAX_VALUE;

            // Query the number of messages sent today in this conversation from the current USER
            String countSql = """
                    SELECT COUNT(*) FROM Message m
                    JOIN Conversation c ON m.conversation_id = c.conversation_id
                    WHERE c.user_id = (SELECT user_id FROM Conversation WHERE conversation_id=?)
                      AND m.sender_role = 'USER'
                      AND TRUNC(m.time_sent) = TRUNC(SYSDATE)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                ps.setInt(1, convId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) todayCount = rs.getInt(1);
            }

            // Query the message rate limit for the user's tier
            String limitSql = """
                    SELECT t.message_limit FROM Tier t
                    JOIN LLMUser u ON u.tier_id = t.tier_id
                    JOIN Conversation c ON c.user_id = u.user_id
                    WHERE c.conversation_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(limitSql)) {
                ps.setInt(1, convId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) limit = rs.getInt(1);
            }

            // Check if the user has reached their daily rate limit
            if (todayCount >= limit) {
                System.out.println("Rate limit reached (" + limit + " USER messages/day). Cannot send message.");
                return;
            }
        }

        // If the rate limit check passed (or it's an AI message), insert the new message record
        String sql = "INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (?, ?, SYSTIMESTAMP, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"message_id"})) {
            ps.setInt(1, convId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Message added with ID " + keys.getInt(1) + ".");
        }
    }

    private static void updateFeedback(Connection conn, Scanner sc) throws SQLException {
        // Prompt for message ID and validate that it exists
        int msgId = DBUtil.promptInt(sc, "Message ID: ");
        if(!DBUtil.checkExists(conn, msgId, "Message", "message_id")) return;

        // Validate that the message is an AI message, since only those can be rated
        String messageRole = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT sender_role FROM Message WHERE message_id=?")) {
            ps.setInt(1, msgId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) messageRole = rs.getString(1);
        }
        if (!messageRole.equals("AI")) {
            System.out.println("Feedback can only be submitted for AI messages.");
            return;
        }

        // Prompt for the feedback rating and an optional comment
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

        // Submit or update feedback accordingly
        if (existing > 0) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE MessageFeedback SET rating=?, comment_text=?, submitted_at=SYSTIMESTAMP WHERE message_id=?")) {
                ps.setString(1, rating);
                ps.setString(2, comment);
                ps.setInt(3, msgId);
                ps.executeUpdate();
                System.out.println("Feedback updated.");
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at) VALUES (?,?,?,SYSTIMESTAMP)")) {
                ps.setInt(1, msgId);
                ps.setString(2, rating);
                ps.setString(3, comment);
                ps.executeUpdate();
                System.out.println("Feedback submitted.");
            }
        }
    }

    private static void bookmarkMessage(Connection conn, Scanner sc) throws SQLException {
        // Prompt for user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if(!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Prompt for message ID and validate that it exists
        int msgId  = DBUtil.promptInt(sc, "Message ID: ");
        if(!DBUtil.checkExists(conn, msgId, "Message", "message_id")) return;

        // Check if the message is already bookmarked by this user
        boolean isBookmarked = false;
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM MessageBookmark WHERE user_id=? AND message_id=?")) {
            ps.setInt(1, userId);
            ps.setInt(2, msgId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) isBookmarked = true;
        }
        if (isBookmarked) {
            System.out.println("Message is already bookmarked.");
            return;
        }

        // Add a bookmark record for this user and message unless it already exists
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time) VALUES (?,?,SYSTIMESTAMP)")) {
            ps.setInt(1, userId);
            ps.setInt(2, msgId);
            ps.executeUpdate();
            System.out.println("Bookmarked.");
        }
    }

    private static void viewMessages(Connection conn, Scanner sc) throws SQLException {
        // Prompt for conversation ID and validate that it exists
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        if (!DBUtil.checkExists(conn, convId, "Conversation", "conversation_id")) return;

        // Display all messages in the conversation with their ID, sender role, time sent, and a content preview
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT message_id, sender_role, TO_CHAR(time_sent,'YYYY-MM-DD HH24:MI:SS') AS sent,
            SUBSTR(content,1,80) AS content_preview
            FROM Message WHERE conversation_id=? ORDER BY time_sent
            """)) {
            ps.setInt(1, convId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }

    private static void archiveConversation(Connection conn, Scanner sc) throws SQLException {
        // Prompt for conversation ID and validate that it exists
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        if (!DBUtil.checkExists(conn, convId, "Conversation", "conversation_id")) return;

        // Update the conversation's status to 'ARCHIVED'
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Conversation SET status='ARCHIVED' WHERE conversation_id=?")) {
            ps.setInt(1, convId);
            ps.executeUpdate();
            System.out.println("Conversation archived.");
        }
    }
}
