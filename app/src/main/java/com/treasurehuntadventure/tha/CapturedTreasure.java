package com.treasurehuntadventure.tha;

public class CapturedTreasure {
    private String imageUrl;
    private TreasureRarity rarity;

    public CapturedTreasure(String imageUrl, TreasureRarity rarity) {
        this.imageUrl = imageUrl;
        this.rarity = rarity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public TreasureRarity getRarity() {
        return rarity;
    }
}