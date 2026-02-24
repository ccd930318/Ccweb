"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Cookies from "js-cookie";
import { authApi } from "../../../lib/api";

const schema = z.object({
  email: z.string().email("請輸入有效的電子信箱"),
  password: z.string().min(1, "請輸入密碼"),
});

type FormData = z.infer<typeof schema>;

export default function AdminLoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    try {
      setError(null);
      const res = await authApi.login({ email: data.email, password: data.password });
      const token = res.data.accessToken;

      // Decode JWT payload to check role
      const payload = JSON.parse(atob(token.split(".")[1]));
      if (payload.role !== "ADMIN") {
        setError("此帳號沒有管理員權限");
        return;
      }

      Cookies.set("adminToken", token, { expires: 1 / 96 });
      router.push("/admin/dashboard");
    } catch {
      setError("電子信箱或密碼錯誤");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-xl p-8 space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-800">後台管理系統</h1>
          <p className="text-sm text-gray-500 mt-1">請使用管理員帳號登入</p>
        </div>

        {error && (
          <p className="text-sm text-red-600 text-center bg-red-50 py-2 px-3 rounded-lg">
            {error}
          </p>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              電子信箱
            </label>
            <input
              type="email"
              {...register("email")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="admin@ccweb.com"
            />
            {errors.email && (
              <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              密碼
            </label>
            <input
              type="password"
              {...register("password")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="••••••••"
            />
            {errors.password && (
              <p className="text-xs text-red-500 mt-1">{errors.password.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-medium rounded-lg py-2 text-sm transition-colors"
          >
            {isSubmitting ? "登入中…" : "登入"}
          </button>
        </form>
      </div>
    </div>
  );
}
