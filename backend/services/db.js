require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');

// 1. Initialize Supabase
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_SERVICE_KEY;

if (!supabaseUrl || !supabaseKey) {
    console.error('❌ Missing Supabase environment variables');
    process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);
console.log('✅ Connected to Supabase');

// 2. Initialize Firebase Admin
try {
    const serviceAccount = require('../firebase-service-account.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log('✅ Firebase Admin initialized');
} catch (error) {
    console.error('❌ Failed to initialize Firebase Admin. Ensure firebase-service-account.json is present.', error.message);
}

module.exports = {
    supabase,
    admin
};
