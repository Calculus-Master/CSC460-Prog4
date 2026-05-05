import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*+----------------------------------------------------------------------
 ||
 ||  Class DBUtil
 ||
 ||         Author:  Ojas Sanghi, Saptarshi Mallick
 ||
 ||        Purpose:  Provides static utility methods shared across all menu
 ||                  classes.  Handles formatted ResultSet printing, console
 ||                  input prompting (with type coercion and optional-value
 ||                  support), tier display, and existence checks against
 ||                  arbitrary database tables.
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
 ||   Constructors:  None defined (static-only utility class).
 ||
 ||  Class Methods:  printResultSet(ResultSet) -- prints a ResultSet as a
 ||                      formatted, column-aligned table to stdout.
 ||                  promptInt(Scanner, String) -- prompts until a valid
 ||                      integer is entered; returns the integer.
 ||                  promptString(Scanner, String) -- prompts and returns
 ||                      the trimmed input string.
 ||                  promptOptional(Scanner, String) -- prompts with a
 ||                      "(leave blank to skip)" note; returns null if blank.
 ||                  promptOptionalInt(Scanner, String) -- like promptOptional
 ||                      but parses to int; returns -1 if blank or invalid.
 ||                  displayTiers(Connection, boolean) -- prints available
 ||                      subscription tiers, optionally with monthly cost.
 ||                  checkExists(Connection, int, String, String) -- returns
 ||                      true if a row with the given PK exists in a table.
 ||
 ||  Inst. Methods:  None.
 ||
 ++-----------------------------------------------------------------------*/
public class DBUtil {

