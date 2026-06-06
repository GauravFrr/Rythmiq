const express = require('express');
const router = express.Router();
const JioSaavnService = require('../services/jiosaavn');

/**
 * GET /api/music/search?q=query
 * Searches JioSaavn and returns metadata and direct audio streams.
 */
router.get('/search', async (req, res) => {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Query parameter "q" is required' });

    try {
        const results = await JioSaavnService.searchSongs(q);
        console.log(`🔎 SEARCH: "${q}" -> Found ${results.length} tracks`);
        res.json(results);
    } catch (error) {
        res.status(500).json({ error: 'Failed to search music' });
    }
});

/**
 * GET /api/music/stream?q=searchKey
 * Backward compatibility: Returns the direct stream URL for a given song query.
 */
router.get('/stream', async (req, res) => {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Query parameter "q" is required' });

    try {
        // Find the song via JioSaavn and return its stream URL
        const results = await JioSaavnService.searchSongs(q);
        if (results && results.length > 0 && results[0].audioUrl) {
            res.json({ streamUrl: results[0].audioUrl, videoId: results[0].id });
        } else {
            res.status(404).json({ error: 'Failed to get stream URL' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to get stream URL' });
    }
});
router.get('/searchAll', async (req, res) => {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Query parameter "q" is required' });

    try {
        const results = await JioSaavnService.searchAll(q);
        res.json(results);
    } catch (error) {
        res.status(500).json({ error: 'Failed to search all' });
    }
});

router.get('/artist', async (req, res) => {
    const { name } = req.query;
    if (!name) return res.status(400).json({ error: 'Query parameter "name" is required' });
    try {
        const tracks = await JioSaavnService.getArtistTopSongs(name);
        res.json(tracks);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch artist songs' });
    }
});

router.get('/album', async (req, res) => {
    const { id } = req.query;
    if (!id) return res.status(400).json({ error: 'Query parameter "id" is required' });
    try {
        const tracks = await JioSaavnService.getAlbumDetails(id);
        res.json(tracks);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch album' });
    }
});

router.get('/playlist', async (req, res) => {
    const { id } = req.query;
    if (!id) return res.status(400).json({ error: 'Query parameter "id" is required' });
    try {
        const tracks = await JioSaavnService.getPlaylistDetails(id);
        res.json(tracks);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch playlist' });
    }
});

router.get('/recommend', async (req, res) => {
    const { id } = req.query;
    if (!id) return res.status(400).json({ error: 'Query parameter "id" is required' });
    try {
        const tracks = await JioSaavnService.getRecommendedSongs(id);
        res.json(tracks);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch recommendations' });
    }
});

module.exports = router;
