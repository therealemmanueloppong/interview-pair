import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from './api'
import { fetchTicket } from './mockApi'
import type { SessionView } from './mockApi'

export type Connection = 'connecting' | 'open' | 'reconnecting' | 'closed'

type ServerMessage =
  | { type: 'state'; session: SessionView }
  | { type: 'code'; text: string }
  | { type: 'presence'; online: string[] }
  | { type: 'error'; message: string }

/** Code 4001 means the same person opened the session in another tab, so do not reconnect. */
const REPLACED = 4001
const SEND_DELAY_MS = 200

export function socketUrl(ticket: string): string {
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${scheme}://${window.location.host}/ws/sessions?ticket=${encodeURIComponent(ticket)}`
}

/**
 * Keeps a live connection to one session. The server sends the session as this person may see it,
 * the other person's code as they type, and who is online. Dropped connections are retried with a
 * fresh one-time ticket each time.
 */
export function useSessionSocket(sessionId: string, initial?: SessionView) {
  const [session, setSession] = useState<SessionView | null>(initial ?? null)
  const [code, setCode] = useState(initial?.code ?? '')
  const [online, setOnline] = useState<string[]>([])
  const [connection, setConnection] = useState<Connection>('connecting')
  const [notice, setNotice] = useState('')
  const socket = useRef<WebSocket | null>(null)
  const timer = useRef<number | undefined>(undefined)

  useEffect(() => {
    let stopped = false
    let attempt = 0
    let retry: number | undefined

    async function connect() {
      try {
        const { ticket } = await fetchTicket(sessionId)
        if (stopped) return
        const ws = new WebSocket(socketUrl(ticket))
        socket.current = ws
        ws.onopen = () => {
          attempt = 0
          setConnection('open')
          setNotice('')
        }
        ws.onmessage = (event) => {
          let message: ServerMessage
          try {
            message = JSON.parse(String(event.data))
          } catch {
            return
          }
          if (message.type === 'state') {
            setSession(message.session)
            setCode(message.session.code)
          } else if (message.type === 'code') {
            setCode(message.text)
          } else if (message.type === 'presence') {
            setOnline(message.online)
          } else if (message.type === 'error') {
            setNotice(message.message)
          }
        }
        ws.onclose = (event) => {
          if (stopped || socket.current !== ws) return
          socket.current = null
          setOnline([])
          if (event.code === REPLACED) {
            setConnection('closed')
            setNotice('This session was opened in another tab, so this one stopped.')
            return
          }
          scheduleRetry()
        }
      } catch (err) {
        if (stopped) return
        // A session that is not yours or a login that expired will not fix itself.
        if (err instanceof ApiError && (err.status === 401 || err.status === 404)) {
          setConnection('closed')
          setNotice(err.status === 404 ? 'This session was not found.' : 'Please log in again.')
          return
        }
        scheduleRetry()
      }
    }

    function scheduleRetry() {
      setConnection('reconnecting')
      attempt += 1
      const delay = Math.min(10_000, 1_000 * 2 ** (attempt - 1))
      retry = window.setTimeout(connect, delay)
    }

    connect()
    return () => {
      stopped = true
      window.clearTimeout(retry)
      window.clearTimeout(timer.current)
      socket.current?.close()
      socket.current = null
    }
  }, [sessionId])

  /** Shows the text right away and sends it to the other person after a short pause in typing. */
  const edit = useCallback((text: string) => {
    setCode(text)
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => {
      const ws = socket.current
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'code', text }))
      }
    }, SEND_DELAY_MS)
  }, [])

  /** Used when a REST call returns the new session (switch roles, finish), ahead of the socket. */
  const replaceSession = useCallback((next: SessionView) => {
    setSession(next)
    setCode(next.code)
  }, [])

  return { session, code, online, connection, notice, edit, replaceSession }
}
