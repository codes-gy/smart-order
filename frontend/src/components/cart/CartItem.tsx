import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { IconX } from "@/components/ui/icons";
import type { CartLineItem } from "@/types/cart.types";

export interface CartItemProps {
  line: CartLineItem;
  onQuantityChange: (lineId: string, quantity: number) => void;
  onRemove: (lineId: string) => void;
}

/** 장바구니 라인 아이템 카드 (F-03) */
export function CartItem({ line, onQuantityChange, onRemove }: CartItemProps) {
  const optionText = line.optionSummary.flatMap((summary) => summary.choiceLabels).join(", ");

  return (
    <Card className="space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate font-semibold text-foreground">{line.menuName}</p>
          {optionText && <p className="truncate text-sm text-foreground/60">{optionText}</p>}
        </div>
        <button
          type="button"
          onClick={() => onRemove(line.lineId)}
          aria-label={`${line.menuName} 삭제`}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-foreground/50 hover:bg-surface-muted"
        >
          <IconX className="h-4 w-4" />
        </button>
      </div>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => onQuantityChange(line.lineId, line.quantity - 1)}
            aria-label={`${line.menuName} 수량 감소`}
          >
            -
          </Button>
          <span className="w-6 text-center font-semibold text-foreground">{line.quantity}</span>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => onQuantityChange(line.lineId, line.quantity + 1)}
            aria-label={`${line.menuName} 수량 증가`}
          >
            +
          </Button>
        </div>
        <p className="font-semibold text-foreground">{(line.unitPrice * line.quantity).toLocaleString()}원</p>
      </div>
    </Card>
  );
}
