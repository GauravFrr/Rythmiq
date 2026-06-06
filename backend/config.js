module.exports = {
    // JioSaavn API Instances (We rotate them if one fails)
    JIOSAAVN_URLS: [
        'https://saavn.me',
        'https://jiosaavn-api-tau.vercel.app',
        'https://jiosaavn-api-sigma-six.vercel.app',
        'https://jiosaavn-api-v3.vercel.app'
    ],
    
    // Genius API Key (Get yours at https://genius.com/api-clients)
    GENIUS_API_KEY: process.env.GENIUS_API_KEY || 'YOUR_GENIUS_API_KEY_HERE',
    
    // Last.fm API Keys (Get yours at https://www.last.fm/api/account/create)
    LASTFM_API_KEY: process.env.LASTFM_API_KEY || 'YOUR_LASTFM_API_KEY_HERE',
    LASTFM_SECRET: process.env.LASTFM_SECRET || 'YOUR_LASTFM_SECRET_HERE'
};
