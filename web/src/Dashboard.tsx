import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { User } from './api'
import {
  practiceKey,
  prompts,
  readPractice,
  topics,
  weeklyEntries,
  weekStart,
} from './practice'
import type { Confidence, PracticeData, PracticeEntry, Topic } from './practice'

type View = 'Overview' | 'Practice room' | 'My notebook'
type Props = { user: User; onLogout: () => void }
function Arrow() {
  return <span aria-hidden="true">↗</span>
}
function Icon({ name }: { name: string }) {
  const paths: Record<string, string> = {
    overview: 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
    practice: 'm8 4 12 8-12 8V4Z',
    notebook: 'M5 3h14v18H5zM9 8h6M9 12h6M9 16h4',
    clock: 'M12 7v5l3 2',
    check: 'm5 12 4 4L19 6',
  }
  return (
    <svg
      viewBox="0 0 24 24"
      width="19"
      height="19"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {name === 'clock' && <circle cx="12" cy="12" r="9" />}
      <path d={paths[name] || paths.check} />
    </svg>
  )
}
export default function Dashboard({ user, onLogout }: Props) {
  const [initial] = useState(() => readPractice(user.id))
  const [data, setData] = useState<PracticeData>(initial.data)
  const [storageError, setStorageError] = useState(initial.error)
  const [view, setView] = useState<View>('Overview')
  const [selected, setSelected] = useState(prompts[0].id)
  const [filter, setFilter] = useState('All topics')
  const [savedOnly, setSavedOnly] = useState(false)
  const [notice, setNotice] = useState('')
  const [removed, setRemoved] = useState<PracticeEntry | null>(null)
  const [duration, setDuration] = useState(45)
  const [remaining, setRemaining] = useState(45 * 60)
  const [deadline, setDeadline] = useState<number | null>(null)
  const [showHints, setShowHints] = useState(false)
  const [logTopic, setLogTopic] = useState<Topic>(topics[0])
  const [logMinutes, setLogMinutes] = useState(45)
  const [confidence, setConfidence] = useState<Confidence>('Getting there')
  const [note, setNote] = useState('')
  const [timerLog, setTimerLog] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null)
  const active = prompts.find((p) => p.id === selected) || prompts[0]
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const tick = window.setInterval(() => setNow(new Date()), 30_000)
    return () => window.clearInterval(tick)
  }, [])
  useEffect(() => {
    if (deadline === null) return
    const update = () => {
      const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
      setRemaining(seconds)
      if (seconds === 0) {
        setDeadline(null)
        setNotice(
          'Time is up. Take a moment to reflect and save your practice.',
        )
      }
    }
    update()
    const interval = window.setInterval(update, 250)
    return () => window.clearInterval(interval)
  }, [deadline])
  function commit(next: PracticeData) {
    setData(next)
    try {
      localStorage.setItem(practiceKey(user.id), JSON.stringify(next))
      setStorageError(false)
    } catch {
      setStorageError(true)
    }
  }
  function choosePrompt(id: string) {
    if (remaining !== duration * 60 || deadline !== null) {
      setView('Practice room')
      setNotice(
        'Finish or reset your current practice before choosing a different prompt.',
      )
      return
    }
    setSelected(id)
    setShowHints(false)
    setView('Practice room')
    setNotice('')
  }
  function resetTimer() {
    setDeadline(null)
    setRemaining(duration * 60)
    setNotice('Timer reset. Ready when you are.')
  }
  function openLog(fromTimer = false) {
    setTimerLog(fromTimer)
    const seconds =
      deadline === null
        ? remaining
        : Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
    if (fromTimer) {
      setDeadline(null)
      setRemaining(seconds)
    }
    setLogTopic(fromTimer ? active.topic : topics[0])
    setLogMinutes(
      fromTimer ? Math.max(1, Math.ceil((duration * 60 - seconds) / 60)) : 45,
    )
    setConfidence('Getting there')
    setNote('')
    dialog.current?.showModal()
  }
  function saveLog(e: FormEvent) {
    e.preventDefault()
    if (!Number.isInteger(logMinutes) || logMinutes < 1 || logMinutes > 180)
      return
    const entry: PracticeEntry = {
      id: crypto.randomUUID(),
      topic: logTopic,
      minutes: logMinutes,
      confidence,
      note: note.trim(),
      date: new Date().toISOString(),
    }
    commit({ ...data, entries: [entry, ...data.entries] })
    setNow(new Date(entry.date))
    if (timerLog) {
      setDeadline(null)
      setRemaining(duration * 60)
    }
    setRemoved(null)
    dialog.current?.close()
    setNotice('Practice recorded. A little more prepared than yesterday.')
    setView('My notebook')
  }
  function bookmark(id: string) {
    commit({
      ...data,
      saved: data.saved.includes(id)
        ? data.saved.filter((p) => p !== id)
        : [...data.saved, id],
    })
  }
  function exportNotebook() {
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'interviewpair-practice.json'
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  const week = weeklyEntries(data.entries, now)
  const weekMinutes = week.reduce((sum, e) => sum + e.minutes, 0)
  const practicedTopics = new Set(data.entries.map((e) => e.topic)).size
  const visiblePrompts = prompts.filter(
    (p) =>
      (filter === 'All topics' || p.topic === filter) &&
      (!savedOnly || data.saved.includes(p.id)),
  )
  const start = weekStart(now)
  const days = Array.from({ length: 7 }, (_, i) => {
    const day = new Date(start)
    day.setDate(day.getDate() + i)
    const count = week.filter(
      (e) => new Date(e.date).toDateString() === day.toDateString(),
    ).length
    return {
      label: day.toLocaleDateString('en', { weekday: 'short' }),
      count,
      today: day.toDateString() === now.toDateString(),
    }
  })
  const timerStarted = remaining < duration * 60 || deadline !== null
  const firstName = user.displayName.trim().split(/\s+/)[0] || 'there'
  return (
    <div className="workspace">
      <aside className="sidebar" aria-label="Workspace navigation">
        <div className="workspace-label">YOUR WORKSPACE</div>
        <nav>
          {(['Overview', 'Practice room', 'My notebook'] as View[]).map(
            (item, i) => (
              <button
                key={item}
                className={`nav-item ${view === item ? 'selected' : ''}`}
                aria-current={view === item ? 'page' : undefined}
                onClick={() => {
                  setView(item)
                  setNotice('')
                }}
              >
                <Icon name={['overview', 'practice', 'notebook'][i]} />
                {item}
                {item === 'My notebook' && data.entries.length > 0 && (
                  <span className="count">{data.entries.length}</span>
                )}
              </button>
            ),
          )}
        </nav>
        <div className="sidebar-note">
          <span className="tiny-label">BETTER, TOGETHER</span>
          <p>Good interviews start with a little practice.</p>
          <span className="muted small">
            Partner matching is on the way. Build your confidence here today.
          </span>
        </div>
        <div className="profile">
          <span className="avatar">
            {user.displayName.slice(0, 1).toUpperCase()}
          </span>
          <div>
            <strong>{user.displayName}</strong>
            <span title={user.email}>{user.email}</span>
          </div>
        </div>
      </aside>
      <main id="main-content" className="dashboard-main">
        <div className="page-context">
          <span>
            Workspace <span aria-hidden="true">/</span> <strong>{view}</strong>
          </span>
          <div className="page-utilities">
            <button className="text-button" onClick={onLogout}>
              Log out
            </button>
            <time dateTime={now.toISOString()}>
              {now.toLocaleDateString('en', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              })}
            </time>
          </div>
        </div>
        <header className="page-heading">
          <div>
            <p className="eyebrow">
              {view === 'Overview'
                ? 'MAKE ROOM FOR PROGRESS'
                : view === 'Practice room'
                  ? 'ONE PROBLEM. YOUR FULL ATTENTION.'
                  : 'TURN PRACTICE INTO PROGRESS'}
            </p>
            <h1>
              {view === 'Overview' ? (
                <>
                  A little practice.
                  <br />
                  <span className="serif">A lot more confidence.</span>
                </>
              ) : view === 'Practice room' ? (
                <>
                  Your focus <span className="serif">starts here.</span>
                </>
              ) : (
                <>
                  Keep the <span className="serif">learning.</span>
                </>
              )}
            </h1>
            <p className="intro">
              {view === 'Overview'
                ? `Welcome back, ${firstName}. Give your next interview a head start.`
                : view === 'Practice room'
                  ? 'Think out loud. Explore the trade-offs. Make your thinking visible.'
                  : 'Your sessions, honest reflections, and the things to try next.'}
            </p>
          </div>
          {view !== 'Practice room' && (
            <button className="button secondary" onClick={() => openLog()}>
              + Log practice
            </button>
          )}
        </header>
        <div
          aria-live="polite"
          role="status"
          className={notice ? 'notice' : 'sr-only'}
        >
          {notice}
          {removed && (
            <button
              className="text-button"
              onClick={() => {
                commit({
                  ...data,
                  entries: [...data.entries, removed].sort(
                    (a, b) => Date.parse(b.date) - Date.parse(a.date),
                  ),
                })
                setRemoved(null)
                setNotice('Entry restored.')
              }}
            >
              Undo
            </button>
          )}
        </div>
        {storageError && (
          <p role="alert" className="storage-warning">
            Your browser could not load or save your notebook. New changes stay
            in this visit only until saving succeeds. Export your notes before
            closing this page.
          </p>
        )}
        {view === 'Overview' && (
          <>
            <section
              className="stats-strip"
              aria-label="Your practice statistics"
            >
              <div>
                <span className="stat-label">Sessions this week</span>
                <strong>
                  {week.length}
                  <small>/ {data.goal} goal</small>
                </strong>
              </div>
              <div>
                <span className="stat-label">Time well spent this week</span>
                <strong>
                  {weekMinutes}
                  <small>{weekMinutes === 1 ? 'minute' : 'minutes'}</small>
                </strong>
              </div>
              <div>
                <span className="stat-label">Topics explored</span>
                <strong>
                  {practicedTopics}
                  <small>of {topics.length}</small>
                </strong>
              </div>
            </section>
            <div className="overview-grid">
              <section className="feature-panel">
                <div className="panel-kicker">
                  <span className="pill">YOUR NEXT SMALL WIN</span>
                  <span className="small">01 / PRACTICE</span>
                </div>
                <h2>
                  Get comfortable
                  <br />
                  thinking out loud.
                </h2>
                <p>
                  A focused session, one thoughtful prompt, and space to
                  reflect. Start where you are.
                </p>
                <button
                  className="button primary"
                  onClick={() => setView('Practice room')}
                >
                  {timerStarted
                    ? 'Return to your session'
                    : 'Enter practice room'}{' '}
                  <Arrow />
                </button>
                <div className="feature-footer">
                  <Icon name="clock" />
                  <span>25, 45 or 60 minutes. Your pace.</span>
                </div>
                <div className="orbit-art" aria-hidden="true">
                  <div />
                  <div />
                  <span>you</span>
                  <span>
                    next
                    <br />
                    level
                  </span>
                </div>
              </section>
              <section className="panel goal-panel">
                <div className="section-heading">
                  <h2>A rhythm that works</h2>
                  <span className="tiny-label">THIS WEEK</span>
                </div>
                <p className="muted">
                  Consistency starts small. Make a little room for yourself.
                </p>
                <div className="goal-number">
                  {Math.min(week.length, data.goal)}{' '}
                  <span>
                    / {data.goal} {data.goal === 1 ? 'session' : 'sessions'}
                  </span>
                  <Icon name={week.length >= data.goal ? 'check' : 'clock'} />
                </div>
                <progress
                  max={data.goal}
                  value={Math.min(week.length, data.goal)}
                  aria-label="Weekly practice goal"
                />
                <div className="week-days">
                  {days.map((d) => (
                    <div key={d.label} className={d.today ? 'today' : ''}>
                      <span
                        className={d.count ? 'day done' : 'day'}
                        title={`${d.count} ${d.count === 1 ? 'session' : 'sessions'}`}
                        aria-label={`${d.label}: ${d.count} ${d.count === 1 ? 'session' : 'sessions'}`}
                      >
                        {d.count ? <Icon name="check" /> : '·'}
                      </span>
                      <small>{d.label}</small>
                    </div>
                  ))}
                </div>
                <label className="goal-setting">
                  Weekly goal{' '}
                  <select
                    aria-label="Weekly session goal"
                    value={data.goal}
                    onChange={(e) =>
                      commit({ ...data, goal: Number(e.target.value) })
                    }
                  >
                    {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                      <option key={n} value={n}>
                        {n} {n === 1 ? 'session' : 'sessions'}
                      </option>
                    ))}
                  </select>
                </label>
              </section>
            </div>
            <section className="topics-section">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">BUILD YOUR RANGE</p>
                  <h2>A good place to begin</h2>
                </div>
                <button
                  className="text-button"
                  onClick={() => setView('Practice room')}
                >
                  Explore all prompts <Arrow />
                </button>
              </div>
              <div className="topic-grid">
                {prompts.slice(0, 3).map((p, i) => (
                  <button
                    className="topic-card"
                    key={p.id}
                    onClick={() => choosePrompt(p.id)}
                  >
                    <div className="topic-top">
                      <span className="topic-number">0{i + 1}</span>
                      <span className="tag">{p.level}</span>
                    </div>
                    <h3>{p.topic}</h3>
                    <p>{p.title}</p>
                    <span className="topic-link">
                      Start exploring <Arrow />
                    </span>
                  </button>
                ))}
              </div>
            </section>
            <section className="next-chapter">
              <span className="chapter-mark" aria-hidden="true">
                ↔
              </span>
              <div>
                <h2>Practice is good. Practicing together is better.</h2>
                <p>
                  Partner matching and scheduled interviews are coming next.
                  Your practice room is ready now.
                </p>
              </div>
              <span className="tag">ON THE ROADMAP</span>
            </section>
          </>
        )}
        {view === 'Practice room' && (
          <>
            <div className="practice-grid">
              <section className="panel prompt-detail">
                <div className="section-heading">
                  <span className="eyebrow">ON YOUR DESK</span>
                  <button
                    className="text-button"
                    aria-pressed={data.saved.includes(active.id)}
                    onClick={() => bookmark(active.id)}
                  >
                    {data.saved.includes(active.id)
                      ? 'Saved ✓'
                      : '+ Save prompt'}
                  </button>
                </div>
                <div className="prompt-tags">
                  <span className="tag">{active.topic}</span>
                  <span className="tag">{active.level}</span>
                </div>
                <h2>{active.title}</h2>
                <p className="prompt-brief">{active.brief}</p>
                <div className="approach">
                  <span className="tiny-label">A SIMPLE STRUCTURE</span>
                  <ol>
                    <li>Clarify the problem and assumptions.</li>
                    <li>Explain an approach before coding.</li>
                    <li>Test edge cases and discuss trade-offs.</li>
                  </ol>
                </div>
                <button
                  className="text-button"
                  aria-expanded={showHints}
                  onClick={() => setShowHints(!showHints)}
                >
                  {showHints ? 'Hide' : 'Need a nudge? Show'} thinking prompts
                </button>
                {showHints && (
                  <ul className="hints">
                    {active.hints.map((h) => (
                      <li key={h}>{h}</li>
                    ))}
                  </ul>
                )}
              </section>
              <section className="panel timer-panel">
                <p className="eyebrow">PROTECT YOUR FOCUS</p>
                <div
                  className="duration-options"
                  role="group"
                  aria-label="Session duration"
                >
                  {[25, 45, 60].map((n) => (
                    <button
                      key={n}
                      aria-pressed={duration === n}
                      disabled={timerStarted}
                      onClick={() => {
                        setDuration(n)
                        setRemaining(n * 60)
                      }}
                    >
                      {n} min
                    </button>
                  ))}
                </div>
                <div
                  className="timer-display"
                  role="timer"
                  aria-label={`${Math.floor(remaining / 60)} minutes ${remaining % 60} seconds remaining`}
                >
                  {String(Math.floor(remaining / 60)).padStart(2, '0')}
                  <span>:</span>
                  {String(remaining % 60).padStart(2, '0')}
                </div>
                <p className="muted small">
                  {remaining === 0
                    ? 'Well done. Capture what you learned.'
                    : deadline
                      ? 'One thought at a time. You have this.'
                      : timerStarted
                        ? 'Paused. Take a breath.'
                        : 'A little space to do your best thinking.'}
                </p>
                <button
                  className="button primary wide"
                  disabled={remaining === 0}
                  onClick={() => {
                    if (deadline) {
                      setRemaining(
                        Math.max(0, Math.ceil((deadline - Date.now()) / 1000)),
                      )
                      setDeadline(null)
                    } else setDeadline(Date.now() + remaining * 1000)
                  }}
                >
                  {deadline
                    ? 'Pause session'
                    : timerStarted
                      ? 'Resume session'
                      : 'Start session'}
                </button>
                <div className="timer-actions">
                  <button
                    className="text-button"
                    disabled={!timerStarted}
                    onClick={resetTimer}
                  >
                    Reset
                  </button>
                  <button
                    className="text-button"
                    disabled={!timerStarted}
                    onClick={() => openLog(true)}
                  >
                    Finish & reflect <Arrow />
                  </button>
                </div>
                <p className="timer-footnote">
                  Keep this tab open during practice. Your recorded sessions are
                  saved in your notebook.
                </p>
              </section>
            </div>
            <section className="prompt-library">
              <div className="section-heading">
                <h2>Choose your next challenge</h2>
                <div className="library-filters">
                  <select
                    aria-label="Filter prompts by topic"
                    value={filter}
                    onChange={(e) => setFilter(e.target.value)}
                  >
                    <option>All topics</option>
                    {topics.map((t) => (
                      <option key={t}>{t}</option>
                    ))}
                  </select>
                  <button
                    className="button secondary compact"
                    aria-pressed={savedOnly}
                    onClick={() => setSavedOnly(!savedOnly)}
                  >
                    Saved {data.saved.length > 0 && `(${data.saved.length})`}
                  </button>
                </div>
              </div>
              <div className="prompt-list">
                {visiblePrompts.map((p, i) => (
                  <div className="prompt-row" key={p.id}>
                    <span className="muted index">
                      {String(i + 1).padStart(2, '0')}
                    </span>
                    <button
                      className="prompt-name"
                      aria-label={`Practice ${p.title} — ${p.topic}`}
                      onClick={() => choosePrompt(p.id)}
                    >
                      <strong>{p.title}</strong>
                      <span>{p.topic}</span>
                    </button>
                    <span className="tag">{p.level}</span>
                    <button
                      className="icon-button"
                      aria-label={`${data.saved.includes(p.id) ? 'Unsave' : 'Save'} ${p.title}`}
                      aria-pressed={data.saved.includes(p.id)}
                      onClick={() => bookmark(p.id)}
                    >
                      {data.saved.includes(p.id) ? '✓' : '+'}
                    </button>
                  </div>
                ))}
                {visiblePrompts.length === 0 && (
                  <p className="empty-small">
                    No saved prompts in this topic yet. Save a prompt using its
                    + button.
                  </p>
                )}
              </div>
            </section>
          </>
        )}
        {view === 'My notebook' && (
          <section className="panel notebook">
            <div className="section-heading">
              <div>
                <h2>Your practice, in perspective</h2>
                <p className="muted small">
                  {data.entries.length} recorded{' '}
                  {data.entries.length === 1 ? 'session' : 'sessions'} ·{' '}
                  {data.entries.reduce((sum, e) => sum + e.minutes, 0)} total
                  minutes
                </p>
              </div>
              <button
                className="button secondary compact"
                onClick={exportNotebook}
                disabled={!data.entries.length}
              >
                Export notes <span aria-hidden="true">↓</span>
              </button>
            </div>
            {data.entries.length === 0 ? (
              <div className="notebook-empty">
                <div className="empty-symbol">
                  <Icon name="notebook" />
                </div>
                <h3>Your next insight belongs here.</h3>
                <p>
                  Record a practice session and one thing you learned.
                  <br />
                  Over time, you’ll see how far you’ve come.
                </p>
                <button className="button primary" onClick={() => openLog()}>
                  Log your first session <Arrow />
                </button>
              </div>
            ) : (
              <div className="entries">
                {data.entries.map((entry) => (
                  <article className="entry" key={entry.id}>
                    <div className="entry-date">
                      <strong>
                        {new Date(entry.date).toLocaleDateString('en', {
                          day: '2-digit',
                        })}
                      </strong>
                      <span>
                        {new Date(entry.date).toLocaleDateString('en', {
                          month: 'short',
                          year: 'numeric',
                        })}
                      </span>
                    </div>
                    <div className="entry-content">
                      <h3>{entry.topic}</h3>
                      <div className="entry-meta">
                        <span>{entry.minutes} min</span>
                        <span>{entry.confidence}</span>
                      </div>
                      {entry.note && <p>{entry.note}</p>}
                    </div>
                    <button
                      className="text-button delete"
                      aria-label={`Delete ${entry.topic} session from ${new Date(entry.date).toLocaleDateString()}`}
                      onClick={() => {
                        commit({
                          ...data,
                          entries: data.entries.filter(
                            (e) => e.id !== entry.id,
                          ),
                        })
                        setRemoved(entry)
                        setNotice('Session removed.')
                      }}
                    >
                      Remove
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        )}
        <footer className="workspace-footer">
          <span>
            <span className="local-dot" />
            Your notebook stays in this browser, for this account.
          </span>
          <span className="footer-motto">Small steps. Real progress.</span>
        </footer>
      </main>
      <dialog ref={dialog} className="log-dialog" aria-labelledby="log-title">
        <form onSubmit={saveLog}>
          <div className="section-heading">
            <p className="eyebrow">MAKE IT COUNT</p>
            <button
              type="button"
              className="icon-button"
              onClick={() => dialog.current?.close()}
              aria-label="Close practice log"
            >
              ×
            </button>
          </div>
          <h2 id="log-title">A moment to reflect.</h2>
          <p className="muted">Capture the practice. Keep the lesson.</p>
          <div className="form-row">
            <label>
              Topic
              <select
                value={logTopic}
                onChange={(e) => setLogTopic(e.target.value as Topic)}
              >
                {topics.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </label>
            <label>
              Minutes practiced
              <input
                type="number"
                min="1"
                max="180"
                step="1"
                required
                value={logMinutes || ''}
                onChange={(e) => setLogMinutes(Number(e.target.value))}
              />
            </label>
          </div>
          <label>
            How did it feel?
            <select
              value={confidence}
              onChange={(e) => setConfidence(e.target.value as Confidence)}
            >
              {['Learning', 'Getting there', 'Confident'].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
          <label>
            One thing to take forward <span className="muted">(optional)</span>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              maxLength={2000}
              rows={4}
              placeholder="What clicked? What would you try differently?"
            />
          </label>
          <p className="small muted">
            Saved to your notebook on this browser. You can export it anytime.
          </p>
          <button className="button primary wide" type="submit">
            Save practice <Arrow />
          </button>
        </form>
      </dialog>
    </div>
  )
}
