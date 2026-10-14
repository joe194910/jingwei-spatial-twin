/**
 * 后端统一响应体，业务数据必须从 data 字段读取。
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId: string | null
  timestamp: string
}

/**
 * 后端统一分页数据，页码从1开始。
 */
export interface PageResult<T> {
  pageNo: number
  pageSize: number
  total: number
  records: T[]
}

export class ApiError extends Error {
  readonly code: number
  readonly requestId: string | null
  readonly status: number

  constructor(message: string, code: number, status: number, requestId: string | null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.requestId = requestId
  }
}
