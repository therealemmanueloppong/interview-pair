import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, login, register } from './api'
import type { AuthResult } from './api'

type Props = { onAuthenticated: (result: AuthResult) => void }

export default function AuthForm({ onAuthenticated }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setBusy(true)
    try {
      const cleanEmail = email.trim()
      const result =
        mode === 'login'
          ? await login(cleanEmail, password)
          : await register(cleanEmail, password, displayName.trim())
      onAuthenticated(result)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(err.fields)
      } else {
        setError('Could not reach the server. Try again.')
      }
    } finally {
      setBusy(false)
    }
  }

  const input = 'auth-input'
  const problem = (name: string) =>
    fieldErrors[name] ? (
      <p className="mt-1 text-sm text-red-700">{fieldErrors[name]}</p>
    ) : null

  return (
    <form
      onSubmit={submit}
      className="auth-form"
      aria-label={mode === 'login' ? 'Log in' : 'Create account'}
    >
      <h2 className="auth-title">
        {mode === 'login' ? 'Log in' : 'Create your account'}
      </h2>

      {mode === 'register' && (
        <div>
          <label className="block">
            <span className="text-sm">Display name</span>
            <input
              className={input}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              maxLength={80}
            />
          </label>
          {problem('displayName')}
        </div>
      )}

      <div>
        <label className="block">
          <span className="text-sm">Email</span>
          <input
            className={input}
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        {problem('email')}
      </div>

      <div>
        <label className="block">
          <span className="text-sm">Password</span>
          <input
            className={input}
            type="password"
            autoComplete={
              mode === 'login' ? 'current-password' : 'new-password'
            }
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={mode === 'register' ? 8 : undefined}
          />
        </label>
        {mode === 'register' && (
          <p className="mt-1 text-xs text-slate-500">At least 8 characters.</p>
        )}
        {problem('password')}
      </div>

      {error && (
        <p
          role="alert"
          className="rounded border border-red-300 bg-red-50 p-2 text-sm text-red-800"
        >
          {error}
        </p>
      )}

      <button type="submit" disabled={busy} className="button primary wide">
        {busy
          ? 'Please wait...'
          : mode === 'login'
            ? 'Log in'
            : 'Create account'}
      </button>

      <button
        type="button"
        className="text-button auth-switch"
        onClick={() => {
          setMode(mode === 'login' ? 'register' : 'login')
          setError(null)
          setFieldErrors({})
        }}
      >
        {mode === 'login'
          ? 'New here? Create an account'
          : 'Already have an account? Log in'}
      </button>
    </form>
  )
}
