/**
 * SmartAttend - AI Chatbot Module
 */

class SmartChatbot {
    constructor() {
        this.isOpen = false;
        this.jwtToken = null;
        this.userRole = null;
    }

    init(token, role) {
        this.jwtToken = token;
        this.userRole = role;
        this.setupEventListeners();
        this.renderSuggestedChips();
    }

    toggle() {
        this.isOpen = !this.isOpen;
        const drawer = document.getElementById('chatbotDrawer');
        if (drawer) {
            if (this.isOpen) {
                drawer.classList.add('open');
                document.getElementById('chatInput').focus();
            } else {
                drawer.classList.remove('open');
            }
        }
    }

    setupEventListeners() {
        const fab = document.getElementById('chatbotFab');
        const closeBtn = document.getElementById('closeChatBtn');
        const sendBtn = document.getElementById('sendChatBtn');
        const chatInput = document.getElementById('chatInput');

        if (fab) fab.addEventListener('click', () => this.toggle());
        if (closeBtn) closeBtn.addEventListener('click', () => this.toggle());

        if (sendBtn && chatInput) {
            sendBtn.addEventListener('click', () => this.sendMessage());
            chatInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
        }
    }

    renderSuggestedChips() {
        const container = document.getElementById('chatSuggestions');
        if (!container) return;

        let suggestions = [];
        if (this.userRole === 'ROLE_ADMIN') {
            suggestions = [
                'How many students are registered?',
                'How many students have pending barcode registration?',
                'Which students have low attendance?',
                'How many attendance anomalies require review?'
            ];
        } else if (this.userRole === 'ROLE_FACULTY') {
            suggestions = [
                'How many students are present today?',
                'Which students are absent from my class?',
                'Show my active attendance sessions'
            ];
        } else if (this.userRole === 'ROLE_STUDENT') {
            suggestions = [
                'What is my attendance percentage?',
                'How many more classes do I need to attend?',
                'Which subjects need improvement?'
            ];
        }

        container.innerHTML = suggestions.map(s => 
            `<button class="chat-chip" onclick="window.smartChatbot.sendQuickPrompt('${s.replace(/'/g, "\\'")}')">${s}</button>`
        ).join('');
    }

    sendQuickPrompt(text) {
        const input = document.getElementById('chatInput');
        if (input) {
            input.value = text;
            this.sendMessage();
        }
    }

    async sendMessage() {
        const input = document.getElementById('chatInput');
        const message = input.value.trim();
        if (!message) return;

        input.value = '';
        this.appendMessage('user', message);

        const typingId = this.appendTypingIndicator();

        try {
            const token = localStorage.getItem('smartattend_token') || this.jwtToken;
            const res = await fetch('/api/chatbot/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ message })
            });

            this.removeTypingIndicator(typingId);

            if (res.ok) {
                const data = await res.json();
                this.appendMessage('bot', data.reply, data.source);
            } else {
                const errData = await res.json().catch(() => ({}));
                this.appendMessage('bot', '⚠️ Error: ' + (errData.message || 'Unable to connect to AI assistant service.'), 'ERROR');
            }
        } catch (e) {
            this.removeTypingIndicator(typingId);
            this.appendMessage('bot', '⚠️ Network error: Could not reach backend server.', 'ERROR');
        }
    }

    appendMessage(sender, text, source = null) {
        const container = document.getElementById('chatMessages');
        if (!container) return;

        const bubble = document.createElement('div');
        bubble.className = `chat-bubble ${sender}`;

        // Format basic markdown: **bold**, newlines
        let formatted = text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/\n/g, '<br/>');

        let sourceLabel = '';
        if (source === 'GEMINI_AI') {
            sourceLabel = '<div class="chat-bubble-source">✨ Powered by Google Gemini 1.5 Flash</div>';
        } else if (source === 'SMARTATTEND_ENGINE') {
            sourceLabel = '<div class="chat-bubble-source">📊 Real-Time Database Verified</div>';
        }

        bubble.innerHTML = formatted + sourceLabel;
        container.appendChild(bubble);
        container.scrollTop = container.scrollHeight;
    }

    appendTypingIndicator() {
        const container = document.getElementById('chatMessages');
        if (!container) return null;

        const id = 'typing_' + Date.now();
        const bubble = document.createElement('div');
        bubble.id = id;
        bubble.className = 'chat-bubble bot';
        bubble.innerHTML = '<span class="spinner-grow spinner-grow-sm text-primary" role="status"></span> Thinking...';
        container.appendChild(bubble);
        container.scrollTop = container.scrollHeight;
        return id;
    }

    removeTypingIndicator(id) {
        if (!id) return;
        const el = document.getElementById(id);
        if (el) el.remove();
    }
}

window.smartChatbot = new SmartChatbot();
