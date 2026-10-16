/**
 * scripts/verify_puter.js
 * Verification harness for @heyputer/puter.js integration and live catalog validation.
 */
const https = require('https');

async function fetchModelDetails() {
    return new Promise((resolve, reject) => {
        https.get('https://api.puter.com/puterai/chat/models/details', (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

async function runVerification() {
    console.log('=== Puter.js Integration Verification ===');
    
    // 1. Verify @heyputer/puter.js package can be imported
    try {
        const { init } = require('@heyputer/puter.js/src/init.cjs');
        const puter = init();
        console.log('✔ @heyputer/puter.js package successfully loaded and initialized');
    } catch (e) {
        console.error('✘ Failed to initialize @heyputer/puter.js:', e.message);
        process.exit(1);
    }

    // 2. Verify live model catalog and check default models
    try {
        const catalog = await fetchModelDetails();
        console.log(`✔ Puter live model catalog queried successfully (${catalog.models.length} active models)`);

        const targetModel = 'gpt-5-nano';
        const found = catalog.models.find(m => m.id === targetModel);
        if (found) {
            console.log(`✔ Target model "${targetModel}" found in live catalog:`);
            console.log(`   - Provider: ${found.provider}`);
            console.log(`   - Prompt token cost: ${found.costs?.prompt_tokens} cents / 1M tokens`);
            console.log(`   - Completion token cost: ${found.costs?.completion_tokens} cents / 1M tokens`);
        } else {
            console.warn(`⚠ Target model "${targetModel}" not found directly by ID.`);
        }

        const alternative = catalog.models.find(m => m.id === 'gemini-3.1-flash-lite');
        if (alternative) {
            console.log(`✔ Alternative model "gemini-3.1-flash-lite" confirmed active.`);
        }
    } catch (e) {
        console.error('✘ Failed to query Puter model catalog:', e.message);
        process.exit(1);
    }

    console.log('=== Verification Completed Successfully ===');
    process.exit(0);
}

runVerification();
