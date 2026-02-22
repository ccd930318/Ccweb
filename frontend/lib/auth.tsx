"use client";

import { createContext, useContext, useEffect, useState, useCallback } from "react";
import Cookies from "js-cookie";
import { authApi } from "./api";

interface AuthState {
  accessToken: string | null;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setToken: (token: string) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ accessToken: null, isLoading: true });

  useEffect(() => {
    const token = Cookies.get("accessToken");
    setState({ accessToken: token ?? null, isLoading: false });
  }, []);

  const setToken = useCallback((token: string) => {
    Cookies.set("accessToken", token, { expires: 1 / 96 }); // 15 min
    setState({ accessToken: token, isLoading: false });
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password });
    setToken(res.data.accessToken);
  }, [setToken]);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    Cookies.remove("accessToken");
    setState({ accessToken: null, isLoading: false });
    window.location.href = "/login";
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, logout, setToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be inside AuthProvider");
  return ctx;
}
