# 🎵 Rythmiq

A premium Spotify-inspired music streaming Android app with a Node.js backend, built with Jetpack Compose and powered by JioSaavn.

## Features

- 🎧 **Personalized Feed** — Advanced recommendation engine based on your listening history
- 🔍 **Smart Search** — Search millions of songs (Hindi, Punjabi, and more)
- ❤️ **Liked Songs & Playlists** — Save and organize your music
- 🎤 **Lyrics Support** — View lyrics while listening
- 👥 **Artist Following** — Follow your favorite artists
- 🔐 **Authentication** — Email, Phone, and Google Sign-In
- 📱 **Infinite Queue** — Never-ending playback with smart queue preloading

## Tech Stack

### Android App
- **Kotlin** + **Jetpack Compose**
- **Room** (local database)
- **Retrofit** (networking)
- **Firebase Auth** (authentication)
- **ExoPlayer** (media playback)

### Backend (Node.js)
- **Express.js**
- **Supabase** (PostgreSQL database)
- **JioSaavn API** (music data)
- **Last.fm API** (similar artist discovery)
- **Firebase Admin SDK** (token verification)

## Recommendation Engine

Our advanced recommendation system uses a 5-layer scoring model:
1. **Followed Artists** (weight: 0.40)
2. **Top Listened Artists + Last.fm Discovery** (weight: 0.30)
3. **Language & Genre Matching** (weight: 0.15)
4. **Context-Aware Time-of-Day** queries
5. **JioSaavn Native Recommendations** for loved songs

Songs you skip frequently are penalized, and blocked languages are excluded.

## Setup

### Backend
```bash
cd backend
npm install
cp .env.example .env   # fill in your keys
npm start
```

### Android App
1. Open `android-app/` in Android Studio
2. Add your `google-services.json` to `app/`
3. Update `DevApiBaseUrl.kt` with your backend URL
4. Build and run

## Environment Variables (Backend)
```
SUPABASE_URL=
SUPABASE_SERVICE_KEY=
LASTFM_API_KEY=
```
