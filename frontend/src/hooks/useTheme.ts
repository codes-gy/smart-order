"use client";

import { useEffect, useState } from "react";
import { useThemeStore, type ThemeMode } from "@/stores/themeStore";

export interface UseThemeResult {
  mode: ThemeMode;
  /** 실제로 화면에 적용된 테마 (system이면 matchMedia 결과로 해석됨) */
  resolvedTheme: "light" | "dark";
  setMode: (mode: ThemeMode) => void;
}

/**
 * themeStore의 선호값(mode)과 시스템 다크모드 설정을 조합해
 * <html> 요소에 실제 "dark" 클래스를 동기화하는 훅.
 * layout.tsx의 인라인 스크립트가 최초 페인트 전 클래스를 이미 세팅해두므로,
 * 여기서는 이후의 변경(수동 토글, 시스템 설정 변경)만 반영하면 된다.
 */
export function useTheme(): UseThemeResult {
  const mode = useThemeStore((state) => state.mode);
  const setMode = useThemeStore((state) => state.setMode);
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  });

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handleChange = (event: MediaQueryListEvent) => setSystemPrefersDark(event.matches);
    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, []);

  const resolvedTheme: "light" | "dark" =
    mode === "system" ? (systemPrefersDark ? "dark" : "light") : mode;

  useEffect(() => {
    document.documentElement.classList.toggle("dark", resolvedTheme === "dark");
  }, [resolvedTheme]);

  return { mode, resolvedTheme, setMode };
}
