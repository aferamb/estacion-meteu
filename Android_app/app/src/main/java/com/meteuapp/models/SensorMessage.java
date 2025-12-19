package com.meteuapp.models;

/**
 * Represents a short live message returned by /admin/live (recent readings list).
 * Fields mirror what the server sends: sensor_id, recorded_at, temp, humid, aqi, lux
 */
public class SensorMessage {
    private String sensor_id;
    private String recorded_at;
    private Double temp;
    private Double humid;
    private Integer aqi;
    private Double lux;

    public String getSensorId() { return sensor_id; }
    public String getRecordedAt() { return recorded_at; }
    public Double getTemp() { return temp; }
    public Double getHumid() { return humid; }
    public Integer getAqi() { return aqi; }
    public Double getLux() { return lux; }

    // Safe accessors
    public String getSensorIdSafe() { return sensor_id == null ? "" : sensor_id; }
    public String getRecordedAtSafe() { return recorded_at == null ? "" : recorded_at; }
    public double getTempSafe() { return temp == null ? Double.NaN : temp; }
    public double getHumidSafe() { return humid == null ? Double.NaN : humid; }
    public int getAqiSafe() { return aqi == null ? -1 : aqi; }
    public double getLuxSafe() { return lux == null ? Double.NaN : lux; }
}
