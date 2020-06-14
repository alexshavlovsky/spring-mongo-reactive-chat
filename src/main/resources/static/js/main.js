// client UUID resolver

const LS_UUID_KEY = "client-uuid";
const newUuid = () => ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
    (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16));
const loadUuid = () => {
    if (localStorage.getItem(LS_UUID_KEY) == null) localStorage.setItem(LS_UUID_KEY, newUuid());
    return localStorage.getItem(LS_UUID_KEY);
};
const CLIENT_ID = loadUuid();

// ws connection

const wsBaseUrl = (l = window.location) => ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + ":" + l.port;
const WS_API_URL = wsBaseUrl() + '/ws/';

let wsClient;
let snapshot;
let frameID = 0;

function sendTypedMessage(type, payload) {
    wsClient.send(JSON.stringify({
        frameId: frameID++,
        user: {id: CLIENT_ID, nick: CLIENT_ID.slice(0, 8)},
        type,
        payload
    }));
}

function ws_init() {
    wsClient = new WebSocket(WS_API_URL);
    wsClient.onopen = () => {
        clearConsole();
        frameID = 0;
        adaptInfo('The ws connection is open');
        sendTypedMessage("hello", "");
    };
    wsClient.onerror = () => {
    };
    wsClient.onclose = () => {
        adaptError('The ws connection was closed abnormally. Retry in 5 seconds...');
        setTimeout(ws_init, 5000);
    };
    wsClient.onmessage = wsDispatcher;
}

function wsDispatcher(event) {
    try {
        let json = JSON.parse(event.data);
        let time = new Date(json.timestamp);
        json.ago = (Date.now() - time) / 1000;
        json.time = ("0" + time.getHours()).slice(-2) + ":" + ("0" + time.getMinutes()).slice(-2) + ":" + ("0" + time.getSeconds()).slice(-2);
        if (json.type === 'snapshot') {
            snapshot = JSON.parse(json.payload);
            users_list.clear();
            snapshot.clients.forEach(c => users_list.set(c.sessionId, c.nick));
            redrawUsersList();
        }
        if (json.type === 'snapshotUpdate') {
            let update = JSON.parse(json.payload);
            if (update.version >= snapshot.version) {
                let client = update.client;
                if (update.type === 'addUser' || update.type === 'updateUser') users_list.set(client.sessionId, client.nick);
                if (update.type === 'removeUser') users_list.delete(client.sessionId);
                redrawUsersList();
            }
        }
        if (json.type === 'info') adaptInfo(json.time + ' ' + json.payload);
        if (json.type === 'error') adaptError(json.time + ' ' + json.payload);
        if (json.type === 'msg') adaptMsg(CLIENT_ID !== json.client.clientId, json.time, json.ago, json.client.nick, json.payload);
        return;
    } catch (e) {
        adaptError('Error while parsing backend message: ' + e);
    }
    adaptError('Error while parsing backend message: ' + event.data);
}

ws_init();

// outgoing ws events

const elInput = document.getElementById("msg-input");
const elButton = document.getElementById("btn-submit");

elInput.addEventListener("keyup", sendOnEnter);
elButton.addEventListener("click", send);

function sendOnEnter(event) {
    if (event.key === "Enter") send();
}

function send() {
    if (elInput.value === "") return;
    sendTypedMessage("msg", elInput.value);
    elInput.value = "";
}

// backend message adapters

function adaptError(s) {
    let el = document.createElement('p');
    el.classList.add('text-center', 'fs-80', 'text-danger');
    el.innerHTML = s;
    appendToConsole(el);
}

function adaptInfo(s) {
    let el = document.createElement('p');
    el.classList.add('text-center', 'fs-80', 'text-info');
    el.innerHTML = s;
    appendToConsole(el);
}

function adaptMsg(incoming, time, ago, author, text) {
    let el = document.createElement('p');
    el.classList.add('msg', 'shad', incoming ? 'msg-inc' : 'msg-out');
    if (ago < 10) el.classList.add(incoming ? 'scale-in-bl' : 'scale-in-br');
    el.innerHTML = text;
    appendToConsole(el);

    el = document.createElement('p');
    el.classList.add('fs-80', incoming ? 'text-left' : 'text-right');
    if (incoming) el.innerHTML = '<span class="mr-2 font-weight-bold">' + author + '</span>' + time;
    else el.innerHTML = time + '<span class="ml-2 font-weight-bold">' + author + '</span>';
    appendToConsole(el);
}

// chat console appender

const elConsole = document.getElementById("console");
const elConsoleContainer = document.getElementById("console-container");

function appendToConsole(el) {
    elConsole.appendChild(el);
    elConsoleContainer.scrollTop = elConsoleContainer.scrollHeight;
}

function clearConsole() {
    while (elConsole.firstChild) {
        elConsole.removeChild(elConsole.firstChild);
    }
}

// users list

let users_list = new Map();

function redrawUsersList() {
    clearUsersList();
    users_list.forEach(v => {
        let el = document.createElement('li');
        el.innerHTML = v;
        appendToUsersList(el);
    });
}

const elUsersList = document.getElementById("users-list");

function appendToUsersList(el) {
    elUsersList.appendChild(el);
}

function clearUsersList() {
    while (elUsersList.firstChild) {
        elUsersList.removeChild(elUsersList.firstChild);
    }
}
