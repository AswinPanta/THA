# Treasure Hunt Adventure (THA)

A real-time, multiplayer, location-based treasure hunt game for Android, built as a 6th semester BIT project. Pirate-themed with AR, chat, leaderboard, challenges, and more!

---

## 🚩 Project Overview
- **Theme:** Pirates (Jolly Roger)
- **Platform:** Android (Java/Kotlin)
- **Tech:** Firebase (Auth, Firestore), ARCore/SceneView, osmdroid, Glide, Volley, Material Design
- **Goal:** Stand out with a modern, feature-rich, multiplayer mobile app

---

## 🏴‍☠️ Features
- **Onboarding:** Pirate-themed, only on first launch
- **Authentication:** Email/password (Firebase Auth)
- **Map:** Real-time player locations, treasures, and AR markers
- **AR Mode:** Find and capture treasures in AR (3D .glb models)
- **Chat:** Real-time group chat for all players
- **Leaderboard:** See top pirates by treasures captured
- **Challenges:** Daily/weekly tasks for extra rewards
- **Profile:** Avatar/banner from Pexels, stats, captured treasures
- **Stats:** Dashboard for treasures, distance, time, challenges
- **Settings:** Sound toggle
- **Animations:** Pirate boat sailing transition, pulsing AR button, treasure capture
- **Modern UI:** Material Design, pirate palette, custom icons

---

## ⚡ Setup Instructions
1. **Clone the repo:**
   ```sh
   git clone <your-repo-url>
   cd THA
   ```
2. **Open in Android Studio.**
3. **Firebase Setup:**
   - Create a Firebase project.
   - Add an Android app with your package name.
   - Download `google-services.json` and place it in `app/`.
   - In Firebase Console:
     - Enable **Email/Password** in Auth.
     - Create a **Firestore Database** (test mode for demo, or use rules below).
     - (Optional) Enable **Storage** if you want to upload images.
   - Add your Pexels API key to `local.properties`:
     ```
     PEXELS_API_KEY=your_pexels_api_key
     ```
4. **Build and run the app!**

---

## 🔒 Firebase Rules
**Firestore:**
```plaintext
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
**Storage (if used):**
```plaintext
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🧭 Demo Script (Exam-Ready)
1. **Launch the app:**
   - Onboarding appears (pirate ViewPager, only on first launch).
2. **Register a new user:**
   - Email/password signup.
3. **Main map:**
   - See your location, treasures, and other players.
   - Pirate boat sails across the screen on every page change.
4. **AR mode:**
   - Tap the AR button (jollyroger) to find and capture treasures in AR.
5. **Chat:**
   - Open chat, send/receive messages in real time.
6. **Leaderboard:**
   - View top pirates by treasures captured.
7. **Challenges:**
   - See and complete daily/weekly pirate challenges.
8. **Profile:**
   - Avatar/banner from Pexels, stats, captured treasures.
9. **Stats:**
   - Dashboard for treasures, distance, time, challenges.
10. **Settings:**
    - Toggle sound.
11. **Logout and login:**
    - Test authentication flow.

---

## 💡 Notes
- All images are royalty-free (Pexels API).
- All code, UI, and features are original and exam-ready.
- If Firestore asks for an index, just click the link it provides.
- No template tints or icons—everything is pirate-branded!

---

**Good luck, captain! May your project sail to the top of the leaderboard! 🏴‍☠️** 