import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from './Dashboard'
import ThemeToggle from './ThemeToggle'
import { practiceKey, readPractice } from './practice'
const user = {
  id: 'ada',
  displayName: 'Ada Lovelace',
  email: 'ada@example.com',
}
beforeEach(() => {
  localStorage.clear()
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute('open')
  }
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.useRealTimers()
})
describe('dashboard', () => {
  it('records practice, persists it on reload, and allows removal with undo', async () => {
    const u = userEvent.setup()
    const first = render(<Dashboard user={user} onLogout={() => {}} />)
    await u.click(screen.getByRole('button', { name: '+ Log practice' }))
    const dialog = screen.getByRole('dialog')
    await u.selectOptions(within(dialog).getByLabelText('Topic'), 'Graphs')
    await u.clear(within(dialog).getByLabelText('Minutes practiced'))
    await u.type(within(dialog).getByLabelText('Minutes practiced'), '30')
    await u.type(
      within(dialog).getByLabelText(/One thing/),
      'Explain visited nodes before coding.',
    )
    await u.click(within(dialog).getByRole('button', { name: /Save practice/ }))
    expect(readPractice(user.id).data.entries[0]).toMatchObject({
      topic: 'Graphs',
      minutes: 30,
      note: 'Explain visited nodes before coding.',
    })
    await u.click(screen.getByRole('button', { name: 'Overview' }))
    expect(
      screen.getByRole('progressbar', { name: 'Weekly practice goal' }),
    ).toHaveAttribute('value', '1')
    first.unmount()
    render(<Dashboard user={user} onLogout={() => {}} />)
    await u.click(screen.getByRole('button', { name: /My notebook/ }))
    expect(
      screen.getByText('Explain visited nodes before coding.'),
    ).toBeInTheDocument()
    await u.click(screen.getByRole('button', { name: /Delete Graphs session/ }))
    expect(readPractice(user.id).data.entries).toHaveLength(0)
    await u.click(screen.getByRole('button', { name: 'Undo' }))
    expect(readPractice(user.id).data.entries).toHaveLength(1)
  })
  it('persists goal and bookmarks and filters saved prompts', async () => {
    const u = userEvent.setup()
    render(<Dashboard user={user} onLogout={() => {}} />)
    await u.selectOptions(screen.getByLabelText('Weekly session goal'), '5')
    await u.click(screen.getByRole('button', { name: 'Practice room' }))
    await u.click(screen.getByRole('button', { name: 'Save Map the islands' }))
    await u.click(screen.getByRole('button', { name: 'Saved (1)' }))
    expect(
      screen.getByRole('button', { name: /Practice Map the islands/ }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', {
        name: /Practice Find the hidden groups/,
      }),
    ).not.toBeInTheDocument()
    expect(readPractice(user.id).data).toMatchObject({
      goal: 5,
      saved: ['islands'],
    })
  })
  it('keeps elapsed time when paused and uses wall clock time after background throttling', () => {
    vi.useFakeTimers()
    render(<Dashboard user={user} onLogout={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'Practice room' }))
    fireEvent.click(screen.getByRole('button', { name: 'Start session' }))
    act(() => {
      vi.setSystemTime(Date.now() + 90_000)
      vi.advanceTimersByTime(250)
    })
    expect(screen.getByRole('timer')).toHaveAccessibleName(
      '43 minutes 30 seconds remaining',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Pause session' }))
    act(() => {
      vi.advanceTimersByTime(60_000)
    })
    expect(screen.getByRole('timer')).toHaveAccessibleName(
      '43 minutes 30 seconds remaining',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Finish & reflect' }))
    expect(screen.getByLabelText('Minutes practiced')).toHaveValue(2)
  })
  it('warns about unavailable storage without preventing practice', async () => {
    const u = userEvent.setup()
    render(<Dashboard user={user} onLogout={() => {}} />)
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Blocked')
    })
    await u.selectOptions(screen.getByLabelText('Weekly session goal'), '4')
    expect(screen.getByRole('alert')).toHaveTextContent(
      'New changes stay in this visit only',
    )
    expect(screen.getByLabelText('Weekly session goal')).toHaveValue('4')
  })
  it('shows an honest empty state without inventing session data', () => {
    render(<Dashboard user={user} onLogout={() => {}} />)
    expect(screen.getByText(/Welcome back, Ada/)).toBeInTheDocument()
    expect(localStorage.getItem(practiceKey(user.id))).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'My notebook' }))
    expect(
      screen.getByText('Your next insight belongs here.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Export notes/ })).toBeDisabled()
  })
})
it('switches themes and restores the saved preference', async () => {
  localStorage.setItem('interviewpair.theme', 'light')
  const u = userEvent.setup()
  const first = render(<ThemeToggle />)
  await u.click(screen.getByRole('button', { name: 'Switch to dark mode' }))
  expect(document.documentElement.dataset.theme).toBe('dark')
  first.unmount()
  render(<ThemeToggle />)
  expect(
    screen.getByRole('button', { name: 'Switch to light mode' }),
  ).toBeInTheDocument()
})
