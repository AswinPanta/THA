# Treasure Hunt Adventure (THA) Setup Guide

## 🏴‍☠️ Overview
This is a pirate-themed, location-based treasure hunting game built with Android (Java/Kotlin) and Firebase.

## 🔧 Prerequisites
- Android Studio (latest version)
- Firebase account
- Pexels API account (for images)
- Android device/emulator with API 24+

## 📋 Setup Steps

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd THA
```

### 2. Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "THA" or similar
3. Add an Android app with package name: `com.treasurehuntadventure.tha`
4. Download `google-services.json` and replace the template file in `app/`
5. Enable Authentication (Email/Password)
6. Create Firestore database in test mode
7. Set up Firestore rules (see below)

### 3. Pexels API Setup
1. Register at [Pexels](https://www.pexels.com/api/)
2. Get your API key
3. Add it to `local.properties`:
```
PEXELS_API_KEY=your_actual_pexels_api_key_here
```

### 4. Firebase Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /players/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /challenges/{challengeId} {
      allow read, write: if request.auth != null;
    }
    match /chats/{chatId} {
      allow read, write: if request.auth != null;
    }
    match /leaderboard/{docId} {
      allow read: if true;
      allow write: if false;
    }
    match /treasures/{treasureId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 5. Build and Run
1. Open project in Android Studio
2. Sync Gradle files
3. Run the app on device/emulator

## 🎮 Features Implemented

### Core Features
- ✅ Firebase Authentication (Email/Password)
- ✅ Real-time map with OSMDroid
- ✅ Location-based treasure spawning
- ✅ AR treasure capture with SceneView
- ✅ Real-time chat system
- ✅ Leaderboard system
- ✅ Challenge system
- ✅ Profile with captured treasures
- ✅ Sound effects and settings
- ✅ Pirate-themed UI with animations

### Technical Features
- ✅ Firebase Firestore for real-time data
- ✅ Location services with Google Play Services
- ✅ AR with SceneView library
- ✅ Image loading with Glide
- ✅ Material Design components
- ✅ Pirate transition animations
- ✅ Sound management system

## 🎨 UI/UX Features
- Pirate-themed color scheme
- Custom jolly roger icons
- Smooth animations and transitions
- Material Design components
- Responsive layout for different screen sizes

## 🔐 Security Notes
- Never commit `google-services.json` to public repositories
- Keep `local.properties` in `.gitignore`
- Use Firebase Security Rules for data protection

## 🐛 Troubleshooting
- If build fails, check if `google-services.json` is properly placed
- Ensure all Firebase services are enabled in console
- Check if location permissions are granted on device
- Verify Pexels API key is valid and has sufficient quota

## 📱 Testing
- Test on real device for GPS and AR features
- Test Firebase authentication flows
- Test real-time features with multiple users
- Test different treasure rarities and AR models

## 🚀 Deployment
- Generate signed APK through Android Studio
- Configure Firebase project for production
- Update Firestore rules for production security
- Test thoroughly before release

## 📞 Support
For issues or questions, check the README.md file or create an issue in the repository.

---
**Happy Treasure Hunting, Captain! 🏴‍☠️**
