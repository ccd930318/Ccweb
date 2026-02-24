import axios from "axios";
import Cookies from "js-cookie";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const api = axios.create({ baseURL: BASE, withCredentials: true });

api.interceptors.request.use((config) => {
  const token = Cookies.get("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      Cookies.remove("accessToken");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export default api;

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data: { email: string; password: string; name: string }) =>
    api.post("/api/auth/register", data),
  login: (data: { email: string; password: string }) =>
    api.post<{ accessToken: string }>("/api/auth/login", data),
  logout: () => api.post("/api/auth/logout"),
  refresh: () => api.post<{ accessToken: string }>("/api/auth/refresh"),
};

// ── Babies ────────────────────────────────────────────────────────────────────
export interface Baby {
  id: string;
  name: string;
  birthDate: string;
  gender?: string;
}

export const babyApi = {
  list: () => api.get<Baby[]>("/api/babies"),
  create: (data: { name: string; birthDate: string; gender?: string }) =>
    api.post<Baby>("/api/babies", data),
  get: (id: string) => api.get<Baby>(`/api/babies/${id}`),
};

// ── Growth ────────────────────────────────────────────────────────────────────
export interface GrowthRecord {
  id: string;
  measurementDate: string;
  weightKg?: number;
  heightCm?: number;
  headCm?: number;
  weightPercentile?: number;
  heightPercentile?: number;
  headPercentile?: number;
}

export const growthApi = {
  list: (babyId: string) => api.get<GrowthRecord[]>(`/api/babies/${babyId}/growth`),
  create: (
    babyId: string,
    data: { measurementDate: string; weightKg?: number | string; heightCm?: number | string; headCm?: number | string }
  ) => api.post<GrowthRecord>(`/api/babies/${babyId}/growth`, data),
};

// ── Vaccinations ──────────────────────────────────────────────────────────────
export interface VaccineScheduleItem {
  id: number;
  vaccineName: string;
  doseNumber: number;
  recommendedAgeMonths: number;
  notes?: string;
}

export interface VaccinationRecord {
  id: string;
  scheduleId: number;
  vaccineName: string;
  doseNumber: number;
  administeredDate: string;
  notes?: string;
}

export const vaccinationApi = {
  listSchedule: () => api.get<VaccineScheduleItem[]>("/api/vaccines/schedule"),
  list: (babyId: string) => api.get<VaccinationRecord[]>(`/api/babies/${babyId}/vaccinations`),
  create: (
    babyId: string,
    data: { scheduleId: number; administeredDate: string; notes?: string }
  ) => api.post<VaccinationRecord>(`/api/babies/${babyId}/vaccinations`, data),
};

// ── Food ──────────────────────────────────────────────────────────────────────
export interface FoodItem {
  id: number;
  name: string;
  category: string;
  recommendedAgeMonths: number;
}

export interface FoodLogEntry {
  id: string;
  foodItemId: number;
  foodName: string;
  category: string;
  introducedDate: string;
  reaction?: string;
}

export const foodApi = {
  listCatalogue: () => api.get<FoodItem[]>("/api/foods"),
  listLogs: (babyId: string) => api.get<FoodLogEntry[]>(`/api/babies/${babyId}/food-logs`),
  createLog: (
    babyId: string,
    data: { foodItemId: number; introducedDate: string; reaction?: string }
  ) => api.post<FoodLogEntry>(`/api/babies/${babyId}/food-logs`, data),
};

// ── Logs ──────────────────────────────────────────────────────────────────────
export interface TrackingLog {
  id: string;
  type: string;
  occurredAt: string;
  notes?: string;
}

export const logApi = {
  list: (babyId: string) => api.get<TrackingLog[]>(`/api/babies/${babyId}/logs`),
  create: (babyId: string, data: { type: string; occurredAt: string; notes?: string }) =>
    api.post<TrackingLog>(`/api/babies/${babyId}/logs`, data),
};

// ── Articles ──────────────────────────────────────────────────────────────────
export interface Article {
  id: string;
  title: string;
  content: string;
  category: string;
  createdAt: string;
}

export const articleApi = {
  list: () => api.get<Article[]>("/api/articles"),
  get: (id: string) => api.get<Article>(`/api/articles/${id}`),
};

// ── Invites ───────────────────────────────────────────────────────────────────
export const inviteApi = {
  create: (babyId: string, data: { email: string; role: string }) =>
    api.post<{ token: string }>(`/api/babies/${babyId}/invites`, data),
  accept: (token: string) => api.post(`/api/invites/${token}/accept`),
};

// ── Notifications ─────────────────────────────────────────────────────────────
export interface AppNotification {
  id: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export const notificationApi = {
  list: () => api.get<AppNotification[]>("/api/notifications"),
  markRead: (id: string) => api.patch<AppNotification>(`/api/notifications/${id}/read`),
  unreadCount: () => api.get<{ count: number }>("/api/notifications/unread-count"),
};

// ── Admin ──────────────────────────────────────────────────────────────────────
export interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: string;
  createdAt: string;
}

export interface AdminStats {
  totalUsers: number;
  adminCount: number;
  totalBabies: number;
}

export const adminApi = {
  listUsers: () => api.get<AdminUser[]>("/api/admin/users"),
  getStats: () => api.get<AdminStats>("/api/admin/stats"),
  updateRole: (id: string, role: string) =>
    api.patch<AdminUser>(`/api/admin/users/${id}/role`, { role }),
};
