# Baby Tracker App — Design Document

**Date:** 2026-02-22
**Author:** Claude (via brainstorming session with Tony)
**Status:** Approved

---

## Overview

A web-based baby tracking application for mothers (and families) in Taiwan,
covering birth to 6 years old. Replaces paper-based health records with a
simple, fast digital experience. Content is grounded in Taiwan's National Health
Administration (國健署) and CDC guidelines.

**Core design principle:** Minimum taps, single-hand operation. Recording a
routine event must take one tap or fewer than 5 seconds.

---

## Target Users

- **Primary:** New mothers with babies aged 0–6 years
- **Secondary:** Fathers and other caregivers invited to the same baby profile

---

## Platform

- **Phase 1:** Web App (responsive, mobile-first)
- **Phase 2:** Progressive Web App (PWA) with browser push notifications;
  eventual native app packaging

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js 14 (React + TypeScript) |
| Backend API | Spring Boot 3 (Java) |
| Database | PostgreSQL |
| Authentication | Spring Security + JWT |
| Email | Resend |
| Frontend hosting | Vercel |
| Backend hosting | Railway |

---

## Feature Scope

### Phase 1 (MVP)

- Account registration / login (JWT)
- Baby profile creation (name, birth date, gender)
- Family sharing via invite link (owner + viewer roles)
- One-tap daily tracking: feeding, sleep, wet diaper, dirty diaper
- Complementary food diary with allergy tracking
- Growth records (height, weight, head circumference) + percentile chart
- Vaccination schedule (Taiwan CDC 0–6 yr data pre-loaded) + email reminders
- Health education articles (admin-managed, per age range, with 國健署 links)
- In-app notification centre
- Email reminders for upcoming vaccines

### Phase 2

- Development milestone self-assessment (by age band)
- PWA service worker + browser push notifications
- Feeding / sleep trend charts
- Baby photo log

---

## Architecture

```
[Next.js Frontend]  ──HTTPS REST──>  [Spring Boot API]  ──>  [PostgreSQL]
      │                                      │
  Vercel CDN                           Railway (VPC)
                                             │
                                       [Resend Email]
```

All API responses are JSON. JWT access tokens expire in 15 minutes; refresh
tokens are stored in HttpOnly cookies (7-day rolling).

---

## Database Design

### Flexibility strategy

Every extensible table carries a `metadata JSONB` column so new fields can be
added without schema migrations.

### Tables

#### Account & Family

```sql
users (
  id            UUID PRIMARY KEY,
  email         TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name          TEXT NOT NULL,
  created_at    TIMESTAMPTZ DEFAULT now()
)

babies (
  id         UUID PRIMARY KEY,
  name       TEXT NOT NULL,
  birth_date DATE NOT NULL,
  gender     TEXT,               -- 'M' | 'F' | null
  extra      JSONB DEFAULT '{}', -- blood type, premature weeks, allergies, etc.
  created_at TIMESTAMPTZ DEFAULT now()
)

family_members (
  baby_id UUID REFERENCES babies,
  user_id UUID REFERENCES users,
  role    TEXT NOT NULL,         -- 'owner' | 'viewer'
  PRIMARY KEY (baby_id, user_id)
)

invitations (
  id         UUID PRIMARY KEY,
  baby_id    UUID REFERENCES babies,
  token      TEXT UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at    TIMESTAMPTZ
)
```

#### Daily Tracking

```sql
tracking_logs (
  id        UUID PRIMARY KEY,
  baby_id   UUID REFERENCES babies,
  user_id   UUID REFERENCES users,
  type      TEXT NOT NULL,       -- FEED | SLEEP_START | SLEEP_END |
                                 --  DIAPER_WET | DIAPER_DIRTY
  value     NUMERIC,             -- ml for feed; auto for sleep duration
  note      TEXT,
  metadata  JSONB DEFAULT '{}',  -- {"unit":"ml","side":"left"} for feed
                                 --  {"quality":"good"} for sleep, etc.
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

One-tap rules:
- `DIAPER_WET` / `DIAPER_DIRTY` → single POST, no additional fields required
- `FEED` → modal: amount (ml or minutes) + optional side (left/right breast)
- `SLEEP_START` → single POST; `SLEEP_END` closes the open session and stores
  duration

#### Complementary Food

```sql
baby_foods (
  id                 UUID PRIMARY KEY,
  baby_id            UUID REFERENCES babies,
  name               TEXT NOT NULL,
  category           TEXT,       -- 蔬菜 | 水果 | 蛋白質 | 穀類 | 其他
  first_introduced   DATE,
  allergy_status     TEXT DEFAULT 'normal', -- normal | suspected | confirmed
  notes              TEXT,
  created_at         TIMESTAMPTZ DEFAULT now()
)

