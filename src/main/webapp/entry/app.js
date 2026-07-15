// Daily-entry PWA logic. No build step, no framework — a form and a fetch
// call is all this screen needs (§1 of the architecture doc). Offline
// tolerance is a client-side localStorage queue, not a framework feature:
// if the POST fails, the submission is queued and retried on 'online'.
const API_BASE = '/ums/api';
const QUEUE_KEY = 'ums.pendingStatusLogs';

const loginForm = document.getElementById('loginForm');
const entryForm = document.getElementById('entryForm');
const loginStatusEl = document.getElementById('loginStatus');
const resultEl = document.getElementById('resultMessage');
const queueNoteEl = document.getElementById('queueNote');
const equipmentSelect = document.getElementById('equipment');
const logDateInput = document.getElementById('logDate');
const statusSelect = document.getElementById('statusSelect');
const statusField = document.getElementById('statusField');
const countInput = document.getElementById('count');
const userBar = document.getElementById('userBar');
const currentUserLabel = document.getElementById('currentUserLabel');
const logoutBtn = document.getElementById('logoutBtn');

statusSelect.addEventListener('change', () => {
    statusField.dataset.status = statusSelect.value;
});

function todayLocalIso() {
    const now = new Date();
    const offsetMs = now.getTimezoneOffset() * 60000;
    return new Date(now - offsetMs).toISOString().slice(0, 10);
}

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
        resultEl.textContent = body.message;
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
    userBar.style.display = 'none';
    currentUserLabel.textContent = '';
}

function showEntry(displayName) {
    loginForm.style.display = 'none';
    entryForm.style.display = 'block';
    userBar.style.display = 'flex';
    if (displayName) {
        currentUserLabel.textContent = displayName;
    }
    const today = todayLocalIso();
    logDateInput.value = today;
    logDateInput.max = today;
}

logoutBtn.addEventListener('click', async () => {
    try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
    } finally {
        showLogin();
    }
});

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
        showEntry(body.data && body.data.displayName);
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
        logDate: logDateInput.value,
        status: statusSelect.value,
        procedureCount: Number(countInput.value)
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
        resultEl.textContent = body.message || (body.success ? 'Saved.' : 'Could not save — please try again.');
    } catch (networkError) {
        queueSubmission(payload);
        resultEl.textContent = 'Offline — saved on this device, will send once back online.';
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
