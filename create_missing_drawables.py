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

def create_missing_drawables():
    """Create missing drawable XML files"""
    drawable_dir = create_drawable_dir()
    
    # Missing icon drawables
    ic_location = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13C19,5.13 15.87,2 12,2zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5S13.38,11.5 12,11.5z"/>
</vector>"""
    
    ic_close = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_text_secondary"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/>
</vector>"""
    
    ic_map = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M20.5,3l-0.16,0.03L15,5.1 9,3 3.36,4.9c-0.21,0.07 -0.36,0.25 -0.36,0.48V20.5c0,0.28 0.22,0.5 0.5,0.5l0.16,-0.03L9,18.9l6,2.1 5.64,-1.9c0.21,-0.07 0.36,-0.25 0.36,-0.48V3.5c0,-0.28 -0.22,-0.5 -0.5,-0.5zM15,19l-6,-2.11V5l6,2.11V19z"/>
</vector>"""
    
    ic_capture = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/white"
        android:pathData="M9,2L7.17,4H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5s5,2.24 5,5S14.76,17 12,17zM12,9c-1.65,0 -3,1.35 -3,3s1.35,3 3,3s3,-1.35 3,-3S13.65,9 12,9z"/>
</vector>"""
    
    ic_trophy_gold = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M7,4V2C7,1.45 7.45,1 8,1H16C16.55,1 17,1.45 17,2V4H20.38C20.77,4 21.08,4.32 21.08,4.71C21.08,4.83 21.04,4.94 20.96,5.04L18.92,7.5L20.96,9.96C21.09,10.12 21.09,10.35 20.96,10.51C20.87,10.63 20.72,10.71 20.56,10.71H17V13C17,14.1 16.1,15 15,15H9C7.9,15 7,14.1 7,13V10.71H3.44C3.05,10.71 2.74,10.39 2.74,10C2.74,9.88 2.78,9.77 2.86,9.67L4.9,7.21L2.86,4.75C2.73,4.59 2.73,4.36 2.86,4.2C2.95,4.08 3.1,4 3.26,4H7Z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M9,16H15V18H9V16Z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M10,19H14V21H10V19Z"/>
</vector>"""
    
    ic_crown = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M5,16L3,5L6.5,9L12,4L17.5,9L21,5L19,16H5ZM6,18H18V20H6V18Z"/>
</vector>"""
    
    ic_refresh = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/white"
        android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4C7.58,4 4,7.58 4,12C4,16.42 7.58,20 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18C8.69,18 6,15.31 6,12C6,8.69 8.69,6 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
</vector>"""
    
    ic_email = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M20,4H4C2.9,4 2.01,4.9 2.01,6L2,18C2,19.1 2.9,20 4,20H20C21.1,20 22,19.1 22,18V6C22,4.9 21.1,4 20,4ZM20,8L12,13L4,8V6L12,11L20,6V8Z"/>
</vector>"""
    
    ic_lock = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10C20,8.9 19.1,8 18,8zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2s2,0.9 2,2S13.1,17 12,17zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1c1.71,0 3.1,1.39 3.1,3.1V8z"/>
</vector>"""
    
    ic_sailing = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/white"
        android:pathData="M11,2V19L3,17V15L11,2ZM13,2L21,15V17L13,19V2ZM2,20H22V22H2V20Z"/>
</vector>"""
    
    ic_person_add = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M15,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4s-4,1.79 -4,4S12.79,12 15,12zM6,10V7H4V10H1V12H4V15H6V12H9V10H6zM15,14c-2.67,0 -8,1.34 -8,4v2h16v-2C23,15.34 17.67,14 15,14z"/>
