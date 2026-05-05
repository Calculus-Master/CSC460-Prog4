import java.sql.*;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class PersonaMenu
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides the Persona Management submenu of the LLM
 ||                  Platform Management System.  Allows operators to create
 ||                  new AI personas with a name and instruction set, update
 ||                  an existing persona's name or instructions, delete a
 ||                  persona (blocked if it has more than 5 active
 ||                  conversations), and view all personas owned by a user.
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
 ||  Class Methods:  show(Connection, Scanner) -- displays the Persona
 ||                      Management menu and dispatches to sub-operations.
 ||                  createPersona(Connection, Scanner) -- inserts a new
 ||                      Persona row.
 ||                  updatePersona(Connection, Scanner) -- updates the name
 ||                      or instructions of an existing Persona row.
 ||                  deletePersona(Connection, Scanner) -- deletes a Persona
 ||                      row after checking the active-conversation guard.
 ||                  viewPersonas(Connection, Scanner) -- displays all Persona
 ||                      rows owned by a given user.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class PersonaMenu {

    /*---------------------------------------------------------------------
     |  Method show
     |
     |  Purpose:  Displays the Persona Management submenu in a loop,
     |      reading the user's choice and dispatching to createPersona,
     |      updatePersona, deletePersona, or viewPersonas until the user
     |      selects "Back".
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

    /*---------------------------------------------------------------------
     |  Method createPersona
     |
     |  Purpose:  Creates a new Persona row owned by the specified user,
     |      storing the persona's name and instruction text.  The
     |      auto-generated persona_id is retrieved and printed.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified owner user_id exists in LLMUser.
     |
     |  Post-condition: A new Persona row has been inserted with
     |      created_at = SYSTIMESTAMP.  The new persona_id is printed
     |      to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method updatePersona
     |
     |  Purpose:  Updates either the name or the instructions of an existing
     |      Persona row.  Note that updating a persona does not retroactively
     |      affect conversations that have already snapshot-copied the
     |      persona's data; only future conversations are impacted.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified persona_id exists in the Persona table.
     |
     |  Post-condition: Either the name or instructions column of the target
     |      Persona row has been updated.  "Updated." is printed to stdout.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method deletePersona
     |
     |  Purpose:  Deletes an existing Persona row, subject to an
     |      active-conversation guard: if the persona is currently associated
     |      with more than 5 ACTIVE conversations, deletion is blocked and
     |      the operator is asked to archive some conversations first.
     |      If the guard passes, the operator must type "YES" to confirm.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified persona_id exists in the Persona table.
     |
     |  Post-condition: If the guard passed and the operator confirmed, the
     |      Persona row has been deleted.  Existing conversations retain
     |      their snapshot data (persona_name_snapshot, persona_instr_snapshot)
     |      so historical context is preserved.  Otherwise the database is
     |      unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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

    /*---------------------------------------------------------------------
     |  Method viewPersonas
     |
     |  Purpose:  Retrieves and displays all Persona rows owned by the
     |      specified user, showing each persona's ID, name, creation date,
     |      and a truncated instructions preview (first 60 characters).
     |      Results are ordered by persona_id.
     |
     |  Pre-condition:  conn is open.  sc is ready to read input.  The
     |      specified user_id exists in LLMUser.
     |
     |  Post-condition: All Persona rows for the user have been printed
     |      to stdout.  The database is unchanged.
     |
     |  Parameters:
     |      conn -- open JDBC Connection to the Oracle database.
     |      sc   -- Scanner for reading user input from stdin.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
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
