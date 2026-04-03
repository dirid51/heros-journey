const input = document.getElementById('user-input');
const history = document.getElementById('history-log');

input.addEventListener('keydown', async (e) => {
    if (e.key === 'Enter') {
        const cmd = input.value;
        input.value = '';

        // Add user command to log
        appendLog(`> ${cmd}`, 'action-text');

        // Send to Spring Boot
        const response = await fetch('/api/game/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command: cmd })
        });

        const gameState = await response.json();
        updateUI(gameState);
    }
});

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
        return;
    }
}