import { StickyTabs } from "@/components/ui/StickyTabs";
import type { MenuCategory } from "@/types/menu.types";

export interface CategoryTabsProps {
  categories: MenuCategory[];
  value: string;
  onChange: (id: string) => void;
  stickyOffsetClassName?: string;
}

/** 메뉴 카테고리 스티키 탭 (F-02) */
export function CategoryTabs({ categories, value, onChange, stickyOffsetClassName }: CategoryTabsProps) {
  return (
    <StickyTabs
      items={categories.map((category) => ({ id: category.id, label: category.name }))}
      value={value}
      onChange={onChange}
      stickyOffsetClassName={stickyOffsetClassName}
    />
  );
}
