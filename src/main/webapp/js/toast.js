/* toast.js */
const Toast = {
    init() {
        if (!document.getElementById('toastContainer')) {
            const container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
    },

    show(message, type = 'info', duration = 5000) {
        this.init();
        const container = document.getElementById('toastContainer');
        
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icons = {
            error: '🔴',
            success: '🟢',
            info: '🔵'
        };
        
        toast.innerHTML = `
            <span class="toast-icon">${icons[type] || icons.info}</span>
            <div class="toast-message">${message}</div>
            <span class="toast-close">×</span>
        `;
        
        container.appendChild(toast);
        
        // Trigger animation
        setTimeout(() => toast.classList.add('show'), 10);
        
        const closeToast = () => {
            toast.classList.add('hiding');
            setTimeout(() => {
                if (toast.parentNode === container) {
                    container.removeChild(toast);
                }
            }, 300);
        };
        
        toast.querySelector('.toast-close').onclick = closeToast;
        toast.onclick = closeToast;
        
        if (duration > 0) {
            setTimeout(closeToast, duration);
        }
    },
    
    error(message) { this.show(message, 'error'); },
    success(message) { this.show(message, 'success'); },
    info(message) { this.show(message, 'info'); }
};

// Global function to be called from JSP script tags
function showMessage(type, message) {
    if (message && message.trim() !== '') {
        Toast.show(message, type);
    }
}

// Shorthand used by dashboard pages and the shared layout WebSocket handler
function showToast(message, type) {
    Toast.show(message, type || 'info');
}

// Global button loader for SaaS aesthetic
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function() {
            // Find the submit button inside this form (can be btn-submit class or submit type)
            const submitBtn = this.querySelector('button[type="submit"], input[type="submit"], .btn-submit');
            if (submitBtn) {
                // If the form has required fields, check validity first
                if (this.checkValidity && !this.checkValidity()) {
                    return; // Let the browser handle validation errors
                }
                submitBtn.classList.add('loading');
            }
        });
    });
});