food_logs (
  id            UUID PRIMARY KEY,
  baby_id       UUID REFERENCES babies,
  food_id       UUID REFERENCES baby_foods,
  amount        TEXT,            -- e.g. "2湯匙"
  texture       TEXT,            -- 泥狀 | 碎末 | 小塊
  reaction      TEXT DEFAULT 'normal', -- normal | rash | vomit | other
  reaction_note TEXT,
  metadata      JSONB DEFAULT '{}',
  logged_at     TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

First-time food rule: when `first_introduced` is set today, the frontend shows
an in-app tip: "建議觀察 3 天再嘗試下一種新食物".

#### Growth

```sql
growth_records (
  id                UUID PRIMARY KEY,
  baby_id           UUID REFERENCES babies,
  weight_kg         NUMERIC(5,3),
  height_cm         NUMERIC(5,1),
  head_cm           NUMERIC(5,1),
  measured_at       DATE NOT NULL,
  note              TEXT,
  created_at        TIMESTAMPTZ DEFAULT now()
)
```

Percentile data (WHO / 國健署 standard) is stored as a static JSON file in the
backend and used for chart rendering.

#### Vaccination

```sql
vaccines (
  id            UUID PRIMARY KEY,
  name          TEXT NOT NULL,   -- e.g. "五合一疫苗 第1劑"
  age_months    INT NOT NULL,    -- scheduled age in months
  doses_total   INT DEFAULT 1,
  dose_number   INT DEFAULT 1,
  funded        BOOLEAN DEFAULT true, -- 公費 / 自費
  description   TEXT,
  source_url    TEXT             -- CDC official link
)

vaccine_records (
  id              UUID PRIMARY KEY,
  baby_id         UUID REFERENCES babies,
  vaccine_id      UUID REFERENCES vaccines,
  administered_at DATE,
  clinic          TEXT,
  note            TEXT,
  created_at      TIMESTAMPTZ DEFAULT now()
)
```

The `vaccines` table is pre-seeded with Taiwan CDC's 0–6 year schedule
(2025/114年版). Admins can add or update entries without code changes.

#### Health Education

```sql
health_articles (
  id              UUID PRIMARY KEY,
  title           TEXT NOT NULL,
  content         TEXT NOT NULL,
  age_min_months  INT NOT NULL,
  age_max_months  INT NOT NULL,
  tags            TEXT[],
  source_url      TEXT,          -- 國健署 or CDC link
  published_at    TIMESTAMPTZ,
  created_at      TIMESTAMPTZ DEFAULT now()
)
```

Articles are filtered server-side by the baby's current age in months and
returned in the dashboard feed.

#### Notifications

```sql
notifications (
  id         UUID PRIMARY KEY,
  user_id    UUID REFERENCES users,
  type       TEXT,               -- VACCINE_REMINDER | MILESTONE | ARTICLE
  title      TEXT NOT NULL,
  body       TEXT,
  is_read    BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT now()
)

email_reminders (
  id           UUID PRIMARY KEY,
  user_id      UUID REFERENCES users,
  baby_id      UUID REFERENCES babies,
  type         TEXT,
  scheduled_at TIMESTAMPTZ NOT NULL,
  sent_at      TIMESTAMPTZ
)
```

---

## Key UI Screens

### Home Dashboard

```
┌──────────────────────────────┐
│  🍼 小明   3個月12天          │
│  ──────────────────────      │
│  ⚠️  本週提醒：五合一第2劑    │
│      建議預約：2/28 前        │
│  ──────────────────────      │
│  今日記錄                    │
│  餵奶 ×4  睡眠 6h  尿布 ×5   │
│  ──────────────────────      │
│  本月衛教                    │
│  [3個月寶寶注意事項 →]        │
└──────────────────────────────┘
         底部快速記錄列
  [🍼] [😴] [💧] [💩] [🥣副食品]
```

### One-Tap Recording

| Type | Interaction |
|------|-------------|
| Wet diaper | Tap → recorded instantly |
| Dirty diaper | Tap → recorded instantly |
| Feeding | Tap → bottom sheet → enter ml or minutes → Done |
| Sleep | Tap "睡覺" → timer starts; tap again → session closed |
| Solid food | Tap → pick from food list or add new → amount + texture → reaction |

### Complementary Food Page

```
副食品
├── 已嘗試食物
│     紅蘿蔔泥   ✅ 正常   首次：3/1
│     蛋黃       ⚠️ 疑似   首次：3/8
│     豆腐       ✅ 正常   首次：3/15
│
└── 今日記錄
      [+ 新增食物]
        → 選已有 or 輸入新食物名稱
        → 份量 / 型態
        → 反應：[正常] [紅疹] [嘔吐] [其他]
```

### Vaccination Page

Timeline view showing: Done ✅ / Upcoming ⏰ / Overdue ❗
Each entry links to the CDC source page.

---

## Notification Strategy

| Channel | Trigger | Phase |
|---------|---------|-------|
| In-app | Vaccine due in 7 days, new article for current age | 1 |
| Email (Resend) | Vaccine due in 7 days and 1 day | 1 |
| Browser push (PWA) | Same as email triggers | 2 |

---

## Admin Content Management

A minimal `/admin` section (protected by a separate admin role) allows:
- Create / edit / delete health articles
- Set article age range and source URL
- View registered families (read-only, for support)

---

## Security Notes

- Passwords hashed with BCrypt (cost factor 12)
- JWT access token in memory; refresh token in HttpOnly cookie
- Invite tokens are single-use, expire in 48 hours
- All endpoints require authentication except `/auth/**`
- Baby data is scoped to `family_members`; no cross-family data leakage

---

## Data Sources

- Vaccination schedule: [衛生福利部疾病管制署](https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA)
- Growth percentile charts: WHO Child Growth Standards / 國民健康署
- Health education content: [國民健康署電子書](https://www.hpa.gov.tw/Pages/EBook.aspx?nodeid=1139)
