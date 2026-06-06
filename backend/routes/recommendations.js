const express = require('express');
const router = express.Router();
const { supabase } = require('../services/db');
const JioSaavnService = require('../services/jiosaavn');
const LastFMService = require('../services/lastfm');
const { optionalAuthMiddleware } = require('../middleware/authMiddleware');

// ── Constants ──────────────────────────────────────────────────────────────────
const BLOCKED_LANGUAGES = new Set(['bhojpuri']);
const RECENTLY_PLAYED_DAYS = 7;     // Exclude songs played in last 7 days
const MAX_SONGS_PER_ARTIST = 3;     // Diversity cap per artist in final list
const SKIP_THRESHOLD = 0.3;         // completion < 30% = skip/hate

// ── Helpers ───────────────────────────────────────────────────────────────────
function parseArtists(artistStr) {
    return (artistStr || '').split(',').map(a => a.trim().toLowerCase()).filter(Boolean);
}

// Weighted random selection — higher scored items have higher probability
function weightedPickRandom(arr, n) {
    const result = [];
    const pool = [...arr];
    while (result.length < n && pool.length > 0) {
        const totalWeight = pool.reduce((sum, item) => sum + (item._score || 0.1), 0);
        let rand = Math.random() * totalWeight;
        let idx = 0;
        for (let i = 0; i < pool.length; i++) {
            rand -= (pool[i]._score || 0.1);
            if (rand <= 0) { idx = i; break; }
        }
        result.push(pool.splice(idx, 1)[0]);
    }
    return result;
}

