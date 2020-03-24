// ws connection

const wsBaseUrl = (l = window.location) => ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + ":" + l.port;
const WS_API_URL = wsBaseUrl() + '/messages/';

let wsClient;

function ws_init() {
    wsClient = new WebSocket(WS_API_URL);
    wsClient.onopen = () => {
        clearConsole();
        adaptInfo('The ws connection is open');
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
        let time = new Date(Date.parse(json.time));
        json.ago = (Date.now() - time) / 1000;
        json.time = ("0" + time.getHours()).slice(-2) + ":" + ("0" + time.getMinutes()).slice(-2);
        if (json.type === 'log') adaptLog(json);
        if (json.type === 'msg') adaptMsg(json);
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
    wsClient.send(elInput.value);
    elInput.value = "";
}

// backend message adapters

function adaptLog(ev) {
    let s = ev.time + ' ' + ev.message;
    if (ev.payload1 === 'ERROR') adaptError(s); else adaptInfo(s);
}

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

function adaptMsg(ev) {
    let el = document.createElement('p');
    el.classList.add('msg', 'shad', ev.payload2 === '<' ? 'msg-inc' : 'msg-out');
    if (ev.ago < 10) el.classList.add(ev.payload2 === '<' ? 'scale-in-bl' : 'scale-in-br');
    el.innerHTML = ev.message;
    appendToConsole(el);

    el = document.createElement('p');
    el.classList.add('fs-80', ev.payload2 === '<' ? 'text-left' : 'text-right');
    if (ev.payload2 === '>') el.innerHTML = ev.time + '<span class="ml-2 font-weight-bold">' + ev.payload1 + '</span>';
    else el.innerHTML = '<span class="mr-2 font-weight-bold">' + ev.payload1 + '</span>' + ev.time;
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
