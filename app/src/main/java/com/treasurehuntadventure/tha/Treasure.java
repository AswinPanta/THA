package com.treasurehuntadventure.tha;

import org.osmdroid.util.GeoPoint;

public class Treasure {
    private String id;
    private String title;
    private String imageUrl;
    private GeoPoint location;
    private TreasureRarity rarity;

    public Treasure(String title, String imageUrl, GeoPoint location, TreasureRarity rarity) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.location = location;
        this.rarity = rarity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public TreasureRarity getRarity() {
        return rarity;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}