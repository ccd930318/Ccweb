-- Complementary food catalogue per baby
CREATE TABLE baby_foods (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id          UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  name             TEXT NOT NULL,
  category         TEXT,
  first_introduced DATE,
  allergy_status   TEXT NOT NULL DEFAULT 'normal',
  notes            TEXT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Food logs (daily)
CREATE TABLE food_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id       UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  food_id       UUID NOT NULL REFERENCES baby_foods(id),
  amount        TEXT,
  texture       TEXT,
  reaction      TEXT NOT NULL DEFAULT 'normal',
  reaction_note TEXT,
  metadata      JSONB NOT NULL DEFAULT '{}',
  logged_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Vaccine definitions (pre-seeded)
CREATE TABLE vaccines (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  age_months  INT NOT NULL,
  dose_number INT NOT NULL DEFAULT 1,
  funded      BOOLEAN NOT NULL DEFAULT true,
  description TEXT,
  source_url  TEXT,
  sort_order  INT NOT NULL DEFAULT 0
);

-- Vaccine records per baby
CREATE TABLE vaccine_records (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id         UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  vaccine_id      UUID NOT NULL REFERENCES vaccines(id),
  administered_at DATE,
  clinic          TEXT,
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Health education articles
CREATE TABLE health_articles (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title          TEXT NOT NULL,
  content        TEXT NOT NULL,
  age_min_months INT NOT NULL,
  age_max_months INT NOT NULL,
  tags           TEXT[] NOT NULL DEFAULT '{}',
  source_url     TEXT,
  published_at   TIMESTAMPTZ,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Notifications
CREATE TABLE notifications (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       TEXT NOT NULL,
  title      TEXT NOT NULL,
  body       TEXT,
  is_read    BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Scheduled email reminders
CREATE TABLE email_reminders (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  baby_id      UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  type         TEXT NOT NULL,
  scheduled_at TIMESTAMPTZ NOT NULL,
  sent_at      TIMESTAMPTZ
);
