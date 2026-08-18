"use client";

import { useRef, type ClipboardEvent, type KeyboardEvent } from "react";
import { cn } from "@/utils/cn";

export interface OtpInputFieldProps {
  value: string;
  onChange: (value: string) => void;
  length?: number;
  error?: boolean;
  disabled?: boolean;
  autoFocus?: boolean;
}

/** 4자리(기본값) 숫자 OTP 입력 필드. 자동 포커스 이동/붙여넣기를 지원한다 */
export function OtpInputField({
  value,
  onChange,
  length = 4,
  error = false,
  disabled = false,
  autoFocus = false,
}: OtpInputFieldProps) {
  const inputRefs = useRef<Array<HTMLInputElement | null>>([]);
  const digits = Array.from({ length }, (_, index) => value[index] ?? "");

  const setDigitAt = (index: number, digit: string) => {
    const next = [...digits];
    next[index] = digit;
    onChange(next.join(""));
  };

  const handleChange = (index: number, raw: string) => {
    const digit = raw.replace(/[^0-9]/g, "").slice(-1);
    setDigitAt(index, digit);
    if (digit && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>) => {
    const pasted = event.clipboardData.getData("text").replace(/[^0-9]/g, "").slice(0, length);
    if (!pasted) return;
    event.preventDefault();
    onChange(pasted);
    const lastFilledIndex = Math.max(pasted.length - 1, 0);
    inputRefs.current[lastFilledIndex]?.focus();
  };

  return (
    <div className="flex justify-center gap-2" role="group" aria-label="인증번호 입력">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(node) => {
            inputRefs.current[index] = node;
          }}
          value={digit}
          onChange={(event) => handleChange(index, event.target.value)}
          onKeyDown={(event) => handleKeyDown(index, event)}
          onPaste={handlePaste}
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={1}
          disabled={disabled}
          autoFocus={autoFocus && index === 0}
          aria-label={`인증번호 ${index + 1}번째 자리`}
          className={cn(
            "h-14 w-12 rounded-xl border bg-surface text-center text-xl font-semibold text-foreground outline-none disabled:opacity-50",
            error ? "border-danger" : "border-border focus:border-accent",
          )}
        />
      ))}
    </div>
  );
}
