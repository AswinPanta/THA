package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ARActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_simple);
        
        TextView arTitle = findViewById(R.id.ar_title);
        ImageView arImage = findViewById(R.id.ar_image);
        
        String glbFile = getIntent().getStringExtra("glbFile");
        int resId = getGlbResId(glbFile);
        
        arTitle.setText("🏴‍☠️ AR Treasure View");
        
        if (resId != 0) {
            arImage.setImageResource(R.drawable.jollyroger);
        }
    }
    
    private int getGlbResId(String glbFile) {
        if (glbFile == null) return 0;
        switch (glbFile) {
            case "gold_bar.glb":
                return R.raw.gold_bar;
            case "gold_coin.glb":
                return R.raw.gold_coin;
            case "treasure_chest.glb":
                return R.raw.treasure_chest;
            default:
                return 0;
        }
    }
}
