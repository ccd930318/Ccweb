"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { authApi } from "../../lib/api";
import { useAuth } from "../../lib/auth";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useState } from "react";

const schema = z.object({
  name: z.string().min(1, "請輸入姓名"),
  email: z.string().email("請輸入有效的電子信箱"),
  password: z
    .string()
    .min(8, "密碼至少 8 個字元")
    .regex(/[A-Z]/, "需包含大寫字母")
    .regex(/[0-9]/, "需包含數字")
    .regex(/[!@#$%^&*]/, "需包含特殊符號"),
});

type FormData = z.infer<typeof schema>;

export default function RegisterPage() {
  const { setToken } = useAuth();
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
      await authApi.register({ name: data.name, email: data.email, password: data.password });
      const res = await authApi.login({ email: data.email, password: data.password });
      setToken(res.data.accessToken);
      router.push("/dashboard");
    } catch {
      setError("註冊失敗，該信箱可能已被使用");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow p-8 space-y-6">
        <h1 className="text-2xl font-bold text-center text-gray-800">建立帳號</h1>

        {error && (
          <p className="text-sm text-red-600 text-center">{error}</p>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">姓名</label>
            <input
              type="text"
              {...register("name")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="你的姓名"
            />
            {errors.name && (
              <p className="text-xs text-red-500 mt-1">{errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">電子信箱</label>
            <input
              type="email"
              {...register("email")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="you@example.com"
            />
            {errors.email && (
              <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">密碼</label>
            <input
              type="password"
              {...register("password")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="••••••••"
            />
            {errors.password && (
              <p className="text-xs text-red-500 mt-1">{errors.password.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-medium rounded-lg py-2 text-sm transition-colors"
          >
            {isSubmitting ? "建立中…" : "建立帳號"}
          </button>
        </form>

        <p className="text-sm text-center text-gray-500">
          已有帳號？{" "}
          <Link href="/login" className="text-blue-600 hover:underline">
            前往登入
          </Link>
        </p>
      </div>
    </div>
  );
}
