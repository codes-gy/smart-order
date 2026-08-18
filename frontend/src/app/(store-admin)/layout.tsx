import type { ReactNode } from "react";

/**
 * 매장(POS/태블릿) 라우트 그룹 공통 레이아웃.
 * PRD 5.2: 매장용은 시스템 설정과 무관하게 고대비 다크모드를 고정 적용한다 (번인/난반사 방지).
 */
export default function StoreAdminLayout({ children }: { children: ReactNode }) {
  return <div className="admin-high-contrast min-h-screen bg-background text-foreground">{children}</div>;
}
