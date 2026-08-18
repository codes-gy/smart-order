import type { StoreDetail } from "@/types/store.types";

export interface LocalBusinessJsonLd {
  "@context": "https://schema.org";
  "@type": "CafeOrCoffeeShop";
  name: string;
  address: { "@type": "PostalAddress"; streetAddress: string };
  telephone: string;
  openingHours: string;
  geo: { "@type": "GeoCoordinates"; latitude: number; longitude: number };
}

/** PRD 5.1: 매장 상세 페이지 JSON-LD (LocalBusiness/CafeOrCoffeeShop 스키마) */
export function buildStoreJsonLd(store: StoreDetail): LocalBusinessJsonLd {
  return {
    "@context": "https://schema.org",
    "@type": "CafeOrCoffeeShop",
    name: store.name,
    address: { "@type": "PostalAddress", streetAddress: store.address },
    telephone: store.phoneNumber,
    openingHours: store.businessHours,
    geo: {
      "@type": "GeoCoordinates",
      latitude: store.location.lat,
      longitude: store.location.lng,
    },
  };
}