// ── GET /api/recommendations ────────────────────────────────────────────────
// 5-Layer Advanced Taste-Based Recommendation Engine
router.get('/', optionalAuthMiddleware, async (req, res) => {
    try {
        const userId = req.uid || req.query.userId;
        const limit = parseInt(req.query.limit) || 15;
        const excludeSet = new Set();

        if (req.query.excludeIds) {
            try {
                JSON.parse(req.query.excludeIds).forEach(id => excludeSet.add(id));
            } catch {
                req.query.excludeIds.split(',').filter(Boolean).forEach(id => excludeSet.add(id));
            }
        }

        // ── Layer 0: Parallel Data Collection ──────────────────────────────
        let profile = null;
        let followedArtists = [];
        let recentSongIds = new Set();
        let lovedSongIds = [];
        let skippedArtists = new Set();     // Artists user has skipped often
        let dislikedArtists = new Set();    // Artists with avg completion < 0.3

        if (userId) {
            const cutoff = new Date(Date.now() - RECENTLY_PLAYED_DAYS * 24 * 60 * 60 * 1000).toISOString();

            const [tasteRes, followsRes, recentRes, lovedRes, skippedRes] = await Promise.all([
                // Taste profile
                supabase.from('user_taste_profile').select('*').eq('user_id', userId).single(),

                // Followed artists (strongest signal)
                supabase.from('user_follows').select('artist_name').eq('user_id', userId),

                // Recently played song IDs → exclude from feed
                supabase.from('play_events').select('song_id').eq('user_id', userId)
                    .gte('played_at', cutoff).limit(300),

                // Loved songs (completion ≥ 0.8 or liked) → feed into JioSaavn's reco API
                supabase.from('play_events').select('song_id').eq('user_id', userId)
                    .or('completion_rate.gte.0.8,liked.eq.true')
                    .order('played_at', { ascending: false }).limit(10),

                // Skipped artists → penalize them in scoring
                supabase.from('play_events').select('artist_name, completion_rate, skipped')
                    .eq('user_id', userId).eq('skipped', true)
                    .order('played_at', { ascending: false }).limit(100)
            ]);

            if (tasteRes.data) profile = tasteRes.data;
            if (followsRes.data) followedArtists = followsRes.data.map(f => f.artist_name);
            if (recentRes.data) recentRes.data.forEach(p => recentSongIds.add(p.song_id));
            if (lovedRes.data) lovedSongIds = lovedRes.data.map(p => p.song_id);

            // Build skipped/disliked artist sets
            if (skippedRes.data) {
                const artistSkipCount = {};
                skippedRes.data.forEach(p => {
                    if (p.artist_name) {
                        p.artist_name.split(',').forEach(a => {
                            const name = a.trim().toLowerCase();
                            artistSkipCount[name] = (artistSkipCount[name] || 0) + 1;
                        });
                    }
                });
                Object.entries(artistSkipCount).forEach(([artist, count]) => {
                    if (count >= 3) skippedArtists.add(artist);  // skipped 3+ times → penalize
                    if (count >= 6) dislikedArtists.add(artist); // skipped 6+ times → exclude
                });
            }
        }

        // Add recently played to exclude set
        recentSongIds.forEach(id => excludeSet.add(id));

        const isNewOrGuest = !profile || (profile.total_plays || 0) < 5;
        const currentHour = new Date().getHours();

        // ── Layer 1: Build Taste Signals ───────────────────────────────────
        const topArtists = Object.entries(profile?.top_artists || {})
            .sort(([, a], [, b]) => b - a).map(([name]) => name);
        const topGenres = Object.entries(profile?.top_genres || {})
            .sort(([, a], [, b]) => b - a).map(([name]) => name);
        const topLanguages = Object.entries(profile?.top_languages || {})
            .filter(([lang]) => !BLOCKED_LANGUAGES.has(lang.toLowerCase()))
            .sort(([, a], [, b]) => b - a).map(([name]) => name);

        // Active hours: what time does user usually listen?
        const activeHours = Object.entries(profile?.active_hours || {})
            .sort(([, a], [, b]) => b - a).map(([h]) => parseInt(h));
        const isActiveHour = activeHours.slice(0, 3).includes(currentHour);
        const skipRate = profile?.skip_rate || 0;

        // ── Layer 2: Get Last.fm Similar Artists (Discovery) ───────────────
        let similarArtists = [];
        if (topArtists.length > 0 && !isNewOrGuest) {
            try {
                // Use top artist to get similar ones from Last.fm
                const similar = await LastFMService.getSimilarArtists(topArtists[0]);
                similarArtists = similar
                    .map(a => a.name)
                    .filter(name => {
                        const nameLower = name.toLowerCase();
                        // Only include similar artists the user hasn't already heard too much
                        return !dislikedArtists.has(nameLower);
                    })
                    .slice(0, 4);
                console.log(`🎵 Last.fm similar to "${topArtists[0]}":`, similarArtists.join(', '));
            } catch (e) {
                // Last.fm optional — continue without it
            }
        }

        // ── Layer 3: Build Weighted Search Query Buckets ───────────────────
        let searchBuckets = [];

        if (isNewOrGuest) {
            // Cold start: serve popular Hindi/Punjabi music
            searchBuckets = [
                { query: 'Arijit Singh hits', weight: 15 },
                { query: 'Jubin Nautiyal songs', weight: 12 },
                { query: 'Punjabi Hits 2024', weight: 10 },
                { query: 'Bollywood romantic songs', weight: 10 },
                { query: 'Shreya Ghoshal songs', weight: 8 },
                { query: 'latest Hindi songs 2024', weight: 7 },
                { query: 'Diljit Dosanjh songs', weight: 7 },
                { query: 'Neha Kakkar songs', weight: 6 },
            ];
        } else {
            // ── Bucket A: Followed artists (weight 20 — user explicitly follows these) ──
            followedArtists.forEach(artist => {
                searchBuckets.push({ query: artist, weight: 20 });
                searchBuckets.push({ query: `${artist} new songs`, weight: 17 });
            });

            // ── Bucket B: Top listened artists (weight 15-7) ──
            topArtists.slice(0, 5).forEach((artist, i) => {
                searchBuckets.push({ query: artist, weight: 15 - i * 2 });
            });

            // ── Bucket C: Artist + Genre combos (within comfort zone) ──
            if (topArtists.length > 0 && topGenres.length > 0) {
                searchBuckets.push({ query: `${topArtists[0]} ${topGenres[0]}`, weight: 13 });
            }
            if (topArtists.length > 1 && topGenres.length > 0) {
                searchBuckets.push({ query: `${topArtists[1]} ${topGenres[0]}`, weight: 11 });
            }

            // ── Bucket D: Last.fm similar artists (Discovery Layer) ──
            similarArtists.forEach((artist, i) => {
                searchBuckets.push({ query: artist, weight: 9 - i }); // weight 9,8,7,6
            });

            // ── Bucket E: Language + Genre combos ──
            if (topLanguages.length > 0 && topGenres.length > 0) {
                searchBuckets.push({
                    query: `${topLanguages[0]} ${topGenres[0]} songs`,
                    weight: 8
                });
            }

            // ── Bucket F: Context-aware (time of day) — ONLY genre/language user likes ──
            // Context uses the user's own active_hours data, not generic "lofi chill"
            if (topArtists.length > 0) {
                const isLateNight = currentHour >= 22 || currentHour <= 4;
                const isMorning = currentHour >= 6 && currentHour <= 10;
                const isWeekend = [0, 6].includes(new Date().getDay()); // Sun=0, Sat=6

                if (isLateNight && isActiveHour) {
                    // Late night + it's their usual listening time → their fav artist + a slow genre
                    const slowGenre = topGenres.find(g => ['romantic', 'sad', 'lofi', 'slow'].some(s => g.toLowerCase().includes(s)));
                    if (slowGenre) {
                        searchBuckets.push({ query: `${topArtists[0]} ${slowGenre}`, weight: 10 });
                    }
                } else if (isMorning) {
                    searchBuckets.push({ query: `${topArtists[0]} upbeat`, weight: 7 });
                } else if (isWeekend && topLanguages.length > 0) {
                    searchBuckets.push({ query: `${topLanguages[0]} party songs`, weight: 7 });
                }
            }

            if (searchBuckets.length === 0) {
                searchBuckets.push({ query: 'Hindi Top Songs', weight: 5 });
            }
        }

        // ── Layer 4: Fetch Candidates in Parallel ──────────────────────────
        const uniqueBuckets = [...new Map(searchBuckets.map(b => [b.query, b])).values()]
            .sort((a, b) => b.weight - a.weight)
            .slice(0, 8); // Cap at 8 parallel queries

        // Also get JioSaavn's native reco for songs user loved
        const jioRecoPromises = lovedSongIds.slice(0, 2).map(songId =>
            JioSaavnService.getRecommendedSongs(songId).catch(() => [])
        );

        const [searchResults, ...jioRecos] = await Promise.all([
            Promise.all(
                uniqueBuckets.map(b =>
                    JioSaavnService.searchSongs(b.query)
                        .then(songs => songs.map(s => ({ ...s, _sourceWeight: b.weight })))
                        .catch(() => [])
                )
            ),
            ...jioRecoPromises
        ]);

        const allCandidates = [
            ...searchResults.flat(),
            ...jioRecos.flat().map(s => ({ ...s, _sourceWeight: 18 })) // Native reco highest base
        ];

        console.log(`📥 Got ${allCandidates.length} raw candidates from ${uniqueBuckets.length} queries + ${jioRecos.length} JioSaavn recos`);

        // ── Layer 5: Multi-Signal Scoring ──────────────────────────────────
        const topArtistSet = new Set(topArtists.map(a => a.toLowerCase()));
        const followedArtistSet = new Set(followedArtists.map(a => a.toLowerCase()));
        const topLangSet = new Set(topLanguages.map(l => l.toLowerCase()));

        const scoredMap = new Map();

        for (const song of allCandidates) {
            if (!song?.id) continue;
            if (excludeSet.has(song.id)) continue; // Already played recently

            const songLang = (song.language || '').toLowerCase();
            if (BLOCKED_LANGUAGES.has(songLang)) continue; // Block English/Bhojpuri

            const songArtists = parseArtists(song.artist);

            // Hard exclude: artists user has skipped 6+ times
            if (songArtists.some(a => dislikedArtists.has(a))) continue;

            let score = 0;

            if (!isNewOrGuest && profile) {
                // ── Signal 1: Followed artist (+0.40) — strongest signal ──
                if (songArtists.some(a => followedArtistSet.has(a))) score += 0.40;

                // ── Signal 2: Top listened artist (+0.30) ──
                if (songArtists.some(a => topArtistSet.has(a))) score += 0.30;

                // ── Signal 3: Language match (+0.15) ──
                if (songLang && songLang !== 'unknown' && topLangSet.has(songLang)) score += 0.15;

                // ── Signal 4: Source query weight bonus (up to +0.10) ──
                score += (song._sourceWeight || 5) / 200;

                // ── Signal 5: Freshness — new release this year (+0.05) ──
                if (song.year && parseInt(song.year) >= new Date().getFullYear()) score += 0.05;

                // ── Signal 6: Global trending (+0.05 if high play count) ──
                if (song.play_count && parseInt(song.play_count) > 1000000) score += 0.05;

                // ── Signal 7: Liked similar song boost (+0.08) ──
                // If this song came from a JioSaavn reco (based on a loved song), boost it
                if (song._sourceWeight === 18) score += 0.08;

                // ── Penalty: Skipped artist (-0.15) ──
                if (songArtists.some(a => skippedArtists.has(a))) score -= 0.15;

                // ── Penalty: Picky user with no match gets heavy reduction ──
                if (score < 0.05 && skipRate > 0.5) {
                    score = 0.01; // Picky user — only show taste-matched songs
                } else if (score < 0.05) {
                    score = Math.random() * 0.04; // Fill gaps with tiny random score
                }

            } else {
                // Guest/new user: source weight + random for variety
                score = (song._sourceWeight || 5) / 20 + Math.random() * 0.2;
            }

            // Keep best score if duplicate
            const existing = scoredMap.get(song.id);
            if (!existing || existing._score < score) {
                scoredMap.set(song.id, { ...song, _score: score, recommendation_score: score });
            }
        }

        // ── Layer 6: Diversity + Weighted Final Selection ──────────────────
        const sorted = Array.from(scoredMap.values())
            .sort((a, b) => b._score - a._score);

        // Enforce diversity: max MAX_SONGS_PER_ARTIST per artist
        const artistCountMap = new Map();
        const finalList = [];

        for (const track of sorted) {
            if (finalList.length >= limit) break;
            const primaryArtist = parseArtists(track.artist)[0] || 'unknown';
            const count = artistCountMap.get(primaryArtist) || 0;
            if (count < MAX_SONGS_PER_ARTIST) {
                const { _score, _sourceWeight, ...cleanTrack } = track;
                finalList.push(cleanTrack);
                artistCountMap.set(primaryArtist, count + 1);
            }
        }

        // Backfill if not enough after diversity cap
        if (finalList.length < limit) {
            for (const track of sorted) {
                if (finalList.length >= limit) break;
                if (!finalList.find(t => t.id === track.id)) {
                    const { _score, _sourceWeight, ...cleanTrack } = track;
                    finalList.push(cleanTrack);
                }
            }
        }

        const topArtistNames = [...new Set(finalList.slice(0, 5).map(t => t.artist))];
        console.log(`✅ Returning ${finalList.length} recs. Top artists: ${topArtistNames.join(' | ')}`);

        res.json(finalList);
    } catch (error) {
        console.error('Recommendations Error:', error);
        res.status(500).json({ error: 'Failed to fetch recommendations' });
    }
});

module.exports = router;
