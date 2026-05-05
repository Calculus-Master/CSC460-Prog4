import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DBUtil {

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

    // Helper for checking if an entity with a given ID exists in a specified table,
    // and print a message if not found
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
