import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/utils/cn";

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "prefix"> {
  label?: string;
  errorMessage?: string;
  helperText?: string;
  leadingSlot?: ReactNode;
  trailingSlot?: ReactNode;
  containerClassName?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    label,
    errorMessage,
    helperText,
    leadingSlot,
    trailingSlot,
    className,
    containerClassName,
    id,
    ...rest
  },
  ref,
) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;
  const hasError = Boolean(errorMessage);

  return (
    <div className={cn("flex flex-col gap-1.5", containerClassName)}>
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-foreground">
          {label}
        </label>
      )}
      <div
        className={cn(
          "flex h-12 items-center gap-2 rounded-xl border bg-surface px-3",
          hasError ? "border-danger" : "border-border focus-within:border-accent",
        )}
      >
        {leadingSlot && <span className="shrink-0 text-foreground/60">{leadingSlot}</span>}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={hasError}
          aria-describedby={hasError ? errorId : helperText ? helperId : undefined}
          className={cn(
            "h-full min-w-0 flex-1 bg-transparent text-base text-foreground outline-none",
            "placeholder:text-foreground/40",
            className,
          )}
          {...rest}
        />
        {trailingSlot && <span className="shrink-0 text-foreground/60">{trailingSlot}</span>}
      </div>
      {hasError ? (
        <p id={errorId} role="alert" className="text-sm text-danger">
          {errorMessage}
        </p>
      ) : helperText ? (
        <p id={helperId} className="text-sm text-foreground/60">
          {helperText}
        </p>
      ) : null}
    </div>
  );
});
