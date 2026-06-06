const express = require('express');
const router = express.Router();
const LastFMService = require('../services/lastfm');
const db = require('../services/db');
const { authMiddleware } = require('../middleware/authMiddleware');

/**
 * GET /api/artist/similar?name=ArtistName
 */
router.get('/similar', async (req, res) => {
    const { name } = req.query;
    if (!name) return res.status(400).json({ error: 'Artist name is required' });
    try {
        const artists = await LastFMService.getSimilarArtists(name);
        res.json(artists);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch similar artists' });
    }
});

/**
 * GET /api/artist/info?name=ArtistName
 */
router.get('/info', async (req, res) => {
    const { name } = req.query;
    if (!name) return res.status(400).json({ error: 'Artist name is required' });
    try {
        const info = await LastFMService.getArtistInfo(name);
        res.json(info);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch artist info' });
    }
});

// ── POST /api/artist/follow ──────────────────────────────────────────────────
router.post('/follow', authMiddleware, async (req, res) => {
    const { artistId, artistName, imageUrl } = req.body;
    if (!artistId || !artistName) return res.status(400).json({ error: 'artistId and artistName required' });
    try {
        await db.asyncRun(
            `INSERT OR IGNORE INTO followed_artists (user_id, artist_id, artist_name, image_url)
             VALUES (?, ?, ?, ?)`,
            [req.uid, artistId, artistName, imageUrl || '']
        );
        console.log(`💚 User ${req.uid} followed "${artistName}"`);
        res.json({ success: true, following: true });
    } catch (e) {
        res.status(500).json({ error: 'Failed to follow artist' });
    }
});

// ── DELETE /api/artist/follow ────────────────────────────────────────────────
router.delete('/follow', authMiddleware, async (req, res) => {
    const { artistId } = req.body;
    if (!artistId) return res.status(400).json({ error: 'artistId required' });
    try {
        await db.asyncRun(
            `DELETE FROM followed_artists WHERE user_id = ? AND artist_id = ?`,
            [req.uid, artistId]
        );
        res.json({ success: true, following: false });
    } catch (e) {
        res.status(500).json({ error: 'Failed to unfollow artist' });
    }
});

// ── GET /api/artist/following ────────────────────────────────────────────────
router.get('/following', authMiddleware, async (req, res) => {
    try {
        const artists = await db.asyncAll(
            `SELECT artist_id, artist_name, image_url, followed_at
             FROM followed_artists WHERE user_id = ? ORDER BY followed_at DESC`,
            [req.uid]
        );
        res.json(artists);
    } catch (e) {
        res.status(500).json({ error: 'Failed to fetch following list' });
    }
});

// ── GET /api/artist/is-following?artistId=X ─────────────────────────────────
router.get('/is-following', authMiddleware, async (req, res) => {
    const { artistId } = req.query;
    if (!artistId) return res.status(400).json({ error: 'artistId required' });
    try {
        const row = await db.asyncGet(
            `SELECT 1 FROM followed_artists WHERE user_id = ? AND artist_id = ?`,
            [req.uid, artistId]
        );
        res.json({ following: !!row });
    } catch (e) {
        res.status(500).json({ error: 'Failed to check follow status' });
    }
});

module.exports = router;
