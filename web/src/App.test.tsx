import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => vi.restoreAllMocks())

describe('App', () => {
  it('shows the server as online when the health check succeeds', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 'ok', service: 'interviewpair' })),
    )
    render(<App />)
    expect(await screen.findByText(/Server online: interviewpair/)).toBeInTheDocument()
  })

  it('shows an error state when the server is down', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network'))
    render(<App />)
    expect(await screen.findByText(/not reachable/)).toBeInTheDocument()
  })
})
