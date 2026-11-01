import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './mockApi'
import type { SessionView } from './mockApi'
import SessionRoom from './SessionRoom'

vi.mock('./mockApi')
const mocked = vi.mocked(api)

/** Stands in for the browser WebSocket so a test can play the server. */
class FakeWebSocket {
  static instances: FakeWebSocket[] = []
  static OPEN = 1
  readyState = 0
  sent: string[] = []
  onopen: (() => void) | null = null
  onmessage: ((event: { data: string }) => void) | null = null
  onclose: ((event: { code: number }) => void) | null = null

  url: string

  constructor(url: string) {
    this.url = url
    FakeWebSocket.instances.push(this)
  }

  send(data: string) {
    this.sent.push(data)
  }

  close() {
    this.readyState = 3
  }

  // helpers the tests use to act as the server
  open() {
    this.readyState = 1
    this.onopen?.()
  }

  receive(message: unknown) {
    this.onmessage?.({ data: JSON.stringify(message) })
  }

  drop(code = 1006) {
    this.readyState = 3
    this.onclose?.({ code })
  }
}

const answering: SessionView = {
  id: 's1',
  topic: 'ALGORITHMS',
  topicLabel: 'Data Structures & Algorithms',
  level: 'BEGINNER',
  slotStart: '2026-11-02T18:00:00Z',
  status: 'ACTIVE',
  round: 1,
  youAreInterviewee: true,
  partner: { id: 'p1', displayName: 'Grace' },
  question: {
    id: 'q1',
    title: 'Duplicate check',
    prompt: 'Return True if any value appears twice.',
    language: 'python',
    starterCode: 'def has_duplicate(nums):\n    pass\n',
    hints: null,
    solution: null,
  },
  code: 'def has_duplicate(nums):\n    pass\n',
  feedbackGiven: false,
}

const interviewing: SessionView = {
  ...answering,
  youAreInterviewee: false,
  question: {
    ...answering.question,
    hints: ['Think about the brute force first', 'Which structure answers "seen before?"'],
    solution: 'Scan once and keep a set.',
  },
}

let tickets = 0

beforeEach(() => {
  FakeWebSocket.instances = []
  tickets = 0
  vi.stubGlobal('WebSocket', FakeWebSocket)
  mocked.fetchTicket.mockImplementation(async () => ({ ticket: `ticket-${++tickets}` }))
  mocked.fetchFeedback.mockResolvedValue({ given: false, received: null })
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.useRealTimers()
  vi.resetAllMocks()
})

async function openRoom(view: SessionView) {
  mocked.fetchSession.mockResolvedValue(view)
  render(<SessionRoom sessionId={view.id} onBack={() => {}} />)
  await screen.findByRole('heading', { name: 'Duplicate check' })
  await waitFor(() => expect(FakeWebSocket.instances).toHaveLength(1))
  const socket = FakeWebSocket.instances[0]
  act(() => socket.open())
  return socket
}

describe('SessionRoom as the person answering', () => {
  it('connects with a one-time ticket and can type, while the guide stays hidden', async () => {
    const socket = await openRoom(answering)

    expect(socket.url).toMatch(/^wss?:\/\/.+\/ws\/sessions\?ticket=ticket-1$/)
    expect(screen.getByText('You are answering')).toBeInTheDocument()
    expect(screen.getByLabelText('Your answer')).not.toHaveAttribute('readonly')
    expect(screen.queryByRole('tab', { name: /guide/i })).not.toBeInTheDocument()
    expect(screen.queryByText(/Scan once/)).not.toBeInTheDocument()
  })

  it('sends what you type to your partner after a short pause, once instead of per keystroke', async () => {
    const u = userEvent.setup()
    const socket = await openRoom(answering)

    const editor = screen.getByLabelText('Your answer')
    await u.clear(editor)
    await u.type(editor, 'return True')

    await waitFor(() => expect(socket.sent.length).toBeGreaterThan(0))
    expect(socket.sent).toHaveLength(1)
    expect(JSON.parse(socket.sent[0])).toEqual({ type: 'code', text: 'return True' })
  })

  it('shows when your partner is connected', async () => {
    const socket = await openRoom(answering)
    expect(screen.getByText(/Grace is not connected yet/)).toBeInTheDocument()

    act(() => socket.receive({ type: 'presence', online: ['p1'] }))

    expect(screen.getByText(/Grace is here/)).toBeInTheDocument()
  })

  it('shows the server message when an edit is refused', async () => {
    const socket = await openRoom(answering)

    act(() => socket.receive({ type: 'error', message: 'Only the person answering can edit the code' }))

    expect(screen.getByRole('status')).toHaveTextContent('Only the person answering can edit the code')
  })
})

