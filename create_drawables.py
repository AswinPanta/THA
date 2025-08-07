#!/usr/bin/env python3
"""
Script to create missing drawable XML files for THA enhanced UI
"""

import os

def create_drawable_dir():
    """Create the drawable directory if it doesn't exist"""
    drawable_dir = "/Users/admin/AndroidStudioProjects/THA/app/src/main/res/drawable"
    os.makedirs(drawable_dir, exist_ok=True)
    return drawable_dir

def create_drawable_files():
    """Create missing drawable XML files"""
    drawable_dir = create_drawable_dir()
    
    # Circle borders for avatars
    circle_gold_border = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke
        android:width="4dp"
        android:color="@color/op_gold" />
    <solid android:color="@android:color/transparent" />
</shape>"""
    
    circle_silver_border = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke
        android:width="3dp"
        android:color="#C0C0C0" />
    <solid android:color="@android:color/transparent" />
</shape>"""
    
    circle_bronze_border = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke
        android:width="3dp"
        android:color="@color/pirate_brown" />
    <solid android:color="@android:color/transparent" />
</shape>"""
    
    circle_avatar_border = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke
        android:width="2dp"
        android:color="@color/op_ocean_blue" />
    <solid android:color="@android:color/transparent" />
</shape>"""
    
    # Background gradients
    pirate_login_background = """<?xml version="1.0" encoding="utf-8"?>
<gradient xmlns:android="http://schemas.android.com/apk/res/android"
    android:angle="135"
    android:startColor="@color/op_deep_blue"
    android:centerColor="@color/op_ocean_blue"
    android:endColor="@color/op_light_blue"
    android:type="linear" />"""
    
    pirate_leaderboard_background = """<?xml version="1.0" encoding="utf-8"?>
<gradient xmlns:android="http://schemas.android.com/apk/res/android"
    android:angle="180"
    android:startColor="@color/op_navy"
    android:centerColor="@color/op_deep_blue"
    android:endColor="@color/op_ocean_blue"
    android:type="linear" />"""
    
    # Points badge
    points_badge = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="12dp" />
    <solid android:color="@color/op_cream" />
    <stroke
        android:width="1dp"
        android:color="@color/op_sunny_orange" />
</shape>"""
    
    # Circle status indicator
    circle_status_indicator = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/op_green" />
</shape>"""
    
    # Dot separator
    dot_separator = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/op_text_secondary" />
</shape>"""
    
    # AR crosshair
    ar_crosshair = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="80dp"
    android:viewportWidth="80"
    android:viewportHeight="80">
    
    <!-- Outer circle -->
    <path
        android:pathData="M40,5 A35,35 0 0,1 75,40 A35,35 0 0,1 40,75 A35,35 0 0,1 5,40 A35,35 0 0,1 40,5 Z"
        android:fillColor="@android:color/transparent"
        android:strokeColor="@color/op_sunny_orange"
        android:strokeWidth="2" />
    
    <!-- Cross lines -->
    <path
        android:pathData="M40,10 L40,25"
        android:strokeColor="@color/op_sunny_orange"
        android:strokeWidth="3"
        android:strokeLineCap="round" />
    
    <path
        android:pathData="M40,55 L40,70"
        android:strokeColor="@color/op_sunny_orange"
        android:strokeWidth="3"
        android:strokeLineCap="round" />
    
    <path
        android:pathData="M10,40 L25,40"
        android:strokeColor="@color/op_sunny_orange"
        android:strokeWidth="3"
        android:strokeLineCap="round" />
    
    <path
        android:pathData="M55,40 L70,40"
        android:strokeColor="@color/op_sunny_orange"
        android:strokeWidth="3"
        android:strokeLineCap="round" />
    
    <!-- Center dot -->
    <circle
        android:cx="40"
        android:cy="40"
        android:r="3"
        android:fillColor="@color/op_bright_red" />
</vector>"""
    
    # Compass needle
    compass_needle = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    
    <!-- North arrow -->
    <path
        android:pathData="M24,4 L28,20 L24,18 L20,20 Z"
        android:fillColor="@color/op_bright_red" />
    
    <!-- South arrow -->
    <path
        android:pathData="M24,44 L20,28 L24,30 L28,28 Z"
        android:fillColor="@color/op_text_secondary" />
    
    <!-- Center circle -->
    <circle
        android:cx="24"
        android:cy="24"
        android:r="3"
        android:fillColor="@color/op_navy" />
</vector>"""
    
    # Create the files
    drawables = {
        'circle_gold_border.xml': circle_gold_border,
        'circle_silver_border.xml': circle_silver_border,
        'circle_bronze_border.xml': circle_bronze_border,
        'circle_avatar_border.xml': circle_avatar_border,
        'pirate_login_background.xml': pirate_login_background,
        'pirate_leaderboard_background.xml': pirate_leaderboard_background,
        'points_badge.xml': points_badge,
        'circle_status_indicator.xml': circle_status_indicator,
        'dot_separator.xml': dot_separator,
        'ar_crosshair.xml': ar_crosshair,
        'compass_needle.xml': compass_needle
    }
    
    created_files = []
    for filename, content in drawables.items():
        file_path = os.path.join(drawable_dir, filename)
        with open(file_path, 'w') as f:
            f.write(content)
        created_files.append(file_path)
    
    return created_files

def main():
    print("🎨 Creating Enhanced UI Drawable Resources...")
    created_files = create_drawable_files()
    
    print(f"✅ Created {len(created_files)} drawable files:")
    for file_path in created_files:
        print(f"   📄 {os.path.basename(file_path)}")
    
    print("\n🚀 Enhanced UI drawables ready for THA!")

if __name__ == "__main__":
    main()