    /*---------------------------------------------------------------------
     |  Method printResultSet
     |
     |  Purpose:  Prints the contents of a JDBC ResultSet to standard output
     |      as a plain-text, column-aligned table.  The method first collects
     |      all column headers and all row values into memory so that each
     |      column's display width can be determined before any output is
     |      printed.  It then prints a separator line of dashes, the header
     |      row, all data rows, another separator line, and a count of the
     |      number of rows returned.  NULL database values are printed as
     |      the literal string "NULL".
     |
     |  Pre-condition:  rs is a valid, open ResultSet positioned before the
     |      first row.  The ResultSet has at least one column.
     |
     |  Post-condition: All rows of rs have been consumed (cursor is after
     |      the last row).  The formatted table plus row count have been
     |      written to stdout.
     |
     |  Parameters:
     |      rs -- the open ResultSet to print.  Column labels are read from
     |          its ResultSetMetaData; all values are retrieved as Strings.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        int[] widths = new int[cols + 1];

        // collect headers
        ArrayList<String> headers = new ArrayList<>();
        for (int i = 1; i <= cols; i++) {
            headers.add(meta.getColumnLabel(i));
            widths[i] = headers.get(i - 1).length();
        }

        // collect all row vaues
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols + 1];
            for (int i = 1; i <= cols; i++) {
                row[i] = rs.getString(i) == null ? "NULL" : rs.getString(i);
                widths[i] = Math.max(widths[i], row[i].length());
            }
            rows.add(row);
        }

        System.out.println();

        // print starting line of ---
        for (int i = 1; i <= cols; i++)
            for (int j = 0; j < widths[i]; j++)
                System.out.print("-");
        System.out.println();

        // print header
        for (int i = 1; i <= cols; i++) {
            String endChar = "\n";
            if (i < cols)
                endChar = ", ";
            System.out.printf("%-" + widths[i] + "s%s", headers.get(i - 1), endChar);
        }

        // print all row values
        int rowCount = 0;
        for (String[] row : rows) {
            for (int i = 1; i <= cols; i++) {
                String endChar = "\n";
                if (i < cols)
                    endChar = ", ";
                System.out.printf("%-" + widths[i] + "s%s", row[i], endChar);
            }
            rowCount++;
        }

        // print ending line of ---
        for (int i = 1; i <= cols; i++)
            for (int j = 0; j < widths[i]; j++)
                System.out.print("-");
        System.out.println();

        // print # rows
        System.out.println("\n# Entries: " + rowCount);
        System.out.println();
    }

    /*---------------------------------------------------------------------
     |  Methods promptInt / promptString / promptOptional / promptOptionalInt
     |
     |  Purpose:  A family of console input helpers used throughout the menu
     |      classes to read and coerce user input from stdin.
     |
     |      promptInt     -- prints the prompt string, reads a line, and
     |          retries until the user enters a parseable integer; returns
     |          the integer value.
     |      promptString  -- prints the prompt string and returns the
     |          trimmed input line; does not retry on blank input.
     |      promptOptional -- appends "(leave blank to skip)" to the prompt,
     |          then returns null if the user enters a blank line, or the
     |          trimmed non-blank string otherwise.
     |      promptOptionalInt -- calls promptOptional and attempts to parse
     |          the result as an integer; returns -1 if the input was blank
     |          or unparseable.
     |
     |  Pre-condition:  sc is a valid, open Scanner connected to stdin.
     |      prompt is a non-null, non-empty display string.
     |
     |  Post-condition: Exactly one line has been consumed from sc (promptInt
     |      may consume multiple lines until a valid integer is entered).
     |      The prompt has been printed to stdout.
     |
     |  Parameters:
     |      sc     -- Scanner for reading console input.
     |      prompt -- label string to display before the input cursor.
     |
     |  Returns:  promptInt: the parsed integer entered by the user.
     |      promptString: the trimmed input string (may be empty).
     |      promptOptional: the trimmed input string, or null if blank.
     |      promptOptionalInt: the parsed integer, or -1 if blank/invalid.
     *-------------------------------------------------------------------*/
    public static int promptInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid integer.");
            }
        }
    }

    public static String promptString(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    public static String promptOptional(Scanner sc, String prompt) {
        System.out.print(prompt + " (leave blank to skip): ");
        String val = sc.nextLine().trim();
        return val.isEmpty() ? null : val;
    }

    public static int promptOptionalInt(Scanner sc, String prompt) {
        String input = promptOptional(sc, prompt);
        if (input == null)
            return -1;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, ignoring.");
            return -1;
        }
    }

    /*---------------------------------------------------------------------
     |  Method displayTiers
     |
     |  Purpose:  Queries the Tier table and prints each tier's ID and name
     |      to stdout, with an optional monthly cost column.  Used to
     |      present a numbered tier selection list to the user before
     |      operations that require a tier ID (e.g., adding a user or
     |      previewing available plans).
     |
     |  Pre-condition:  conn is an open, valid JDBC Connection to the Oracle
     |      database, and the Tier table exists and contains at least one row.
     |
     |  Post-condition: Each available tier has been printed to stdout in
     |      ascending tier_id order.  The database and conn are unchanged.
     |
     |  Parameters:
     |      conn     -- open JDBC Connection to the Oracle database.
     |      withCost -- if true, each tier line also shows the monthly cost
     |                  formatted as "$X.XX/mo"; if false, only the tier
     |                  ID and name are shown.
     |
     |  Returns:  None.
     *-------------------------------------------------------------------*/
    public static void displayTiers(Connection conn, boolean withCost) {
        String sql = "SELECT tier_id, tier_name" + (withCost ? ", cost" : "") + " FROM Tier ORDER BY tier_id";

        try (Statement s = conn.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                if (withCost) {
                    System.out.printf("  %d. %s ($%.2f/mo)%n",
                            rs.getInt(1), rs.getString(2), rs.getDouble(3));
                } else {
                    System.out.printf("  %d. %s%n",
                            rs.getInt(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching tiers: " + e.getMessage());
        }
    }

    /*---------------------------------------------------------------------
     |  Method checkExists
     |
     |  Purpose:  Determines whether a row with the given integer primary key
     |      exists in the specified database table.  If no matching row is
     |      found, an error message identifying the table and missing ID is
     |      printed to stdout.  This method is used as a guard before any
     |      operation that assumes a particular entity is present (e.g.,
     |      before updating or deleting a user, conversation, or persona).
     |
     |  Pre-condition:  conn is an open, valid JDBC Connection to the Oracle
     |      database.  tableName and pkColumn are valid, existing table and
     |      column names in the connected schema.  pkID is a positive integer.
     |
     |  Post-condition: The database is unchanged.  If the row does not exist,
     |      a "not found" message has been printed to stdout.
     |
     |  Parameters:
     |      conn      -- open JDBC Connection to the Oracle database.
     |      pkID      -- integer primary key value to search for.
     |      tableName -- name of the database table to search.
     |      pkColumn  -- name of the primary key column in that table.
     |
     |  Returns:  true if a row with pkID exists in tableName; false otherwise.
     *-------------------------------------------------------------------*/
    public static boolean checkExists(Connection conn, int pkID, String tableName, String pkColumn)
            throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE " + pkColumn + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pkID);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            if (!exists)
                System.out.println(tableName + " with ID " + pkID + " not found.");
            return exists;
        }
    }
}
