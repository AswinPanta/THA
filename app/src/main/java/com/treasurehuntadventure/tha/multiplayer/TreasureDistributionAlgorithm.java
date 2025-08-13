package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Intelligent treasure distribution algorithm to ensure fair and balanced spawning
 */
public class TreasureDistributionAlgorithm {
    private static final String TAG = "TreasureDistribution";
    
    // Grid system for distribution analysis
    private static final double GRID_SIZE_KM = 0.5; // 500m grid cells
    private static final int MAX_TREASURES_PER_GRID = 3;
    private static final int MIN_DISTANCE_BETWEEN_SAME_RARITY_M = 200;
    private static final int MIN_DISTANCE_BETWEEN_TREASURES_M = 50;
    
    // Distribution weights for different area types
    private static final Map<AreaType, DistributionWeight> AREA_WEIGHTS = new HashMap<>();
    
    // Terrain and location factors
    private static final double URBAN_DENSITY_MULTIPLIER = 1.2;
    private static final double PARK_DENSITY_MULTIPLIER = 1.5;
    private static final double WATER_PROXIMITY_BONUS = 1.3;
    private static final double ELEVATION_VARIANCE_BONUS = 1.1;
    
    static {
        AREA_WEIGHTS.put(AreaType.URBAN, new DistributionWeight(1.0, 0.8, 1.2));
        AREA_WEIGHTS.put(AreaType.SUBURBAN, new DistributionWeight(1.2, 1.0, 1.0));
        AREA_WEIGHTS.put(AreaType.PARK, new DistributionWeight(1.5, 1.3, 0.9));
        AREA_WEIGHTS.put(AreaType.WATERFRONT, new DistributionWeight(1.3, 1.4, 1.1));
        AREA_WEIGHTS.put(AreaType.RURAL, new DistributionWeight(0.8, 1.1, 1.3));
    }
    
    private final Random random;
    private final Map<String, GridCell> distributionGrid;
    private final TreasureAnalytics analytics;
    
    public enum AreaType {
        URBAN, SUBURBAN, PARK, WATERFRONT, RURAL
    }
    
    public TreasureDistributionAlgorithm(TreasureAnalytics analytics) {
        this.random = new Random();
        this.distributionGrid = new HashMap<>();
        this.analytics = analytics;
    }
    
    /**
     * Calculates optimal treasure distribution for a given area
     */
    public List<TreasureSpawnCandidate> calculateOptimalDistribution(Location centerLocation, 
                                                                   double radiusKm, 
                                                                   List<ExistingTreasure> existingTreasures,
                                                                   int targetTreasureCount,
                                                                   int playerLevel) {
        Log.d(TAG, "Calculating optimal distribution for " + targetTreasureCount + " treasures");
        
        // Analyze the area
        AreaAnalysis analysis = analyzeArea(centerLocation, radiusKm, existingTreasures);
        
        // Update distribution grid
        updateDistributionGrid(centerLocation, radiusKm, existingTreasures);
        
        // Generate spawn candidates
        List<TreasureSpawnCandidate> candidates = generateSpawnCandidates(
                centerLocation, radiusKm, analysis, targetTreasureCount, playerLevel);
        
        // Apply distribution balancing
        candidates = balanceDistribution(candidates, analysis);
        
        // Sort by priority
        candidates.sort(Comparator.comparingDouble(c -> -c.priority));
        
        // Return top candidates up to target count
        return candidates.subList(0, Math.min(candidates.size(), targetTreasureCount));
    }
    
