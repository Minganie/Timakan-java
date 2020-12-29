package com.hydrometeocharlevoix.timakan;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class modelizing one data report, i.e. one data email from one LevelSender.
 */
public class Report {
//    private int id; // was only necessary for Reparser
    private int station;
    private int no;
    private int battery;
    private int sample_rate; // minutes
    private int report_rate; // minutes
    private String state;
    private String start_report; // Date, DD/MM/YYYY HH24:MI:SS // DID I MENTION I HATE DATE TIIIIIMES
    private String sentDate; // JAVA yyyy-MM-dd HH:mm:ss // POSTGRES YYYY-MM-DD HH24:MI:SS // I HATE DATE TIMES
    private String body;
    // Levelogger info
    private int levelLogger = 0;
    private String ll_type;
    private String ll_model;
    private String ll_v;
    private int ll_battery; // %
    private int ll_n_logs;
    private int ll_max_logs;
    private int ll_rate; // seconds
    private String ll_mem;
    private String ll_log_type;
    private String ll_state;
    private String ll_start_logger; // date, DD/MM/YYYY HH24:MI:SS // DID I MENTION I HATE DATE TIIIIIMES
    // Barologger info
    private int baroLogger = 0;
    private String pl_type;
    private String pl_model;
    private String pl_v;
    private int pl_battery; // %
    private int pl_n_logs;
    private int pl_max_logs;
    private int pl_rate; // seconds
    private String pl_mem;
    private String pl_log_type;
    private String pl_state;
    private String pl_start_logger; // date, DD/MM/YYYY HH24:MI:SS // DID I MENTION I HATE DATE TIIIIIMES

    // Observations
    private List<Observation> levels = new ArrayList<>();
    private List<Observation> pressures = new ArrayList<>();
    private List<List<Observation>> obs = new ArrayList<>();


    // GETTERS
    public int getStation() {
        return station;
    }

    public int getNo() { return no; }

    public String getSentDate() {
        return sentDate;
    }

    public String getBody() {
        return body;
    }

    public int getLevelLogger() {
        return levelLogger;
    }

    public int getBaroLogger() {
        return baroLogger;
    }

    public List<Observation> getLevels() {
        return levels;
    }

    public List<Observation> getPressures() {
        return pressures;
    }

    // PARSERS

