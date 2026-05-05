import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class ConversationMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Conversations & Messages submenu of the
 ||                  LLM Platform Management System.  Supports starting new
 ||                  conversations (optionally with a persona and workspace),
 ||                  adding USER or AI messages with per-tier rate-limit
 ||                  enforcement, submitting or updating message feedback,
 ||                  bookmarking messages, viewing conversation history, and
 ||                  archiving conversations.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Conversations
 ||                      & Messages menu and dispatches to sub-operations.
 ||                  startConversation(Connection, Scanner) -- inserts a new
 ||                      Conversation row, capturing persona snapshots.
 ||                  addMessage(Connection, Scanner) -- inserts a new Message
 ||                      row, enforcing the tier's daily USER message limit.
 ||                  updateFeedback(Connection, Scanner) -- inserts or updates
 ||                      a MessageFeedback row for an AI message.
 ||                  bookmarkMessage(Connection, Scanner) -- inserts a
 ||                      MessageBookmark row if not already present.
 ||                  viewMessages(Connection, Scanner) -- displays all messages
 ||                      in a conversation ordered by time sent.
 ||                  archiveConversation(Connection, Scanner) -- sets a
 ||                      conversation's status to ARCHIVED.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class ConversationMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Conversations & Messages submenu in a loop,
     |      reading the user's choice and dispatching to startConversation,
     |      addMessage, updateFeedback, bookmarkMessage, viewMessages, or
     |      archiveConversation until the user selects "Back".
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

    /*---------------------------------------------------------------------
     |  Method startConversation
     |
     |  Purpose:  Creates a new Conversation row for a given user.  If the
     |      operator supplies an optional persona ID, the method fetches that
     |      persona's current name and instructions and stores them as
     |      snapshot columns on the conversation, preserving historical
     |      context even if the persona is later modified or deleted.
     |      An optional workspace ID associates the conversation with a
     |      workspace; the conversation owner must be a member of that
     |      workspace.  The new conversation is inserted with status ACTIVE.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      LLMUser, Persona, and Workspace tables exist and are accessible.
     |
     |  Post-condition: A new Conversation row has been inserted.  The new
     |      conversation_id is printed to stdout.  persona_name_snapshot and
     |      persona_instr_snapshot reflect the persona's state at insert time.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method addMessage
     |
     |  Purpose:  Inserts a new Message row into the given conversation.
     |      For USER-role messages, the method enforces the per-tier daily
     |      message limit by counting how many USER messages the conversation
     |      owner has sent today across all conversations, then comparing
     |      that count against the message_limit stored in the user's Tier
     |      row.  If the limit is reached the insert is blocked and a
     |      descriptive message is shown.  AI-role messages are never
     |      rate-limited.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified conversation_id exists in the Conversation table.
     |
     |  Post-condition: If the rate limit was not exceeded (or the message
     |      is AI-role), a new Message row has been inserted with
     |      time_sent = SYSTIMESTAMP.  The new message_id is printed.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method updateFeedback
     |
     |  Purpose:  Submits or updates the feedback (Thumbs Up / Thumbs Down
     |      plus optional free-text comment) for an AI-role message.  The
     |      method first verifies that the target message exists and that its
     |      sender_role is 'AI' (only AI messages may be rated).  It then
     |      checks whether a MessageFeedback row already exists for the
     |      message: if so it issues an UPDATE; otherwise it inserts a new row.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified message_id exists in the Message table.
     |
     |  Post-condition: A MessageFeedback row for the given message_id
     |      reflects the newly provided rating and comment.  The database
     |      is otherwise unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method bookmarkMessage
     |
     |  Purpose:  Creates a MessageBookmark record linking a given user to
     |      a given message.  Before inserting, the method verifies that both
     |      the user and the message exist, and that the bookmark does not
     |      already exist (duplicates are rejected with an informational
     |      message rather than a SQL error).
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      LLMUser and Message tables exist.
     |
     |  Post-condition: If the bookmark did not previously exist, a new
     |      MessageBookmark row has been inserted with bookmarked_time =
     |      SYSTIMESTAMP.  Otherwise the database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method viewMessages
     |
     |  Purpose:  Retrieves and displays all messages belonging to the
     |      specified conversation, showing each message's ID, sender role,
     |      timestamp, and a truncated content preview (first 80 characters).
     |      Results are ordered chronologically by time_sent.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified conversation_id exists in the Conversation table.
     |
     |  Post-condition: All message rows for the conversation have been
     |      printed to stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method archiveConversation
     |
     |  Purpose:  Sets the status of an existing conversation to 'ARCHIVED',
     |      indicating it is no longer active.  Archived conversations are
     |      retained in the database for historical query purposes but will
     |      no longer appear in active-conversation filters.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified conversation_id exists in the Conversation table.
     |
     |  Post-condition: The Conversation row's status column has been
     |      updated to 'ARCHIVED'.  "Conversation archived." is printed.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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
