-- ============================================================
-- V7: Align database schema with JPA entity definitions
-- ============================================================

-- 1. users: add role column expected by User entity
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(255) NOT NULL DEFAULT 'USER';

-- 2. tracking_logs: entity expects occurred_at (Instant) and notes (String)
--    V2 created logged_at and note instead
ALTER TABLE tracking_logs ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE tracking_logs ADD COLUMN IF NOT EXISTS notes TEXT;

-- 3. growth_records: entity expects measurement_date (LocalDate)
--    V2 created measured_at instead
ALTER TABLE growth_records ADD COLUMN IF NOT EXISTS measurement_date DATE NOT NULL DEFAULT CURRENT_DATE;

-- 4. notifications: entity expects message (String) and read (boolean)
--    V3 created title, body, is_read instead
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message TEXT NOT NULL DEFAULT '';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read BOOLEAN NOT NULL DEFAULT false;

-- 5. food_logs (V3) has incompatible structure (UUID food_id -> baby_foods).
--    Rename old table to preserve it, then create the correct one.
ALTER TABLE food_logs RENAME TO baby_food_logs;

-- 6. food_items: reference catalogue required by FoodItem entity and FoodLog FK
CREATE TABLE food_items (
  id                     BIGSERIAL PRIMARY KEY,
  name                   VARCHAR(255) NOT NULL,
  category               VARCHAR(255),
  recommended_age_months INTEGER NOT NULL DEFAULT 0,
  notes                  TEXT
);

-- 7. food_logs: new table matching FoodLog entity
CREATE TABLE food_logs (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id         UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  food_item_id    BIGINT NOT NULL REFERENCES food_items(id),
  introduced_date DATE NOT NULL,
  reaction        VARCHAR(255),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 8. invites: required by Invite entity (V1 created invitations, not invites)
CREATE TABLE invites (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id       UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  invitee_email VARCHAR(255) NOT NULL,
  role          VARCHAR(255) NOT NULL,
  token         VARCHAR(255) UNIQUE NOT NULL,
  accepted      BOOLEAN NOT NULL DEFAULT false,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at    TIMESTAMPTZ NOT NULL
);

-- 9. vaccine_schedules: required by VaccineSchedule entity (V3 created vaccines)
CREATE TABLE vaccine_schedules (
  id                       BIGSERIAL PRIMARY KEY,
  vaccine_name             VARCHAR(255) NOT NULL,
  dose_number              INTEGER NOT NULL,
  recommended_age_months   INTEGER NOT NULL,
  notes                    TEXT
);

-- 10. vaccination_records: required by VaccinationRecord entity (V3 created vaccine_records)
CREATE TABLE vaccination_records (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id           UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  schedule_id       BIGINT NOT NULL REFERENCES vaccine_schedules(id),
  administered_date DATE NOT NULL,
  notes             TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
