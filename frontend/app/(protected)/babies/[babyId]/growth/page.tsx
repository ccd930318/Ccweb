"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import Link from "next/link";
import { growthApi, GrowthRecord } from "../../../../../lib/api";

const schema = z.object({
  measurementDate: z.string().min(1, "請選擇日期"),
  weightKg: z.number().positive("請輸入正確體重").optional(),
  heightCm: z.number().positive("請輸入正確身高").optional(),
  headCm: z.number().positive("請輸入正確頭圍").optional(),
});

type FormData = z.infer<typeof schema>;

export default function GrowthPage() {
  const { babyId } = useParams<{ babyId: string }>();
  const [records, setRecords] = useState<GrowthRecord[]>([]);
  const [showForm, setShowForm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { measurementDate: new Date().toISOString().slice(0, 10) },
  });

  const fetchRecords = () =>
    growthApi.list(babyId).then((r) => setRecords(r.data));

  useEffect(() => {
    fetchRecords();
  }, [babyId]);

  const onSubmit = async (data: FormData) => {
    await growthApi.create(babyId, {
      measurementDate: data.measurementDate,
      weightKg: data.weightKg || undefined,
      heightCm: data.heightCm || undefined,
      headCm: data.headCm || undefined,
    });
    await fetchRecords();
    setShowForm(false);
    reset({ measurementDate: new Date().toISOString().slice(0, 10) });
  };

  const chartData = [...records]
    .reverse()
    .map((r) => ({
      date: r.measurementDate,
      體重: r.weightKg,
      身高: r.heightCm,
      頭圍: r.headCm,
    }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">← 返回</Link>
          <h1 className="text-xl font-bold text-gray-800">成長記錄</h1>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          {showForm ? "取消" : "+ 新增量測"}
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">量測日期</label>
            <input
              type="date"
              {...register("measurementDate")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.measurementDate && (
              <p className="text-xs text-red-500 mt-1">{errors.measurementDate.message}</p>
            )}
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">體重 (kg)</label>
              <input
                type="number"
                step="0.01"
                {...register("weightKg", { setValueAs: (v: string) => v === "" ? undefined : Number(v) })}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="5.20"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">身高 (cm)</label>
              <input
                type="number"
                step="0.1"
                {...register("heightCm", { setValueAs: (v: string) => v === "" ? undefined : Number(v) })}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="60.0"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">頭圍 (cm)</label>
              <input
                type="number"
                step="0.1"
                {...register("headCm", { setValueAs: (v: string) => v === "" ? undefined : Number(v) })}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="40.0"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "儲存中…" : "儲存"}
          </button>
        </form>
      )}

      {/* Weight chart */}
      {chartData.length > 1 && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">體重趨勢 (kg)</h2>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="體重" stroke="#3b82f6" dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Height chart */}
      {chartData.length > 1 && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">身高趨勢 (cm)</h2>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="身高" stroke="#10b981" dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Records list */}
      <div className="space-y-2">
        {records.map((r) => (
          <div
            key={r.id}
            className="bg-white rounded-xl border border-gray-100 px-4 py-3 flex items-center justify-between shadow-sm"
          >
            <span className="text-sm font-medium text-gray-700">{r.measurementDate}</span>
            <div className="flex gap-4 text-sm text-gray-500">
              {r.weightKg != null && (
                <span>
                  {r.weightKg} kg
                  {r.weightPercentile != null && (
                    <span className="text-xs text-blue-500 ml-1">
                      ({Math.round(r.weightPercentile)}%ile)
                    </span>
                  )}
                </span>
              )}
              {r.heightCm != null && <span>{r.heightCm} cm</span>}
              {r.headCm != null && <span>{r.headCm} cm</span>}
            </div>
          </div>
        ))}

        {records.length === 0 && (
          <div className="text-center py-12 text-gray-400">
            <p className="text-3xl mb-2">📏</p>
            <p className="text-sm">尚無成長記錄</p>
          </div>
        )}
      </div>
    </div>
  );
}
