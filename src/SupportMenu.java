import java.sql.*;
import java.util.Scanner;

public class SupportMenu {

    public static void show(Connection conn, Scanner sc) throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("--- Support Tickets ---");
            System.out.println("1. Create Ticket");
            System.out.println("2. Assign to Agent");
            System.out.println("3. Resolve / Escalate Ticket");
            System.out.println("4. View Open Tickets");
            System.out.println("5. Back");
            int choice = DBUtil.promptInt(sc, "Choice: ");
            System.out.println();
            switch (choice) {
                case 1: createTicket(conn, sc);   break;
                case 2: assignAgent(conn, sc);    break;
                case 3: resolveTicket(conn, sc);  break;
                case 4: viewOpenTickets(conn);    break;
                case 5: back = true;              break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void createTicket(Connection conn, Scanner sc) throws SQLException {
        int userId = DBUtil.promptInt(sc, "User ID: ");
        System.out.println("Topic: 1. Billing  2. Model Error  3. Account  4. Feature Request  5. Other");
        int t = DBUtil.promptInt(sc, "Choice: ");
        String[] topics = {"Billing","Model Error","Account","Feature Request","Other"};
        String topic = (t >= 1 && t <= 5) ? topics[t-1] : "Other";
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO SupportTicket (user_id, topic_name, opened_time, status) " +
                "VALUES (?,?,SYSTIMESTAMP,'Open')", new String[]{"ticket_id"})) {
            ps.setInt(1, userId);
            ps.setString(2, topic);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) System.out.println("Ticket created with ID " + keys.getInt(1) + " (status: Open).");
        }
    }

    private static void assignAgent(Connection conn, Scanner sc) throws SQLException {
        // Show available agents
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT agent_id, name, specialty FROM SupportAgent ORDER BY agent_id")) {
            System.out.println("Available agents:");
            while (rs.next()) System.out.printf("  %d. %s (%s)%n",
                rs.getInt(1), rs.getString(2), rs.getString(3));
        }
        int ticketId = DBUtil.promptInt(sc, "Ticket ID: ");
        int agentId  = DBUtil.promptInt(sc, "Agent ID: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE SupportTicket SET agent_id=?, status='In Progress' WHERE ticket_id=?")) {
            ps.setInt(1, agentId); ps.setInt(2, ticketId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Agent assigned; status set to In Progress." : "Ticket not found.");
        }
    }

    private static void resolveTicket(Connection conn, Scanner sc) throws SQLException {
        int ticketId = DBUtil.promptInt(sc, "Ticket ID: ");
        System.out.println("Final status: 1. Resolved  2. Escalated");
        int s = DBUtil.promptInt(sc, "Choice: ");
        String status = (s == 1) ? "Resolved" : "Escalated";
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE SupportTicket SET status=?, closed_time=SYSTIMESTAMP WHERE ticket_id=?")) {
            ps.setString(1, status); ps.setInt(2, ticketId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Ticket " + status + "." : "Ticket not found.");
        }
    }

    private static void viewOpenTickets(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT st.ticket_id, u.email AS user_email, sa.name AS agent, " +
                "st.topic_name, TO_CHAR(st.opened_time,'YYYY-MM-DD HH24:MI') AS opened, st.status " +
                "FROM SupportTicket st " +
                "JOIN \"User\" u ON st.user_id=u.user_id " +
                "LEFT JOIN SupportAgent sa ON st.agent_id=sa.agent_id " +
                "WHERE st.status IN ('Open','In Progress') " +
                "ORDER BY st.opened_time")) {
            DBUtil.printResultSet(rs);
        }
    }
}
