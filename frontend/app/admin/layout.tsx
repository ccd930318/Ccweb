"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import Cookies from "js-cookie";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (pathname === "/admin/login") {
      setReady(true);
      return;
    }
    const token = Cookies.get("adminToken");
    if (!token) {
      router.replace("/admin/login");
      return;
    }
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      if (payload.role !== "ADMIN" || payload.exp * 1000 < Date.now()) {
        Cookies.remove("adminToken");
        router.replace("/admin/login");
        return;
      }
    } catch {
      Cookies.remove("adminToken");
      router.replace("/admin/login");
      return;
    }
    setReady(true);
  }, [pathname, router]);

  const logout = () => {
    Cookies.remove("adminToken");
    router.push("/admin/login");
  };

  if (!ready) return null;

  if (pathname === "/admin/login") return <>{children}</>;

  const navItems = [
    { href: "/admin/dashboard", label: "儀表板" },
    { href: "/admin/users", label: "使用者管理" },
  ];

  return (
    <div className="min-h-screen flex bg-gray-100">
      {/* Sidebar */}
      <aside className="w-56 bg-gray-900 text-white flex flex-col">
        <div className="px-6 py-5 border-b border-gray-700">
          <h1 className="text-lg font-bold">後台管理</h1>
          <p className="text-xs text-gray-400 mt-0.5">Admin Panel</p>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`block px-3 py-2 rounded-lg text-sm transition-colors ${
                pathname === item.href
                  ? "bg-indigo-600 text-white"
                  : "text-gray-300 hover:bg-gray-800"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="px-3 py-4 border-t border-gray-700">
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-lg text-sm text-gray-300 hover:bg-gray-800 transition-colors"
          >
            登出
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-8 overflow-auto">{children}</main>
    </div>
  );
}
