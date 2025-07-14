package com.treasurehuntadventure.tha;

public class CapturedTreasure {
    private String imageUrl;
    private TreasureRarity rarity;
    private String characterName;
    private String characterDescription;
    private String characterImageUrl;
    private String devilFruit;
    private String crew;
    private String job;
    private String bounty;

    public CapturedTreasure(String imageUrl, TreasureRarity rarity) {
        this.imageUrl = imageUrl;
        this.rarity = rarity;
    }

    public CapturedTreasure(String imageUrl, TreasureRarity rarity, String characterName, 
                          String characterDescription, String characterImageUrl, 
                          String devilFruit, String crew, String job, String bounty) {
        this.imageUrl = imageUrl;
        this.rarity = rarity;
        this.characterName = characterName;
        this.characterDescription = characterDescription;
        this.characterImageUrl = characterImageUrl;
        this.devilFruit = devilFruit;
        this.crew = crew;
        this.job = job;
        this.bounty = bounty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public TreasureRarity getRarity() {
        return rarity;
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getCharacterDescription() {
        return characterDescription;
    }

    public String getCharacterImageUrl() {
        return characterImageUrl;
    }

    public String getDevilFruit() {
        return devilFruit;
    }

    public String getCrew() {
        return crew;
    }

    public String getJob() {
        return job;
    }

    public String getBounty() {
        return bounty;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public void setCharacterDescription(String characterDescription) {
        this.characterDescription = characterDescription;
    }

    public void setCharacterImageUrl(String characterImageUrl) {
        this.characterImageUrl = characterImageUrl;
    }

    public void setDevilFruit(String devilFruit) {
        this.devilFruit = devilFruit;
    }

    public void setCrew(String crew) {
        this.crew = crew;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void setBounty(String bounty) {
        this.bounty = bounty;
    }
}