    /**
     * Validates if a spawn location meets distribution requirements
     */
    public boolean validateSpawnLocation(GeoPoint location, TreasureRarity rarity, 
                                       List<ExistingTreasure> nearbyTreasures) {
        // Check minimum distance requirements
        for (ExistingTreasure existing : nearbyTreasures) {
            double distance = calculateDistance(location, existing.location);
            
            if (distance < MIN_DISTANCE_BETWEEN_TREASURES_M) {
                return false;
            }
            
            if (existing.rarity == rarity && distance < MIN_DISTANCE_BETWEEN_SAME_RARITY_M) {
                return false;
            }
        }
        
        // Check grid cell capacity
        String gridKey = getGridKey(location);
        GridCell cell = distributionGrid.get(gridKey);
        if (cell != null && cell.treasureCount >= MAX_TREASURES_PER_GRID) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets distribution statistics for an area
     */
    public DistributionStats getDistributionStats(Location centerLocation, double radiusKm) {
        AreaAnalysis analysis = analyzeArea(centerLocation, radiusKm, new ArrayList<>());
        
        DistributionStats stats = new DistributionStats();
        stats.totalGridCells = analysis.gridCells.size();
        stats.occupiedCells = (int) analysis.gridCells.values().stream()
                .mapToInt(cell -> cell.treasureCount > 0 ? 1 : 0).sum();
        stats.averageDensity = analysis.gridCells.values().stream()
                .mapToInt(cell -> cell.treasureCount).average().orElse(0.0);
        stats.maxDensity = analysis.gridCells.values().stream()
                .mapToInt(cell -> cell.treasureCount).max().orElse(0);
        stats.distributionBalance = calculateDistributionBalance(analysis);
        
        return stats;
    }
    
    private AreaAnalysis analyzeArea(Location centerLocation, double radiusKm, 
                                   List<ExistingTreasure> existingTreasures) {
        AreaAnalysis analysis = new AreaAnalysis();
        
        // Determine area type
        analysis.areaType = determineAreaType(centerLocation);
        
        // Create grid cells for the area
        analysis.gridCells = createGridCells(centerLocation, radiusKm);
        
        // Populate grid with existing treasures
        for (ExistingTreasure treasure : existingTreasures) {
            String gridKey = getGridKey(treasure.location);
            GridCell cell = analysis.gridCells.get(gridKey);
            if (cell != null) {
                cell.treasureCount++;
                cell.rarityDistribution.merge(treasure.rarity, 1, Integer::sum);
            }
        }
        
        // Calculate density metrics
        analysis.currentDensity = existingTreasures.size() / (Math.PI * radiusKm * radiusKm);
        analysis.targetDensity = calculateTargetDensity(analysis.areaType, centerLocation);
        
        // Identify hotspots and cold spots
        analysis.hotspots = identifyHotspots(analysis.gridCells);
        analysis.coldspots = identifyColdspots(analysis.gridCells);
        
        return analysis;
    }
    
    private Map<String, GridCell> createGridCells(Location centerLocation, double radiusKm) {
        Map<String, GridCell> cells = new HashMap<>();
        
        double latStep = GRID_SIZE_KM / 111.32; // Approximate km to degrees
        double lonStep = GRID_SIZE_KM / (111.32 * Math.cos(Math.toRadians(centerLocation.getLatitude())));
        
        int gridRadius = (int) Math.ceil(radiusKm / GRID_SIZE_KM);
        
        for (int latOffset = -gridRadius; latOffset <= gridRadius; latOffset++) {
            for (int lonOffset = -gridRadius; lonOffset <= gridRadius; lonOffset++) {
                double lat = centerLocation.getLatitude() + latOffset * latStep;
                double lon = centerLocation.getLongitude() + lonOffset * lonStep;
                
                GeoPoint cellCenter = new GeoPoint(lat, lon);
                double distance = calculateDistance(cellCenter, centerLocation);
                
                if (distance <= radiusKm * 1000) { // Within radius
                    String gridKey = getGridKey(cellCenter);
                    GridCell cell = new GridCell(cellCenter, determineAreaType(new Location("") {{
                        setLatitude(lat);
                        setLongitude(lon);
                    }}));
                    cells.put(gridKey, cell);
                }
            }
        }
        
        return cells;
    }
    
    private List<TreasureSpawnCandidate> generateSpawnCandidates(Location centerLocation, 
                                                               double radiusKm, 
                                                               AreaAnalysis analysis, 
                                                               int targetCount, 
                                                               int playerLevel) {
        List<TreasureSpawnCandidate> candidates = new ArrayList<>();
        
        // Prioritize cold spots for spawning
        List<GridCell> sortedCells = new ArrayList<>(analysis.gridCells.values());
        sortedCells.sort(Comparator.comparingInt(cell -> cell.treasureCount));
        
        for (GridCell cell : sortedCells) {
            if (candidates.size() >= targetCount * 3) break; // Generate 3x candidates for selection
            
            if (cell.treasureCount < MAX_TREASURES_PER_GRID) {
                List<TreasureSpawnCandidate> cellCandidates = generateCandidatesForCell(
                        cell, analysis, playerLevel);
                candidates.addAll(cellCandidates);
            }
        }
        
        return candidates;
    }
    
    private List<TreasureSpawnCandidate> generateCandidatesForCell(GridCell cell, 
                                                                 AreaAnalysis analysis, 
                                                                 int playerLevel) {
        List<TreasureSpawnCandidate> candidates = new ArrayList<>();
        
        int maxCandidatesPerCell = MAX_TREASURES_PER_GRID - cell.treasureCount;
        
        for (int i = 0; i < maxCandidatesPerCell; i++) {
            // Generate random location within cell
            GeoPoint location = generateRandomLocationInCell(cell);
            
            // Determine rarity based on cell characteristics and player level
            TreasureRarity rarity = calculateOptimalRarity(cell, analysis, playerLevel);
            
            // Calculate priority score
            double priority = calculateSpawnPriority(location, cell, analysis, rarity);
            
            TreasureSpawnCandidate candidate = new TreasureSpawnCandidate(
                    location, rarity, priority, cell.areaType);
            candidates.add(candidate);
        }
        
        return candidates;
    }
    
    private double calculateSpawnPriority(GeoPoint location, GridCell cell, 
                                        AreaAnalysis analysis, TreasureRarity rarity) {
        double priority = 1.0;
        
        // Base priority from area type
        DistributionWeight weight = AREA_WEIGHTS.get(cell.areaType);
        if (weight != null) {
            priority *= weight.basePriority;
        }
        
        // Density balancing - prefer less dense areas
        double densityFactor = 1.0 - (cell.treasureCount / (double) MAX_TREASURES_PER_GRID);
        priority *= (1.0 + densityFactor);
        
        // Rarity balancing - avoid oversaturation of same rarity
        int existingRarityCount = cell.rarityDistribution.getOrDefault(rarity, 0);
        if (existingRarityCount > 0) {
            priority *= (1.0 - existingRarityCount * 0.2);
        }
        
        // Distance from center (slight preference for variety)
        double distanceFromCenter = calculateDistance(location, analysis.centerLocation);
        priority *= (1.0 + Math.min(distanceFromCenter / 1000.0, 0.3)); // Max 30% bonus
        
        // Analytics-based adjustments
        if (analytics != null) {
            TreasureAnalytics.SpawnRecommendation recommendation = 
                    analytics.getSpawnRecommendation(locationToLocation(location), 1);
            priority *= recommendation.spawnRateMultiplier;
        }
        
        return priority;
    }
    
    private List<TreasureSpawnCandidate> balanceDistribution(List<TreasureSpawnCandidate> candidates, 
                                                           AreaAnalysis analysis) {
        // Ensure rarity distribution balance
        Map<TreasureRarity, Integer> targetRarityCount = calculateTargetRarityDistribution(candidates.size());
        Map<TreasureRarity, Integer> currentRarityCount = new HashMap<>();
        
        List<TreasureSpawnCandidate> balancedCandidates = new ArrayList<>();
        
        // Sort candidates by priority
        candidates.sort(Comparator.comparingDouble(c -> -c.priority));
        
        for (TreasureSpawnCandidate candidate : candidates) {
            int currentCount = currentRarityCount.getOrDefault(candidate.rarity, 0);
            int targetCount = targetRarityCount.getOrDefault(candidate.rarity, 0);
            
            if (currentCount < targetCount) {
                balancedCandidates.add(candidate);
                currentRarityCount.merge(candidate.rarity, 1, Integer::sum);
            }
        }
        
        return balancedCandidates;
    }
    
    private Map<TreasureRarity, Integer> calculateTargetRarityDistribution(int totalTreasures) {
        Map<TreasureRarity, Integer> distribution = new HashMap<>();
        
        // Base distribution percentages
        distribution.put(TreasureRarity.COMMON, (int) (totalTreasures * 0.50));
        distribution.put(TreasureRarity.UNCOMMON, (int) (totalTreasures * 0.30));
        distribution.put(TreasureRarity.RARE, (int) (totalTreasures * 0.15));
        distribution.put(TreasureRarity.EPIC, (int) (totalTreasures * 0.04));
        distribution.put(TreasureRarity.LEGENDARY, (int) (totalTreasures * 0.01));
        
        return distribution;
    }
    
    private void updateDistributionGrid(Location centerLocation, double radiusKm, 
                                      List<ExistingTreasure> existingTreasures) {
        // Clear old grid data for this area
        String areaKey = getAreaKey(centerLocation);
        
        // Update grid with current treasure positions
        for (ExistingTreasure treasure : existingTreasures) {
            String gridKey = getGridKey(treasure.location);
            GridCell cell = distributionGrid.computeIfAbsent(gridKey, 
                    k -> new GridCell(treasure.location, determineAreaType(locationToLocation(treasure.location))));
            cell.treasureCount++;
            cell.rarityDistribution.merge(treasure.rarity, 1, Integer::sum);
        }
    }
    
    // Helper methods
    private String getGridKey(GeoPoint location) {
        int latGrid = (int) Math.round(location.getLatitude() / GRID_SIZE_KM * 111.32);
        int lonGrid = (int) Math.round(location.getLongitude() / GRID_SIZE_KM * 111.32);
        return latGrid + "," + lonGrid;
    }
    
    private String getAreaKey(Location location) {
        int latArea = (int) Math.round(location.getLatitude() * 100);
        int lonArea = (int) Math.round(location.getLongitude() * 100);
        return latArea + "," + lonArea;
    }
    
    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        Location loc1 = new Location("");
        loc1.setLatitude(point1.getLatitude());
        loc1.setLongitude(point1.getLongitude());
        
        Location loc2 = new Location("");
        loc2.setLatitude(point2.getLatitude());
        loc2.setLongitude(point2.getLongitude());
        
        return loc1.distanceTo(loc2);
    }
    
