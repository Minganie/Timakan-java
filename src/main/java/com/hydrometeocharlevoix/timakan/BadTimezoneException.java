package com.hydrometeocharlevoix.timakan;

/**
 * Used to signify the timezone between the Java program and the Postgres server differ, which may make TIMESTAMP
 * WITH TIMEZONE string conversion inconsistent.
 */
public class BadTimezoneException extends Exception {
    public BadTimezoneException(String s) {
        super(s);
    }
}
