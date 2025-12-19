package com.meteuapp.models;

import java.util.List;

/**
 * Represents the response returned by /admin/live endpoint.
 */
public class LiveResponse {
    private List<SensorMessage> messages;
    private List<Station> stations;

    public List<SensorMessage> getMessages() { return messages; }
    public List<Station> getStations() { return stations; }
}
