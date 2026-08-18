"use client";

import { useEffect, useState } from "react";

/** 입력값을 delayMs만큼 지연시켜 반환한다 (검색어 입력 등에서 과도한 재요청 방지) */
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
