import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from './api'
import CodeEditor from './CodeEditor'
import { levelLabel, whenLabel } from './mock'
import {
  fetchFeedback,
  fetchSession,
  finishSession,
  leaveFeedback,
  switchRoles,
} from './mockApi'
import type { FeedbackOverview, SessionView } from './mockApi'
import { useSessionSocket } from './useSessionSocket'

type Props = { sessionId: string; onBack: () => void }

const messageOf = (err: unknown) =>
  err instanceof ApiError || err instanceof Error ? err.message : 'Something went wrong'

export default function SessionRoom({ sessionId, onBack }: Props) {
  const [initial, setInitial] = useState<SessionView | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchSession(sessionId)
      .then(setInitial)
      .catch((err) => setError(messageOf(err)))
  }, [sessionId])

  if (error) {
    return (
      <section className="panel">
        <p role="alert" className="form-error">
          {error}
        </p>
        <button className="button secondary" onClick={onBack}>
          Back to sessions
        </button>
      </section>
    )
  }
  if (!initial) return <p className="muted">Opening your session...</p>
  return <Room initial={initial} onBack={onBack} />
}

function Room({ initial, onBack }: { initial: SessionView; onBack: () => void }) {
  const { session: live, code, online, connection, notice, edit, replaceSession } =
    useSessionSocket(initial.id, initial)
  const session = live ?? initial
  const [tab, setTab] = useState<'question' | 'guide'>('question')
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState('')

  const { question, partner } = session
  const answering = session.youAreInterviewee
  const done = session.status === 'DONE'
  const hasGuide = question.hints !== null
  // If the roles flip while the guide tab is open, the tab disappears, so fall back to the question.
  const activeTab = tab === 'guide' && hasGuide ? 'guide' : 'question'
  const partnerOnline = online.includes(partner.id)
  const isCode = question.language === 'python'

  async function act(action: (id: string) => Promise<SessionView>) {
    setBusy(true)
    setActionError('')
    try {
      replaceSession(await action(session.id))
    } catch (err) {
      setActionError(messageOf(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="room" aria-label="Mock interview room">
      <div className="room-top">
        <button className="text-button" onClick={onBack}>
          ← Back to sessions
        </button>
        <div className="room-meta">
          <span className="tag">{session.topicLabel}</span>
          <span className="tag">{levelLabel(session.level)}</span>
          <span className="muted small">{whenLabel(session.slotStart)}</span>
        </div>
      </div>

      <header className="room-header">
        <div>
          <p className="tiny-label">
            {done ? 'SESSION COMPLETE' : `ROUND ${session.round} OF 2`}
          </p>
          <h2>{question.title}</h2>
        </div>
        <div className="room-people">
          <span className={`role-badge ${answering && !done ? 'answering' : 'interviewing'}`}>
            {done ? 'Session finished' : answering ? 'You are answering' : 'You are interviewing'}
          </span>
          <span className="partner-status">
            <span className={`dot ${partnerOnline ? 'on' : ''}`} aria-hidden="true" />
            {partner.displayName} is {partnerOnline ? 'here' : 'not connected yet'}
          </span>
        </div>
      </header>

      {connection !== 'open' && (
        <p role="status" className="notice">
          {connection === 'closed'
            ? notice || 'This room is closed.'
            : 'Connecting to the live room. Your partner may not see your changes yet.'}
        </p>
      )}
      {connection === 'open' && notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}

      <div className="room-grid">
        <div className="panel room-guide">
          <div className="room-tabs" role="tablist" aria-label="Question and guide">
            <button
              role="tab"
              aria-selected={activeTab === 'question'}
              className={activeTab === 'question' ? 'selected' : ''}
              onClick={() => setTab('question')}
            >
              Question
            </button>
            {hasGuide && (
              <button
                role="tab"
                aria-selected={activeTab === 'guide'}
                className={activeTab === 'guide' ? 'selected' : ''}
                onClick={() => setTab('guide')}
              >
                {done ? 'Hints and solution' : 'Interviewer guide'}
              </button>
            )}
          </div>
          {activeTab === 'question' ? (
            <div role="tabpanel" aria-label="Question">
              <p className="room-prompt">{question.prompt}</p>
              <p className="muted small">
                {answering
                  ? 'Think out loud while you work. Your partner sees everything you type.'
                  : 'Ask follow up questions. Use the guide only if your partner is stuck.'}
              </p>
            </div>
          ) : (
            <div role="tabpanel" aria-label="Interviewer guide">
              {!done && (
                <p className="muted small">Only you can see this. Your partner never receives it.</p>
              )}
              <h3>Hints to offer</h3>
              <ol>
                {question.hints?.map((hint) => (
                  <li key={hint}>{hint}</li>
                ))}
              </ol>
              <h3>What a strong answer looks like</h3>
              <p className="room-solution">{question.solution}</p>
            </div>
          )}
        </div>

        <div className="panel room-editor">
          <div className="room-editor-head">
            <strong>
              {isCode ? 'Code' : 'Shared notes'} ·{' '}
              {answering ? 'you are typing' : `${partner.displayName} is typing`}
            </strong>
            <span className="muted small">
              {done ? 'Locked' : answering ? 'Your partner sees this live' : 'Read only'}
            </span>
          </div>
          <CodeEditor
            value={code}
            onChange={edit}
            readOnly={!answering || done}
            label={answering ? 'Your answer' : `${partner.displayName}'s answer, read only`}
            lineNumbers={isCode}
          />
          {!done && answering && isCode && (
            <p className="muted small">Press Escape, then Tab, to leave the editor with the keyboard.</p>
          )}
        </div>
      </div>

      {actionError && (
        <p role="alert" className="form-error">
          {actionError}
        </p>
      )}

      {!done ? (
        <div className="room-actions">
          {session.round === 1 ? (
            <>
              <button className="button primary" disabled={busy} onClick={() => act(switchRoles)}>
                Switch roles
              </button>
              <button className="text-button" disabled={busy} onClick={() => act(finishSession)}>
                End session early
              </button>
            </>
          ) : (
            <button className="button primary" disabled={busy} onClick={() => act(finishSession)}>
              Finish session
            </button>
          )}
          <span className="muted small">
            {session.round === 1
              ? 'Switch when you are ready. You will each get a different question.'
              : 'Either of you can finish the session.'}
          </span>
        </div>
      ) : (
        <FeedbackPanel sessionId={session.id} partnerName={partner.displayName} />
      )}
    </section>
  )
}

function FeedbackPanel({ sessionId, partnerName }: { sessionId: string; partnerName: string }) {
  const [overview, setOverview] = useState<FeedbackOverview | null>(null)
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetchFeedback(sessionId)
      .then(setOverview)
      .catch((err) => setError(messageOf(err)))
  }, [sessionId])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      setOverview(await leaveFeedback(sessionId, rating, comment))
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="panel room-feedback">
      <h3>Feedback</h3>
      {error && (
        <p role="alert" className="form-error">
          {error}
        </p>
      )}
      {overview?.received ? (
        <div className="received">
          <p className="tiny-label">FROM {partnerName.toUpperCase()}</p>
          <p>
            <strong>{overview.received.rating} out of 5</strong>
          </p>
          <p>{overview.received.comment}</p>
        </div>
      ) : (
        overview && (
          <p className="muted small">
            {partnerName} has not left feedback for you yet. It will show up here.
          </p>
        )
      )}
      {overview &&
        (overview.given ? (
          <p>Thanks, your feedback for {partnerName} was saved.</p>
        ) : (
          <form onSubmit={submit}>
            <fieldset>
              <legend>How was {partnerName} as a partner?</legend>
              <div className="rating">
                {[1, 2, 3, 4, 5].map((n) => (
                  <label key={n} className={rating === n ? 'chosen' : ''}>
                    <input
                      type="radio"
                      name="rating"
                      value={n}
                      checked={rating === n}
                      onChange={() => setRating(n)}
                    />
                    {n}
                  </label>
                ))}
              </div>
            </fieldset>
            <label className="field">
              What went well, and what could they improve?
              <textarea
                value={comment}
                maxLength={1000}
                rows={4}
                onChange={(event) => setComment(event.target.value)}
              />
            </label>
            <button
              type="submit"
              className="button primary"
              disabled={busy || rating === 0 || comment.trim() === ''}
            >
              Save feedback
            </button>
          </form>
        ))}
    </div>
  )
}
