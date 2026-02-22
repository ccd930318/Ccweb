"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { inviteApi } from "../../../lib/api";
import { useAuth } from "../../../lib/auth";

type Status = "idle" | "loading" | "success" | "error" | "needsLogin";

export default function AcceptInvitePage() {
  const { token } = useParams<{ token: string }>();
  const { accessToken, isLoading } = useAuth();
  const router = useRouter();
  const [status, setStatus] = useState<Status>("idle");
  const [message, setMessage] = useState("");

  // Once auth is determined, redirect unauthenticated users to login
  useEffect(() => {
    if (!isLoading && !accessToken) {
      router.replace(`/login?redirect=/invite/${token}`);
    }
  }, [isLoading, accessToken, token, router]);

  const handleAccept = async () => {
    setStatus("loading");
    try {
      await inviteApi.accept(token);
      setStatus("success");
      setMessage("邀請已接受！正在前往首頁…");
      setTimeout(() => router.push("/dashboard"), 2000);
    } catch {
      setStatus("error");
      setMessage("邀請連結無效或已過期");
    }
  };

  if (isLoading || !accessToken) return null;

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow p-8 space-y-6 text-center">
        {status === "success" ? (
          <>
            <p className="text-5xl">🎉</p>
            <p className="font-semibold text-gray-800">已成功加入！</p>
            <p className="text-sm text-gray-500">{message}</p>
          </>
        ) : status === "error" ? (
          <>
            <p className="text-5xl">😕</p>
            <p className="font-semibold text-gray-800">邀請失敗</p>
            <p className="text-sm text-red-500">{message}</p>
            <button
              onClick={() => router.push("/dashboard")}
              className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm hover:bg-blue-700"
            >
              返回首頁
            </button>
          </>
        ) : (
          <>
            <p className="text-5xl">👶</p>
            <p className="font-semibold text-gray-800">您收到了家庭邀請</p>
            <p className="text-sm text-gray-500">
              接受邀請後，您將可以查看或編輯寶寶的成長資料。
            </p>
            <button
              onClick={handleAccept}
              disabled={status === "loading"}
              className="w-full bg-blue-600 text-white font-medium rounded-lg py-3 text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {status === "loading" ? "處理中…" : "接受邀請"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
