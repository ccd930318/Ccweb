-- tracking_logs is missing created_at expected by TrackingLog.createdAt
ALTER TABLE tracking_logs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
