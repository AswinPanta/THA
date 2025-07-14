package com.treasurehuntadventure.tha;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class PirateTransitionManager {
    private static Bitmap seaBitmap;
    private static Bitmap boatBitmap;
    private static boolean imagesLoaded = false;

    public static void preloadImages(Activity activity, String seaUrl, String boatUrl) {
        if (activity == null) {
            imagesLoaded = false;
            return;
        }
        Glide.with(activity).asBitmap().load(seaUrl).into(new CustomTarget<Bitmap>() {
            @Override public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                seaBitmap = resource;
                checkLoaded(activity);
            }
            @Override public void onLoadCleared(Drawable placeholder) {}
        });
        Glide.with(activity).asBitmap().load(boatUrl).into(new CustomTarget<Bitmap>() {
            @Override public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                boatBitmap = resource;
                checkLoaded(activity);
            }
            @Override public void onLoadCleared(Drawable placeholder) {}
        });
    }
    private static void checkLoaded(Activity activity) {
        if (seaBitmap != null && boatBitmap != null) imagesLoaded = true;
    }
    public static void showTransition(Activity activity, Runnable onComplete) {
        if (!imagesLoaded) { onComplete.run(); return; }
        LayoutInflater inflater = activity.getLayoutInflater();
        View overlay = inflater.inflate(R.layout.transition_pirate_boat, null);
        ImageView seaBg = overlay.findViewById(R.id.seaBackgroundImageView);
        ImageView boat = overlay.findViewById(R.id.pirateBoatImageView);
        seaBg.setImageBitmap(seaBitmap);
        boat.setImageBitmap(boatBitmap);
        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        decor.addView(overlay);
        int screenWidth = decor.getWidth();
        boat.setTranslationX(-boat.getWidth());
        boat.animate().translationX(screenWidth + boat.getWidth()).setDuration(900).withEndAction(() -> {
            decor.removeView(overlay);
            onComplete.run();
        }).start();
    }
} 