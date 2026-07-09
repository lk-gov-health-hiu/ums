// Daily-entry PWA logic. No build step, no framework — a form and a fetch
// call is all this screen needs (§1 of the architecture doc). Offline
// tolerance is a client-side localStorage queue, not a framework feature:
// if the POST fails, the submission is queued and retried on 'online'.
const API_BASE = '/ums/api';
const QUEUE_KEY = 'ums.pendingStatusLogs';

const loginForm = document.getElementById('loginForm');
const entryForm = document.getElementById('entryForm');
const loginStatusEl = document.getElementById('loginStatus');
const statusEl = document.getElementById('status');
const queueNoteEl = document.getElementById('queueNote');
const equipmentSelect = document.getElementById('equipment');

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('service-worker.js');
}

async function loadEquipment() {
    const res = await fetch(`${API_BASE}/equipment`);
    if (res.status === 401) {
        showLogin();
        return false;
    }
    const body = await res.json();
    if (!body.success) {
        statusEl.textContent = body.message;
        return false;
    }
    equipmentSelect.innerHTML = body.data
        .map((e) => `<option value="${e.id}">${e.equipmentTypeName} — ${e.location || ''}</option>`)
        .join('');
    return true;
}

function showLogin() {
    loginForm.style.display = 'block';
    entryForm.style.display = 'none';
}

function showEntry() {
    loginForm.style.display = 'none';
    entryForm.style.display = 'block';
}

loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    const body = await res.json();
    if (body.success) {
        showEntry();
        await loadEquipment();
        flushQueue();
    } else {
        loginStatusEl.textContent = body.message || 'Sign-in failed.';
    }
});

entryForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const payload = {
        equipmentId: Number(equipmentSelect.value),
        logDate: new Date().toISOString().slice(0, 10),
        status: document.getElementById('status').value,
        procedureCount: Number(document.getElementById('count').value)
    };
    await submitOrQueue(payload);
});

async function submitOrQueue(payload) {
    try {
        const res = await fetch(`${API_BASE}/status-logs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const body = await res.json();
        statusEl.textContent = body.success ? 'Saved.' : body.message;
    } catch (networkError) {
        queueSubmission(payload);
        statusEl.textContent = 'Offline — saved on this device, will send once back online.';
    }
    renderQueueNote();
}

function queueSubmission(payload) {
    const queue = JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]');
    queue.push(payload);
    localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
}

function renderQueueNote() {
    const queue = JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]');
    queueNoteEl.textContent = queue.length ? `${queue.length} update(s) waiting to sync.` : '';
}

async function flushQueue() {
    const queue = JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]');
    if (!queue.length) {
        return;
    }
    const remaining = [];
    for (const payload of queue) {
        try {
            const res = await fetch(`${API_BASE}/status-logs`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                remaining.push(payload);
            }
        } catch (e) {
            remaining.push(payload);
        }
    }
    localStorage.setItem(QUEUE_KEY, JSON.stringify(remaining));
    renderQueueNote();
}

window.addEventListener('online', flushQueue);

(async function init() {
    renderQueueNote();
    const ok = await loadEquipment();
    if (ok) {
        showEntry();
        flushQueue();
    }
})();
