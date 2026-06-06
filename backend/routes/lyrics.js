const express = require('express');
const router = express.Router();
const GeniusService = require('../services/genius');

/**
 * GET /api/lyrics?title=SongTitle&artist=ArtistName
 */
router.get('/', async (req, res) => {
    const { title, artist } = req.query;
    if (!title) return res.status(400).json({ error: 'Title is required' });

    try {
        const lyrics = await GeniusService.fetchLyrics(title, artist || '');
        res.json({ lyrics });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch lyrics' });
    }
});

module.exports = router;
