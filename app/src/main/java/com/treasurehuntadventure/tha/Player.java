package com.treasurehuntadventure.tha;

public class Player {
    public String id;
    public double latitude;
    public double longitude;
    public long lastUpdated;
    public long treasuresCaptured;

    public Player() {}
    public Player(String id, double latitude, double longitude, long lastUpdated) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastUpdated = lastUpdated;
    }
} 