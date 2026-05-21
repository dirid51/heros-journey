const input = document.getElementById('user-input');
const history = document.getElementById('history-log');

const spinner = document.getElementById('spinner');
const SPINNER_FRAMES = ['⠋','⠙','⠹','⠸','⠼','⠴','⠦','⠧','⠇','⠏'];
let spinnerInterval = null;

// In game.js, replace the DOMContentLoaded handler:
document.addEventListener('DOMContentLoaded', async () => {
    await fetch('/api/game/init', { credentials: 'same-origin' });

    const loadingScreen = document.getElementById('loading-screen');
    const loadingSpinner = document.getElementById('loading-spinner');
    const loadingText = document.getElementById('loading-text');

    // Animate the loading spinner
    let frame = 0;
    const loadInterval = setInterval(() => {
        loadingSpinner.textContent = SPINNER_FRAMES[frame++ % SPINNER_FRAMES.length];
    }, 80);

    // Poll until the room is ready
    let attempts = 0;
    const MAX_ATTEMPTS = 60; // 30 seconds
    const poll = setInterval(async () => {
        attempts++;
        if (attempts > MAX_ATTEMPTS) {
            clearInterval(poll);
            clearInterval(loadInterval);
            loadingText.textContent = 'The void is unresponsive. Please refresh.';
            return;
        }
        try {
            const res = await fetch('/api/game/ready', { credentials: 'same-origin' });
            const data = await res.json();
            if (data.ready) {
                clearInterval(poll);
                clearInterval(loadInterval);
                appendLog('The mists clear... ' + data.firstRoomDescription, 'response-text');
                loadingScreen.classList.add('hidden');
                input.focus();
            }
        } catch (e) { /* keep polling */ }
    }, 500);
});

input.addEventListener('keydown', async (e) => {
    if (e.key === 'Enter') {
        const cmd = input.value;
        input.value = '';

        // Add user command to log
        appendLog(`> ${cmd}`, 'action-text');

        setLoading(true);
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
        } finally {
            setLoading(false);
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

function setLoading(isLoading) {
    input.disabled = isLoading;
    if (isLoading) {
        let frame = 0;
        spinner.classList.remove('hidden');
        spinnerInterval = setInterval(() => {
            spinner.textContent = SPINNER_FRAMES[frame++ % SPINNER_FRAMES.length];
        }, 80);
    } else {
        clearInterval(spinnerInterval);
        spinner.classList.add('hidden');
        input.disabled = false;
        input.focus();
    }
}