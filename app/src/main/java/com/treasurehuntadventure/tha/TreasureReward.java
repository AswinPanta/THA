package com.treasurehuntadventure.tha;

public class TreasureReward {
    private int coins;
    private int experience;
    private int bounty;
    private String specialItem;
    private TreasureRarity rarity;
    
    public TreasureReward(TreasureRarity rarity) {
        this.rarity = rarity;
        calculateRewards();
    }
    
    private void calculateRewards() {
        switch (rarity) {
            case COMMON:
                coins = 50 + (int)(Math.random() * 51); // 50-100 coins
                experience = 10 + (int)(Math.random() * 11); // 10-20 XP
                bounty = 100 + (int)(Math.random() * 101); // 100-200 bounty
                specialItem = null;
                break;
            case RARE:
                coins = 150 + (int)(Math.random() * 101); // 150-250 coins
                experience = 25 + (int)(Math.random() * 16); // 25-40 XP
                bounty = 300 + (int)(Math.random() * 201); // 300-500 bounty
                specialItem = getRandomRareItem();
                break;
            case LEGENDARY:
                coins = 500 + (int)(Math.random() * 251); // 500-750 coins
                experience = 75 + (int)(Math.random() * 26); // 75-100 XP
                bounty = 1000 + (int)(Math.random() * 501); // 1000-1500 bounty
                specialItem = getRandomLegendaryItem();
                break;
        }
    }
    
    private String getRandomRareItem() {
        String[] rareItems = {
            "Golden Compass", "Pirate Spyglass", "Ancient Map Fragment",
            "Silver Doubloon", "Captain's Hat", "Mystic Pearl"
        };
        return rareItems[(int)(Math.random() * rareItems.length)];
    }
    
    private String getRandomLegendaryItem() {
        String[] legendaryItems = {
            "One Piece Fragment", "Devil Fruit", "Legendary Sword",
            "Poseidon's Trident", "Ancient Treasure Map", "Pirate King's Crown"
        };
        return legendaryItems[(int)(Math.random() * legendaryItems.length)];
    }
    
    // Getters
    public int getCoins() { return coins; }
    public int getExperience() { return experience; }
    public int getBounty() { return bounty; }
    public String getSpecialItem() { return specialItem; }
    public TreasureRarity getRarity() { return rarity; }
    
    public String getRewardSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("💰 ").append(coins).append(" Coins\n");
        summary.append("⭐ ").append(experience).append(" Experience\n");
        summary.append("🏴‍☠️ ").append(bounty).append(" Bounty");
        
        if (specialItem != null) {
            summary.append("\n🎁 ").append(specialItem);
        }
        
        return summary.toString();
    }
}
