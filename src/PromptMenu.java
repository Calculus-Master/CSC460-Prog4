import java.sql.*;
import java.util.Scanner;

public class PromptMenu {

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

        // If not, add a new record to WorkspacePromptTemplate
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at) VALUES (?,?,SYSTIMESTAMP)")) {
            ps.setInt(1, wsId);
            ps.setInt(2, tmplId);
            ps.executeUpdate();
            System.out.println("Template shared with workspace.");
        }
    }

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

    private static void update(Connection conn, String sql, String val, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, val); 
            ps.setInt(2, id); 
            ps.executeUpdate();
        }
    }
}
