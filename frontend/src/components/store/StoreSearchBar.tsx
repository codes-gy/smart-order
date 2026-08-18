"use client";

import { Input } from "@/components/ui/Input";

export interface StoreSearchBarProps {
  value: string;
  onChange: (value: string) => void;
}

export function StoreSearchBar({ value, onChange }: StoreSearchBarProps) {
  return (
    <div className="px-4 py-2">
      <Input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="매장명으로 검색"
        aria-label="매장 검색"
      />
    </div>
  );
}
