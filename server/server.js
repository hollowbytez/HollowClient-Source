const express = require('express');
const mongoose = require('mongoose');
const path = require('path');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
app.use(express.json());

// Serve static admin files
app.use(express.static(path.join(__dirname, 'public')));

const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI;
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || 'Minhal786.New';

// Connect to MongoDB
mongoose.connect(MONGODB_URI)
  .then(() => console.log('Successfully connected to MongoDB!'))
  .catch(err => console.error('MongoDB connection error:', err));

// --- Schemas & Models ---

const keySchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true },
  hwids: { type: [String], default: [] }, // Multi-device support
  maxDevices: { type: Number, default: 1 },
  username: { type: String, default: null }, // Last active username
  usernames: { type: [String], default: [] }, // All usernames associated
  status: { type: String, default: 'active' }, // 'active', 'suspended'
  expiresAt: { type: Date, default: null }, // null = lifetime
  createdAt: { type: Date, default: Date.now },
  
  // Live Telemetry Details
  serverIp: { type: String, default: 'Offline' },
  coords: { type: String, default: 'N/A' },
  worldName: { type: String, default: 'N/A' },
  seed: { type: String, default: 'N/A' },
  lastActive: { type: Date, default: Date.now }
});

const hwidBlacklistSchema = new mongoose.Schema({
  hwid: { type: String, required: true, unique: true },
  reason: { type: String, default: 'Banned by administrator' },
  bannedAt: { type: Date, default: Date.now }
});

const versionSchema = new mongoose.Schema({
  version: { type: String, required: true },
  downloadUrl: { type: String, required: true },
  mandatory: { type: Boolean, default: true }
});

const Key = mongoose.model('Key', keySchema);
const HwidBlacklist = mongoose.model('HwidBlacklist', hwidBlacklistSchema);
const Version = mongoose.model('Version', versionSchema);

// --- Middleware ---

const authenticateAdmin = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Missing Authorization Token' });
  }

  const token = authHeader.split(' ')[1];
  if (token !== ADMIN_TOKEN) {
    return res.status(401).json({ success: false, message: 'Invalid Admin Token' });
  }
  next();
};

// --- Client Endpoints ---

// Endpoint 1: Key & HWID Verification with hardware ban checks & multi-device limit
app.post('/api/verify', async (req, res) => {
  const { key, hwid, username } = req.body;

  if (!key || !hwid) {
    return res.status(400).json({ success: false, message: 'Missing key or HWID!' });
  }

  const clientUsername = username ? username.trim() : 'Unknown';

  try {
    // 1. Check if HWID is blacklisted
    const isBanned = await HwidBlacklist.findOne({ hwid: hwid.trim().toUpperCase() });
    if (isBanned) {
      return res.status(403).json({ success: false, message: `Your hardware (HWID) is permanently banned: ${isBanned.reason}` });
    }

    // 2. Find license
    const license = await Key.findOne({ key: key.trim() });
    if (!license) {
      return res.status(404).json({ success: false, message: 'Invalid license key!' });
    }

    // 3. Check status
    if (license.status !== 'active') {
      return res.status(403).json({ success: false, message: 'This key has been suspended!' });
    }

    // 4. Check expiration
    if (license.expiresAt && license.expiresAt < new Date()) {
      return res.status(403).json({ success: false, message: 'This key has expired!' });
    }

    const clientHwid = hwid.trim().toUpperCase();

    // 5. Check if HWID is already registered to this key
    if (license.hwids.includes(clientHwid)) {
      // Log username if new
      let updated = false;
      if (!license.usernames.includes(clientUsername)) {
        license.usernames.push(clientUsername);
        updated = true;
      }
      license.username = clientUsername;
      if (updated || license.isModified()) {
        await license.save();
      }
      return res.json({ success: true, message: 'Key verification successful!' });
    }

    // 6. Register new HWID if under limit
    if (license.hwids.length < license.maxDevices) {
      license.hwids.push(clientHwid);
      if (!license.usernames.includes(clientUsername)) {
        license.usernames.push(clientUsername);
      }
      license.username = clientUsername;
      await license.save();
      return res.json({ success: true, message: 'Key successfully activated and bound to your device!' });
    }

    // 7. Limit reached
    return res.status(403).json({ success: false, message: `Device limit reached! Max devices: ${license.maxDevices}` });

  } catch (error) {
    console.error(error);
    return res.status(500).json({ success: false, message: 'Server database error!' });
  }
});

// Endpoint 2: Version Checking & Auto-Updates
app.get('/api/check-update', async (req, res) => {
  const clientVersion = req.query.version;

  if (!clientVersion) {
    return res.status(400).json({ success: false, message: 'Missing client version query parameter!' });
  }

  try {
    const latest = await Version.findOne().sort({ _id: -1 });

    if (!latest) {
      return res.json({ updateAvailable: false });
    }

    if (latest.version !== clientVersion) {
      return res.json({
        updateAvailable: true,
        latestVersion: latest.version,
        downloadUrl: latest.downloadUrl,
        mandatory: latest.mandatory
      });
    }

    return res.json({ updateAvailable: false });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ success: false, message: 'Server database error!' });
  }
});

