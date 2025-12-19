package com.meteuapp.models;

/**
 * Flexible model for rows returned by /sensor/readings and the query endpoint.
 * The server may return many different columns; we include the common ones
 * and add "safe" getters so the app won't NPE if fields arrive null.
 */
public class SensorReading {
    private Long id;
    private String sensor_id;
    private String sensor_type;
    private String street_id;
    private String recorded_at;

    // Location
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private String district;
    private String neighborhood;

    // Basic measurements
    private Double temp;
    private Double humid;
    private Integer aqi;
    private Double lux;
    private Double sound_db;
    private Double atmhpa;
    private Double uv_index;

    // Extra / BSEC-like fields
    private Integer bsec_status;
    private Double iaq;
    private Double static_iaq;
    private Double co2_eq;
    private Double breath_voc_eq;
    private Double raw_temperature;
    private Double raw_humidity;
    private Double pressure_hpa;
    private Double gas_resistance_ohm;
    private Double gas_percentage;
    private Double stabilization_status;
    private Double run_in_status;
    private Double sensor_heat_comp_temp;
    private Double sensor_heat_comp_hum;

    // Generic accessors (boxed types preserved for Gson compatibility)
    public Long getId() { return id; }
    public String getSensorId() { return sensor_id; }
    public String getSensorType() { return sensor_type; }
    public String getStreetId() { return street_id; }
    public String getRecordedAt() { return recorded_at; }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getAltitude() { return altitude; }
    public String getDistrict() { return district; }
    public String getNeighborhood() { return neighborhood; }

    public Double getTemp() { return temp; }
    public Double getHumid() { return humid; }
    public Integer getAqi() { return aqi; }
    public Double getLux() { return lux; }
    public Double getSoundDb() { return sound_db; }
    public Double getAtmHpa() { return atmhpa; }
    public Double getUvIndex() { return uv_index; }

    public Integer getBsecStatus() { return bsec_status; }
    public Double getIaq() { return iaq; }
    public Double getStaticIaq() { return static_iaq; }
    public Double getCo2Eq() { return co2_eq; }
    public Double getBreathVocEq() { return breath_voc_eq; }
    public Double getRawTemperature() { return raw_temperature; }
    public Double getRawHumidity() { return raw_humidity; }
    public Double getPressureHpa() { return pressure_hpa; }
    public Double getGasResistanceOhm() { return gas_resistance_ohm; }
    public Double getGasPercentage() { return gas_percentage; }
    public Double getStabilizationStatus() { return stabilization_status; }
    public Double getRunInStatus() { return run_in_status; }
    public Double getSensorHeatCompTemp() { return sensor_heat_comp_temp; }
    public Double getSensorHeatCompHum() { return sensor_heat_comp_hum; }

    // Safe getters that never return null. Use these in UI code to avoid NPEs.
    public double getTempSafe() { return temp == null ? Double.NaN : temp; }
    public double getHumidSafe() { return humid == null ? Double.NaN : humid; }
    public int getAqiSafe() { return aqi == null ? -1 : aqi; }
    public double getLuxSafe() { return lux == null ? Double.NaN : lux; }
    public double getLatitudeSafe() { return latitude == null ? Double.NaN : latitude; }
    public double getLongitudeSafe() { return longitude == null ? Double.NaN : longitude; }

    // Convenience string getters that return empty string when null
    public String getSensorIdSafe() { return sensor_id == null ? "" : sensor_id; }
    public String getStreetIdSafe() { return street_id == null ? "" : street_id; }
    public String getRecordedAtSafe() { return recorded_at == null ? "" : recorded_at; }
    public String getDistrictSafe() { return district == null ? "" : district; }
    public String getNeighborhoodSafe() { return neighborhood == null ? "" : neighborhood; }
}
