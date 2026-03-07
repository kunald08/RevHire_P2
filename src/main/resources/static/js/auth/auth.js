// Function to toggle password visibility
function setupPasswordToggle(buttonId, inputId, iconId) {
    const toggleBtn = document.getElementById(buttonId);
    const passwordInput = document.getElementById(inputId);
    const eyeIcon = document.getElementById(iconId);

    if (toggleBtn && passwordInput && eyeIcon) {
        toggleBtn.addEventListener('click', function () {
            // Toggle the type attribute
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            
            // Toggle the Bootstrap icon classes
            eyeIcon.classList.toggle('bi-eye');
            eyeIcon.classList.toggle('bi-eye-slash');
        });
    }
}

// Initialize when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', function() {
    setupPasswordToggle('togglePassword', 'passwordInput', 'eyeIcon');
});

document.addEventListener('DOMContentLoaded', function() {
    const registrationForm = document.querySelector('form[th\\:action*="/auth/register"]');
    const regBtn = document.getElementById('regBtn');
    const btnText = document.getElementById('btnText');
    const btnSpinner = document.getElementById('btnSpinner');

    if (registrationForm) {
        registrationForm.addEventListener('submit', function() {
            // 1. Disable the button to prevent double submission
            regBtn.disabled = true;

            // 2. Switch text to "Sending..." and show spinner
            btnText.innerText = "Sending Verification Code...";
            btnSpinner.classList.remove('d-none');
            
            // The form will now proceed to submit to the backend
        });
    }
});