const express = require('express');
const mongoose = require('mongoose');
require('dotenv').config();

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI;

// Connect to MongoDB
mongoose.connect(MONGODB_URI)
  .then(() => console.log('Successfully connected to MongoDB!'))
  .catch(err => console.error('MongoDB connection error:', err));

// Database Schemas
const keySchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true },
  hwid: { type: String, default: null },
  status: { type: String, default: 'active' }, // 'active', 'suspended', 'banned'
  createdAt: { type: Date, default: Date.now }
});

const versionSchema = new mongoose.Schema({
  version: { type: String, required: true },
  downloadUrl: { type: String, required: true },
  mandatory: { type: Boolean, default: true }
});

const Key = mongoose.model('Key', keySchema);
const Version = mongoose.model('Version', versionSchema);

// Endpoint 1: Key & HWID Verification
app.post('/api/verify', async (req, res) => {
  const { key, hwid } = req.body;

  if (!key || !hwid) {
    return res.status(400).json({ success: false, message: 'Missing key or HWID!' });
  }

  try {
    const license = await Key.findOne({ key: key.trim() });

    if (!license) {
      return res.status(404).json({ success: false, message: 'Invalid license key!' });
    }

    if (license.status !== 'active') {
      return res.status(403).json({ success: false, message: 'This key has been suspended or banned!' });
    }

    // First time activation: bind the key to the user's HWID
    if (!license.hwid) {
      license.hwid = hwid;
      await license.save();
      return res.json({ success: true, message: 'Key successfully activated and bound to your device!' });
    }

    // Subsequent checks: verify if the hardware matches the bound HWID
    if (license.hwid !== hwid) {
      return res.status(403).json({ success: false, message: 'HWID mismatch! This key is locked to another device.' });
    }

    return res.json({ success: true, message: 'Key verification successful!' });
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

app.listen(PORT, () => {
  console.log(`HollowClient Auth Server running on port ${PORT}`);
});