</vector>"""
    
    ic_error = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2V17zM13,13h-2V7h2V13z"/>
</vector>"""
    
    # Background images (simple colored rectangles)
    ocean_waves_bg = """<?xml version="1.0" encoding="utf-8"?>
<gradient xmlns:android="http://schemas.android.com/apk/res/android"
    android:angle="45"
    android:startColor="@color/op_deep_blue"
    android:centerColor="@color/op_ocean_blue"
    android:endColor="@color/op_light_blue"
    android:type="linear" />"""
    
    leaderboard_header_bg = """<?xml version="1.0" encoding="utf-8"?>
<gradient xmlns:android="http://schemas.android.com/apk/res/android"
    android:angle="135"
    android:startColor="@color/op_navy"
    android:centerColor="@color/op_deep_blue"
    android:endColor="@color/op_ocean_blue"
    android:type="linear" />"""
    
    # Default avatar
    default_avatar = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="64"
    android:viewportHeight="64">
    <path
        android:fillColor="@color/op_ocean_blue"
        android:pathData="M32,32m-30,0a30,30 0,1 1,60 0a30,30 0,1 1,-60 0"/>
    <path
        android:fillColor="@color/white"
        android:pathData="M32,20c-4.42,0 -8,3.58 -8,8c0,4.42 3.58,8 8,8s8,-3.58 8,-8C40,23.58 36.42,20 32,20z"/>
    <path
        android:fillColor="@color/white"
        android:pathData="M32,38c-8.84,0 -16,7.16 -16,16h32C48,45.16 40.84,38 32,38z"/>
</vector>"""
    
    # Treasure chest
    treasure_chest = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path
        android:fillColor="@color/pirate_brown"
        android:pathData="M8,16h32v20H8z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M6,14h36v4H6z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M22,20h4v4h-4z"/>
    <path
        android:fillColor="@color/op_dark_gold"
        android:pathData="M8,12h32v4H8z"/>
</vector>"""
    
    # Circle treasure background
    circle_treasure_bg = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <gradient
        android:startColor="@color/op_gold"
        android:endColor="@color/op_dark_gold"
        android:type="radial"
        android:gradientRadius="50dp" />
    <stroke
        android:width="2dp"
        android:color="@color/op_dark_gold" />
</shape>"""
    
    # Small icons for list items
    ic_star_small = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="12dp"
    android:height="12dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.63L12,2L9.19,8.63L2,9.24L7.46,13.97L5.82,21L12,17.27Z"/>
</vector>"""
    
    ic_treasure_small = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="12dp"
    android:height="12dp"
    android:viewportWidth="24"
    android:viewportWidth="24">
    <path
        android:fillColor="@color/op_text_secondary"
        android:pathData="M4,8h16v8H4z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M2,6h20v2H2z"/>
    <path
        android:fillColor="@color/op_gold"
        android:pathData="M11,10h2v2h-2z"/>
</vector>"""
    
    ic_trending_up = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/op_green"
        android:pathData="M16,6L18.29,8.29L13.41,13.17L9.41,9.17L2,16.59L3.41,18L9.41,12L13.41,16L19.71,9.71L22,12V6H16Z"/>
</vector>"""
    
    # Create the files
    drawables = {
        'ic_location.xml': ic_location,
        'ic_close.xml': ic_close,
        'ic_map.xml': ic_map,
        'ic_capture.xml': ic_capture,
        'ic_trophy_gold.xml': ic_trophy_gold,
        'ic_crown.xml': ic_crown,
        'ic_refresh.xml': ic_refresh,
        'ic_email.xml': ic_email,
        'ic_lock.xml': ic_lock,
        'ic_sailing.xml': ic_sailing,
        'ic_person_add.xml': ic_person_add,
        'ic_error.xml': ic_error,
        'ocean_waves_bg.xml': ocean_waves_bg,
        'leaderboard_header_bg.xml': leaderboard_header_bg,
        'default_avatar.xml': default_avatar,
        'treasure_chest.xml': treasure_chest,
        'circle_treasure_bg.xml': circle_treasure_bg,
        'ic_star_small.xml': ic_star_small,
        'ic_treasure_small.xml': ic_treasure_small,
        'ic_trending_up.xml': ic_trending_up
    }
    
    created_files = []
    for filename, content in drawables.items():
        file_path = os.path.join(drawable_dir, filename)
        with open(file_path, 'w') as f:
            f.write(content)
        created_files.append(file_path)
    
    return created_files

def main():
    print("🎨 Creating Missing Drawable Resources...")
    created_files = create_missing_drawables()
    
    print(f"✅ Created {len(created_files)} missing drawable files:")
    for file_path in created_files:
        print(f"   📄 {os.path.basename(file_path)}")
    
    print("\n🚀 All drawable resources ready!")

if __name__ == "__main__":
    main()
