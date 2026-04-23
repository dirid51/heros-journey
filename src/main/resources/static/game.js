const input = document.getElementById('user-input');
const history = document.getElementById('history-log');

// Client-side rate limiting: debounce requests to prevent rapid-fire API calls
let lastRequestTime = 0;
const MIN_REQUEST_INTERVAL_MS = 500; // Minimum 500ms between requests

// Initialize game on page load to ensure CSRF token is generated
document.addEventListener('DOMContentLoaded', async () => {
    try {
        await fetch('/api/game/init', {
            method: 'GET',
            credentials: 'same-origin'
        });
    } catch (error) {
        console.error('Failed to initialize game:', error);
    }
});

input.addEventListener('keydown', async (e) => {
    if (e.key === 'Enter') {
        const now = Date.now();

        // Enforce client-side rate limiting
        if (now - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
            appendLog('Please wait before acting again...', 'rate-limit-text');
            return;
        }

        lastRequestTime = now;
        const cmd = input.value;
        input.value = '';

        // Add user command to log
        appendLog(`> ${cmd}`, 'action-text');

        try {
            // Send to Spring Boot with CSRF token from cookie
            const response = await fetch('/api/game/action', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': getCsrfToken()
                },
                credentials: 'same-origin',
                body: JSON.stringify({ command: cmd })
            });

            if (!response.ok) {
                appendLog('Error communicating with server. Please try again.', 'error-text');
                return;
            }

            const gameState = await response.json();
            updateUI(gameState);
        } catch (error) {
            console.error('Error:', error);
            appendLog('Network error. Please try again.', 'error-text');
        }
    }
});

/**
 * Extract CSRF token from cookies for secure POST requests
 */
function getCsrfToken() {
    const name = 'XSRF-TOKEN';
    let csrfToken = '';
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (const cookie of cookies) {
            const trimmedCookie = cookie.trim();
            if (trimmedCookie.startsWith(name + '=')) {
                csrfToken = decodeURIComponent(trimmedCookie.substring(name.length + 1));
                break;
            }
        }
    }
    return csrfToken;
}

function appendLog(text, className) {
    const p = document.createElement('p');
    p.className = className;
    p.innerText = text;
    history.appendChild(p);
    history.scrollTop = history.scrollHeight; // Auto-scroll to bottom
}

function updateUI(state) {
    appendLog(state.description, 'response-text');

    // Update Stats
    document.getElementById('hp-val').innerText = state.player.currentHealth;
    document.getElementById('max-hp-val').innerText = state.player.maxHealth;
    document.getElementById('ir-val').innerText = (state.player.injuryReduction * 100).toFixed(0);

    // Update Skills
    const list = document.getElementById('skills-list');
    list.innerHTML = '';
    for (const [name, level] of Object.entries(state.player.skills)) {
        const li = document.createElement('li');
        li.innerText = `${name}: ${level}`;
        list.appendChild(li);
    }

    if (state.player.currentHealth < 0) {
        appendLog("FATAL INJURY: Your journey ends here.", "death-text");
        document.getElementById('user-input').disabled = true;
        document.getElementById('user-input').placeholder = "GAME OVER";
    }
}