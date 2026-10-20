export type Health = { status: string; service: string }
export type User = { id: string; email: string; displayName: string }
export type AuthResult = { token: string; user: User }

export class ApiError extends Error {
  status: number
  fields: Record<string, string>

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as Record<string, string>) }
  if (init.body) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(path, { ...init, headers })
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    let fields: Record<string, string> = {}
    try {
      const body = await res.json()
      if (body.error) message = body.error
      if (body.fields) fields = body.fields
    } catch {
      // the response had no JSON body
    }
    throw new ApiError(res.status, message, fields)
  }
  return res.json()
}

export const fetchHealth = () => request<Health>('/api/health')

export const register = (email: string, password: string, displayName: string) =>
  request<AuthResult>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  })

export const login = (email: string, password: string) =>
  request<AuthResult>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

export const fetchMe = (token: string) => request<User>('/api/me', {}, token)
