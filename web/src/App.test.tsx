import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

type Handler = (init?: RequestInit) => Response | Promise<Response>
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

const ada = { id: '1', email: 'ada@example.com', displayName: 'Ada' }

function mockServer(routes: Record<string, Handler>) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const handler = routes[String(input)]
    if (!handler) throw new Error(`unexpected request to ${String(input)}`)
    return handler(init)
  })
}

const health: Handler = () => json({ status: 'ok', service: 'interviewpair' })

beforeEach(() => localStorage.clear())
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('App', () => {
  it('shows the server as online when the health check succeeds', async () => {
    mockServer({ '/api/health': health })
    render(<App />)
    expect(await screen.findByText(/Server online: interviewpair/)).toBeInTheDocument()
  })

  it('shows an error state when the server is down', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network'))
    render(<App />)
    expect(await screen.findByText(/not reachable/)).toBeInTheDocument()
  })

  it('logs in, shows the user, and stores the token', async () => {
    const user = userEvent.setup()
    const login = vi.fn<Handler>(() => json({ token: 'tok-123', user: ada }))
    mockServer({ '/api/health': health, '/api/auth/login': login })
    render(<App />)

    await user.type(screen.getByLabelText('Email'), '  ada@example.com ')
    await user.type(screen.getByLabelText('Password'), 'correct horse')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Ada')).toBeInTheDocument()
    expect(localStorage.getItem('interviewpair.token')).toBe('tok-123')
    const sent = login.mock.calls[0][0] as RequestInit
    expect(JSON.parse(String(sent.body))).toEqual({
      email: 'ada@example.com',
      password: 'correct horse',
    })
  })

  it('shows the server message when the password is wrong', async () => {
    const user = userEvent.setup()
    mockServer({
      '/api/health': health,
      '/api/auth/login': () => json({ error: 'Invalid email or password', fields: {} }, 401),
    })
    render(<App />)
    await user.type(screen.getByLabelText('Email'), 'ada@example.com')
    await user.type(screen.getByLabelText('Password'), 'nope')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(localStorage.getItem('interviewpair.token')).toBeNull()
  })

  it('registers a new account and shows field errors from the server', async () => {
    const user = userEvent.setup()
    mockServer({
      '/api/health': health,
      '/api/auth/register': () =>
        json({ error: 'Invalid request', fields: { email: 'must be a well-formed email address' } }, 400),
    })
    render(<App />)
    await user.click(screen.getByRole('button', { name: /Create an account/ }))
    await user.type(screen.getByLabelText('Display name'), 'Ada')
    await user.type(screen.getByLabelText('Email'), 'ada@example.com')
    await user.type(screen.getByLabelText('Password'), 'correct horse')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
  })

  it('restores a saved session on load', async () => {
    localStorage.setItem('interviewpair.token', 'saved-token')
    const me = vi.fn<Handler>(() => json(ada))
    mockServer({ '/api/health': health, '/api/me': me })
    render(<App />)

    expect(await screen.findByText('Ada')).toBeInTheDocument()
    const sent = me.mock.calls[0][0] as RequestInit
    expect((sent.headers as Record<string, string>).Authorization).toBe('Bearer saved-token')
  })

  it('drops an expired token and shows the login form', async () => {
    localStorage.setItem('interviewpair.token', 'expired')
    mockServer({ '/api/health': health, '/api/me': () => json({}, 401) })
    render(<App />)

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    await waitFor(() => expect(localStorage.getItem('interviewpair.token')).toBeNull())
  })

  it('logs out and clears the token', async () => {
    const user = userEvent.setup()
    localStorage.setItem('interviewpair.token', 'saved-token')
    mockServer({ '/api/health': health, '/api/me': () => json(ada) })
    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Log out' }))
    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    expect(localStorage.getItem('interviewpair.token')).toBeNull()
  })
})
