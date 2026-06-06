const express = require('express');
const router = express.Router();
const { supabase } = require('../services/db');
const { authMiddleware } = require('../middleware/authMiddleware');

// ── POST /api/history/log ──────────────────────────────────────────────────
// Called by the Android app every time a user plays a song
router.post('/log', authMiddleware, async (req, res) => {
    const { trackId, title, artist } = req.body;
    const uid = req.uid;

    if (!trackId || !title || !artist) {
        return res.status(400).json({ error: 'trackId, title, and artist are required.' });
    }

    try {
        const { error } = await supabase
            .from('recently_played')
            .insert([{
                user_id: uid,
                song_id: trackId,
                song_name: title,
                artist_name: artist
            }]);

        if (error) {
            console.error('Supabase insert error (history):', error);
            return res.status(500).json({ error: 'Failed to log event.' });
        }

        res.json({ success: true, message: 'Play event logged.' });
    } catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
});

// ── GET /api/history/me ────────────────────────────────────────────────────
// Returns user's full listening history
router.get('/me', authMiddleware, async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 20;
        const uid = req.uid;

        const { data: history, error } = await supabase
            .from('recently_played')
            .select('*')
            .eq('user_id', uid)
            .order('played_at', { ascending: false })
            .limit(limit);

        if (error) {
            console.error('Supabase fetch error (history):', error);
            return res.status(500).json({ error: 'Failed to fetch history.' });
        }

        res.json(history);
    } catch (e) {
        res.status(500).json({ error: 'Internal server error.' });
    }
});

// ── DELETE /api/history/clear ──────────────────────────────────────────────
router.delete('/clear', authMiddleware, async (req, res) => {
    try {
        const uid = req.uid;
        const { error } = await supabase
            .from('recently_played')
            .delete()
            .eq('user_id', uid);

        if (error) {
            console.error('Supabase delete error (history):', error);
            return res.status(500).json({ error: 'Failed to clear history.' });
        }

        res.json({ success: true, message: 'History cleared.' });
    } catch (e) {
        res.status(500).json({ error: 'Internal server error.' });
    }
});

module.exports = router;
