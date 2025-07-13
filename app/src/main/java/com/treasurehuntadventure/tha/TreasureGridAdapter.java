package com.treasurehuntadventure.tha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.List;

public class TreasureGridAdapter extends ArrayAdapter<CapturedTreasure> {
    private final List<CapturedTreasure> treasures;

    public TreasureGridAdapter(@NonNull AppCompatActivity context, @NonNull List<CapturedTreasure> treasures) {
        super(context, 0, treasures);
        this.treasures = treasures;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_treasure, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.treasure_image);
        TextView rarityTextView = convertView.findViewById(R.id.rarity_text);

        CapturedTreasure treasure = treasures.get(position);

        Glide.with(getContext())
                .load(treasure.getImageUrl())
                .placeholder(R.drawable.jollyroger) // Placeholder while loading
                .error(R.drawable.jollyroger) // Error image if loading fails
                .into(imageView);

        rarityTextView.setText(treasure.getRarity().name());

        // Set background color based on rarity
        switch (treasure.getRarity()) {
            case COMMON:
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.common_treasure_color));
                break;
            case RARE:
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.rare_treasure_color));
                break;
            case LEGENDARY:
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.legendary_treasure_color));
                break;
        }
        // Animate item appearance
        convertView.setAlpha(0f);
        convertView.animate().alpha(1f).setDuration(400).start();
        return convertView;
    }
}