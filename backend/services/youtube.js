const ytdl = require('@distube/ytdl-core');
const yts = require('yt-search');
const axios = require('axios');

const withTimeout = (promise, ms) => {
    return Promise.race([
        promise,
        new Promise((_, reject) => setTimeout(() => reject(new Error('TIMEOUT')), ms))
    ]);
};

const YouTubeService = {
    getStreamUrl: async (query) => {
        let videoId = query;
        let videoUrl;

        // A YouTube video ID is exactly 11 chars of alphanumeric + _ + -
        const isVideoId = /^[a-zA-Z0-9_-]{11}$/.test(query);
        if (isVideoId) {
            videoId = query;
            videoUrl = `https://www.youtube.com/watch?v=${videoId}`;
        } else {
            console.log(`🔎 Searching YouTube for: "${query}"`);
            const r = await yts(query);
            const video = r.videos[0];
            if (!video) throw new Error('No YouTube match found');
            videoUrl = video.url;
            videoId = video.videoId;
        }

        // --- ATTEMPT 1: Local Extraction ---
        try {
            console.log(`🎵 [Attempt 1] Local Extraction: ${videoId}`);
            const info = await withTimeout(ytdl.getInfo(videoUrl, {
                requestOptions: { headers: { 'User-Agent': 'Mozilla/5.0' } }
            }), 8000);
            const format = ytdl.chooseFormat(info.formats, { quality: 'highestaudio', filter: 'audioonly' });
            if (format) return { streamUrl: format.url, videoId };
        } catch (e) { console.log('⚠️ [Attempt 1] Failed'); }

        // --- ATTEMPT 2: Cobalt API (Updated v1 format) ---
        try {
            console.log(`🚀 [Attempt 2] Cobalt Proxy: ${videoId}`);
            const response = await axios.post('https://api.cobalt.tools/', {
                url: videoUrl,
                downloadMode: 'audio',
                audioFormat: 'mp3'
            }, {
                headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
                timeout: 8000
            });
            const cobaltUrl = response.data?.url || response.data?.tunnel;
            if (cobaltUrl) {
                console.log('✅ [Attempt 2] Success via Cobalt!');
                return { streamUrl: cobaltUrl, videoId };
            }
        } catch (e) { console.log('⚠️ [Attempt 2] Cobalt Failed:', e.message); }

        // --- ATTEMPT 3: Piped API ---
        try {
            console.log(`🌐 [Attempt 3] Piped Proxy: ${videoId}`);
            const response = await axios.get(`https://pipedapi.kavin.rocks/streams/${videoId}`, { timeout: 8000 });
            const audioStream = response.data.audioStreams.find(s => s.format === 'M4A' || s.bitrate > 100000) || response.data.audioStreams[0];
            if (audioStream) return { streamUrl: audioStream.url, videoId };
        } catch (e) { console.log('⚠️ [Attempt 3] Piped Failed'); }

        // --- ATTEMPT 4: Invidious Rotation ---
        const invInstances = ['https://invidious.projectsegfau.lt', 'https://iv.melmac.space'];
        for (const inst of invInstances) {
            try {
                console.log(`☁️ [Attempt 4] Invidious (${inst}): ${videoId}`);
                const response = await axios.get(`${inst}/api/v1/videos/${videoId}`, { timeout: 8000 });
                if (response.data.adaptiveFormats) {
                    const format = response.data.adaptiveFormats.find(f => f.type.includes('audio')) || response.data.adaptiveFormats[0];
                    return { streamUrl: format.url, videoId };
                }
            } catch (e) {}
        }

        throw new Error('All streaming sources exhausted');
    }
};

module.exports = YouTubeService;
