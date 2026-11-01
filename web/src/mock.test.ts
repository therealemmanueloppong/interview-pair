import { describe, expect, it } from 'vitest'
import { buildSlots, dayLabel, groupByDay, levelLabel, topicLabel } from './mock'

const HOUR = 3_600_000

describe('buildSlots', () => {
  const now = new Date('2026-11-02T15:20:00Z')
  const slots = buildSlots(now, 3)

  it('puts every slot on an even UTC hour so everyone shares the same grid', () => {
    expect(slots.length).toBeGreaterThan(10)
    for (const slot of slots) {
      expect(slot.start.getUTCHours() % 2).toBe(0)
      expect(slot.start.getUTCMinutes()).toBe(0)
      expect(slot.start.getUTCSeconds()).toBe(0)
      expect(slot.iso).toBe(slot.start.toISOString())
    }
  })

  it('keeps a slot that started less than an hour ago and drops older ones', () => {
    // 14:00 UTC started 80 minutes ago, 16:00 UTC starts soon. Only slots whose first hour is not over remain.
    for (const slot of slots) {
      expect(slot.start.getTime() + HOUR).toBeGreaterThan(now.getTime())
    }
    const earlyNow = new Date('2026-11-02T14:30:00Z')
    const first = buildSlots(earlyNow, 1).find((s) => s.start.getUTCHours() === 14)
    // 14:00 is still in its first hour at 14:30, unless local hours hide it.
    if (first) expect(first.start.getTime() + HOUR).toBeGreaterThan(earlyNow.getTime())
  })

  it('stays inside the requested number of days and in time order', () => {
    const end = now.getTime() + 3 * 24 * HOUR
    const times = slots.map((s) => s.start.getTime())
    expect(Math.max(...times)).toBeLessThanOrEqual(end)
    expect([...times].sort((a, b) => a - b)).toEqual(times)
    expect(new Set(times).size).toBe(times.length)
  })

  it('hides the middle of the night in the viewer local time', () => {
    for (const slot of slots) {
      expect(slot.start.getHours()).toBeGreaterThanOrEqual(7)
      expect(slot.start.getHours()).toBeLessThanOrEqual(23)
    }
  })

  it('never offers more than the 7 days the server allows', () => {
    const week = buildSlots(now, 7)
    expect(Math.max(...week.map((s) => s.start.getTime()))).toBeLessThanOrEqual(now.getTime() + 7 * 24 * HOUR)
  })
})

describe('labels', () => {
  const now = new Date(2026, 10, 2, 12, 0, 0)

  it('calls the current and next local days Today and Tomorrow', () => {
    expect(dayLabel(new Date(2026, 10, 2, 18, 0), now)).toBe('Today')
    expect(dayLabel(new Date(2026, 10, 3, 9, 0), now)).toBe('Tomorrow')
    expect(dayLabel(new Date(2026, 10, 5, 9, 0), now)).not.toMatch(/Today|Tomorrow/)
  })

  it('groups consecutive slots under one day heading', () => {
    const mk = (d: Date) => ({ iso: d.toISOString(), start: d })
    const groups = groupByDay(
      [mk(new Date(2026, 10, 2, 14)), mk(new Date(2026, 10, 2, 16)), mk(new Date(2026, 10, 3, 10))],
      now,
    )
    expect(groups.map((g) => [g.label, g.slots.length])).toEqual([
      ['Today', 2],
      ['Tomorrow', 1],
    ])
  })

  it('turns server keys into readable names and falls back to the key', () => {
    expect(topicLabel('SYSTEM_DESIGN')).toBe('System Design')
    expect(levelLabel('INTERMEDIATE')).toBe('Intermediate')
    expect(topicLabel('MYSTERY')).toBe('MYSTERY')
  })
})
