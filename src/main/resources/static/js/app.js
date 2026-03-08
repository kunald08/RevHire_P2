/**
 * RevHire — Main JavaScript
 */
document.addEventListener('DOMContentLoaded', function () {

    // Fetch and display unread notification count in navbar badge
    fetchUnreadCount();

    // Poll for new notifications every 30 seconds
    setInterval(fetchUnreadCount, 30000);

});

/**
 * Fetches unread notification count and updates the navbar badge.
 */
function fetchUnreadCount() {
    const badge = document.getElementById('notif-count');
    if (!badge) return;

    fetch('/notifications/unread-count')
        .then(response => {
            if (!response.ok) return;
            return response.json();
        })
        .then(count => {
            if (count && count > 0) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = 'inline';
            } else {
                badge.style.display = 'none';
            }
        })
        .catch(() => {
            badge.style.display = 'none';
        });
}
