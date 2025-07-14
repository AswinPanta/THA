package com.treasurehuntadventure.tha;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseTestHelper {
    private static final String TAG = "FirebaseTest";
    
    public static void testFirebaseConnection(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        Log.d(TAG, "Testing Firebase connection...");
        
        // Test Firestore connection
        Map<String, Object> testData = new HashMap<>();
        testData.put("test", "Hello Firebase!");
        testData.put("timestamp", System.currentTimeMillis());
        
        db.collection("test")
            .add(testData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Firestore connection successful! Document ID: " + documentReference.getId());
                Toast.makeText(context, "✅ Firestore connected successfully!", Toast.LENGTH_SHORT).show();
                
                // Clean up test data
                documentReference.delete();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Firestore connection failed", e);
                Toast.makeText(context, "❌ Firestore connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        
        // Test Auth connection
        if (auth.getCurrentUser() != null) {
            Log.d(TAG, "User is authenticated: " + auth.getCurrentUser().getEmail());
            Toast.makeText(context, "✅ User authenticated: " + auth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "No user currently authenticated");
            Toast.makeText(context, "ℹ️ No user authenticated", Toast.LENGTH_SHORT).show();
        }
    }
}
