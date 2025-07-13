package com.treasurehuntadventure.tha;

import android.content.Context;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class PexelsApiClient {

    private static final String BASE_URL = "https://api.pexels.com/v1/";
    private RequestQueue requestQueue;

    public PexelsApiClient(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void searchPhotos(String query, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "search?query=" + query + "&per_page=1";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", BuildConfig.PEXELS_API_KEY);
                return headers;
            }
        };

        requestQueue.add(stringRequest);
    }
}