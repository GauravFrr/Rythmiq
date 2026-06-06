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
// Supports both local file (dev) and env variable (production/cloud hosting)
try {
    let serviceAccount;
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        // Cloud hosting: pass the JSON as an environment variable
        serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        console.log('✅ Firebase Admin: loaded from FIREBASE_SERVICE_ACCOUNT env var');
    } else {
        // Local dev: load from file
        serviceAccount = require('../firebase-service-account.json');
        console.log('✅ Firebase Admin: loaded from firebase-service-account.json');
    }
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log('✅ Firebase Admin initialized');
} catch (error) {
    console.error('❌ Failed to initialize Firebase Admin.', error.message);
}

module.exports = {
    supabase,
    admin
};
