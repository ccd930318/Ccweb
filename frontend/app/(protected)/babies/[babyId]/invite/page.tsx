"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { inviteApi } from "../../../../../lib/api";

const schema = z.object({
  email: z.string().email("請輸入有效的電子信箱"),
  role: z.enum(["viewer", "editor"]),
});

type FormData = z.infer<typeof schema>;

export default function InvitePage() {
  const { babyId } = useParams<{ babyId: string }>();
  const [inviteToken, setInviteToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: "viewer" },
  });

  const onSubmit = async (data: FormData) => {
    try {
      setError(null);
      const res = await inviteApi.create(babyId, {
        email: data.email,
        role: data.role,
      });
      setInviteToken(res.data.token);
    } catch {
      setError("邀請建立失敗，請確認您是寶寶的擁有者");
    }
  };

  const inviteLink =
    inviteToken && typeof window !== "undefined"
      ? `${window.location.origin}/invite/${inviteToken}`
      : null;

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-gray-800">邀請家人</h1>

      {inviteLink ? (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          <div className="text-center">
            <p className="text-3xl mb-2">🎉</p>
            <p className="font-semibold text-gray-800">邀請連結已產生！</p>
            <p className="text-sm text-gray-500 mt-1">請將以下連結傳送給對方</p>
          </div>

          <div className="bg-gray-50 rounded-xl p-3 break-all text-sm text-blue-600 font-mono select-all">
            {inviteLink}
          </div>

          <button
            onClick={() => {
              navigator.clipboard.writeText(inviteLink);
            }}
            className="w-full border border-gray-300 text-gray-700 rounded-lg py-2 text-sm hover:bg-gray-50"
          >
            複製連結
          </button>

          <button
            onClick={() => setInviteToken(null)}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700"
          >
            再邀請一人
          </button>
        </div>
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 space-y-4"
        >
          {error && (
            <p className="text-sm text-red-600">{error}</p>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              對方的電子信箱
            </label>
            <input
              type="email"
              {...register("email")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="family@example.com"
            />
            {errors.email && (
              <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">權限</label>
            <select
              {...register("role")}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="viewer">只能查看</option>
              <option value="editor">可以編輯</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "建立中…" : "產生邀請連結"}
          </button>
        </form>
      )}
    </div>
  );
}
