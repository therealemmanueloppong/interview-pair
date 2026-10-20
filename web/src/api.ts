export type Health = { status: string; service: string }

export async function fetchHealth(): Promise<Health> {
  const res = await fetch('/api/health')
  if (!res.ok) throw new Error(`Server responded ${res.status}`)
  return res.json()
}
