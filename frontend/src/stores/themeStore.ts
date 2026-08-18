import { create } from "zustand";
import { persist } from "zustand/middleware";
import { THEME_STORAGE_KEY } from "@/utils/constants";

export type ThemeMode = "light" | "dark" | "system";

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
}

/**
 * 사용자가 선택한 테마 "선호값"만 저장한다 (system 포함).
 * 실제로 화면에 적용되는 dark/light 여부는 useTheme 훅에서 matchMedia와 조합해 계산한다.
 * persist 키(THEME_STORAGE_KEY)는 src/app/layout.tsx의 FOUC 방지 스크립트와 반드시 동일해야 한다.
 */
export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: "system",
      setMode: (mode) => set({ mode }),
    }),
    {
      name: THEME_STORAGE_KEY,
    },
  ),
);
