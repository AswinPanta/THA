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
        // Preload pirate ship and sea cartoon images for transitions
        PexelsApiClient pexels = new PexelsApiClient(this);
        pexels.searchPhotos("sea cartoon", response -> {
            try {
                org.json.JSONObject json = new org.json.JSONObject(response);
                String seaUrl = json.getJSONArray("photos").getJSONObject(0).getJSONObject("src").getString("large");
                pexels.searchPhotos("pirate ship cartoon", response2 -> {
                    try {
                        org.json.JSONObject json2 = new org.json.JSONObject(response2);
                        String boatUrl = json2.getJSONArray("photos").getJSONObject(0).getJSONObject("src").getString("large");
                        PirateTransitionManager.preloadImages((android.app.Activity) null, seaUrl, boatUrl);
                    } catch (Exception ignore) {}
                }, error -> {});
            } catch (Exception ignore) {}
        }, error -> {});
    }
}