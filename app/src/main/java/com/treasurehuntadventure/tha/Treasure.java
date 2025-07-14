package com.treasurehuntadventure.tha;

import org.osmdroid.util.GeoPoint;

public class Treasure {
    private String id;
    private String title;
    private String imageUrl;
    private GeoPoint location;
    private TreasureRarity rarity;
    
    // One Piece character data
    private String characterName;
    private String characterDescription;
    private String characterImageUrl;
    private String devilFruit;
    private String crew;
    private String job;
    private int bounty;

    public Treasure(String title, String imageUrl, GeoPoint location, TreasureRarity rarity) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.location = location;
        this.rarity = rarity;
    }
    
    // Enhanced constructor with One Piece character data
    public Treasure(String title, String imageUrl, GeoPoint location, TreasureRarity rarity, 
                   String characterName, String characterDescription, String characterImageUrl,
                   String devilFruit, String crew, String job, int bounty) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.location = location;
        this.rarity = rarity;
        this.characterName = characterName;
        this.characterDescription = characterDescription;
        this.characterImageUrl = characterImageUrl;
        this.devilFruit = devilFruit;
        this.crew = crew;
        this.job = job;
        this.bounty = bounty;
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
    
    // Getters and setters for One Piece character data
    public String getCharacterName() {
        return characterName;
    }
    
    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }
    
    public String getCharacterDescription() {
        return characterDescription;
    }
    
    public void setCharacterDescription(String characterDescription) {
        this.characterDescription = characterDescription;
    }
    
    public String getCharacterImageUrl() {
        return characterImageUrl;
    }
    
    public void setCharacterImageUrl(String characterImageUrl) {
        this.characterImageUrl = characterImageUrl;
    }
    
    public String getDevilFruit() {
        return devilFruit;
    }
    
    public void setDevilFruit(String devilFruit) {
        this.devilFruit = devilFruit;
    }
    
    public String getCrew() {
        return crew;
    }
    
    public void setCrew(String crew) {
        this.crew = crew;
    }
    
    public String getJob() {
        return job;
    }
    
    public void setJob(String job) {
        this.job = job;
    }
    
    public int getBounty() {
        return bounty;
    }
    
    public void setBounty(int bounty) {
        this.bounty = bounty;
    }
}