describe('SessionRoom as the interviewer', () => {
  it('shows the guide, makes the editor read only and follows the live code', async () => {
    const u = userEvent.setup()
    const socket = await openRoom(interviewing)

    expect(screen.getByText('You are interviewing')).toBeInTheDocument()
    const editor = screen.getByLabelText("Grace's answer, read only")
    expect(editor).toHaveAttribute('readonly')

    act(() => socket.receive({ type: 'code', text: 'return len(set(nums)) < len(nums)' }))
    expect(editor).toHaveValue('return len(set(nums)) < len(nums)')

    await u.click(screen.getByRole('tab', { name: 'Interviewer guide' }))
    const panel = screen.getByRole('tabpanel', { name: 'Interviewer guide' })
    expect(within(panel).getByText('Think about the brute force first')).toBeInTheDocument()
    expect(within(panel).getByText('Scan once and keep a set.')).toBeInTheDocument()
    expect(within(panel).getByText(/Only you can see this/)).toBeInTheDocument()
  })

  it('never sends anything when its read only editor is clicked', async () => {
    const u = userEvent.setup()
    const socket = await openRoom(interviewing)

    await u.type(screen.getByLabelText("Grace's answer, read only"), 'hello')

    expect(socket.sent).toHaveLength(0)
  })
})

