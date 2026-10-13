import { ApiError, type ApiResponse } from '@/types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const TOKEN_NAME_KEY = 'jw-token-name'
const TOKEN_VALUE_KEY = 'jw-token-value'

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
}

let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler
}

export function saveSessionToken(tokenName: string, tokenValue: string) {
  sessionStorage.setItem(TOKEN_NAME_KEY, tokenName)
  sessionStorage.setItem(TOKEN_VALUE_KEY, tokenValue)
}

export function clearSessionToken() {
  sessionStorage.removeItem(TOKEN_NAME_KEY)
  sessionStorage.removeItem(TOKEN_VALUE_KEY)
}

export function hasSessionToken() {
  return Boolean(sessionStorage.getItem(TOKEN_NAME_KEY) && sessionStorage.getItem(TOKEN_VALUE_KEY))
}

/**
 * 统一处理令牌、超时、响应体业务码和中文错误提示。
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), 15_000)
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  const tokenName = sessionStorage.getItem(TOKEN_NAME_KEY)
  const tokenValue = sessionStorage.getItem(TOKEN_VALUE_KEY)
  if (tokenName && tokenValue) {
    headers.set(tokenName, tokenValue)
  }
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal,
    })
    const payload = (await response.json()) as ApiResponse<T>
    if (!response.ok || payload.code !== 0) {
      if (response.status === 401) {
        clearSessionToken()
        unauthorizedHandler?.()
      }
      throw new ApiError(payload.message || '请求处理失败', payload.code, response.status, payload.requestId)
    }
    return payload.data
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('请求超时，请检查后端服务或网络连接', 40800, 408, null)
    }
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError('无法连接后端服务，请确认服务已启动', 50300, 503, null)
  } finally {
    window.clearTimeout(timeoutId)
  }
}
