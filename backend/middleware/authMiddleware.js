const { admin } = require('../services/db');

async function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Unauthorized: No token provided' });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decodedToken = await admin.auth().verifyIdToken(token);
        // Attach the firebase user id (uid) to the request object
        req.uid = decodedToken.uid;
        req.email = decodedToken.email;
        req.phone = decodedToken.phone_number;
        next();
    } catch (error) {
        console.error('Firebase token verification failed:', error.message);
        return res.status(401).json({ error: 'Unauthorized: Invalid token' });
    }
}

// Optional middleware for routes that can be accessed by guests OR logged in users
async function optionalAuthMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.split(' ')[1];
        try {
            const decodedToken = await admin.auth().verifyIdToken(token);
            req.uid = decodedToken.uid;
        } catch (error) {
            // Ignore invalid tokens for optional routes, just treat as guest
        }
    }
    next();
}

module.exports = {
    authMiddleware,
    optionalAuthMiddleware
};
