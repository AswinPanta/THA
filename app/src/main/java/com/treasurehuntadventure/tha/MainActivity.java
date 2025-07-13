package com.treasurehuntadventure.tha;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private AppLocationManager appLocationManager;
    private MyLocationNewOverlay mLocationOverlay;
    private SoundManager soundManager;

    private ImageView treasureIcon;
    private Button zoomInButton;
    private Button zoomOutButton;
    private TextView nearbyTreasureText;
    private Treasure currentNearbyTreasure;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private float currentAzimuth = 0f; // To store the current device azimuth

    private final List<Marker> treasureMarkers = new ArrayList<>();
    private Marker activeTreasure;
    public static int capturedTreasuresCount = 0;
    private SharedPreferences sharedPreferences;
    private Set<CapturedTreasure> capturedTreasures = new HashSet<>();
    private PexelsApiClient pexelsApiClient;

    private FirebaseFirestore db; // Firestore instance
    private CollectionReference treasuresCollection; // Firestore collection reference

    private String userId;
    private ListenerRegistration playersListener;
    private final List<Marker> playerMarkers = new ArrayList<>();

    private ImageView treasureDirectionArrow; // New: 2D arrow for treasure direction

    private ActivityResultLauncher<Intent> captureActivityLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        treasureIcon = findViewById(R.id.treasure_icon);
        zoomInButton = findViewById(R.id.zoom_in_button);
        zoomOutButton = findViewById(R.id.zoom_out_button);
        nearbyTreasureText = findViewById(R.id.nearby_treasure_text);

        treasureDirectionArrow = findViewById(R.id.treasure_direction_arrow); // Initialize the new arrow
        treasureDirectionArrow.setVisibility(View.GONE); // Hide by default

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sharedPreferences = getSharedPreferences("TreasureHuntPrefs", MODE_PRIVATE);
        loadCapturedTreasures();

        appLocationManager = new AppLocationManager(this);
        soundManager = SoundManager.getInstance(this);
        pexelsApiClient = new PexelsApiClient(this);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        treasuresCollection = db.collection("treasures");

        // osmdroid configuration
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
        org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(new java.io.File(getCacheDir(), "osmdroid"));

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.setPersonIcon(((android.graphics.drawable.BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.jollyroger)).getBitmap());
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        appLocationManager.startLocationUpdates(new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    appLocationManager.setCurrentLocation(location);
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().setCenter(geoPoint);
                    map.getController().setZoom(18.0);
                    mLocationOverlay.onLocationChanged(location, null);
                    checkForNearbyTreasures();
                    spawnTreasures();
                    updatePlayerLocation(location);
                }
            }
        });

        zoomInButton.setOnClickListener(v -> map.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> map.getController().zoomOut());

        userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        captureActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Treasure captured, update UI
                    if (activeTreasure != null) {
                        // Update local data
                        Treasure capturedTreasureData = (Treasure) activeTreasure.getRelatedObject();
                        if (capturedTreasureData != null) {
                            capturedTreasures.add(new CapturedTreasure(capturedTreasureData.getImageUrl(), capturedTreasureData.getRarity()));
                            saveCapturedTreasures();
                        }

                        map.getOverlays().remove(activeTreasure);
                        treasureMarkers.remove(activeTreasure);
                        capturedTreasuresCount++;
                        activeTreasure = null;
                        treasureIcon.setVisibility(View.GONE);
                        nearbyTreasureText.setVisibility(View.GONE);
                        nearbyTreasureText.setText("No treasures nearby");
                        map.invalidate();
                        spawnTreasures();
                        soundManager.playTreasureCaptureSound();
                    }
                }
            });

        // Set up FAB buttons
        findViewById(R.id.fab_profile).setOnClickListener(v -> 
            startActivity(new Intent(this, ProfileActivity.class))
        );
        findViewById(R.id.fab_settings).setOnClickListener(v -> 
            startActivity(new Intent(this, SettingsActivity.class))
        );
        findViewById(R.id.fab_ar).setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            intent.putExtra("glbFile", "treasure_chest.glb");
            startActivity(intent);
        });

        treasureIcon.setOnClickListener(v -> {
            if (activeTreasure != null) {
                Intent intent = new Intent(this, CaptureActivity.class);
                Treasure treasure = (Treasure) activeTreasure.getRelatedObject();
                intent.putExtra("treasureId", treasure.getId());
                intent.putExtra("treasureImageUrl", treasure.getImageUrl());
                intent.putExtra("treasureTitle", treasure.getTitle());
                intent.putExtra("treasureRarity", treasure.getRarity().name());
                captureActivityLauncher.launch(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        appLocationManager.startLocationUpdates(new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    appLocationManager.setCurrentLocation(location);
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().setCenter(geoPoint);
                    map.getController().setZoom(18.0);
                    mLocationOverlay.onLocationChanged(location, null);
                    checkForNearbyTreasures();
                    spawnTreasures();
                    updatePlayerLocation(location);
                }
            }
        });
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
        listenForOtherPlayers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        appLocationManager.stopLocationUpdates();
        sensorManager.unregisterListener(this);
        if (playersListener != null) playersListener.remove();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        appLocationManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void startActivity(Intent intent) {
        PirateTransitionManager.showTransition(this, () -> super.startActivity(intent));
    }

    private void spawnTreasures() {
        Location currentLocation = appLocationManager.getCurrentLocation();
        if (currentLocation == null) {
            return;
        }

        // Clear existing treasures
        for (Marker marker : treasureMarkers) {
            map.getOverlays().remove(marker);
        }
        treasureMarkers.clear();

        // Check existing treasures in Firestore and generate new ones if needed
        treasuresCollection.whereEqualTo("captured", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int existingTreasures = task.getResult().size();
                        int treasuresToGenerate = 5 - existingTreasures; // Maintain 5 uncaptured treasures

                        if (treasuresToGenerate > 0) {
                            for (int i = 0; i < treasuresToGenerate; i++) {
                                generateAndSaveTreasureToFirestore(currentLocation);
                            }
                        }
                        // After ensuring enough treasures, fetch and display them
                        fetchTreasuresFromFirestore();
                    } else {
                        Log.w("Firestore", "Error getting documents for spawning: ", task.getException());
                        Toast.makeText(MainActivity.this, "Error checking treasures in Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generateAndSaveTreasureToFirestore(Location currentLocation) {
        Random random = new Random();
        double latOffset = (random.nextDouble() - 0.5) * 0.02;
        double lonOffset = (random.nextDouble() - 0.5) * 0.02;
        GeoPoint treasureLocation = new GeoPoint(currentLocation.getLatitude() + latOffset,
                currentLocation.getLongitude() + lonOffset);

        Treasure newTreasure = new Treasure("Treasure " + System.currentTimeMillis(), "", treasureLocation, getRandomRarity());
        newTreasure.setId(treasuresCollection.document().getId()); // Generate a unique ID for Firestore

        // Fetch image for treasure details dialog (asynchronously)
        pexelsApiClient.searchPhotos(newTreasure.getRarity().name().toLowerCase() + " treasure",
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray photos = jsonResponse.getJSONArray("photos");
                        if (photos.length() > 0) {
                            JSONObject photo = photos.getJSONObject(0);
                            JSONObject src = photo.getJSONObject("src");
                            String imageUrl = src.getString("medium");
                            newTreasure.setImageUrl(imageUrl);
                        }
                    } catch (Exception e) {
                        Log.e("PexelsAPI", "Error parsing Pexels API response for new treasure: " + e.getMessage(), e);
                    } finally {
                        // Save to Firestore after image URL is fetched or if fetching fails
                        Map<String, Object> treasureData = new HashMap<>();
                        treasureData.put("id", newTreasure.getId());
                        treasureData.put("title", newTreasure.getTitle());
                        treasureData.put("imageUrl", newTreasure.getImageUrl());
                        treasureData.put("rarity", newTreasure.getRarity().name());
                        treasureData.put("latitude", newTreasure.getLocation().getLatitude());
                        treasureData.put("longitude", newTreasure.getLocation().getLongitude());
                        treasureData.put("captured", false); // Initial status

                        treasuresCollection.document(newTreasure.getId()).set(treasureData)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Treasure added with ID: " + newTreasure.getId()))
                                .addOnFailureListener(e -> Log.w("Firestore", "Error adding treasure", e));
                    }
                },
                error -> {
                    Log.e("PexelsAPI", "Error fetching image from Pexels for new treasure: " + error.getMessage(), error);
                    // Save to Firestore even if image fetching fails
                    Map<String, Object> treasureData = new HashMap<>();
                    treasureData.put("id", newTreasure.getId());
                    treasureData.put("title", newTreasure.getTitle());
                    treasureData.put("imageUrl", newTreasure.getImageUrl()); // Will be null or empty
                    treasureData.put("rarity", newTreasure.getRarity().name());
                    treasureData.put("latitude", newTreasure.getLocation().getLatitude());
                    treasureData.put("longitude", newTreasure.getLocation().getLongitude());
                    treasureData.put("captured", false); // Initial status

                    treasuresCollection.document(newTreasure.getId()).set(treasureData)
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "Treasure added with ID: " + newTreasure.getId()))
                            .addOnFailureListener(e -> Log.w("Firestore", "Error adding treasure", e));
                });
    }

    private void fetchTreasuresFromFirestore() {
        treasuresCollection.whereEqualTo("captured", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Treasure> fetchedTreasures = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            // Convert Firestore document to Treasure object
                            String id = document.getId();
                            String title = document.getString("title");
                            String imageUrl = document.getString("imageUrl");
                            double latitude = document.getDouble("latitude");
                            double longitude = document.getDouble("longitude");
                            TreasureRarity rarity = TreasureRarity.valueOf(document.getString("rarity"));

                            GeoPoint location = new GeoPoint(latitude, longitude);
                            Treasure treasure = new Treasure(title, imageUrl, location, rarity);
                            treasure.setId(id);
                            fetchedTreasures.add(treasure);
                        }

                        // Display fetched treasures
                        displayTreasures(fetchedTreasures);

                    } else {
                        Log.w("Firestore", "Error getting documents: ", task.getException());
                        Toast.makeText(MainActivity.this, "Error fetching treasures from Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayTreasures(List<Treasure> treasures) {
        for (Marker marker : treasureMarkers) {
            map.getOverlays().remove(marker);
        }
        treasureMarkers.clear();

        for (Treasure newTreasure : treasures) {
            Marker treasureMarker = new Marker(map);
            treasureMarker.setPosition(newTreasure.getLocation());
            treasureMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            TreasureRarity rarity = newTreasure.getRarity();
            if (rarity == TreasureRarity.COMMON) {
                treasureMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.jollyroger));
            } else if (rarity == TreasureRarity.RARE) {
                treasureMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.jollyroger));
            } else {
                treasureMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.jollyroger));
            }

            treasureMarker.setTitle(newTreasure.getTitle());
            treasureMarker.setSnippet(newTreasure.getImageUrl());
            treasureMarker.setRelatedObject(newTreasure);

            map.getOverlays().add(treasureMarker);
            treasureMarkers.add(treasureMarker);
        }
        map.invalidate();
        soundManager.playTreasureAppearSound();
    }

    private TreasureRarity getRandomRarity() {
        Random random = new Random();
        double rarityRoll = random.nextDouble();
        if (rarityRoll < 0.70) {
            return TreasureRarity.COMMON;
        } else if (rarityRoll < 0.95) {
            return TreasureRarity.RARE;
        } else {
            return TreasureRarity.LEGENDARY;
        }
    }

    private void checkForNearbyTreasures() {
        Location currentLocation = appLocationManager.getCurrentLocation();
        if (currentLocation == null) return;

        activeTreasure = null;
        for (Marker treasure : treasureMarkers) {
            Location treasureLocation = new Location("");
            treasureLocation.setLatitude(treasure.getPosition().getLatitude());
            treasureLocation.setLongitude(treasure.getPosition().getLongitude());

            float distance = currentLocation.distanceTo(treasureLocation);

            if (distance < 50) { // If treasure is within 50 meters
                activeTreasure = treasure;
                // Calculate bearing to treasure
                float bearingToTreasure = currentLocation.bearingTo(treasureLocation);
                // Calculate relative angle for the arrow
                float relativeBearing = (bearingToTreasure - currentAzimuth + 360) % 360;
                treasureDirectionArrow.setRotation(relativeBearing);
                treasureDirectionArrow.setVisibility(View.VISIBLE);
                break;
            }
        }

        if (activeTreasure != null) {
            treasureIcon.setVisibility(View.VISIBLE);
            nearbyTreasureText.setVisibility(View.VISIBLE);
            Treasure currentTreasure = (Treasure) activeTreasure.getRelatedObject();
            nearbyTreasureText.setText("Treasure nearby: " + currentTreasure.getTitle());
            Glide.with(this).load(currentTreasure.getImageUrl()).into(treasureIcon);

            // Add pulsing animation to treasureIcon
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(treasureIcon, "scaleX", 0.8f, 1.2f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(treasureIcon, "scaleY", 0.8f, 1.2f);
            scaleX.setDuration(1000);
            scaleY.setDuration(1000);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleY.setRepeatMode(ObjectAnimator.REVERSE);
            scaleX.start();
            scaleY.start();

            currentNearbyTreasure = (Treasure) activeTreasure.getRelatedObject();

        } else {
            treasureIcon.setVisibility(View.GONE);
            nearbyTreasureText.setVisibility(View.GONE);
            nearbyTreasureText.setText("No treasures nearby");
            // Stop any ongoing animations when treasure is not active
            treasureIcon.clearAnimation();
            currentNearbyTreasure = null;
            treasureDirectionArrow.setVisibility(View.GONE); // Hide arrow if no treasure nearby
        }
    }

    private void saveCapturedTreasures() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(capturedTreasures);
        editor.putString("capturedTreasures", json);
        editor.apply();
    }

    private void loadCapturedTreasures() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString("capturedTreasures", null);
        Type type = new TypeToken<HashSet<CapturedTreasure>>() {}.getType();
        capturedTreasures = gson.fromJson(json, type);
        if (capturedTreasures == null) {
            capturedTreasures = new HashSet<>();
        }
        capturedTreasuresCount = capturedTreasures.size();
    }

    private void updatePlayerLocation(Location location) {
        if (userId == null || location == null) return;
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("latitude", location.getLatitude());
        playerData.put("longitude", location.getLongitude());
        playerData.put("lastUpdated", System.currentTimeMillis());
        treasuresCollection.getFirestore().collection("players").document(userId).set(playerData);
    }

    private void listenForOtherPlayers() {
        if (playersListener != null) playersListener.remove();
        playersListener = treasuresCollection.getFirestore().collection("players")
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                // Remove old markers
                for (Marker m : playerMarkers) map.getOverlays().remove(m);
                playerMarkers.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    String id = doc.getId();
                    if (id.equals(userId)) continue; // Don't show self
                    double lat = doc.getDouble("latitude");
                    double lon = doc.getDouble("longitude");
                    Marker marker = new Marker(map);
                    marker.setPosition(new org.osmdroid.util.GeoPoint(lat, lon));
                    marker.setIcon(ContextCompat.getDrawable(this, R.drawable.player_icon));
                    marker.setTitle("Player");
                    map.getOverlays().add(marker);
                    playerMarkers.add(marker);
                }
                map.invalidate();
            });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }
        if (gravity != null && geomagnetic != null) {
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                float azimuthInRadians = orientation[0];
                currentAzimuth = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360; // Update currentAzimuth
                ImageView playerAvatar = findViewById(R.id.player_avatar);
                playerAvatar.setRotation(-currentAzimuth);
            }
        }
    }

    private void fetchTreasureImage(Treasure treasure) {
        String query;
        switch (treasure.getRarity()) {
            case COMMON:
                query = "common treasure";
                break;
            case RARE:
                query = "rare treasure";
                break;
            case LEGENDARY:
                query = "legendary treasure";
                break;
            default:
                query = "treasure";
        }

        pexelsApiClient.searchPhotos(query,
                response -> {
                    try {
                        Log.d("PexelsAPI", "Pexels API Response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray photos = jsonResponse.getJSONArray("photos");
                        if (photos.length() > 0) {
                            JSONObject photo = photos.getJSONObject(0);
                            JSONObject src = photo.getJSONObject("src");
                            String imageUrl = src.getString("medium"); // Or "large", "original"
                            treasure.setImageUrl(imageUrl);
                            // Update the marker's snippet with the new image URL
                            for (Marker marker : treasureMarkers) {
                                if (marker.getRelatedObject() == treasure) {
                                    marker.setSnippet(imageUrl);
                                    break;
                                }
                            }
                        } else {
                            Log.w("PexelsAPI", "No photos found for query: " + query);
                            Toast.makeText(MainActivity.this, "No Pexels images found for " + query, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("PexelsAPI", "Error parsing Pexels API response: " + e.getMessage(), e);
                        Toast.makeText(MainActivity.this, "Error parsing Pexels API response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("PexelsAPI", "Error fetching image from Pexels: " + error.getMessage(), error);
                    Toast.makeText(MainActivity.this, "Error fetching image from Pexels", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used for this implementation
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (id == R.id.action_chat) {
            startActivity(new Intent(this, ChatActivity.class));
            return true;
        }
        if (id == R.id.action_leaderboard) {
            startActivity(new Intent(this, LeaderboardActivity.class));
            return true;
        }
        if (id == R.id.action_challenges) {
            startActivity(new Intent(this, ChallengesActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
