export const topics = [
  'Arrays & strings',
  'Hash maps',
  'Trees',
  'Graphs',
  'Dynamic programming',
  'System design',
] as const
export type Topic = (typeof topics)[number]
export type Confidence = 'Learning' | 'Getting there' | 'Confident'
export type PracticeEntry = {
  id: string
  topic: Topic
  minutes: number
  confidence: Confidence
  note: string
  date: string
}
export type PracticeData = {
  version: 1
  goal: number
  entries: PracticeEntry[]
  saved: string[]
}
export const emptyPractice = (): PracticeData => ({
  version: 1,
  goal: 3,
  entries: [],
  saved: [],
})
export const practiceKey = (userId: string) =>
  `interviewpair.practice.v1.${userId}`
export const prompts: {
  id: string
  topic: Topic
  level: string
  title: string
  brief: string
  hints: string[]
}[] = [
  {
    id: 'window',
    topic: 'Arrays & strings',
    level: 'Medium',
    title: 'A window into the problem',
    brief:
      'Given a string, find the length of its longest substring with no repeated characters. Explain how your approach changes as the input grows.',
    hints: [
      'Start with a brute-force solution and name its cost.',
      'Can you maintain a window that always has unique characters?',
      'Walk through an empty string and a string with all identical characters.',
    ],
  },
  {
    id: 'group',
    topic: 'Hash maps',
    level: 'Medium',
    title: 'Find the hidden groups',
    brief:
      'Group a list of words into sets of anagrams. Explain what makes two words belong together, and choose a representation for that relationship.',
    hints: [
      'What signature would every anagram share?',
      'Compare sorting each word with counting its characters.',
      'Discuss duplicates, empty words, and character sets.',
    ],
  },
  {
    id: 'tree',
    topic: 'Trees',
    level: 'Easy',
    title: 'One level at a time',
    brief:
      'Return the values in a binary tree grouped by level, from the root down. Talk through the data structure you would use and why.',
    hints: [
      'What structure preserves first-in, first-out order?',
      'How will you know where a level ends?',
      'Compare space usage for a wide tree and a long chain.',
    ],
  },
  {
    id: 'islands',
    topic: 'Graphs',
    level: 'Medium',
    title: 'Map the islands',
    brief:
      'A grid contains land and water. Count the connected islands, where land cells connect horizontally or vertically. Explain how to avoid counting an island twice.',
    hints: [
      'Treat each land cell as a node in a graph.',
      'Choose depth-first or breadth-first traversal.',
      'Consider an empty grid and a grid entirely covered by land.',
    ],
  },
  {
    id: 'stairs',
    topic: 'Dynamic programming',
    level: 'Easy',
    title: 'Small steps, better answers',
    brief:
      'You can climb one or two steps at a time. How many distinct ways are there to climb n steps? Derive a recurrence before writing code.',
    hints: [
      'List the possibilities for the first few values of n.',
      'Which earlier answers determine the next one?',
      'Can you reduce memory without changing the running time?',
    ],
  },
  {
    id: 'queue',
    topic: 'System design',
    level: 'Open-ended',
    title: 'Design a fair waiting room',
    brief:
      'Design a service that pairs people waiting for a mock interview. Discuss requirements, storage, fairness, and what happens when a participant disconnects.',
    hints: [
      'Clarify scale, wait-time expectations, and matching criteria.',
      'Explain how two workers avoid assigning the same person twice.',
      'Describe retries, timeouts, and the metrics you would watch.',
    ],
  },
]
export function readPractice(userId: string): {
  data: PracticeData
  error: boolean
} {
  try {
    const raw = localStorage.getItem(practiceKey(userId))
    if (!raw) return { data: emptyPractice(), error: false }
    const data = JSON.parse(raw)
    if (
      data.version !== 1 ||
      !Number.isInteger(data.goal) ||
      data.goal < 1 ||
      data.goal > 7 ||
      !Array.isArray(data.entries) ||
      !Array.isArray(data.saved)
    )
      throw new Error('Invalid practice data')
    if (
      !data.entries.every(
        (e: PracticeEntry) =>
          typeof e.id === 'string' &&
          topics.includes(e.topic) &&
          Number.isInteger(e.minutes) &&
          e.minutes > 0 &&
          e.minutes <= 180 &&
          ['Learning', 'Getting there', 'Confident'].includes(e.confidence) &&
          typeof e.note === 'string' &&
          typeof e.date === 'string' &&
          Number.isFinite(Date.parse(e.date)),
      )
    )
      throw new Error('Invalid entry')
    return {
      data: {
        ...data,
        saved: data.saved.filter(
          (id: unknown) =>
            typeof id === 'string' && prompts.some((p) => p.id === id),
        ),
      },
      error: false,
    }
  } catch {
    return { data: emptyPractice(), error: true }
  }
}
export function weekStart(now: Date): Date {
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7))
  return start
}
export function weeklyEntries(entries: PracticeEntry[], now = new Date()) {
  const start = weekStart(now).getTime()
  return entries.filter(
    (e) => Date.parse(e.date) >= start && Date.parse(e.date) <= now.getTime(),
  )
}
