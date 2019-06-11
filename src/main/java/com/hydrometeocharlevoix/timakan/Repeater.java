package com.hydrometeocharlevoix.timakan;

import java.sql.*;
import java.text.ParseException;

/**
 * One use class, to copy existing data from a database rather than an email mailbox.
 */
public class Repeater {
    private static final String ls_path = "C:\\Users\\Myriam\\Documents\\Timakan\\levelsender.sqlite";

    public static void other_main(String[] args) throws ClassNotFoundException {
        System.out.println("Running Repeater");
        Class.forName("org.postgresql.Driver");
        Class.forName("org.sqlite.JDBC"); // DONT REMOVE THIS or freakin Maven assembly overwrites one

        try (Connection ls_conn = DriverManager.getConnection("jdbc:sqlite:" + ls_path);
             Connection tim_conn = DriverManager.getConnection("jdbc:postgresql://" + Private.getDbHost() + "/" + Private.getDbName(), Private.getDbUser(), Private.getDbPw())) {
            PreparedStatement stmt = ls_conn.prepareStatement("SELECT * FROM ReceivedEmail WHERE Body not like '%TEST EMAIL%' and subject like '%LS Report%'");
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                String subject = rs.getString("Subject");
                String date = rs.getString("ReceivedDate");
                String body = rs.getString("Body");
                try {
                    Report report = new Report(subject, body, date);
                    System.out.println(report.toString());
                    report.insertIntoDb(tim_conn);
                } catch (BadTimeUnit | UnknownStationException | BadSensorConfiguration e) {
                    System.out.println("Ignoring error " + e.getMessage());
                }
            }
        } catch (SQLException | ParseException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
