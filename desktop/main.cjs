const { app, BrowserWindow, Menu, dialog, session } = require('electron');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const { spawn } = require('node:child_process');

const STARTUP_TIMEOUT_MS = 45_000;
const POLL_INTERVAL_MS = 100;

let mainWindow;
let backendProcess;
let backendLog;
let backendOrigin;
let backendPortFile;
let backendStartError;
let isQuitting = false;

function bundledPath(...segments) {
  return path.join(process.resourcesPath, ...segments);
}

function backendPaths() {
  if (app.isPackaged) {
    return {
      jar: bundledPath('backend', 'blackjack.jar'),
      java: bundledPath('runtime', 'bin', process.platform === 'win32' ? 'javaw.exe' : 'java')
    };
  }

  const java = process.env.BLACKJACK_JAVA
    || (process.env.JAVA_HOME
      ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
      : 'java');
  return { jar: path.resolve(__dirname, '..', 'target', 'blackjack.jar'), java };
}

function createWindow() {
  mainWindow = new BrowserWindow({
    title: 'Blackjack',
    width: 1180,
    height: 820,
    minWidth: 320,
    minHeight: 568,
    backgroundColor: '#f5f3ed',
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
      devTools: !app.isPackaged
    }
  });

  mainWindow.once('ready-to-show', () => mainWindow?.show());
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mainWindow.webContents.on('will-navigate', (event, destination) => {
    try {
      if (!backendOrigin || new URL(destination).origin !== backendOrigin) {
        event.preventDefault();
      }
    } catch {
      event.preventDefault();
    }
  });
  mainWindow.on('closed', () => {
    mainWindow = undefined;
  });

  return mainWindow.loadFile(path.join(__dirname, 'loading.html'));
}

function startBackend() {
  const { jar, java } = backendPaths();
  if (!fs.existsSync(jar)) {
    throw new Error(`The backend is missing: ${jar}`);
  }
  if (app.isPackaged && !fs.existsSync(java)) {
    throw new Error(`The bundled Java runtime is missing: ${java}`);
  }

  const dataDirectory = app.getPath('userData');
  const logDirectory = path.join(dataDirectory, 'logs');
  const portFile = path.join(dataDirectory, 'server.port');
  fs.mkdirSync(logDirectory, { recursive: true });
  fs.rmSync(portFile, { force: true });
  backendLog = fs.createWriteStream(path.join(logDirectory, 'backend.log'), { flags: 'a' });

  const arguments = [
    '-Djava.awt.headless=true',
    '-jar', jar,
    '--spring.profiles.active=desktop',
    '--server.address=127.0.0.1',
    '--server.port=0',
    '--vaadin.productionMode=true',
    '--spring.main.banner-mode=off',
    `--blackjack.desktop.port-file=${portFile}`,
    `--blackjack.desktop.parent-pid=${process.pid}`
  ];

  backendProcess = spawn(java, arguments, {
    cwd: dataDirectory,
    windowsHide: true,
    stdio: ['ignore', 'pipe', 'pipe']
  });
  backendProcess.stdout.pipe(backendLog, { end: false });
  backendProcess.stderr.pipe(backendLog, { end: false });
  backendPortFile = portFile;
  backendStartError = undefined;
  backendProcess.once('error', (error) => {
    backendStartError = error;
  });
  backendProcess.once('exit', (code, signal) => {
    if (!isQuitting && mainWindow) {
      dialog.showErrorBox('Blackjack stopped',
        `The game service ended unexpectedly (${signal || `exit ${code}`}).\n\nLog: ${backendLog.path}`);
      app.quit();
    }
  });

  return { portFile, logFile: backendLog.path };
}

function readPort(portFile) {
  try {
    const value = fs.readFileSync(portFile, 'utf8').trim();
    if (!/^\d{1,5}$/.test(value)) return undefined;
    const port = Number(value);
    return port > 0 && port <= 65_535 ? port : undefined;
  } catch (error) {
    if (error.code === 'ENOENT') return undefined;
    throw error;
  }
}

function serverResponds(origin) {
  return new Promise((resolve) => {
    const request = http.get(origin, { timeout: 1_000 }, (response) => {
      response.resume();
      resolve(response.statusCode >= 200 && response.statusCode < 500);
    });
    request.on('timeout', () => {
      request.destroy();
      resolve(false);
    });
    request.on('error', () => resolve(false));
  });
}

async function waitForBackend(portFile) {
  const deadline = Date.now() + STARTUP_TIMEOUT_MS;
  while (Date.now() < deadline) {
    if (backendStartError) throw backendStartError;
    if (backendProcess?.exitCode !== null) {
      throw new Error(`The game service exited with code ${backendProcess.exitCode}.`);
    }
    const port = readPort(portFile);
    if (port) {
      const origin = `http://127.0.0.1:${port}`;
      if (await serverResponds(origin)) return origin;
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
  }
  throw new Error('The game did not finish starting within 45 seconds.');
}

function stopBackend() {
  isQuitting = true;
  if (backendProcess && backendProcess.exitCode === null) {
    backendProcess.kill();
  }
  backendProcess = undefined;
  backendLog?.end();
  backendLog = undefined;
  if (backendPortFile) fs.rmSync(backendPortFile, { force: true });
  backendPortFile = undefined;
}

async function launch() {
  Menu.setApplicationMenu(null);
  session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => callback(false));
  session.defaultSession.setPermissionCheckHandler(() => false);
  await createWindow();
  const { portFile, logFile } = startBackend();

  try {
    backendOrigin = await waitForBackend(portFile);
    const locale = app.getLocale().toLowerCase();
    const route = locale.startsWith('ru') ? '/ru' : locale.startsWith('es') ? '/es' : '/';
    await mainWindow.loadURL(`${backendOrigin}${route}`);
  } catch (error) {
    dialog.showErrorBox('Blackjack could not start', `${error.message}\n\nLog: ${logFile}`);
    app.quit();
  }
}

const hasSingleInstanceLock = app.requestSingleInstanceLock();
if (!hasSingleInstanceLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });
  app.whenReady().then(launch).catch((error) => {
    dialog.showErrorBox('Blackjack could not start', error.message);
    app.quit();
  });
}

app.on('window-all-closed', () => app.quit());
app.on('before-quit', stopBackend);
process.on('exit', stopBackend);
