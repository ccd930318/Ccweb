"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { babyApi, logApi, Baby } from "../../../lib/api";

const quickLogSchema = z.object({
  type: z.string().min(1, "請選擇類型"),
  occurredAt: z.string().min(1),
  notes: z.string().optional(),
});

type QuickLogForm = z.infer<typeof quickLogSchema>;

export default function DashboardPage() {
  const [babies, setBabies] = useState<Baby[]>([]);
  const [selectedBabyId, setSelectedBabyId] = useState<string | null>(null);
  const [logSuccess, setLogSuccess] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<QuickLogForm>({
    resolver: zodResolver(quickLogSchema),
    defaultValues: { occurredAt: new Date().toISOString().slice(0, 16) },
  });

  useEffect(() => {
    babyApi.list().then((r) => {
      setBabies(r.data);
      if (r.data.length > 0) setSelectedBabyId(r.data[0].id);
    });
  }, []);

  const onSubmit = async (data: QuickLogForm) => {
    if (!selectedBabyId) return;
    await logApi.create(selectedBabyId, {
      type: data.type,
      occurredAt: new Date(data.occurredAt).toISOString(),
      notes: data.notes,
    });
    setLogSuccess(true);
    setShowForm(false);
    reset({ occurredAt: new Date().toISOString().slice(0, 16) });
    setTimeout(() => setLogSuccess(false), 3000);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">我的寶寶</h1>
        <Link
          href="/babies/new"
          className="bg-blue-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          + 新增寶寶
        </Link>
      </div>

      {logSuccess && (
        <div className="bg-green-50 border border-green-200 text-green-800 text-sm px-4 py-3 rounded-lg">
          記錄已儲存！
        </div>
      )}

      {babies.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3">👶</p>
          <p>還沒有寶寶資料，先新增一個吧！</p>
        </div>
      ) : (
        <>
          {/* Baby selector */}
          {babies.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-1">
              {babies.map((b) => (
                <button
                  key={b.id}
                  onClick={() => setSelectedBabyId(b.id)}
                  className={`flex-shrink-0 px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
                    selectedBabyId === b.id
                      ? "bg-blue-600 text-white border-blue-600"
                      : "bg-white text-gray-700 border-gray-300 hover:border-blue-400"
                  }`}
                >
                  {b.name}
                </button>
              ))}
            </div>
          )}

          {/* Baby card */}
          {babies
            .filter((b) => b.id === selectedBabyId)
            .map((b) => (
              <div key={b.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center text-2xl">
                    {b.gender === "F" ? "👧" : "👦"}
                  </div>
                  <div>
                    <p className="font-semibold text-gray-800 text-lg">{b.name}</p>
                    <p className="text-sm text-gray-500">{b.birthDate}</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <Link
                    href={`/babies/${b.id}/growth`}
                    className="flex flex-col items-center p-3 bg-green-50 rounded-xl hover:bg-green-100 transition-colors"
                  >
                    <span className="text-2xl">📈</span>
                    <span className="text-xs text-gray-700 mt-1">成長記錄</span>
                  </Link>
                  <Link
                    href={`/babies/${b.id}/vaccinations`}
                    className="flex flex-col items-center p-3 bg-purple-50 rounded-xl hover:bg-purple-100 transition-colors"
                  >
                    <span className="text-2xl">💉</span>
                    <span className="text-xs text-gray-700 mt-1">疫苗接種</span>
                  </Link>
                  <Link
                    href={`/babies/${b.id}/food`}
                    className="flex flex-col items-center p-3 bg-orange-50 rounded-xl hover:bg-orange-100 transition-colors"
                  >
                    <span className="text-2xl">🥕</span>
                    <span className="text-xs text-gray-700 mt-1">副食品日記</span>
                  </Link>
                  <Link
                    href="/articles"
                    className="flex flex-col items-center p-3 bg-yellow-50 rounded-xl hover:bg-yellow-100 transition-colors"
                  >
                    <span className="text-2xl">📖</span>
                    <span className="text-xs text-gray-700 mt-1">育兒文章</span>
                  </Link>
                </div>
              </div>
            ))}
        </>
      )}

      {/* Quick-log bottom bar */}
      {selectedBabyId && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4">
          <div className="max-w-2xl mx-auto">
            {showForm ? (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
                <div className="flex gap-2">
                  <select
                    {...register("type")}
                    className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">選擇類型</option>
                    <option value="feed">餵食</option>
                    <option value="sleep">睡眠</option>
                    <option value="diaper">換尿布</option>
                    <option value="play">玩耍</option>
                    <option value="other">其他</option>
                  </select>
                  <input
                    type="datetime-local"
                    {...register("occurredAt")}
                    className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                {errors.type && (
                  <p className="text-xs text-red-500">{errors.type.message}</p>
                )}
                <input
                  type="text"
                  {...register("notes")}
                  placeholder="備註（選填）"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setShowForm(false)}
                    className="flex-1 border border-gray-300 text-gray-700 rounded-lg py-2 text-sm hover:bg-gray-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="flex-1 bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700 disabled:opacity-50"
                  >
                    {isSubmitting ? "儲存中…" : "儲存記錄"}
                  </button>
                </div>
              </form>
            ) : (
              <button
                onClick={() => setShowForm(true)}
                className="w-full bg-blue-600 text-white font-medium rounded-xl py-3 text-sm hover:bg-blue-700 transition-colors"
              >
                + 快速記錄
              </button>
            )}
          </div>
        </div>
      )}

      {/* Spacer so content isn't hidden behind the bottom bar */}
      {selectedBabyId && <div className="h-24" />}
    </div>
  );
}
