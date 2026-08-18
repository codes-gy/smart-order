/** 키보드 사용자가 헤더/내비게이션을 건너뛰고 바로 본문으로 이동할 수 있게 하는 접근성 링크 */
export function SkipLink() {
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[200] focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-primary-foreground"
    >
      본문으로 바로가기
    </a>
  );
}
