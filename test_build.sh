#!/bin/bash

echo "=== Treasure Hunt Adventure - Build and Test Script ==="
echo "Starting build and test process..."

# Make gradlew executable
chmod +x gradlew

echo "1. Cleaning project..."
./gradlew clean

echo "2. Compiling project..."
./gradlew compileDebugJavaWithJavac --stacktrace

echo "3. Running unit tests..."
./gradlew testDebugUnitTest --stacktrace

echo "4. Building APK..."
./gradlew assembleDebug --stacktrace

echo "=== Build and Test Complete ==="
