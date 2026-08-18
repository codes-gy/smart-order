import type { SVGProps } from "react";
import { cn } from "@/utils/cn";

/**
 * 외부 아이콘 라이브러리(lucide-react 등) 없이 직접 작성한 최소 SVG 아이콘 세트.
 * 모든 아이콘은 24x24 기준, stroke 기반이며 className으로 크기/색상을 제어한다.
 */
type IconProps = SVGProps<SVGSVGElement>;

function baseProps(className?: string): IconProps {
  return {
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    className: cn("h-5 w-5", className),
    "aria-hidden": true,
  };
}

export function IconChevronLeft({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M15 18l-6-6 6-6" />
    </svg>
  );
}

export function IconX({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  );
}

export function IconSun({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </svg>
  );
}

export function IconMoon({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
    </svg>
  );
}

export function IconMonitor({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <rect x="2" y="4" width="20" height="13" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </svg>
  );
}

export function IconAlertTriangle({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
      <path d="M12 9v4M12 17h.01" />
    </svg>
  );
}

export function IconCheckCircle({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="12" cy="12" r="10" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

export function IconXCircle({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="12" cy="12" r="10" />
      <path d="m15 9-6 6M9 9l6 6" />
    </svg>
  );
}

export function IconInfo({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 16v-4M12 8h.01" />
    </svg>
  );
}

export function IconHome({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.5V21h14V9.5" />
    </svg>
  );
}

export function IconReceipt({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <path d="M6 2h12v20l-3-2-3 2-3-2-3 2Z" />
      <path d="M9 8h6M9 12h6" />
    </svg>
  );
}

export function IconUser({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}

export function IconShoppingCart({ className }: { className?: string }) {
  return (
    <svg {...baseProps(className)}>
      <circle cx="9" cy="21" r="1" />
      <circle cx="19" cy="21" r="1" />
      <path d="M3 3h2l2.4 12.2a2 2 0 0 0 2 1.8h7.6a2 2 0 0 0 2-1.6L21 8H6" />
    </svg>
  );
}
