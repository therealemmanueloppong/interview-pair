import { ApiError, request } from './api'
import type { LevelKey, TopicKey } from './mock'
import { loadToken } from './tokenStorage'

export type Partner = { id: string; displayName: string }

export type Question = {
  id: string
  title: string
  prompt: string
  language: 'python' | 'text'
  starterCode: string
  // The server sends hints and the solution only to the person who is not answering.
  hints: string[] | null
  solution: string | null
}

export type RequestView = {
  id: string
  topic: TopicKey
  topicLabel: string
  level: LevelKey
  slotStart: string
  status: 'OPEN' | 'MATCHED' | 'CANCELLED'
  sessionId: string | null
}

export type SessionSummary = {
  id: string
  topic: TopicKey
  topicLabel: string
  level: LevelKey
  slotStart: string
  status: 'ACTIVE' | 'DONE'
  round: number
  partner: Partner
  questionTitle: string
  feedbackGiven: boolean
}

export type SessionView = {
  id: string
  topic: TopicKey
  topicLabel: string
  level: LevelKey
  slotStart: string
  status: 'ACTIVE' | 'DONE'
  round: number
  youAreInterviewee: boolean
  partner: Partner
  question: Question
  code: string
  feedbackGiven: boolean
}

export type FeedbackOverview = {
  given: boolean
  received: { rating: number; comment: string; createdAt: string } | null
}

function authed<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = loadToken()
  if (!token) return Promise.reject(new ApiError(401, 'Please log in again'))
  return request<T>(path, init, token)
}

const post = (body?: unknown): RequestInit => ({
  method: 'POST',
  body: body === undefined ? undefined : JSON.stringify(body),
})

export const requestInterview = (topic: TopicKey, level: LevelKey, slotStart: string) =>
  authed<RequestView>('/api/interview-requests', post({ topic, level, slotStart }))

export const fetchRequests = () => authed<RequestView[]>('/api/interview-requests/mine')

export const cancelRequest = (id: string) =>
  authed<void>(`/api/interview-requests/${id}`, { method: 'DELETE' })

export const fetchSessions = () => authed<SessionSummary[]>('/api/sessions')

export const fetchSession = (id: string) => authed<SessionView>(`/api/sessions/${id}`)

export const switchRoles = (id: string) => authed<SessionView>(`/api/sessions/${id}/switch`, post())

export const finishSession = (id: string) => authed<SessionView>(`/api/sessions/${id}/finish`, post())

export const fetchFeedback = (id: string) => authed<FeedbackOverview>(`/api/sessions/${id}/feedback`)

export const leaveFeedback = (id: string, rating: number, comment: string) =>
  authed<FeedbackOverview>(`/api/sessions/${id}/feedback`, post({ rating, comment }))

export const fetchTicket = (id: string) =>
  authed<{ ticket: string }>(`/api/sessions/${id}/ws-ticket`, post())
