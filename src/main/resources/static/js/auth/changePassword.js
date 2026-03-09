document.querySelector('form').addEventListener('submit', function(e) {
        const btn = document.getElementById('submitBtn');
        const text = document.getElementById('btnText');
        const spinner = document.getElementById('btnSpinner');

        // Disable button to prevent double-submission
        btn.disabled = true;
        
        // Show spinner and change text
        text.innerText = "Processing...";
        spinner.classList.remove('d-none');
    });