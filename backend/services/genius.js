const { getLyrics, getSong } = require('genius-lyrics-api');
const config = require('../config');

const GeniusService = {
    fetchLyrics: async (title, artist) => {
        try {
            const options = {
                apiKey: config.GENIUS_API_KEY,
                title: title,
                artist: artist,
                optimizeQuery: true
            };

            const lyrics = await getLyrics(options);
            return lyrics || "Lyrics not available for this track.";
        } catch (error) {
            console.error('Genius lyrics error:', error);
            return "Failed to fetch lyrics.";
        }
    }
};

module.exports = GeniusService;
