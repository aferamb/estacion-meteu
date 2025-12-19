package com.meteuapp.models;

/**
 * Represents a station seen recently (from /admin/live)
 */
public class Station {
    private String sensor_id;
    private String last_seen;

    public String getSensorId() { return sensor_id; }
    public String getLastSeen() { return last_seen; }
}
