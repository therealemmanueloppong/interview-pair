// Pure helpers for the mock interview screens, kept apart from React so they are easy to test.

export type TopicKey = 'ALGORITHMS' | 'BEHAVIORAL' | 'SYSTEM_DESIGN'
export type LevelKey = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export type TopicInfo = { key: TopicKey; label: string; blurb: string }
export type LevelInfo = { key: LevelKey; label: string; blurb: string }

export const TOPICS: TopicInfo[] = [
  {
    key: 'ALGORITHMS',
    label: 'Data Structures & Algorithms',
    blurb: 'Solve a coding question together in a live code editor.',
  },
  {
    key: 'BEHAVIORAL',
    label: 'Behavioral',
    blurb: 'Practice telling your stories clearly. You get a shared notepad.',
  },
  {
    key: 'SYSTEM_DESIGN',
    label: 'System Design',
    blurb: 'Talk through a design and sketch it in a shared notepad.',
  },
]

export const LEVELS: LevelInfo[] = [
  { key: 'BEGINNER', label: 'Beginner', blurb: 'I am new to mock interviews.' },
  {
    key: 'INTERMEDIATE',
    label: 'Intermediate',
    blurb: 'I have done a few interviews already.',
  },
  { key: 'ADVANCED', label: 'Advanced', blurb: 'I want a tough, realistic session.' },
]

export const topicLabel = (key: string) =>
  TOPICS.find((t) => t.key === key)?.label ?? key

export const levelLabel = (key: string) =>
  LEVELS.find((l) => l.key === key)?.label ?? key

export type Slot = { iso: string; start: Date }

const HOUR = 3_600_000
const SESSION_LENGTH = HOUR

/**
 * Two hour slots on the UTC clock. Everyone gets the same instants whatever their time zone, so
 * two people can find each other. Slots that have already finished their first hour are left out,
 * and the early hours of the night are hidden from the list.
 */
export function buildSlots(now: Date, days = 3): Slot[] {
  const slots: Slot[] = []
  const twoHours = 2 * HOUR
  // The first grid point at or after (now - one hour) is still bookable until its first hour ends.
  let t = Math.ceil((now.getTime() - SESSION_LENGTH) / twoHours) * twoHours
  const end = now.getTime() + days * 24 * HOUR
  for (; t <= end; t += twoHours) {
    const start = new Date(t)
    const hour = start.getHours()
    if (hour < 7 || hour > 23) continue
    slots.push({ iso: start.toISOString(), start })
  }
  return slots
}

const sameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString()

export function dayLabel(date: Date, now: Date): string {
  const tomorrow = new Date(now.getTime() + 24 * HOUR)
  if (sameDay(date, now)) return 'Today'
  if (sameDay(date, tomorrow)) return 'Tomorrow'
  return date.toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
  })
}

export const timeLabel = (date: Date) =>
  date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })

export function groupByDay(slots: Slot[], now: Date): { label: string; slots: Slot[] }[] {
  const groups: { label: string; slots: Slot[] }[] = []
  for (const slot of slots) {
    const label = dayLabel(slot.start, now)
    const last = groups[groups.length - 1]
    if (last && last.label === label) last.slots.push(slot)
    else groups.push({ label, slots: [slot] })
  }
  return groups
}

export function whenLabel(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  return `${dayLabel(date, now)}, ${timeLabel(date)}`
}

export const timeZoneName = () =>
  new Intl.DateTimeFormat(undefined, { timeZoneName: 'short' })
    .formatToParts(new Date())
    .find((p) => p.type === 'timeZoneName')?.value ?? 'your time zone'
