package com.hydrometeocharlevoix.timakan;

import java.util.Locale;

public class DataBag {
    private Double battery;
    private Double pTemp;
    private Double waterLevel;
    private Double wTemp;

    public DataBag(int batteryPercent, Double pTemp, Double waterLevel, Double wTemp) {
        this.battery = 9.16023d + batteryPercent * 0.0284091d;
        this.pTemp = pTemp;
        this.waterLevel = waterLevel;
        this.wTemp = wTemp;
    }

    public Double getBattery() {
        return battery;
    }

    public Double getpTemp() {
        return pTemp;
    }

    public Double getWaterLevel() {
        return waterLevel;
    }

    public Double getwTemp() {
        return wTemp;
    }

    public String toJson(String moment) {
        return String.format(Locale.US, "{\"time\":\"%s\",\"vals\":[%f,%f,%f,%f]}", moment, battery, pTemp, waterLevel, wTemp);
    }

    @Override
    public String toString() {
        return String.format("b: %f V, p: %f C, w: %f C, lvl: %f", battery, pTemp, wTemp, waterLevel);
    }
}
