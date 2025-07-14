package com.treasurehuntadventure.tha;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.Random;

public class PexelsApiClient {
    private RequestQueue requestQueue;
    private Random random;

    public PexelsApiClient(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        random = new Random();
    }

    public void searchPhotos(String query, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        // Use Lorem Picsum for free placeholder images
        int imageId = random.nextInt(1000) + 1; // Random image ID
        String imageUrl = "https://picsum.photos/400/400?random=" + imageId;
        
        // Create a fake JSON response similar to Pexels API
        String fakeResponse = "{" +
                "  \"photos\": [" +
                "    {" +
                "      \"src\": {" +
                "        \"medium\": \"" + imageUrl + "\"," +
                "        \"large\": \"" + imageUrl + "\"" +
                "      }" +
                "    }" +
                "  ]" +
                "}";
        
        // Simulate API response
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> listener.onResponse(fakeResponse), 100);
    }
}
