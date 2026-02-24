"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { vaccinationApi, VaccineScheduleItem, VaccinationRecord } from "../../../../../lib/api";

const schema = z.object({
  scheduleId: z.number().int().positive("請選擇疫苗"),
  administeredDate: z.string().min(1, "請選擇接種日期"),
  notes: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

export default function VaccinationsPage() {
  const { babyId } = useParams<{ babyId: string }>();
  const [schedule, setSchedule] = useState<VaccineScheduleItem[]>([]);
  const [records, setRecords] = useState<VaccinationRecord[]>([]);
  const [showForm, setShowForm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { administeredDate: new Date().toISOString().slice(0, 10) },
  });

  const fetchData = async () => {
    const [s, r] = await Promise.all([
      vaccinationApi.listSchedule(),
      vaccinationApi.list(babyId),
    ]);
    setSchedule(s.data);
    setRecords(r.data);
  };

  useEffect(() => {
    fetchData();
  }, [babyId]);

  const onSubmit = async (data: FormData) => {
    await vaccinationApi.create(babyId, {
      scheduleId: data.scheduleId,
      administeredDate: data.administeredDate,
      notes: data.notes,
    });
    await fetchData();
    setShowForm(false);
    reset({ administeredDate: new Date().toISOString().slice(0, 10) });
  };

  const administeredIds = new Set(records.map((r) => r.scheduleId));

  // Group schedule by age
  const grouped = schedule.reduce<Record<number, VaccineScheduleItem[]>>((acc, item) => {
    const age = item.recommendedAgeMonths;
    if (!acc[age]) acc[age] = [];
    acc[age].push(item);
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">疫苗接種</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          {showForm ? "取消" : "+ 記錄接種"}
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">疫苗</label>
            <select
              {...register("scheduleId", { valueAsNumber: true })}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">選擇疫苗</option>
              {schedule.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.vaccineName}（第 {s.doseNumber} 劑，{s.recommendedAgeMonths} 個月）
                </option>
              ))}
            </select>
            {errors.scheduleId && (
              <p className="text-xs text-red-500 mt-1">{errors.scheduleId.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">接種日期</label>
            <input
              type="date"
              {...register("administeredDate")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.administeredDate && (
              <p className="text-xs text-red-500 mt-1">{errors.administeredDate.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">備註（選填）</label>
            <input
              type="text"
              {...register("notes")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="接種部位、反應等"
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "儲存中…" : "記錄接種"}
          </button>
        </form>
      )}

      {/* Timeline grouped by age */}
      <div className="space-y-4">
        {Object.entries(grouped)
          .sort(([a], [b]) => Number(a) - Number(b))
          .map(([age, items]) => (
            <div key={age} className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
              <div className="bg-purple-50 px-4 py-2 border-b border-purple-100">
                <span className="text-sm font-semibold text-purple-700">
                  {age} 個月
                </span>
              </div>
              <div className="divide-y divide-gray-50">
                {items.map((item) => {
                  const done = administeredIds.has(item.id);
                  const record = records.find((r) => r.scheduleId === item.id);
                  return (
                    <div key={item.id} className="flex items-center gap-3 px-4 py-3">
                      <div
                        className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 text-xs ${
                          done
                            ? "bg-green-500 text-white"
                            : "bg-gray-200 text-gray-500"
                        }`}
                      >
                        {done ? "✓" : "○"}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-800">
                          {item.vaccineName}
                          <span className="text-gray-400 font-normal ml-1">
                            第 {item.doseNumber} 劑
                          </span>
                        </p>
                        {record && (
                          <p className="text-xs text-gray-500 mt-0.5">
                            已於 {record.administeredDate} 接種
                            {record.notes && ` · ${record.notes}`}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}

        {schedule.length === 0 && (
          <div className="text-center py-12 text-gray-400">
            <p className="text-3xl mb-2">💉</p>
            <p className="text-sm">尚無疫苗排程</p>
          </div>
        )}
      </div>
    </div>
  );
}
