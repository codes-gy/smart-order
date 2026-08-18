import { Button } from "@/components/ui/Button";
import type { PackagingType } from "@/types/cart.types";

export interface PackagingSelectorProps {
  value: PackagingType;
  onChange: (value: PackagingType) => void;
}

const OPTIONS: { value: PackagingType; label: string }[] = [
  { value: "TAKE_OUT", label: "포장" },
  { value: "DINE_IN", label: "매장" },
];

/** 포장/매장 선택 토글 (F-03) */
export function PackagingSelector({ value, onChange }: PackagingSelectorProps) {
  return (
    <div className="flex gap-2" role="radiogroup" aria-label="포장 방식">
      {OPTIONS.map((option) => (
        <Button
          key={option.value}
          type="button"
          variant={value === option.value ? "primary" : "outline"}
          size="md"
          className="flex-1"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </Button>
      ))}
    </div>
  );
}
