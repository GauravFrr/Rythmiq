-- Run this in your Supabase SQL Editor to create the necessary tables for the app

CREATE TABLE IF NOT EXISTS public.users (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    firebase_uid TEXT UNIQUE NOT NULL,
    email TEXT,
    phone TEXT,
    name TEXT,
    username TEXT,
    photo_url TEXT,
    login_method TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.recently_played (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    song_name TEXT NOT NULL,
    artist_name TEXT NOT NULL,
    played_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.liked_songs (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    song_name TEXT NOT NULL,
    artist_name TEXT NOT NULL,
    image_url TEXT,
    liked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, song_id)
);

CREATE TABLE IF NOT EXISTS public.playlists (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE,
    name TEXT NOT NULL,
    cover_url TEXT,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.playlist_songs (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    playlist_id UUID REFERENCES public.playlists(id) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    song_name TEXT NOT NULL,
    song_image TEXT,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(playlist_id, song_id)
);

CREATE TABLE IF NOT EXISTS public.search_history (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE,
    query TEXT NOT NULL,
    searched_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Advanced Recommendation System Tables

CREATE TABLE IF NOT EXISTS public.play_events (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    song_name TEXT NOT NULL,
    artist_id TEXT,
    artist_name TEXT NOT NULL,
    album_id TEXT,
    genre TEXT,
    language TEXT,
    duration INT,
    listened_duration INT,
    completion_rate FLOAT,
    skipped BOOLEAN DEFAULT false,
    liked BOOLEAN DEFAULT false,
    added_to_playlist BOOLEAN DEFAULT false,
    hour_of_day INT,
    day_of_week INT,
    source TEXT,
    played_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_taste_profile (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id TEXT REFERENCES public.users(firebase_uid) ON DELETE CASCADE UNIQUE,
    top_genres JSONB DEFAULT '{}'::jsonb,
    top_artists JSONB DEFAULT '{}'::jsonb,
    top_languages JSONB DEFAULT '{}'::jsonb,
    active_hours JSONB DEFAULT '{}'::jsonb,
    avg_completion FLOAT DEFAULT 0,
    skip_rate FLOAT DEFAULT 0,
    total_plays INT DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
