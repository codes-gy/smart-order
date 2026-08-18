import { cn } from "@/utils/cn";
import type { MenuOptionGroup } from "@/types/menu.types";

export interface OptionSelectorProps {
  group: MenuOptionGroup;
  isSelected: (choiceId: string) => boolean;
  onToggle: (choiceId: string) => void;
  hasError?: boolean;
}

/** 옵션 그룹 하나(온도/컵종류/샷추가/시럽/우유변경 등)의 선택 UI (F-02) */
export function OptionSelector({ group, isSelected, onToggle, hasError = false }: OptionSelectorProps) {
  return (
    <fieldset className="space-y-2">
      <legend className="flex items-center gap-1 text-sm font-semibold text-foreground">
        {group.name}
        {group.required && (
          <span className="text-danger" aria-hidden="true">
            *
          </span>
        )}
      </legend>
      <div className="flex flex-wrap gap-2" role={group.type === "single" ? "radiogroup" : "group"}>
        {group.choices.map((choice) => {
          const selected = isSelected(choice.id);
          return (
            <button
              key={choice.id}
              type="button"
              role={group.type === "single" ? "radio" : undefined}
              aria-checked={group.type === "single" ? selected : undefined}
              aria-pressed={group.type === "multiple" ? selected : undefined}
              disabled={choice.isSoldOut}
              onClick={() => onToggle(choice.id)}
              className={cn(
                "flex h-10 items-center gap-1 rounded-full border px-3 text-sm font-medium transition-colors disabled:opacity-40",
                selected
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-surface text-foreground",
              )}
            >
              <span>{choice.label}</span>
              {choice.priceDelta !== 0 && (
                <span className="text-xs opacity-80">
                  {choice.priceDelta > 0 ? "+" : ""}
                  {choice.priceDelta.toLocaleString()}원
                </span>
              )}
              {choice.isSoldOut && <span className="text-xs">(품절)</span>}
            </button>
          );
        })}
      </div>
      {hasError && (
        <p role="alert" className="text-xs text-danger">
          필수 옵션을 선택해주세요.
        </p>
      )}
    </fieldset>
  );
}
