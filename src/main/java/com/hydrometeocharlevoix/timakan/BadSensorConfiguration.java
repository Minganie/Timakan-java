package com.hydrometeocharlevoix.timakan;

/**
 * Used to signify a problem with a levelogger or barologger sensor in a LevelSender: levelogger or barologger
 * serial is marked as NA in the data email, or other issue.
 */
public class BadSensorConfiguration extends Exception {
    private Report report;
    public BadSensorConfiguration(String s, Report report) {
        super(s);
        this.report = report;
    }

    public Report getReport() {
        return report;
    }

    public String getMessage() {
        return "Station #" + report.getStation() + " a un problème de configuration de ses senseurs: baro #" + report.getBaroLogger() +
                " ou niveau #" + report.getLevelLogger();
    }
}
