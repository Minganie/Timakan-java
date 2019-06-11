package com.hydrometeocharlevoix.timakan;

/**
 * Used to signify that a station that sent an email report has not been added in the database using the QGIS project
 * "StationEditor".
 */
public class UnknownStationException extends Exception {
    private Report report;
    public UnknownStationException(String s, Report report) {
        super(s);
        this.report = report;
    }

    public String getMessage() {
        return "Station #" + report.getStation() + " inconnue; avez-vous oublié de l'ajouter avec l'éditeur de station?";
    }

    public Report getReport() {
        return report;
    }
}
