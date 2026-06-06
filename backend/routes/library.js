const express = require('express');
const router = express.Router();
const { supabase } = require('../services/db');
const { authMiddleware } = require('../middleware/authMiddleware');

router.use(authMiddleware);

// ── LIKED SONGS ─────────────────────────────────────────────────────────────
router.get('/likes', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('liked_songs')
            .select('*')
            .eq('user_id', req.uid)
            .order('liked_at', { ascending: false });

        if (error) throw error;
        res.json(data);
    } catch (e) {
        res.status(500).json({ error: 'Failed to fetch liked songs' });
    }
});

router.post('/likes', async (req, res) => {
    const { songId, songName, artistName, imageUrl } = req.body;
    try {
        const { error } = await supabase
            .from('liked_songs')
            .insert([{
                user_id: req.uid,
                song_id: songId,
                song_name: songName,
                artist_name: artistName,
                image_url: imageUrl
            }]);

        if (error) throw error;
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: 'Failed to add liked song' });
    }
});

router.delete('/likes/:songId', async (req, res) => {
    try {
        const { error } = await supabase
            .from('liked_songs')
            .delete()
            .eq('user_id', req.uid)
            .eq('song_id', req.params.songId);

        if (error) throw error;
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: 'Failed to remove liked song' });
    }
});

// ── PLAYLISTS ───────────────────────────────────────────────────────────────
router.get('/playlists', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('playlists')
            .select('*, playlist_songs(*)')
            .eq('user_id', req.uid)
            .order('created_at', { ascending: false });

        if (error) throw error;
        res.json(data);
    } catch (e) {
        res.status(500).json({ error: 'Failed to fetch playlists' });
    }
});

router.post('/playlists', async (req, res) => {
    const { name, coverUrl, isPublic } = req.body;
    try {
        const { data, error } = await supabase
            .from('playlists')
            .insert([{
                user_id: req.uid,
                name,
                cover_url: coverUrl,
                is_public: isPublic || false
            }])
            .select()
            .single();

        if (error) throw error;
        res.json(data);
    } catch (e) {
        res.status(500).json({ error: 'Failed to create playlist' });
    }
});

router.post('/playlists/:playlistId/songs', async (req, res) => {
    const { songId, songName, songImage } = req.body;
    const { playlistId } = req.params;
    try {
        const { error } = await supabase
            .from('playlist_songs')
            .insert([{
                playlist_id: playlistId,
                song_id: songId,
                song_name: songName,
                song_image: songImage
            }]);

        if (error) throw error;
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: 'Failed to add song to playlist' });
    }
});

// ── SEARCH HISTORY ──────────────────────────────────────────────────────────
router.get('/search-history', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('search_history')
            .select('*')
            .eq('user_id', req.uid)
            .order('searched_at', { ascending: false })
            .limit(10);

        if (error) throw error;
        res.json(data);
    } catch (e) {
        res.status(500).json({ error: 'Failed to fetch search history' });
    }
});

router.post('/search-history', async (req, res) => {
    const { query } = req.body;
    try {
        const { error } = await supabase
            .from('search_history')
            .insert([{
                user_id: req.uid,
                query: query
            }]);

        if (error) throw error;
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: 'Failed to save search history' });
    }
});

module.exports = router;
