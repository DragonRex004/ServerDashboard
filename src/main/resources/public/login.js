const loginForm = document.getElementById('loginForm');
    const messageDiv = document.getElementById('message');

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const formData = new FormData(loginForm);
        const response = await fetch('/api/login', {
            method: 'POST',
            body: new URLSearchParams(formData)
        });

        if (response.ok) {
            // Erfolgreicher Login
            window.location.href = '/dashboard';
        } else {
            // Fehlgeschlagener Login
            const errorMessage = await response.text();
            messageDiv.textContent = errorMessage;
            messageDiv.className = 'text-center mt-4 text-red-500';
        }
    });