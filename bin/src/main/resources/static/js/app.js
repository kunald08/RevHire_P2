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

/**
 * Load recent notifications into the navbar dropdown.
 */
function loadNotifDropdown() {
    const body = document.getElementById('notif-dropdown-body');
    if (!body) return;

    body.innerHTML = '<div class="text-center py-4 text-muted" style="font-size:0.88rem;"><div class="spinner-border spinner-border-sm text-primary me-1"></div> Loading...</div>';

    fetch('/notifications/recent')
        .then(r => r.json())
        .then(data => {
            const items = data.notifications || [];
            if (items.length === 0) {
                body.innerHTML = `
                    <div class="text-center py-4">
                        <i class="bi bi-bell-slash text-muted" style="font-size:1.8rem;"></i>
                        <p class="text-muted mb-0 mt-2" style="font-size:0.85rem;">No new notifications</p>
                    </div>`;
                return;
            }
            let html = '';
            items.forEach(n => {
                const icon = getNotifIcon(n.type);
                const bgColor = getNotifBgColor(n.type);
                const iconColor = getNotifIconColor(n.type);
                const unreadClass = !n.isRead ? 'notif-dropdown-unread' : '';
                const timeAgo = n.timeAgo || formatTimeAgo(n.createdAt);
                const link = n.link || '#';
                html += `
                    <a href="${link}" class="notif-dropdown-item ${unreadClass}" 
                       onclick="markNotifRead(event, ${n.id}, '${link}')">
                        <div class="d-flex align-items-start gap-2">
                            <div class="notif-dropdown-icon flex-shrink-0" style="background:${bgColor};">
                                <i class="bi ${icon} ${iconColor}"></i>
                            </div>
                            <div class="flex-grow-1 min-width-0">
                                <p class="mb-0 ${!n.isRead ? 'fw-semibold' : 'text-secondary'}" 
                                   style="font-size:0.84rem;line-height:1.35;">
                                    ${escapeHtml(n.message)}
                                </p>
                                <small class="text-muted" style="font-size:0.72rem;">${timeAgo}</small>
                            </div>
                            ${!n.isRead ? '<span class="notif-dot"></span>' : ''}
                        </div>
                    </a>`;
            });
            body.innerHTML = html;
        })
        .catch(() => {
            body.innerHTML = '<div class="text-center py-3 text-danger" style="font-size:0.85rem;">Failed to load</div>';
        });
}

/**
 * Mark notification as read then navigate to link.
 */
function markNotifRead(event, notifId, link) {
    event.preventDefault();
    fetch(`/notifications/${notifId}/read`, { method: 'POST' })
        .then(() => {
            fetchUnreadCount();
            if (link && link !== '#') {
                window.location.href = link;
            }
        })
        .catch(() => {
            if (link && link !== '#') window.location.href = link;
        });
}

/**
 * Mark all notifications as read from dropdown.
 */
function markAllReadFromDropdown(event) {
    event.preventDefault();
    event.stopPropagation();
    fetch('/notifications/read-all', { method: 'POST' })
        .then(r => {
            if (r.ok) {
                fetchUnreadCount();
                loadNotifDropdown();
            }
        })
        .catch(err => console.error('Error marking all read:', err));
}

/**
 * Mark a notification as read (from notifications page, AJAX).
 */
function markAsReadAjax(notifId, btn) {
    fetch(`/notifications/${notifId}/read`, { method: 'POST' })
        .then(r => {
            if (r.ok) {
                const card = btn.closest('.notif-card');
                if (card) {
                    card.classList.remove('notif-unread');
                    // Remove NEW badge
                    const badge = card.querySelector('.badge.bg-primary');
                    if (badge) badge.remove();
                    // Remove mark-read button
                    btn.closest('form, .d-inline')?.remove();
                }
                fetchUnreadCount();
                updateNotifStats();
            }
        })
        .catch(err => console.error('Error:', err));
}

/**
 * Mark all as read from notifications page (AJAX).
 */
