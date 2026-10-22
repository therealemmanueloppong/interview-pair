import { useCallback, useEffect, useState } from 'react'
import { ApiError, fetchHealth, fetchMe } from './api'
import type { AuthResult, User } from './api'
import AuthForm from './AuthForm'
import Dashboard from './Dashboard'
import ThemeToggle from './ThemeToggle'
import { clearToken, loadToken, saveToken } from './tokenStorage'

type ServerState =
  { kind: 'loading' } | { kind: 'up'; service: string } | { kind: 'down' }
type Session =
  { kind: 'checking' } | { kind: 'out' } | { kind: 'in'; user: User }

export default function App() {
  const [server, setServer] = useState<ServerState>({ kind: 'loading' })
  const [session, setSession] = useState<Session>(() =>
    loadToken() ? { kind: 'checking' } : { kind: 'out' },
  )

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

  const healthText =
    server.kind === 'loading'
      ? 'Checking server...'
      : server.kind === 'up'
        ? `Server online: ${server.service}`
        : 'Server is not reachable'
  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="topbar">
        <a className="brand" href="/" aria-label="InterviewPair home">
          <span className="brand-mark" aria-hidden="true">
            <i />
            <i />
          </span>
          interview<span>pair</span>
          <span className="beta-label">EARLY ACCESS</span>
        </a>
        <div className="topbar-actions">
          <span className={`connection ${server.kind}`} role="status">
            <span aria-hidden="true" />
            {healthText}
          </span>
          <ThemeToggle />
        </div>
      </header>
      {session.kind === 'in' ? (
        <Dashboard
          key={session.user.id}
          user={session.user}
          onLogout={logout}
        />
      ) : (
        <main id="main-content" className="auth-layout">
          <section className="auth-story">
            <p className="eyebrow">PRACTICE WITH PURPOSE</p>
            <h1>
              Your next chapter
              <br />
              starts with a<br />
              <em>little practice.</em>
            </h1>
            <p>
              Build confidence, one conversation at a time.
              <br />
              Your space to prepare for what comes next.
            </p>
            <div className="auth-caption">
              <span>01 / THINK</span>
              <span>02 / PRACTICE</span>
              <span>03 / GROW</span>
            </div>
          </section>
          <section className="auth-panel">
            {session.kind === 'checking' ? (
              <p>Checking your session...</p>
            ) : (
              <AuthForm onAuthenticated={onAuthenticated} />
            )}
            <p className="auth-note">
              A thoughtful place to prepare.
              <br />
              Built for the interview ahead.
            </p>
          </section>
        </main>
      )}
    </div>
  )
}
