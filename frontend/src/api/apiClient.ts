import { ApiError, type ApiErrorBody } from "@/types/common.types";
import { useAuthStore } from "@/stores/authStore";

type HttpMethod = "GET" | "POST" | "PATCH" | "PUT" | "DELETE";

export interface RequestConfig<TBody = never> {
  method?: HttpMethod;
  body?: TBody;
  headers?: Record<string, string>;
  /** true면 X-Idempotency-Key를 자동 생성해 헤더에 포함한다 (예: 주문 생성 POST /orders) */
  idempotent?: boolean;
  /** true면 Authorization 헤더를 붙이지 않는다 (로그인/SMS 인증 등 인증 전 호출) */
  skipAuth?: boolean;
  signal?: AbortSignal;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

let refreshInFlight: Promise<string> | null = null;

async function parseJsonSafely<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) return null;
  return JSON.parse(text) as T;
}

/**
 * 여러 요청이 동시에 401을 맞아도 /auth/refresh는 한 번만 호출되도록
 * in-flight Promise를 공유한다 (Silent Refresh).
 */
async function refreshAccessToken(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = useAuthStore.getState().tokens?.refreshToken;
      if (!refreshToken) {
        throw new ApiError(401, { code: "NO_REFRESH_TOKEN", message: "세션이 만료되었습니다. 다시 로그인해주세요." });
      }

      const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: { Authorization: `Bearer ${refreshToken}` },
      });

      if (!response.ok) {
        useAuthStore.getState().clearSession();
        throw new ApiError(response.status, {
          code: "REFRESH_FAILED",
          message: "세션이 만료되었습니다. 다시 로그인해주세요.",
        });
      }

      const data = await parseJsonSafely<{ accessToken: string }>(response);
      if (!data?.accessToken) {
        useAuthStore.getState().clearSession();
        throw new ApiError(response.status, {
          code: "REFRESH_FAILED",
          message: "세션이 만료되었습니다. 다시 로그인해주세요.",
        });
      }

      useAuthStore.getState().setAccessToken(data.accessToken);
      return data.accessToken;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

function buildHeaders<TBody>(config: RequestConfig<TBody>): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...config.headers,
  };

  if (config.idempotent) {
    headers["X-Idempotency-Key"] = crypto.randomUUID();
  }

  if (!config.skipAuth) {
    const accessToken = useAuthStore.getState().tokens?.accessToken;
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  }

  return headers;
}

async function performFetch<TBody>(
  path: string,
  method: HttpMethod,
  headers: Record<string, string>,
  body: TBody | undefined,
  signal: AbortSignal | undefined,
): Promise<Response> {
  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch {
    // fetch 자체가 실패하는 경우 = 오프라인/네트워크 유실/타임아웃
    throw new ApiError(0, { code: "NETWORK_ERROR", message: "네트워크 연결을 확인해주세요." });
  }
}

/**
 * 실제 백엔드 연동용 공통 fetch 래퍼.
 * 현재 단계(Mock)에서는 각 도메인 api 모듈이 src/api/mock/* 에 직접 위임하고 있어
 * 이 함수는 아직 실호출되지 않지만, 연동 시점에 도메인 api 함수 본문만
 * `apiRequest(...)` 호출로 바꾸면 되도록 최종 인터페이스에 맞춰 구현해두었다.
 */
export async function apiRequest<TResponse, TBody = never>(
  path: string,
  config: RequestConfig<TBody> = {},
): Promise<TResponse> {
  const { method = "GET", body, signal, skipAuth } = config;
  const headers = buildHeaders(config);

  let response = await performFetch(path, method, headers, body, signal);

  if (response.status === 401 && !skipAuth) {
    const newAccessToken = await refreshAccessToken();
    const retryHeaders = { ...headers, Authorization: `Bearer ${newAccessToken}` };
    response = await performFetch(path, method, retryHeaders, body, signal);
  }

  if (!response.ok) {
    const errorBody = await parseJsonSafely<ApiErrorBody>(response);
    throw new ApiError(
      response.status,
      errorBody ?? { code: "UNKNOWN_ERROR", message: "요청을 처리하지 못했습니다." },
    );
  }

  if (response.status === 204) {
    return undefined as TResponse;
  }

  const data = await parseJsonSafely<TResponse>(response);
  return data as TResponse;
}
