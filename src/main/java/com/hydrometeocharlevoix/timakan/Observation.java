package com.hydrometeocharlevoix.timakan;

/**
 * Used to hold one line of data in the email, i.e. one observation, one data point. That is a moment + temperature +
 * level/pressure, depending on sensor.
 */
public class Observation {
    private String dateTime;
    private float temp;
    private float levelOrBaro;

    public Observation(String dateTime, float temp, float levelOrBaro) {
        this.dateTime = dateTime;
        this.temp = temp;
        this.levelOrBaro = levelOrBaro;
    }

    public String getDateTime() {
        return dateTime;
    }

    public float getTemp() {
        return temp;
    }

    public float getLevelOrBaro() {
        return levelOrBaro;
    }

    @Override
    public String toString() {
        return dateTime + " " + temp + "\u00B0C " + levelOrBaro;
    }
}