function markAllReadAjax() {
    fetch('/notifications/read-all', { method: 'POST' })
        .then(r => {
            if (r.ok) {
                document.querySelectorAll('.notif-card.notif-unread').forEach(card => {
                    card.classList.remove('notif-unread');
                    const badge = card.querySelector('.badge.bg-primary');
                    if (badge) badge.remove();
                });
                // Remove all mark-read buttons
                document.querySelectorAll('.mark-read-btn').forEach(b => b.closest('.d-inline')?.remove());
                fetchUnreadCount();
                updateNotifStats();
                // Hide "Mark all read" button
                const markAllBtn = document.getElementById('markAllReadBtn');
                if (markAllBtn) markAllBtn.style.display = 'none';
            }
        })
        .catch(err => console.error('Error:', err));
}

/**
 * Delete a notification (AJAX).
 */
function deleteNotifAjax(notifId, btn) {
    const card = btn.closest('.notif-card');
    if (card) {
        card.style.transition = 'all 0.3s ease';
        card.style.opacity = '0';
        card.style.transform = 'translateX(20px)';
    }
    fetch(`/notifications/${notifId}/delete`, { method: 'POST' })
        .then(r => {
            if (r.ok) {
                setTimeout(() => {
                    if (card) card.remove();
                    fetchUnreadCount();
                    updateNotifStats();
                    // Check if list is now empty
                    const remaining = document.querySelectorAll('.notif-card');
                    if (remaining.length === 0) {
                        const container = document.getElementById('notif-list');
                        if (container) {
                            container.innerHTML = `
                                <div class="text-center py-5">
                                    <div class="notif-empty-icon mx-auto mb-3">
                                        <i class="bi bi-bell-slash" style="font-size: 2.5rem; color: #94a3b8;"></i>
                                    </div>
                                    <h5 class="fw-semibold text-secondary">No notifications</h5>
                                    <p class="text-muted mb-0" style="font-size: 0.9rem;">
                                        When there's activity on your account, you'll see it here.
                                    </p>
                                </div>`;
                        }
                    }
                }, 300);
            }
        })
        .catch(err => console.error('Error:', err));
}

/**
 * Update notification stats badges on the page.
 */
function updateNotifStats() {
    const unreadEl = document.getElementById('stat-unread');
    const readEl = document.getElementById('stat-read');
    const totalEl = document.getElementById('stat-total');
    if (!unreadEl) return;

    const total = document.querySelectorAll('.notif-card').length;
    const unread = document.querySelectorAll('.notif-card.notif-unread').length;
    const read = total - unread;
    if (totalEl) totalEl.textContent = total;
    if (unreadEl) unreadEl.textContent = unread;
    if (readEl) readEl.textContent = read;
    // Update header badge
    const headerBadge = document.getElementById('header-unread-badge');
    if (headerBadge) {
        headerBadge.textContent = unread;
        headerBadge.style.display = unread > 0 ? 'inline' : 'none';
    }
}

// ─── Utility functions ───

function getNotifIcon(type) {
    const icons = {
        'APPLICATION_UPDATE': 'bi-file-earmark-check',
        'APPLICATION_RECEIVED': 'bi-inbox-fill',
        'APPLICATION_WITHDRAWN': 'bi-arrow-return-left',
        'JOB_RECOMMENDATION': 'bi-briefcase-fill',
        'SYSTEM': 'bi-gear-fill'
    };
    return icons[type] || 'bi-bell';
}

function getNotifBgColor(type) {
    const colors = {
        'APPLICATION_UPDATE': '#dbeafe',
        'APPLICATION_RECEIVED': '#dcfce7',
        'APPLICATION_WITHDRAWN': '#fef3c7',
        'JOB_RECOMMENDATION': '#e0f2fe',
        'SYSTEM': '#f3f4f6'
    };
    return colors[type] || '#f3f4f6';
}

function getNotifIconColor(type) {
    const colors = {
        'APPLICATION_UPDATE': 'text-primary',
        'APPLICATION_RECEIVED': 'text-success',
        'APPLICATION_WITHDRAWN': 'text-warning',
        'JOB_RECOMMENDATION': 'text-info',
        'SYSTEM': 'text-secondary'
    };
    return colors[type] || 'text-secondary';
}

function formatTimeAgo(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return diffMins + 'm ago';
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return diffHours + 'h ago';
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return diffDays + 'd ago';
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
}
