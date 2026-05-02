import java.sql.*;
import java.util.Scanner;

public class DBUtil {

    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        int[] widths = new int[cols + 1];
        for (int i = 1; i <= cols; i++) {
            widths[i] = Math.max(meta.getColumnLabel(i).length(), 14);
        }
        StringBuilder sep = new StringBuilder("+");
        for (int i = 1; i <= cols; i++) {
            sep.append("-".repeat(widths[i] + 2)).append("+");
        }
        System.out.println(sep);
        StringBuilder header = new StringBuilder("|");
        for (int i = 1; i <= cols; i++) {
            header.append(String.format(" %-" + widths[i] + "s |", meta.getColumnLabel(i)));
        }
        System.out.println(header);
        System.out.println(sep);
        int rowCount = 0;
        while (rs.next()) {
            StringBuilder row = new StringBuilder("|");
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                if (val == null) val = "NULL";
                if (val.length() > widths[i]) val = val.substring(0, widths[i] - 3) + "...";
                row.append(String.format(" %-" + widths[i] + "s |", val));
            }
            System.out.println(row);
            rowCount++;
        }
        System.out.println(sep);
        System.out.println(rowCount + " row(s).");
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

    public static void displayTiers(Connection conn, boolean withCost)
    {
        String sql = "SELECT tier_id, tier_name" + (withCost ? ", cost" : "") + " FROM Tier ORDER BY tier_id";

        try(Statement s = conn.createStatement())
        {
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                if(withCost) {
                    System.out.printf("  %d. %s ($%.2f/mo)%n",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3));
                } else {
                    System.out.printf("  %d. %s%n",
                        rs.getInt(1), rs.getString(2));
                }
            }
        } catch(SQLException e)
        {
            System.out.println("Error fetching tiers: " + e.getMessage());
        }
    }
}
