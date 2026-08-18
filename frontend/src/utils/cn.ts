/**
 * 외부 라이브러리(clsx, tailwind-merge) 없이 구현한 최소 className 병합 유틸.
 * 조건부/중첩 배열 클래스명을 하나의 문자열로 합쳐준다.
 *
 * 주의: tailwind-merge처럼 충돌하는 유틸리티 클래스(z.B. "p-2 p-4")를
 * 자동으로 제거해주지는 않는다. 실제로 충돌 케이스가 자주 발생하면
 * `tailwind-merge` 추가 설치를 검토할 것 (현재는 무의존성 원칙에 따라 보류).
 */
type ClassValue =
  | string
  | number
  | null
  | boolean
  | undefined
  | ClassValue[];

function flatten(input: ClassValue, output: string[]): void {
  if (input === null || input === undefined || input === false) return;
  if (typeof input === "string") {
    if (input.trim().length > 0) output.push(input);
    return;
  }
  if (typeof input === "number") {
    output.push(String(input));
    return;
  }
  if (Array.isArray(input)) {
    for (const item of input) flatten(item, output);
  }
}

export function cn(...inputs: ClassValue[]): string {
  const output: string[] = [];
  for (const input of inputs) flatten(input, output);
  return output.join(" ");
}
