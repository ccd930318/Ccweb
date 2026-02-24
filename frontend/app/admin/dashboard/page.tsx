"use client";

import { useEffect, useState } from "react";
import { AdminStats } from "../../../lib/api";
import Cookies from "js-cookie";
import axios from "axios";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

function adminAxios() {
  const token = Cookies.get("adminToken");
  return axios.create({
    baseURL: BASE,
    headers: { Authorization: `Bearer ${token}` },
  });
}

export default function AdminDashboard() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    adminAxios()
      .get<AdminStats>("/api/admin/stats")
      .then((r) => setStats(r.data))
      .catch(() => setError("無法載入統計資料"));
  }, []);

  const cards = stats
    ? [
        { label: "總使用者數", value: stats.totalUsers },
        { label: "管理員數", value: stats.adminCount },
        { label: "寶寶總數", value: stats.totalBabies },
      ]
    : [];

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-800 mb-6">儀表板</h2>

      {error && (
        <p className="text-red-600 bg-red-50 px-4 py-3 rounded-lg mb-6">{error}</p>
      )}

      {!stats && !error && (
        <p className="text-gray-500">載入中…</p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {cards.map((c) => (
          <div key={c.label} className="bg-white rounded-xl shadow p-6">
            <p className="text-sm text-gray-500">{c.label}</p>
            <p className="text-3xl font-bold text-indigo-600 mt-1">{c.value}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
