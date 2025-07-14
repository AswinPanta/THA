package com.treasurehuntadventure.tha;

import android.app.Application;
import org.osmdroid.config.Configuration;

public class THAApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //load/initialize the osmdroid configuration.
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));
        // Initialize pirate transition manager with static images
        // PirateTransitionManager.preloadImages(null, "sea_image", "boat_image");
    }
}