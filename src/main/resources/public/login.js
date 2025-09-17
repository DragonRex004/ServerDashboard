// Korrektes JSON-Login
fetch('/api/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    },
    body: JSON.stringify({ 
        username: username, 
        password: password 
    })
})
.then(response => {
    if (response.ok) {
        return response.json();
    } else {
        throw new Error('Login fehlgeschlagen');
    }
})
.then(data => {
    if (data.success && data.redirect) {
        window.location.href = data.redirect;
    }
})
.catch(error => {
    console.error('Login-Fehler:', error);
});
