-- Migration: create audit_logs table
-- Run this on the project's database to enable audit logging

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  event_time TIMESTAMPTZ NOT NULL DEFAULT now(),
  actor TEXT,
  event_type TEXT NOT NULL,
  target_id TEXT,
  details JSONB,
  ip_address TEXT,
  created_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_event_time ON audit_logs (event_time);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs (event_type);