// Endpoint 3: Telemetry Receiver
app.post('/api/telemetry', async (req, res) => {
  const { key, hwid, username, serverIp, coords, worldName, seed } = req.body;

  if (!key || !hwid) {
    return res.status(400).json({ success: false, message: 'Missing telemetry key or HWID' });
  }

  try {
    const license = await Key.findOne({ key: key.trim() });
    if (license && license.status === 'active') {
      license.serverIp = serverIp || 'Unknown';
      license.coords = coords || 'N/A';
      license.worldName = worldName || 'N/A';
      license.seed = seed || 'N/A';
      license.username = username || license.username;
      license.lastActive = new Date();
      await license.save();
      return res.json({ success: true });
    }
    return res.status(403).json({ success: false, message: 'Key inactive or invalid' });
  } catch (error) {
    console.error('Telemetry error:', error);
    return res.status(500).json({ success: false });
  }
});

// --- Admin Endpoints (Secured) ---

// Verify Admin Token
app.post('/api/admin/login', authenticateAdmin, (req, res) => {
  res.json({ success: true, message: 'Authenticated successfully!' });
});

// Get Database Stats
app.get('/api/admin/stats', authenticateAdmin, async (req, res) => {
  try {
    const totalKeys = await Key.countDocuments();
    const activeKeys = await Key.countDocuments({ status: 'active' });
    const suspendedKeys = await Key.countDocuments({ status: 'suspended' });
    const bannedHwids = await HwidBlacklist.countDocuments();
    
    // Check expired keys count
    const expiredKeys = await Key.countDocuments({ expiresAt: { $lt: new Date() } });

    res.json({
      totalKeys,
      activeKeys,
      suspendedKeys,
      bannedHwids,
      expiredKeys
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get All Keys
app.get('/api/admin/keys', authenticateAdmin, async (req, res) => {
  try {
    const keys = await Key.find().sort({ createdAt: -1 });
    res.json(keys);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Generate Keys
app.post('/api/admin/keys/generate', authenticateAdmin, async (req, res) => {
  const { amount, maxDevices, durationDays, prefix } = req.body;

  const count = parseInt(amount) || 1;
  const devLimit = parseInt(maxDevices) || 1;
  const keyPrefix = prefix ? prefix.trim().toUpperCase() : 'HOLLOW';

  try {
    const generated = [];
    for (let i = 0; i < count; i++) {
      const randomBytes = crypto.randomBytes(6).toString('hex').toUpperCase();
      const keyString = `${keyPrefix}-${randomBytes.slice(0, 4)}-${randomBytes.slice(4, 8)}-${randomBytes.slice(8, 12)}`;

      let expiresAt = null;
      if (durationDays && parseFloat(durationDays) > 0) {
        expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + parseFloat(durationDays));
      }

      const newKey = new Key({
        key: keyString,
        maxDevices: devLimit,
        expiresAt
      });

      await newKey.save();
      generated.push(newKey);
    }
    res.json({ success: true, keys: generated });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Key Action (suspend, activate, reset hwid, delete)
app.post('/api/admin/keys/action', authenticateAdmin, async (req, res) => {
  const { key, action } = req.body;

  try {
    const license = await Key.findOne({ key });
    if (!license) {
      return res.status(404).json({ success: false, message: 'Key not found!' });
    }

    if (action === 'suspend') {
      license.status = 'suspended';
    } else if (action === 'activate') {
      license.status = 'active';
    } else if (action === 'reset_hwid') {
      license.hwids = [];
    } else if (action === 'delete') {
      await Key.deleteOne({ key });
      return res.json({ success: true, message: 'Key deleted successfully!' });
    } else {
      return res.status(400).json({ success: false, message: 'Invalid action!' });
    }

    await license.save();
    res.json({ success: true, key: license });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get Blacklisted HWIDs
app.get('/api/admin/blacklist', authenticateAdmin, async (req, res) => {
  try {
    const list = await HwidBlacklist.find().sort({ bannedAt: -1 });
    res.json(list);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Ban an HWID
app.post('/api/admin/blacklist/ban', authenticateAdmin, async (req, res) => {
  const { hwid, reason } = req.body;

  if (!hwid) {
    return res.status(400).json({ success: false, message: 'Missing HWID to ban!' });
  }

  try {
    const targetHwid = hwid.trim().toUpperCase();
    const existing = await HwidBlacklist.findOne({ hwid: targetHwid });
    if (existing) {
      return res.status(400).json({ success: false, message: 'HWID is already banned!' });
    }

    const blacklist = new HwidBlacklist({
      hwid: targetHwid,
      reason: reason ? reason.trim() : 'Banned by administrator'
    });

    await blacklist.save();
    res.json({ success: true, message: 'HWID successfully blacklisted!' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Unban an HWID
app.post('/api/admin/blacklist/unban', authenticateAdmin, async (req, res) => {
  const { hwid } = req.body;

  try {
    await HwidBlacklist.deleteOne({ hwid: hwid.trim().toUpperCase() });
    res.json({ success: true, message: 'HWID successfully unbanned!' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Register / Update Client Update details
app.post('/api/admin/version', authenticateAdmin, async (req, res) => {
  const { version, downloadUrl, mandatory } = req.body;

  if (!version || !downloadUrl) {
    return res.status(400).json({ success: false, message: 'Missing version or downloadUrl' });
  }

  try {
    const update = new Version({
      version: version.trim(),
      downloadUrl: downloadUrl.trim(),
      mandatory: mandatory !== false
    });

    await update.save();
    res.json({ success: true, message: 'Latest version registration updated successfully!' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`HollowClient Auth Server running on port ${PORT}`);
});
