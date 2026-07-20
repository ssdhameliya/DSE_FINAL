# DB migration: add reminders and notifications

-- Create reminders table
CREATE TABLE IF NOT EXISTS reminders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  module TEXT NOT NULL,
  ref_id INTEGER NOT NULL,
  trigger_time TEXT NOT NULL,
  method TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING',
  attempts INTEGER DEFAULT 0,
  created_at TEXT
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT,
  message TEXT,
  severity TEXT DEFAULT 'INFO',
  is_read INTEGER DEFAULT 0,
  created_at TEXT
);
