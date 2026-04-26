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
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${resolveApiBaseUrl()}${normalizedPath}`
}

const parseResponsePayload = async (response: Response): Promise<unknown> => {
  const raw = await response.text()
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
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
