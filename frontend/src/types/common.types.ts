/**
 * PRD 4.3 커서 기반 페이징 응답 규격.
 * 신규 데이터가 추가되어도 오프셋 밀림(중복/누락)이 발생하지 않도록
 * 모든 목록형 API 응답은 이 포맷을 따른다.
 */
export interface CursorResponseMeta {
  nextCursor: string | null;
  hasNext: boolean;
}

export interface CursorResponse<T> {
  result: T[];
  meta: CursorResponseMeta;
}

/** 서버가 내려주는 에러 바디 공통 규격 */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Record<string, string[]>;
}

/** apiClient가 던지는 공통 에러 타입 (4xx/5xx, 네트워크 유실 포함) */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: Record<string, string[]>;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.status = status;
    this.code = body.code;
    this.details = body.details;
  }

  /** 네트워크 유실(오프라인, 타임아웃 등) 여부 */
  get isNetworkError(): boolean {
    return this.status === 0;
  }
}

export type Nullable<T> = T | null;

export type ID = string;

/**
 * SSE 훅이 노출하는 연결 상태.
 * PRD 4.1 재연결 정책(Exponential Backoff → 3회 실패 시 REST Polling 폴백)과 매핑된다.
 */
export type ConnectionStatus =
  | "connecting"
  | "open"
  | "reconnecting"
  | "polling"
  | "closed";

/** UI 4대 상태(성공/로딩/에러/데이터 없음) 분기에 쓰는 공통 판별 유니온 */
export type AsyncState<T> =
  | { status: "loading" }
  | { status: "error"; error: ApiError | Error }
  | { status: "empty" }
  | { status: "success"; data: T };
