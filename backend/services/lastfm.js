const LastFM = require('lastfmapi');
const config = require('../config');

const lfm = new LastFM({
    api_key: config.LASTFM_API_KEY,
    secret: config.LASTFM_SECRET
});

const LastFMService = {
    getSimilarArtists: (artistName) => {
        return new Promise((resolve, reject) => {
            lfm.artist.getSimilar({
                artist: artistName,
                limit: 10
            }, (err, similar) => {
                if (err) return resolve([]);
                resolve(similar.artist.map(a => ({
                    name: a.name,
                    url: a.url,
                    image: a.image?.[2]?.['#text'] || ''
                })));
            });
        });
    },

    getArtistInfo: (artistName) => {
        return new Promise((resolve, reject) => {
            lfm.artist.getInfo({
                artist: artistName
            }, (err, info) => {
                if (err) return resolve(null);
                resolve({
                    name: info.name,
                    bio: info.bio?.summary || '',
                    tags: info.tags?.tag?.map(t => t.name) || []
                });
            });
        });
    }
};

module.exports = LastFMService;
