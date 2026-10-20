const KEY = 'interviewpair.token'

// Storage can throw (private windows, blocked cookies), so every call is guarded.
export function loadToken(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function saveToken(token: string): void {
  try {
    localStorage.setItem(KEY, token)
  } catch {
    // stay logged in for this tab only
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // nothing to clear
  }
}
