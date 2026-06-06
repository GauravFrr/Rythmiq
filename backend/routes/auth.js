const express = require('express');
const router = express.Router();
const { supabase } = require('../services/db');
const { authMiddleware } = require('../middleware/authMiddleware');

// Check if a username is available
router.get('/check-username', async (req, res) => {
    try {
        const { username } = req.query;
        if (!username) return res.status(400).json({ error: 'Username is required' });

        const { data, error } = await supabase
            .from('users')
            .select('username')
            .eq('username', username)
            .single();

        if (error && error.code !== 'PGRST116') {
            console.error('Error checking username:', error);
            return res.status(500).json({ error: 'Database error' });
        }

        // If data exists, the username is taken
        if (data) {
            return res.status(200).json({ available: false });
        } else {
            return res.status(200).json({ available: true });
        }
    } catch (error) {
        console.error('Check username error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Sync Firebase User with Supabase Users Table
router.post('/sync', authMiddleware, async (req, res) => {
    try {
        const { name, username, photoUrl, loginMethod } = req.body;
        const uid = req.uid;
        const email = req.email || null;
        const phone = req.phone || null;

        // Check if user exists in Supabase
        const { data: existingUser, error: fetchError } = await supabase
            .from('users')
            .select('*')
            .eq('firebase_uid', uid)
            .single();

        if (fetchError && fetchError.code !== 'PGRST116') { // PGRST116 means 0 rows returned
            console.error('Error fetching user:', fetchError);
            return res.status(500).json({ error: 'Database error' });
        }

        if (existingUser) {
            // User exists, update last seen and username if newly set
            const updates = { last_seen_at: new Date().toISOString() };
            if (username && !existingUser.username) {
                updates.username = username;
            }

            const { error: updateError } = await supabase
                .from('users')
                .update(updates)
                .eq('firebase_uid', uid);

            if (updateError) console.error('Error updating last_seen:', updateError);

            return res.status(200).json({ message: 'User synced', user: existingUser });
        } else {
            // New user, insert into Supabase
            const { data: newUser, error: insertError } = await supabase
                .from('users')
                .insert([{
                    firebase_uid: uid,
                    email: email,
                    phone: phone,
                    name: name,
                    username: username,
                    photo_url: photoUrl,
                    login_method: loginMethod
                }])
                .select()
                .single();

            if (insertError) {
                console.error('Error inserting user:', insertError);
                return res.status(500).json({ error: 'Failed to create user in database' });
            }

            return res.status(201).json({ message: 'User created', user: newUser });
        }
    } catch (error) {
        console.error('Sync error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
