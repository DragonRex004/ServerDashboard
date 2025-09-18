const logoutButton = document.getElementById('logout-button');

logoutButton.addEventListener('click', async () => {
    const response = await fetch('/api/logout', {method: 'POST'});

    if (response.ok) {
        window.location.href = '/';
    } else {
        console.error('Logout failed.');
    }
});

async function fetchUsername() {
    const response = await fetch('/api/user/me');
    if (response.ok) {
        const data = await response.json();
        document.getElementById('username-display').textContent = data.username;
    } else {
        document.getElementById('username-display').textContent = 'Guest';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    fetchUsername().then(() => console.log("Username loaded!"));
});

const playerCountSpan = document.getElementById('player-count');
const maxPlayersSpan = document.getElementById('max-players');
const serverNameDisplay = document.getElementById('server-name-display');
const statusLight = document.getElementById('status-light');
const serverStatusText = document.getElementById('server-status-text');

function setupWebSocket() {
    const ws = new WebSocket("ws://localhost:7070/ws/dashboard");

    ws.onopen = () => {
        console.log("WebSocket-Verbindung zum Dashboard hergestellt.");
        statusLight.classList.remove('bg-red-500');
        statusLight.classList.add('bg-green-500');
        serverStatusText.textContent = "Online";
    };

    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);

        playerCountSpan.textContent = data.currentPlayers;
        maxPlayersSpan.textContent = data.maxPlayers;
        serverNameDisplay.textContent = `Servername: ${data.serverName}`;
    };

    ws.onclose = (event) => {
        console.log("WebSocket-Verbindung geschlossen. Versuche erneut zu verbinden...");
        statusLight.classList.remove('bg-green-500');
        statusLight.classList.add('bg-red-500');
        serverStatusText.textContent = "Offline";

        setTimeout(setupWebSocket, 5000);
    };

    ws.onerror = (error) => {
        console.error("WebSocket-Fehler:", error);
    };
}

document.addEventListener('DOMContentLoaded', () => {
    fetchUsername().then(() => console.log("Username loaded!"));
    setupWebSocket();
});