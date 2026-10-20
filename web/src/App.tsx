import { useEffect, useState } from 'react'
import { fetchHealth } from './api'

type State = { kind: 'loading' } | { kind: 'up'; service: string } | { kind: 'down' }

export default function App() {
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    fetchHealth()
      .then((h) => setState({ kind: 'up', service: h.service }))
      .catch(() => setState({ kind: 'down' }))
  }, [])

  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-4 p-6">
      <h1 className="text-3xl font-bold">InterviewPair</h1>
      <p className="text-slate-600">
        Find a partner for a mock technical interview, by topic, level and time.
      </p>
      <p role="status" className="rounded border p-3 text-sm">
        {state.kind === 'loading' && 'Checking server...'}
        {state.kind === 'up' && `Server online: ${state.service}`}
        {state.kind === 'down' && 'Server is not reachable'}
      </p>
    </main>
  )
}
