"use client";

import { useEffect, useState } from "react";
import { articleApi, Article } from "../../../lib/api";

export default function ArticlesPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [selected, setSelected] = useState<Article | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    articleApi.list().then((r) => setArticles(r.data));
  }, []);

  const filtered = articles.filter(
    (a) =>
      a.title.toLowerCase().includes(search.toLowerCase()) ||
      a.category.toLowerCase().includes(search.toLowerCase())
  );

  if (selected) {
    return (
      <div className="space-y-4">
        <button
          onClick={() => setSelected(null)}
          className="flex items-center gap-1 text-sm text-blue-600 hover:underline"
        >
          ← 返回文章列表
        </button>
        <article className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          <div>
            <span className="text-xs bg-blue-50 text-blue-700 px-2 py-1 rounded-full">
              {selected.category}
            </span>
          </div>
          <h1 className="text-xl font-bold text-gray-900">{selected.title}</h1>
          <p className="text-xs text-gray-400">{selected.createdAt.slice(0, 10)}</p>
          <div className="prose prose-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
            {selected.content}
          </div>
        </article>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-gray-800">育兒文章</h1>

      <input
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="搜尋文章標題或類別…"
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      />

      {filtered.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-3xl mb-2">📖</p>
          <p className="text-sm">目前沒有文章</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((a) => (
            <button
              key={a.id}
              onClick={() => setSelected(a)}
              className="w-full text-left bg-white rounded-2xl border border-gray-100 shadow-sm p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="space-y-1">
                  <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full">
                    {a.category}
                  </span>
                  <p className="text-sm font-semibold text-gray-800 leading-snug">{a.title}</p>
                  <p className="text-xs text-gray-400">{a.createdAt.slice(0, 10)}</p>
                </div>
                <span className="text-gray-300 text-lg flex-shrink-0">›</span>
              </div>
              <p className="text-xs text-gray-500 mt-2 line-clamp-2">{a.content.slice(0, 120)}</p>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
