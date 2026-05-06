import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class PromptMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Prompt Library submenu of the LLM Platform
 ||                  Management System.  Allows operators to add new prompt
 ||                  templates (with a name, text, category, and visibility),
 ||                  update any of those fields, share a template with a
 ||                  workspace (creating a WorkspacePromptTemplate record),
 ||                  and view all templates owned by a given user.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Prompt Library
 ||                      menu and dispatches to sub-operations.
 ||                  addTemplate(Connection, Scanner) -- inserts a new
 ||                      PromptTemplate row.
 ||                  updateTemplate(Connection, Scanner) -- updates one field
 ||                      of an existing PromptTemplate row.
 ||                  shareTemplate(Connection, Scanner) -- inserts a
 ||                      WorkspacePromptTemplate row linking a template to a
 ||                      workspace, if not already shared.
 ||                  viewTemplates(Connection, Scanner) -- displays all
 ||                      PromptTemplate rows owned by a given user.
 ||                  update(Connection, String, String, int) -- helper that
 ||                      executes a parameterized string-value UPDATE.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class PromptMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Prompt Library submenu in a loop, reading
     |      the user's choice and dispatching to addTemplate, updateTemplate,
     |      shareTemplate, or viewTemplates until the user selects "Back".
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
                --- Prompt Library ---
                1. Add Template
                2. Update Template
                3. Share Template with Workspace
                4. View Templates for User
                5. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> addTemplate(conn, sc);
                case 2 -> updateTemplate(conn, sc);
                case 3 -> shareTemplate(conn, sc);
                case 4 -> viewTemplates(conn, sc);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    /*---------------------------------------------------------------------
     |  Method addTemplate
     |
     |  Purpose:  Inserts a new PromptTemplate row for a given owner user.
     |      Collects the template name, full text, category, and visibility
     |      (PRIVATE or SHARED) from the operator, then performs the insert.
     |      The auto-generated template_id is printed on success.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified owner user_id exists in LLMUser.
     |
     |  Post-condition: A new PromptTemplate row has been inserted with
     |      created_at = SYSTIMESTAMP.  The new template_id is printed.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void addTemplate(Connection conn, Scanner sc) throws SQLException {
        // Prompt for owner user ID and validate that it exists
        int ownerId = DBUtil.promptInt(sc, "Owner User ID: ");
        if (!DBUtil.checkExists(conn, ownerId, "LLMUser", "user_id")) return;

        // Prompt for template details: name, text, category, visibility
        String name = DBUtil.promptString(sc, "Template name: ");
        String text = DBUtil.promptString(sc, "Template text: ");
        String cat = DBUtil.promptString(sc, "Category: ");
        System.out.println("Visibility: 1. PRIVATE  2. SHARED");
        int v = DBUtil.promptInt(sc, "Choice: ");
        String vis = (v == 2) ? "SHARED" : "PRIVATE";

        String sql = "INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at) VALUES (?,?,?,?,?,SYSTIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"template_id"})) {
            ps.setInt(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, text);
            ps.setString(4, cat);
            ps.setString(5, vis);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Template created with ID " + keys.getInt(1) + ".");
        }
    }

    /*---------------------------------------------------------------------
     |  Method updateTemplate
     |
     |  Purpose:  Updates one field (name, text, category, or visibility)
     |      of an existing PromptTemplate row.  Presents a field selection
     |      menu, prompts for the new value, and delegates to the update()
     |      helper to execute the parameterized SQL UPDATE.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified template_id exists in the PromptTemplate table.
     |
     |  Post-condition: The selected field of the target PromptTemplate row
     |      has been updated.  "Updated." is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void updateTemplate(Connection conn, Scanner sc) throws SQLException {
        // Prompt for template ID and validate that it exists
        int tmplId = DBUtil.promptInt(sc, "Template ID: ");
        if (!DBUtil.checkExists(conn, tmplId, "PromptTemplate", "template_id")) return;
        
        // Prompt for which field to update, and the new value, then perform the update
        System.out.println("Update: 1. Name  2. Text  3. Category  4. Visibility");
        int f = DBUtil.promptInt(sc, "Field: ");
        switch (f) {
            case 1 -> {
                String val = DBUtil.promptString(sc, "New name: ");
                update(conn, "UPDATE PromptTemplate SET name=? WHERE template_id=?", val, tmplId);
            }
            case 2 -> {
                String val = DBUtil.promptString(sc, "New text: ");
                update(conn, "UPDATE PromptTemplate SET text=? WHERE template_id=?", val, tmplId);
            }
            case 3 -> {
                String val = DBUtil.promptString(sc, "New category: ");
                update(conn, "UPDATE PromptTemplate SET category=? WHERE template_id=?", val, tmplId);
            }
            case 4 -> {
                System.out.println("Visibility: 1. PRIVATE  2. SHARED");
                int v = DBUtil.promptInt(sc, "Choice: ");
                String vis = (v == 2) ? "SHARED" : "PRIVATE";
                update(conn, "UPDATE PromptTemplate SET visibility=? WHERE template_id=?", vis, tmplId);
            }
            default -> {
                System.out.println("Invalid field.");
                return;
            }
        }
        System.out.println("Updated.");
    }

    /*---------------------------------------------------------------------
     |  Method shareTemplate
     |
     |  Purpose:  Shares a prompt template with a workspace by inserting a
     |      WorkspacePromptTemplate row.  Before inserting, the method verifies
     |      that the template and workspace both exist, and that the template
     |      is not already shared with that workspace (duplicates are rejected
     |      gracefully).
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      PromptTemplate and Workspace tables exist.
     |
     |  Post-condition: If not already shared, a new WorkspacePromptTemplate
     |      row has been inserted with shared_at = SYSTIMESTAMP.  Otherwise
     |      the database is unchanged and an informational message is shown.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void shareTemplate(Connection conn, Scanner sc) throws SQLException {
        // Prompt for template ID and validate that it exists
        int tmplId = DBUtil.promptInt(sc, "Template ID: ");
        if (!DBUtil.checkExists(conn, tmplId, "PromptTemplate", "template_id")) return;

        // Prompt for workspace ID to share with and validate that it exists
        int wsId = DBUtil.promptInt(sc, "Workspace ID: ");
        if (!DBUtil.checkExists(conn, wsId, "Workspace", "workspace_id")) return;

        // Check if template is already shared in the workspace
        boolean isShared = false;
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM WorkspacePromptTemplate WHERE workspace_id=? AND template_id=?")) {
            ps.setInt(1, wsId);
            ps.setInt(2, tmplId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) isShared = true;
        }
        if (isShared) {
            System.out.println("Template is already shared with this workspace.");
            return;
        }

        // Check if the template visibility is PRIVATE
        boolean isPrivate = false;
        try (PreparedStatement ps = conn.prepareStatement("SELECT visibility FROM PromptTemplate WHERE template_id=?")) {
            ps.setInt(1, tmplId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String vis = rs.getString("visibility");
                isPrivate = vis.equals("PRIVATE");
            }
        }
        if (isPrivate) {
            System.out.println("Template is set to PRIVATE and cannot be shared in a Workspace.");
            return;
        }

        // If not, add a new record to WorkspacePromptTemplate
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at) VALUES (?,?,SYSTIMESTAMP)")) {
            ps.setInt(1, wsId);
            ps.setInt(2, tmplId);
            ps.executeUpdate();
            System.out.println("Template shared with workspace.");
        }
    }

    /*---------------------------------------------------------------------
     |  Method viewTemplates
     |
     |  Purpose:  Retrieves and displays all PromptTemplate rows owned by
     |      the specified user, showing each template's ID, name, category,
     |      visibility, and creation date.  Results are ordered by template_id.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: All PromptTemplate rows for the user have been
     |      printed to stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void viewTemplates(Connection conn, Scanner sc) throws SQLException {
        // Prompt for owner user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Display all templates created by a user
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT template_id, name, category, visibility, TO_CHAR(created_at,'YYYY-MM-DD') AS created
                FROM PromptTemplate WHERE owner_user_id=? ORDER BY template_id
                """)) {
            ps.setInt(1, userId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }

    /*---------------------------------------------------------------------
     |  Method update
     |
     |  Purpose:  Executes a parameterized SQL UPDATE statement, binding a
     |      string as the first parameter and an integer primary key as the
     |      second.  This helper centralizes PreparedStatement lifecycle
     |      management for all four field-update cases in updateTemplate.
     |
     |  Pre-condition:  conn is open.  sql is a two-parameter UPDATE of the
     |      form "UPDATE PromptTemplate SET <col>=? WHERE template_id=?".
     |      val is the new string value.  id is the primary key of the row
     |      to update.
     |
     |  Post-condition: The targeted field of the PromptTemplate row has
     |      been updated.  The PreparedStatement has been closed.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sql  -- parameterized UPDATE statement to execute.
     |      val  -- new string value to bind as the first parameter.
     |      id   -- primary key value to bind as the second parameter.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    private static void update(Connection conn, String sql, String val, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, val); 
            ps.setInt(2, id); 
            ps.executeUpdate();
        }
    }
}
