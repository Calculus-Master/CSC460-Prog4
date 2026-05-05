import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class WorkspaceMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Workspace Organization submenu of the
 ||                  LLM Platform Management System.  Allows operators to
 ||                  create workspaces (automatically adding the creator as
 ||                  an OWNER member), modify a workspace's name or
 ||                  visibility, add users as MEMBER-role workspace members,
 ||                  move a conversation into a workspace (subject to an
 ||                  ownership-membership check), and view the member list
 ||                  of a workspace.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Workspace
 ||                      Organization menu and dispatches to sub-operations.
 ||                  createWorkspace(Connection, Scanner) -- inserts a new
 ||                      Workspace row and a corresponding OWNER WorkspaceMember.
 ||                  modifyWorkspace(Connection, Scanner) -- updates the name
 ||                      or visibility of an existing Workspace row.
 ||                  addMember(Connection, Scanner) -- inserts a MEMBER-role
 ||                      WorkspaceMember row for a given user and workspace.
 ||                  moveConversation(Connection, Scanner) -- reassigns a
 ||                      conversation's workspace_id after verifying ownership.
 ||                  viewMembers(Connection, Scanner) -- displays all members
 ||                      of a workspace with their roles and join dates.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class WorkspaceMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Workspace Organization submenu in a loop,
     |      reading the user's choice and dispatching to createWorkspace,
     |      modifyWorkspace, addMember, moveConversation, or viewMembers
     |      until the user selects "Back".
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
                --- Workspace Organization ---
                1. Create Workspace
                2. Modify Workspace
                3. Add Member
                4. Move Conversation to Workspace
                5. View Workspace Members
                6. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> createWorkspace(conn, sc);
                case 2 -> modifyWorkspace(conn, sc);
                case 3 -> addMember(conn, sc);
                case 4 -> moveConversation(conn, sc);
                case 5 -> viewMembers(conn, sc);
                case 6 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    /*---------------------------------------------------------------------
     |  Method createWorkspace
     |
     |  Purpose:  Inserts a new Workspace row owned by a given user, then
     |      automatically inserts a WorkspaceMember row for that user with
     |      role 'OWNER'.  The auto-generated workspace_id (from a database
     |      sequence trigger) is retrieved via getGeneratedKeys() so it can
     |      be used immediately for the member insert.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified owner user_id exists in LLMUser.
     |
     |  Post-condition: A new Workspace row and a corresponding OWNER
     |      WorkspaceMember row have been inserted.  The new workspace_id
     |      is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void createWorkspace(Connection conn, Scanner sc) throws SQLException {
        // Prompt for workspace owner (user ID) and validate that it exists
        int ownerId = DBUtil.promptInt(sc, "Owner User ID: ");
        if (!DBUtil.checkExists(conn, ownerId, "LLMUser", "user_id")) return;

        // Prompt for workspace name
        String name = DBUtil.promptString(sc, "Name: ");

        // Prompt for workspace visibility
        System.out.println("Visibility: 1. PRIVATE  2. SHARED");
        int v = DBUtil.promptInt(sc, "Choice: ");
        String vis = (v == 2) ? "SHARED" : "PRIVATE";

        // Create the workspace record, and capture the generated workspace ID from the trigger
        String sql = "INSERT INTO Workspace (owner_id, name, visibility, created_at) VALUES (?,?,?,SYSTIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"workspace_id"})) {
            ps.setInt(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, vis);
            ps.executeUpdate();

            // Automatically create a WorksspaceMember entry for the owner that created the workspace, with role 'OWNER'
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int wsId = keys.getInt(1);
                // auto-add owner as OWNER member
                try (PreparedStatement mp = conn.prepareStatement(
                        "INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at) VALUES (?,?,'OWNER',SYSTIMESTAMP)")) {
                    mp.setInt(1, wsId);
                    mp.setInt(2, ownerId);
                    mp.executeUpdate();
                }
                System.out.println("Workspace created with ID " + wsId + ".");
            }
        }
    }

    /*---------------------------------------------------------------------
     |  Method modifyWorkspace
     |
     |  Purpose:  Updates either the name or the visibility of an existing
     |      Workspace row.  The method presents a two-option field menu,
     |      prompts for the new value, and issues the corresponding
     |      parameterized UPDATE statement.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified workspace_id exists in the Workspace table.
     |
     |  Post-condition: Either the name or visibility column of the target
     |      Workspace row has been updated.  "Updated." is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void modifyWorkspace(Connection conn, Scanner sc) throws SQLException {
        // Prompt for workspace ID and validate that it exists
        int wsId = DBUtil.promptInt(sc, "Workspace ID: ");
        if (!DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Prompt for which field to update
        System.out.println("Update: 1. Name  2. Visibility");
        int f = DBUtil.promptInt(sc, "Field: ");
        if (f == 1) {
            String name = DBUtil.promptString(sc, "New name: ");
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Workspace SET name=? WHERE workspace_id=?")) {
                ps.setString(1, name); 
                ps.setInt(2, wsId); 
                ps.executeUpdate();
            }
        } else {
            System.out.println("Visibility: 1. PRIVATE  2. SHARED");
            int v = DBUtil.promptInt(sc, "Choice: ");
            String vis = (v == 2) ? "SHARED" : "PRIVATE";
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Workspace SET visibility=? WHERE workspace_id=?")) {
                ps.setString(1, vis); 
                ps.setInt(2, wsId); 
                ps.executeUpdate();
            }
        }
        System.out.println("Updated.");
    }

    /*---------------------------------------------------------------------
     |  Method addMember
     |
     |  Purpose:  Adds a user as a MEMBER-role participant of a workspace.
     |      Before inserting, the method verifies that the workspace and
     |      user both exist, and that the user is not already a member of
     |      the workspace (duplicate membership is rejected gracefully).
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      Workspace and LLMUser tables exist.
     |
     |  Post-condition: If the user was not already a member, a new
     |      WorkspaceMember row with role 'MEMBER' and joined_at =
     |      SYSTIMESTAMP has been inserted.  Otherwise, the database
     |      is unchanged and an informational message is shown.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void addMember(Connection conn, Scanner sc) throws SQLException {
        // Prompt for workspace ID and validate that it exists
        int wsId = DBUtil.promptInt(sc, "Workspace ID: ");
        if (!DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Prompt for user ID of the new member and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Check if the user is already a member of the workspace
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM WorkspaceMember WHERE workspace_id=? AND user_id=?")) {
            ps.setInt(1, wsId); 
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("User " + userId + " is already a member of this workspace.");
                return;
            }
        }

        // Add user as a MEMBER of the workspace
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at) VALUES (?,?,'MEMBER',SYSTIMESTAMP)")) {
            ps.setInt(1, wsId); 
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("Member added.");
        }
    }

    /*---------------------------------------------------------------------
     |  Method moveConversation
     |
     |  Purpose:  Reassigns a conversation's workspace_id to a target
     |      workspace.  Before updating, the method verifies that the
     |      conversation and target workspace both exist, and that the
     |      conversation owner is a member of the target workspace (a user
     |      cannot move a conversation into a workspace they do not belong
     |      to, as they would lose access to it).
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified conversation_id and workspace_id exist.
     |
     |  Post-condition: If the owner is a member of the target workspace,
     |      the Conversation row's workspace_id has been updated.
     |      "Conversation moved." is printed to stdout.  Otherwise the
     |      database is unchanged and an error message is displayed.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void moveConversation(Connection conn, Scanner sc) throws SQLException {
        // Prompt for conversation ID and validate that it exists
        int convId = DBUtil.promptInt(sc, "Conversation ID: ");
        if (!DBUtil.checkExists(conn, convId, "Conversation", "conversation_id")) return;

        // Prompt for target workspace ID and validate that it exists
        int wsId = DBUtil.promptInt(sc, "Target Workspace ID: ");
        if (!DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Get conversation owner
        int ownerId = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM Conversation WHERE conversation_id=?")) {
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) ownerId = rs.getInt(1);
            else return; // should not happen since we validated the conversation ID, but just in case
        }

        // Membership check
        boolean isOwnerMember = true;
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM WorkspaceMember WHERE workspace_id=? AND user_id=?")) {
            ps.setInt(1, wsId); 
            ps.setInt(2, ownerId);
            ResultSet rs = ps.executeQuery();
            isOwnerMember = rs.next();
        }
        if (!isOwnerMember) {
            System.out.println("Cannot move conversation as the owner (user " + ownerId + ") is not a member of workspace " + wsId + ".");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement("UPDATE Conversation SET workspace_id=? WHERE conversation_id=?")) {
            ps.setInt(1, wsId); ps.setInt(2, convId);
            ps.executeUpdate();
            System.out.println("Conversation moved.");
        }
    }

    /*---------------------------------------------------------------------
     |  Method viewMembers
     |
     |  Purpose:  Retrieves and displays all members of a workspace, showing
     |      each member's user_id, name, email, role, and join date.  Results
     |      are ordered by role then alphabetically by name.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified workspace_id exists in the Workspace table.
     |
     |  Post-condition: All WorkspaceMember rows for the workspace have been
     |      printed to stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void viewMembers(Connection conn, Scanner sc) throws SQLException {
        // Prompt for workspace ID and validate that it exists
        int wsId = DBUtil.promptInt(sc, "Workspace ID: ");
        if (!DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Display all members of the workspace with their fields
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT wm.user_id, u.name, u.email, wm.role, TO_CHAR(wm.joined_at,'YYYY-MM-DD') AS joined
                FROM WorkspaceMember wm JOIN LLMUser u ON wm.user_id=u.user_id
                WHERE wm.workspace_id=? ORDER BY wm.role, u.name
                """)) {
            ps.setInt(1, wsId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }
}
