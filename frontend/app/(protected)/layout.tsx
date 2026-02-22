"use client";

import { useAuth } from "../../lib/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import Link from "next/link";

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, isLoading, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !accessToken) {
      router.replace("/login");
    }
  }, [accessToken, isLoading, router]);

  if (isLoading || !accessToken) return null;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link href="/dashboard" className="text-lg font-bold text-blue-600">
            BabyTracker
          </Link>
          <nav className="flex items-center gap-4 text-sm text-gray-600">
            <Link href="/articles" className="hover:text-blue-600">文章</Link>
            <button onClick={logout} className="hover:text-blue-600">登出</button>
          </nav>
        </div>
      </header>
      <main className="max-w-2xl mx-auto px-4 py-6">{children}</main>
    </div>
  );
}
