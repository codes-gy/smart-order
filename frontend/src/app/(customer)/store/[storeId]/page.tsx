import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { storeApi } from "@/api/storeApi";
import { StoreDetailRouter } from "@/routers/StoreDetailRouter";
import { buildStoreJsonLd } from "@/utils/structuredData";
import { ApiError } from "@/types/common.types";
import type { StoreDetail } from "@/types/store.types";

async function loadStore(storeId: string): Promise<StoreDetail> {
  try {
    return await storeApi.detail(storeId);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}

export async function generateMetadata({
  params,
}: PageProps<"/store/[storeId]">): Promise<Metadata> {
  const { storeId } = await params;
  const store = await loadStore(storeId);

  return {
    title: store.name,
    description: `${store.name} · 실시간 대기 ${store.waitingOrderCount}건, 예상 조리 ${store.estimatedPrepMinutes}분. ${store.description}`,
    openGraph: {
      title: store.name,
      description: store.description,
      type: "website",
    },
  };
}

export default async function StoreDetailPage({ params }: PageProps<"/store/[storeId]">) {
  const { storeId } = await params;
  const store = await loadStore(storeId);
  const jsonLd = buildStoreJsonLd(store);

  return (
    <>
      {/* PRD 5.1: 구조화된 데이터(JSON-LD)로 검색 엔진 노출 최적화 */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <StoreDetailRouter initialStore={store} />
    </>
  );
}
