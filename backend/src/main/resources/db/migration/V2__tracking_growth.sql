-- Daily tracking logs
CREATE TABLE tracking_logs (
  id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id   UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  user_id   UUID NOT NULL REFERENCES users(id),
  type      TEXT NOT NULL,
  value     NUMERIC(8,2),
  note      TEXT,
  metadata  JSONB NOT NULL DEFAULT '{}',
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tracking_baby_date ON tracking_logs(baby_id, logged_at DESC);

-- Growth records
CREATE TABLE growth_records (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id     UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  weight_kg   NUMERIC(5,3),
  height_cm   NUMERIC(5,1),
  head_cm     NUMERIC(5,1),
  measured_at DATE NOT NULL,
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
