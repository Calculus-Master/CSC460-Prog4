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
                case 1: addTemplate(conn, sc);   break;
                case 2: updateTemplate(conn, sc);break;
                case 3: shareTemplate(conn, sc); break;
                case 4: viewTemplates(conn, sc); break;
                case 5: back = true;             break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void addTemplate(Connection conn, Scanner sc) throws SQLException {
        int ownerId  = DBUtil.promptInt(sc, "Owner User ID: ");
        String name  = DBUtil.promptString(sc, "Template name: ");
        String text  = DBUtil.promptString(sc, "Template text: ");
        String cat   = DBUtil.promptString(sc, "Category: ");
        System.out.println("Visibility: 1. PRIVATE  2. SHARED");
        int v = DBUtil.promptInt(sc, "Choice: ");
        String vis = (v == 2) ? "SHARED" : "PRIVATE";

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at) " +
                "VALUES (?,?,?,?,?,SYSTIMESTAMP)", new String[]{"template_id"})) {
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
        int tmplId = DBUtil.promptInt(sc, "Template ID: ");
        System.out.println("Update: 1. Name  2. Text  3. Category  4. Visibility");
        int f = DBUtil.promptInt(sc, "Field: ");
        switch (f) {
            case 1: {
                String val = DBUtil.promptString(sc, "New name: ");
                update(conn, "UPDATE PromptTemplate SET name=? WHERE template_id=?", val, tmplId);
                break;
            }
            case 2: {
                String val = DBUtil.promptString(sc, "New text: ");
                update(conn, "UPDATE PromptTemplate SET text=? WHERE template_id=?", val, tmplId);
                break;
            }
            case 3: {
                String val = DBUtil.promptString(sc, "New category: ");
                update(conn, "UPDATE PromptTemplate SET category=? WHERE template_id=?", val, tmplId);
                break;
            }
            case 4: {
                System.out.println("Visibility: 1. PRIVATE  2. SHARED");
                int v = DBUtil.promptInt(sc, "Choice: ");
                String vis = (v == 2) ? "SHARED" : "PRIVATE";
                update(conn, "UPDATE PromptTemplate SET visibility=? WHERE template_id=?", vis, tmplId);
                break;
            }
            default: System.out.println("Invalid field."); return;
        }
        System.out.println("Updated.");
    }

    private static void shareTemplate(Connection conn, Scanner sc) throws SQLException {
        int tmplId = DBUtil.promptInt(sc, "Template ID: ");
        int wsId   = DBUtil.promptInt(sc, "Workspace ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at) VALUES (?,?,SYSTIMESTAMP)")) {
            ps.setInt(1, wsId);
            ps.setInt(2, tmplId);
            ps.executeUpdate();
            System.out.println("Template shared with workspace.");
        } catch (SQLException e) {
            // ORA-20002 from trigger = not SHARED visibility
            if (e.getErrorCode() == 20002) System.out.println("Error: " + e.getMessage().split("\n")[0]);
            else if (e.getErrorCode() == 1)  System.out.println("Already shared with this workspace.");
            else throw e;
        }
    }

    private static void viewTemplates(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT template_id, name, category, visibility, TO_CHAR(created_at,'YYYY-MM-DD') AS created " +
                "FROM PromptTemplate WHERE owner_user_id=? ORDER BY template_id")) {
            ps.setInt(1, userId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }

    private static void update(Connection conn, String sql, String val, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, val); ps.setInt(2, id); ps.executeUpdate();
        }
    }
}
