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
    private String sensor_type;
    private String street_id;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private String district;
    private String neighborhood;
    private Double sound_db;
    private Double atmhpa;
    private Double uv_index;

    public String getSensorId() { return sensor_id; }
    public String getRecordedAt() { return recorded_at; }
    public Double getTemp() { return temp; }
    public Double getHumid() { return humid; }
    public Integer getAqi() { return aqi; }
    public Double getLux() { return lux; }

    public String getSensorType() { return sensor_type; }
    public String getStreetId() { return street_id; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getAltitude() { return altitude; }
    public String getDistrict() { return district; }
    public String getNeighborhood() { return neighborhood; }
    public Double getSoundDb() { return sound_db; }
    public Double getAtmHpa() { return atmhpa; }
    public Double getUvIndex() { return uv_index; }

    // Safe accessors
    public String getSensorIdSafe() { return sensor_id == null ? "" : sensor_id; }
    public String getRecordedAtSafe() { return recorded_at == null ? "" : recorded_at; }
    public double getTempSafe() { return temp == null ? Double.NaN : temp; }
    public double getHumidSafe() { return humid == null ? Double.NaN : humid; }
    public int getAqiSafe() { return aqi == null ? -1 : aqi; }
    public double getLuxSafe() { return lux == null ? Double.NaN : lux; }
    public String getSensorTypeSafe() { return sensor_type == null ? "" : sensor_type; }
    public String getStreetIdSafe() { return street_id == null ? "" : street_id; }
    public double getLatitudeSafe() { return latitude == null ? Double.NaN : latitude; }
    public double getLongitudeSafe() { return longitude == null ? Double.NaN : longitude; }
    public double getAltitudeSafe() { return altitude == null ? Double.NaN : altitude; }
    public String getDistrictSafe() { return district == null ? "" : district; }
    public String getNeighborhoodSafe() { return neighborhood == null ? "" : neighborhood; }
    public double getSoundDbSafe() { return sound_db == null ? Double.NaN : sound_db; }
    public double getAtmHpaSafe() { return atmhpa == null ? Double.NaN : atmhpa; }
    public double getUvIndexSafe() { return uv_index == null ? Double.NaN : uv_index; }
}