    /**
     * Get station serial from the line in the email that goes "Serial: 384235"
     * @param mumbo
     * @return station serial number
     * @throws ParseException
     */
    private int parseStationSerial(String mumbo) throws ParseException {
        Pattern pattern = Pattern.compile("Serial:.(\\d+)");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount()==1)
            return Integer.parseInt(matcher.group(1));
        else
            throw new ParseException("Can't find station serial from '" + mumbo + "'", 0);
    }

    /**
     * Determines whether the logger is a barologger or a level logger from the line in the email that goes
     * "Type: Levelogger Edge, M5, 3.0040"; also saves the type, model and version number for this logger
     * @param mumbo
     * @return
     * @throws ParseException
     */
    private String parseLoggerType(String mumbo) throws ParseException {
        Pattern pattern = Pattern.compile("Type:.(.+?),\\s(.+?),\\s(.+)");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount()==3) {
            if(matcher.group(1).equals("Levelogger Edge")) {
                ll_type = matcher.group(1);
                ll_model = matcher.group(2);
                ll_v = matcher.group(3);
                return "Leve";
            } else if(matcher.group(1).equals("Barologger Edge")) {
                pl_type = matcher.group(1);
                pl_model = matcher.group(2);
                pl_v = matcher.group(3);
                return "Baro";
            } else
                throw new ParseException("Unexpected logger type: '" + matcher.group(1) + "'", 0);
        } else
            throw new ParseException("Can't seem to find logger info from '" + mumbo + "'", 0);
    }

    /**
     * Goes down the file line by line until it finds the line which holds the station serial, i.e. the line that
     * contains the text "Serial: "
     * @param scanner
     * @throws ParseException
     */
    private void findStationSerial(Scanner scanner) throws ParseException {
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Serial: ")) {
                station = parseStationSerial(line);
                break;
            }
        }
    }

    /**
     * Goes down the file line by line until it finds the line about battery level, i.e. the line that contains
     * "Battery: "
     * @param scanner
     * @throws ParseException
     */
    private void findBatteryLevel(Scanner scanner) throws ParseException {
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Battery: ")) {
                battery = parseBatteryLevel(line);
                break;
            }
        }
    }

    /**
     * Determines the battery level from the line of text that goes "Battery: 97%"
     * @param line
     * @return
     * @throws ParseException
     */
    private int parseBatteryLevel(String line) throws ParseException {
        Pattern pattern = Pattern.compile("Battery: (\\d+)%");
        Matcher matcher = pattern.matcher(line);
        if(matcher.find() && matcher.groupCount() == 1)
            return Integer.parseInt(matcher.group(1));
        else
            throw new ParseException("Can't find battery level from '" + line + "'", 0);
    }


    /**
     * Goes down the file until it finds the line about sample rate, i.e. the line containing "Sample Rate: "
     * @param scanner
     */
    private void findSampleRate(Scanner scanner) throws ParseException, BadTimeUnit {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Sample Rate: ")) {
                sample_rate = parseSampleRate(line);
                break;
            }
        }
    }

    private int getMinutes(String n, String units) throws BadTimeUnit {
        if(units.equals("minutes"))
            return Integer.parseInt(n);
        if(units.equals("hours"))
            return Integer.parseInt(n)*60;
        else throw new BadTimeUnit(units, this);
    }

    /**
     * Determines the sample rate from the line of text that goes "Sample Rate: 30 minutes". Checks for the time unites.
     * @param mumbo
     * @return
     */
    private int parseSampleRate(String mumbo) throws ParseException, BadTimeUnit {
        Pattern pattern = Pattern.compile("Sample.Rate:.(\\d+)\\s(.+)");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount()==2) {
            return getMinutes(matcher.group(1), matcher.group(2));
        } else
            throw new ParseException("Can't figure out sample rate from '" + mumbo + "'", 0);
    }


    /**
     * Goes down the report and finds the line about report rate, i.e. the line containing "Report Rate: "
     * @param scanner
     */
    private void findReportRate(Scanner scanner) throws ParseException, BadTimeUnit {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Report Rate: ")) {
                report_rate = parseReportRate(line);
                break;
            }
        }
    }

    /**
     * Determines the report rate in minutes from the line that goes "Report Rate: 6 hours"
     * @param mumbo
     * @return
     */
    private int parseReportRate(String mumbo) throws BadTimeUnit, ParseException {
        Pattern pattern = Pattern.compile("Report.Rate:.(\\d+)\\s(.+)");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount()==2) {
            return getMinutes(matcher.group(1), matcher.group(2));
        } else
            throw new ParseException("Can't figure out report rate from '" + mumbo + "'", 0);
    }

    /**
     * Find the line about LevelSender state, and determines the state from this line that goes "State: stopped"
     * @param scanner
     */
    private void findState(Scanner scanner) throws ParseException {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("State: ")) {
                Pattern pattern = Pattern.compile("State:\\s(.+)");
                Matcher matcher = pattern.matcher(line);
                if(matcher.find() && matcher.groupCount()==1)
                    state = matcher.group(1);
                else throw new ParseException("Can't figure out state from '" + line + "'", 0);
                break;
            }
        }
    }

    /**
     * Find the line about LevelSender start, and determines the start from this line that goes
     * "Start Report: 17/12/2018 13:00:00"
     * @param scanner
     */
    private void findStart(Scanner scanner) throws ParseException {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Start Report: ")) {
                Pattern pattern = Pattern.compile("Start Report:\\s(.+)");
                Matcher matcher = pattern.matcher(line);
                if(matcher.find() && matcher.groupCount()==1)
                    start_report = matcher.group(1);
                else throw new ParseException("Can't figure out start report from '" + line + "'", 0);
                break;
            }
        }
    }

    /**
     * Determines the logger serial number from the line of text that goes "Serial: 2856244"
     * @param mumbo
     * @return
     * @throws ParseException
     */
    private int parseLoggerSerial(String mumbo) throws ParseException {
        Pattern pattern = Pattern.compile("Serial: (\\d+)");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount()==1)
            return Integer.parseInt(matcher.group(1));
        else
            throw new ParseException("Can't seem to find logger serial from '" + mumbo + "'", 0);
    }

    private void findInfo(Scanner scanner, String logger) throws ParseException {
        String line = scanner.nextLine();
        Pattern batteryPattern = Pattern.compile("Battery:\\s(\\d+)%");
        Pattern logPattern = Pattern.compile("Total\\sLogs:\\s(\\d+?)\\sof\\s(\\d+)");
        Pattern ratepattern = Pattern.compile("Log\\sRate:\\s(\\d+)\\sseconds");
        Pattern memoryPattern = Pattern.compile("Memory\\sMode:\\s(.+)");
        Pattern typePattern = Pattern.compile("Log\\sType:\\s(.+)");
        Pattern statePattern = Pattern.compile("State:\\s(.+)");
        Pattern startPattern = Pattern.compile("Start\\sLogger:\\s(.+)");
        if(logger.equals("Leve")) {
            // Battery
            Matcher batteryMatcher = batteryPattern.matcher(line);
            if(!line.contains("Battery:") || !batteryMatcher.find() || batteryMatcher.groupCount()!=1)
                throw new ParseException("Can't find battery level for " + logger + " in '" + line + "'", 0);
            ll_battery = Integer.parseInt(batteryMatcher.group(1));

            // Logs
            line = scanner.nextLine();
            Matcher logMatcher = logPattern.matcher(line);
            if(!line.contains("Total Logs:") || ! logMatcher.find() || logMatcher.groupCount()!=2)
                throw new ParseException("Can't find log number info for " + logger + " in '" + line + "'", 0);
            ll_n_logs = Integer.parseInt(logMatcher.group(1));
            ll_max_logs = Integer.parseInt(logMatcher.group(2));

            // Rate
            line = scanner.nextLine();
            Matcher rateMatcher = ratepattern.matcher(line);
            if(!line.contains("Log Rate:") || !rateMatcher.find() || rateMatcher.groupCount()!=1)
                throw new ParseException("Can't find sample rate for " + logger + " in '" + line + "'", 0);
            ll_rate = Integer.parseInt(rateMatcher.group(1));

            // Memory
            line = scanner.nextLine();
            Matcher memMatcher = memoryPattern.matcher(line);
            if(!line.contains("Memory Mode:") || !memMatcher.find() || memMatcher.groupCount()!=1)
                throw new ParseException("Can't find memory mode for " + logger + " in '" + line + "'", 0);
            ll_mem = memMatcher.group(1);

            // Type
            line = scanner.nextLine();
            Matcher typeMatcher = typePattern.matcher(line);
            if(!line.contains("Log Type:") || !typeMatcher.find() || typeMatcher.groupCount()!=1)
                throw new ParseException("Can't find log type for " + logger + " in '" + line + "'", 0);
            ll_log_type = typeMatcher.group(1);

            // State
            line = scanner.nextLine();
            Matcher stateMatcher = statePattern.matcher(line);
            if(!line.contains("State:") || !stateMatcher.find() || stateMatcher.groupCount()!=1)
                throw new ParseException("Can't find state for " + logger + " in '" + line + "'", 0);
            ll_state = stateMatcher.group(1);

            // Start
            line = scanner.nextLine();
            Matcher startMatcher = startPattern.matcher(line);
            if(!line.contains("Start Logger:") || !startMatcher.find() || startMatcher.groupCount()!=1)
                throw new ParseException("Can't find start for " + logger + " in '" + line + "'", 0);
            ll_start_logger = startMatcher.group(1);
        } else {
            // Battery
            Matcher batteryMatcher = batteryPattern.matcher(line);
            if(!line.contains("Battery:") || !batteryMatcher.find() || batteryMatcher.groupCount()!=1)
                throw new ParseException("Can't find battery level for " + logger + " in '" + line + "'", 0);
            pl_battery = Integer.parseInt(batteryMatcher.group(1));

            // Logs
            line = scanner.nextLine();
            Matcher logMatcher = logPattern.matcher(line);
            if(!line.contains("Total Logs:") || ! logMatcher.find() || logMatcher.groupCount()!=2)
                throw new ParseException("Can't find log number info for " + logger + " in '" + line + "'", 0);
            pl_n_logs = Integer.parseInt(logMatcher.group(1));
            pl_max_logs = Integer.parseInt(logMatcher.group(2));

            // Rate
            line = scanner.nextLine();
            Matcher rateMatcher = ratepattern.matcher(line);
            if(!line.contains("Log Rate:") || !rateMatcher.find() || rateMatcher.groupCount()!=1)
                throw new ParseException("Can't find sample rate for " + logger + " in '" + line + "'", 0);
            pl_rate = Integer.parseInt(rateMatcher.group(1));

            // Memory
            line = scanner.nextLine();
            Matcher memMatcher = memoryPattern.matcher(line);
            if(!line.contains("Memory Mode:") || !memMatcher.find() || memMatcher.groupCount()!=1)
                throw new ParseException("Can't find memory mode for " + logger + " in '" + line + "'", 0);
            pl_mem = memMatcher.group(1);

            // Type
            line = scanner.nextLine();
            Matcher typeMatcher = typePattern.matcher(line);
            if(!line.contains("Log Type:") || !typeMatcher.find() || typeMatcher.groupCount()!=1)
                throw new ParseException("Can't find log type for " + logger + " in '" + line + "'", 0);
            pl_log_type = typeMatcher.group(1);

            // State
            line = scanner.nextLine();
            Matcher stateMatcher = statePattern.matcher(line);
            if(!line.contains("State:") || !stateMatcher.find() || stateMatcher.groupCount()!=1)
                throw new ParseException("Can't find state for " + logger + " in '" + line + "'", 0);
            pl_state = stateMatcher.group(1);

            // Start
            line = scanner.nextLine();
            Matcher startMatcher = startPattern.matcher(line);
            if(!line.contains("Start Logger:") || !startMatcher.find() || startMatcher.groupCount()!=1)
                throw new ParseException("Can't find start for " + logger + " in '" + line + "'", 0);
            pl_start_logger = startMatcher.group(1);
        }
    }

    /**
     * Goes down the file line by line until it finds the line which details the type of logger, i.e. the line of text
     * that goes "Type: Levelogger Edge"
     * @param scanner
     * @throws ParseException
     */
    private void findLoggerType(Scanner scanner) throws ParseException {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.contains("Type: ")) {
                String type = parseLoggerType(line);
                line = scanner.nextLine();
                if(type.equals("Leve")) {
                    levelLogger = parseLoggerSerial(line);
                    obs.add(levels);
                    findInfo(scanner, type);
                } else if(type.equals("Baro")) {
                    baroLogger = parseLoggerSerial(line);
                    obs.add(pressures);
                    findInfo(scanner, type);
                } else {
                    throw new ParseException("Unknown logger type '" + type + "'", 0);
                }
                while(scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if(line.equals(""))
                        break;
                }
                break;
            }
        }
    }

    /**
     * Parses one line of text in the data part of the email into an @see Observation
     * @param line
     * @return
     * @throws ParseException
     */
    private Observation parseObservation(String line) throws ParseException {
        Pattern pattern = Pattern.compile("(\\d+/\\d+/\\d+.\\d+:\\d+:\\d+),.(-?\\d+\\.\\d+),.(-?\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(line);
        if(matcher.find() && matcher.groupCount()==3)
            return new Observation(matcher.group(1), Float.parseFloat(matcher.group(2)), Float.parseFloat(matcher.group(3)));
        else
            throw new ParseException("Can't seem to parse observation from '" + line + "'", 0);
    }

    /**
     * Iterates through the lines of text in the email corresponding to one logger, specified by @param order. Also
     * makes sure there is a header with appropriate units to the data section.
     * @param order
     * @param scanner
     * @throws ParseException
     */
    private void parseLoggerSamples(int order, Scanner scanner) throws ParseException {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            while(scanner.hasNextLine() && line.equals("")) {
                line = scanner.nextLine();
            }
            if(line.matches("Logger [12] Samples"))
                line = scanner.nextLine();
            if(!line.contains("Time, Temperature( C), "))
                throw new ParseException("No header line for sample section?", 0);
            List<Observation> obs = this.obs.get(order);
            while(!line.equals("")) {
                line = scanner.nextLine();
                if(!line.equals(""))
                    obs.add(parseObservation(line));
            }
            break;
        }
    }

    /**
     * Determines the report number from the subject of the email.
     * @param subject
     * @return
     * @throws ParseException
     */
    private int parseReportNumber(String subject) throws ParseException {
        Pattern pattern = Pattern.compile("\\d+ LS Report (\\d+)");
        Matcher matcher = pattern.matcher(subject);
        if(matcher.find() && matcher.groupCount() == 1)
            return Integer.parseInt(matcher.group(1));
        else
            throw new ParseException("Can't find report number in " + subject, 0);
    }

    /**
     * Determines the sent date of the email from the header "X-Apparently-To" in the raw email, because POP3 does
     * not give any information about sent date...
     * @param mumbo
     * @return
     * @throws ParseException
     */
    public static String parseSentDate(String mumbo) throws ParseException {
        //home_test@yahoo.com; Sat, 16 Mar 2019 23:32:41 +0000
        //^.+      @.+       ;(                         )+\\d+
        Pattern pattern = Pattern.compile("^.+@.+;\\s(.+)\\s\\+\\d+$");
        Matcher matcher = pattern.matcher(mumbo);
        if(matcher.find() && matcher.groupCount() == 1)
            return matcher.group(1);
        else
            throw new ParseException("Can't find email sent date from '" + mumbo + "'", 0);
    }

    // EXTRACT TEXT BODY FROM JAVAX MESSAGE

    /**
     * Extracts the body of the email as text, compensating for ContentType header shenanigans (i.e. typo in charset
     * that makes isMimeType("text/plain") == false)
     * @param message
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws ParseException
     */
    private static String getTextFromMessage(Message message) throws IOException, MessagingException, ParseException {
        String result = "";
//        if(message.isMimeType("text/plain"))
//            return message.getContent().toString(); // text/plain; charset... isMimeType(text/plain). sigh.
        // message.getContent().toString(); // would work if isMimeType("text/plain") returned true...
        if(message.getContentType().equals("text/plain; charset=\"us-ascii\".")) {
            try {
                ByteArrayInputStream in = (ByteArrayInputStream) message.getContent();
                int n = in.available();
                byte[] bytes = new byte[n];
                in.read(bytes, 0, n);
                result = new String(bytes, StandardCharsets.US_ASCII);
            } catch (ClassCastException e) {
                throw new ParseException("Weird ContentType behavior...", 0);
            }
        } else
            throw new ParseException("Solinst fixed their ContentType email header... change your Report::getTextFromMessage accordingly: '" +
                    message.getContentType() + "'", 0);
        return result;
    }