describe('switching roles', () => {
  it('moves to round two and flips what you can see and do', async () => {
    const u = userEvent.setup()
    await openRoom(answering)
    const roundTwo: SessionView = {
      ...interviewing,
      round: 2,
      question: { ...interviewing.question, id: 'q2', title: 'Balanced brackets' },
      code: 'def is_balanced(s):\n    pass\n',
    }
    mocked.switchRoles.mockResolvedValue(roundTwo)

    await u.click(screen.getByRole('button', { name: 'Switch roles' }))

    expect(await screen.findByRole('heading', { name: 'Balanced brackets' })).toBeInTheDocument()
    expect(screen.getByText('ROUND 2 OF 2')).toBeInTheDocument()
    expect(screen.getByText('You are interviewing')).toBeInTheDocument()
    expect(screen.getByLabelText("Grace's answer, read only")).toHaveAttribute('readonly')
    expect(screen.queryByRole('button', { name: 'Switch roles' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Finish session' })).toBeInTheDocument()
  })

  it('also follows a switch started by the other person through the socket', async () => {
    const socket = await openRoom(interviewing)
    expect(screen.getByLabelText("Grace's answer, read only")).toBeInTheDocument()

    act(() =>
      socket.receive({
        type: 'state',
        session: { ...answering, round: 2, question: { ...answering.question, title: 'Balanced brackets' } },
      }),
    )

    expect(await screen.findByRole('heading', { name: 'Balanced brackets' })).toBeInTheDocument()
    expect(screen.getByLabelText('Your answer')).not.toHaveAttribute('readonly')
    // The guide tab disappears because the server no longer sends hints to the person answering.
    expect(screen.queryByRole('tab', { name: /guide/i })).not.toBeInTheDocument()
  })

  it('shows the error when the switch is refused', async () => {
    const u = userEvent.setup()
    await openRoom(answering)
    mocked.switchRoles.mockRejectedValue(new Error('The roles have already been switched'))

    await u.click(screen.getByRole('button', { name: 'Switch roles' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The roles have already been switched')
  })
})

describe('finishing and feedback', () => {
  it('locks the editor, shows the solution to both people and collects feedback', async () => {
    const u = userEvent.setup()
    await openRoom({ ...answering, round: 2 })
    const done: SessionView = {
      ...answering,
      round: 2,
      status: 'DONE',
      question: { ...answering.question, hints: ['Use a set'], solution: 'Scan once and keep a set.' },
    }
    mocked.finishSession.mockResolvedValue(done)
    mocked.fetchFeedback.mockResolvedValue({
      given: false,
      received: { rating: 4, comment: 'Clear explanation.', createdAt: '2026-11-02T19:00:00Z' },
    })
    mocked.leaveFeedback.mockResolvedValue({
      given: true,
      received: { rating: 4, comment: 'Clear explanation.', createdAt: '2026-11-02T19:00:00Z' },
    })

    await u.click(screen.getByRole('button', { name: 'Finish session' }))

    expect(await screen.findByText('SESSION COMPLETE')).toBeInTheDocument()
    expect(screen.getByLabelText('Your answer')).toHaveAttribute('readonly')
    expect(await screen.findByText('Clear explanation.')).toBeInTheDocument()
    await u.click(screen.getByRole('tab', { name: 'Hints and solution' }))
    expect(screen.getByText('Scan once and keep a set.')).toBeInTheDocument()

    const save = screen.getByRole('button', { name: 'Save feedback' })
    expect(save).toBeDisabled()
    await u.click(screen.getByLabelText('5'))
    expect(save).toBeDisabled()
    await u.type(screen.getByLabelText(/What went well/), 'Great at asking questions.')
    expect(save).toBeEnabled()
    await u.click(save)

    expect(mocked.leaveFeedback).toHaveBeenCalledWith('s1', 5, 'Great at asking questions.')
    expect(await screen.findByText(/your feedback for Grace was saved/)).toBeInTheDocument()
  })
})

describe('connection handling', () => {
  it('reconnects with a fresh ticket after the connection drops', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    mocked.fetchSession.mockResolvedValue(answering)
    render(<SessionRoom sessionId="s1" onBack={() => {}} />)
    await screen.findByRole('heading', { name: 'Duplicate check' })
    await waitFor(() => expect(FakeWebSocket.instances).toHaveLength(1))
    act(() => FakeWebSocket.instances[0].open())

    act(() => FakeWebSocket.instances[0].drop())
    expect(screen.getByRole('status')).toHaveTextContent(/Connecting to the live room/)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_100)
    })

    expect(FakeWebSocket.instances).toHaveLength(2)
    expect(FakeWebSocket.instances[1].url).toContain('ticket=ticket-2')
  })

  it('does not reconnect when the same person opened the session in another tab', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    mocked.fetchSession.mockResolvedValue(answering)
    render(<SessionRoom sessionId="s1" onBack={() => {}} />)
    await screen.findByRole('heading', { name: 'Duplicate check' })
    await waitFor(() => expect(FakeWebSocket.instances).toHaveLength(1))
    act(() => FakeWebSocket.instances[0].open())

    act(() => FakeWebSocket.instances[0].drop(4001))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000)
    })

    expect(FakeWebSocket.instances).toHaveLength(1)
    expect(screen.getByRole('status')).toHaveTextContent(/another tab/)
  })

  it('shows an error and a way back when the session cannot be opened', async () => {
    mocked.fetchSession.mockRejectedValue(new Error('Session not found'))
    const onBack = vi.fn()
    render(<SessionRoom sessionId="nope" onBack={onBack} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Session not found')
    fireEvent.click(screen.getByRole('button', { name: 'Back to sessions' }))
    expect(onBack).toHaveBeenCalled()
    expect(FakeWebSocket.instances).toHaveLength(0)
  })
})
