import { useCallback, useEffect, useState } from 'react'
import { ApiError, fetchHealth, fetchMe } from './api'
import type { AuthResult, User } from './api'
import AuthForm from './AuthForm'
import { clearToken, loadToken, saveToken } from './tokenStorage'

type ServerState = { kind: 'loading' } | { kind: 'up'; service: string } | { kind: 'down' }
type Session = { kind: 'checking' } | { kind: 'out' } | { kind: 'in'; user: User }

export default function App() {
  const [server, setServer] = useState<ServerState>({ kind: 'loading' })
  const [session, setSession] = useState<Session>(() => (loadToken() ? { kind: 'checking' } : { kind: 'out' }))

  useEffect(() => {
    fetchHealth()
      .then((h) => setServer({ kind: 'up', service: h.service }))
      .catch(() => setServer({ kind: 'down' }))
  }, [])

  useEffect(() => {
    const token = loadToken()
    if (!token) return
    fetchMe(token)
      .then((user) => setSession({ kind: 'in', user }))
      .catch((err) => {
        // An expired or invalid token means logged out. A network error keeps the token for a retry.
        if (err instanceof ApiError && err.status === 401) clearToken()
        setSession({ kind: 'out' })
      })
  }, [])

  const onAuthenticated = useCallback((result: AuthResult) => {
    saveToken(result.token)
    setSession({ kind: 'in', user: result.user })
  }, [])

  function logout() {
    clearToken()
    setSession({ kind: 'out' })
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-4 p-6">
      <h1 className="text-3xl font-bold">InterviewPair</h1>
      <p className="text-slate-600">Find a partner for a mock technical interview, by topic, level and time.</p>

      {session.kind === 'checking' && <p>Checking your session...</p>}
      {session.kind === 'out' && <AuthForm onAuthenticated={onAuthenticated} />}
      {session.kind === 'in' && (
        <section className="flex flex-col gap-2 rounded border p-4">
          <p>
            Signed in as <strong>{session.user.displayName}</strong> ({session.user.email})
          </p>
          <p className="text-sm text-slate-600">Availability and matching are coming next.</p>
          <button onClick={logout} className="self-start rounded border px-3 py-1 text-sm">
            Log out
          </button>
        </section>
      )}

      <p role="status" className="rounded border p-3 text-sm">
        {server.kind === 'loading' && 'Checking server...'}
        {server.kind === 'up' && `Server online: ${server.service}`}
        {server.kind === 'down' && 'Server is not reachable'}
      </p>
    </main>
  )
}
