const mongoose = require('mongoose');
const crypto = require('crypto');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI;

const keySchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true },
  hwid: { type: String, default: null },
  status: { type: String, default: 'active' },
  createdAt: { type: Date, default: Date.now }
});

const Key = mongoose.model('Key', keySchema);

async function generateKeys(amount) {
  if (!MONGODB_URI) {
    console.error('Error: MONGODB_URI is not set in your .env file!');
    process.exit(1);
  }

  try {
    await mongoose.connect(MONGODB_URI);
    console.log('Connected to MongoDB.');

    const generated = [];
    for (let i = 0; i < amount; i++) {
      const randomBytes = crypto.randomBytes(6).toString('hex').toUpperCase();
      const key = `HOLLOW-${randomBytes.slice(0, 4)}-${randomBytes.slice(4, 8)}-${randomBytes.slice(8, 12)}`;
      
      const newKey = new Key({ key });
      await newKey.save();
      generated.push(key);
    }

    console.log('\n--- SUCCESS! Generated Keys: ---');
    generated.forEach(k => console.log(k));
    console.log('--------------------------------\n');
  } catch (error) {
    console.error('Error generating keys:', error);
  } finally {
    await mongoose.disconnect();
    console.log('Disconnected from MongoDB.');
  }
}

const amount = parseInt(process.argv[2]) || 5;
generateKeys(amount);
