"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { foodApi, FoodItem, FoodLogEntry } from "../../../../../lib/api";

const schema = z.object({
  foodItemId: z.number().int().positive("請選擇食物"),
  introducedDate: z.string().min(1, "請選擇日期"),
  reaction: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

const reactionOptions = ["接受良好", "輕微過敏", "拒絕", "腹瀉", "其他"];

export default function FoodPage() {
  const { babyId } = useParams<{ babyId: string }>();
  const [catalogue, setCatalogue] = useState<FoodItem[]>([]);
  const [logs, setLogs] = useState<FoodLogEntry[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { introducedDate: new Date().toISOString().slice(0, 10) },
  });

  const fetchData = async () => {
    const [c, l] = await Promise.all([foodApi.listCatalogue(), foodApi.listLogs(babyId)]);
    setCatalogue(c.data);
    setLogs(l.data);
  };

  useEffect(() => {
    fetchData();
  }, [babyId]);

  const onSubmit = async (data: FormData) => {
    await foodApi.createLog(babyId, {
      foodItemId: data.foodItemId,
      introducedDate: data.introducedDate,
      reaction: data.reaction,
    });
    await fetchData();
    setShowForm(false);
    reset({ introducedDate: new Date().toISOString().slice(0, 10) });
  };

  const triedIds = new Set(logs.map((l) => l.foodItemId));

  const filteredCatalogue = catalogue.filter(
    (f) =>
      f.name.toLowerCase().includes(search.toLowerCase()) ||
      f.category.toLowerCase().includes(search.toLowerCase())
  );

  // Group catalogue by category
  const grouped = filteredCatalogue.reduce<Record<string, FoodItem[]>>((acc, item) => {
    if (!acc[item.category]) acc[item.category] = [];
    acc[item.category].push(item);
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">← 返回</Link>
          <h1 className="text-xl font-bold text-gray-800">副食品日記</h1>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          {showForm ? "取消" : "+ 記錄嘗試"}
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">食物</label>
            <select
              {...register("foodItemId", { valueAsNumber: true })}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">選擇食物</option>
              {catalogue.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}（{f.category}，{f.recommendedAgeMonths}+ 個月）
                </option>
              ))}
            </select>
            {errors.foodItemId && (
              <p className="text-xs text-red-500 mt-1">{errors.foodItemId.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">嘗試日期</label>
            <input
              type="date"
              {...register("introducedDate")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.introducedDate && (
              <p className="text-xs text-red-500 mt-1">{errors.introducedDate.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">反應（選填）</label>
            <select
              {...register("reaction")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">未填寫</option>
              {reactionOptions.map((o) => (
                <option key={o} value={o}>
                  {o}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "儲存中…" : "記錄嘗試"}
          </button>
        </form>
      )}

      {/* Recent logs */}
      {logs.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">最近紀錄</h2>
          <div className="space-y-2">
            {logs.slice(0, 5).map((l) => (
              <div
                key={l.id}
                className="bg-white rounded-xl border border-gray-100 shadow-sm px-4 py-3 flex items-center justify-between"
              >
                <div>
                  <p className="text-sm font-medium text-gray-800">{l.foodName}</p>
                  <p className="text-xs text-gray-500">{l.introducedDate}</p>
                </div>
                {l.reaction && (
                  <span className="text-xs bg-orange-50 text-orange-700 px-2 py-1 rounded-full">
                    {l.reaction}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Catalogue */}
      <div>
        <h2 className="text-sm font-semibold text-gray-700 mb-3">副食品目錄</h2>
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="搜尋食物或類別…"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
        />

        <div className="space-y-4">
          {Object.entries(grouped).map(([category, items]) => (
            <div key={category} className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
              <div className="bg-orange-50 px-4 py-2 border-b border-orange-100">
                <span className="text-sm font-semibold text-orange-700">{category}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {items.map((item) => {
                  const tried = triedIds.has(item.id);
                  return (
                    <div key={item.id} className="flex items-center gap-3 px-4 py-3">
                      <div
                        className={`w-5 h-5 rounded-full flex items-center justify-center text-xs flex-shrink-0 ${
                          tried ? "bg-green-500 text-white" : "bg-gray-100 text-gray-400"
                        }`}
                      >
                        {tried ? "✓" : "·"}
                      </div>
                      <div className="flex-1 min-w-0">
                        <span className="text-sm text-gray-800">{item.name}</span>
                        <span className="text-xs text-gray-400 ml-2">{item.recommendedAgeMonths}+ 個月</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}

          {filteredCatalogue.length === 0 && (
            <div className="text-center py-12 text-gray-400">
              <p className="text-3xl mb-2">🥕</p>
              <p className="text-sm">找不到符合的食物</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
