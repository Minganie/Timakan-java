package com.hydrometeocharlevoix.timakan;

public class BadTimeUnit extends Exception {
    private Report report;
    public BadTimeUnit(String s, Report report) {
        super(s);
        this.report = report;
    }

    public Report getReport() {
        return report;
    }

    public String getMessage() {
        return "Station #" + report.getStation() + " a un problème d'unités de temps ('" + super.getMessage() + "') dans son rapport #" + report.getNo();
    }
}
