import com.hydrometeocharlevoix.timakan.DataBag;
import com.hydrometeocharlevoix.timakan.Private;
import com.hydrometeocharlevoix.timakan.Report;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

class ReportTest {
    static String body;
    static Connection conn;
    static int lastReportNo;

    @BeforeAll
    static void setup() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String connString = "jdbc:postgresql://" + Private.getDbHost() + "/" + Private.getDbName();
        conn = DriverManager.getConnection(connString, Private.getDbUser(), Private.getDbPw());
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT num FROM emails WHERE station=2 ORDER BY sent DESC LIMIT 1");
        rs.next();
        lastReportNo = rs.getInt(1);
        body = "\n" +
                "\n" +
                "LevelSender\n" +
                "Serial: 284269\n" +
                "Location: Gouffre-Pont-362\n" +
                "Battery: 69%\n" +
                "Sample Rate: 6 hours\n" +
                "Report Rate: 24 hours\n" +
                "State: reporting \n" +
                "Start Report: 14/06/2021 12:00:00\n" +
                "\n" +
                "Logger 1\n" +
                "Location: \n" +
                "Type: Levelogger Edge, M10, 3.0040\n" +
                "Serial: 2082616\n" +
                "Battery: 97%\n" +
                "Total Logs: 40000 of 40000\n" +
                "Log Rate: 60 seconds\n" +
                "Memory Mode: continuous\n" +
                "Log Type: linear\n" +
                "State: stopped\n" +
                "Start Logger: 23/07/2018 13:42:29\n" +
                "\n" +
                "Logger 2\n" +
                "Location: \n" +
                "Type: Barologger Edge, M1.5, 3.0040\n" +
                "Serial: 2086251\n" +
                "Battery: 99%\n" +
                "Total Logs: 2 of 40000\n" +
                "Log Rate: 300 seconds\n" +
                "Memory Mode: slate\n" +
                "Log Type: linear\n" +
                "State: stopped\n" +
                "Start Logger: 23/02/2018 14:57:07\n" +
                "\n" +
                "\n" +
                "Logger 1 Samples\n" +
                "Time, Temperature( C), Level(m)\n" +
                "04/07/2021 12:00:00, 15.7080, 0.9609  \n" +
                "04/07/2021 18:00:00, 18.5590, 0.8129  \n" +
                "05/07/2021 00:00:00, 17.2900, 1.0402  \n" +
                "05/07/2021 06:00:00, 15.5830, 0.8007  \n" +
                "\n" +
                "Logger 2 Samples\n" +
                "Time, Temperature( C), Level(kPa)\n" +
                "04/07/2021 12:00:00, 19.8864, 101.489  \n" +
                "04/07/2021 18:00:00, 19.5130, 101.406  \n" +
                "05/07/2021 00:00:00, 14.5354, 101.501  \n" +
                "05/07/2021 06:00:00, 10.9689, 101.493  \n" +
                "\n" +
                "MESSAGES: Email report 21, LS reporting, L1 stopped, L2 stopped, \n" +
                "\n" +
                "\n";
    }
    @AfterAll
    static void teardown() throws SQLException {
        conn.close();
    }

    @Test
    void insertPopulatesCorrected() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        try {
            Report report = new Report("284269 LS Report "+ (++lastReportNo), body, date);
            report.insertIntoDb(conn);
            assert (report.getCorrected().size() == 4);
        } catch (Exception e) {
            System.err.println("Unplanned error while creating a report:"+(e.getMessage() != null ? e.getMessage() : "Null"));
            assert(false);
        }
    }

    @Test
    void toJsonFormatsProperly() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        try {
            Report report = new Report("284269 LS Report "+ (++lastReportNo), body, date);
            report.insertIntoDb(conn);
            String json = report.toJson();
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("Unplanned error while creating a report:"+(e.getMessage() != null ? e.getMessage() : "Null"));
            assert(false);
        }
    }

    @Test
    void actuallySendsByFtp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        try {
            Report report = new Report("284269 LS Report "+ (++lastReportNo), body, date);
            report.insertIntoDb(conn);
            report.sendByFtp(conn);
        } catch (Exception e) {
            System.err.println("Unplanned error while creating a report:"+(e.getMessage() != null ? e.getMessage() : "Null"));
            assert(false);
        }
    }
}