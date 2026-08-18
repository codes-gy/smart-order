"use client";

import { useMemo } from "react";
import type { MenuItem } from "@/types/menu.types";

/** 카테고리 + 검색어로 메뉴 목록을 필터링한다 (F-02) */
export function useMenuSearch(items: MenuItem[], categoryId: string, keyword: string): MenuItem[] {
  return useMemo(() => {
    const trimmed = keyword.trim();
    return items.filter((item) => {
      const matchesCategory = item.categoryId === categoryId;
      const matchesKeyword = trimmed ? item.name.includes(trimmed) : true;
      return matchesCategory && matchesKeyword;
    });
  }, [items, categoryId, keyword]);
}