    private double calculateDistance(GeoPoint point, Location location) {
        Location pointLoc = new Location("");
        pointLoc.setLatitude(point.getLatitude());
        pointLoc.setLongitude(point.getLongitude());
        
        return pointLoc.distanceTo(location);
    }
    
    private Location locationToLocation(GeoPoint geoPoint) {
        Location location = new Location("");
        location.setLatitude(geoPoint.getLatitude());
        location.setLongitude(geoPoint.getLongitude());
        return location;
    }
    
    private AreaType determineAreaType(Location location) {
        // Simplified area type determination
        // In a real implementation, this would use map data APIs
        return AreaType.SUBURBAN; // Default
    }
    
    private double calculateTargetDensity(AreaType areaType, Location location) {
        DistributionWeight weight = AREA_WEIGHTS.get(areaType);
        return weight != null ? weight.densityMultiplier : 1.0;
    }
    
    private List<GridCell> identifyHotspots(Map<String, GridCell> gridCells) {
        return gridCells.values().stream()
                .filter(cell -> cell.treasureCount >= MAX_TREASURES_PER_GRID * 0.8)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private List<GridCell> identifyColdspots(Map<String, GridCell> gridCells) {
        return gridCells.values().stream()
                .filter(cell -> cell.treasureCount == 0)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private GeoPoint generateRandomLocationInCell(GridCell cell) {
        double latOffset = (random.nextDouble() - 0.5) * GRID_SIZE_KM / 111.32;
        double lonOffset = (random.nextDouble() - 0.5) * GRID_SIZE_KM / 
                (111.32 * Math.cos(Math.toRadians(cell.center.getLatitude())));
        
        return new GeoPoint(
                cell.center.getLatitude() + latOffset,
                cell.center.getLongitude() + lonOffset
        );
    }
    
    private TreasureRarity calculateOptimalRarity(GridCell cell, AreaAnalysis analysis, int playerLevel) {
        // Base rarity weights
        Map<TreasureRarity, Double> weights = new HashMap<>();
        weights.put(TreasureRarity.COMMON, 0.50);
        weights.put(TreasureRarity.UNCOMMON, 0.30);
        weights.put(TreasureRarity.RARE, 0.15);
        weights.put(TreasureRarity.EPIC, 0.04);
        weights.put(TreasureRarity.LEGENDARY, 0.01);
        
        // Adjust based on area type
        DistributionWeight areaWeight = AREA_WEIGHTS.get(cell.areaType);
        if (areaWeight != null) {
            weights.put(TreasureRarity.RARE, weights.get(TreasureRarity.RARE) * areaWeight.rarityBonus);
            weights.put(TreasureRarity.EPIC, weights.get(TreasureRarity.EPIC) * areaWeight.rarityBonus);
            weights.put(TreasureRarity.LEGENDARY, weights.get(TreasureRarity.LEGENDARY) * areaWeight.rarityBonus);
        }
        
        // Player level bonus
        double levelBonus = Math.min(playerLevel / 50.0, 0.2);
        if (levelBonus > 0) {
            weights.put(TreasureRarity.RARE, weights.get(TreasureRarity.RARE) * (1 + levelBonus));
            weights.put(TreasureRarity.EPIC, weights.get(TreasureRarity.EPIC) * (1 + levelBonus * 1.5));
            weights.put(TreasureRarity.LEGENDARY, weights.get(TreasureRarity.LEGENDARY) * (1 + levelBonus * 2));
        }
        
        // Select rarity based on weights
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double currentWeight = 0;
        for (Map.Entry<TreasureRarity, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        return TreasureRarity.COMMON;
    }
    
    private double calculateDistributionBalance(AreaAnalysis analysis) {
        if (analysis.gridCells.isEmpty()) return 1.0;
        
        double[] densities = analysis.gridCells.values().stream()
                .mapToDouble(cell -> cell.treasureCount).toArray();
        
        double mean = java.util.Arrays.stream(densities).average().orElse(0.0);
        double variance = java.util.Arrays.stream(densities)
                .map(d -> Math.pow(d - mean, 2))
                .average().orElse(0.0);
        
        // Lower variance = better balance (closer to 1.0)
        return 1.0 / (1.0 + variance);
    }
    
    // Data classes
    public static class TreasureSpawnCandidate {
        public final GeoPoint location;
        public final TreasureRarity rarity;
        public final double priority;
        public final AreaType areaType;
        
        public TreasureSpawnCandidate(GeoPoint location, TreasureRarity rarity, double priority, AreaType areaType) {
            this.location = location;
            this.rarity = rarity;
            this.priority = priority;
            this.areaType = areaType;
        }
    }
    
    public static class ExistingTreasure {
        public final GeoPoint location;
        public final TreasureRarity rarity;
        public final long spawnTime;
        
        public ExistingTreasure(GeoPoint location, TreasureRarity rarity, long spawnTime) {
            this.location = location;
            this.rarity = rarity;
            this.spawnTime = spawnTime;
        }
    }
    
    public static class DistributionStats {
        public int totalGridCells;
        public int occupiedCells;
        public double averageDensity;
        public int maxDensity;
        public double distributionBalance;
    }
    
    private static class AreaAnalysis {
        AreaType areaType;
        Map<String, GridCell> gridCells;
        double currentDensity;
        double targetDensity;
        List<GridCell> hotspots;
        List<GridCell> coldspots;
        Location centerLocation;
    }
    
    private static class GridCell {
        final GeoPoint center;
        final AreaType areaType;
        int treasureCount = 0;
        final Map<TreasureRarity, Integer> rarityDistribution = new HashMap<>();
        
        GridCell(GeoPoint center, AreaType areaType) {
            this.center = center;
            this.areaType = areaType;
        }
    }
    
    private static class DistributionWeight {
        final double basePriority;
        final double densityMultiplier;
        final double rarityBonus;
        
        DistributionWeight(double basePriority, double densityMultiplier, double rarityBonus) {
            this.basePriority = basePriority;
            this.densityMultiplier = densityMultiplier;
            this.rarityBonus = rarityBonus;
        }
    }
}
