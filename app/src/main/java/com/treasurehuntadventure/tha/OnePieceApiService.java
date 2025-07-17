package com.treasurehuntadventure.tha;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnePieceApiService {
    private static final String TAG = "OnePieceApiService";
    private static final String BASE_URL = "https://api.api-onepiece.com/v2/characters";
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface OnePieceCharacterCallback {
        void onSuccess(List<OnePieceCharacter> characters);
        void onError(String error);
    }
    
    public interface CharacterCallback {
        void onSuccess(OnePieceCharacter character);
        void onError(String error);
    }
    
    public static class OnePieceCharacter {
        public String name;
        public String description;
        public String imageUrl;
        public String devilFruit;
        public String crew;
        public String job;
        public int bounty;
        
        public OnePieceCharacter(String name, String description, String imageUrl, 
                               String devilFruit, String crew, String job, int bounty) {
            this.name = name;
            this.description = description;
            this.imageUrl = imageUrl;
            this.devilFruit = devilFruit;
            this.crew = crew;
            this.job = job;
            this.bounty = bounty;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
        public String getDevilFruit() { return devilFruit; }
        public String getCrew() { return crew; }
        public String getJob() { return job; }
        public int getBounty() { return bounty; }
    }
    
    public static void fetchCharacters(OnePieceCharacterCallback callback) {
        executor.execute(() -> {
            try {
                String jsonResponse = makeHttpRequest(BASE_URL);
                List<OnePieceCharacter> characters = parseCharacters(jsonResponse);
                
                mainHandler.post(() -> callback.onSuccess(characters));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching One Piece characters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public static void fetchRandomCharacter(CharacterCallback callback) {
        executor.execute(() -> {
            try {
                String jsonResponse = makeHttpRequest(BASE_URL);
                List<OnePieceCharacter> characters = parseCharacters(jsonResponse);
                
                if (characters.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No characters found"));
                    return;
                }
                
                // Get random character
                int randomIndex = new java.util.Random().nextInt(characters.size());
                OnePieceCharacter randomCharacter = characters.get(randomIndex);
                
                mainHandler.post(() -> callback.onSuccess(randomCharacter));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching random One Piece character", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    private static String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        
        reader.close();
        inputStream.close();
        connection.disconnect();
        
        return response.toString();
    }
    
    private static List<OnePieceCharacter> parseCharacters(String jsonResponse) throws JSONException {
        List<OnePieceCharacter> characters = new ArrayList<>();
        JSONArray charactersArray = new JSONArray(jsonResponse);
        
        for (int i = 0; i < charactersArray.length(); i++) {
            JSONObject charObj = charactersArray.getJSONObject(i);
            
            String name = charObj.optString("name", "Unknown");
            String description = charObj.optString("description", "");
            String imageUrl = charObj.optString("image", "");
            String devilFruit = charObj.optString("devil_fruit", "");
            String crew = charObj.optString("crew", "");
            String job = charObj.optString("job", "");
            int bounty = charObj.optInt("bounty", 0);
            
            OnePieceCharacter character = new OnePieceCharacter(name, description, imageUrl, 
                                                               devilFruit, crew, job, bounty);
            characters.add(character);
        }
        
        return characters;
    }
}
