package com.treasurehuntadventure.tha;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class TreasureDetailsDialog extends Dialog {

    private CapturedTreasure treasure;

    public TreasureDetailsDialog(Context context, CapturedTreasure treasure) {
        super(context);
        this.treasure = treasure;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_treasure_details);

        ImageView detailTreasureImage = findViewById(R.id.detail_treasure_image);
        TextView detailTreasureTitle = findViewById(R.id.detail_treasure_title);
        TextView detailTreasureRarity = findViewById(R.id.detail_treasure_rarity);

        Glide.with(getContext()).load(treasure.getImageUrl()).into(detailTreasureImage);
        detailTreasureTitle.setText(treasure.getImageUrl().substring(treasure.getImageUrl().lastIndexOf("/") + 1)); // Simple title from URL
        detailTreasureRarity.setText(treasure.getRarity().name());

        // Set background color based on rarity
        switch (treasure.getRarity()) {
            case COMMON:
                detailTreasureRarity.setBackgroundColor(getContext().getResources().getColor(android.R.color.darker_gray));
                break;
            case RARE:
                detailTreasureRarity.setBackgroundColor(getContext().getResources().getColor(android.R.color.holo_blue_light));
                break;
            case LEGENDARY:
                detailTreasureRarity.setBackgroundColor(getContext().getResources().getColor(android.R.color.holo_orange_light));
                break;
        }

        View root = findViewById(android.R.id.content);
        root.setScaleX(0.8f);
        root.setScaleY(0.8f);
        root.setAlpha(0f);
        root.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350).start();
    }
}