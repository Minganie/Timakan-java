package com.hydrometeocharlevoix.timakan;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;

/**
 * Main class, with the entry point for the Timakan program and various static functions that did not require their
 * own class.
 */
public class Main {
    // Set logging
    private static Logger logger = LogManager.getLogger(Main.class);

    /**
     * Utility class to send a warning email (plain text, utf 8) to the address defined as a static in the class. Only
     * needs subject and body. Uses timakan system email as the sender address.
     * @param subject
     * @param body
     */
    public static void sendWarningEmail(String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.mail.yahoo.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(Private.getTimakanEmail(),Private.getTimakanPw());
                        }
                    });

            Message message = new MimeMessage(session);
            message.addHeader("Content-type", "text/plain; charset=UTF-8");
            message.addHeader("format", "flowed");
            message.addHeader("Content-Transfer-Encoding", "8bit");
            message.setSentDate(new Date());

            message.setFrom(new InternetAddress(Private.getTimakanEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(Private.warningEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        }
        catch (Exception e) {
            logger.error("Unexpected error while sending a warning email: " + (e.getMessage() == null ? "NullPointerException?" : e.getMessage()));
        }
    }

    /**
     * Workhorse of the Timakan system. Checks home station email, opens inbox, verifies if each message is a data
     * report from a LevelSender. If so, makes a @see com.hydrometeocharlevoix.timakan.Report out of it and saves it to database. If the report
     * encounters a formatting issue, sends a warning email and a copy of the report to the address specified as a
     * static in the class.
     * @param conn
     * @throws SQLException
     * @throws BadTimezoneException
     */
    public static void checkEmail(Connection conn) throws SQLException, BadTimezoneException {
        checkDbTz(conn);
        try {

            //create properties field
            Properties properties = new Properties();

            properties.put("mail.pop3.host", "pop.mail.yahoo.com");
            properties.put("mail.pop3.port", "995");
            properties.put("mail.pop3.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("pop3s");

            store.connect("pop.mail.yahoo.com", Private.getHomeStationEmail(), Private.getHomeStationPw());

            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                if(message.getSubject().contains("LS Report")) {
                    try {
                        Report report = new Report(message);
//                        logger.info(report.toString());
                        report.insertIntoDb(conn);
                        message.setFlag(Flags.Flag.DELETED, true);
                    } catch (BadSensorConfiguration bsce) {
                        logger.error("Bad sensor config, sending warning email with report copy");
                        Gson gson = new Gson();
                        String body = gson.toJson(bsce.getReport());
                        sendWarningEmail("Avertissement de Timakan: mauvaise configuration", bsce.getMessage() + "\nCopie du rapport au cas où:\n" + body);
                        message.setFlag(Flags.Flag.DELETED, true);
                    } catch (UnknownStationException use) {
                        logger.error("Unknown station (i.e. not in water_stations table), sending warning email with report copy");
                        Gson gson = new Gson();
                        String body = gson.toJson(use.getReport());
                        sendWarningEmail("Avertissement de Timakan: station manquante", use.getMessage() + "\nCopie du rapport au cas où:\n" + body);
                        message.setFlag(Flags.Flag.DELETED, true);
                    } catch (Exception e) {
                        logger.error("Unplanned error while checking emails: " + (e.getMessage() == null ? "NullPointerException?" : e.getMessage()));
                    }
                }
            }

            //close the store and folder objects
            emailFolder.close(true);
            store.close();

        } catch (Exception e) {
            logger.error("Encountered error while reading emails: " + (e.getMessage() == null ? "" : e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Data integrity verification method. Since we are using a String as the input date for each data point, and the
     * database uses a TIMESTAMP WITH TIMEZONE, it is crucial that the input date string and the database agree on the
     * timezone.
     * @param conn
     * @throws SQLException
     * @throws BadTimezoneException
     */
    private static void checkDbTz(Connection conn) throws SQLException, BadTimezoneException {
        // Check time zone coherence between this program and the db server conn
        // This is important because Postgres uses timestamp with time zone and you're giving it a string SQlite made
        LocalDateTime now = LocalDateTime.now();
        ResultSet ras = conn.createStatement().executeQuery("SELECT to_char(NOW(), 'YYYY-MM-DD HH24')");
        if(ras.next()) {
            String pghour = ras.getString(1);
            String javahour = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
            if(!pghour.equalsIgnoreCase(javahour))
                throw new BadTimezoneException("Postgres (" + pghour + ") et Java (" + javahour + ") ne sont pas d'accord sur l'heure: y a-t-il un problème de fuseau horaire? Malheureusement, l'exécution de Timakan échouera jusqu'à ce que le problème soit réglé...");
        }
    }

    /**
     * Updates csv files from database data for use by the hydrometeocharlevoix.com website. Runs after new data reports
     * have been input into the database by @see com.hydrometeocharlevoix.timakan.Main#checkEmail().
     * @param conn
     * @throws SQLException
     */
    public static void updateCsvs(Connection conn) throws SQLException {
        String stn_sql = "SELECT serial FROM water_stations";
        ResultSet stations = conn.createStatement().executeQuery(stn_sql);
        PreparedStatement csv_stmt = conn.prepareStatement("SELECT timakan_export_historic(?), timakan_export_year(?), timakan_export_week(?)");
        while(stations.next()) {
            csv_stmt.setInt(1, stations.getInt("serial"));
            csv_stmt.setInt(2, stations.getInt("serial"));
            csv_stmt.setInt(3, stations.getInt("serial"));
            csv_stmt.execute();
//            logger.info("Successfully executed Timakan and updated the csv's for station " + stations.getInt("serial"));
        }
    }

    /**
     * Utility function to pretty print execution time, which is measured in nanoseconds, and nobody wants to try to
     * guess how long 856226556 is.
     * @param nanos
     * @return
     */
    public static String formatTime(long nanos) {
        long micros = 1000L;
        long millis = micros*1000L;
        long secs = millis*1000L;
        long mins = secs*60L;
        StringBuilder b = new StringBuilder();
        if(nanos/mins > 0) {
            b.append(nanos/mins + "m ");
            nanos = nanos%mins;
        }
        if(nanos/secs > 0) {
            b.append(nanos/secs + "s ");
            nanos = nanos%secs;
        }
        if(nanos/millis > 0) {
            b.append(nanos/millis + "ms ");
            nanos = nanos%millis;
        }
        if(nanos/micros > 0) {
            b.append(nanos/micros + "us ");
            nanos = nanos%micros;
        }
        b.append(nanos + "ns");
        return b.toString();
    }

    /**
     * Le main. Connects to database; checks email; updates csvs; quits.
     * @param args
     */
    public static void main(String[] args) {

        // Setup
        try {
            long startTime = System.nanoTime();
            Class.forName("org.postgresql.Driver");
            String connString = "jdbc:postgresql://" + Private.getDbHost() + "/" + Private.getDbName();
            try (Connection conn = DriverManager.getConnection(connString, Private.getDbUser(), Private.getDbPw())) {

                // Check email
                logger.info("Starting email check");
                checkEmail(conn);
                long aftermail = System.nanoTime();
                logger.info("Done checking emails in " + formatTime(aftermail-startTime));

                // Update csvs
                logger.info("Starting update of csvs");
                updateCsvs(conn);
                long aftercsvs = System.nanoTime();
                logger.info("Done updating csvs in " + formatTime(aftercsvs - aftermail));

            } catch (SQLException e) {
                logger.error("Encountered SQL error in Main::main [180-190]: " + e.getMessage());
            } catch (BadTimezoneException e) {
                logger.error("Timezone issue: check your db config; sending warning email");
                sendWarningEmail("Avertissement de Timakan: problème de configuration du serveur", e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            logger.error("Problem with the jdbc PostgreSQL driver: " + e.getMessage());
        }
    }
}
