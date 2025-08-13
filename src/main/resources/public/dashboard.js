const logoutButton = document.getElementById('logout-button');

    logoutButton.addEventListener('click', async () => {
        // Hier sollte der Backend-Aufruf für den Logout stattfinden
        // Beispiel: await fetch('/api/logout', { method: 'POST' });

        // Zurück zur Login-Seite weiterleiten
        window.location.href = '/';
    });

    // Beispiel: Fetch-Aufruf, um Server-Status zu laden
    async function fetchServerStatus() {
        const response = await fetch('/api/server/status');
        const data = await response.json();
        document.getElementById('server-status').textContent = `Server Status: ${data.status}`;
    }

    document.addEventListener('DOMContentLoaded', fetchServerStatus);