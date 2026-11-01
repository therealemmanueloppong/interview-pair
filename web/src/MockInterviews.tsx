import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from './api'
import {
  LEVELS,
  TOPICS,
  buildSlots,
  groupByDay,
  levelLabel,
  timeLabel,
  timeZoneName,
  topicLabel,
  whenLabel,
} from './mock'
import type { LevelKey, TopicKey } from './mock'
import {
  cancelRequest,
  fetchRequests,
  fetchSessions,
  requestInterview,
} from './mockApi'
import type { RequestView, SessionSummary } from './mockApi'
import SessionRoom from './SessionRoom'

const POLL_MS = 10_000

// The visible button text, also used in its accessible name so the two always agree.
const actionLabel = (s: SessionSummary) =>
  s.status === 'ACTIVE' ? 'Open room' : s.feedbackGiven ? 'View feedback' : 'Leave feedback'

const messageOf = (err: unknown) =>
  err instanceof ApiError || err instanceof Error ? err.message : 'Something went wrong'

type WizardProps = {
  onScheduled: (request: RequestView) => void
  onCancel: () => void
}

/** Three short steps: what to practice, how experienced you are, and when. */
function ScheduleWizard({ onScheduled, onCancel }: WizardProps) {
  const [now] = useState(() => new Date())
  const [step, setStep] = useState(0)
  const [topic, setTopic] = useState<TopicKey>('ALGORITHMS')
  const [level, setLevel] = useState<LevelKey>('BEGINNER')
  const [slot, setSlot] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const days = groupByDay(buildSlots(now), now)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (step < 2) {
      setStep(step + 1)
      return
    }
    setBusy(true)
    setError('')
    try {
      onScheduled(await requestInterview(topic, level, slot))
    } catch (err) {
      setError(messageOf(err))
      setBusy(false)
    }
  }

  return (
    <form className="panel mock-wizard" onSubmit={submit} aria-label="Schedule a mock interview">
      <p className="tiny-label">STEP {step + 1} OF 3</p>
      {step === 0 && (
        <fieldset>
          <legend>What do you want to practice?</legend>
          {TOPICS.map((t) => (
            <label key={t.key} className={`choice ${topic === t.key ? 'chosen' : ''}`}>
              <input
                type="radio"
                name="topic"
                checked={topic === t.key}
                onChange={() => setTopic(t.key)}
              />
              <span>
                <strong>{t.label}</strong>
                <small>{t.blurb}</small>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {step === 1 && (
        <fieldset>
          <legend>Choose your level</legend>
          <p className="muted small">We pair you with someone at the same level.</p>
          {LEVELS.map((l) => (
            <label key={l.key} className={`choice ${level === l.key ? 'chosen' : ''}`}>
              <input
                type="radio"
                name="level"
                checked={level === l.key}
                onChange={() => setLevel(l.key)}
              />
              <span>
                <strong>{l.label}</strong>
                <small>{l.blurb}</small>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {step === 2 && (
        <fieldset>
          <legend>Pick a time</legend>
          <p className="muted small">
            Times are in {timeZoneName()}. Everyone sees the same slots, and a session
            lasts about an hour.
          </p>
          {days.map((day) => (
            <div key={day.label} className="slot-day">
              <h3>{day.label}</h3>
              <div className="slot-grid">
                {day.slots.map((s) => (
                  <label key={s.iso} className={`slot ${slot === s.iso ? 'chosen' : ''}`}>
                    <input
                      type="radio"
                      name="slot"
                      value={s.iso}
                      checked={slot === s.iso}
                      onChange={() => setSlot(s.iso)}
                    />
                    {timeLabel(s.start)}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </fieldset>
      )}
      {error && (
        <p role="alert" className="form-error">
          {error}
        </p>
      )}
      <div className="wizard-actions">
        {step === 0 ? (
          <button type="button" className="button secondary" onClick={onCancel}>
            Cancel
          </button>
        ) : (
          <button type="button" className="button secondary" onClick={() => setStep(step - 1)}>
            Back
          </button>
        )}
        <button type="submit" className="button primary" disabled={busy || (step === 2 && !slot)}>
          {step < 2 ? 'Next' : busy ? 'Scheduling...' : 'Schedule interview'}
        </button>
      </div>
    </form>
  )
}

export default function MockInterviews() {
  const [sessions, setSessions] = useState<SessionSummary[] | null>(null)
  const [requests, setRequests] = useState<RequestView[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [scheduling, setScheduling] = useState(false)
  const [openId, setOpenId] = useState<string | null>(null)
  // Session ids seen so far, so a partner showing up later can be announced. Null until first load.
  const seen = useRef<Set<string> | null>(null)

  const refresh = useCallback(async () => {
    try {
      const [nextSessions, nextRequests] = await Promise.all([fetchSessions(), fetchRequests()])
      if (seen.current) {
        const fresh = nextSessions.filter((s) => !seen.current!.has(s.id))
        if (fresh.length > 0) {
          setNotice(`You were matched with ${fresh[0].partner.displayName} for ${fresh[0].questionTitle}.`)
        }
      }
      seen.current = new Set(nextSessions.map((s) => s.id))
      setSessions(nextSessions)
      setRequests(nextRequests)
      setError('')
    } catch (err) {
      setError(messageOf(err))
    }
  }, [])

  useEffect(() => {
    refresh()
    const timer = window.setInterval(refresh, POLL_MS)
    return () => window.clearInterval(timer)
  }, [refresh])

  async function scheduled(request: RequestView) {
    setScheduling(false)
    await refresh()
    if (request.sessionId) {
      setOpenId(request.sessionId)
    } else {
      setNotice(
        `You are on the list for ${whenLabel(request.slotStart)}. We will pair you as soon as someone else asks for the same topic, level and time.`,
      )
    }
  }

  async function cancel(request: RequestView) {
    try {
      await cancelRequest(request.id)
      setNotice('Request cancelled.')
      await refresh()
    } catch (err) {
      setError(messageOf(err))
    }
  }

  if (openId) {
    return (
      <SessionRoom
        key={openId}
        sessionId={openId}
        onBack={() => {
          setOpenId(null)
          refresh()
        }}
      />
    )
  }

  return (
    <section className="mock" aria-label="Mock interviews">
      <div className="mock-intro">
        <div>
          <h2>How it works</h2>
          <p className="muted">
            Get paired with another student for a live mock interview. You take turns: one
            person answers while the other interviews, then you switch roles.
          </p>
        </div>
        {!scheduling && (
          <button className="button primary" onClick={() => { setScheduling(true); setNotice('') }}>
            Schedule peer mock interview
          </button>
        )}
      </div>

      <div aria-live="polite" role="status" className={notice ? 'notice' : 'sr-only'}>
        {notice}
      </div>
      {error && (
        <p role="alert" className="storage-warning">
          {error}
        </p>
      )}

      {scheduling && (
        <ScheduleWizard onScheduled={scheduled} onCancel={() => setScheduling(false)} />
      )}

      {requests.length > 0 && (
        <div className="panel">
          <h3>Waiting for a partner</h3>
          <ul className="mock-list">
            {requests.map((r) => (
              <li key={r.id}>
                <span>
                  <strong>{r.topicLabel}</strong> · {levelLabel(r.level)}
                  <small>{whenLabel(r.slotStart)}</small>
                </span>
                <button
                  className="text-button"
                  aria-label={`Cancel ${r.topicLabel} request for ${whenLabel(r.slotStart)}`}
                  onClick={() => cancel(r)}
                >
                  Cancel
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="panel">
        <h3>Your sessions</h3>
        {sessions === null ? (
          <p className="muted">Loading your sessions...</p>
        ) : sessions.length === 0 ? (
          <p className="muted">
            No sessions yet. Schedule one and we will pair you with someone at your level.
          </p>
        ) : (
          <table className="mock-table">
            <thead>
              <tr>
                <th scope="col">Session</th>
                <th scope="col">When</th>
                <th scope="col">Partner</th>
                <th scope="col">
                  <span className="sr-only">Action</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.id}>
                  <td>
                    <strong>{topicLabel(s.topic)}</strong>
                    <small>
                      {levelLabel(s.level)} · {s.questionTitle}
                    </small>
                  </td>
                  <td>{whenLabel(s.slotStart)}</td>
                  <td>{s.partner.displayName}</td>
                  <td>
                    <button
                      className="button secondary compact"
                      aria-label={`${actionLabel(s)} for ${s.questionTitle} with ${s.partner.displayName}`}
                      onClick={() => setOpenId(s.id)}
                    >
                      {actionLabel(s)}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  )
}