//    private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws IOException, MessagingException {
//
//        int count = mimeMultipart.getCount();
//        if (count == 0)
//            throw new MessagingException("Multipart with no body parts not supported.");
//        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
//        if (multipartAlt)
//            // alternatives appear in an order of increasing
//            // faithfulness to the original content. Customize as req'd.
//            // try zero for plain text?
//            return getTextFromBodyPart(mimeMultipart.getBodyPart(0));
//        String result = "";
//        for (int i = 0; i < count; i++) {
//            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
//            result += "\n" + getTextFromBodyPart(bodyPart);
//        }
//        return result;
//    }
//
//    private static String getTextFromBodyPart(BodyPart bodyPart) throws IOException, MessagingException {
//        String result = "";
//        if (bodyPart.isMimeType("text/plain")) {
//            result += "\r\n" + bodyPart.getContent();
//        } else if (bodyPart.isMimeType("text/html")) {
//            String html = (String) bodyPart.getContent();
//            result += "\n" + org.jsoup.Jsoup.parse(html).text();
//        } else if (bodyPart.getContent() instanceof MimeMultipart){
//            result += "\n" + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
//        }
//        return result;
//    }

    // CONSTRUCTORS

    /**
     * Parsing workhorse. Finds the station serial, its battery level, both sensor serials, and then the series of
     * observations. Also validates that both sensors are configured properly and report their serial number.
     * @param content
     * @throws ParseException
     * @throws BadSensorConfiguration
     */
    private void parseReport(String content) throws ParseException, BadSensorConfiguration, BadTimeUnit {
        Scanner scanner = new Scanner(content);

        // LevelSender info
        findStationSerial(scanner);
        findBatteryLevel(scanner);
        findSampleRate(scanner);
        findReportRate(scanner);
        findState(scanner);
        findStart(scanner);

        // Levelogger and Barologger info
        findLoggerType(scanner);
        findLoggerType(scanner);

        // Observations
        parseLoggerSamples(0, scanner);
        parseLoggerSamples(1, scanner);

        if(baroLogger == 0 || levelLogger == 0)
            throw new BadSensorConfiguration("", this);
    }

    /**
     * Constructor for use with @see Repeater. Only used to copy data from database rather than email.
     * @param subject
     * @param body
     * @param date
     * @throws ParseException
     * @throws BadSensorConfiguration
     */
    public Report(String subject, String body, String date) throws ParseException, BadSensorConfiguration, BadTimeUnit {
        no = parseReportNumber(subject);
        this.body = body;
        this.sentDate = date;
        parseReport(body);
    }
