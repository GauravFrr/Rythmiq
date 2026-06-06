const express = require('express');
const router = express.Router();
const { supabase } = require('../services/db');
const { authMiddleware } = require('../middleware/authMiddleware');

// ── POST /api/plays/log ──────────────────────────────────────────────────
// Log a play event and update taste profile asynchronously
router.post('/log', authMiddleware, async (req, res) => {
    try {
        const userId = req.uid; // From authMiddleware
        const {
            songId,
            songName,
            artistId,
            artistName,
            albumId,
            genre,
            language,
            duration,
            listenedDuration,
            skipped,
            liked,
            addedToPlaylist,
            hourOfDay,
            dayOfWeek,
            source
        } = req.body;

        if (!songId || !songName || !artistName) {
            return res.status(400).json({ error: 'Missing required fields' });
        }

        // Calculate completion rate
        let completionRate = 0;
        if (duration && duration > 0 && listenedDuration) {
            completionRate = listenedDuration / duration;
            if (completionRate > 1) completionRate = 1;
        }

        // 1. Insert into play_events
        const { error: insertError } = await supabase
            .from('play_events')
            .insert([{
                user_id: userId,
                song_id: songId,
                song_name: songName,
                artist_id: artistId,
                artist_name: artistName,
                album_id: albumId,
                genre: genre || 'unknown',
                language: language || 'unknown',
                duration: duration || 0,
                listened_duration: listenedDuration || 0,
                completion_rate: completionRate,
                skipped: skipped || false,
                liked: liked || false,
                added_to_playlist: addedToPlaylist || false,
                hour_of_day: hourOfDay != null ? hourOfDay : new Date().getHours(),
                day_of_week: dayOfWeek != null ? dayOfWeek : new Date().getDay(),
                source: source || 'unknown',
                played_at: new Date().toISOString()
            }]);

        if (insertError) {
            console.error('Error inserting play_event:', insertError);
            return res.status(500).json({ error: 'Failed to log play event' });
        }

        // 2. Asynchronously update the user_taste_profile
        // We don't await this so the API responds quickly
        updateTasteProfile(userId).catch(err => {
            console.error('Error updating taste profile in background:', err);
        });

        res.status(200).json({ success: true, completionRate });
    } catch (error) {
        console.error('Error logging play:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Helper to recalculate user_taste_profile
async function updateTasteProfile(userId) {
    // 1. Fetch last 100 play events for this user to rebuild their profile
    const { data: plays, error } = await supabase
        .from('play_events')
        .select('*')
        .eq('user_id', userId)
        .order('played_at', { ascending: false })
        .limit(100);

    if (error || !plays || plays.length === 0) return;

    let totalPlays = plays.length;
    let totalSkips = 0;
    let totalCompletion = 0;
    
    let genreCounts = {};
    let artistCounts = {};
    let langCounts = {};
    let hourCounts = {};

    plays.forEach(play => {
        if (play.skipped) totalSkips++;
        totalCompletion += play.completion_rate || 0;

        // Weights based on user logic:
        // completion > 0.8 -> high weight (1.5)
        // liked -> high weight (1.5)
        // skipped -> low weight (0.2)
        let weight = 1.0;
        if (play.completion_rate >= 0.8) weight += 0.5;
        if (play.liked) weight += 0.5;
        if (play.skipped || play.completion_rate < 0.3) weight = 0.2;

        // Artists
        if (play.artist_name) {
            play.artist_name.split(',').forEach(a => {
                const name = a.trim();
                if (name) artistCounts[name] = (artistCounts[name] || 0) + weight;
            });
        }
        
        // Genres
        if (play.genre) {
            play.genre.split(',').forEach(g => {
                const name = g.trim();
                if (name && name !== 'unknown') genreCounts[name] = (genreCounts[name] || 0) + weight;
            });
        }

        // Languages
        if (play.language && play.language !== 'unknown') {
            langCounts[play.language] = (langCounts[play.language] || 0) + weight;
        }

        // Active Hours
        if (play.hour_of_day != null) {
            hourCounts[play.hour_of_day] = (hourCounts[play.hour_of_day] || 0) + 1; // raw count for hours
        }
    });

    // Sort and take top N
    const getTop = (counts, limit) => {
        return Object.entries(counts)
            .sort(([, a], [, b]) => b - a)
            .slice(0, limit)
            .reduce((obj, [k, v]) => {
                obj[k] = Math.round(v * 10) / 10;
                return obj;
            }, {});
    };

    const topGenres = getTop(genreCounts, 5);
    const topArtists = getTop(artistCounts, 10);
    const topLanguages = getTop(langCounts, 3);
    const activeHours = getTop(hourCounts, 5); // top 5 most active hours
    
    const avgCompletion = totalPlays > 0 ? (totalCompletion / totalPlays) : 0;
    const skipRate = totalPlays > 0 ? (totalSkips / totalPlays) : 0;

    // Upsert into user_taste_profile
    // Since Supabase requires specifying conflict resolution columns, we do:
    const { error: upsertError } = await supabase
        .from('user_taste_profile')
        .upsert({
            user_id: userId,
            top_genres: topGenres,
            top_artists: topArtists,
            top_languages: topLanguages,
            active_hours: activeHours,
            avg_completion: avgCompletion,
            skip_rate: skipRate,
            total_plays: totalPlays,
            last_updated: new Date().toISOString()
        }, { onConflict: 'user_id' });

    if (upsertError) {
        console.error('Error upserting taste profile:', upsertError);
    } else {
        console.log(`✅ Taste profile updated for user ${userId}`);
    }
}

// ── POST /api/plays/onboard ───────────────────────────────────────────────
// Seed user_taste_profile for a new user
router.post('/onboard', authMiddleware, async (req, res) => {
    try {
        const userId = req.uid;
        const { genres, artists } = req.body;

        const topGenres = {};
        const topArtists = {};

        if (genres && Array.isArray(genres)) {
            genres.forEach(g => topGenres[g] = 5.0); // Seed weight
        }
        if (artists && Array.isArray(artists)) {
            artists.forEach(a => topArtists[a] = 5.0); // Seed weight
        }

        const { error } = await supabase
            .from('user_taste_profile')
            .upsert({
                user_id: userId,
                top_genres: topGenres,
                top_artists: topArtists,
                total_plays: 0,
                last_updated: new Date().toISOString()
            }, { onConflict: 'user_id' });

        if (error) {
            console.error('Error seeding taste profile:', error);
            return res.status(500).json({ error: 'Failed to save preferences' });
        }

        res.status(200).json({ success: true });
    } catch (error) {
        console.error('Error onboard seeding:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
