import { useRef } from 'react'
import type { KeyboardEvent, UIEvent } from 'react'

type Props = {
  value: string
  onChange: (value: string) => void
  readOnly: boolean
  label: string
  /** Line numbers make sense for code but not for a notepad. */
  lineNumbers: boolean
}

const TAB = '    '

/**
 * A plain text editor with line numbers and a Tab key that indents. Only one person edits at a
 * time in a mock interview, so there is nothing to merge and a text area is enough.
 */
export default function CodeEditor({ value, onChange, readOnly, label, lineNumbers }: Props) {
  const gutter = useRef<HTMLDivElement>(null)
  // Tab indents inside the editor. Pressing Escape first lets the next Tab move focus away.
  const letTabLeave = useRef(false)
  const lines = value.split('\n').length

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (readOnly) return
    if (event.key === 'Escape') {
      letTabLeave.current = true
      return
    }
    if (event.key === 'Tab' && !event.shiftKey) {
      if (letTabLeave.current) {
        letTabLeave.current = false
        return
      }
      event.preventDefault()
      const area = event.currentTarget
      const { selectionStart, selectionEnd } = area
      const next = value.slice(0, selectionStart) + TAB + value.slice(selectionEnd)
      onChange(next)
      // Put the cursor after the inserted spaces once React has rendered the new value.
      requestAnimationFrame(() => {
        area.selectionStart = area.selectionEnd = selectionStart + TAB.length
      })
    } else {
      letTabLeave.current = false
    }
  }

  function onScroll(event: UIEvent<HTMLTextAreaElement>) {
    if (gutter.current) gutter.current.scrollTop = event.currentTarget.scrollTop
  }

  return (
    <div className={`code-editor ${readOnly ? 'read-only' : ''}`}>
      {lineNumbers && (
        <div className="code-gutter" ref={gutter} aria-hidden="true">
          {Array.from({ length: lines }, (_, i) => (
            <div key={i}>{i + 1}</div>
          ))}
        </div>
      )}
      <textarea
        aria-label={label}
        value={value}
        readOnly={readOnly}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={onKeyDown}
        onScroll={onScroll}
        spellCheck={false}
        autoCapitalize="off"
        autoCorrect="off"
        wrap="off"
      />
    </div>
  )
}
