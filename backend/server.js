require('dotenv').config();
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_spotify_key';

const authRoutes    = require('./routes/auth');
const musicRoutes   = require('./routes/music');
const lyricsRoutes  = require('./routes/lyrics');
const artistRoutes  = require('./routes/artist');
const historyRoutes = require('./routes/history');
const libraryRoutes = require('./routes/library');
const playsRoutes   = require('./routes/plays');
const recommendationsRoutes = require('./routes/recommendations');
const JioSaavnService = require('./services/jiosaavn');
const { optionalAuthMiddleware } = require('./middleware/authMiddleware');

app.use(cors());
app.use(express.json());

// ── Registered Routes ──────────────────────────────────────────────────────
app.use('/api/auth',    authRoutes);
app.use('/api/music',   musicRoutes);
app.use('/api/lyrics',  lyricsRoutes);
app.use('/api/artist',  artistRoutes);
app.use('/api/history', historyRoutes);
app.use('/api/library', libraryRoutes);
app.use('/api/plays',   playsRoutes);
app.use('/api/recommendations', recommendationsRoutes);

// ── GET /api/tracks/recent ─────────────────────────────────────────────────
// Trending / chart tracks for the home screen top section
app.get('/api/tracks/recent', async (req, res) => {
    try {
        const query = req.query.query || 'Top Hindi Songs';
        const tracks = await JioSaavnService.searchSongs(query);
        console.log(`🔎 TRENDING: "${query}" -> Found ${tracks.length} tracks`);
        res.json(tracks);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch tracks' });
    }
});

// Removed old /api/tracks/foryou route as it is now replaced by /api/recommendations

// ── Sockets (Listen Together) ──────────────────────────────────────────────
const server = require('http').createServer(app);
const io = require('socket.io')(server, {
    cors: { origin: "*" }
});

io.on('connection', (socket) => {
    // User creates or joins a room
    socket.on('join_room', ({ roomCode, userId }) => {
        socket.join(roomCode);
        io.to(roomCode).emit('user_joined', { userId });
        console.log(`[Socket] User ${userId} joined room ${roomCode}`);
    });

    // Someone plays/pauses
    socket.on('play_pause', ({ roomCode, isPlaying, timestamp }) => {
        socket.to(roomCode).emit('sync_play_pause', { isPlaying, timestamp });
    });

    // Someone changes song
    socket.on('change_song', ({ roomCode, song }) => {
        socket.to(roomCode).emit('sync_song', { song });
    });

    // Someone seeks (drags progress bar)
    socket.on('seek', ({ roomCode, position }) => {
        socket.to(roomCode).emit('sync_seek', { position });
    });

    // Someone adds to queue
    socket.on('add_to_queue', ({ roomCode, song }) => {
        socket.to(roomCode).emit('sync_queue', { song });
    });

    // Someone leaves
    socket.on('leave_room', ({ roomCode }) => {
        socket.leave(roomCode);
        socket.to(roomCode).emit('user_left');
        console.log(`[Socket] User left room ${roomCode}`);
    });

    socket.on('disconnect', () => {
        // notify room someone left
    });
});

server.listen(PORT, () => {
    console.log(`\n🎵 Rythmiq Backend running on http://localhost:${PORT}`);
    console.log(`📊 Personalized recommendation engine: ACTIVE`);
    console.log(`🔌 WebSockets (Listen Together): ACTIVE`);
});
