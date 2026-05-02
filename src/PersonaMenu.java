import java.sql.*;
import java.util.Scanner;

public class PersonaMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("""
                --- Persona Management ---
                1. Create Persona
                2. Update Persona
                3. Delete Persona
                4. View My Personas
                5. Back
                """);
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> createPersona(conn, sc);
                case 2 -> updatePersona(conn, sc);
                case 3 -> deletePersona(conn, sc);
                case 4 -> viewPersonas(conn, sc);
                case 5 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void createPersona(Connection conn, Scanner sc) throws SQLException {
        // Prompt for owner user ID and validate that it exists
        int ownerId = DBUtil.promptInt(sc, "Owner User ID: ");
        if (!DBUtil.checkExists(conn, ownerId, "LLMUser", "user_id")) return;

        // Prompt for persona name and instructions
        String name = DBUtil.promptString(sc, "Persona name: ");
        String instr = DBUtil.promptString(sc, "Instructions: ");

        // Create new persona with given info
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Persona (owner_user_id, name, instructions, created_at) VALUES (?,?,?,SYSTIMESTAMP)", new String[]{"persona_id"})) {
            ps.setInt(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, instr);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Persona created with ID " + keys.getInt(1) + ".");
        }
    }

    private static void updatePersona(Connection conn, Scanner sc) throws SQLException {
        // Prompt for persona ID and validate that it exists
        int personaId = DBUtil.promptInt(sc, "Persona ID: ");
        if (!DBUtil.checkExists(conn, personaId, "Persona", "persona_id")) return;

        // Prompt for which field to update, and the new value, then perform the update
        System.out.println("Update: 1. Name  2. Instructions");
        int f = DBUtil.promptInt(sc, "Field: ");
        if (f == 1) {
            String val = DBUtil.promptString(sc, "New name: ");
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Persona SET name=? WHERE persona_id=?")) {
                ps.setString(1, val); 
                ps.setInt(2, personaId); 
                ps.executeUpdate();
            }
        } else {
            String val = DBUtil.promptString(sc, "New instructions: ");
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Persona SET instructions=? WHERE persona_id=?")) {
                ps.setString(1, val); 
                ps.setInt(2, personaId); 
                ps.executeUpdate();
            }
        }
        System.out.println("Updated.");
    }

    private static void deletePersona(Connection conn, Scanner sc) throws SQLException {
        // Prompt for persona ID and validate that it exists
        int personaId = DBUtil.promptInt(sc, "Persona ID: ");
        if (!DBUtil.checkExists(conn, personaId, "Persona", "persona_id")) return;

        // Pre-check: count ACTIVE conversations
        int activeCount = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Conversation WHERE persona_id=? AND status='ACTIVE'")) {
            ps.setInt(1, personaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) activeCount = rs.getInt(1);
        }
        if (activeCount > 5) {
            System.out.println("Cannot delete: persona has " + activeCount + " active conversations. Archive some conversations first.");
            return;
        }

        // Confirm deletion
        String confirm = DBUtil.promptString(sc, "Type YES to confirm deletion: ");
        if (confirm.equals("YES")) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Persona WHERE persona_id=?")) {
                ps.setInt(1, personaId);
                ps.executeUpdate();
                System.out.println("Persona deleted.");
            }
        } else System.out.println("Cancelled.");
    }

    private static void viewPersonas(Connection conn, Scanner sc) throws SQLException {
        // Prompt for owner user ID and validate that it exists
        int userId = DBUtil.promptInt(sc, "User ID: ");
        if (!DBUtil.checkExists(conn, userId, "LLMUser", "user_id")) return;

        // Display all personas created by a user
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT persona_id, name, TO_CHAR(created_at,'YYYY-MM-DD') AS created,
                SUBSTR(instructions,1,60) AS instructions_preview
                FROM Persona WHERE owner_user_id=? ORDER BY persona_id
                """)) {
            ps.setInt(1, userId);
            DBUtil.printResultSet(ps.executeQuery());
        }
    }
}
