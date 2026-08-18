/** Vibration API 안전 래퍼. 미지원 브라우저(iOS Safari 등)에서도 에러 없이 조용히 무시된다 */
export function vibrate(pattern: number | number[]): void {
  if (typeof navigator === "undefined" || typeof navigator.vibrate !== "function") return;
  navigator.vibrate(pattern);
}
