import type { MetadataRoute } from "next";
import { storeApi } from "@/api/storeApi";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

/** 홈과 공개 매장 상세 페이지만 사이트맵에 포함한다 (개인화 페이지는 robots.ts에서 색인 차단) */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const stores = await storeApi.list({ lat: 37.5006, lng: 127.0364, limit: 50 });

  const storeEntries: MetadataRoute.Sitemap = stores.result.map((store) => ({
    url: `${SITE_URL}/store/${store.id}`,
    changeFrequency: "hourly",
    priority: 0.8,
  }));

  return [
    {
      url: SITE_URL,
      changeFrequency: "always",
      priority: 1,
    },
    ...storeEntries,
  ];
}
