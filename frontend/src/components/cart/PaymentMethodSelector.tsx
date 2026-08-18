import { Button } from "@/components/ui/Button";
import type { PaymentMethod, PaymentMethodOption } from "@/types/payment.types";

export interface PaymentMethodSelectorProps {
  value: PaymentMethod;
  onChange: (value: PaymentMethod) => void;
}

const OPTIONS: PaymentMethodOption[] = [
  { id: "KAKAO_PAY", label: "카카오페이" },
  { id: "TOSS_PAY", label: "토스페이" },
  { id: "CARD", label: "카드결제" },
];

/** 결제 수단 선택 (F-03) */
export function PaymentMethodSelector({ value, onChange }: PaymentMethodSelectorProps) {
  return (
    <div className="flex flex-col gap-2" role="radiogroup" aria-label="결제 수단">
      {OPTIONS.map((option) => (
        <Button
          key={option.id}
          type="button"
          variant={value === option.id ? "primary" : "outline"}
          size="lg"
          fullWidth
          role="radio"
          aria-checked={value === option.id}
          onClick={() => onChange(option.id)}
          className="justify-start"
        >
          {option.label}
        </Button>
      ))}
    </div>
  );
}
