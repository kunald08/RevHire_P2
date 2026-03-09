// Function to toggle password visibility
function setupPasswordToggle(buttonId, inputId, iconId) {
    const toggleBtn = document.getElementById(buttonId);
    const passwordInput = document.getElementById(inputId);
    const eyeIcon = document.getElementById(iconId);

    if (toggleBtn && passwordInput && eyeIcon) {
        toggleBtn.addEventListener('click', function (e) {
            e.preventDefault(); // Prevent accidental form submission
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            
            eyeIcon.classList.toggle('bi-eye');
            eyeIcon.classList.toggle('bi-eye-slash');
        });
    }
}

document.addEventListener('DOMContentLoaded', function() {
    // 1. Setup Toggle
    setupPasswordToggle('togglePassword', 'passwordInput', 'eyeIcon');

    // 2. Setup Loading State
    // Target any form that submits to /auth/register
    const registrationForms = document.querySelectorAll('form');
    const regBtn = document.getElementById('regBtn');
    const btnText = document.getElementById('btnText');
    const btnSpinner = document.getElementById('btnSpinner');

    registrationForms.forEach(form => {
        // Check if the action contains '/auth/register'
        if (form.getAttribute('action') && form.getAttribute('action').includes('/auth/register')) {
            form.addEventListener('submit', function() {
                if (regBtn) {
                    regBtn.disabled = true;
                    if (btnText) btnText.innerText = "Sending Verification Code...";
                    if (btnSpinner) btnSpinner.classList.remove('d-none');
                }
            });
        }
    });
});