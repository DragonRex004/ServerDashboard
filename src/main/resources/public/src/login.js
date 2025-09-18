const loginForm = document.getElementById('loginForm');
const messageDiv = document.getElementById('message');

loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const formData = new FormData(loginForm);
    const response = await fetch('/api/login', {
        method: 'POST',
        body: new URLSearchParams(formData)
    });

    if (response.ok) {
        window.location.href = '/dashboard';
    } else {
        messageDiv.textContent = await response.text();
        messageDiv.className = 'text-center mt-4 text-red-500';
    }
});