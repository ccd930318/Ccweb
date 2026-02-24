"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { babyApi } from "../../../../lib/api";

const schema = z.object({
  name: z.string().min(1, "請輸入寶寶姓名"),
  birthDate: z.string().min(1, "請選擇出生日期"),
  gender: z.enum(["M", "F", ""]).optional(),
});

type FormData = z.infer<typeof schema>;

export default function NewBabyPage() {
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { gender: "" },
  });

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      await babyApi.create({
        name: data.name,
        birthDate: data.birthDate,
        gender: data.gender || undefined,
      });
      router.push("/dashboard");
    } catch {
      setServerError("新增失敗，請稍後再試");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">
          ← 返回
        </Link>
        <h1 className="text-xl font-bold text-gray-800">新增寶寶</h1>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-5"
      >
        {serverError && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg">
            {serverError}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            寶寶姓名 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            {...register("name")}
            placeholder="例如：小明"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.name && (
            <p className="text-xs text-red-500 mt-1">{errors.name.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            出生日期 <span className="text-red-500">*</span>
          </label>
          <input
            type="date"
            {...register("birthDate")}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.birthDate && (
            <p className="text-xs text-red-500 mt-1">{errors.birthDate.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">性別（選填）</label>
          <select
            {...register("gender")}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">不填寫</option>
            <option value="M">男寶</option>
            <option value="F">女寶</option>
          </select>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-blue-600 text-white font-medium rounded-xl py-3 text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {isSubmitting ? "新增中…" : "新增寶寶"}
        </button>
      </form>
    </div>
  );
}
