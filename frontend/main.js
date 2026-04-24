const { app, BrowserWindow } = require('electron');

let win;

function createWindow() {
  win = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: true
    }
  });

  win.loadURL('http://localhost:4200');
}

app.on('window-all-closed', () => {
  app.quit();
});

app.whenReady().then(createWindow);
