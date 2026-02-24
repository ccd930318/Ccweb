"use client";

import { useEffect, useState } from "react";
import { AdminUser } from "../../../lib/api";
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

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);

  useEffect(() => {
    adminAxios()
      .get<AdminUser[]>("/api/admin/users")
      .then((r) => setUsers(r.data))
      .catch(() => setError("無法載入使用者列表"))
      .finally(() => setLoading(false));
  }, []);

  const toggleRole = async (user: AdminUser) => {
    const newRole = user.role === "ADMIN" ? "USER" : "ADMIN";
    setUpdating(user.id);
    try {
      const res = await adminAxios().patch<AdminUser>(
        `/api/admin/users/${user.id}/role`,
        { role: newRole }
      );
      setUsers((prev) => prev.map((u) => (u.id === user.id ? res.data : u)));
    } catch {
      setError("更新角色失敗");
    } finally {
      setUpdating(null);
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-800 mb-6">使用者管理</h2>

      {error && (
        <p className="text-red-600 bg-red-50 px-4 py-3 rounded-lg mb-4">{error}</p>
      )}

      {loading ? (
        <p className="text-gray-500">載入中…</p>
      ) : (
        <div className="bg-white rounded-xl shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-xs uppercase">
              <tr>
                <th className="px-6 py-3 text-left">名稱</th>
                <th className="px-6 py-3 text-left">電子信箱</th>
                <th className="px-6 py-3 text-left">角色</th>
                <th className="px-6 py-3 text-left">建立時間</th>
                <th className="px-6 py-3 text-left">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 font-medium text-gray-900">{user.name}</td>
                  <td className="px-6 py-4 text-gray-600">{user.email}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        user.role === "ADMIN"
                          ? "bg-indigo-100 text-indigo-700"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {user.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString("zh-TW")}
                  </td>
                  <td className="px-6 py-4">
                    <button
                      onClick={() => toggleRole(user)}
                      disabled={updating === user.id}
                      className="text-xs px-3 py-1 rounded-lg border border-gray-300 hover:bg-gray-100 disabled:opacity-50 transition-colors"
                    >
                      {updating === user.id
                        ? "更新中…"
                        : user.role === "ADMIN"
                        ? "降為一般使用者"
                        : "設為管理員"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {users.length === 0 && (
            <p className="text-center py-8 text-gray-400">目前沒有使用者</p>
          )}
        </div>
      )}
    </div>
  );
}
