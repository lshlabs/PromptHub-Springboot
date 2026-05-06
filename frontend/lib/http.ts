export class HttpRequestError extends Error {
  status: number
  payload: unknown

  constructor(message: string, status: number, payload: unknown) {
    super(message)
    this.name = 'HttpRequestError'
    this.status = status
    this.payload = payload
  }
}

export const resolveApiBaseUrl = (): string => {
  const publicBase = process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || ''
  const internalBase = process.env.NEXT_INTERNAL_API_BASE_URL?.trim() || ''

  // 서버 컴포넌트/라우트에서는 컨테이너 내부 주소를 써야 배포 환경에서 자기 자신을 다시 밖으로 돌지 않는다.
  const base = typeof window === 'undefined' ? internalBase || publicBase : publicBase

  if (!base) {
    throw new Error('Missing required environment variable: NEXT_PUBLIC_API_BASE_URL')
  }

  return base.replace(/\/+$/, '')
}

export const buildApiUrl = (path: string): string => {
  if (/^https?:\/\//i.test(path)) {
    return path
  }
  // 백엔드는 후행 슬래시 없는 canonical URL을 기준으로 맞춰 둬서 여기서는 앞쪽 슬래시만 보정한다.
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${resolveApiBaseUrl()}${normalizedPath}`
}

const parseResponsePayload = async (response: Response): Promise<unknown> => {
  const raw = await response.text()
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    // 오류 응답이 HTML/텍스트로 와도 원문 메시지를 잃지 않게 그대로 돌려준다.
    return raw
  }
}

export const tokenAuthHeaders = (token: string): HeadersInit => ({
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
})

export const fetchJson = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
  const response = await fetch(buildApiUrl(path), init)
  const payload = await parseResponsePayload(response)

  if (!response.ok) {
    const fallbackMessage = `Request failed with status ${response.status}`
    const message =
      (typeof payload === 'object' &&
        payload !== null &&
        'message' in payload &&
        typeof (payload as { message?: unknown }).message === 'string' &&
        (payload as { message: string }).message) ||
      fallbackMessage
    throw new HttpRequestError(message, response.status, payload)
  }

  return payload as T
}

export const postJson = async <T>(
  path: string,
  body: unknown,
  init: Omit<RequestInit, 'method' | 'body'> = {},
): Promise<T> => {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return fetchJson<T>(path, {
    ...init,
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })
}
