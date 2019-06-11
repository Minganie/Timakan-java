package com.hydrometeocharlevoix.timakan;

import java.sql.*;
import java.text.ParseException;

public class Reparser {
    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println("Running Reparser");
        Class.forName("org.postgresql.Driver");

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://" + Private.getDbHost() + "/" + Private.getDbName(), Private.getDbUser(), Private.getDbPw())) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, station, num, " +
                    "to_char(sent at time zone 'America/Montreal', 'YYYY-MM-DD HH24:MI:SS') as date, level_logger, pressure_logger, " +
                    "battery, body FROM emails");
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                int num = rs.getInt("num");
                int stn = rs.getInt("station");
                String body = rs.getString("body");
                String date = rs.getString("date");
//                try {
                    Report report = null;
//                    report = new Report(id, body, date);
//                    System.out.println(report.toString());
//                    report.updateIntoDb(conn);
//                } catch (BadTimeUnit | BadSensorConfiguration e) {
//                    System.out.println("Ignoring error " + e.getMessage());
//                }
            }
        } catch (SQLException e /*| ParseException e*/) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
