package com.meteuapp.models;

/**
 * Alarm model returned by /admin/alarms
 */
public class Alarm {
    private Long id;
    private String sensor_id;
    private String street_id;
    private String parameter;
    private Double triggered_value;
    private String triggered_at;
    private String resolved_at;
    private Double resolved_value;
    private Boolean active;

    public Long getId() { return id; }
    public String getSensorId() { return sensor_id; }
    public String getStreetId() { return street_id; }
    public String getParameter() { return parameter; }
    public Double getTriggeredValue() { return triggered_value; }
    public String getTriggeredAt() { return triggered_at; }
    public String getResolvedAt() { return resolved_at; }
    public Double getResolvedValue() { return resolved_value; }
    public Boolean getActive() { return active; }
}
