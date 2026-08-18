import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

/** 홈/매장 상세만 색인을 허용하고, 로그인·장바구니·주문·매장관리자 등 개인화 페이지는 차단한다 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [
        "/login",
        "/mypage",
        "/cart",
        "/checkout",
        "/orders/",
        "/store-admin/",
      ],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
