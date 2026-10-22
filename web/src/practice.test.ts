import { beforeEach, describe, expect, it } from 'vitest'
import {
  emptyPractice,
  practiceKey,
  readPractice,
  weeklyEntries,
} from './practice'
import type { PracticeEntry } from './practice'
beforeEach(() => localStorage.clear())
const entry = (date: string): PracticeEntry => ({
  id: date,
  topic: 'Graphs',
  minutes: 30,
  confidence: 'Learning',
  note: '',
  date,
})
describe('practice persistence', () => {
  it('keeps notebooks separate between accounts', () => {
    localStorage.setItem(
      practiceKey('ada'),
      JSON.stringify({
        ...emptyPractice(),
        entries: [entry('2026-10-20T12:00:00Z')],
      }),
    )
    expect(readPractice('ada').data.entries).toHaveLength(1)
    expect(readPractice('grace').data.entries).toHaveLength(0)
  })
  it('recovers safely from malformed and invalid saved data', () => {
    for (const raw of [
      'not-json',
      JSON.stringify({ ...emptyPractice(), entries: [{}] }),
      JSON.stringify({ ...emptyPractice(), goal: 100 }),
    ]) {
      localStorage.setItem(practiceKey('ada'), raw)
      expect(readPractice('ada')).toEqual({
        data: emptyPractice(),
        error: true,
      })
    }
  })
  it('counts this calendar week only, excluding future entries', () => {
    const now = new Date(2026, 9, 21, 12)
    const dates = [
      new Date(2026, 9, 18, 23),
      new Date(2026, 9, 19),
      now,
      new Date(2026, 9, 22),
    ]
    expect(
      weeklyEntries(
        dates.map((d) => entry(d.toISOString())),
        now,
      ),
    ).toHaveLength(2)
  })
})
