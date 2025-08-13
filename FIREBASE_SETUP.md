# 🔥 Firebase Setup Guide

This guide covers the complete Firebase setup for the Treasure Hunt Adventure app with AI-powered challenge system.

## 📋 Prerequisites

1. **Firebase Project**: Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. **Android App**: Add an Android app to your Firebase project
3. **Package Name**: Use `com.treasurehuntadventure.tha`

## 🔧 Firebase Services Setup

### 1. Authentication
- Enable **Email/Password** authentication
- Optional: Enable **Google Sign-In** for enhanced user experience

### 2. Firestore Database
- Create a Firestore database in **test mode** initially
- Apply the security rules provided below after testing

### 3. Storage
- Enable Firebase Storage for user-generated content
- Apply the storage rules provided below

### 4. Analytics (Optional)
- Enable Google Analytics for user behavior tracking
- Useful for ML model training data

## 🛡️ Security Rules

### Firestore Rules
Copy and paste these rules in the Firebase Console > Firestore Database > Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Player profiles - users can only access their own data
    match /players/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Personalized challenges - users can only access their own challenges
    match /personalized_challenges/{challengeId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Challenge progress - users can only access their own progress
    match /challenge_progress/{progressId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Challenge milestones - users can only access their own milestones
    match /challenge_milestones/{milestoneId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Player behavior data - users can only access their own behavior data
    match /player_behavior/{behaviorId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Behavior sessions - users can only access their own sessions
    match /behavior_sessions/{sessionId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Challenge analytics - users can read their own analytics
    match /challenge_analytics/{analyticsId} {
      allow read: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
      allow write: if false; // Only server can write analytics
    }
    
    // ML model feedback - users can write feedback for their challenges
    match /ml_feedback/{feedbackId} {
      allow read, write: if request.auth != null && 
        (resource == null || resource.data.playerId == request.auth.uid);
    }
    
    // Challenge templates - read-only for all authenticated users
    match /challenge_templates/{templateId} {
      allow read: if request.auth != null;
      allow write: if false; // Only admins can modify templates
    }
    
    // Global challenge statistics - read-only for all authenticated users
    match /global_challenge_stats/{statId} {
      allow read: if request.auth != null;
      allow write: if false; // Only server can update global stats
    }
    
    // Original collections (keeping existing functionality)
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
    
    // Admin-only collections
    match /admin/{document=**} {
      allow read, write: if request.auth != null && 
        request.auth.token.admin == true;
    }
  }
}
```

### Storage Rules
Copy and paste these rules in the Firebase Console > Storage > Rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // User profile images
    match /profiles/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Challenge-related images
    match /challenges/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ML model files (read-only for authenticated users)
    match /ml_models/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if false; // Only admins can upload models
    }
    
    // Analytics exports (admin only)
    match /analytics/{allPaths=**} {
      allow read, write: if request.auth != null && 
        request.auth.token.admin == true;
    }
    
    // General user content
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 📱 App Configuration

### 1. Download google-services.json
- In Firebase Console, go to Project Settings
- Download `google-services.json`
- Place it in the `app/` directory of your Android project

### 2. Add API Keys to local.properties
```properties
# Pexels API for images
PEXELS_API_KEY=your_pexels_api_key_here

# Optional: Add other API keys here
# MAPS_API_KEY=your_maps_api_key_here
```

### 3. Firestore Indexes
The app may require composite indexes. When you run the app and see index errors in the logs:
1. Click the provided link in the error message
2. Firebase will automatically create the required indexes
3. Wait for indexes to build (usually takes a few minutes)

## 🤖 ML Model Setup (Optional)

For full AI functionality, place TensorFlow Lite models in `app/src/main/assets/`:
- `challenge_recommendation_model.tflite`
- `difficulty_prediction_model.tflite`
- `reward_optimization_model.tflite`

If models are not available, the app will use fallback rule-based algorithms.

## 🧪 Testing Setup

### Test Data Collections
For testing, you can create sample data in these collections:
- `challenge_templates`: Sample challenge templates
- `global_challenge_stats`: Global statistics for testing
- `players`: Test player profiles

### Development Mode
During development, you can temporarily use more permissive rules:
```javascript
// DEVELOPMENT ONLY - DO NOT USE IN PRODUCTION
match /{document=**} {
  allow read, write: if request.auth != null;
}
```

## 🚀 Production Deployment

### Security Checklist
- [ ] Apply production Firestore rules
- [ ] Apply production Storage rules
- [ ] Enable App Check for additional security
- [ ] Set up monitoring and alerts
- [ ] Configure backup and recovery
- [ ] Review and audit user permissions

### Performance Optimization
- [ ] Create necessary Firestore indexes
- [ ] Enable offline persistence
- [ ] Implement proper caching strategies
- [ ] Monitor read/write operations
- [ ] Optimize query patterns

## 📊 Monitoring

### Firebase Console Monitoring
- **Authentication**: Monitor user sign-ups and activity
- **Firestore**: Track read/write operations and performance
- **Storage**: Monitor file uploads and downloads
- **Analytics**: Track user behavior and app usage

### Custom Analytics
The app includes custom analytics for:
- Challenge completion rates
- AI model performance
- User engagement metrics
- Behavior pattern analysis

## 🆘 Troubleshooting

### Common Issues
1. **Index Errors**: Click the provided link to create indexes
2. **Permission Denied**: Check Firestore rules and user authentication
3. **Quota Exceeded**: Monitor usage in Firebase Console
4. **Model Loading Errors**: Ensure TensorFlow Lite models are in assets folder

### Support Resources
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [TensorFlow Lite Android](https://www.tensorflow.org/lite/android)

---

**Ready to set sail with Firebase! 🏴‍☠️🔥**