/*
    /**
     * Constructor for use with @see Reparser. Only used to copy data from database rather than email.
     * @param id
     * @param body
     * @param date
     */
    /*public Report(int id, String body, String date) throws ParseException, BadSensorConfiguration, BadTimeUnit {
        this.id = id;
        this.body = body;
        this.sentDate = date;
        parseReport(body);
    }
*/
    /**
     * Standard constructor. Extracts all necessary information from a @see javax.mail.Message
     * @param email
     * @throws IOException
     * @throws MessagingException
     * @throws ParseException
     * @throws BadSensorConfiguration
     */
    public Report(Message email) throws IOException, MessagingException, ParseException, BadSensorConfiguration, BadTimeUnit {
        no = parseReportNumber(email.getSubject());
        body = getTextFromMessage(email);
        Date ladate;
        if(email.getSentDate() == null) {   // yaaaay, pop3 doesn't give you date. needle-haystack it from headers
            DateFormat edf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
            String dateString = parseSentDate(email.getHeader("X-Apparently-To")[0]);
            ladate = edf.parse(dateString);
        } else {
            ladate = email.getSentDate();
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sentDate = df.format(ladate);
        parseReport(body);
    }

    // COPY TO DATABASE

    /**
     * Copies the report and its data using the given database connection. Saves the email itself into an "emails"
     * table, and then the levels and pressures into "corrected". Database triggers take care of the barometric
     * correction of water levels.
     * @param conn
     * @throws UnknownStationException
     * @throws SQLException
     */
    public void insertIntoDb(Connection conn) throws UnknownStationException, SQLException {
        // Email backup
        try (PreparedStatement m_stmt = conn.prepareStatement("INSERT INTO emails (station, sent, num, battery, " +
                "level_logger, pressure_logger, body, " +
                "sample_rate, report_rate, state, start_report, ll_type, ll_model, ll_v, ll_battery, ll_n_logs, ll_max_logs, ll_rate, " +
                "ll_mem, ll_log_type, ll_state, ll_start_logger, pl_type, pl_model, pl_v, pl_battery, pl_n_logs, pl_max_logs, pl_rate, " +
                "pl_mem, pl_log_type, pl_state, pl_start_logger) " +
                // 1-10
                "VALUES ((SELECT station FROM water_stations_levelsender WHERE levelsender=?), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), " +
                "?, ?, ?, ?, ?, ?, ?, ?," +
                // 11-20
                "to_timestamp(?, 'DD-MM-YYYY HH24:MI:SS'), ?, ?, ?, ?, ?, ?, ?, ?, ?," +
                // 21-30
                "?, to_timestamp(?, 'DD-MM-YYYY HH24:MI:SS'), ?, ?, ?, ?, ?, ?, ?, ?," +
                // 31-33
                "?, ?, to_timestamp(?, 'DD-MM-YYYY HH24:MI:SS')) " +
                "ON CONFLICT ON CONSTRAINT emails_station_sent_key DO NOTHING")){
            m_stmt.setInt(1, station);
            m_stmt.setString(2, sentDate);
            m_stmt.setInt(3, no);
            m_stmt.setInt(4, battery);
            m_stmt.setInt(5, levelLogger);
            m_stmt.setInt(6, baroLogger);
            m_stmt.setString(7, body);
            // ----------------- new info
// sample_rate
            m_stmt.setInt(8, sample_rate);
// report_rate
            m_stmt.setInt(9, report_rate);
// state
            m_stmt.setString(10, state);
// start_report
            m_stmt.setString(11, start_report);
// ll_type
            m_stmt.setString(12, ll_type);
// ll_model
            m_stmt.setString(13, ll_model);
// ll_v
            m_stmt.setString(14, ll_v);
// ll_battery
            m_stmt.setInt(15, ll_battery);
// ll_n_logs
            m_stmt.setInt(16, ll_n_logs);
// ll_max_logs
            m_stmt.setInt(17, ll_max_logs);
// ll_rate
            m_stmt.setInt(18, ll_rate);
// ll_mem
            m_stmt.setString(19, ll_mem);
// ll_log_type
            m_stmt.setString(20, ll_log_type);
// ll_state
            m_stmt.setString(21, ll_state);
// ll_start_logger
            m_stmt.setString(22, ll_start_logger);
// pl_type
            m_stmt.setString(23, pl_type);
// pl_model
            m_stmt.setString(24, pl_model);
// pl_v
            m_stmt.setString(25, pl_v);
// pl_battery
            m_stmt.setInt(26, pl_battery);
// pl_n_logs
            m_stmt.setInt(27, pl_n_logs);
// pl_max_logs
            m_stmt.setInt(28, pl_max_logs);
// pl_rate
            m_stmt.setInt(29, pl_rate);
// pl_mem
            m_stmt.setString(30, pl_mem);
// pl_log_type
            m_stmt.setString(31, pl_log_type);
// pl_state
            m_stmt.setString(32, pl_state);
// pl_start_logger
            m_stmt.setString(33, pl_start_logger);
            // ----------------- end new info
            m_stmt.execute();
            String sql = "SELECT id FROM emails WHERE station=(SELECT station FROM water_stations_levelsender WHERE levelsender=" +
                    station + ") AND lower(to_char(sent, 'YYYY-MM-DD HH24:MI:SS'))=lower('" + sentDate + "')";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            Integer id = null;
            if(rs.next()) {
                id = rs.getInt(1);

                // Observations within the email
                PreparedStatement lvl_stmt = conn.prepareStatement("INSERT INTO corrected (station, moment, level, l_temp, email) " +
                        "VALUES ((SELECT station FROM water_stations_levelsender WHERE levelsender=?), to_timestamp(?, 'DD/MM/YYYY HH24:MI:SS'), ?, ?, ?)" +
                        "ON CONFLICT (station, moment) DO " +
                        "UPDATE SET level = EXCLUDED.level, l_temp = EXCLUDED.l_temp");
                PreparedStatement p_stmt = conn.prepareStatement("INSERT INTO corrected (station, moment, pressure, p_temp, email) " +
                        "VALUES ((SELECT station FROM water_stations_levelsender WHERE levelsender=?), to_timestamp(?, 'DD/MM/YYYY HH24:MI:SS'), ?, ?, ?)" +
                        "ON CONFLICT (station, moment) DO " +
                        "UPDATE SET pressure = EXCLUDED.pressure, p_temp = EXCLUDED.p_temp");
                lvl_stmt.setInt(1, station);
                lvl_stmt.setInt(5, id);
                p_stmt.setInt(1, station);
                p_stmt.setInt(5, id);
                for (Observation obs : levels) {
                    lvl_stmt.setString(2, obs.getDateTime());
                    lvl_stmt.setFloat(3, obs.getLevelOrBaro());
                    lvl_stmt.setFloat(4, obs.getTemp());
                    lvl_stmt.execute();
                }
                for (Observation obs : pressures) {
                    p_stmt.setString(2, obs.getDateTime());
                    p_stmt.setFloat(3, obs.getLevelOrBaro());
                    p_stmt.setFloat(4, obs.getTemp());
                    p_stmt.execute();
                }
            }
        } catch (SQLException e) {
            if(e.getMessage().contains("emails_station_fkey"))
                throw new UnknownStationException("", this);
            else
                throw e;
        }
    }

    /*public void updateIntoDb(Connection conn) throws SQLException {
        // Email backup
        try (PreparedStatement m_stmt = conn.prepareStatement("UPDATE emails SET sample_rate=?, report_rate=?, " +
                "state=?, start_report=to_timestamp(?, 'DD/MM/YYYY HH24:MI:SS'), ll_type=?, ll_model=?, ll_v=?, " +
                "ll_battery=?, ll_n_logs=?, ll_max_logs=?, ll_rate=?, ll_mem=?, ll_log_type=?, ll_state=?, " +
                "ll_start_logger=to_timestamp(?, 'DD/MM/YYYY HH24:MI:SS'), pl_type=?, pl_model=?, pl_v=?, pl_battery=?, " +
                "pl_n_logs=?, pl_max_logs=?, pl_rate=?, pl_mem=?, pl_log_type=?, pl_state=?, " +
                "pl_start_logger=to_timestamp(?, 'DD/MM/YYYY HH24:MI:SS') WHERE id=?")) {
// sample_rate
            m_stmt.setInt(1, sample_rate);
// report_rate
            m_stmt.setInt(2, report_rate);
// state
            m_stmt.setString(3, state);
// start_report
//            System.out.println("Start report: '" + start_report + "'");
//            System.out.println("ll_start: '" + ll_start_logger + "'");
//            System.out.println("pl_start: '" + pl_start_logger + "'");
            m_stmt.setString(4, start_report);
// ll_type
            m_stmt.setString(5, ll_type);
// ll_model
            m_stmt.setString(6, ll_model);
// ll_v
            m_stmt.setString(7, ll_v);
// ll_battery
            m_stmt.setInt(8, ll_battery);
// ll_n_logs
            m_stmt.setInt(9, ll_n_logs);
// ll_max_logs
            m_stmt.setInt(10, ll_max_logs);
// ll_rate
            m_stmt.setInt(11, ll_rate);
// ll_mem
            m_stmt.setString(12, ll_mem);
// ll_log_type
            m_stmt.setString(13, ll_log_type);
// ll_state
            m_stmt.setString(14, ll_state);
// ll_start_logger
            m_stmt.setString(15, ll_start_logger);
// pl_type
            m_stmt.setString(16, pl_type);
// pl_model
            m_stmt.setString(17, pl_model);
// pl_v
            m_stmt.setString(18, pl_v);
// pl_battery
            m_stmt.setInt(19, pl_battery);
// pl_n_logs
            m_stmt.setInt(20, pl_n_logs);
// pl_max_logs
            m_stmt.setInt(21, pl_max_logs);
// pl_rate
            m_stmt.setInt(22, pl_rate);
// pl_mem
            m_stmt.setString(23, pl_mem);
// pl_log_type
            m_stmt.setString(24, pl_log_type);
// pl_state
            m_stmt.setString(25, pl_state);
// pl_start_logger
            m_stmt.setString(26, pl_start_logger);
// id
//            m_stmt.setInt(27, id);
            m_stmt.execute();
        } catch (Exception e) {
            throw e;
        }
    }*/

    // UTILITIES
    @Override
    public String toString() {
        return "Report for #" + station + " (level@" + levelLogger + " [" + levels.size() + " obs], pressure@" + baroLogger +
                " [" + pressures.size() + " obs])";
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Report))
            return false;
        Report other = (Report) o;
        return station == other.station
                && levelLogger == other.levelLogger
                && baroLogger == other.baroLogger
                && obs.equals(other.obs);
    }

    @Override
    public int hashCode() {
        return obs.hashCode() + station * levelLogger - baroLogger;
    }
}
