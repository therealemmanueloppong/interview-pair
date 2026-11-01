import { act, cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import MockInterviews from './MockInterviews'
import * as api from './mockApi'
import type { RequestView, SessionSummary } from './mockApi'

vi.mock('./mockApi')
// The room has its own tests. Here it is a stand-in that shows which session was opened.
vi.mock('./SessionRoom', () => ({
  default: ({ sessionId, onBack }: { sessionId: string; onBack: () => void }) => (
    <div>
      Room for {sessionId}
      <button onClick={onBack}>Leave room</button>
    </div>
  ),
}))
const mocked = vi.mocked(api)

const summary = (over: Partial<SessionSummary> = {}): SessionSummary => ({
  id: 's1',
  topic: 'ALGORITHMS',
  topicLabel: 'Data Structures & Algorithms',
  level: 'BEGINNER',
  slotStart: '2026-11-02T18:00:00Z',
  status: 'ACTIVE',
  round: 1,
  partner: { id: 'p1', displayName: 'Grace' },
  questionTitle: 'Duplicate check',
  feedbackGiven: false,
  ...over,
})

const waiting = (over: Partial<RequestView> = {}): RequestView => ({
  id: 'r1',
  topic: 'BEHAVIORAL',
  topicLabel: 'Behavioral',
  level: 'INTERMEDIATE',
  slotStart: '2026-11-03T16:00:00Z',
  status: 'OPEN',
  sessionId: null,
  ...over,
})

beforeEach(() => {
  mocked.fetchSessions.mockResolvedValue([])
  mocked.fetchRequests.mockResolvedValue([])
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.resetAllMocks()
})

describe('the sessions list', () => {
  it('invites you to schedule when there is nothing yet', async () => {
    render(<MockInterviews />)
    expect(await screen.findByText(/No sessions yet/)).toBeInTheDocument()
  })

  it('lists sessions with the partner and opens the room', async () => {
    const u = userEvent.setup()
    mocked.fetchSessions.mockResolvedValue([
      summary(),
      summary({ id: 's2', status: 'DONE', questionTitle: 'Merge intervals', level: 'INTERMEDIATE', partner: { id: 'p2', displayName: 'Linus' } }),
    ])
    render(<MockInterviews />)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('Grace')).toBeInTheDocument()
    expect(within(table).getByText('Linus')).toBeInTheDocument()
    expect(within(table).getByRole('button', { name: 'Leave feedback for Merge intervals with Linus' })).toBeInTheDocument()

    await u.click(screen.getByRole('button', { name: 'Open room for Duplicate check with Grace' }))
    expect(screen.getByText('Room for s1')).toBeInTheDocument()

    await u.click(screen.getByRole('button', { name: 'Leave room' }))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it('shows requests that are still waiting and cancels them', async () => {
    const u = userEvent.setup()
    mocked.fetchRequests.mockResolvedValueOnce([waiting()]).mockResolvedValue([])
    mocked.cancelRequest.mockResolvedValue(undefined)
    render(<MockInterviews />)

    expect(await screen.findByText('Waiting for a partner')).toBeInTheDocument()
    await u.click(screen.getByRole('button', { name: /Cancel Behavioral request/ }))

    expect(mocked.cancelRequest).toHaveBeenCalledWith('r1')
    expect(await screen.findByText('Request cancelled.')).toBeInTheDocument()
    expect(screen.queryByText('Waiting for a partner')).not.toBeInTheDocument()
  })

  it('shows an error when the server cannot be reached', async () => {
    mocked.fetchSessions.mockRejectedValue(new Error('Request failed (500)'))
    render(<MockInterviews />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Request failed (500)')
  })

  it('announces a partner who is matched while you wait', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    mocked.fetchSessions.mockResolvedValueOnce([]).mockResolvedValue([summary()])
    render(<MockInterviews />)
    await screen.findByText(/No sessions yet/)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_500)
    })

    expect(await screen.findByText(/You were matched with Grace for Duplicate check/)).toBeInTheDocument()
  })
})

describe('scheduling', () => {
  async function chooseThroughTheWizard(u: ReturnType<typeof userEvent.setup>) {
    await u.click(await screen.findByRole('button', { name: 'Schedule peer mock interview' }))
    await u.click(screen.getByLabelText(/Behavioral/))
    await u.click(screen.getByRole('button', { name: 'Next' }))
    await u.click(screen.getByLabelText(/Intermediate/))
    await u.click(screen.getByRole('button', { name: 'Next' }))
    const slots = screen.getAllByRole('radio', { name: /\d/ })
    await u.click(slots[1])
    return slots[1] as HTMLInputElement
  }

  it('walks through topic, level and time, then asks the server for that exact slot', async () => {
    const u = userEvent.setup()
    mocked.requestInterview.mockResolvedValue(waiting())
    render(<MockInterviews />)

    const chosen = await chooseThroughTheWizard(u)
    await u.click(screen.getByRole('button', { name: 'Schedule interview' }))

    expect(mocked.requestInterview).toHaveBeenCalledWith('BEHAVIORAL', 'INTERMEDIATE', chosen.value)
    // The slot sent to the server is a UTC instant on an even hour.
    expect(new Date(chosen.value).getUTCHours() % 2).toBe(0)
    expect(await screen.findByText(/You are on the list for/)).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Schedule a mock interview' })).not.toBeInTheDocument()
  })

  it('opens the room straight away when a partner is already waiting', async () => {
    const u = userEvent.setup()
    mocked.requestInterview.mockResolvedValue(waiting({ status: 'MATCHED', sessionId: 's9' }))
    render(<MockInterviews />)

    await chooseThroughTheWizard(u)
    await u.click(screen.getByRole('button', { name: 'Schedule interview' }))

    expect(await screen.findByText('Room for s9')).toBeInTheDocument()
  })

  it('needs a time before it will schedule, and lets you go back', async () => {
    const u = userEvent.setup()
    render(<MockInterviews />)
    await u.click(await screen.findByRole('button', { name: 'Schedule peer mock interview' }))
    await u.click(screen.getByRole('button', { name: 'Next' }))
    await u.click(screen.getByRole('button', { name: 'Next' }))

    expect(screen.getByRole('button', { name: 'Schedule interview' })).toBeDisabled()
    expect(screen.getByText('STEP 3 OF 3')).toBeInTheDocument()

    await u.click(screen.getByRole('button', { name: 'Back' }))
    expect(screen.getByText('STEP 2 OF 3')).toBeInTheDocument()
  })

  it('shows the server reason when a slot is refused and keeps the form open', async () => {
    const u = userEvent.setup()
    mocked.requestInterview.mockRejectedValue(new Error('You already have an interview in that time slot'))
    render(<MockInterviews />)

    await chooseThroughTheWizard(u)
    await u.click(screen.getByRole('button', { name: 'Schedule interview' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You already have an interview in that time slot')
    expect(screen.getByRole('button', { name: 'Schedule interview' })).toBeEnabled()
  })

  it('can be cancelled', async () => {
    const u = userEvent.setup()
    render(<MockInterviews />)
    await u.click(await screen.findByRole('button', { name: 'Schedule peer mock interview' }))
    await u.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByText('STEP 1 OF 3')).not.toBeInTheDocument()
  })
})
