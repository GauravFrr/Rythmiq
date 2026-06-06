const axios = require('axios');
const CryptoJS = require('crypto-js');

function decryptUrl(encryptedUrl) {
    try {
        const key = CryptoJS.enc.Utf8.parse('38346591');
        const decrypted = CryptoJS.DES.decrypt({
            ciphertext: CryptoJS.enc.Base64.parse(encryptedUrl)
        }, key, {
            mode: CryptoJS.mode.ECB,
            padding: CryptoJS.pad.Pkcs7
        });
        const decoded = decrypted.toString(CryptoJS.enc.Utf8);
        return decoded.replace('_96.mp4', '_320.mp4').replace('_96_p.mp4', '_320.mp4');
    } catch (e) {
        console.error("DECRYPT ERROR:", e);
        return null;
    }
}

const JioSaavnService = {
    searchSongs: async (query) => {
        console.log(`🔎 Searching Native JioSaavn for: "${query}"`);
        
        try {
            // Use search.getResults for full search
            const response = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'search.getResults', q: query, p: 1, n: 20, _format: 'json', _marker: '0', ctx: 'web6dot0' },
                timeout: 5000
            });
            
            const detailedSongs = response.data?.results || [];
            
            if (detailedSongs.length > 0) {
                console.log(`✅ Found ${detailedSongs.length} tracks on JioSaavn`);
                return detailedSongs.map(song => {
                    const streamUrl = song.encrypted_media_url ? decryptUrl(song.encrypted_media_url) : '';
                    return {
                        id: song.id,
                        title: song.song || song.title,
                        artist: song.primary_artists || song.singers || 'Unknown Artist',
                        album: song.album || '',
                        coverUrl: song.image ? song.image.replace('150x150', '500x500') : '',
                        durationMs: parseInt(song.duration || 0) * 1000,
                        audioUrl: streamUrl,
                        language: song.language || 'unknown',
                        year: song.year || '',
                        play_count: song.play_count || 0,
                        artistId: song.primary_artists_id || ''
                    };
                });
            }
        } catch (error) {
            console.error('⚠️ Native JioSaavn API failed:', error.message);
        }

        return [];
    },

    searchAll: async (query) => {
        try {
            const response = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'autocomplete.get', query: query, _format: 'json', _marker: '0', ctx: 'web6dot0' },
                timeout: 5000
            });
            const data = response.data;
            
            let songsResult = await JioSaavnService.searchSongs(query);

            let artistsResult = [];
            if (data.topquery?.data?.[0]?.type === 'artist') {
                 artistsResult.push(data.topquery.data[0]);
            }
            if (data.artists?.data) {
                 artistsResult = [...artistsResult, ...data.artists.data];
            }
            artistsResult = artistsResult.map(a => ({
                id: a.id,
                title: a.title,
                subtitle: a.description || 'Artist',
                imageUrl: a.image ? a.image.replace('50x50', '500x500') : ''
            }));

            const albumsResult = (data.albums?.data || []).map(a => ({
                id: a.id,
                title: a.title,
                subtitle: a.description || 'Album',
                imageUrl: a.image ? a.image.replace('50x50', '500x500') : ''
            }));

            const playlistsResult = (data.playlists?.data || []).map(p => ({
                id: p.id,
                title: p.title,
                subtitle: p.description || 'Playlist',
                imageUrl: p.image ? p.image.replace('50x50', '500x500') : ''
            }));

            return {
                songs: songsResult,
                artists: artistsResult,
                albums: albumsResult,
                playlists: playlistsResult
            };
        } catch (e) {
            console.error('searchAll Error:', e.message);
            return { songs: [], artists: [], albums: [], playlists: [] };
        }
    },

    getAlbumDetails: async (albumId) => {
        try {
            const res = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'content.getAlbumDetails', albumid: albumId, _format: 'json', _marker: '0', ctx: 'web6dot0' }
            });
            const songs = res.data.list || res.data.songs || [];
            if (!songs.length) return [];
            
            const pids = songs.map(s => s.id).join(',');
            const detailResponse = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'song.getDetails', pids: pids, _format: 'json', _marker: '0', ctx: 'web6dot0' }
            });
            const detailedSongs = detailResponse.data?.songs || Object.values(detailResponse.data).filter(item => item && item.id);
            
            if (detailedSongs && detailedSongs.length > 0) {
                return detailedSongs.map(song => ({
                    id: song.id,
                    title: song.song || song.title,
                    artist: song.primary_artists || song.singers || 'Unknown Artist',
                    album: song.album || '',
                    coverUrl: song.image ? song.image.replace('150x150', '500x500') : '',
                    durationMs: parseInt(song.duration || 0) * 1000,
                    audioUrl: song.encrypted_media_url ? decryptUrl(song.encrypted_media_url) : ''
                }));
            }
        } catch(e) { console.error(e.message); }
        return [];
    },

    getPlaylistDetails: async (playlistId) => {
        try {
            const res = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'playlist.getDetails', listid: playlistId, _format: 'json', _marker: '0', ctx: 'web6dot0' }
            });
            const songs = res.data.list || res.data.songs || [];
            if (!songs.length) return [];
            
            const pids = songs.map(s => s.id).join(',');
            const detailResponse = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'song.getDetails', pids: pids, _format: 'json', _marker: '0', ctx: 'web6dot0' }
            });
            const detailedSongs = detailResponse.data?.songs || Object.values(detailResponse.data).filter(item => item && item.id);
            
            if (detailedSongs && detailedSongs.length > 0) {
                return detailedSongs.map(song => ({
                    id: song.id,
                    title: song.song || song.title,
                    artist: song.primary_artists || song.singers || 'Unknown Artist',
                    album: song.album || '',
                    coverUrl: song.image ? song.image.replace('150x150', '500x500') : '',
                    durationMs: parseInt(song.duration || 0) * 1000,
                    audioUrl: song.encrypted_media_url ? decryptUrl(song.encrypted_media_url) : ''
                }));
            }
        } catch(e) { console.error(e.message); }
        return [];
    },

    getArtistTopSongs: async (artistName) => {
        return await JioSaavnService.searchSongs(`${artistName} hits`);
    },

    getTrendingHindi: async () => {
        try {
            const res = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'content.getBrowseModules', _format: 'json', _marker: '0', ctx: 'web6dot0' },
                timeout: 5000
            });
            const charts = res.data;
            // Try to get chart song IDs
            const firstChart = Array.isArray(charts) ? charts[0] : null;
            if (firstChart && firstChart.id) {
                return await JioSaavnService.searchSongs('Hindi Top Songs 2024');
            }
        } catch (e) { /* ignore */ }
        return await JioSaavnService.searchSongs('Hindi Top Songs');
    },

    getSongDetails: async (id) => {
        try {
            const detailResponse = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'song.getDetails', pids: id, _format: 'json', _marker: '0', ctx: 'web6dot0' },
                timeout: 5000
            });
            const song = detailResponse.data?.songs?.[0] || Object.values(detailResponse.data).find(item => item && item.id === id);
            
            if (song) {
                const streamUrl = song.encrypted_media_url ? decryptUrl(song.encrypted_media_url) : '';
                return {
                    id: song.id,
                    title: song.song || song.title,
                    artist: song.primary_artists || song.singers || 'Unknown Artist',
                    album: song.album || '',
                    coverUrl: song.image ? song.image.replace('150x150', '500x500') : '',
                    durationMs: parseInt(song.duration || 0) * 1000,
                    audioUrl: streamUrl
                };
            }
        } catch (e) {
            console.error('⚠️ Native JioSaavn Details API failed:', e.message);
        }
        return null;
    },

    getRecommendedSongs: async (id) => {
        try {
            const res = await axios.get('https://www.jiosaavn.com/api.php', {
                params: { __call: 'reco.getreco', pid: id, _format: 'json', _marker: '0', ctx: 'web6dot0' }
            });
            let songs = res.data;
            if (Array.isArray(songs) && songs.length > 0) {
                const pids = songs.map(s => s.id).join(',');
                const detailResponse = await axios.get('https://www.jiosaavn.com/api.php', {
                    params: { __call: 'song.getDetails', pids: pids, _format: 'json', _marker: '0', ctx: 'web6dot0' }
                });
                const detailedSongs = detailResponse.data?.songs || Object.values(detailResponse.data).filter(item => item && item.id);
                
                if (detailedSongs && detailedSongs.length > 0) {
                    return detailedSongs.map(song => {
                        const streamUrl = song.encrypted_media_url ? decryptUrl(song.encrypted_media_url) : '';
                        return {
                            id: song.id,
                            title: song.song || song.title,
                            artist: song.primary_artists || song.singers || 'Unknown Artist',
                            album: song.album || '',
                            coverUrl: song.image ? song.image.replace('150x150', '500x500') : '',
                            durationMs: parseInt(song.duration || 0) * 1000,
                            audioUrl: streamUrl,
                            language: song.language || 'unknown',
                            year: song.year || '',
                            play_count: song.play_count || 0
                        };
                    });
                }
            }
        } catch(e) { console.error('getRecommendedSongs error:', e.message); }
        return [];
    }
};
module.exports = JioSaavnService;
