// ws connection

const wsBaseUrl = (l = window.location) => ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + ":" + l.port;
const WS_API_URL = wsBaseUrl() + '/ws/';

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
        let time = new Date(json.timestamp);
        json.ago = (Date.now() - time) / 1000;
        json.time = ("0" + time.getHours()).slice(-2) + ":" + ("0" + time.getMinutes()).slice(-2) + ":" + ("0" + time.getSeconds()).slice(-2);
        console.log(json);
        if (json.type === 'info') adaptInfo(json.time + ' ' + json.text);
        if (json.type === 'error') adaptError(json.time + ' ' + json.text);
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
    ev.dir = ">";
    el.classList.add('msg', 'shad', ev.dir === '<' ? 'msg-inc' : 'msg-out');
    if (ev.ago < 10) el.classList.add(ev.dir === '<' ? 'scale-in-bl' : 'scale-in-br');
    el.innerHTML = ev.text;
    appendToConsole(el);

    el = document.createElement('p');
    el.classList.add('fs-80', ev.dir === '<' ? 'text-left' : 'text-right');
    if (ev.dir === '>') el.innerHTML = ev.time + '<span class="ml-2 font-weight-bold">' + ev.author + '</span>';
    else el.innerHTML = '<span class="mr-2 font-weight-bold">' + ev.author + '</span>' + ev.time;
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
