import type { Metadata, Viewport } from "next";
import { Providers } from "./providers";
import { SkipLink } from "@/components/ui/SkipLink";
import { ServiceWorkerRegister } from "@/components/ServiceWorkerRegister";
import { THEME_STORAGE_KEY } from "@/utils/constants";
import "./globals.css";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Smart Order - 카페 실시간 선주문 & 픽업",
    template: "%s | Smart Order",
  },
  description:
    "출근길 이동 중 음료를 미리 주문하고 대기 없이 픽업하는 카페 실시간 선주문 & 픽업 플랫폼",
  openGraph: {
    type: "website",
    locale: "ko_KR",
    siteName: "Smart Order",
    title: "Smart Order - 카페 실시간 선주문 & 픽업",
    description:
      "출근길 이동 중 음료를 미리 주문하고 대기 없이 픽업하는 카페 실시간 선주문 & 픽업 플랫폼",
  },
  robots: {
    index: true,
    follow: true,
  },
  icons: {
    icon: [
      { url: "/icons/icon.svg", type: "image/svg+xml" },
      { url: "/icons/icon-32.png", sizes: "32x32", type: "image/png" },
    ],
    apple: [{ url: "/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
    { media: "(prefers-color-scheme: dark)", color: "#0a0a0a" },
  ],
};

/**
 * 최초 페인트 이전에 <html>에 다크모드 클래스를 동기적으로 적용해 FOUC(테마 깜빡임)를 막는다.
 * themeStore(zustand persist, Phase 1)가 같은 THEME_STORAGE_KEY에 { state: { mode } } 형태로 저장하는 것을 전제로 한다.
 * mode: "light" | "dark" | "system"
 */
function buildThemeInitScript(storageKey: string): string {
  return `(function(){try{
    var raw = localStorage.getItem(${JSON.stringify(storageKey)});
    var mode = "system";
    if (raw) {
      var parsed = JSON.parse(raw);
      if (parsed && parsed.state && typeof parsed.state.mode === "string") {
        mode = parsed.state.mode;
      }
    }
    var prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    var isDark = mode === "dark" || (mode === "system" && prefersDark);
    document.documentElement.classList.toggle("dark", isDark);
  } catch (e) {} })();`;
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{ __html: buildThemeInitScript(THEME_STORAGE_KEY) }}
        />
      </head>
      <body className="min-h-full flex flex-col bg-background text-foreground" suppressHydrationWarning>
        <SkipLink />
        <ServiceWorkerRegister />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
